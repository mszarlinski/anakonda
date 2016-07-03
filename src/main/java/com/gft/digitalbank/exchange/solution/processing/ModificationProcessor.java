package com.gft.digitalbank.exchange.solution.processing;

import com.gft.digitalbank.exchange.solution.dataStructures.OrdersLog;
import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.message.Modification;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.message.OrderSide;
import com.google.gson.JsonObject;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class ModificationProcessor {

    private final OrdersLog ordersLog;
    private final ConcurrentMap<Integer, Order> ordersRegistry;

    public ModificationProcessor(final OrdersLog ordersLog, final ConcurrentMap<Integer, Order> ordersRegistry) {
        this.ordersLog = ordersLog;
        this.ordersRegistry = ordersRegistry;
    }

    public void process(final JsonObject message) {
        final Modification modification = Modification.fromMessage(message);

        System.out.println(Thread.currentThread().getName()+ " - " +modification); //FIXME

        final int modifiedOrderId = modification.getModifiedOrderId();
        final Order order = ordersRegistry.get(modifiedOrderId);

        final String product = order.getProduct();

        final ProductRegistry productRegistry = ordersLog.getProductRegistryForProduct(product);
        productRegistry.doWithLock(() -> {
            if (order.getOrderSide() == OrderSide.BUY) {
                modifyOrderInQueue(modification, productRegistry.getBuyOrders());
            } else {
                modifyOrderInQueue(modification, productRegistry.getSellOrders());
            }
        });
    }

    private void modifyOrderInQueue(final Modification modification, final PriorityQueue<Order> ordersQueue) {

        final Order modifiedOrder = ordersQueue.stream()
                .filter(o -> o.getId() == modification.getModifiedOrderId())
                .findFirst()
                .get();

        if (modification.getNewPrice() != modifiedOrder.getPrice()) {
            ordersQueue.remove(modifiedOrder); // we should reinsert the order to alter order in the queue
            modifiedOrder.modify(modification);
            if (modifiedOrder.getAmount() > 0) {
                ordersQueue.add(modifiedOrder);
            } else {
                ordersRegistry.remove(modification.getModifiedOrderId());
            }
        } else {
            // there is no need to reinsert the order
            modifiedOrder.modify(modification);
        }
    }
}
