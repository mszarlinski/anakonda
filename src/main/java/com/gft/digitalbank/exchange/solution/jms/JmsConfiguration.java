package com.gft.digitalbank.exchange.solution.jms;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring context configuration for Exchange application.
 *
 * @author mszarlinskion 2016-06-28.
 */
@Configuration
public class JmsConfiguration {

    @Bean
    public QueueConnector queueConnector() {
        return new QueueConnector();
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
    public MessageConsumersCreator messageConsumersCreator() {
        return new MessageConsumersCreator();
    }
}
