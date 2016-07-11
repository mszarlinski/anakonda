package com.gft.digitalbank.exchange.solution.processing;

import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.message.Order;

import java.util.concurrent.ConcurrentMap;

/**
 * @author mszarlinski on 2016-07-11.
 */
public class MessageDispatcherFactory {
    public static MessageProcessingDispatcher createMessageDispatcher(final ConcurrentMap<Integer, Order> ordersRegistry, final ExchangeRegistry exchangeRegistry) {
        return new MessageProcessingDispatcher(
                new ModificationProcessor(exchangeRegistry, ordersRegistry),
                new CancellationProcessor(exchangeRegistry, ordersRegistry),
                new BuySellOrderProcessor(exchangeRegistry, ordersRegistry));
    }
}
