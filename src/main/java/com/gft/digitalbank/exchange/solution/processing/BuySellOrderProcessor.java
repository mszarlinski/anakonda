package com.gft.digitalbank.exchange.solution.processing;

import com.gft.digitalbank.exchange.solution.dataStructures.OrdersLog;
import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class BuySellOrderProcessor {

    private final OrdersLog ordersLog;

    private final ConcurrentMap<Integer, Order> ordersRegistry;

    public BuySellOrderProcessor(final OrdersLog ordersLog, final ConcurrentMap<Integer, Order> ordersRegistry) {
        this.ordersLog = ordersLog;
        this.ordersRegistry = ordersRegistry;
    }

    public void process(final JsonObject message) {
        final Order order = Order.fromMessage(message);
        System.out.println(Thread.currentThread().getName()+ " - " + order);//FIXME

        final String product = order.getProduct();
        final ProductRegistry productRegistry = ordersLog.getOrCreateProductRegistryForProduct(product);

        productRegistry.addOrderToRegistry(order, ordersRegistry);
    }

}
