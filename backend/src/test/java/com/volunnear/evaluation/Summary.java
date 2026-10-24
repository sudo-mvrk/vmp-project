package com.volunnear.evaluation;

import java.util.Arrays;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Mean, sample standard deviation and the half-width of the 95% confidence interval of the mean
 * (1.96 s / sqrt(n), METHOD.md section 6) over the defined values; n counts only those.
 */
record Summary(double mean, double std, double ci95, int n) {
    static Summary of(double[] values) {
        double[] defined = Arrays.stream(values).filter(x -> !Double.isNaN(x)).toArray();
        int n = defined.length;
        if (n == 0) {
            return new Summary(Double.NaN, Double.NaN, Double.NaN, 0);
        }
        double mean = Arrays.stream(defined).average().orElseThrow();
        double std = n == 1 ? 0 : Math.sqrt(Arrays.stream(defined).map(x -> (x - mean) * (x - mean)).sum() / (n - 1));
        return new Summary(mean, std, 1.96 * std / Math.sqrt(n), n);
    }

    static <T> Summary of(List<T> items, ToDoubleFunction<T> value) {
        return of(items.stream().mapToDouble(value).toArray());
    }
}
