package com.volunnear.volunteer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** A weekly availability slot (Q13): every week on this day, from start to end, in the configured zone. */
@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerAvailability {
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
