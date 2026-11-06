package com.wetech.demo.web3j.controller;

import com.wetech.demo.web3j.service.EscrowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.exceptions.ContractCallException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Member 2: HTTP entry points for the Escrow Service.
 *
 * <p>Only the transport/validation/error mapping lives here; deployment, event scanning,
 * status decoding and on-chain role/state checks stay in {@link EscrowService} and the contract.
 */
@RestController
@RequestMapping("/api/escrow")
public class EscrowController {

    private final EscrowService escrowService;

    public EscrowController(EscrowService escrowService) {
        this.escrowService = escrowService;
    }

    /** Deploy a new Escrow; the backend account becomes arbiter. */
    @PostMapping("/deploy")
    public CompletableFuture<ResponseEntity<Object>> deploy() {
        return escrowService.deploy().handle((contract, error) -> {
            if (error != null) {
                return errorResponse(error);
            }
            // Read address and receipt from the contract returned by this deploy call, not the
            // mutable global binding, which a concurrent /load could replace.
            Optional<TransactionReceipt> receipt = contract.getTransactionReceipt();
            if (receipt.isEmpty()) {
                return error(HttpStatus.BAD_GATEWAY, "Deployment receipt is unavailable", null);
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contractAddress", contract.getContractAddress());
            body.put("transactionHash", receipt.get().getTransactionHash());
            body.put("deployBlock", receipt.get().getBlockNumber().toString());
            return ResponseEntity.ok(body);
        });
    }

    /** Restore an existing contract after a restart; fromBlock must be the real deployment block. */
    @PostMapping("/load")
    public ResponseEntity<Object> load(@RequestParam String address,
                                       @RequestParam BigInteger fromBlock) {
        try {
            escrowService.load(address, fromBlock);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contractAddress", address);
            return ResponseEntity.ok(body);
        } catch (IOException | RuntimeException error) {
            return errorResponse(error);
        }
    }

    /** Current contract address and its on-chain arbiter. */
    @GetMapping("/address")
    public CompletableFuture<ResponseEntity<Object>> address() {
        try {
            return escrowService.getAddress().handle(this::respond);
        } catch (RuntimeException error) {
            // e.g. no contract deployed/loaded yet
            return CompletableFuture.completedFuture(errorResponse(error));
        }
    }

    /** Single order detail, including the human-readable status. */
    @GetMapping("/orders/{id}")
    public CompletableFuture<ResponseEntity<Object>> order(@PathVariable BigInteger id) {
        try {
            return escrowService.getOrder(id).handle(this::respond);
        } catch (RuntimeException error) {
            return CompletableFuture.completedFuture(errorResponse(error));
        }
    }

    /** Orders discovered from OrderCreated events, ordered by block. Never a partial list. */
    @GetMapping("/orders")
    public ResponseEntity<Object> orders() {
        try {
            List<Map<String, String>> orders = escrowService.listOrders();
            return ResponseEntity.ok(orders);
        } catch (IOException | RuntimeException error) {
            return errorResponse(error);
        }
    }

    /**
     * Arbiter resolution. The contract checks arbiter, Disputed status and token transfer; a failed
     * transaction still carries a receipt with status 0x0 and gasUsed, which is returned as is.
     */
    @PostMapping("/orders/{id}/resolve")
    public CompletableFuture<ResponseEntity<Object>> resolve(@PathVariable BigInteger id,
                                                             @RequestParam boolean sellerWins) {
        try {
            return escrowService.resolve(id, sellerWins).handle((receipt, error) -> {
                if (error != null) {
                    return errorResponse(error);
                }
                return ResponseEntity.ok(receiptBody(receipt));
            });
        } catch (RuntimeException error) {
            return CompletableFuture.completedFuture(errorResponse(error));
        }
    }

    /** Spring converts query/path values before the handler runs; report those as parameter errors. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentNotValidException.class})
    public ResponseEntity<Object> invalidParameter(Exception error) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request parameter: " + error.getMessage(), null);
    }

    private ResponseEntity<Object> respond(Map<String, String> body, Throwable error) {
        if (error != null) {
            return errorResponse(error);
        }
        return ResponseEntity.ok(new LinkedHashMap<String, Object>(body));
    }

    private Map<String, Object> receiptBody(TransactionReceipt receipt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transactionHash", receipt.getTransactionHash());
        body.put("blockNumber", receipt.getBlockNumber() == null
                ? null : receipt.getBlockNumber().toString());
        body.put("status", receipt.getStatus());
        return body;
    }

    /**
     * Map service/chain failures to the four statuses agreed in the integration doc. Details stay
     * limited to the message and, when a failed transaction reached the chain, its receipt.
     */
    private ResponseEntity<Object> errorResponse(Throwable error) {
        Throwable cause = unwrap(error);
        HttpStatus status;
        String message;
        if (cause instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            message = cause.getMessage();
        } else if (cause instanceof TransactionException) {
            // The receipt (if any) is attached below; a timeout may have none.
            status = HttpStatus.CONFLICT;
            message = cause.getMessage();
        } else if (cause instanceof ContractCallException && isRevert(cause.getMessage())) {
            status = HttpStatus.CONFLICT;
            message = "On-chain call reverted: " + cause.getMessage();
        } else if (cause instanceof IOException) {
            if (isRevert(cause.getMessage())) {
                status = HttpStatus.CONFLICT;
                message = "On-chain call reverted: " + cause.getMessage();
            } else {
                status = HttpStatus.BAD_GATEWAY;
                message = "RPC request failed: " + cause.getMessage();
            }
        } else if (cause instanceof IllegalStateException) {
            if (isChainMismatch(cause.getMessage())) {
                status = HttpStatus.BAD_GATEWAY;
            } else {
                // "not deployed or loaded" and unsupported status values are client/state conflicts.
                status = HttpStatus.CONFLICT;
            }
            message = cause.getMessage();
        } else {
            status = HttpStatus.BAD_GATEWAY;
            message = "Escrow request failed: " + safeMessage(cause);
        }
        return error(status, message, findReceipt(error));
    }

    private ResponseEntity<Object> error(HttpStatus status, String message, TransactionReceipt receipt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message == null || message.isBlank()
                ? status.getReasonPhrase() : message);
        if (receipt != null) {
            body.put("transactionHash", receipt.getTransactionHash());
            body.put("blockNumber", receipt.getBlockNumber() == null
                    ? null : receipt.getBlockNumber().toString());
            body.put("status", receipt.getStatus());
            body.put("gasUsed", receipt.getGasUsed() == null
                    ? null : receipt.getGasUsed().toString());
        }
        return ResponseEntity.status(status).body(body);
    }

    /** CompletionException/ExecutionException wrappers hide the actual cause of async failures. */
    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /** Walk the whole cause chain: TransactionException may be wrapped more than once. */
    private static TransactionReceipt findReceipt(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof TransactionException tx
                    && tx.getTransactionReceipt().isPresent()) {
                return tx.getTransactionReceipt().get();
            }
        }
        return null;
    }

    private static boolean isRevert(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("revert") || lower.contains("execution reverted");
    }

    private static boolean isChainMismatch(String message) {
        return message != null && message.contains("chain ID differs");
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null ? error.getClass().getSimpleName() : message;
    }
}
