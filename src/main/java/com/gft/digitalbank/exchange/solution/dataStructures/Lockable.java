package com.gft.digitalbank.exchange.solution.dataStructures;

import com.google.common.base.Throwables;

import java.util.concurrent.Callable;

/**
 * @author mszarlinski on 2016-07-03.
 */
public interface Lockable {

    default void doWithLock(final Runnable runnable) {
        lock();
        try {
            runnable.run();
        } finally {
            unlock();
        }
    }

    void lock();

    void unlock();
}
