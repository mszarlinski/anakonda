package com.gft.digitalbank.exchange.solution;

import lombok.SneakyThrows;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * @author mszarlinski on 2016-06-28.
 */
public class Jndi {

    private final Context context;

    public Jndi(final Context context) {
        this.context = context;
    }

    @SuppressWarnings("unchecked")
//    @SneakyThrows
    public <T> T lookup(final String name) {
        try {
            return (T) context.lookup(name);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
