package com.gft.digitalbank.exchange.solution.processing;

import com.gft.digitalbank.exchange.solution.jms.GuardedPriorityQueue;
import com.gft.digitalbank.exchange.solution.message.Cancellation;
import com.gft.digitalbank.exchange.solution.message.Order;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class CancellationProcessor {

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueuesByProduct;

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueuesByProduct;

    private final ConcurrentMap<Integer, Order> orderIdToOrder;

    private final ConcurrentSkipListSet<Integer> buyOrders;

    public CancellationProcessor(final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueuesByProduct, final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueuesByProduct, final
    ConcurrentMap<Integer, Order> orderIdToOrder, final ConcurrentSkipListSet<Integer> buyOrders) {
        this.buyQueuesByProduct = buyQueuesByProduct;
        this.sellQueuesByProduct = sellQueuesByProduct;
        this.orderIdToOrder = orderIdToOrder;
        this.buyOrders = buyOrders;
    }

    public void process(final Map<String, Object> message) {
        final Cancellation cancellation = Cancellation.fromMessage(message);

        final int cancelledOrderId = cancellation.getCancelledOrderId();
        final Order order = orderIdToOrder.get(cancelledOrderId);
        final String product = order.getProduct();

        if (isBuyOrder(cancelledOrderId)) {
            cancelOrderInQueue(cancelledOrderId, buyQueuesByProduct.get(product));
        } else {
            cancelOrderInQueue(cancelledOrderId, sellQueuesByProduct.get(product));
        }
    }

    private boolean isBuyOrder(final int orderId) {
        return buyOrders.contains(orderId);
    }

    private void cancelOrderInQueue(final int cancelledOrderId, final GuardedPriorityQueue<Order> ordersQueue) {
        ordersQueue.lock();
        try {
            ordersQueue.stream()
                    .filter(o -> o.getId() == cancelledOrderId)
                    .findFirst()
                    .ifPresent(ordersQueue::remove);
        } finally {
            ordersQueue.unlock();
        }
    }
}


