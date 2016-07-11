package com.gft.digitalbank.exchange.solution.integrationTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.gft.digitalbank.exchange.model.OrderBook;
import com.gft.digitalbank.exchange.model.OrderDetails;
import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.model.orders.BrokerMessage;
import com.gft.digitalbank.exchange.model.orders.ModificationOrder;
import com.gft.digitalbank.exchange.model.orders.PositionOrder;
import com.gft.digitalbank.exchange.model.orders.Side;
import com.gft.digitalbank.exchange.verification.scenario.Scenario;

/**
 * @author mszarlinski on 2016-07-09.
 */
class OrderModificationsScenario extends Scenario {

    private static final int NUM_OF_BATCHES = 10_000;

    @Override
    public String description() {
        return "Order modifications scenario";
    }

    @Override
    public Set<Transaction> transactions() {
        return IntStream.rangeClosed(1, NUM_OF_BATCHES)
            .mapToObj(x -> Transaction.builder().id(x).amount(900).price(500)
                .brokerBuy("broker").brokerSell("broker").clientBuy("client").clientSell("client").product("A").build())
            .collect(Collectors.toSet());
    }

    @Override
    public Set<OrderBook> orderBooks() {
        return Collections.emptySet();
    }

    @Override
    protected List<BrokerMessage> orders() {
        int orderId = 1;
        List<BrokerMessage> messages = new ArrayList<>();

        for (int batch = 0; batch < NUM_OF_BATCHES; batch++) {
            int sellOrderId = orderId;
            messages.add(
                PositionOrder.builder().id(sellOrderId).side(Side.SELL).timestamp(sellOrderId)
                    .client("client").broker("broker").product("A")
                    .details(OrderDetails.builder().amount(1000).price(1000).build()).build());

            orderId++;
            messages.add(
                ModificationOrder.builder().id(orderId).modifiedOrderId(sellOrderId).timestamp(orderId).broker("broker")
                    .details(OrderDetails.builder().amount(900).price(500).build()).build());

            orderId++;
            messages.add(
                PositionOrder.builder().id(orderId).side(Side.BUY).timestamp(orderId)
                    .client("client").broker("broker").product("A")
                    .details(OrderDetails.builder().amount(900).price(500).build()).build());
        }

        return messages;

    }
}
