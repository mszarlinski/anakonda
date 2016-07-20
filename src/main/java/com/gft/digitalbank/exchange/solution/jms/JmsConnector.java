package com.gft.digitalbank.exchange.solution.jms;

import com.gft.digitalbank.exchange.solution.Jndi;
import com.gft.digitalbank.exchange.solution.error.AsyncErrorsKeeper;
import com.gft.digitalbank.exchange.solution.resequencer.ResequencerDispatcher;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author mszarlinski on 2016-06-28.
 */
public class JmsConnector {

    private static final Log log = LogFactory.getLog(JmsConnector.class);

    private final Jndi jndi;

    private final AsyncErrorsKeeper asyncErrorsKeeper;

    public JmsConnector(final Jndi jndi, final AsyncErrorsKeeper asyncErrorsKeeper) {
        this.jndi = jndi;
        this.asyncErrorsKeeper = asyncErrorsKeeper;
    }

    public JmsContext connect(final List<String> queues, final CountDownLatch shutdownLatch, final ResequencerDispatcher resequencerDispatcher) throws JMSException {
        final ActiveMQConnectionFactory connectionFactory = jndi.lookup("ConnectionFactory");

        final Connection connection = connectionFactory.createConnection();
        connection.setExceptionListener(ex -> asyncErrorsKeeper.logError(ex.getMessage()));
        connection.start();

        queues.forEach(queue -> {
            final ExchangeMessageListener pt = new ExchangeMessageListener();
            pt.start(queue, shutdownLatch, connection, resequencerDispatcher, asyncErrorsKeeper);
        });

        return JmsContext.builder()
                .connection(connection)
                .build();
    }

    public void shutdown(final JmsContext jmsContext) {
        try {
            jmsContext.getConnection().close();
        } catch (JMSException e) {
            log.error("Error in shutdown", e);
        }
    }
}
