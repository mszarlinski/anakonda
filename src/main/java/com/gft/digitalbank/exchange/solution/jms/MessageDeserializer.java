package com.gft.digitalbank.exchange.solution.jms;

import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

import javax.jms.TextMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author mszarlinski on 2016-06-29.
 */
class MessageDeserializer {

    private final ObjectMapper objectMapper;

    MessageDeserializer(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    Map<String, Object> deserialize(final TextMessage message) {
        //TODO: return model object
        return objectMapper.readValue(message.getText(), HashMap.class);
    }
}
