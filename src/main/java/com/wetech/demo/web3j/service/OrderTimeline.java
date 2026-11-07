package com.wetech.demo.web3j.service;

import com.wetech.demo.web3j.contracts.escrow.Escrow;
import com.wetech.demo.web3j.contracts.escrowenhanced.EscrowEnhanced;
import org.web3j.abi.EventEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read every Escrow event for one order; each POTOS log query covers at most ten blocks. */
final class OrderTimeline {
    private static final BigInteger TEN = BigInteger.TEN;
    // POTOS rejects JSON-RPC arrays larger than eight, independently of its ten-block log limit.
    private static final int REQUESTS_PER_BATCH = 8;
    private static final Map<String, String> EVENTS = Map.of(
            EventEncoder.encode(Escrow.ORDERCREATED_EVENT), "OrderCreated",
            EventEncoder.encode(Escrow.ORDERPAID_EVENT), "OrderPaid",
            EventEncoder.encode(Escrow.ORDERCOMPLETED_EVENT), "OrderCompleted",
            EventEncoder.encode(Escrow.ORDERREFUNDED_EVENT), "OrderRefunded",
            EventEncoder.encode(Escrow.ORDERDISPUTED_EVENT), "OrderDisputed",
            EventEncoder.encode(Escrow.ORDERRESOLVED_EVENT), "OrderResolved",
            EventEncoder.encode(EscrowEnhanced.ORDERSHIPPED_EVENT), "OrderShipped",
            EventEncoder.encode(EscrowEnhanced.ARBITRATIONVOTED_EVENT), "ArbitrationVoted");
    private record Range(BigInteger from, BigInteger to) { }

    private OrderTimeline() { }

    static List<Map<String, String>> read(Web3j web3j, String address, BigInteger deployBlock,
                                         BigInteger id) throws IOException {
        if (id == null || id.signum() < 0 || id.bitLength() > 256) {
            throw new IllegalArgumentException("id must be a uint256");
        }
        var head = web3j.ethBlockNumber().send();
        if (head.hasError()) throw new IOException("eth_blockNumber: " + head.getError().getMessage());
        BigInteger latest = head.getBlockNumber();
        if (deployBlock.compareTo(latest) > 0) {
            throw new IllegalArgumentException("fromBlock is ahead of the chain head");
        }
        String idTopic = Numeric.toHexStringWithPrefixZeroPadded(id, 64);
        List<Log> logs = new ArrayList<>();
        BigInteger from = deployBlock;
        while (from.compareTo(latest) <= 0) {
            var batch = web3j.newBatch();
            Map<Long, Range> ranges = new HashMap<>();
            for (int n = 0; n < REQUESTS_PER_BATCH && from.compareTo(latest) <= 0; n++) {
                BigInteger to = from.add(TEN).subtract(BigInteger.ONE).min(latest);
                var filter = new EthFilter(DefaultBlockParameter.valueOf(from),
                        DefaultBlockParameter.valueOf(to), address);
                filter.addOptionalTopics(EVENTS.keySet().toArray(String[]::new));
                filter.addSingleTopic(idTopic);
                var request = web3j.ethGetLogs(filter);
                ranges.put(request.getId(), new Range(from, to));
                batch.add(request);
                from = to.add(BigInteger.ONE);
            }
            var responses = batch.send().getResponses();
            if (responses == null || responses.size() != ranges.size()) {
                throw new IOException("Incomplete event RPC batch; no partial timeline returned");
            }
            for (var response : responses) {
                Range range = ranges.remove(response.getId());
                if (range == null) throw new IOException("Unexpected or duplicate RPC response id");
                if (response.hasError()) throw new IOException("eth_getLogs " + range.from() + ".."
                        + range.to() + ": " + response.getError().getMessage());
                if (!(response instanceof EthLog result) || result.getLogs() == null) {
                    throw new IOException("eth_getLogs returned no result");
                }
                for (var entry : result.getLogs()) {
                    if (!(entry.get() instanceof Log log)) throw new IOException("Invalid event log");
                    if (log.isRemoved()) continue;
                    if (!address.equalsIgnoreCase(log.getAddress()) || log.getTopics() == null
                            || log.getTopics().size() < 2 || !EVENTS.containsKey(log.getTopics().get(0))
                            || !idTopic.equalsIgnoreCase(log.getTopics().get(1))) {
                        throw new IOException("RPC returned a log outside the requested order filter");
                    }
                    if (log.getBlockNumber() == null || log.getTransactionIndex() == null
                            || log.getLogIndex() == null || log.getTransactionHash() == null
                            || log.getBlockHash() == null || log.getBlockNumber().compareTo(range.from()) < 0
                            || log.getBlockNumber().compareTo(range.to()) > 0) {
                        throw new IOException("RPC returned invalid event location metadata");
                    }
                    logs.add(log);
                }
            }
        }
        logs.sort(Comparator.comparing(Log::getBlockNumber)
                .thenComparing(Log::getTransactionIndex).thenComparing(Log::getLogIndex));
        var seen = new HashSet<String>();
        List<Map<String, String>> events = new ArrayList<>();
        for (Log log : logs) {
            String identity = log.getBlockHash() + ":" + log.getTransactionHash() + ":" + log.getLogIndex();
            if (!seen.add(identity)) continue;
            try {
                events.add(decode(log, id));
            } catch (RuntimeException malformed) {
                throw new IOException("Unable to decode Escrow event; no partial timeline returned", malformed);
            }
        }
        return events;
    }

    private static Map<String, String> decode(Log log, BigInteger id) {
        String name = EVENTS.get(log.getTopics().get(0));
        Map<String, String> event = new LinkedHashMap<>();
        event.put("id", id.toString());
        event.put("event", name);
        event.put("contractAddress", log.getAddress());
        event.put("blockNumber", log.getBlockNumber().toString());
        event.put("blockHash", log.getBlockHash());
        event.put("transactionHash", log.getTransactionHash());
        event.put("transactionIndex", log.getTransactionIndex().toString());
        event.put("logIndex", log.getLogIndex().toString());
        switch (name) {
            case "OrderCreated" -> {
                var e = Escrow.getOrderCreatedEventFromLog(log);
                event.put("buyer", e.buyer); event.put("seller", e.seller); event.put("token", e.token);
                event.put("amount", e.amount.toString()); event.put("deadline", e.deadline.toString());
            }
            case "OrderDisputed" -> event.put("by", Escrow.getOrderDisputedEventFromLog(log).by);
            case "OrderResolved" -> event.put("sellerWins", Escrow.getOrderResolvedEventFromLog(log).sellerWins.toString());
            case "OrderShipped" -> event.put("seller", EscrowEnhanced.getOrderShippedEventFromLog(log).seller);
            case "ArbitrationVoted" -> {
                var e = EscrowEnhanced.getArbitrationVotedEventFromLog(log);
                event.put("voter", e.voter); event.put("sellerWins", e.sellerWins.toString());
            }
            case "OrderPaid" -> Escrow.getOrderPaidEventFromLog(log);
            case "OrderCompleted" -> Escrow.getOrderCompletedEventFromLog(log);
            case "OrderRefunded" -> Escrow.getOrderRefundedEventFromLog(log);
            default -> throw new IllegalArgumentException("Unsupported event");
        }
        return event;
    }
}
