package com.gft.digitalbank.exchange.solution.jms;

import com.gft.digitalbank.exchange.solution.Jndi;
import com.gft.digitalbank.exchange.solution.Spring;
import com.gft.digitalbank.exchange.solution.dataStructures.ExchangeRegistry;
import com.gft.digitalbank.exchange.solution.message.Order;
import com.gft.digitalbank.exchange.solution.processing.BuySellOrderProcessor;
import com.gft.digitalbank.exchange.solution.processing.CancellationProcessor;
import com.gft.digitalbank.exchange.solution.processing.ModificationProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author mszarlinskion 2016-06-28.
 */
@Configuration
public class ProcessingConfiguration {

    @Bean
    public MessageDeserializer messageDeserializer() {
        return new MessageDeserializer();
    }

    @Bean
    public ExchangeRegistry ordersLog() {
        return new ExchangeRegistry();
    }

    @Bean
    public ConcurrentMap<Integer, Order> ordersRegistry() {
        return new ConcurrentHashMap();
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
        return new BuySellOrderProcessor(ordersLog(), ordersRegistry());
    }

    @Bean
    public ModificationProcessor modificationProcessor() {
        return new ModificationProcessor(ordersLog(), ordersRegistry());
    }

    @Bean
    public CancellationProcessor cancellationProcessor() {
        return new CancellationProcessor(ordersLog(), ordersRegistry());
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
