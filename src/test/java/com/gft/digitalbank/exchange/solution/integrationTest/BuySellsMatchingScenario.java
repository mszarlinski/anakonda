package com.gft.digitalbank.exchange.solution.integrationTest;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.gft.digitalbank.exchange.model.OrderBook;
import com.gft.digitalbank.exchange.model.OrderDetails;
import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.model.orders.BrokerMessage;
import com.gft.digitalbank.exchange.model.orders.PositionOrder;
import com.gft.digitalbank.exchange.model.orders.Side;
import com.gft.digitalbank.exchange.verification.scenario.Scenario;

/**
 * @author mszarlinski on 2016-07-09.
 */
class BuySellsMatchingScenario extends Scenario {

    private static final int NUM_OF_BATCHES = 10_000;

    @Override
    public String description() {
        return "Order modifications scenario";
    }

    @Override
    public Set<Transaction> transactions() {
        return IntStream.rangeClosed(1, 2 * NUM_OF_BATCHES)
            .mapToObj(x -> Transaction.builder().id(x).amount(500).price(1000)
                .brokerBuy("broker1").brokerSell("broker2").clientBuy("client1").clientSell("client2").product("A").build())
            .collect(toSet());
    }

    @Override
    public Set<OrderBook> orderBooks() {
        return emptySet();
    }

    @Override
    protected List<BrokerMessage> orders() {
        int orderId = 1;
        List<BrokerMessage> messages = new ArrayList<>();

        for (int batch = 0; batch < NUM_OF_BATCHES; batch++) {

            messages.add(
                PositionOrder.builder().id(orderId).side(Side.BUY).timestamp(orderId)
                    .client("client1").broker("broker1").product("A")
                    .details(OrderDetails.builder().amount(1000).price(1000).build()).build());

            orderId++;

            messages.add(
                PositionOrder.builder().id(orderId).side(Side.SELL).timestamp(orderId)
                    .client("client2").broker("broker2").product("A")
                    .details(OrderDetails.builder().amount(500).price(500).build()).build());

            orderId++;

            messages.add(
                PositionOrder.builder().id(orderId).side(Side.SELL).timestamp(orderId)
                    .client("client2").broker("broker2").product("A")
                    .details(OrderDetails.builder().amount(500).price(500).build()).build());
        }

        return messages;

    }
}
