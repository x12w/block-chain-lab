package com.wetech.demo.web3j.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.wetech.demo.web3j.contracts.escrow.Escrow;
import org.junit.jupiter.api.*;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OrderTimelineTest {
    private static final String ADDRESS = "0x" + "11".repeat(20);
    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> filters = new ArrayList<>();
    private final List<Integer> batchSizes = new ArrayList<>();
    private HttpServer server;
    private Web3j web3j;
    private String mode = "ok";

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            JsonNode request = json.readTree(exchange.getRequestBody());
            Object result;
            if (request.isArray()) {
                batchSizes.add(request.size());
                List<Object> replies = new ArrayList<>();
                for (JsonNode item : request) {
                    JsonNode filter = item.get("params").get(0); filters.add(filter);
                    int start = Integer.parseInt(filter.get("fromBlock").asText().substring(2),16);
                    if (mode.equals("failure") && start == 300) {
                        replies.add(Map.of("jsonrpc","2.0","id",item.get("id"),"error",Map.of("code",-32000,"message","batch failed")));
                    } else {
                        List<Object> logs = new ArrayList<>();
                        if (!mode.equals("empty") && start <= 150) {
                            logs.add(event(start, mode.equals("wrongOrder") ? 8 : 7));
                            if (start == 110) logs.add(event(start, 7)); // duplicate delivery
                        }
                        replies.add(Map.of("jsonrpc","2.0","id",item.get("id"),"result",logs));
                    }
                }
                Collections.reverse(replies); // RPC batches need not preserve request order
                if (mode.equals("missing")) replies.remove(0);
                result = replies;
            } else {
                result = Map.of("jsonrpc","2.0","id",request.get("id"),"result","0x142"); // 322
            }
            byte[] bytes = json.writeValueAsBytes(result);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.start();web3j=Web3j.build(new HttpService("http://127.0.0.1:"+server.getAddress().getPort()));
    }
    @AfterEach void stop(){web3j.shutdown();server.stop(0);}
    private List<Map<String,String>> read() throws IOException {
        return OrderTimeline.read(web3j,ADDRESS,BigInteger.valueOf(100),BigInteger.valueOf(7));
    }
    @Test void allSixEventsDecodedSortedAndDeduplicatedAcrossMultipleRpcBatches() throws Exception {
        var events=read();
        assertEquals(List.of("OrderCreated","OrderPaid","OrderDisputed","OrderResolved","OrderCompleted","OrderRefunded"),events.stream().map(e->e.get("event")).toList());
        assertEquals("30000000000000000000",events.get(0).get("amount"));
        assertEquals(ADDRESS,events.get(2).get("by"));assertEquals("false",events.get(3).get("sellerWins"));
        assertEquals(List.of(8,8,7),batchSizes);
        for(int i=0;i<23;i++) {
            JsonNode f=filters.get(i);
            assertEquals("0x"+Integer.toHexString(100+i*10),f.get("fromBlock").asText());
            assertEquals("0x"+Integer.toHexString(Math.min(109+i*10,322)),f.get("toBlock").asText());
            assertEquals(8,f.get("topics").get(0).size());
            assertEquals("0x"+TypeEncoder.encode(new Uint256(7)),f.get("topics").get(1).asText());
            assertTrue(f.get("address").toString().contains(ADDRESS));
        }
    }
    @Test void laterBatchErrorDoesNotReturnEarlierPartialEvents(){mode="failure";assertThrows(IOException.class,this::read);}
    @Test void incompleteBatchIsRejected(){mode="missing";assertThrows(IOException.class,this::read);}
    @Test void rpcCannotMixAnotherOrdersEvents(){mode="wrongOrder";assertThrows(IOException.class,this::read);}
    @Test void malformedEventIsAnError(){mode="malformed";assertThrows(IOException.class,this::read);}
    @Test void noMatchingEventsReturnsEmptyList() throws Exception {mode="empty";assertEquals(List.of(),read());}
    @Test void negativeOrOversizedIdRejectedBeforeNetwork(){
        assertThrows(IllegalArgumentException.class,()->OrderTimeline.read(web3j,ADDRESS,BigInteger.ZERO,BigInteger.valueOf(-1)));
        assertThrows(IllegalArgumentException.class,()->OrderTimeline.read(web3j,ADDRESS,BigInteger.ZERO,BigInteger.ONE.shiftLeft(256)));
        assertTrue(filters.isEmpty());
    }
    private Map<String,Object> event(int block,int id) {
        var event=switch(block){case 100->Escrow.ORDERCREATED_EVENT;case 110->Escrow.ORDERPAID_EVENT;case 120->Escrow.ORDERDISPUTED_EVENT;case 130->Escrow.ORDERRESOLVED_EVENT;case 140->Escrow.ORDERCOMPLETED_EVENT;default->Escrow.ORDERREFUNDED_EVENT;};
        List<String> topics=new ArrayList<>(List.of(EventEncoder.encode(event),"0x"+TypeEncoder.encode(new Uint256(id))));
        String data="0x";
        if(block==100){topics.add("0x"+TypeEncoder.encode(new Address(ADDRESS)));topics.add("0x"+TypeEncoder.encode(new Address(ADDRESS)));data+=""+TypeEncoder.encode(new Address(ADDRESS))+TypeEncoder.encode(new Uint256(new BigInteger("30000000000000000000")))+TypeEncoder.encode(new Uint256(2_000_000_000));}
        if(block==120)data+=TypeEncoder.encode(new Address(ADDRESS));
        if(block==130)data+=TypeEncoder.encode(new Bool(false));
        if(mode.equals("malformed")&&block==120)data="0x";
        Map<String,Object> log=new HashMap<>();
        log.put("address",ADDRESS);log.put("blockNumber","0x"+Integer.toHexString(block));log.put("blockHash","0x"+String.format("%064x",block));
        log.put("transactionHash","0x"+String.format("%064x",block+1));log.put("transactionIndex","0x0");log.put("logIndex","0x0");log.put("removed",false);log.put("topics",topics);log.put("data",data);return log;
    }
}
