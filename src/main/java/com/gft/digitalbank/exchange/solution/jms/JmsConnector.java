package com.gft.digitalbank.exchange.solution.jms;

import lombok.SneakyThrows;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.gft.digitalbank.exchange.solution.Jndi;
import com.gft.digitalbank.exchange.solution.resequencer.ResequencerDispatcher;

/**
 * @author mszarlinski on 2016-06-28.
 */
public class JmsConnector {

    private final Jndi jndi;

    public JmsConnector(final Jndi jndi) {
        this.jndi = jndi;
    }

    public JmsContext connect(final List<String> queues, final CountDownLatch shutdownLatch, final ResequencerDispatcher resequencerDispatcher) throws JMSException {
        final ActiveMQConnectionFactory connectionFactory = jndi.lookup("ConnectionFactory");

        final Connection connection = connectionFactory.createConnection();
        connection.start();

        queues.parallelStream()
            .forEach(queue -> {
                final ExchangeMessageListener pt = new ExchangeMessageListener();
                pt.start(queue, shutdownLatch, connection, resequencerDispatcher);
            });

        return JmsContext.builder()
            .connection(connection)
            .build();
    }

    @SneakyThrows
    public void shutdown(final JmsContext jmsContext) {
        jmsContext.getConnection().close();
    }
}
