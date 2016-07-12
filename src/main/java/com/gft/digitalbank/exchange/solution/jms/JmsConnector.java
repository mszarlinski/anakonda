package com.gft.digitalbank.exchange.solution.jms;

import com.gft.digitalbank.exchange.solution.Jndi;
import com.gft.digitalbank.exchange.solution.error.ErrorsLog;
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

    private final ErrorsLog errorsLog;

    public JmsConnector(final Jndi jndi, final ErrorsLog errorsLog) {
        this.jndi = jndi;
        this.errorsLog = errorsLog;
    }

    public JmsContext connect(final List<String> queues, final CountDownLatch shutdownLatch, final ResequencerDispatcher resequencerDispatcher) throws JMSException {
        final ActiveMQConnectionFactory connectionFactory = jndi.lookup("ConnectionFactory");

        final Connection connection = connectionFactory.createConnection();
        connection.setExceptionListener(ex -> errorsLog.logException(ex.getMessage()));
        connection.start();

        //TODO: parallel??
        queues.stream()
                .forEach(queue -> {
                    final ExchangeMessageListener pt = new ExchangeMessageListener();
                    pt.start(queue, shutdownLatch, connection, resequencerDispatcher, errorsLog);
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
