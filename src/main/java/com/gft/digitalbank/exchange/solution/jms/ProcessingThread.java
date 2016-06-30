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

import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.model.SolutionResult;

/**
 * @author mszarlinski on 2016-06-30.
 */
public class ProcessingThread implements Runnable, MessageListener {

    private static final Log log = LogFactory.getLog(ProcessingThread.class);

    private static final boolean NON_TRANSACTED = false;

    private final String queueName;

    private final Connection connection;

    private final CountDownLatch shutdownLatch;

    private final ProcessingListener processingListener;

    private final MessageDeserializer messageDeserializer;

    private Session session;

    public ProcessingThread(final String queueName, final Connection connection, final CountDownLatch shutdownLatch, final ProcessingListener processingListener,
        final MessageDeserializer messageDeserializer) {
        this.queueName = queueName;
        this.connection = connection;
        this.shutdownLatch = shutdownLatch;
        this.processingListener = processingListener;
        this.messageDeserializer = messageDeserializer;
    }

    @Override
    public void onMessage(final Message message) {
        log.info("Message received: " + message);

        if (message instanceof TextMessage) {
            final Map<String, Object> messageObj = messageDeserializer.deserialize((TextMessage) message);

            if ("SHUTDOWN_NOTIFICATION".equals(messageObj.get("messageType"))) {
                processingListener.processingDone(SolutionResult.builder()
                    .build());

                shutdown();
            }
        }
    }

    private void shutdown() {
        try {
            session.close();
        } catch (JMSException e) {
            log.error(e.getMessage(), e);
        } finally {
            shutdownLatch.countDown();
        }
    }

    @Override
    public void run() {
        try {
            session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            final Queue queue = createQueue(queueName, session);
            final MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            log.error(e.getMessage(), e);
            tryClose(session);
        }
    }

    private void tryClose(final Session session) {
        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private Queue createQueue(final String name, final Session session) throws JMSException {
        return session.createQueue(name);
    }
}
