package com.gft.digitalbank.exchange.solution.resequencer;

import com.gft.digitalbank.exchange.solution.error.AsyncErrorsKeeper;
import com.gft.digitalbank.exchange.solution.processing.MessageProcessingDispatcher;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static com.gft.digitalbank.exchange.solution.MessageFactory.createBuyMessage;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author mszarlinski on 2016-07-10.
 */
@RunWith(MockitoJUnitRunner.class)
public class ResequencerTest {

    @Mock
    private MessageProcessingDispatcher messageProcessingDispatcher;

    @Captor
    private ArgumentCaptor<JsonObject> processedMessageCaptor;

    @Mock
    private AsyncErrorsKeeper asyncErrorsKeeper;

    @Captor
    private ArgumentCaptor<String> errorMessageCaptor;

    @InjectMocks
    private Resequencer resequencer;

    @Test
    public void shouldSortMessagesByTimestamp() {
        // given
        List<Long> timestamps = newArrayList(2L, 4L, 5L, 10L, 9L, 6L, 1L, 7L, 3L, 8L);

        List<JsonObject> messages = IntStream.range(0, timestamps.size())
                .mapToObj(i -> createBuyMessage(i, "A", 10, 10, timestamps.get(i), "broker", "client"))
                .collect(toList());

        // when
        resequencer.startWithWindowOf(1);
        messages.forEach(resequencer::addMessage);
        resequencer.awaitShutdown();

        // then
        verify(messageProcessingDispatcher, times(timestamps.size())).process(processedMessageCaptor.capture());

        assertThat(processedMessageCaptor.getAllValues()).extracting(json -> json.get("timestamp").getAsLong())
                .isEqualTo(sorted(timestamps));
    }

    private List<Long> sorted(final List<Long> timestamps) {
        Collections.sort(timestamps);
        return timestamps;
    }

    @Test
    public void shouldDetectIncorrectOrder() throws InterruptedException {
        // given
        JsonObject buyMessage1 = createBuyMessage(1, "A", 10, 10, 1L, "broker", "client");
        JsonObject buyMessage2 = createBuyMessage(2, "A", 10, 10, 2L, "broker", "client");

        // when
        resequencer.startWithWindowOf(10);
        resequencer.addMessage(buyMessage2);
        Thread.sleep(100);
        resequencer.addMessage(buyMessage1);
        Thread.sleep(100);

        // then
        verify(asyncErrorsKeeper).logError(errorMessageCaptor.capture());
        assertThat(errorMessageCaptor.getValue()).contains("Messages are not processed in correct order");
    }
}
