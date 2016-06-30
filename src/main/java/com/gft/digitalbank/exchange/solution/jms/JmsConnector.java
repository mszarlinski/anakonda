package com.gft.digitalbank.exchange.solution.jms;

import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.solution.Jndi;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author mszarlinski@bravurasolutions.com on 2016-06-28.
 */
public class JmsConnector {

    private static final Log log = LogFactory.getLog(JmsConnector.class);

    private final Jndi jndi;

    private final MessageDeserializer messageDeserializer; // TODO: inject directly into Processing Thread

    public JmsConnector(final Jndi jndi, final MessageDeserializer messageDeserializer) {
        this.jndi = jndi;
        this.messageDeserializer = messageDeserializer;
    }

    public JmsContext connect(final List<String> queues, final CountDownLatch shutdownLatch, final ExecutorService executorService, final ProcessingListener processingListener) throws JMSException {
        final ActiveMQConnectionFactory connectionFactory = jndi.lookup("ConnectionFactory");
//        connectionFactory.setMaxThreadPoolSize(queues.size());
//        connectionFactory.setAlwaysSessionAsync(true);

        connectionFactory.setExceptionListener(e -> log.error(e.getMessage(), e));

        log.warn("*** Connection factory ready");

        final Connection connection = connectionFactory.createConnection();
        connection.setExceptionListener(e -> log.error(e.getMessage(), e));
        connection.start();

        log.warn("*** Connection started");

        final List<Session> sessions = queues.stream()
                .map(queue -> {
                    //TODO: Spring prototype
                    final MessageProcessingTask pt = new MessageProcessingTask(queue, connection, shutdownLatch, processingListener, messageDeserializer);
                    return pt.start();
                })
                .filter(session -> session != null) //TODO: Optional?
                .collect(Collectors.toList());

        return JmsContext.builder()
                .connection(connection)
                .sessions(sessions)
                .build();
    }

    public void shutdown(final JmsContext jmsContext) {
        try {
            jmsContext.getConnection().close();
            jmsContext.getSessions().forEach(this::closeSession);
        } catch (JMSException e) {
            e.printStackTrace(); //FIXME: @SneakyThrows
        }
    }

    private void closeSession(final Session session) {
        try {
            session.close();
        } catch (JMSException e) {
            e.printStackTrace(); //FIXME
        }
    }
}
