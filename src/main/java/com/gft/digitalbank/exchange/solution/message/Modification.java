package com.gft.digitalbank.exchange.solution.message;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

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

    public static Modification fromMessage(final Map<String, Object> message) {
        final Map<String, Object> details = (Map<String, Object>) message.get("details"); // TODO: JsonObject

        return Modification.builder()
            .modifiedOrderId((int) message.get("modifiedOrderId"))
            .timestamp((int) message.get("timestamp"))
            .newAmount((int) details.get("amount"))
            .newPrice((int) message.get("price"))
            .build();
    }
}
