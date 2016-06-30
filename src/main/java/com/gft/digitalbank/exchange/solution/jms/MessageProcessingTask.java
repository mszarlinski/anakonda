package com.gft.digitalbank.exchange.solution.jms;

import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.model.SolutionResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author mszarlinski on 2016-06-30.
 */
public class MessageProcessingTask implements MessageListener {

    private static final Log log = LogFactory.getLog(MessageProcessingTask.class);

    private static final boolean NON_TRANSACTED = false;

    private final String queueName;

    private final Connection connection;

    private final CountDownLatch shutdownLatch;

    private final ProcessingListener processingListener;

    private final MessageDeserializer messageDeserializer;

    public MessageProcessingTask(final String queueName, final Connection connection, final CountDownLatch shutdownLatch, final ProcessingListener processingListener,
                                 final MessageDeserializer messageDeserializer) {
        this.queueName = queueName;
        this.connection = connection;
        this.shutdownLatch = shutdownLatch;
        this.processingListener = processingListener;
        this.messageDeserializer = messageDeserializer;
    }

    public Session start() {
        Session session = null;
        try {
            session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            final Queue queue = createQueue(queueName, session);
            final MessageConsumer consumer = session.createConsumer(queue);
            consumer.setMessageListener(this);
            log.warn("*** Task started");
            return session;
        } catch (JMSException e) {
            log.error(e.getMessage(), e);
            tryClose(session);
            return null;
        }


    }

    @Override
    public void onMessage(final Message message) {
        log.info("Message received: " + message);

        if (message instanceof TextMessage) {
            final Map<String, Object> messageObj = messageDeserializer.deserialize((TextMessage) message);

            if ("SHUTDOWN_NOTIFICATION" .equals(messageObj.get("messageType"))) {
                processingListener.processingDone(SolutionResult.builder()
                        .build());

                shutdownLatch.countDown();
            }
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
