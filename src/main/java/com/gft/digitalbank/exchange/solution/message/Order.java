package com.gft.digitalbank.exchange.solution.message;

import lombok.Builder;
import lombok.Data;

import com.gft.digitalbank.exchange.model.OrderEntry;
import com.gft.digitalbank.exchange.model.orders.Side;
import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-06-29.
 */
@Data
@Builder
public class Order {

    private final int id;

    private int amount;

    private int price;

    private long timestamp;

    private final String product;

    private final Side side;

    private final String broker;

    private final String client;

    public static Order fromMessage(final JsonObject message) {
        final JsonObject details = message.get("details").getAsJsonObject();
        return Order.builder()
            .id(message.get("id").getAsInt())
            .timestamp(message.get("timestamp").getAsInt())
            .amount(details.get("amount").getAsInt())
            .price(details.get("price").getAsInt())
            .product(message.get("product").getAsString())
            .side(Side.valueOf(message.get("side").getAsString()))
            .broker(message.get("broker").getAsString())
            .client(message.get("client").getAsString())
            .build();
    }

    public void modify(final Modification modification) {
        amount = modification.getNewAmount();
        price = modification.getNewPrice();
        timestamp = modification.getTimestamp();
    }

    //TODO: test
    public boolean sameBrokerAs(final JsonObject message) {
        final String messageBroker = message.get("broker").getAsString();
        return broker != null && broker.equals(messageBroker);
    }

    public OrderEntry toOrderEntry(final int position) {
        return OrderEntry.builder()
            .id(position)
            .broker(broker)
            .amount(amount)
            .client(client)
            .price(price)
            .build();
    }
}
