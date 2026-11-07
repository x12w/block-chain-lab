package com.wetech.demo.web3j.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetech.demo.web3j.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Real read-only HTTP acceptance. No testnet signing key is read or used. */
public final class PotosTimelineAcceptance {
    public static void main(String[] args) throws Exception {
        ObjectMapper json = new ObjectMapper();
        var app = new SpringApplication(Application.class);
        app.setRegisterShutdownHook(false); app.setLogStartupInfo(false);
        String temporaryKey = Numeric.toHexStringWithPrefixZeroPadded(Keys.createEcKeyPair().getPrivateKey(),64);
        app.addInitializers(context -> context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("potos-read-only-acceptance", Map.of(
                        "server.address", "127.0.0.1", "server.port", "0",
                        "web3j.client-address", "https://rpc-testnet.potos.hk", "web3j.chain-id", "60600",
                        "web3j.private-key", temporaryKey,
                        "web3j.gas-price", "1000000", "web3j.gas-limit", "6721975"))));
        List<Map<String,Object>> exchanges = new ArrayList<>();
        try (var context = app.run(); var http = HttpClient.newHttpClient()) {
            int port = ((ServletWebServerApplicationContext)context).getWebServer().getPort();
            String base = "http://127.0.0.1:"+port+"/api/escrow";
            for (String path : List.of("/load?address=0xd7320d80b687766bA73997B97dAE61Ff1f9ed737&fromBlock=2821876",
                    "/orders/0/events", "/orders/1/events")) {
                String method=path.startsWith("/load")?"POST":"GET";
                Instant start=Instant.now();
                var request=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofMinutes(10))
                        .method(method,HttpRequest.BodyPublishers.noBody()).build();
                var response=http.send(request,HttpResponse.BodyHandlers.ofString());
                Instant finish=Instant.now();
                JsonNode body=json.readTree(response.body());
                exchanges.add(Map.of("method",method,"url",base+path,"status",response.statusCode(),"startedAt",start.toString(),
                        "finishedAt",finish.toString(),"elapsedMillis",Duration.between(start,finish).toMillis(),
                        "headers",response.headers().map(),"rawBody",response.body(),"body",body));
                if(response.statusCode()!=200)throw new IllegalStateException("HTTP "+response.statusCode()+" for "+path+": "+response.body());
                if(method.equals("GET")) {
                    boolean normal=path.contains("/0/");
                    List<String> names=new ArrayList<>();List<Long> blocks=new ArrayList<>();
                    body.forEach(e->{names.add(e.get("event").asText());blocks.add(e.get("blockNumber").asLong());});
                    if(!names.equals(normal?List.of("OrderCreated","OrderPaid","OrderCompleted"):List.of("OrderCreated","OrderPaid","OrderDisputed","OrderResolved")))throw new IllegalStateException("Unexpected lifecycle "+names);
                    if(!blocks.equals(normal?List.of(2822499L,2822648L,2828331L):List.of(2822524L,2822676L,2823067L,2823583L)))throw new IllegalStateException("Unexpected event blocks "+blocks);
                }
                System.out.println(method+" "+path+" -> "+response.statusCode()+" in "+Duration.between(start,finish).toMillis()+" ms");
            }
        } finally {
            Path out=Path.of("evidence/bonus/timeline/http-acceptance.json");Files.createDirectories(out.getParent());
            json.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),Map.of("classification","REAL_POTOS_READ_ONLY_HTTP_ACCEPTANCE",
                    "chainId",60600,"signing","Fresh unfunded in-memory credentials only to construct Spring beans; no transaction was sent and no real testnet private key was read.","httpExchanges",exchanges));
        }
    }
}
