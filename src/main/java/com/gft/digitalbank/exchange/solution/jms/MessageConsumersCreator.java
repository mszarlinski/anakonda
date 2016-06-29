package com.gft.digitalbank.exchange.solution.jms;

import static java.util.stream.Collectors.toList;
import lombok.SneakyThrows;

import java.util.List;

import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;

/**
 * @author mszarlinski on 2016-06-28.
 */
public class MessageConsumersCreator {

    public List<MessageConsumer> createMessageConsumers(final List<Queue> queues, final Session session) {
        return queues.stream()
            .map(q -> createConsumer(q, session))
            .collect(toList());
    }

    @SneakyThrows
    private MessageConsumer createConsumer(final Queue queue, final Session session) {
        return session.createConsumer(queue);
    }
}
