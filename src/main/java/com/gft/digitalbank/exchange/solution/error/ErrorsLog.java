package com.gft.digitalbank.exchange.solution.error;

import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.String.format;

/**
 * @author mszarlinski on 2016-07-11.
 */
public class ErrorsLog {

    public static final String MESSAGE_FORMAT = "[%s] - %s";

    private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

    public void logException(final String message) {
        messages.add(format(MESSAGE_FORMAT, Thread.currentThread().getName(), message));
    }

    public ConcurrentLinkedQueue<String> getMessages() {
        return messages;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
