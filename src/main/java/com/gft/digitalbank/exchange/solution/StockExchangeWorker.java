package com.gft.digitalbank.exchange.solution;

import lombok.NonNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.model.SolutionResult;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.jms.JmsConnector;
import com.gft.digitalbank.exchange.solution.jms.JmsContext;
import com.gft.digitalbank.exchange.solution.resequencer.ResequencerDispatcher;

/**
 * FIXME: czy ten task potrzebny gdy jest asynchorniczny Consumer?
 * @author mszarlinski on 2016-07-01.
 */
public class StockExchangeWorker extends Thread {

    private static final Log log = LogFactory.getLog(StockExchangeWorker.class);

    private final ProcessingListener processingListener;

    private final List<String> destinations;

    // BEANS

    private final JmsConnector jmsConnector;

    private final ExchangeRegistry exchangeRegistry;

    private final ResequencerDispatcher resequencerDispatcher;

    public StockExchangeWorker(@NonNull final ProcessingListener processingListener, @NonNull final List<String> destinations) {
        // TODO: StockExchangeWorker as prototype bean
        jmsConnector = Spring.getBean(JmsConnector.class);
        exchangeRegistry = Spring.getBean(ExchangeRegistry.class);
        resequencerDispatcher = Spring.getBean(ResequencerDispatcher.class);

        this.processingListener = processingListener;
        this.destinations = destinations;
    }

    @Override
    public void run() {
        log.info("Starting StockExchangeWorker");

        JmsContext jmsContext = null;
        try {
            final CountDownLatch shutdownLatch = new CountDownLatch(destinations.size());
            jmsContext = jmsConnector.connect(destinations, shutdownLatch, resequencerDispatcher);

            shutdownLatch.await();

            resequencerDispatcher.awaitShutdown();

            processingListener.processingDone(SolutionResult.builder()
                .orderBooks(exchangeRegistry.extractOrderBooks())
                .transactions(exchangeRegistry.extractTransactions())
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
