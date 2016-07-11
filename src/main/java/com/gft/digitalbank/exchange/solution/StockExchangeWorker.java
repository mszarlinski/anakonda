package com.gft.digitalbank.exchange.solution;

import lombok.NonNull;

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
import com.gft.digitalbank.exchange.solution.error.ErrorsLog;
import com.gft.digitalbank.exchange.solution.jms.JmsConnector;
import com.gft.digitalbank.exchange.solution.jms.JmsContext;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.resequencer.ResequencerDispatcher;
import com.gft.digitalbank.exchange.solution.resequencer.ResequencerDispatcherFactory;

/**
 * FIXME: czy ten task potrzebny gdy jest asynchorniczny Consumer?
 *
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

    private final ErrorsLog errorsLog;

    public StockExchangeWorker(@NonNull final ProcessingListener processingListener, @NonNull final List<String> destinations) throws NamingException {
        jmsConnector = new JmsConnector(new Jndi());
        exchangeRegistry = new ExchangeRegistry();
        errorsLog = new ErrorsLog();

        final ConcurrentMap<Integer, Order> ordersRegistry = new ConcurrentHashMap<>();
        resequencerDispatcher = ResequencerDispatcherFactory.createResequencerDispatcher(ordersRegistry, exchangeRegistry, errorsLog);

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

            if (errorsLog.isEmpty()) {
                processingListener.processingDone(SolutionResult.builder()
                    .orderBooks(exchangeRegistry.extractOrderBooks())
                    .transactions(exchangeRegistry.extractTransactions())
                    .build());

                log.debug("Processing finished");
            } else {
                log.error(errorsLog.getMessages());
                System.err.println(errorsLog.getMessages());
                processingListener.processingDone(SolutionResult.builder().build()); //TODO: ErrorSolutionResult
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            jmsConnector.shutdown(jmsContext);
            log.debug("Shutdown finished");
        }
    }
}
