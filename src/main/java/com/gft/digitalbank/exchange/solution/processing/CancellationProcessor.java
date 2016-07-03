package com.gft.digitalbank.exchange.solution.processing;

import com.gft.digitalbank.exchange.solution.dataStructures.OrdersLog;
import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.message.Cancellation;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.message.OrderSide;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentMap;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class CancellationProcessor {

    private final OrdersLog ordersLog;
    private final ConcurrentMap<Integer, Order> ordersRegistry;

    public CancellationProcessor(final OrdersLog ordersLog, final ConcurrentMap<Integer, Order> ordersRegistry) {
        this.ordersLog = ordersLog;
        this.ordersRegistry = ordersRegistry;
    }

    public void process(final JsonObject message) {
        final Cancellation cancellation = Cancellation.fromMessage(message);

        System.out.println(Thread.currentThread().getName()+ " - " +cancellation); //FIXME

        final int cancelledOrderId = cancellation.getCancelledOrderId();
        final Order order = ordersRegistry.get(cancelledOrderId);
        final String product = order.getProduct();

        final ProductRegistry productRegistry = ordersLog.getProductRegistries().get(product);

        productRegistry.doWithLock(() -> {
            if (order.getOrderSide() == OrderSide.BUY) {
                cancelOrderInQueue(cancelledOrderId, productRegistry.getBuyOrders());
            } else {
                cancelOrderInQueue(cancelledOrderId, productRegistry.getSellOrders());
            }
        });
    }

    private void cancelOrderInQueue(final int cancelledOrderId, final Queue<Order> ordersQueue) {
        ordersQueue.stream()
                .filter(o -> o.getId() == cancelledOrderId)
                .findFirst()
                .ifPresent(ordersQueue::remove);

        ordersRegistry.remove(cancelledOrderId);
    }
}


