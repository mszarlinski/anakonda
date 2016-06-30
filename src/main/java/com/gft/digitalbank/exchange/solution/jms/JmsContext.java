package com.gft.digitalbank.exchange.solution.jms;

import javax.jms.Connection;
import javax.jms.Session;
import java.util.List;

/**
 * @author mszarlinski@bravurasolutions.com on 2016-06-28.
 */
public class JmsContext {

    private final Connection connection;

    private final List<Session> sessions;

    @java.beans.ConstructorProperties({"connection"})
    JmsContext(final Connection connection, final List<Session> sessions) {
        this.connection = connection;
        this.sessions = sessions;
    }

    public static JmsContextBuilder builder() {
        return new JmsContextBuilder();
    }

    public Connection getConnection() {
        return this.connection;
    }

    public List<Session> getSessions() {
        return sessions;
    }

    public static class JmsContextBuilder {
        private Connection connection;
        private List<Session> sessions;

        JmsContextBuilder() {
        }

        public JmsContext.JmsContextBuilder connection(final Connection connection) {
            this.connection = connection;
            return this;
        }

        public JmsContextBuilder sessions(final List<Session> sessions) {
            this.sessions = sessions;
            return this;
        }

        public JmsContext build() {
            return new JmsContext(connection, sessions);
        }

        public String toString() {
            return "com.gft.digitalbank.exchange.solution.jms.JmsContext.JmsContextBuilder(connection=" + this.connection + ")";
        }
    }
}
