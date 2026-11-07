package com.wetech.demo.web3j.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetech.demo.web3j.Application;
import com.wetech.demo.web3j.contracts.escrow.Escrow;
import com.wetech.demo.web3j.contracts.wqltoken.WQLToken;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Actual HTTP -> Spring -> Service -> disposable local chain; never POTOS group evidence. */
class EscrowHttpIntegrationTest {
    private static final BigInteger UNIT = BigInteger.TEN.pow(18);
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private final List<Map<String, Object>> exchanges = new ArrayList<>();
    private final List<Map<String, String>> transactions = new ArrayList<>();
    private String baseUrl;

    @Test void sixEndpointsWorkThroughRealHttpAndPreserveFailedReceipts() throws Exception {
        HttpService transport = new HttpService("http://127.0.0.1:18545");
        Web3j chain = Web3j.build(transport);
        try {
            assertEquals(BigInteger.valueOf(31337), chain.ethChainId().send().getChainId());
            Credentials backend = account(transport);
            Credentials buyer = account(transport);
            Credentials seller = account(transport);
            var gas = new StaticGasProvider(BigInteger.valueOf(2_000_000_000L), BigInteger.valueOf(6_721_975));
            var buyerTx = new RawTransactionManager(chain, buyer, 31337);
            var sellerTx = new RawTransactionManager(chain, seller, 31337);
            WQLToken token = WQLToken.deploy(chain, buyerTx, gas).send();
            record("deploy token (local only)", token.getTransactionReceipt().orElseThrow());
            record("mint 100 tokens", token.mint(buyer.getAddress(), UNIT.multiply(BigInteger.valueOf(100))).send());

            SpringApplication app = new SpringApplication(Application.class);
            app.setRegisterShutdownHook(false);
            app.setLogStartupInfo(false);
            // Highest-priority in-memory properties prevent any inherited testnet key/RPC being used.
            // The generated private key is never passed on the command line or written to a file.
            app.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("local-http-test", Map.of(
                            "server.address", "127.0.0.1", "server.port", "0",
                            "web3j.client-address", "http://127.0.0.1:18545", "web3j.chain-id", "31337",
                            "web3j.private-key", Numeric.toHexStringWithPrefixZeroPadded(backend.getEcKeyPair().getPrivateKey(), 64),
                            "web3j.gas-price", "2000000000", "web3j.gas-limit", "6721975"))));
            try (var context = app.run()) {
                int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
                baseUrl = "http://127.0.0.1:" + port;
                request("GET", "/address", 409);
                request("GET", "/orders", 409);
                request("GET", "/orders/not-a-number", 400);
                request("GET", "/orders/-1", 400);
                request("GET", "/orders/0/events", 409);
                request("GET", "/orders/not-a-number/events", 400);
                request("GET", "/orders/-1/events", 400);
                request("POST", "/load?address=bad&fromBlock=0", 400);
                request("POST", "/load?address=bad", 400);
                request("POST", "/orders/0/resolve", 400);
                request("POST", "/orders/0/resolve?sellerWins=invalid", 400);

                JsonNode deployment = request("POST", "/deploy", 200);
                String escrowAddress = deployment.get("contractAddress").asText();
                String deployBlock = deployment.get("deployBlock").asText();
                TransactionReceipt deployed = receipt(chain, deployment);
                record("HTTP deploy Escrow", deployed);
                assertEquals(deployed.getContractAddress(), escrowAddress);
                assertEquals(deployed.getBlockNumber().toString(), deployBlock);
                assertEquals(backend.getAddress(), deployed.getFrom());
                assertEquals(backend.getAddress(), request("GET", "/address", 200).get("arbiter").asText());
                assertEquals(0, request("GET", "/orders", 200).size());
                assertEquals(0, request("GET", "/orders/99/events", 200).size());
                request("GET", "/orders/" + BigInteger.ONE.shiftLeft(256) + "/events", 400);
                request("GET", "/orders/99", 409);

                Escrow buyerEscrow = Escrow.load(escrowAddress, chain, buyerTx, gas);
                Escrow sellerEscrow = Escrow.load(escrowAddress, chain, sellerTx, gas);
                BigInteger amount = UNIT.multiply(BigInteger.valueOf(30));
                BigInteger deadline = chain.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                        .send().getBlock().getTimestamp().add(BigInteger.valueOf(1800));
                record("createOrder 0", buyerEscrow.createOrder(seller.getAddress(), token.getContractAddress(),
                        amount, deadline, "HTTP normal order").send());
                rpc(transport, "hardhat_mine", List.of("0xb"));
                record("createOrder 1", buyerEscrow.createOrder(seller.getAddress(), token.getContractAddress(),
                        amount, deadline, "HTTP disputed order").send());
                record("approve 60 tokens", token.approve(escrowAddress, amount.multiply(BigInteger.TWO)).send());
                record("deposit 0", buyerEscrow.deposit(BigInteger.ZERO).send());
                record("deposit 1", buyerEscrow.deposit(BigInteger.ONE).send());
                record("seller dispute 1", sellerEscrow.dispute(BigInteger.ONE).send());
                JsonNode orders = request("GET", "/orders", 200);
                assertEquals(2, orders.size());
                assertEquals("0", orders.get(0).get("id").asText());
                assertEquals("1", orders.get(1).get("id").asText());
                assertTrue(new BigInteger(orders.get(1).get("blockNumber").asText()).subtract(
                        new BigInteger(orders.get(0).get("blockNumber").asText())).compareTo(BigInteger.TEN) > 0);
                JsonNode firstOrder = request("GET", "/orders/0", 200);
                assertEquals("Paid", firstOrder.get("status").asText());
                assertTrue(firstOrder.get("amount").isTextual());
                assertEquals(amount.toString(), firstOrder.get("amount").asText());
                assertEquals("Disputed", request("GET", "/orders/1", 200).get("status").asText());

                JsonNode failed = request("POST", "/orders/0/resolve?sellerWins=true", 409);
                TransactionReceipt failedReceipt = receipt(chain, failed);
                record("HTTP resolve 0 rejected: wrong status", failedReceipt);
                assertEquals("0x0", failed.get("status").asText());
                assertFalse(failedReceipt.isStatusOK());
                assertEquals(failedReceipt.getGasUsed().toString(), failed.get("gasUsed").asText());
                assertEquals(failedReceipt.getBlockNumber().toString(), failed.get("blockNumber").asText());
                assertTrue(failedReceipt.getGasUsed().signum() > 0);
                assertEquals(backend.getAddress(), failedReceipt.getFrom());
                assertEquals("Paid", request("GET", "/orders/0", 200).get("status").asText());

                JsonNode refunded = request("POST", "/orders/1/resolve?sellerWins=false", 200);
                TransactionReceipt refundedReceipt = receipt(chain, refunded);
                record("HTTP resolve 1 refund", refundedReceipt);
                assertEquals("0x1", refunded.get("status").asText());
                assertTrue(refundedReceipt.isStatusOK());
                assertEquals(backend.getAddress(), refundedReceipt.getFrom());
                assertEquals(BigInteger.ONE, Escrow.getOrderResolvedEvents(refundedReceipt).get(0).id);
                assertFalse(Escrow.getOrderResolvedEvents(refundedReceipt).get(0).sellerWins);
                var transfer = WQLToken.getTransferEvents(refundedReceipt).get(0);
                assertEquals(escrowAddress, transfer.from);
                assertEquals(buyer.getAddress(), transfer.to);
                assertEquals(amount, transfer.value);
                assertEquals("Refunded", request("GET", "/orders/1", 200).get("status").asText());
                record("buyer confirm 0", buyerEscrow.confirm(BigInteger.ZERO).send());
                assertEquals("Completed", request("GET", "/orders/0", 200).get("status").asText());
                assertEquals(UNIT.multiply(BigInteger.valueOf(70)), token.balanceOf(buyer.getAddress()).send());
                assertEquals(amount, token.balanceOf(seller.getAddress()).send());
                assertEquals(BigInteger.ZERO, token.balanceOf(escrowAddress).send());

                JsonNode normalEvents = request("GET", "/orders/0/events", 200);
                JsonNode disputedEvents = request("GET", "/orders/1/events", 200);
                assertEquals(List.of("OrderCreated", "OrderPaid", "OrderCompleted"), eventNames(normalEvents));
                assertEquals(List.of("OrderCreated", "OrderPaid", "OrderDisputed", "OrderResolved"), eventNames(disputedEvents));
                assertEquals("false", disputedEvents.get(3).get("sellerWins").asText());
                assertEquals(seller.getAddress(), disputedEvents.get(2).get("by").asText());
                assertEquals(refundedReceipt.getTransactionHash(), disputedEvents.get(3).get("transactionHash").asText());
                for (JsonNode timeline : List.of(normalEvents, disputedEvents)) {
                    BigInteger previous = BigInteger.ZERO;
                    for (JsonNode event : timeline) {
                        BigInteger block = new BigInteger(event.get("blockNumber").asText());
                        assertTrue(block.compareTo(previous) >= 0);
                        assertEquals(escrowAddress, event.get("contractAddress").asText());
                        previous = block;
                    }
                }

                // Clear the binding by deploying a second contract, then restore through HTTP /load.
                JsonNode second = request("POST", "/deploy", 200);
                record("HTTP deploy second Escrow for load test", receipt(chain, second));
                assertNotEquals(escrowAddress, second.get("contractAddress").asText());
                assertEquals(0, request("GET", "/orders", 200).size());
                assertEquals(0, request("GET", "/orders/0/events", 200).size());
                assertEquals(escrowAddress, request("POST", "/load?address=" + escrowAddress
                        + "&fromBlock=" + deployBlock, 200).get("contractAddress").asText());
                assertEquals(orders, request("GET", "/orders", 200));
                assertEquals(normalEvents, request("GET", "/orders/0/events", 200));
                assertEquals("Refunded", request("GET", "/orders/1", 200).get("status").asText());

                Path output = Path.of("build/local-evidence/http-integration.json");
                Files.createDirectories(output.getParent());
                mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), Map.of(
                        "classification", "LOCAL_SIMULATION_NOT_GROUP_ACCEPTANCE", "chainId", 31337,
                        "recordedAtUtc", Instant.now().toString(), "escrowAddress", escrowAddress,
                        "deployBlock", deployBlock, "transactions", transactions, "httpExchanges", exchanges,
                        "accounts", Map.of("backend", backend.getAddress(), "buyer", buyer.getAddress(), "seller", seller.getAddress()),
                        "finalState", Map.of("order0", "Completed", "order1", "Refunded", "buyerTokens", "70", "sellerTokens", "30", "escrowTokens", "0")));
                System.out.println("PASS: six real HTTP endpoints; parameter conversion; >10-block events; failed receipt/gas; refund; confirmation; load");
            }
        } finally {
            http.close();
            chain.shutdown();
        }
    }

    private JsonNode request(String method, String path, int expectedStatus) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + "/api/escrow" + path)).timeout(Duration.ofSeconds(60));
        HttpResponse<String> response = http.send(builder.method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
        JsonNode body = mapper.readTree(response.body());
        exchanges.add(Map.of("method", method, "path", "/api/escrow" + path, "status", response.statusCode(), "body", body));
        System.out.println(method + " /api/escrow" + path + " -> " + response.statusCode());
        assertEquals(expectedStatus, response.statusCode(), response.body());
        return body;
    }

    private List<String> eventNames(JsonNode timeline) {
        List<String> names = new ArrayList<>();
        timeline.forEach(event -> names.add(event.get("event").asText()));
        return names;
    }

    private TransactionReceipt receipt(Web3j chain, JsonNode response) throws Exception {
        return chain.ethGetTransactionReceipt(response.get("transactionHash").asText()).send()
                .getTransactionReceipt().orElseThrow();
    }

    private Credentials account(HttpService transport) throws Exception {
        Credentials credentials = Credentials.create(Keys.createEcKeyPair());
        rpc(transport, "hardhat_setBalance", List.of(credentials.getAddress(), "0x" + UNIT.multiply(BigInteger.TEN).toString(16)));
        return credentials;
    }

    @SuppressWarnings("rawtypes")
    private void rpc(HttpService transport, String method, List<String> params) throws Exception {
        Response response = new Request<>(method, params, transport, Response.class).send();
        assertFalse(response.hasError(), "Local chain setup RPC failed: " + method);
    }

    private void record(String operation, TransactionReceipt receipt) {
        transactions.add(Map.of("operation", operation, "hash", receipt.getTransactionHash(),
                "block", receipt.getBlockNumber().toString(), "from", receipt.getFrom(),
                "status", receipt.getStatus(), "gasUsed", receipt.getGasUsed().toString()));
    }
}
