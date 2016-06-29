package com.gft.digitalbank.exchange.solution.jms;

import lombok.Builder;
import lombok.Getter;

import javax.jms.Connection;
import javax.jms.Session;

/**
 * @author mszarlinski@bravurasolutions.com on 2016-06-28.
 */
@Getter
@Builder
public class JmsContext {

    private final Connection connection;

    private final Session session;
}
