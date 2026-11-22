package com.agrosentinel.agrosentinel.dtos;

import java.time.OffsetDateTime;

/** Hora real en que el técnico inició la orden (puede llegar tarde si estaba sin internet). */
public record StartReviewRequest(OffsetDateTime startedAt) {
}
