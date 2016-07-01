package com.gft.digitalbank.exchange.solution.message;

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

    public static Cancellation fromMessage(final Map<String, Object> message) {
        return Cancellation.builder()
            .cancelledOrderId((int) message.get("cancelledOrderId"))
            .timestamp((int) message.get("timestamp"))
            .build();
    }
}
