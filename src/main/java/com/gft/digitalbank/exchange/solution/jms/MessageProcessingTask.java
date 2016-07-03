package com.gft.digitalbank.exchange.solution.jms;

import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author mszarlinski on 2016-06-30.
 */
public class MessageProcessingTask implements MessageListener {

    private static final Log log = LogFactory.getLog(MessageProcessingTask.class);

    private static final boolean NON_TRANSACTED = false;

    private CountDownLatch shutdownLatch;

    private final MessageDeserializer messageDeserializer;

    private final MessageProcessingDispatcher messageProcessingDispatcher;

    private Session session;

    private MessageConsumer messageConsumer;

    public MessageProcessingTask(final MessageDeserializer messageDeserializer, final MessageProcessingDispatcher messageProcessingDispatcher) {
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
            final JsonObject messageObj = messageDeserializer.deserialize((TextMessage) message);
            final String messageType = messageObj.get("messageType").getAsString();

            switch (messageType) {
                case "SHUTDOWN_NOTIFICATION":
                    processShutdownNotification();
                    break;
                case "ORDER":
                case "MODIFICATION":
                case "CANCEL":
                    messageProcessingDispatcher.process(messageObj);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown message type: " + messageType);
            }
        } else {
            throw new IllegalArgumentException("Unsupported message class: " + message.getClass().getName());
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
