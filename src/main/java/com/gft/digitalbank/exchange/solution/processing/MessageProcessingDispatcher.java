package com.gft.digitalbank.exchange.solution.processing;

import com.gft.digitalbank.exchange.model.orders.MessageType;
import com.gft.digitalbank.exchange.solution.error.AsyncErrorsKeeper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class MessageProcessingDispatcher {

    private final Map<MessageType, MessageProcessor> messageProcessors;

    private final AsyncErrorsKeeper asyncErrorsKeeper;

    MessageProcessingDispatcher(final ModificationProcessor modificationProcessor, final CancellationProcessor cancellationProcessor,
        final BuySellOrderProcessor buySellOrderProcessor, final AsyncErrorsKeeper asyncErrorsKeeper) {

        this.asyncErrorsKeeper = asyncErrorsKeeper;

        messageProcessors = ImmutableMap.<MessageType, MessageProcessor>builder()
            .put(MessageType.ORDER, buySellOrderProcessor)
            .put(MessageType.MODIFICATION, modificationProcessor)
            .put(MessageType.CANCEL, cancellationProcessor)
            .build();
    }

    public void process(final JsonObject message) {
        final MessageType type = MessageType.valueOf(message.get("messageType").getAsString());

        final MessageProcessor messageProcessor = messageProcessors.get(type);
        if (messageProcessor != null) {
            messageProcessor.process(message);
        } else {
            asyncErrorsKeeper.logError("Unsupported message passed to process: " + message);
        }
    }
}
