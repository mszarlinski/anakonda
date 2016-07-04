package com.gft.digitalbank.exchange.solution.processing;

import com.google.gson.JsonObject;

/**
 * @author mszarlinski on 2016-07-04.
 */
public interface MessageProcessor {
    void process(JsonObject message);
}
