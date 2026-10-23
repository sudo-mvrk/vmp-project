package com.volunnear.activity;

import com.volunnear.common.exception.BadDataInRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityWindowRulesTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 17, 12, 0);

    @Test
    void sameDayWindowInTheFutureIsValid() {
        assertThatCode(() -> ActivityWindowRules.validate(at(18, 9, 0), at(18, 23, 59), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void endBeforeOrAtStartIsRejected() {
        assertThatThrownBy(() -> ActivityWindowRules.validate(at(18, 12, 0), at(18, 9, 0), NOW))
                .isInstanceOf(BadDataInRequestException.class).hasMessageContaining("end after it starts");
        assertThatThrownBy(() -> ActivityWindowRules.validate(at(18, 9, 0), at(18, 9, 0), NOW))
                .isInstanceOf(BadDataInRequestException.class);
    }

    @Test
    void multiDayWindowIsRejected() {
        assertThatThrownBy(() -> ActivityWindowRules.validate(at(18, 9, 0), at(19, 9, 0), NOW))
                .isInstanceOf(BadDataInRequestException.class).hasMessageContaining("same day");
    }

    @Test
    void endAtMidnightIsTheNextDayAndRejected() {
        // Q4d, Q3d: 22:00 - 24:00 is written as 22:00 to 00:00 of the next day.
        assertThatThrownBy(() -> ActivityWindowRules.validate(at(18, 22, 0), at(19, 0, 0), NOW))
                .isInstanceOf(BadDataInRequestException.class).hasMessageContaining("latest end 23:59");
    }

    @Test
    void startInThePastOrNowIsRejected() {
        assertThatThrownBy(() -> ActivityWindowRules.validate(at(17, 11, 0), at(17, 14, 0), NOW))
                .isInstanceOf(BadDataInRequestException.class).hasMessageContaining("future");
        assertThatThrownBy(() -> ActivityWindowRules.validate(NOW, at(17, 14, 0), NOW))
                .isInstanceOf(BadDataInRequestException.class);
    }

    private static LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 10, day, hour, minute);
    }
}
