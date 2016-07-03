package com.gft.digitalbank.exchange.solution.jms;

import com.gft.digitalbank.exchange.solution.processing.BuySellOrderProcessor;
import com.gft.digitalbank.exchange.solution.processing.CancellationProcessor;
import com.gft.digitalbank.exchange.solution.processing.ModificationProcessor;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class MessageProcessingDispatcher {

    private final ModificationProcessor modificationProcessor;

    private final CancellationProcessor cancellationProcessor;

    private final BuySellOrderProcessor buySellOrderProcessor;

    public MessageProcessingDispatcher(final ModificationProcessor modificationProcessor, final CancellationProcessor cancellationProcessor, final BuySellOrderProcessor
            buySellOrderProcessor) {
        this.modificationProcessor = modificationProcessor;
        this.cancellationProcessor = cancellationProcessor;
        this.buySellOrderProcessor = buySellOrderProcessor;
    }

    public void process(final JsonObject message) {
        final String type = message.get("messageType").getAsString();
        final String side = message.get("side").getAsString();
        if (isBuyOrder(side) || isSellOrder(side)) {
            buySellOrderProcessor.process(message);
        } else if (isModificationOrder(type)) {
            modificationProcessor.process(message);
        } else if (isCancelOrder(type)) {
            cancellationProcessor.process(message);
        } else {
            throw new IllegalArgumentException("Unsupported message passed to process: " + message);
        }
    }

    private boolean isCancelOrder(final String type) {
        return "CANCEL".equals(type);
    }

    private boolean isModificationOrder(final String type) {
        return "MODIFICATION".equals(type);
    }

    private boolean isSellOrder(final String side) {
        return "SELL".equals(side);
    }

    private boolean isBuyOrder(final String side) {
        return "BUY".equals(side);
    }
}
