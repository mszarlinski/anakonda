package com.gft.digitalbank.exchange.solution.integrationTest;

import static java.util.Arrays.asList;

import com.gft.digitalbank.exchange.verification.test.VerificationTest;

/**
 * @author mszarlinski on 2016-07-03.
 */
public class ConcurrencyTestSuite extends VerificationTest {

    public ConcurrencyTestSuite() {
        super(asList(new BuySellsMatchingScenario()));
    }
}
