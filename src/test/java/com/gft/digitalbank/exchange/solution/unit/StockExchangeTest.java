package com.gft.digitalbank.exchange.solution.unit;

import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.BDDCatchException.then;
import static com.googlecode.catchexception.apis.BDDCatchException.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.gft.digitalbank.exchange.solution.StockExchange;
import org.junit.Test;
import org.mockito.Mockito;

import com.gft.digitalbank.exchange.listener.ProcessingListener;

/**
 * @author mszarlinski on 2016-06-28.
 */
public class StockExchangeTest {

    @Test
    public void should_set_processing_listener() {
        // given
        StockExchange stockExchange = new StockExchange();
        ProcessingListener listenerMock = Mockito.mock(ProcessingListener.class);
        // when
        stockExchange.register(listenerMock);
        // then
        assertThat(stockExchange.getProcessingListener()).isEqualTo(listenerMock);
    }

    @Test
    public void should_throw_if_processing_listener_is_null() {
        // given
        StockExchange stockExchange = new StockExchange();
        stockExchange.register(null);
        // when
        when(stockExchange).start();
        // then
        then(caughtException())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("processingListener cannot be null");
    }
}
