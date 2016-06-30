package com.gft.digitalbank.exchange.solution.jms;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.digitalbank.exchange.solution.Jndi;

/**
 * @author mszarlinskion 2016-06-28.
 */
@Configuration
public class ProcessingConfiguration {

    private static final int INITIAL_CAPACITY = 11;

    // Unable to use jdk8 Comparator utils due to Spring 3.x
    private static final Comparator<Order> ORDERS_BY_PRICE_AND_ID_COMPARATOR = new Comparator<Order>() {
        @Override
        public int compare(final Order o1, final Order o2) {
            if (o1.getPrice() != o2.getPrice()) {
                return o1.getPrice() - o2.getPrice();
            } else {
                return o1.getId() - o2.getId();
            }
        }
    };

    @Bean
    public MessageDeserializer messageDeserializer() {
        return new MessageDeserializer(new ObjectMapper());
    }

    @Bean
    public OrderProcessor orderProcessor(MessageDeserializer messageDeserializer) {
        return new OrderProcessor(messageDeserializer);
    }

    @Bean
    public PriorityBlockingQueue<Order> buyQueue() {
        return new PriorityBlockingQueue<>(INITIAL_CAPACITY, ORDERS_BY_PRICE_AND_ID_COMPARATOR);
    }

    @Bean
    public PriorityBlockingQueue<Order> sellQueue() {
        return new PriorityBlockingQueue<>(INITIAL_CAPACITY, ORDERS_BY_PRICE_AND_ID_COMPARATOR.reversed());
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
