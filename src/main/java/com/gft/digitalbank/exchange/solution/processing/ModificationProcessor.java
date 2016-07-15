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

        if (order != null && order.sameBrokerAs(message)) {
            processOrder(modification, order);
        }
    }

    private void processOrder(final Modification modification, final Order order) {
        final String product = order.getProduct();

        final ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);
        productRegistry.doWithLock(() -> {
            if (order.getSide() == Side.BUY) {
                modifyOrderInQueue(modification, productRegistry.getBuyOrders(), productRegistry);
            } else {
                modifyOrderInQueue(modification, productRegistry.getSellOrders(), productRegistry);
            }
        });
    }

    private void modifyOrderInQueue(final Modification modification, final PriorityQueue<Order> ordersQueue, final ProductRegistry productRegistry) {
        ordersQueue.stream()
            .filter(modification::willModifyOrder)
            .findFirst()
            .ifPresent(order -> doModifyOrderInQueue(order, modification, ordersQueue, productRegistry));
    }

    private void doModifyOrderInQueue(final Order modifiedOrder, final Modification modification, final PriorityQueue<Order> ordersQueue, final ProductRegistry productRegistry) {
        ordersQueue.remove(modifiedOrder);
        modifiedOrder.modify(modification);

        if (modifiedOrder.getAmount() > 0) {
            productRegistry.doWithLock(() -> {
                productRegistry.addOrderToRegistry(modifiedOrder, ordersRegistry); // reinsert modified order
            });
        } else {
            ordersRegistry.remove(modifiedOrder.getId()); // forget about order
        }
    }
}
