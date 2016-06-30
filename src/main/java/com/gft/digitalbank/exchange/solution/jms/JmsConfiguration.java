package com.gft.digitalbank.exchange.solution.jms;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gft.digitalbank.exchange.solution.Jndi;

/**
 * Spring context configuration for Exchange application.
 *
 * @author mszarlinskion 2016-06-28.
 */
@Configuration
public class JmsConfiguration {

//    @Bean
//    public JmsConnector jmsConnector(Jndi jndi) {
//        return new JmsConnector(jndi, messageDeserializer);
//    }
//
//    @Bean
//    public Jndi jndi() throws NamingException {
//        Context context = new InitialContext();
//        return new Jndi(context);
//    }

}
