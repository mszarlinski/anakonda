package com.gft.digitalbank.exchange.solution.resequencer;

import java.util.concurrent.ConcurrentMap;

import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.error.ErrorsLog;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.processing.BuySellOrderProcessor;
import com.gft.digitalbank.exchange.solution.processing.CancellationProcessor;
import com.gft.digitalbank.exchange.solution.processing.MessageProcessingDispatcher;
import com.gft.digitalbank.exchange.solution.processing.ModificationProcessor;

/**
 * @author mszarlinski on 2016-07-11.
 */
public class ResequencerDispatcherFactory {
    public static ResequencerDispatcher createResequencerDispatcher(final ConcurrentMap<Integer, Order> ordersRegistry, final ExchangeRegistry exchangeRegistry, final ErrorsLog
        errorsLog) {
        return new ResequencerDispatcher(new MessageProcessingDispatcher(
            new ModificationProcessor(exchangeRegistry, ordersRegistry),
            new CancellationProcessor(exchangeRegistry, ordersRegistry),
            new BuySellOrderProcessor(exchangeRegistry, ordersRegistry)), errorsLog);
    }

}
