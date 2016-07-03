package com.gft.digitalbank.exchange.solution.message;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;

/**
 * @author mszarlinski on 2016-06-29.
 */
@Data
@Builder
public class Order {

    private final int id;

    private int amount;

    private int price;

    private final long timestamp;

    private final String product;

    private final OrderSide orderSide;

    public static Order fromMessage(final JsonObject message) {
        final JsonObject details = message.get("details").getAsJsonObject();
        return Order.builder()
                .id(message.get("id").getAsInt())
                .timestamp(message.get("timestamp").getAsInt())
                .amount(details.get("amount").getAsInt())
                .price(details.get("price").getAsInt())
                .product(message.get("product").getAsString())
                .orderSide(OrderSide.valueOf(message.get("side").getAsString()))
                .build();
    }

    public void modify(final Modification modification) {
        this.amount = modification.getNewAmount();
        this.price = modification.getNewPrice();
    }
}
