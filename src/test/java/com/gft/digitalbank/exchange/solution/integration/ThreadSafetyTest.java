package com.gft.digitalbank.exchange.solution.integration;

import static com.gft.digitalbank.exchange.solution.integration.OrdersLogAssertions.assertThat;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gft.digitalbank.exchange.solution.MessageFactory;
import com.gft.digitalbank.exchange.solution.Spring;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.jms.JmsConfiguration;
import com.gft.digitalbank.exchange.solution.jms.MessageProcessingDispatcher;
import com.gft.digitalbank.exchange.solution.jms.ProcessingConfiguration;
import com.google.common.base.Throwables;

/**
 * @author mszarlinski on 2016-07-03.
 */
public class ThreadSafetyTest {

    private static final int NUM_OF_THREADS = 100;

    private static final int NUM_OF_ITERATIONS = 10;

    private MessageProcessingDispatcher messageProcessingDispatcher;

    private ExchangeRegistry exchangeRegistry;

    private ExecutorService executorService = Executors.newFixedThreadPool(NUM_OF_THREADS);

    private Random random = new Random();

    @Before
    public void reloadBeans() {
        new AnnotationConfigApplicationContext(ProcessingConfiguration.class, JmsConfiguration.class);

        messageProcessingDispatcher = Spring.getBean(MessageProcessingDispatcher.class);
        exchangeRegistry = Spring.getBean(ExchangeRegistry.class);
    }

    @Test
    public void buyAndSellOnSingleProduct() {
        final List<CompletableFuture<Void>> futures = range(0, 100)
            .mapToObj(threadNo -> CompletableFuture.runAsync(() -> range(0, 10)
                .forEach(orderNo -> {
                    final int orderId = random.nextInt(1000);
                    messageProcessingDispatcher.process(MessageFactory.createBuyMessage(orderId, "A", 50, 1000, threadNo, "br1", "cl1"));
                    messageProcessingDispatcher.process(MessageFactory.createSellMessage(orderId, "A", 50, 500, threadNo, "br1", "cl1"));
                }), executorService))
            .collect(toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
        } catch (Exception ex) {
            Throwables.propagate(ex);
        }

        assertThat(exchangeRegistry)
            .hasBookedOrders();
//            .hasNoTransactions();
    }

    @Test
    public void buyAndModifyOnMultipleProducts() {
        final List<String> products = asList("0123456789".split(""));

        products.forEach(p -> runOperations((orderId, operationNo) -> {
//            buy(10 * orderId + Integer.parseInt(p), p, 10, 10, orderId);
//            modify(10 * orderId + Integer.parseInt(p), 10, 5, orderId);
        }));

        assertThat(exchangeRegistry)
            .hasBookedOrders();
//            .hasNoTransactions();
    }

    @Test
    public void buyAndSell() {

        runOperations((orderId, operationNo) -> {
//            buy(orderId, "A", 10, 10, operationNo);
//            sell(orderId, "A", 10, 5, operationNo);
        });

        // order registry should be empty
        assertThat(exchangeRegistry)
            .hasBookedOrders();
//            .hasNoTransactions();
    }

    private void runOperations(BiConsumer<Integer, Integer> operation) {
        final List<CompletableFuture<Void>> futures = range(0, NUM_OF_THREADS)
            .mapToObj(threadNo -> CompletableFuture.runAsync(() -> range(0, NUM_OF_ITERATIONS)
                .forEach(orderNo -> operation.accept(random.nextInt(1_000_000), orderNo)), executorService))
            .collect(toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).get();
        } catch (Exception ex) {
            Throwables.propagate(ex);
        }
    }

//    @Test
//    public void sellAndBuy(int threads, int messages) {
//
//        executorService = Executors.newFixedThreadPool(threads);
//
//        final List<CompletableFuture> futures = range(0, threads)
//            .map(orderId -> CompletableFuture.supplyAsync(() -> range(0, messages)
//                .forEach(x -> {
//                    buy(orderId, "A", 10, 10);
//                    sell(orderId, 10, 5);
//                })));
//
//        CompletableFuture.allOf(futures).get();
//    }
//
//    private void modify(final int orderId, final int amount, final int price, final int timestamp) {
//        JsonObject message = MessageFactory.createModificationMessage(orderId, amount, price, timestamp);
//        messageProcessingDispatcher.process(message);
//    }
//
//    private void buy(final int orderId, final String product, final int amount, final int price, final int timestamp) {
//        JsonObject message = MessageFactory.createBuyMessage(orderId, product, amount, price, timestamp);
//        messageProcessingDispatcher.process(message);
//    }
//
//    private void sell(final int orderId, final String product, final int amount, final int price, final int timestamp) {
//        JsonObject message = MessageFactory.createBuyMessage(orderId, product, amount, price, timestamp);
//        messageProcessingDispatcher.process(message);
//    }
}
