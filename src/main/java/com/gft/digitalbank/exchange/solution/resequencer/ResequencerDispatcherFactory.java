package com.gft.digitalbank.exchange.solution.resequencer;

import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.error.AsyncErrorsKeeper;
import com.gft.digitalbank.exchange.solution.message.Order;

import java.util.concurrent.ConcurrentMap;

import static com.gft.digitalbank.exchange.solution.processing.MessageDispatcherFactory.createMessageDispatcher;

/**
 * @author mszarlinski on 2016-07-11.
 */
public class ResequencerDispatcherFactory {

    public static ResequencerDispatcher createResequencerDispatcher(final ConcurrentMap<Integer, Order> ordersRegistry, final ExchangeRegistry exchangeRegistry, final AsyncErrorsKeeper
            asyncErrorsKeeper) {

        return new ResequencerDispatcher(createMessageDispatcher(ordersRegistry, exchangeRegistry, asyncErrorsKeeper), asyncErrorsKeeper);
    }
}
