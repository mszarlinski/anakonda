package com.gft.digitalbank.exchange.solution.jms;

import java.util.concurrent.CountDownLatch;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gft.digitalbank.exchange.model.orders.MessageType;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-06-30.
 */
public class ExchangeMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(ExchangeMessageListener.class);

    private static final boolean NON_TRANSACTED = false;

    private CountDownLatch shutdownLatch;

    private final MessageDeserializer messageDeserializer;

    private final MessageProcessingDispatcher messageProcessingDispatcher;

    private Session session;

    private MessageConsumer messageConsumer;

    public ExchangeMessageListener(final MessageDeserializer messageDeserializer, final MessageProcessingDispatcher messageProcessingDispatcher) {
        this.messageDeserializer = messageDeserializer;
        this.messageProcessingDispatcher = messageProcessingDispatcher;
    }

    public void start(final String queueName, final CountDownLatch shutdownLatch, final Connection connection) {
        log.info("Starting task for queue: " + queueName);

        this.shutdownLatch = shutdownLatch;

        try {
            session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            final Queue queue = createQueue(queueName, session);

            messageConsumer = session.createConsumer(queue);
            messageConsumer.setMessageListener(this);

            log.info("Task started");
        } catch (JMSException e) {
            log.error(e.getMessage(), e);
            shutdown();
        }
    }

    @Override
    public void onMessage(final Message message) {
        if (message instanceof TextMessage) {
            try {
                final JsonObject messageObj = messageDeserializer.deserialize((TextMessage) message);
                final MessageType messageType = MessageType.valueOf(messageObj.get("messageType").getAsString());

                if (messageType == MessageType.SHUTDOWN_NOTIFICATION) {
                    processShutdownNotification();
                } else {
                    messageProcessingDispatcher.process(messageObj);
                }
            } catch (Exception ex) {
                log.error("Error while processing message", ex);
            }
        } else {
            log.error("Unable to process message of class: " + message.getClass().getName());
        }
    }

    private void processShutdownNotification() {
        shutdownLatch.countDown();
        shutdown();
    }

    private void shutdown() {
        try {
            messageConsumer.close();
            session.close();
        } catch (JMSException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private Queue createQueue(final String name, final Session session) throws JMSException {
        return session.createQueue(name);
    }
}
