package com.walkthrough.api.transaction.dto;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CounterpartyResponseTest {

    /** @CsvSource: each line is one test case "input, expected" (quotes allow commas/spaces). */
    @ParameterizedTest
    @CsvSource({
            "'Paid to ONLINE SHOP', 'ONLINE SHOP'",
            "'Received from A FRIEND', 'A FRIEND'",
            "'Paid - electricity', 'electricity'",
            "'Cashback credited', 'Cashback credited'",
            "'Paid to ', 'Paid to '"
    })
    void stripsKnownPrefixes(String details, String expected) {
        assertThat(CounterpartyResponse.displayName(details)).isEqualTo(expected);
    }
}
