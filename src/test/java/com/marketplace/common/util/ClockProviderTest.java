package com.marketplace.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ClockProviderTest {

    @Test
    void returnsTimeFromInjectedClock() {
        Instant expected = Instant.parse("2026-01-15T10:30:00Z");
        ClockProvider provider = new ClockProvider(Clock.fixed(expected, ZoneOffset.UTC));

        assertThat(provider.now()).isEqualTo(expected);
    }
}
