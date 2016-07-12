package com.gft.digitalbank.exchange.solution.integrationTest;

import com.gft.digitalbank.exchange.solution.MessageFactory;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.dataStructures.ProductRegistry;
import com.gft.digitalbank.exchange.solution.error.ErrorsLog;
import com.gft.digitalbank.exchange.solution.processing.MessageDispatcherFactory;
import com.gft.digitalbank.exchange.solution.processing.MessageProcessingDispatcher;
import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mszarlinski on 2016-07-04.
 */
public class OrderProcessingTest {

    private MessageProcessingDispatcher messageProcessingDispatcher;

    private ExchangeRegistry exchangeRegistry;

    private ErrorsLog errorsLog;

    private String product = "A";

    private String broker = "Broker";

    private String client = "Client";

    @Before
    public void setup() {
        exchangeRegistry = new ExchangeRegistry();
        errorsLog = new ErrorsLog();
        messageProcessingDispatcher = MessageDispatcherFactory.createMessageDispatcher(
                new ConcurrentHashMap<>(), exchangeRegistry, errorsLog);
    }

    //TODO: split this test into smaller ones
    // BUY & SELL BASIC TESTS

    @Test
    public void buy_order_should_reduce_sell_order_with_lower_price() {
        // given
        JsonObject sellMessage = MessageFactory.createSellMessage(1, product, 10, 1000, 1, broker, client);
        JsonObject buyMessage = MessageFactory.createBuyMessage(2, product, 3, 2000, 1, broker, client);

        // when
        messageProcessingDispatcher.process(sellMessage);
        messageProcessingDispatcher.process(buyMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getBuyOrders()).isEmpty();

        assertThat(productRegistry.getSellOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getAmount()).isEqualTo(7);
            assertThat(order.getPrice()).isEqualTo(1000);
            assertThat(order.getProduct()).isEqualTo(product);
        });

        assertThat(productRegistry.getTransactions()).hasOnlyOneElementSatisfying(transaction -> {
            assertThat(transaction.getAmount()).isEqualTo(3);
            assertThat(transaction.getPrice()).isEqualTo(1000);
            assertThat(transaction.getProduct()).isEqualTo(product);
        });

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void buy_order_should_not_match_sell_order_with_higher_price() {
        // given
        String product = "A";
        JsonObject sellMessage = MessageFactory.createSellMessage(1, product, 10, 2000, 1, broker, client);
        JsonObject buyMessage = MessageFactory.createBuyMessage(2, product, 3, 1000, 1, broker, client);

        // when
        messageProcessingDispatcher.process(sellMessage);
        messageProcessingDispatcher.process(buyMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getBuyOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getAmount()).isEqualTo(3);
            assertThat(order.getPrice()).isEqualTo(1000);
            assertThat(order.getProduct()).isEqualTo(product);
        });

        assertThat(productRegistry.getSellOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getAmount()).isEqualTo(10);
            assertThat(order.getPrice()).isEqualTo(2000);
            assertThat(order.getProduct()).isEqualTo(product);
        });

        assertThat(productRegistry.getTransactions()).isEmpty();

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void buy_order_should_fully_compensate_sell_order_with_lower_price() {
        // given
        String product = "A";
        JsonObject sellMessage = MessageFactory.createSellMessage(1, product, 3, 1000, 1, broker, client);
        JsonObject buyMessage = MessageFactory.createBuyMessage(2, product, 10, 2000, 1, broker, client);

        // when
        messageProcessingDispatcher.process(sellMessage);
        messageProcessingDispatcher.process(buyMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getBuyOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getAmount()).isEqualTo(7);
            assertThat(order.getPrice()).isEqualTo(2000);
            assertThat(order.getProduct()).isEqualTo(product);
        });

        assertThat(productRegistry.getSellOrders()).isEmpty();

        assertThat(productRegistry.getTransactions()).hasOnlyOneElementSatisfying(transaction -> {
            assertThat(transaction.getAmount()).isEqualTo(3);
            assertThat(transaction.getPrice()).isEqualTo(1000);
            assertThat(transaction.getProduct()).isEqualTo(product);
        });

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void sell_order_should_reduce_buy_order_with_higher_price() {
        // given
        String product = "A";
        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 10, 2000, 1, broker, client);
        JsonObject sellMessage = MessageFactory.createSellMessage(2, product, 3, 1000, 1, broker, client);

        // when
        messageProcessingDispatcher.process(buyMessage);
        messageProcessingDispatcher.process(sellMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getSellOrders()).isEmpty();

        assertThat(productRegistry.getBuyOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getAmount()).isEqualTo(7);
            assertThat(order.getPrice()).isEqualTo(2000);
            assertThat(order.getProduct()).isEqualTo(product);
        });

        assertThat(productRegistry.getTransactions()).hasOnlyOneElementSatisfying(transaction -> {
            assertThat(transaction.getAmount()).isEqualTo(3);
            assertThat(transaction.getPrice()).isEqualTo(2000);
            assertThat(transaction.getProduct()).isEqualTo(product);
        });

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void sell_order_should_not_match_buy_order_with_lower_price() {
        // given
        String product = "A";
        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 10, 1000, 1, broker, client);
        JsonObject sellMessage = MessageFactory.createSellMessage(2, product, 3, 2000, 1, broker, client);

        // when
        messageProcessingDispatcher.process(buyMessage);
        messageProcessingDispatcher.process(sellMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getSellOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getAmount()).isEqualTo(3);
            assertThat(order.getPrice()).isEqualTo(2000);
            assertThat(order.getProduct()).isEqualTo(product);
        });

        assertThat(productRegistry.getBuyOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getAmount()).isEqualTo(10);
            assertThat(order.getPrice()).isEqualTo(1000);
            assertThat(order.getProduct()).isEqualTo(product);
        });

        assertThat(productRegistry.getTransactions()).isEmpty();

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void sell_order_should_fully_compensate_buy_order_with_higher_price() {
        // given
        String product = "A";
        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 2, 2000, 1, broker, client);
        JsonObject sellMessage = MessageFactory.createSellMessage(2, product, 11, 1000, 1, broker, client);

        // when
        messageProcessingDispatcher.process(buyMessage);
        messageProcessingDispatcher.process(sellMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getSellOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getAmount()).isEqualTo(9);
            assertThat(order.getPrice()).isEqualTo(1000);
            assertThat(order.getProduct()).isEqualTo(product);
        });

        assertThat(productRegistry.getBuyOrders()).isEmpty();

        assertThat(productRegistry.getTransactions()).hasOnlyOneElementSatisfying(transaction -> {
            assertThat(transaction.getAmount()).isEqualTo(2);
            assertThat(transaction.getPrice()).isEqualTo(2000);
            assertThat(transaction.getProduct()).isEqualTo(product);
        });

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    // ORDER OF MATCHING

    @Test
    public void buy_orders_should_be_matched_in_correct_order() {
        // given
        String product = "A";
        List<JsonObject> buyMessages = asList(
                MessageFactory.createBuyMessage(1, product, 10, 500, 1, broker, client),
                MessageFactory.createBuyMessage(2, product, 10, 2000, 1, broker, client),
                MessageFactory.createBuyMessage(3, product, 5, 1500, 1, broker, client),
                MessageFactory.createBuyMessage(4, product, 3, 1200, 1, broker, client),
                MessageFactory.createBuyMessage(5, product, 10, 1200, 2, broker, client));

        JsonObject sellMessage = MessageFactory.createSellMessage(6, product, 20, 1000, 5, broker, client);

        // when
        buyMessages.forEach(messageProcessingDispatcher::process);
        messageProcessingDispatcher.process(sellMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getSellOrders()).isEmpty();

        assertThat(productRegistry.getBuyOrders()).hasSize(2);
        assertThat(productRegistry.getBuyOrders().poll())
                .hasFieldOrPropertyWithValue("id", 5)
                .hasFieldOrPropertyWithValue("amount", 8)
                .hasFieldOrPropertyWithValue("price", 1200);

        assertThat(productRegistry.getBuyOrders().poll())
                .hasFieldOrPropertyWithValue("id", 1)
                .hasFieldOrPropertyWithValue("amount", 10)
                .hasFieldOrPropertyWithValue("price", 500);

        assertThat(productRegistry.getTransactions()).hasSize(4);
        assertThat(productRegistry.getTransactions())
                .extracting("id")
                .containsExactly(1, 2, 3, 4);
        assertThat(productRegistry.getTransactions())
                .extracting("amount")
                .containsExactly(10, 5, 3, 2);
        assertThat(productRegistry.getTransactions())
                .extracting("price")
                .containsExactly(2000, 1500, 1200, 1200);

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void sell_orders_should_be_matched_in_correct_order() {
        // given
        String product = "A";

        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 10, 2000, 5, broker, client);

        List<JsonObject> sellMessages = asList(
                MessageFactory.createSellMessage(2, product, 2, 2000, 1, broker, client),
                MessageFactory.createSellMessage(3, product, 3, 3000, 1, broker, client),
                MessageFactory.createSellMessage(4, product, 2, 1000, 2, broker, client),
                MessageFactory.createSellMessage(5, product, 1, 500, 1, broker, client),
                MessageFactory.createSellMessage(6, product, 2, 1000, 1, broker, client)
        );

        // when
        sellMessages.forEach(messageProcessingDispatcher::process);
        messageProcessingDispatcher.process(buyMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getBuyOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getId()).isEqualTo(1);
            assertThat(order.getAmount()).isEqualTo(3);
            assertThat(order.getPrice()).isEqualTo(2000);
        });

        assertThat(productRegistry.getSellOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getId()).isEqualTo(3);
            assertThat(order.getAmount()).isEqualTo(3);
            assertThat(order.getPrice()).isEqualTo(3000);
        });

        assertThat(productRegistry.getTransactions()).hasSize(4);
        assertThat(productRegistry.getTransactions())
                .extracting("amount")
                .containsExactly(1, 2, 2, 2);
        assertThat(productRegistry.getTransactions())
                .extracting("price")
                .containsExactly(500, 1000, 1000, 2000);

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    // MODIFICATION & CANCEL

    @Test
    public void modification_should_modify_given_order_and_change_ordering() {
        // given
        String product = "A";
        JsonObject sellMessage1 = MessageFactory.createSellMessage(1, product, 10, 500, 1, broker, client);
        JsonObject sellMessage2 = MessageFactory.createSellMessage(2, product, 10, 1000, 2, broker, client);
        JsonObject modificationMessage = MessageFactory.createModificationMessage(2, 5, 200, 3, broker);

        // when
        messageProcessingDispatcher.process(sellMessage1);
        messageProcessingDispatcher.process(sellMessage2);
        messageProcessingDispatcher.process(modificationMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getSellOrders().poll())
                .hasFieldOrPropertyWithValue("id", 2)
                .hasFieldOrPropertyWithValue("price", 200)
                .hasFieldOrPropertyWithValue("amount", 5);

        assertThat(productRegistry.getSellOrders().poll())
                .hasFieldOrPropertyWithValue("id", 1)
                .hasFieldOrPropertyWithValue("price", 500)
                .hasFieldOrPropertyWithValue("amount", 10);

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void modification_should_modify_order_timestamp() {
        // given
        String product = "A";
        JsonObject sellMessage = MessageFactory.createSellMessage(1, product, 10, 1000, 1, broker, client);
        JsonObject modificationMessage = MessageFactory.createModificationMessage(1, 10, 100, 99, broker);

        // when
        messageProcessingDispatcher.process(sellMessage);
        messageProcessingDispatcher.process(modificationMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getSellOrders()).hasOnlyOneElementSatisfying(order ->
                assertThat(order.getTimestamp()).isEqualTo(99));

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void modification_should_not_modify_order_timestamp_if_not_updated() {
        // given
        String product = "A";
        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 10, 1000, 1, broker, client);
        JsonObject modificationMessage = MessageFactory.createModificationMessage(1, 10, 1000, 99, broker);

        // when
        messageProcessingDispatcher.process(buyMessage);
        messageProcessingDispatcher.process(modificationMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getBuyOrders()).hasOnlyOneElementSatisfying(order ->
                assertThat(order.getTimestamp()).isEqualTo(1));

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void should_fail_silently_when_order_to_be_modified_not_found() {
        // given
        JsonObject modificationMessage = MessageFactory.createModificationMessage(1, 10, 1000, 1, broker);

        // when
        messageProcessingDispatcher.process(modificationMessage);

        // then
        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void cancel_should_cancel_given_order() {
        // given
        String product = "A";
        JsonObject sellMessage1 = MessageFactory.createSellMessage(1, product, 10, 500, 1, broker, client);
        JsonObject sellMessage2 = MessageFactory.createSellMessage(2, product, 10, 1000, 2, broker, client);
        JsonObject cancelMessage = MessageFactory.createCancelMessage(2, 3, broker);

        // when
        messageProcessingDispatcher.process(sellMessage1);
        messageProcessingDispatcher.process(sellMessage2);
        messageProcessingDispatcher.process(cancelMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getSellOrders()).hasOnlyOneElementSatisfying(order ->
                assertThat(order.getId()).isEqualTo(1));

        assertThat(errorsLog.isEmpty()).isTrue();
    }


    @Test
    public void should_fail_silently_when_order_to_be_cancelled_not_found() {
        // given
        JsonObject cancelMessage = MessageFactory.createCancelMessage(1, 1, broker);

        // when
        messageProcessingDispatcher.process(cancelMessage);

        // then
        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void modification_should_remove_order_when_amount_changed_to_zero() {
        // given
        String product = "A";
        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 10, 1000, 1, broker, client);
        JsonObject modificationMessage = MessageFactory.createModificationMessage(1, 0, 500, 2, broker);

        // when
        messageProcessingDispatcher.process(buyMessage);
        messageProcessingDispatcher.process(modificationMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getBuyOrders()).isEmpty();

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void modification_should_be_sent_by_the_same_broker() {
        // given
        String product = "A";
        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 10, 1000, 1, "Broker1", client);
        JsonObject modificationMessage = MessageFactory.createModificationMessage(1, 5, 500, 2, "Broker2");

        // when
        messageProcessingDispatcher.process(buyMessage);
        messageProcessingDispatcher.process(modificationMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getBuyOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getId()).isEqualTo(1);
            assertThat(order.getAmount()).isEqualTo(10);
            assertThat(order.getPrice()).isEqualTo(1000);
        });

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void cancel_should_be_sent_by_the_same_broker() {
        // given
        String product = "A";
        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 10, 1000, 1, "Broker1", client);
        JsonObject cancelMessage = MessageFactory.createCancelMessage(1, 2, "Broker2");

        // when
        messageProcessingDispatcher.process(buyMessage);
        messageProcessingDispatcher.process(cancelMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);

        assertThat(productRegistry.getBuyOrders()).hasSize(1);

        assertThat(errorsLog.isEmpty()).isTrue();
    }

    @Test
    public void modified_order_should_be_reprocessed() {
        // given
        String product = "A";
        JsonObject buyMessage = MessageFactory.createBuyMessage(1, product, 10, 500, 1, broker, client);
        JsonObject sellMessage = MessageFactory.createSellMessage(2, product, 10, 1000, 2, broker, client);

        // when
        messageProcessingDispatcher.process(buyMessage);
        messageProcessingDispatcher.process(sellMessage);

        // then
        ProductRegistry productRegistry = exchangeRegistry.getProductRegistryForProduct(product);
        assertThat(productRegistry.getBuyOrders()).hasSize(1);
        assertThat(productRegistry.getSellOrders()).hasSize(1);
        assertThat(productRegistry.getTransactions()).isEmpty();

        // when
        JsonObject modificationMessage = MessageFactory.createModificationMessage(2, 5, 200, 3, broker);
        messageProcessingDispatcher.process(modificationMessage);

        // then
        assertThat(productRegistry.getBuyOrders()).hasOnlyOneElementSatisfying(order -> {
            assertThat(order.getId()).isEqualTo(1);
            assertThat(order.getAmount()).isEqualTo(5);
            assertThat(order.getPrice()).isEqualTo(500);
            assertThat(order.getTimestamp()).isEqualTo(1);
        });

        assertThat(productRegistry.getSellOrders()).isEmpty();

        assertThat(productRegistry.getTransactions()).hasOnlyOneElementSatisfying(tx -> {
            assertThat(tx.getAmount()).isEqualTo(5);
            assertThat(tx.getPrice()).isEqualTo(500);
        });

        assertThat(errorsLog.isEmpty()).isTrue();
    }
}
