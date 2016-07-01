package com.gft.digitalbank.exchange.solution;

import lombok.NonNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.model.SolutionResult;
import com.gft.digitalbank.exchange.solution.jms.JmsConnector;
import com.gft.digitalbank.exchange.solution.jms.JmsContext;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class StockExchangeTask extends Thread {

    private static final Log log = LogFactory.getLog(StockExchangeTask.class);

    private final ProcessingListener processingListener;

    private final List<String> destinations;

    // BEANS

    private final JmsConnector jmsConnector;

    public StockExchangeTask(@NonNull final ProcessingListener processingListener, @NonNull final List<String> destinations) {
        // TODO: StockExchangeTask as prototype bean
        jmsConnector = Spring.getBean(JmsConnector.class);

        this.processingListener = processingListener;
        this.destinations = destinations;
    }

    @Override
    public void start() {
        log.info("Starting StockExchangeTask");

        JmsContext jmsContext = null;
        try {
            final CountDownLatch shutdownLatch = new CountDownLatch(destinations.size());
            jmsContext = jmsConnector.connect(destinations, shutdownLatch);

            shutdownLatch.await();

            processingListener.processingDone(SolutionResult.builder()
//                .orderBooks(createOrderBooks)
                .build());

            log.info("Processing finished");
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            jmsConnector.shutdown(jmsContext);
            log.info("Shutdown finished");
        }
    }
}
