package com.gft.digitalbank.exchange.solution;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;

import com.gft.digitalbank.exchange.Exchange;
import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.solution.jms.JmsConfiguration;
import com.gft.digitalbank.exchange.solution.jms.JmsConnector;
import com.gft.digitalbank.exchange.solution.jms.JmsContext;
import com.gft.digitalbank.exchange.solution.jms.MessageConsumersCreator;
import com.gft.digitalbank.exchange.solution.jms.QueueConnector;
import com.gft.digitalbank.exchange.solution.processing.OrderProcessor;
import com.gft.digitalbank.exchange.solution.processing.ProcessingConfiguration;
import com.google.common.annotations.VisibleForTesting;

/**
 * Your solution must implement the {@link Exchange} interface.
 */
@Slf4j
public class StockExchange implements Exchange {

    private ProcessingListener processingListener;

    private List<String> destinations = new ArrayList<>();

    // BEANS

    private OrderProcessor orderProcessor;

    private QueueConnector queueConnector;

    private JmsConnector jmsConnector;

    private MessageConsumersCreator messageConsumersCreator;

    public StockExchange() {
        final ApplicationContext context = new AnnotationConfigApplicationContext(ProcessingConfiguration.class, JmsConfiguration.class);
        queueConnector = context.getBean(QueueConnector.class);
        orderProcessor = context.getBean(OrderProcessor.class);
        jmsConnector = context.getBean(JmsConnector.class);
        messageConsumersCreator = context.getBean(MessageConsumersCreator.class);
    }

    @Override
    public void register(final ProcessingListener processingListener) {
        this.processingListener = processingListener;
    }

    @Override
    public void setDestinations(final List<String> destinations) {
        this.destinations = destinations;
    }

    @Override
    public void start() {
        log.info("Starting StockExchange");

        Assert.notNull(processingListener, "processingListener cannot be null");

        final JmsContext jmsContext = jmsConnector.connect();
        final List<MessageConsumer> consumers = createMessageConsumers(jmsContext);

        try {
            final CountDownLatch shutdownLatch = new CountDownLatch(1);
            orderProcessor.start(processingListener, consumers, shutdownLatch);
            shutdownLatch.await();
        } catch (Exception ex) {
            log.error(ex.getMessage());
        } finally {
            shutdownConsumers(consumers);
            jmsConnector.shutdown(jmsContext);

            log.info("Shutdown finished");
        }
    }

    private void shutdownConsumers(final List<MessageConsumer> consumers) {
        consumers.forEach(this::closeConsumer);
    }

    @SneakyThrows
    private void closeConsumer(final MessageConsumer c) {
        c.close();
    }

    private List<MessageConsumer> createMessageConsumers(final JmsContext jmsContext) {
        final List<Queue> queues = queueConnector.connect(destinations, jmsContext);
        return messageConsumersCreator.createMessageConsumers(queues, jmsContext.getSession());
    }

    @VisibleForTesting
    ProcessingListener getProcessingListener() {
        return processingListener;
    }
}
