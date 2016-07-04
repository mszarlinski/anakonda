package com.gft.digitalbank.exchange.solution;

import lombok.SneakyThrows;

import javax.naming.Context;

/**
 * TODO: JNDI by Spring?
 * @author mszarlinski on 2016-06-28.
 */
public class Jndi {

    private final Context context;

    public Jndi(final Context context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T> T lookup(final String name) {
        return (T) context.lookup(name);

    }
}
