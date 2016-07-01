package com.gft.digitalbank.exchange.solution;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;

import com.gft.digitalbank.exchange.Exchange;
import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.gft.digitalbank.exchange.solution.jms.JmsConfiguration;
import com.gft.digitalbank.exchange.solution.jms.ProcessingConfiguration;
import com.google.common.annotations.VisibleForTesting;

/**
 * Your solution must implement the {@link Exchange} interface.
 */
public class StockExchange implements Exchange {

    private ProcessingListener processingListener;

    private List<String> destinations = new ArrayList<>();

    public StockExchange() {
        // start Spring container
        new AnnotationConfigApplicationContext(ProcessingConfiguration.class, JmsConfiguration.class);
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
        Assert.notNull(processingListener, "processingListener cannot be null");

        new StockExchangeTask(processingListener, destinations).start();
    }

    @VisibleForTesting
    ProcessingListener getProcessingListener() {
        return processingListener;
    }
}
