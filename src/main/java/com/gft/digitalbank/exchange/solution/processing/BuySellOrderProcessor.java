package com.gft.digitalbank.exchange.solution.processing;

import com.gft.digitalbank.exchange.solution.jms.GuardedPriorityQueue;
import com.gft.digitalbank.exchange.solution.message.Order;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.gft.digitalbank.exchange.solution.jms.ProcessingConfiguration.ORDERS_BY_PRICE_AND_TS_COMPARATOR;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class BuySellOrderProcessor {

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueuesByProduct;

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueuesByProduct;

    private final ConcurrentMap<Integer, Order> orderIdToOrder;

    private final ConcurrentSkipListSet<Integer> buyOrders;

    public BuySellOrderProcessor(final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueuesByProduct, final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueuesByProduct, final ConcurrentMap<Integer, Order> orderIdToOrder, final ConcurrentSkipListSet<Integer> buyOrders) {
        this.buyQueuesByProduct = buyQueuesByProduct;
        this.sellQueuesByProduct = sellQueuesByProduct;
        this.orderIdToOrder = orderIdToOrder;
        this.buyOrders = buyOrders;
    }

    public void processBuy(final Map<String, Object> message) {
        processOrder(message, true, buyQueuesByProduct, sellQueuesByProduct, (buyPrice, sellPrice) -> buyPrice - sellPrice);
    }

    private void processOrder(final Map<String, Object> message, final boolean isBuyOrder, final ConcurrentMap targetQueues, final ConcurrentMap counterQueues,
                              final Comparator<Integer> priceCondition) {

        final Order order = Order.fromMessage(message);

        final String product = order.getProduct();
        final GuardedPriorityQueue<Order> targetQueue = getOrCreateQueue(targetQueues, product);
        final GuardedPriorityQueue<Order> counterQueue = getOrCreateQueue(counterQueues, product);

        counterQueue.lock();

        if (counterQueue.isEmpty()) {
            counterQueue.unlock();

            targetQueue.addWithLock(order);
        } else {
            // counterQueue is locked

            int amountLeft = order.getAmount();
            while (!counterQueue.isEmpty() && amountLeft > 0) {

                final Order counterOrder = counterQueue.peek();

                if (priceCondition.compare(counterOrder.getPrice(), order.getPrice()) < 0) {
                    if (amountLeft < counterOrder.getAmount()) {
                        counterOrder.setAmount(counterOrder.getAmount() - amountLeft);
                        amountLeft = 0;
                    } else {
                        amountLeft -= counterOrder.getAmount();
                    }
                }
            }

            counterQueue.unlock();

            if (amountLeft > 0) {
                order.setAmount(amountLeft);
                targetQueue.lock(); //FIXME: synchro
                targetQueue.add(order);
                if (isBuyOrder) {
                    buyOrders.add(order.getId());
                }
                orderIdToOrder.put(order.getId(), order);
                targetQueue.unlock();
            }
        }
    }

    private GuardedPriorityQueue<Order> getOrCreateQueue(final ConcurrentMap<String, GuardedPriorityQueue<Order>> queues, final String product) {
        synchronized (queues) { // FIXME: synchro
            GuardedPriorityQueue<Order> queue = queues.get(product);
            if (queue == null) {
                queue = new GuardedPriorityQueue<>(ORDERS_BY_PRICE_AND_TS_COMPARATOR);
                queues.put(product, queue);
            }
            return queue;
        }
    }

    public void processSell(final Map<String, Object> message) {
        processOrder(message, false, sellQueuesByProduct, buyQueuesByProduct, (buyPrice, sellPrice) -> sellPrice - buyPrice);

    }

}
