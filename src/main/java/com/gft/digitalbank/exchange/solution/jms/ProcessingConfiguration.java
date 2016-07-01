package com.gft.digitalbank.exchange.solution.jms;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gft.digitalbank.exchange.model.Transaction;
import com.gft.digitalbank.exchange.solution.Jndi;
import com.gft.digitalbank.exchange.solution.Spring;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.processing.BuySellOrderProcessor;
import com.gft.digitalbank.exchange.solution.processing.CancellationProcessor;
import com.gft.digitalbank.exchange.solution.processing.ModificationProcessor;

/**
 * @author mszarlinskion 2016-06-28.
 */
@Configuration
public class ProcessingConfiguration {

    public static final Comparator<Order> ORDERS_BY_PRICE_AND_TS_COMPARATOR = Comparator.comparingInt(Order::getPrice)
        .thenComparingLong(Order::getTimestamp);

    @Bean
    public MessageDeserializer messageDeserializer() {
        return new MessageDeserializer(new ObjectMapper());
    }

    @Bean(name = "buyQueues")
    public ConcurrentMap buyQueues() {
        return new ConcurrentHashMap<>();
    }

    @Bean(name = "sellQueues")
    public ConcurrentMap sellQueues() {
        return new ConcurrentHashMap<>();
    }

    @Bean(name = "orderIdToOrder")
    public ConcurrentMap orderIdToOrder() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Queue<Transaction> transactions() {
        return new ConcurrentLinkedQueue<>();
    }

    @Bean
    public JmsConnector jmsConnector(Jndi jndi) {
        return new JmsConnector(jndi);
    }

    @Bean
    public Jndi jndi() throws NamingException {
        Context context = new InitialContext();
        return new Jndi(context);
    }

    @Bean
    public Spring spring(ApplicationContext applicationContext) {
        return new Spring(applicationContext);
    }

    @Bean
    public BuySellOrderProcessor buySellOrderProcessor() {
        return new BuySellOrderProcessor(buyQueues(), sellQueues(), orderIdToOrder, buyOrders);
    }

    @Bean
    public ModificationProcessor modificationProcessor() {
        return new ModificationProcessor(buyQueues(), sellQueues(), orderIdToOrder(), buyOrders);
    }

    @Bean
    public CancellationProcessor cancellationProcessor() {
        return new CancellationProcessor(buyQueues(), sellQueues(), orderIdToOrder(), buyOrders);
    }

    @Bean
    public MessageProcessingDispatcher messageProcessingDispatcher(ModificationProcessor modificationProcessor, CancellationProcessor cancellationProcessor,
        BuySellOrderProcessor buySellOrderProcessor) {
        return new MessageProcessingDispatcher(modificationProcessor, cancellationProcessor, buySellOrderProcessor);
    }

    @Bean
    @Scope("prototype")
    public MessageProcessingTask messageProcessingTask(MessageDeserializer messageDeserializer, MessageProcessingDispatcher messageProcessingDispatcher) {
        return new MessageProcessingTask(messageDeserializer, messageProcessingDispatcher);
    }
}
