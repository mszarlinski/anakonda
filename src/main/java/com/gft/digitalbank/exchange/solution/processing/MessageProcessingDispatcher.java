package com.gft.digitalbank.exchange.solution.processing;

import java.util.Map;
import java.util.Optional;

import com.gft.digitalbank.exchange.model.orders.MessageType;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class MessageProcessingDispatcher {

    private final Map<MessageType, MessageProcessor> messageProcessors;

    public MessageProcessingDispatcher(final ModificationProcessor modificationProcessor, final CancellationProcessor cancellationProcessor,
        final BuySellOrderProcessor buySellOrderProcessor) {

        messageProcessors = ImmutableMap.<MessageType, MessageProcessor>builder()
            .put(MessageType.ORDER, buySellOrderProcessor)
            .put(MessageType.MODIFICATION, modificationProcessor)
            .put(MessageType.CANCEL, cancellationProcessor)
            .build();
    }

    public void process(final JsonObject message) {
        final MessageType type = MessageType.valueOf(message.get("messageType").getAsString());

        final MessageProcessor messageProcessor = Optional.ofNullable(messageProcessors.get(type))
            .orElseThrow(() -> new IllegalArgumentException("Unsupported message passed to process: " + message));

        messageProcessor.process(message);
    }
}
