package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ClientTimestampsTest {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T17:00:00Z"), BOGOTA);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 12, 0);

    @Test
    void usesTheDeviceTimeConvertedToTheServerZone() {
        OffsetDateTime twoDaysAgo = OffsetDateTime.of(2026, 11, 19, 14, 30, 0, 0, ZoneOffset.UTC);
        assertEquals(LocalDateTime.of(2026, 11, 19, 9, 30), ClientTimestamps.resolve(twoDaysAgo, CLOCK));
    }

    @Test
    void fallsBackToServerTimeWhenMissingFutureOrTooOld() {
        assertEquals(NOW, ClientTimestamps.resolve(null, CLOCK));
        assertEquals(NOW, ClientTimestamps.resolve(OffsetDateTime.of(2026, 11, 22, 17, 0, 0, 0, ZoneOffset.UTC), CLOCK));
        assertEquals(NOW, ClientTimestamps.resolve(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), CLOCK));
    }

    @Test
    void toleratesASlightlyFastDeviceClock() {
        OffsetDateTime twoMinutesAhead = OffsetDateTime.of(2026, 11, 21, 17, 2, 0, 0, ZoneOffset.UTC);
        assertEquals(NOW, ClientTimestamps.resolve(twoMinutesAhead, CLOCK));
    }
}
