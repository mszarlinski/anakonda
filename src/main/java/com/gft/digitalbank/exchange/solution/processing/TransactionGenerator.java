package com.gft.digitalbank.exchange.solution.processing;

import java.util.concurrent.atomic.AtomicInteger;

import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.solution.message.Order;

/**
 * @author mszarlinski on 2016-07-04.
 */
public class TransactionGenerator {

    private final AtomicInteger transactionId = new AtomicInteger(0);

    public Transaction generatorTransaction(final Order buyOrder, final Order sellOrder, final int amount, final String product) {

        return Transaction.builder()
            .id(transactionId.incrementAndGet())
            .amount(amount)
            .price(buyOrder.getPrice())
            .product(product)
            .clientBuy(buyOrder.getClient())
            .clientSell(sellOrder.getClient())
            .brokerBuy(buyOrder.getBroker())
            .brokerSell(sellOrder.getBroker())
            .build();
    }
}
