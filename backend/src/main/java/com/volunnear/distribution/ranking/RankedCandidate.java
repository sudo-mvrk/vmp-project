package com.volunnear.distribution.ranking;

import com.volunnear.distribution.scoring.ScoreBreakdown;

/**
 * One entry of a recommendation list: the id of the volunteer or activity and its score with breakdown.
 */
public record RankedCandidate(long id, ScoreBreakdown score) {
}
