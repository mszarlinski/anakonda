package com.gft.digitalbank.exchange.solution.dataStructures;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.gft.digitalbank.exchange.model.OrderBook;
import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.model.orders.Side;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.processing.TransactionGenerator;

/**
 * TODO: use OrderBook class, maybe move transactions to ExchangeRegistry as synchronized LinkedList?
 * @author mszarlinski on 2016-07-03.
 */
@Data
public class ProductRegistry implements Lockable {

    public static final Comparator<Order> SELL_ORDERS_COMPARATOR_COMPARATOR = Comparator.comparingInt(Order::getPrice).thenComparingLong(Order::getTimestamp);

    public static final Comparator<Order> BUY_ORDERS_COMPARATOR_COMPARATOR = Comparator
        .comparing(Order::getPrice, (p1, p2) -> -Integer.compare(p1, p2))
        .thenComparingLong(Order::getTimestamp);

    private static final int INITIAL_CAPACITY = 11;

    @Getter(AccessLevel.NONE)
    private final ReentrantLock lock = new ReentrantLock(true);

    private final String product;

    private final PriorityQueue<Order> buyOrders = new PriorityQueue<>(INITIAL_CAPACITY, BUY_ORDERS_COMPARATOR_COMPARATOR);

    private final PriorityQueue<Order> sellOrders = new PriorityQueue<>(INITIAL_CAPACITY, SELL_ORDERS_COMPARATOR_COMPARATOR);

    private final List<Transaction> transactions = new ArrayList<>();

    private final TransactionGenerator transactionGenerator = new TransactionGenerator();

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    public void addOrderToRegistry(final Order order, final ConcurrentMap<Integer, Order> ordersRegistry) {
        doWithLock(() -> {
            if (order.getSide() == Side.BUY) {
                doAddOrderToRegistry(order, ordersRegistry, buyOrders, sellOrders,
                    (buyPrice, sellPrice) -> sellPrice - buyPrice,
                    (buyOrder, sellOrder) -> buyOrder,
                    (buyOrder, sellOrder) -> sellOrder);
            } else {
                doAddOrderToRegistry(order, ordersRegistry, sellOrders, buyOrders,
                    (buyPrice, sellPrice) -> buyPrice - sellPrice,
                    (sellPrice, buyPrice) -> buyPrice,
                    (sellPrice, buyPrice) -> sellPrice);
            }
        });
    }

    private void doAddOrderToRegistry(final Order order, final ConcurrentMap<Integer, Order> ordersRegistry, final PriorityQueue<Order> targetQueue, final PriorityQueue<Order>
        counterQueue, final Comparator<Integer> priceCondition, final BiFunction<Order, Order, Order> buyOrderSelector, final BiFunction<Order, Order, Order> sellOrderSelector) {
        if (counterQueue.isEmpty()) {
            targetQueue.add(order);
            ordersRegistry.put(order.getId(), order);
        } else {
            int amountLeft = order.getAmount();

            while (!counterQueue.isEmpty() && amountLeft > 0) {

                final Order counterOrder = counterQueue.peek();

                if (priceCondition.compare(counterOrder.getPrice(), order.getPrice()) < 0) {
                    break;
                }

                if (amountLeft < counterOrder.getAmount()) {
                    counterOrder.setAmount(counterOrder.getAmount() - amountLeft);

                    transactions.add(transactionGenerator.generatorTransaction(
                        buyOrderSelector.apply(order, counterOrder),
                        sellOrderSelector.apply(order, counterOrder),
                        amountLeft,
                        order.getProduct()));

                    amountLeft = 0;
                } else {
                    // whole counter order has been used
                    amountLeft -= counterOrder.getAmount();
                    counterQueue.remove();
                    ordersRegistry.remove(counterOrder.getId());

                    transactions.add(transactionGenerator.generatorTransaction(
                        buyOrderSelector.apply(order, counterOrder),
                        sellOrderSelector.apply(order, counterOrder),
                        counterOrder.getAmount(),
                        order.getProduct()));
                }
            }

            if (amountLeft > 0) {
                order.setAmount(amountLeft);
                targetQueue.add(order);
                ordersRegistry.put(order.getId(), order);
            }
        }
    }

    public OrderBook toOrderBook() {
        return OrderBook.builder()
            .product(product)
            .buyEntries(buyOrders.stream()
                .map(Order::toOrderEntry)
                .collect(Collectors.toList()))
            .sellEntries(sellOrders.stream()
                .map(Order::toOrderEntry)
                .collect(Collectors.toList()))
            .build();
    }

    // FIXME: moga tutaj byc transactions - wyniesienie do ExchangeRegistry?
    public boolean isEmpty() {
        return buyOrders.isEmpty() && sellOrders.isEmpty();
    }
}
