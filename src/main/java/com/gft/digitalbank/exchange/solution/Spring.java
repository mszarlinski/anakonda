package com.gft.digitalbank.exchange.solution;

import org.springframework.context.ApplicationContext;

/**
 * @author mszarlinski on 2016-07-01.
 */
public class Spring {

    private static ApplicationContext ctx;

    public Spring(final ApplicationContext ctx) {
        Spring.ctx = ctx;
    }

    public static <T> T getBean(final Class<T> clazz) {
        return ctx.getBean(clazz);
    }

    public static <T> T getBean(final String beanName, final Class<T> clazz) {
        return ctx.getBean(beanName, clazz);
    }
}
