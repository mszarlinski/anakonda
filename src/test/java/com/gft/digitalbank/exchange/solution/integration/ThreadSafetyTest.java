package com.gft.digitalbank.exchange.solution.integration;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gft.digitalbank.exchange.model.OrderBook;
import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.solution.MessageFactory;
import com.gft.digitalbank.exchange.solution.Spring;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.jms.JmsConfiguration;
import com.gft.digitalbank.exchange.solution.jms.MessageProcessingDispatcher;
import com.gft.digitalbank.exchange.solution.jms.ProcessingConfiguration;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-07-03.
 */
public class ThreadSafetyTest {

    private static final int NUM_OF_THREADS = 100;

    private static final int NUM_OF_ITERATIONS = 10;

    private MessageProcessingDispatcher messageProcessingDispatcher;

    private ExchangeRegistry exchangeRegistry;

    private ExecutorService executorService = Executors.newFixedThreadPool(8);

    private Random random = new Random();

    @Before
    public void reloadBeans() {
        new AnnotationConfigApplicationContext(ProcessingConfiguration.class, JmsConfiguration.class);

        messageProcessingDispatcher = Spring.getBean(MessageProcessingDispatcher.class);
        exchangeRegistry = Spring.getBean(ExchangeRegistry.class);
    }

    @Test
    public void scenario46_async() throws ExecutionException, InterruptedException {

        // given
        List<JsonObject> messages = asList(
            MessageFactory.createSellMessage(1, "A", 1_000_000, 5, 1, "1", "100"),
            MessageFactory.createSellMessage(2, "A", 20_000_000, 3, 2, "2", "101"),
            MessageFactory.createSellMessage(3, "A", 300_000, 4, 3, "1", "102"),

            MessageFactory.createCancelMessage(2, 4, "2"),
            MessageFactory.createModificationMessage(1, 200_000_000, 6, 5, "1"),

            MessageFactory.createBuyMessage(6, "A", 10_000_000, 4, 6, "1", "103"),
            MessageFactory.createBuyMessage(7, "A", 20_000_000, 10, 7, "1", "104")
        );

        //when
        final List<CompletableFuture<?>> futures = messages.stream().map(msg -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return CompletableFuture.runAsync(() -> messageProcessingDispatcher.process(msg), executorService);
            }
        )
            .collect(toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();

        System.out.println("AFTER JOIN");

        // then
        final ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct("A");
        final OrderBook orderBook = productRegistry.toOrderBook();
        assertThat(orderBook.getBuyEntries()).hasSize(1);

        assertThat(orderBook.getSellEntries()).hasSize(1);

        final List<Transaction> transactions = productRegistry.getTransactions();
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting("id").containsExactly(1, 2);
        assertThat(transactions).extracting("amount").containsExactly(300000, 20000000);
        assertThat(transactions).extracting("price").containsExactly(4, 6);

    }
//        public Set<Transaction> transactions () {
//            return Sets.newHashSet(
//                new Transaction[]{Transaction.builder().id(1).amount(300000).price(4).brokerBuy("1").brokerSell("1").clientBuy("103").clientSell("102").product("A").build(),
//                                  Transaction.builder().id(2).amount(20000000).price(6).brokerBuy("3").brokerSell("1").clientBuy("104").clientSell("100").product("A").build()});
//        }
//
//        public Set<OrderBook> orderBooks () {
//            return Sets.newHashSet(new OrderBook[]{
//                OrderBook.builder().product("A").sellEntry(OrderEntry.builder().id(1).amount(180000000).price(6).client("100").broker("1").build())
//                    .buyEntry(OrderEntry.builder().id(1).amount(9700000).price(4).client("103").broker("1").build()).build()});
//        }
//    }
}
