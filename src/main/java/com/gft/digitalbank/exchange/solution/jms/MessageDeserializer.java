package com.gft.digitalbank.exchange.solution.jms;

import lombok.SneakyThrows;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
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

//    @SneakyThrows
    Map<String, Object> deserialize(final TextMessage message) {
        //TODO: return model object
        try {
            return objectMapper.readValue(message.getText(), HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return null;
    }
}
