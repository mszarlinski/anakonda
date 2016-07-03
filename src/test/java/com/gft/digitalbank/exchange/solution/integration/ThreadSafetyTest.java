package com.gft.digitalbank.exchange.solution.integration;

import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.jms.MessageProcessingDispatcher;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.IntStream.range;

/**
 * @author mszarlinski on 2016-07-03.
 */
@RunWith(SpringRunner.class)
public class ThreadSafetyTest {

    @Autowired
    private MessageProcessingDispatcher messageProcessingDispatcher;
    @Autowired
    private ProductRegistry productRegistry;

    private ExecutorService executorService;

    @Test
    public void buyAndModify(int threads, int messages) {
        executorService = Executors.newFixedThreadPool(threads);
        final List<CompletableFuture> futures = range(0, threads)
                .map(orderId -> CompletableFuture.supplyAsync(() -> range(0, messages)
                        .forEach(x -> {
                            buy(orderId, "A", 10, 10);
                            modify(orderId, 10, 5);
                        }), executorService));

        CompletableFuture.allOf(futures).get();

        Assertions.assertThat(productRegistry)
                .hasBookedOrders()
                .hasTransactions();
    }

    @Test
    public void buyAndCancel(int threads, int messages) {

        executorService = Executors.newFixedThreadPool(threads);


        final List<CompletableFuture> futures = range(0, threads)
                .map(orderId -> CompletableFuture.supplyAsync(() -> range(0, messages)
                        .forEach(x -> {
                            buy(orderId, "A", 10, 10);
                            cancel(orderId, 10, 5);
                        })));

        CompletableFuture.allOf(futures).get();
    }

    @Test
    public void buyAndSell(int threads, int messages) {

        executorService = Executors.newFixedThreadPool(threads);


        final List<CompletableFuture> futures = range(0, threads)
                .map(orderId -> CompletableFuture.supplyAsync(() -> range(0, messages)
                        .forEach(x -> {
                            buy(orderId, "A", 10, 10);
                            sell(orderId, 10, 5);
                        })));

        CompletableFuture.allOf(futures).get();
    }


    @Test
    public void sellAndBuy(int threads, int messages) {

        executorService = Executors.newFixedThreadPool(threads);


        final List<CompletableFuture> futures = range(0, threads)
                .map(orderId -> CompletableFuture.supplyAsync(() -> range(0, messages)
                        .forEach(x -> {
                            buy(orderId, "A", 10, 10);
                            sell(orderId, 10, 5);
                        })));

        CompletableFuture.allOf(futures).get();
    }

}
