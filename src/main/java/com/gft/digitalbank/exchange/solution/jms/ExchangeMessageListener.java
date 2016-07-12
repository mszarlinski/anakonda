package com.gft.digitalbank.exchange.solution.jms;

import com.gft.digitalbank.exchange.model.orders.MessageType;
import com.gft.digitalbank.exchange.solution.error.AsyncErrorsKeeper;
import com.gft.digitalbank.exchange.solution.resequencer.ResequencerDispatcher;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author mszarlinski on 2016-06-30.
 */
public class ExchangeMessageListener implements MessageListener {

    private static final Log log = LogFactory.getLog(ExchangeMessageListener.class);

    private static final boolean NON_TRANSACTED = false;

    private CountDownLatch shutdownLatch;

    private final MessageDeserializer messageDeserializer;

    private Session session;

    private MessageConsumer messageConsumer;

    private ResequencerDispatcher resequencerDispatcher;
    private AsyncErrorsKeeper asyncErrorsKeeper;

    public ExchangeMessageListener() {
        this.messageDeserializer = new MessageDeserializer();
    }

    public void start(final String queueName, final CountDownLatch shutdownLatch, final Connection connection, final ResequencerDispatcher resequencerDispatcher,
                      final AsyncErrorsKeeper asyncErrorsKeeper) {
        log.debug("Starting task for queue: " + queueName);

        this.shutdownLatch = shutdownLatch;
        this.resequencerDispatcher = resequencerDispatcher;
        this.asyncErrorsKeeper = asyncErrorsKeeper;

        try {
            session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);
            final Queue queue = createQueue(queueName, session);

            messageConsumer = session.createConsumer(queue);
            messageConsumer.setMessageListener(this);

            log.debug("Task started");
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

                switch (messageType) {
                    case ORDER:
                        resequencerDispatcher.addOrderMessage(messageObj);
                        break;
                    case MODIFICATION:
                        resequencerDispatcher.addAlteringMessage(messageObj, messageObj.get("modifiedOrderId").getAsInt());
                        break;
                    case CANCEL:
                        resequencerDispatcher.addAlteringMessage(messageObj, messageObj.get("cancelledOrderId").getAsInt());
                        break;
                    case SHUTDOWN_NOTIFICATION:
                        shutdown();
                        break;
                    default:
                        log.error("Unsupported message type: " + messageType);
                }
            } catch (Exception ex) {
                // TODO: czy jest sens tutaj logowaæ?
                log.error("Error while processing message", ex);
                asyncErrorsKeeper.logError(ex.getMessage());
            }
        } else {
            log.error("Unable to process message of class: " + message.getClass().getName());
        }
    }

    public void shutdown() {
        try {
            messageConsumer.close();
            session.close();
            shutdownLatch.countDown();
        } catch (JMSException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private Queue createQueue(final String name, final Session session) throws JMSException {
        return session.createQueue(name);
    }
}
