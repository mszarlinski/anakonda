package com.gft.digitalbank.exchange.solution.integrationTest;

import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gft.digitalbank.exchange.model.OrderBook;
import com.gft.digitalbank.exchange.model.OrderDetails;
import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.model.orders.BrokerMessage;
import com.gft.digitalbank.exchange.model.orders.CancellationOrder;
import com.gft.digitalbank.exchange.model.orders.PositionOrder;
import com.gft.digitalbank.exchange.model.orders.Side;
import com.gft.digitalbank.exchange.verification.scenario.Scenario;

/**
 * @author mszarlinski on 2016-07-14.
 */
class OrderCancellationsScenario extends Scenario {

    private static final int NUM_OF_BATCHES = 10_000;

    @Override
    public String description() {
        return "Order cancellations scenario";
    }

    @Override
    public Set<Transaction> transactions() {
        return emptySet();
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
            int sellOrderId = orderId;
            messages.add(
                PositionOrder.builder().id(sellOrderId).side(Side.SELL).timestamp(sellOrderId)
                    .client("client").broker("broker").product(Integer.toString(batch))
                    .details(OrderDetails.builder().amount(1000).price(1000).build()).build());

            orderId++;
            messages.add(
                CancellationOrder.builder().id(orderId).cancelledOrderId(sellOrderId).timestamp(orderId).broker("broker-1").build());

            orderId++;
            messages.add(
                    CancellationOrder.builder().id(orderId).cancelledOrderId(sellOrderId).timestamp(orderId).broker("broker").build());

            orderId++;
            messages.add(
                CancellationOrder.builder().id(orderId).cancelledOrderId(sellOrderId).timestamp(orderId).broker("broker").build());
        }

        return messages;

    }
}
