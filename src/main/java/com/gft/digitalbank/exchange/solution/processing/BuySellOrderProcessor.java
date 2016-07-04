package com.gft.digitalbank.exchange.solution.processing;

import java.util.concurrent.ConcurrentMap;

import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class BuySellOrderProcessor implements MessageProcessor {

    private final ExchangeRegistry exchangeRegistry;

    private final ConcurrentMap<Integer, Order> ordersRegistry;

    public BuySellOrderProcessor(final ExchangeRegistry exchangeRegistry, final ConcurrentMap<Integer, Order> ordersRegistry) {
        this.exchangeRegistry = exchangeRegistry;
        this.ordersRegistry = ordersRegistry;
    }

    @Override
    public void process(final JsonObject message) {
        final Order order = Order.fromMessage(message);

        final String product = order.getProduct();
        final ProductRegistry productRegistry = exchangeRegistry.getOrCreateProductRegistryForProduct(product);

        productRegistry.addOrderToRegistry(order, ordersRegistry);
    }

}
