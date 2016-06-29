package com.gft.digitalbank.exchange.solution.processing;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.model.SolutionResult;
import com.google.common.base.Throwables;

/**
 * @author mszarlinski on 2016-06-28.
 */
@Slf4j
public class OrderProcessor implements MessageListener {

    private final MessageDeserializer messageDeserializer;

    private ProcessingListener processingListener;

    private CountDownLatch shutdownLatch;

    OrderProcessor(final MessageDeserializer messageDeserializer) {
        this.messageDeserializer = messageDeserializer;
    }

    /**
     * TODO: this should happen in separate threads
     */
    @Override
    public void onMessage(final Message message) {
        log.info("Message received: {}", message);

        if (message instanceof TextMessage) {
            final Map<String, Object> messageObj = messageDeserializer.deserialize((TextMessage) message);

            if ("SHUTDOWN_NOTIFICATION".equals(messageObj.get("messageType"))) {
                processingListener.processingDone(SolutionResult.builder()
                    .build());

                shutdownLatch.countDown();
            }
        }
    }

    public void start(@NonNull final ProcessingListener processingListener, @NonNull final List<MessageConsumer> messageConsumers, @NonNull final CountDownLatch shutdownLatch) {
        this.processingListener = processingListener;
        this.shutdownLatch = shutdownLatch;

        messageConsumers.forEach(consumer -> {
            try {
                consumer.setMessageListener(this);
            } catch (JMSException e) {
                Throwables.propagate(e);
            }
        });
    }
}
