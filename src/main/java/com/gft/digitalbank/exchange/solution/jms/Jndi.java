package com.gft.digitalbank.exchange.solution.jms;

import lombok.SneakyThrows;

import javax.naming.Context;

/**
 * @author mszarlinski on 2016-06-28.
 */
class Jndi {

    private final Context context;

    @SneakyThrows
    Jndi(final Context context) {
        this.context = context;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    <T> T lookup(final String name) {
        return (T) context.lookup(name);
    }
}
