package com.gft.digitalbank.exchange.solution.dataStructures;

import com.gft.digitalbank.exchange.model.OrderBook;
import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.message.OrderSide;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mszarlinski on 2016-07-03.
 */
@Data
public class ProductRegistry implements Lockable {

    public static final Comparator<Order> ORDERS_BY_PRICE_AND_TS_COMPARATOR = Comparator.comparingInt(Order::getPrice)
            .thenComparingLong(Order::getTimestamp);
    private static final int INITIAL_CAPACITY = 11;

    @Getter(AccessLevel.NONE)
    private final ReentrantLock lock = new ReentrantLock(true);

    private final PriorityQueue<Order> buyOrders = new PriorityQueue<>(INITIAL_CAPACITY, ORDERS_BY_PRICE_AND_TS_COMPARATOR);

    private final PriorityQueue<Order> sellOrders = new PriorityQueue<>(INITIAL_CAPACITY, ORDERS_BY_PRICE_AND_TS_COMPARATOR.reversed());

    private final List<Transaction> transactions = new ArrayList<>();

    private final List<OrderBook> orderBooks = new ArrayList<>();

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    public void addOrderToRegistry(final Order order, final ConcurrentMap<Integer, Order> ordersRegistry) {
        Assert.isTrue(lock.isHeldByCurrentThread(), "Registry must be locked when adding new order");

        if (order.getOrderSide() == OrderSide.BUY)
            doAddOrderToRegistry(order, ordersRegistry, buyOrders, sellOrders, (buyPrice, sellPrice) -> buyPrice - sellPrice);
    }

    private void doAddOrderToRegistry(final Order order, final ConcurrentMap<Integer, Order> ordersRegistry, final PriorityQueue<Order> targetQueue, final PriorityQueue<Order> counterQueue, final Comparator<Integer> priceCondition) {
        if (counterQueue.isEmpty()) {
            targetQueue.add(order);
            ordersRegistry.put(order.getId(), order);
        } else {
            int amountLeft = order.getAmount();
            while (!counterQueue.isEmpty() && amountLeft > 0) {

                final Order counterOrder = counterQueue.peek();

                if (priceCondition.compare(counterOrder.getPrice(), order.getPrice()) < 0) {
                    if (amountLeft < counterOrder.getAmount()) {
                        counterOrder.setAmount(counterOrder.getAmount() - amountLeft);
                        amountLeft = 0;
                    } else {
                        // whole counter order has been used
                        amountLeft -= counterOrder.getAmount();
                        counterQueue.remove();
                        ordersRegistry.remove(counterOrder.getId());
                    }
                }
            }

            if (amountLeft > 0) {
                order.setAmount(amountLeft);
                targetQueue.add(order);
                ordersRegistry.put(order.getId(), order);
            }
        }
    }
}
