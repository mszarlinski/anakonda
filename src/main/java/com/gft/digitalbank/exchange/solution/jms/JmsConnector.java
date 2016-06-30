package com.gft.digitalbank.exchange.solution.jms;

import lombok.SneakyThrows;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.springframework.context.ApplicationContext;

import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.solution.Jndi;

/**
 * @author mszarlinski@bravurasolutions.com on 2016-06-28.
 */
public class JmsConnector {

    private final Jndi jndi;

    private final MessageDeserializer messageDeserializer; // TODO: inject into Processing Thread

    public JmsConnector(final Jndi jndi, final MessageDeserializer messageDeserializer) {
        this.jndi = jndi;
        this.messageDeserializer = messageDeserializer;
    }

    @SneakyThrows
    public JmsContext connect(final List<String> queues, final CountDownLatch shutdownLatch, final ExecutorService executorService, final ProcessingListener processingListener) {
        final ConnectionFactory connectionFactory = jndi.lookup("ConnectionFactory");
        //TODO: ActiveMQConnectionFactory.setDispatchAsync(true);

        final Connection connection = connectionFactory.createConnection();
        connection.start();

        queues.forEach(queue ->
//            executorService.submit(new ProcessingThread(queue, connection, shutdownLatch)));

            new ProcessingThread(queue, connection, shutdownLatch, processingListener, messageDeserializer).run()); //TODO: Spring prototype

        return JmsContext.builder()
            .connection(connection)
            .build();
    }

    @SneakyThrows
    public void shutdown(final JmsContext jmsContext) {
        jmsContext.getConnection().close();
    }
}
