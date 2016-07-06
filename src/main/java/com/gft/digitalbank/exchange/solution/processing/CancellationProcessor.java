package com.gft.digitalbank.exchange.solution.processing;

import java.util.Queue;
import java.util.concurrent.ConcurrentMap;

import com.gft.digitalbank.exchange.model.orders.Side;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.message.Cancellation;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class CancellationProcessor implements MessageProcessor {

    private final ExchangeRegistry exchangeRegistry;

    private final ConcurrentMap<Integer, Order> ordersRegistry;

    public CancellationProcessor(final ExchangeRegistry exchangeRegistry, final ConcurrentMap<Integer, Order> ordersRegistry) {
        this.exchangeRegistry = exchangeRegistry;
        this.ordersRegistry = ordersRegistry;
    }

    @Override
    public void process(final JsonObject message) {
        final Cancellation cancellation = Cancellation.fromMessage(message);

        final int cancelledOrderId = cancellation.getCancelledOrderId();
        final Order order = ordersRegistry.get(cancelledOrderId);

        if (order != null) {
            processOrder(cancellation, order);
        }
    }

    private void processOrder(final Cancellation cancellation, final Order order) {
        final String product = order.getProduct();

        final ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        productRegistry.doWithLock(() -> {
            if (order.getSide() == Side.BUY) {
                cancelOrderInQueue(cancellation, productRegistry.getBuyOrders());
            } else {
                cancelOrderInQueue(cancellation, productRegistry.getSellOrders());
            }
        });
    }

    private void cancelOrderInQueue(final Cancellation cancellation, final Queue<Order> ordersQueue) {
        ordersQueue.stream()
            .filter(o -> cancelCanBeApplied(o, cancellation))
            .findFirst()
            .ifPresent(o -> {
                ordersQueue.remove(o);
                ordersRegistry.remove(cancellation.getCancelledOrderId());
            });
    }

    private boolean cancelCanBeApplied(final Order order, final Cancellation cancellation) {
        return order.getId() == cancellation.getCancelledOrderId() && order.getBroker().equals(cancellation.getBroker());
    }
}


