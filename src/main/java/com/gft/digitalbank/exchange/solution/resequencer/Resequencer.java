package com.gft.digitalbank.exchange.solution.resequencer;

import com.gft.digitalbank.exchange.solution.error.AsyncErrorsKeeper;
import com.gft.digitalbank.exchange.solution.processing.MessageProcessingDispatcher;
import com.google.gson.JsonObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * @author mszarlinski on 2016-07-07.
 */
class Resequencer {

    private static final Log log = LogFactory.getLog(Resequencer.class);

    /**
     * We assume that messages with arrivalTime within the window have correct order. The longer window is, the less performant is the system.
     */
    //TODO: dynamically resizing window
    private long windowSizeMillis;

    private static final String ARRIVAL_TIMESTAMP_PROPERTY = "arrivalTimestamp";

    private final Semaphore shutdownCompletedMutex = new Semaphore(0);

    private long maxArrivalTimestampToProcess = 0;

    private boolean keepRunning;

    private long maxProcessedMessageTimestamp = -1;

    private final PriorityBlockingQueue<JsonObject> orderingQueue =
            new PriorityBlockingQueue<>(11, Comparator.comparingLong(msg -> msg.get("timestamp").getAsLong()));

    private final MessageProcessingDispatcher messageProcessingDispatcher;
    private final AsyncErrorsKeeper asyncErrorsKeeper;

    Resequencer(final MessageProcessingDispatcher messageProcessingDispatcher, final AsyncErrorsKeeper asyncErrorsKeeper) {
        this.messageProcessingDispatcher = messageProcessingDispatcher;
        this.asyncErrorsKeeper = asyncErrorsKeeper;
    }

    void startWithWindowOf(final int initialWindowSizeMillis) {
        keepRunning = true;
        maxArrivalTimestampToProcess = System.currentTimeMillis() + windowSizeMillis;
        windowSizeMillis = initialWindowSizeMillis;

        createTimerThread().start();
    }

    private Thread createTimerThread() {
        return new Thread(() -> {
            while (keepRunning) {
                try {
                    Thread.sleep(windowSizeMillis);
                    flushOldMessages();
                    maxArrivalTimestampToProcess += windowSizeMillis;
                } catch (Exception ex) {
                    // save exception but do not terminate a thread
                    asyncErrorsKeeper.logError(ex.getMessage());
                }
            }

            flushAllMessagesAndShutdown();
        });
    }

    private void flushAllMessagesAndShutdown() {
        try {
            flushAllMessagesOnShutdown();
        } catch (Exception ex) {
            asyncErrorsKeeper.logError(ex.getMessage());
        } finally {
            shutdownCompletedMutex.release();
        }
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
                asyncErrorsKeeper.logError("Messages are not processed in correct order");
                shutdownCompletedMutex.release();
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
