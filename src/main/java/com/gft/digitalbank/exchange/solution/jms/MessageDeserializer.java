package com.gft.digitalbank.exchange.solution.jms;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;

import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * @author mszarlinski on 2016-06-29.
 */
class MessageDeserializer {

    private final JsonParser jsonParser = new JsonParser();

    JsonObject deserialize(final TextMessage message) throws JMSException {
        return jsonParser.parse(message.getText()).getAsJsonObject();
    }
}
