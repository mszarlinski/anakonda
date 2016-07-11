package com.gft.digitalbank.exchange.solution.error;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author mszarlinski on 2016-07-11.
 */
public class ErrorsLog {

    private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

    public void addErrorMessage(final String message) {
        messages.add(message);
    }

    public ConcurrentLinkedQueue<String> getMessages() {
        return messages;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
