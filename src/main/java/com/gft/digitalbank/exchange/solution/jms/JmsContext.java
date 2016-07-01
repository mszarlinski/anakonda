package com.gft.digitalbank.exchange.solution.jms;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import javax.jms.Connection;
import javax.jms.Session;

/**
 * @author mszarlinski on 2016-06-28.
 */
@Data
@Builder
public class JmsContext {

    private final Connection connection;

    private final List<Session> sessions;
}
