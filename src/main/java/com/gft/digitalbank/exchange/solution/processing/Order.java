package com.gft.digitalbank.exchange.solution.processing;

import lombok.Data;

/**
 * @author mszarlinski on 2016-06-29.
 */
@Data
public class Order {

    private final int id;

    private final int amount;

    private final int price;

    private final long timestamp;
}
