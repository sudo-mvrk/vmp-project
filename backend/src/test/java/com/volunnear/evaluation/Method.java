package com.volunnear.evaluation;

/** The compared methods (METHOD.md section 2, Q41, Q42). */
enum Method {
    RANDOM("random"),
    NEAREST("nearest"),
    SMART_A("smart-A"),
    SMART_B("smart-B");

    final String label;

    Method(String label) {
        this.label = label;
    }
}
