package com.gft.digitalbank.exchange.solution.message;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

import org.springframework.util.NumberUtils;

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

    public static Order fromMessage(final Map<String, Object> message) {
        final Map<String, Object> details = (Map<String, Object>) message.get("details"); // TODO: JsonObject

        return Order.builder()
            .id((int) message.get("id"))
            .timestamp((int) message.get("timestamp"))
            .amount((int) details.get("amount"))
            .price((int) details.get("price"))
            .product((String) message.get("product"))
            .build();
    }

    public void modify(final Modification modification) {
        this.amount = modification.getNewAmount();
        this.price = modification.getNewPrice();
    }
}
