package com.gft.digitalbank.exchange.solution.jms;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;

import javax.jms.TextMessage;

/**
 * @author mszarlinski on 2016-06-29.
 */
class MessageDeserializer {

    private final JsonParser jsonParser = new JsonParser();

    @SneakyThrows
    JsonObject deserialize(final TextMessage message) {
        return jsonParser.parse(message.getText()).getAsJsonObject();
    }
    //TODO: może jednak static typed ?
}
