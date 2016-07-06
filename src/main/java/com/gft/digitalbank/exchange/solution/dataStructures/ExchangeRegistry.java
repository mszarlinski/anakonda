package com.gft.digitalbank.exchange.solution.dataStructures;

import static java.util.stream.Collectors.toList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import com.gft.digitalbank.exchange.model.OrderBook;
import com.gft.digitalbank.exchange.model.Transaction;

/**
 * @author mszarlinski on 2016-07-03.
 */
@Data
public class ExchangeRegistry implements Lockable {

    @Getter(AccessLevel.NONE)
    private ReentrantLock lock = new ReentrantLock(true);

    @Getter(AccessLevel.NONE)
    private ConcurrentMap<String, ProductRegistry> productRegistries = new ConcurrentHashMap<>();

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }

    public ProductRegistry getOrCreateProductRegistryForProduct(final String product) {
        return doWithLock(() -> {
            ProductRegistry productRegistry = productRegistries.get(product);
            if (productRegistry == null) {
                productRegistry = new ProductRegistry(product);
                productRegistries.put(product, productRegistry);
            }
            return productRegistry;
        });
    }

    public ProductRegistry getProductRegistryForProduct(final String product) {
        return productRegistries.get(product);
    }

    public List<Transaction> extractTransactions() {
        return productRegistries.values().stream()
            .flatMap(reg -> reg.getTransactions().stream())
            .collect(toList());
    }

    public List<OrderBook> extractOrderBooks() {
        return productRegistries.values().stream()
            .filter(ProductRegistry::isNotEmpty)
            .map(ProductRegistry::toOrderBook)
            .collect(toList());
    }
}
