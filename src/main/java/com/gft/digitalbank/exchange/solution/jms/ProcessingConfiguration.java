package com.gft.digitalbank.exchange.solution.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.digitalbank.exchange.solution.Jndi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author mszarlinskion 2016-06-28.
 */
@Configuration
public class ProcessingConfiguration {

    private static final int INITIAL_CAPACITY = 11;

    private static final Comparator<Order> ORDERS_BY_PRICE_AND_TS_COMPARATOR = Comparator.comparingInt(Order::getPrice)
            .thenComparingLong(Order::getTimestamp);

    @Bean
    public MessageDeserializer messageDeserializer() {
        return new MessageDeserializer(new ObjectMapper());
    }

    @Bean
    public PriorityBlockingQueue<Order> buyQueue() {
        return new PriorityBlockingQueue<>(INITIAL_CAPACITY, ORDERS_BY_PRICE_AND_TS_COMPARATOR);
    }

    @Bean
    public PriorityBlockingQueue<Order> sellQueue() {
        return new PriorityBlockingQueue<>(INITIAL_CAPACITY, ORDERS_BY_PRICE_AND_TS_COMPARATOR.reversed());
    }

    @Bean
    public JmsConnector jmsConnector(Jndi jndi, MessageDeserializer messageDeserializer) {
        return new JmsConnector(jndi, messageDeserializer);
    }

    @Bean
    public Jndi jndi() throws NamingException {
        Context context = new InitialContext();
        return new Jndi(context);
    }
}
