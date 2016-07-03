package com.gft.digitalbank.exchange.solution.message;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * @author mszarlinski on 2016-07-01.
 */
@Data
@Builder
public class Cancellation {

    private final int cancelledOrderId;

    private final long timestamp;

    public static Cancellation fromMessage(final JsonObject message) {
        return Cancellation.builder()
            .cancelledOrderId(message.get("cancelledOrderId").getAsInt())
            .timestamp(message.get("timestamp").getAsInt())
            .build();
    }
}
