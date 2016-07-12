package com.gft.digitalbank.exchange.solution.resequencer;

import com.gft.digitalbank.exchange.solution.error.AsyncErrorsKeeper;
import com.gft.digitalbank.exchange.solution.processing.MessageProcessingDispatcher;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.stream.Collectors.toList;

/**
 * @author mszarlinski on 2016-07-08.
 */
public class ResequencerDispatcher {

    private static final int WINDOW_SIZE_MILLIS = 10;

    private final ConcurrentMap<String, Resequencer> productResequencers = new ConcurrentHashMap<>();

    //TODO: possible duplicate with ordersRegistry
    private final ConcurrentMap<Integer, String> orderIdToProduct = new ConcurrentHashMap<>();

    private final MessageProcessingDispatcher messageProcessingDispatcher;

    private final AsyncErrorsKeeper asyncErrorsKeeper;

    private final ReentrantLock mutex = new ReentrantLock(true);

    ResequencerDispatcher(final MessageProcessingDispatcher messageProcessingDispatcher, final AsyncErrorsKeeper asyncErrorsKeeper) {
        this.messageProcessingDispatcher = messageProcessingDispatcher;
        this.asyncErrorsKeeper = asyncErrorsKeeper;
    }

    public void addOrderMessage(final JsonObject message) {
        final int orderId = message.get("id").getAsInt();
        final String product = message.get("product").getAsString();
        orderIdToProduct.put(orderId, product);
        addMessageToResequencer(message, product);
    }

    public void addAlteringMessage(final JsonObject message, final int orderId) {
        final String product = orderIdToProduct.get(orderId);
        if (product != null) {
            addMessageToResequencer(message, product);
        } else {
            asyncErrorsKeeper.logError("Unable to classify altering message to a product: " + message);
        }
    }

    private void addMessageToResequencer(final JsonObject message, final String product) {
        mutex.lock();
        Resequencer resequencer = productResequencers.get(product);
        if (resequencer == null) {
            resequencer = new Resequencer(messageProcessingDispatcher, asyncErrorsKeeper);
            resequencer.startWithWindowOf(WINDOW_SIZE_MILLIS);
            productResequencers.put(product, resequencer);
        }
        mutex.unlock();
        resequencer.addMessage(message);
    }

    public void awaitShutdown() {
        final List<CompletableFuture<Void>> futures = productResequencers.values()
                .stream()
                .map(r -> CompletableFuture.runAsync(r::awaitShutdown))
                .collect(toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
    }

}
