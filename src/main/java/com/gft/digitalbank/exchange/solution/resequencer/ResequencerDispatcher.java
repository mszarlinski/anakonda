package com.gft.digitalbank.exchange.solution.resequencer;

import com.gft.digitalbank.exchange.solution.processing.MessageProcessingDispatcher;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.toList;

/**
 * @author mszarlinski on 2016-07-08.
 */
public class ResequencerDispatcher {

    public static final int INITIAL_WINDOW_SIZE_MILLIS = 10;

    private final ConcurrentMap<String, Resequencer> productResequencers = new ConcurrentHashMap<>();

    //TODO: possible duplicate with ordersRegistry
    private final ConcurrentMap<Integer, String> orderIdToProduct = new ConcurrentHashMap<>();

    private final MessageProcessingDispatcher messageProcessingDispatcher;
//TODO: zakladajac, ze wiadomosci pochodzace od tego samego brokera przychodza w tej samej kolejnosci,
    // nigdy nie będzie sytuacji, zeby MOD przyszedl przed ORDER
//    private final ConcurrentMap<Integer, List<JsonObject>> unclassifiedMessages = new ConcurrentHashMap<>();

    public ResequencerDispatcher(final MessageProcessingDispatcher messageProcessingDispatcher) {
        this.messageProcessingDispatcher = messageProcessingDispatcher;
    }

    public void addOrderMessage(final JsonObject message) {
        final int orderId = message.get("id").getAsInt();
        final String product = message.get("product").getAsString();
        orderIdToProduct.put(orderId, product);
        addMessageToResequencer(message, product);
    }

    public void addAlteringMessage(final JsonObject message, final int orderId) {
        final String product = orderIdToProduct.get(orderId);
        addMessageToResequencer(message, product);
    }

    private void addMessageToResequencer(final JsonObject message, final String product) {

        Resequencer resequencer = productResequencers.get(product);
        if (resequencer == null) {
            resequencer = new Resequencer(messageProcessingDispatcher);
            resequencer.startWithWindowOf(INITIAL_WINDOW_SIZE_MILLIS);
            productResequencers.put(product, resequencer);
        }
        resequencer.addMessage(message);
    }

    public void awaitShutdown() {
        final List<CompletableFuture<Void>> futures = productResequencers.values()
                .stream()
                .map(r -> CompletableFuture.runAsync(r::awaitShutdown))
                .collect(toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();

        // TODO: handle Resequencers' failures - supplyAsync(r::awaitShutdown)
    }

}
