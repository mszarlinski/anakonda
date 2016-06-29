package com.gft.digitalbank.exchange.solution.jms;

import static java.util.stream.Collectors.toList;
import lombok.SneakyThrows;

import java.util.List;

import javax.jms.Queue;
import javax.jms.Session;

/**
 * @author mszarlinski on 2016-06-28.
 */
public class QueueConnector {

    @SneakyThrows
    public List<Queue> connect(final List<String> queueNames, final JmsContext jmsContext) {
        return queueNames.stream()
            .map(name -> createQueue(name, jmsContext.getSession()))
            .collect(toList());
    }

    @SneakyThrows
    private Queue createQueue(final String name, final Session session) {
        return session.createQueue(name);
    }
}
