package com.gft.digitalbank.exchange.solution.jms;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class GuardedPriorityQueue<T> extends PriorityQueue<T> {
    private static final int INITIAL_CAPACITY = 11;

    private static final boolean FAIR = true;

    private final ReentrantLock lock;

    public GuardedPriorityQueue(final Comparator<T> comparator) {
        super(INITIAL_CAPACITY, comparator);

        lock = new ReentrantLock(FAIR);
    }

    public void addWithLock(final T e) {
        lock();
        try {
            add(e);
        } finally {
            unlock();
        }
    }

    public void lock() {
        this.lock.lock();
    }

    public void unlock() {
        this.lock.unlock();
    }

}
