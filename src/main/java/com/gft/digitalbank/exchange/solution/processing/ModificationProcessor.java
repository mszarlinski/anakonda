package com.gft.digitalbank.exchange.solution.processing;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import com.gft.digitalbank.exchange.solution.jms.GuardedPriorityQueue;
import com.gft.digitalbank.exchange.solution.message.Modification;
import com.gft.digitalbank.exchange.solution.message.Order;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class ModificationProcessor {

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueues;

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueues;

    private final ConcurrentMap<Integer, Order> orderIdToOrder;

    public ModificationProcessor(final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueues, final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueues, final
    ConcurrentMap<Integer, Order> orderIdToOrder) {
        this.buyQueues = buyQueues;
        this.sellQueues = sellQueues;
        this.orderIdToOrder = orderIdToOrder;
    }

    public void process(final Map<String, Object> message) {
        final Modification modification = Modification.fromMessage(message);

        final int modifiedOrderId = modification.getModifiedOrderId();
        final Order order = orderIdToOrder.get(modifiedOrderId);

        final String product = order.getProduct();

        final GuardedPriorityQueue<Order> buyOrders = buyQueues.get(product);
        if (buyOrders != null) {
            final boolean applied = applyModificationToBuyOrder(modification, buyOrders);
            if (!applied) {
                if (!applyModificationToSellOrder(modification, product)) {
                    throw new IllegalArgumentException("Order not found for modification: " + modifiedOrderId);
                }
            }
        } else {
            if (!applyModificationToSellOrder(modification, product)) {
                throw new IllegalArgumentException("Order not found for modification: " + modifiedOrderId);
            }
        }
    }

    private boolean applyModificationToBuyOrder(final Modification modification, final GuardedPriorityQueue<Order> buyOrders) {
        buyOrders.lock();
        try {
            return tryApplyModificationToOrder(modification, buyOrders);
        } finally {
            buyOrders.unlock();
        }
    }

    private boolean tryApplyModificationToOrder(final Modification modification, final GuardedPriorityQueue<Order> ordersQueue) {
        // ordersQueue is locked
        final Optional<Order> modifiedOrderOpt = ordersQueue.stream()
            .filter(o -> o.getId() == modification.getModifiedOrderId())
            .findFirst();

        if (modifiedOrderOpt.isPresent()) {
            final Order modifiedOrder = modifiedOrderOpt.get();

            if (modification.getNewPrice() != modifiedOrder.getPrice()) {
                ordersQueue.remove(modifiedOrder); // we should reinsert the order to alter order in the queue
                modifiedOrder.modify(modification);
                ordersQueue.add(modifiedOrder);
            } else {
                // there is no need to reinsert the order
                modifiedOrder.modify(modification);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean applyModificationToSellOrder(final Modification modification, final String product) {
        final GuardedPriorityQueue<Order> sellOrders = sellQueues.get(product);
        if (sellOrders != null) {
            sellOrders.lock();
            try {
                return tryApplyModificationToOrder(modification, sellOrders);
            } finally {
                sellOrders.unlock();
            }
        } else {
            return false;
        }
    }
}
