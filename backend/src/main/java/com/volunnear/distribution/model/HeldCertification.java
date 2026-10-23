package com.volunnear.distribution.model;

import java.time.LocalDate;

/**
 * A certification a volunteer holds.
 *
 * @param validUntil last day the certification is valid (inclusive); null means it does not expire
 */
public record HeldCertification(long certificationId, LocalDate validUntil) {
    public boolean isValidOn(LocalDate date) {
        return validUntil == null || !validUntil.isBefore(date);
    }
}
