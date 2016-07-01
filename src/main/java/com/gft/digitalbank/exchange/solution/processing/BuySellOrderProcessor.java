package com.gft.digitalbank.exchange.solution.processing;

import static com.gft.digitalbank.exchange.solution.jms.ProcessingConfiguration.ORDERS_BY_PRICE_AND_TS_COMPARATOR;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.gft.digitalbank.exchange.solution.jms.GuardedPriorityQueue;
import com.gft.digitalbank.exchange.solution.message.Order;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class BuySellOrderProcessor {

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueues;

    private final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueues;

    public BuySellOrderProcessor(final ConcurrentMap<String, GuardedPriorityQueue<Order>> buyQueues, final ConcurrentMap<String, GuardedPriorityQueue<Order>> sellQueues) {
        this.buyQueues = buyQueues;
        this.sellQueues = sellQueues;
    }

    public void processBuy(final Map<String, Object> message) {
        final Order buyOrder = Order.fromMessage(message);

        final String product = buyOrder.getProduct();
        GuardedPriorityQueue<Order> buyQueue = getOrCreateQueue(buyQueues, product);
        GuardedPriorityQueue<Order> sellQueue = getOrCreateQueue(sellQueues, product);

        sellQueue.lock();

        if (sellQueue.isEmpty()) {
            sellQueue.unlock();

            buyQueue.addWithLock(buyOrder);
        } else {
            // sellQueues is locked

            int amountLeft = buyOrder.getAmount();
            while (!sellQueue.isEmpty() && amountLeft > 0) {

                final Order sellOrder = sellQueue.peek();

                if (sellOrder.getPrice() < buyOrder.getPrice()) {

                    if (amountLeft < sellOrder.getAmount()) {
                        sellOrder.setAmount(sellOrder.getAmount() - amountLeft);
                        amountLeft = 0;
                    } else {
                        amountLeft -= sellOrder.getAmount();
                    }
                }
            }

            sellQueue.unlock();

            if (amountLeft > 0) {
                buyOrder.setAmount(amountLeft);
                buyQueue.addWithLock(buyOrder);
            }
        }
    }

    private GuardedPriorityQueue<Order> getOrCreateQueue(final ConcurrentMap<String, GuardedPriorityQueue<Order>> queues, final String product) {
        synchronized (queues) {
            GuardedPriorityQueue<Order> queue = queues.get(product);
            if (queue == null) {
                queue = new GuardedPriorityQueue<>(ORDERS_BY_PRICE_AND_TS_COMPARATOR);
                queues.put(product, queue);
            }
            return queue;
        }
    }

    // TODO: code duplication
    public void processSell(final Map<String, Object> message) {
        final Order sellOrder = Order.fromMessage(message);

        final String product = sellOrder.getProduct();
        GuardedPriorityQueue<Order> buyQueue = getOrCreateQueue(buyQueues, product);
        GuardedPriorityQueue<Order> sellQueue = getOrCreateQueue(sellQueues, product);

        buyQueue.lock();

        if (buyQueue.isEmpty()) {
            buyQueue.unlock();

            buyQueue.addWithLock(sellOrder);
        } else {
            // buyQueues is locked

            int amountLeft = sellOrder.getAmount();
            while (!buyQueue.isEmpty() && amountLeft > 0) {

                final Order buyOrder = buyQueue.peek();

                if (buyOrder.getPrice() > sellOrder.getPrice()) {

                    if (amountLeft < buyOrder.getAmount()) {
                        buyOrder.setAmount(buyOrder.getAmount() - amountLeft);
                        amountLeft = 0;
                    } else {
                        amountLeft -= buyOrder.getAmount();
                    }
                }
            }

            buyQueue.unlock();

            if (amountLeft > 0) {
                sellOrder.setAmount(amountLeft);
                sellQueue.addWithLock(sellOrder);
            }
        }
    }

}
