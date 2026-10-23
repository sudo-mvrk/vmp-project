package com.volunnear.application;

/** Q5a, Q5h. Only ACCEPTED applications use a spot and the volunteer's capacity. DECLINED ends an invitation. */
public enum ApplicationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    WITHDRAWN,
    DECLINED
}
