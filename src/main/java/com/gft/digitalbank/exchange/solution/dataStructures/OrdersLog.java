package com.gft.digitalbank.exchange.solution.dataStructures;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mszarlinski on 2016-07-03.
 */
@Data
public class OrdersLog implements Lockable {

    @Getter(AccessLevel.NONE)
    private ReentrantLock lock = new ReentrantLock(true);

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
                productRegistry = new ProductRegistry();
                productRegistries.put(product, productRegistry);
            }
            return productRegistry;
        });
    }

    public ProductRegistry getProductRegistryForProduct(final String product) {
        return productRegistries.get(product);
    }
}
