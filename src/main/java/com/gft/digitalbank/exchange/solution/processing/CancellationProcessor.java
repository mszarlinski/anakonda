package com.gft.digitalbank.exchange.solution.processing;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import com.gft.digitalbank.exchange.solution.jms.GuardedPriorityQueue;
import com.gft.digitalbank.exchange.solution.message.Cancellation;
import com.gft.digitalbank.exchange.solution.message.Order;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class CancellationProcessor {

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueues;

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueues;

    private final ConcurrentMap<Integer, Order> orderIdToOrder;

    public CancellationProcessor(final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueues, final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueues, final
    ConcurrentMap<Integer, Order> orderIdToOrder) {
        this.buyQueues = buyQueues;
        this.sellQueues = sellQueues;
        this.orderIdToOrder = orderIdToOrder;
    }

    public void process(final Map<String, Object> message) {
        final Cancellation cancellation = Cancellation.fromMessage(message);

        final int cancelledOrderId = cancellation.getCancelledOrderId();
        final Order order = orderIdToOrder.get(cancelledOrderId);

        final String product = order.getProduct();

        final GuardedPriorityQueue<Order> buyOrders = buyQueues.get(product);
        if (buyOrders != null) {
            final boolean applied = applyCancellationToBuyOrder(cancelledOrderId, buyOrders);
            if (!applied) {
                if (!applyCancellationToSellOrder(cancelledOrderId, product)) {
                    throw new IllegalArgumentException("Order not found for cancellation: " + cancelledOrderId);
                }
            }
        } else {
            if (!applyCancellationToSellOrder(cancelledOrderId, product)) {
                throw new IllegalArgumentException("Order not found for cancellation: " + cancelledOrderId);
            }
        }
    }

    private boolean applyCancellationToBuyOrder(final int cancelledOrderId, final GuardedPriorityQueue<Order> buyOrders) {
        buyOrders.lock();
        try {
            return tryApplyCancellationToOrder(cancelledOrderId, buyOrders);
        } finally {
            buyOrders.unlock();
        }
    }

    private boolean tryApplyCancellationToOrder(final int cancelledOrderId, final GuardedPriorityQueue<Order> ordersQueue) {
        // ordersQueue is locked
        final Optional<Order> cancelledOrderOpt = ordersQueue.stream()
            .filter(o -> o.getId() == cancelledOrderId)
            .findFirst();

        if (cancelledOrderOpt.isPresent()) {
            ordersQueue.remove(cancelledOrderOpt.get());
            return true;
        } else {
            return false;
        }
    }

    private boolean applyCancellationToSellOrder(final int cancelledOrderId, final String product) {
        final GuardedPriorityQueue<Order> sellOrders = sellQueues.get(product);
        if (sellOrders != null) {
            sellOrders.lock();
            try {
                return tryApplyCancellationToOrder(cancelledOrderId, sellOrders);
            } finally {
                sellOrders.unlock();
            }
        } else {
            return false;
        }
    }
}


