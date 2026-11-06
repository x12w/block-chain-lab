package com.wetech.demo.web3j.controller;

import com.wetech.demo.web3j.service.EscrowService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP-layer contract of the six endpoints: status codes, decimal-string amounts and the receipt
 * fields kept for a failed arbitration. A stub service keeps this a unit test (no chain needed).
 */
class EscrowControllerTest {
    private static final String BUYER = "0x" + "22".repeat(20);
    private static final String SELLER = "0x" + "33".repeat(20);
    private static final String TOKEN = "0x" + "44".repeat(20);

    private final EscrowController controller = new EscrowController(new StubService());

    @Test void unloadedContractReturnsConflict() {
        ResponseEntity<Object> response = controller.address().join();
        assertEquals(409, response.getStatusCode().value());
        assertEquals("Contract not deployed or loaded; call /deploy or /load first",
                body(response).get("message"));
    }

    @Test void orderDetailKeepsDecimalStringsAndStatusName() {
        ResponseEntity<Object> response = controller.order(BigInteger.ZERO).join();
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = body(response);
        assertEquals(BUYER, body.get("buyer"));
        assertEquals(SELLER, body.get("seller"));
        assertEquals(TOKEN, body.get("token"));
        assertEquals("30000000000000000000", body.get("amount"));
        assertEquals("2000000000", body.get("deadline"));
        assertEquals("Paid", body.get("status"));
        assertEquals("test order", body.get("description"));
    }

    @Test void missingOrderRevertReturnsConflict() {
        ResponseEntity<Object> response = controller.order(BigInteger.valueOf(99)).join();
        assertEquals(409, response.getStatusCode().value());
        assertTrue(((String) body(response).get("message")).contains("reverted"));
    }

    @Test void eventQueryFailureIsAnErrorNotAnEmptyArray() {
        ResponseEntity<Object> response = controller.orders();
        assertEquals(502, response.getStatusCode().value());
        assertTrue(((String) body(response).get("message")).contains("eth_getLogs"));
    }

    @Test void invalidLoadParameterReturnsBadRequest() {
        ResponseEntity<Object> response = controller.load("not-an-address", BigInteger.ZERO);
        assertEquals(400, response.getStatusCode().value());
        assertEquals("address must be a nonzero 20-byte hex address",
                body(response).get("message"));
    }

    @Test void failedArbitrationKeepsReceiptAndGasUsage() {
        ResponseEntity<Object> response = controller.resolve(BigInteger.ZERO, true).join();
        assertEquals(409, response.getStatusCode().value());
        Map<String, Object> body = body(response);
        assertTrue(((String) body.get("message")).contains("Transaction has failed"));
        assertEquals("0x" + "ab".repeat(32), body.get("transactionHash"));
        assertEquals("21", body.get("blockNumber"));
        assertEquals("0x0", body.get("status"));
        assertEquals("26216", body.get("gasUsed"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ResponseEntity<Object> response) {
        return (Map<String, Object>) response.getBody();
    }

    private static <T> CompletableFuture<T> failed(Throwable error) {
        return CompletableFuture.failedFuture(new CompletionException(error));
    }

    private static final class StubService extends EscrowService {
        StubService() {
            super(Web3j.build(new HttpService("http://127.0.0.1:1")),
                    Credentials.create("0x" + "11".repeat(32)),
                    new StaticGasProvider(BigInteger.ONE, BigInteger.ONE), 31337);
        }

        @Override
        public CompletableFuture<Map<String, String>> getAddress() {
            return failed(new IllegalStateException(
                    "Contract not deployed or loaded; call /deploy or /load first"));
        }

        @Override
        public CompletableFuture<Map<String, String>> getOrder(BigInteger id) {
            if (id.equals(BigInteger.valueOf(99))) {
                return failed(new IOException(
                        "Error sending request: execution reverted: Escrow: order does not exist"));
            }
            return CompletableFuture.completedFuture(Map.of(
                    "buyer", BUYER, "seller", SELLER, "token", TOKEN,
                    "amount", "30000000000000000000", "deadline", "2000000000",
                    "status", "Paid", "description", "test order"));
        }

        @Override
        public List<Map<String, String>> listOrders() throws IOException {
            throw new IOException("eth_getLogs 100..109: test RPC failure");
        }

        @Override
        public CompletableFuture<TransactionReceipt> resolve(BigInteger id, boolean sellerWins) {
            return failed(new TransactionException(
                    "Transaction has failed with status: 0x0. Gas used: 26216", failedReceipt()));
        }

        @Override
        public void load(String address, BigInteger fromBlock) {
            throw new IllegalArgumentException("address must be a nonzero 20-byte hex address");
        }
    }

    private static TransactionReceipt failedReceipt() {
        TransactionReceipt receipt = new TransactionReceipt();
        receipt.setTransactionHash("0x" + "ab".repeat(32));
        receipt.setBlockNumber("0x15");
        receipt.setStatus("0x0");
        receipt.setGasUsed("0x6668");
        return receipt;
    }
}
