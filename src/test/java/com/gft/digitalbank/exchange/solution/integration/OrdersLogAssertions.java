package com.gft.digitalbank.exchange.solution.integration;

import org.assertj.core.api.AbstractAssert;

import com.gft.digitalbank.exchange.model.OrderBook;
import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;

/**
 * @author mszarlinski@bravurasolutions.com on 2016-07-04.
 */
class OrdersLogAssertions extends AbstractAssert<OrdersLogAssertions, ExchangeRegistry> {
    private OrdersLogAssertions(ExchangeRegistry actual) {
        super(actual, OrdersLogAssertions.class);
    }

    public static OrdersLogAssertions assertThat(ExchangeRegistry actual) {
        return new OrdersLogAssertions(actual);
    }

    public OrdersLogAssertions hasBookedOrders(OrderBook... orderBooks) {
        return this;

    }

    public OrdersLogAssertions hasTransactions(Transaction... transactions) {
        return this;

    }

}
