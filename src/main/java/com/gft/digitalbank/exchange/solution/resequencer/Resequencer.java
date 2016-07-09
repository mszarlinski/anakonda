package com.gft.digitalbank.exchange.solution.resequencer;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gft.digitalbank.exchange.solution.processing.MessageProcessingDispatcher;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-07-07.
 */
class Resequencer {

    private static final Log log = LogFactory.getLog(Resequencer.class);

    /**
     * We assume that messages with arrivalTime within the window have correct order. The longer window is, the less performant is the system.
     */
    //TODO: dynamically resizing window
    private static final long TIME_WINDOW_IN_MILLIS = 10;

    private static final String ARRIVAL_TIMESTAMP_PROPERTY = "arrivalTimestamp";

    private final Semaphore timeoutMutex = new Semaphore(0);

    private final Semaphore shutdownCompletedMutex = new Semaphore(0);

    private long maxArrivalTimestampToProcess = 0;

    private boolean keepRunning;

    private long maxProcessedMessageTimestamp = -1;

    private final PriorityBlockingQueue<JsonObject> orderingQueue =
        new PriorityBlockingQueue<>(11, Comparator.comparingLong(msg -> msg.get("timestamp").getAsLong()));

    private final MessageProcessingDispatcher messageProcessingDispatcher;

    Resequencer(final MessageProcessingDispatcher messageProcessingDispatcher) {
        this.messageProcessingDispatcher = messageProcessingDispatcher;
    }

    void start() {
        keepRunning = true;
        maxArrivalTimestampToProcess = System.currentTimeMillis() + TIME_WINDOW_IN_MILLIS;

        createTimerThread().start();
    }

    private Thread createTimerThread() {
        return new Thread(() -> {
            while (keepRunning) {
                try {
                    timeoutMutex.tryAcquire(TIME_WINDOW_IN_MILLIS, TimeUnit.MILLISECONDS);

                    flushOldMessages();
                    maxArrivalTimestampToProcess += TIME_WINDOW_IN_MILLIS;
                } catch (InterruptedException ex) {
                    log.error("Error while waiting on mutex", ex);
                }
            }
            flushAllMessagesOnShutdown();
            shutdownCompletedMutex.release();
        });
    }

    private void flushOldMessages() {
        JsonObject message;

        while (!orderingQueue.isEmpty() && messageCanBeProcessed(message = orderingQueue.peek())) {
            assertCorrectTimestamp(message.get("timestamp").getAsLong());

            orderingQueue.remove();
            messageProcessingDispatcher.process(message);
        }
    }

    private void flushAllMessagesOnShutdown() {
        while (!orderingQueue.isEmpty()) {
            final JsonObject message = orderingQueue.poll();

            assertCorrectTimestamp(message.get("timestamp").getAsLong());
            messageProcessingDispatcher.process(message);
        }
    }

    private void assertCorrectTimestamp(final long timestampToBeProcessed) throws IllegalStateException {
        if (maxProcessedMessageTimestamp != -1) {
            if (maxProcessedMessageTimestamp > timestampToBeProcessed) {
                throw new IllegalStateException("Messages are not processed in correct order");
            }
        } else {
            maxProcessedMessageTimestamp = timestampToBeProcessed;
        }
    }

    private boolean messageCanBeProcessed(final JsonObject message) {
        return message.get(ARRIVAL_TIMESTAMP_PROPERTY).getAsLong() < maxArrivalTimestampToProcess;
    }

    void addMessage(final JsonObject message) {
        message.addProperty(ARRIVAL_TIMESTAMP_PROPERTY, System.currentTimeMillis());
        orderingQueue.add(message);
    }

    void awaitShutdown() {
        keepRunning = false;
        try {
            shutdownCompletedMutex.acquire();
        } catch (InterruptedException ex) {
            log.error("Failed to awaitShutdown gracefully.", ex);
        }
    }
}
