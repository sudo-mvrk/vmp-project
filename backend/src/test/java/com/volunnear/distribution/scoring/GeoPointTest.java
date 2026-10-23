package com.volunnear.distribution.scoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeoPointTest {

    @Test
    void acceptsTheBoundsAndZero() {
        assertDoesNotThrow(() -> new GeoPoint(0, 0));
        assertDoesNotThrow(() -> new GeoPoint(90, 180));
        assertDoesNotThrow(() -> new GeoPoint(-90, -180));
    }

    @ParameterizedTest
    @CsvSource({"90.1, 0", "-90.1, 0", "0, 180.1", "0, -180.1", "NaN, 0", "0, NaN"})
    void rejectsCoordinatesOutsideTheRange(double lat, double lon) {
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(lat, lon));
    }
}
