package com.gft.digitalbank.exchange.solution.jms;

import java.util.Map;
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
        log.info("Message received: " + message);

        if (message instanceof TextMessage) {
            final Map<String, Object> messageObj = messageDeserializer.deserialize((TextMessage) message);
            final String messageType = (String) messageObj.get("messageType");

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
