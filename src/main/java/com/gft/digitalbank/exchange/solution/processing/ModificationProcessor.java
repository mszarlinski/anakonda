package com.gft.digitalbank.exchange.solution.processing;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentMap;

import com.gft.digitalbank.exchange.model.orders.Side;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.message.Modification;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class ModificationProcessor implements MessageProcessor {

    private final ExchangeRegistry exchangeRegistry;

    private final ConcurrentMap<Integer, Order> ordersRegistry;

    public ModificationProcessor(final ExchangeRegistry exchangeRegistry, final ConcurrentMap<Integer, Order> ordersRegistry) {
        this.exchangeRegistry = exchangeRegistry;
        this.ordersRegistry = ordersRegistry;
    }

    @Override
    public void process(final JsonObject message) {
        final Modification modification = Modification.fromMessage(message);

        final int modifiedOrderId = modification.getModifiedOrderId();
        final Order order = ordersRegistry.get(modifiedOrderId);

        final String product = order.getProduct();

        final ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);
        productRegistry.doWithLock(() -> {
            if (order.getSide() == Side.BUY) {
                modifyOrderInQueue(modification, productRegistry.getBuyOrders());
            } else {
                modifyOrderInQueue(modification, productRegistry.getSellOrders());
            }
        });
    }

    private void modifyOrderInQueue(final Modification modification, final PriorityQueue<Order> ordersQueue) {
        ordersQueue.stream()
            .filter(order -> modificationCanBeApplied(order, modification))
            .findFirst()
            .ifPresent(order -> doModifyOrderInQueue(order, modification, ordersQueue));
    }

    private void doModifyOrderInQueue(final Order modifiedOrder, final Modification modification, final PriorityQueue<Order> ordersQueue) {
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

    private boolean modificationCanBeApplied(final Order order, final Modification modification) {
        return order.getId() == modification.getModifiedOrderId() && order.getBroker().equals(modification.getBroker());
    }
}
