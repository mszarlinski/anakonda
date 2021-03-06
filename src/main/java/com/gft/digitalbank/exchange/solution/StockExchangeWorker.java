package com.gft.digitalbank.exchange.solution;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.model.SolutionResult;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.error.AsyncErrorsKeeper;
import com.gft.digitalbank.exchange.solution.jms.JmsConnector;
import com.gft.digitalbank.exchange.solution.jms.JmsContext;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.resequencer.ResequencerDispatcher;
import com.gft.digitalbank.exchange.solution.resequencer.ResequencerDispatcherFactory;

/**
 * @author mszarlinski on 2016-07-01.
 */
class StockExchangeWorker extends Thread {

    private static final Log log = LogFactory.getLog(StockExchangeWorker.class);

    private final ProcessingListener processingListener;

    private final List<String> destinations;

    private final JmsConnector jmsConnector;

    private final ExchangeRegistry exchangeRegistry;

    private final ResequencerDispatcher resequencerDispatcher;

    private final AsyncErrorsKeeper asyncErrorsKeeper;

    StockExchangeWorker(final ProcessingListener processingListener, final List<String> destinations) throws NamingException {
        asyncErrorsKeeper = new AsyncErrorsKeeper();
        jmsConnector = new JmsConnector(new Jndi(), asyncErrorsKeeper);
        exchangeRegistry = new ExchangeRegistry();

        final ConcurrentMap<Integer, Order> ordersRegistry = new ConcurrentHashMap<>();
        resequencerDispatcher = ResequencerDispatcherFactory.createResequencerDispatcher(ordersRegistry, exchangeRegistry, asyncErrorsKeeper);

        this.processingListener = processingListener;
        this.destinations = destinations;
    }

    @Override
    public void run() {
        log.debug("Starting StockExchangeWorker");

        JmsContext jmsContext = null;
        try {
            final CountDownLatch shutdownLatch = new CountDownLatch(destinations.size());
            jmsContext = jmsConnector.connect(destinations, shutdownLatch, resequencerDispatcher);

            shutdownLatch.await();

            resequencerDispatcher.awaitShutdown();

            if (asyncErrorsKeeper.isEmpty()) {
                processingListener.processingDone(SolutionResult.builder()
                    .orderBooks(exchangeRegistry.extractOrderBooks())
                    .transactions(exchangeRegistry.extractTransactions())
                    .build());

                log.debug("Processing finished");
            } else {
                log.error(asyncErrorsKeeper.getMessages());
                notifyAboutProcessingError();
            }
        } catch (Exception ex) {
            asyncErrorsKeeper.logError(ex.getMessage());
        } finally {
            jmsConnector.shutdown(jmsContext);
            log.debug("Shutdown finished");
        }
    }

    // TODO:
    private void notifyAboutProcessingError() {
        processingListener.processingDone(SolutionResult.builder().build());
    }
}
