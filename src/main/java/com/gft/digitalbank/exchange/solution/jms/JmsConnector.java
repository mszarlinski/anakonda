package com.gft.digitalbank.exchange.solution.jms;

import lombok.SneakyThrows;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;

/**
 * @author mszarlinski@bravurasolutions.com on 2016-06-28.
 */
public class JmsConnector {

    private static final boolean NON_TRANSACTED = false;

    private final Jndi jndi;

    public JmsConnector(final Jndi jndi) {
        this.jndi = jndi;
    }

    @SneakyThrows
    public JmsContext connect() {
        final ConnectionFactory connectionFactory = jndi.lookup("ConnectionFactory");
        //TODO: ActiveMQConnectionFactory.setDispatchAsync(true);

        final Connection connection = connectionFactory.createConnection();
        connection.start();
        final Session session = connection.createSession(NON_TRANSACTED, Session.AUTO_ACKNOWLEDGE);

        return JmsContext.builder()
            .connection(connection)
            .session(session)
            .build();
    }

    @SneakyThrows
    public void shutdown(final JmsContext jmsContext) {
        jmsContext.getConnection().close();
        jmsContext.getSession().close();
    }
}
