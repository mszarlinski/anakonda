package com.gft.digitalbank.exchange.solution.processing;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.gft.digitalbank.exchange.solution.jms.GuardedPriorityQueue;
import com.gft.digitalbank.exchange.solution.message.Modification;
import com.gft.digitalbank.exchange.solution.message.Order;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class ModificationProcessor {

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueuesByProduct;

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueuesByProduct;

    private final ConcurrentMap<Integer, Order> orderIdToOrder;

    private final ConcurrentSkipListSet<Integer> buyOrders;

    public ModificationProcessor(final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueuesByProduct, final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueuesByProduct, final
    ConcurrentMap<Integer, Order> orderIdToOrder, final ConcurrentSkipListSet<Integer> buyOrders) {
        this.buyQueuesByProduct = buyQueuesByProduct;
        this.sellQueuesByProduct = sellQueuesByProduct;
        this.orderIdToOrder = orderIdToOrder;
        this.buyOrders = buyOrders;
    }

    public void process(final Map<String, Object> message) {
        final Modification modification = Modification.fromMessage(message);

        final int modifiedOrderId = modification.getModifiedOrderId();
        final Order order = orderIdToOrder.get(modifiedOrderId);

        final String product = order.getProduct();

        if (isBuyOrder(modifiedOrderId)) {
            modifyOrderInQueue(modification, buyQueuesByProduct.get(product));
        } else {
            modifyOrderInQueue(modification, sellQueuesByProduct.get(product));
        }
    }

    private boolean isBuyOrder(final int orderId) {
        return buyOrders.contains(orderId);
    }

    private void modifyOrderInQueue(final Modification modification, final GuardedPriorityQueue<Order> ordersQueue) {
        ordersQueue.lock();
        try {
            final Order modifiedOrder = ordersQueue.stream()
                    .filter(o -> o.getId() == modification.getModifiedOrderId())
                    .findFirst()
                    .get();

            if (modification.getNewPrice() != modifiedOrder.getPrice()) {
                ordersQueue.remove(modifiedOrder); // we should reinsert the order to alter order in the queue
                modifiedOrder.modify(modification);
                ordersQueue.add(modifiedOrder);
            } else {
                // there is no need to reinsert the order
                modifiedOrder.modify(modification);
            }
        } finally {
            ordersQueue.unlock();
        }
    }
}
