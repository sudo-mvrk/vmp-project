package com.volunnear.activity;

import com.volunnear.common.exception.BadDataInRequestException;

import java.time.LocalDateTime;

/**
 * Time rules for an activity (Q13, Q4d): it starts in the future, ends after it starts, and both are on
 * the same day. An end at midnight belongs to the next day, so the latest end is 23:59.
 */
final class ActivityWindowRules {
    private ActivityWindowRules() {
    }

    static void validate(LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime now) {
        if (!startsAt.isBefore(endsAt)) {
            throw new BadDataInRequestException("An activity must end after it starts: " + startsAt + " - " + endsAt);
        }
        if (!startsAt.toLocalDate().equals(endsAt.toLocalDate())) {
            throw new BadDataInRequestException("An activity must start and end on the same day (latest end 23:59); "
                    + "multi-day activities are not supported: " + startsAt + " - " + endsAt);
        }
        requireFutureStart(startsAt, now);
    }

    static void requireFutureStart(LocalDateTime startsAt, LocalDateTime now) {
        if (!startsAt.isAfter(now)) {
            throw new BadDataInRequestException("An activity must start in the future, got " + startsAt);
        }
    }
}
