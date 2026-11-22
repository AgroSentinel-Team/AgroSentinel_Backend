package com.agrosentinel.agrosentinel.services;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Convierte la hora en que el técnico hizo el trabajo (enviada por el dispositivo,
 * posiblemente horas o días después si estaba sin internet) a la hora local del servidor.
 *
 * <p>Si no llega, o llega en el futuro por un reloj adelantado, se usa la hora del servidor.
 * Nunca se rechaza la operación por la fecha: un rechazo aquí bloquearía la cola offline.</p>
 */
public final class ClientTimestamps {
    private static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofMinutes(5);
    private static final Duration MAX_OFFLINE_AGE = Duration.ofDays(90);

    private ClientTimestamps() {
    }

    public static LocalDateTime resolve(OffsetDateTime clientValue, Clock clock) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (clientValue == null) return now;
        LocalDateTime value = clientValue.atZoneSameInstant(clock.getZone()).toLocalDateTime();
        if (value.isAfter(now.plus(CLOCK_SKEW_TOLERANCE)) || value.isBefore(now.minus(MAX_OFFLINE_AGE))) {
            return now;
        }
        return value.isAfter(now) ? now : value;
    }

    public static LocalDateTime resolve(OffsetDateTime clientValue) {
        return resolve(clientValue, Clock.systemDefaultZone());
    }
}
