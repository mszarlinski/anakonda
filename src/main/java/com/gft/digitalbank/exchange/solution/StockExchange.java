package com.gft.digitalbank.exchange.solution;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import org.springframework.util.Assert;

import com.gft.digitalbank.exchange.Exchange;
import com.gft.digitalbank.exchange.listener.ProcessingListener;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;

/**
 * Your solution must implement the {@link Exchange} interface.
 */
public class StockExchange implements Exchange {

    private ProcessingListener processingListener;

    private List<String> destinations = new ArrayList<>();

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

        try {
            new StockExchangeWorker(processingListener, destinations).start();
        } catch (NamingException e) {
            Throwables.propagate(e);
        }
    }

    @VisibleForTesting
    ProcessingListener getProcessingListener() {
        return processingListener;
    }
}
