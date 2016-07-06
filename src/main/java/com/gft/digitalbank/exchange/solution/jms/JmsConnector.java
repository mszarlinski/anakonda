package com.gft.digitalbank.exchange.solution.jms;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gft.digitalbank.exchange.solution.Jndi;
import com.gft.digitalbank.exchange.solution.Spring;

/**
 * @author mszarlinski on 2016-06-28.
 */
public class JmsConnector {

    private static final Log log = LogFactory.getLog(JmsConnector.class);

    private final Jndi jndi;

    public JmsConnector(final Jndi jndi) {
        this.jndi = jndi;
    }

    public JmsContext connect(final List<String> queues, final CountDownLatch shutdownLatch) throws JMSException {
        final ActiveMQConnectionFactory connectionFactory = jndi.lookup("ConnectionFactory");

        final Connection connection = connectionFactory.createConnection();
        connection.start();

        queues.stream() //TODO: parallel
            .forEach(queue -> {
                final ExchangeMessageListener pt = Spring.getBean(ExchangeMessageListener.class);
                pt.start(queue, shutdownLatch, connection);

            });

        return JmsContext.builder()
            .connection(connection)
            .build();
    }

    public void shutdown(final JmsContext jmsContext) {
        try {
            jmsContext.getConnection().close();
        } catch (JMSException e) {
            e.printStackTrace(); //FIXME: @SneakyThrows
        }
    }
}
