package com.wetech.demo.web3j.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetech.demo.web3j.Application;
import com.wetech.demo.web3j.contracts.escrow.Escrow;
import com.wetech.demo.web3j.contracts.escrowenhanced.EscrowEnhanced;
import com.wetech.demo.web3j.contracts.wqltoken.WQLToken;
import com.wetech.demo.web3j.service.EscrowService;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real EVM + real HTTP, random in-memory keys. Never reads testnet credentials. */
class EnhancedEscrowIntegrationTest {
    private static final BigInteger UNIT = BigInteger.TEN.pow(18);
    private final ObjectMapper json = new ObjectMapper();
    private final List<Map<String,Object>> txs = new ArrayList<>(), exchanges = new ArrayList<>();
    private final List<ConfigurableApplicationContext> apps = new ArrayList<>();
    private HttpService rpc;
    private Web3j chain;
    private HttpClient http;
    private Credentials first, second, buyer, seller, other;
    private final StaticGasProvider gas = new StaticGasProvider(BigInteger.valueOf(2_000_000_000L), BigInteger.valueOf(6_721_975));
    private WQLToken token;
    private EscrowEnhanced contract, buyerContract, sellerContract, secondContract, otherContract;
    private BigInteger deadline;

    @BeforeEach void setup() throws Exception {
        rpc = new HttpService("http://127.0.0.1:18545"); chain = Web3j.build(rpc);
        assertEquals(BigInteger.valueOf(31337), chain.ethChainId().send().getChainId());
        first=account(); second=account(); buyer=account(); seller=account(); other=account();
        http=HttpClient.newHttpClient();
        token=WQLToken.deploy(chain, tx(buyer), gas).send(); record("deploy local WQLToken",token.getTransactionReceipt().orElseThrow());
        record("mint 300 test tokens",token.mint(buyer.getAddress(),UNIT.multiply(BigInteger.valueOf(300))).send());
        contract=EscrowEnhanced.deploy(chain,tx(first),gas,first.getAddress(),second.getAddress()).send();
        record("deploy enhanced",contract.getTransactionReceipt().orElseThrow());
        buyerContract=as(buyer); sellerContract=as(seller); secondContract=as(second); otherContract=as(other);
        deadline=now().add(BigInteger.valueOf(120));
        record("approve 300",token.approve(contract.getContractAddress(),UNIT.multiply(BigInteger.valueOf(300))).send());
    }
    @AfterEach void close() { apps.forEach(ConfigurableApplicationContext::close); if(http!=null)http.close(); if(chain!=null)chain.shutdown(); }

    @Test void shippedOrdersCannotTimeoutButCanSettleAndUnshippedOrdersCanRefund() throws Exception {
        createPaid("shipped order"); createPaid("unshipped order"); createPaid("shipped dispute");
        String sellerBase=server(seller); load(sellerBase);
        String firstBase=server(first); load(firstBase);
        call(firstBase,"POST","/orders/0/mark-shipped",409);
        call(sellerBase,"POST","/orders/0/mark-shipped",200);
        call(sellerBase,"POST","/orders/2/mark-shipped",200);
        assertEquals("Shipped",call(firstBase,"GET","/orders/0",200).get("status").asText());
        failed("seller cannot confirm", ()->sellerContract.confirm(BigInteger.ZERO).send());
        failed("repeat shipment", ()->sellerContract.markShipped(BigInteger.ZERO).send());
        rpc("evm_setNextBlockTimestamp",List.of(deadline.add(BigInteger.ONE).toString())); rpc("evm_mine",List.of());
        failed("shipped timeout refund rejected",()->buyerContract.refund(BigInteger.ZERO).send());
        failed("late shipping rejected",()->sellerContract.markShipped(BigInteger.ONE).send());
        record("unshipped timeout refund succeeds",buyerContract.refund(BigInteger.ONE).send());
        record("shipped buyer confirmation succeeds",buyerContract.confirm(BigInteger.ZERO).send());
        record("shipped seller dispute succeeds",sellerContract.dispute(BigInteger.TWO).send());
        assertEquals("Completed",call(firstBase,"GET","/orders/0",200).get("status").asText());
        assertEquals("Refunded",call(firstBase,"GET","/orders/1",200).get("status").asText());
        assertEquals("Disputed",call(firstBase,"GET","/orders/2",200).get("status").asText());
        JsonNode timeline=call(firstBase,"GET","/orders/0/events",200);
        assertEquals(List.of("OrderCreated","OrderPaid","OrderShipped","OrderCompleted"),names(timeline));
        assertEquals(seller.getAddress(),timeline.get(2).get("seller").asText());
        assertEquals(UNIT.multiply(BigInteger.valueOf(30)),token.balanceOf(seller.getAddress()).send());
        save("shipping",Map.of("order0","Completed","order1","Refunded","order2","Disputed","sellerTokens","30"));
    }

    @Test void twoDistinctMatchingVotesSettleOnceAndConflictsCanBeRevised() throws Exception {
        createPaid("refund by quorum"); createPaid("seller by quorum");
        record("dispute 0",sellerContract.dispute(BigInteger.ZERO).send());
        record("dispute 1",buyerContract.dispute(BigInteger.ONE).send());
        String a=server(first), b=server(second); load(a);load(b);
        JsonNode arbiters=call(a,"GET","/arbitrators",200);
        assertEquals(first.getAddress(),arbiters.get("firstArbiter").asText());
        assertEquals(second.getAddress(),arbiters.get("secondArbiter").asText());
        failed("outsider vote rejected",()->otherContract.resolve(BigInteger.ZERO,false).send());
        BigInteger escrowBefore=token.balanceOf(contract.getContractAddress()).send();
        call(a,"POST","/orders/0/resolve?sellerWins=false",200);
        assertEquals("Disputed",call(a,"GET","/orders/0",200).get("status").asText());
        assertEquals(escrowBefore,token.balanceOf(contract.getContractAddress()).send());
        assertEquals("true",call(a,"GET","/orders/0/votes",200).get("firstVoted").asText());
        call(a,"POST","/orders/0/resolve?sellerWins=false",409);
        call(b,"POST","/orders/0/resolve?sellerWins=true",200);
        assertEquals("Disputed",call(a,"GET","/orders/0",200).get("status").asText());
        assertEquals(escrowBefore,token.balanceOf(contract.getContractAddress()).send());
        JsonNode disagreement=call(a,"GET","/orders/0/votes",200);
        assertEquals("false",disagreement.get("firstSellerWins").asText());
        assertEquals("true",disagreement.get("secondSellerWins").asText());
        call(b,"POST","/orders/0/resolve?sellerWins=false",200);
        assertEquals("Refunded",call(a,"GET","/orders/0",200).get("status").asText());
        call(a,"POST","/orders/0/resolve?sellerWins=false",409);
        call(b,"POST","/orders/1/resolve?sellerWins=true",200);
        assertEquals("Disputed",call(a,"GET","/orders/1",200).get("status").asText());
        call(a,"POST","/orders/1/resolve?sellerWins=true",200);
        assertEquals("Completed",call(a,"GET","/orders/1",200).get("status").asText());
        assertEquals(UNIT.multiply(BigInteger.valueOf(30)),token.balanceOf(seller.getAddress()).send());
        assertEquals(BigInteger.ZERO,token.balanceOf(contract.getContractAddress()).send());
        JsonNode events=call(a,"GET","/orders/0/events",200);
        assertEquals(List.of("OrderCreated","OrderPaid","OrderDisputed","ArbitrationVoted","ArbitrationVoted","ArbitrationVoted","OrderResolved"),names(events));
        assertEquals(first.getAddress(),events.get(3).get("voter").asText());
        assertEquals(second.getAddress(),events.get(5).get("voter").asText());
        assertEquals(events.get(5).get("transactionHash"),events.get(6).get("transactionHash"));
        assertTrue(new BigInteger(events.get(5).get("logIndex").asText()).compareTo(new BigInteger(events.get(6).get("logIndex").asText()))<0);
        save("multisig",Map.of("order0","Refunded","order1","Completed","buyerTokens","270","sellerTokens","30","escrowTokens","0"));
    }

    @Test void deadlineBoundaryAndInvalidDeploymentCannotChangeExistingBinding() throws Exception {
        failed("same arbiter constructor rejected",()->EscrowEnhanced.deploy(chain,tx(first),gas,first.getAddress(),first.getAddress()).send());
        failed("zero arbiter constructor rejected",()->EscrowEnhanced.deploy(chain,tx(first),gas,first.getAddress(),"0x"+"0".repeat(40)).send());
        failed("noncontract token rejected",()->buyerContract.createOrder(seller.getAddress(),other.getAddress(),UNIT,deadline,"invalid token").send());
        createPaid("boundary refund");
        rpc("evm_setNextBlockTimestamp",List.of(deadline.toString()));
        failed("refund at exact deadline rejected",()->buyerContract.refund(BigInteger.ZERO).send());
        record("refund after deadline succeeds",buyerContract.refund(BigInteger.ZERO).send());
        failed("nonexistent shipment",()->sellerContract.markShipped(BigInteger.valueOf(99)).send());
        String a=server(first);load(a);
        call(a,"POST","/deploy-enhanced?secondArbiter="+first.getAddress(),400);
        call(a,"POST","/deploy-enhanced?secondArbiter=bad",400);
        call(a,"GET","/orders/-1/votes",400);
        call(a,"POST","/orders/not-a-number/mark-shipped",400);
        Escrow legacy=Escrow.deploy(chain,tx(first),gas,first.getAddress()).send();
        record("deploy legacy compatibility",legacy.getTransactionReceipt().orElseThrow());
        call(a,"POST","/load-enhanced?address="+legacy.getContractAddress()+"&fromBlock=0",409);
        assertEquals(contract.getContractAddress(),call(a,"GET","/address",200).get("contractAddress").asText());
        JsonNode deployed=call(a,"POST","/deploy-enhanced?secondArbiter="+second.getAddress(),200);
        assertEquals("2",deployed.get("version").asText());
        assertEquals(second.getAddress(),call(a,"GET","/arbitrators",200).get("secondArbiter").asText());
        call(a,"POST","/load?address="+legacy.getContractAddress()+"&fromBlock=0",200);
        call(a,"GET","/arbitrators",409);
        call(a,"POST","/orders/0/mark-shipped",409);
        save("boundaries",Map.of("boundaryRefund","passed","invalidLoadPreservesBinding",true,"legacyCompatibility",true));
    }

    private BigInteger now() throws Exception {return chain.ethGetBlockByNumber(DefaultBlockParameterName.LATEST,false).send().getBlock().getTimestamp();}
    private void createPaid(String label) throws Exception {
        BigInteger id=contract.orderCount().send();
        record("create "+id+" "+label,buyerContract.createOrder(seller.getAddress(),token.getContractAddress(),UNIT.multiply(BigInteger.valueOf(30)),deadline,label).send());
        record("deposit "+id,buyerContract.deposit(id).send());
    }
    private String server(Credentials signer) {
        SpringApplication app=new SpringApplication(Application.class);app.setRegisterShutdownHook(false);app.setLogStartupInfo(false);
        app.addInitializers(c->c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("local-enhanced",Map.of(
                "server.address","127.0.0.1","server.port","0","web3j.client-address","http://127.0.0.1:18545","web3j.chain-id","31337",
                "web3j.private-key",Numeric.toHexStringWithPrefixZeroPadded(signer.getEcKeyPair().getPrivateKey(),64),"web3j.gas-price","2000000000","web3j.gas-limit","6721975"))));
        var c=app.run();apps.add(c);return "http://127.0.0.1:"+((ServletWebServerApplicationContext)c).getWebServer().getPort();
    }
    private void load(String base) throws Exception {call(base,"POST","/load-enhanced?address="+contract.getContractAddress()+"&fromBlock="+contract.getTransactionReceipt().orElseThrow().getBlockNumber(),200);}
    private JsonNode call(String base,String method,String path,int status) throws Exception {
        var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/escrow"+path)).timeout(Duration.ofSeconds(60)).method(method,HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString());
        JsonNode body=json.readTree(response.body());exchanges.add(Map.of("method",method,"path",path,"status",response.statusCode(),"body",body));
        assertEquals(status,response.statusCode(),response.body());
        if(body.has("transactionHash"))record("HTTP "+path,chain.ethGetTransactionReceipt(body.get("transactionHash").asText()).send().getTransactionReceipt().orElseThrow());
        return body;
    }
    private void failed(String label,Throwing action) throws Exception {
        TransactionException e=assertThrows(TransactionException.class,action::run,label);
        TransactionReceipt r=e.getTransactionReceipt().orElseThrow(); assertFalse(r.isStatusOK());record(label,r);
    }
    private interface Throwing {Object run() throws Exception;}
    private List<String> names(JsonNode events){List<String> a=new ArrayList<>();events.forEach(e->a.add(e.get("event").asText()));return a;}
    private RawTransactionManager tx(Credentials c){return new RawTransactionManager(chain,c,31337);}
    private EscrowEnhanced as(Credentials c){return EscrowEnhanced.load(contract.getContractAddress(),chain,tx(c),gas);}
    private Credentials account() throws Exception {var c=Credentials.create(Keys.createEcKeyPair());rpc("hardhat_setBalance",List.of(c.getAddress(),"0x"+UNIT.multiply(BigInteger.TEN).toString(16)));return c;}
    @SuppressWarnings("rawtypes") private void rpc(String method,List<String> params) throws Exception {Response r=new Request<>(method,params,rpc,Response.class).send();assertFalse(r.hasError(),method);}
    private void record(String operation,TransactionReceipt r){txs.add(Map.of("operation",operation,"hash",r.getTransactionHash(),"from",r.getFrom(),"block",r.getBlockNumber().toString(),"status",r.getStatus(),"gasUsed",r.getGasUsed().toString()));}
    private void save(String name,Map<String,Object> result) throws Exception {
        Path out=Path.of("build/local-evidence/enhanced-"+name+".json");Files.createDirectories(out.getParent());
        json.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),Map.of("classification","LOCAL_EVM_HTTP_ACCEPTANCE_NOT_POTOS","chainId",31337,"recordedAt",Instant.now().toString(),"accounts",Map.of("firstArbiter",first.getAddress(),"secondArbiter",second.getAddress(),"buyer",buyer.getAddress(),"seller",seller.getAddress()),"contract",contract.getContractAddress(),"transactions",txs,"httpExchanges",exchanges,"result",result));
        System.out.println("PASS enhanced "+name+" "+result+"; "+txs.size()+" receipts, "+exchanges.size()+" HTTP requests");
    }
}
