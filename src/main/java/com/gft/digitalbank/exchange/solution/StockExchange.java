package com.gft.digitalbank.exchange.solution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;

import com.gft.digitalbank.exchange.Exchange;
import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.solution.jms.JmsConfiguration;
import com.gft.digitalbank.exchange.solution.jms.JmsConnector;
import com.gft.digitalbank.exchange.solution.jms.JmsContext;
import com.gft.digitalbank.exchange.solution.jms.ProcessingConfiguration;
import com.google.common.annotations.VisibleForTesting;

/**
 * Your solution must implement the {@link Exchange} interface.
 */
public class StockExchange implements Exchange {

    private static final Log log = LogFactory.getLog(StockExchange.class);

    private ProcessingListener processingListener;

    private List<String> destinations = new ArrayList<>();

    // BEANS

    private JmsConnector jmsConnector;

    public StockExchange() {
        final ApplicationContext context = new AnnotationConfigApplicationContext(ProcessingConfiguration.class, JmsConfiguration.class);
        jmsConnector = context.getBean(JmsConnector.class);
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

        ExecutorService executorService = null;
        JmsContext jmsContext = null;
        try {
            final CountDownLatch shutdownLatch = new CountDownLatch(destinations.size());
            executorService = Executors.newFixedThreadPool(destinations.size()); //TODO:
            jmsContext = jmsConnector.connect(destinations, shutdownLatch, executorService, processingListener);

            shutdownLatch.await();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            executorService.shutdown();
            jmsConnector.shutdown(jmsContext);

            log.info("Shutdown finished");
        }
    }

    @VisibleForTesting
    ProcessingListener getProcessingListener() {
        return processingListener;
    }
}
