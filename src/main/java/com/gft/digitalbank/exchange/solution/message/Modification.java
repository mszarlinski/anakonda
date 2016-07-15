package com.gft.digitalbank.exchange.solution.message;

import com.google.gson.JsonObject;

import lombok.Builder;
import lombok.Data;

/**
 * @author mszarlinski on 2016-06-29.
 */
@Data
@Builder
public class Modification {

    private final int modifiedOrderId;

    private final int newAmount;

    private final int newPrice;

    private final long timestamp;

    private final String broker;

    public static Modification fromMessage(final JsonObject message) {
        final JsonObject details = message.get("details").getAsJsonObject();

        return Modification.builder()
            .modifiedOrderId(message.get("modifiedOrderId").getAsInt())
            .timestamp(message.get("timestamp").getAsInt())
            .newAmount(details.get("amount").getAsInt())
            .newPrice(details.get("price").getAsInt())
            .broker(message.get("broker").getAsString())
            .build();
    }

    //TODO: test
    public boolean willModifyOrder(final Order order) {
        return order.getId() == modifiedOrderId && (order.getAmount() != newAmount || order.getPrice() != newPrice);
    }
}
