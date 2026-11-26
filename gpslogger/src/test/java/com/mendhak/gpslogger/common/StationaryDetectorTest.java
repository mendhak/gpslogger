package com.mendhak.gpslogger.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.test.filters.SmallTest;
import org.junit.Test;

@SmallTest
public class StationaryDetectorTest {

    // ~1 m of latitude in degrees
    private static final double M = 1.0 / 111_320.0;
    private static final double LAT = 51.5;
    private static final double LON = -0.12;

    private StationaryDetector.State feed(StationaryDetector.State s, double northMeters, int required) {
        return StationaryDetector.next(s, LAT + northMeters * M, LON, 50, required);
    }

    @Test
    public void firstPoint_SetsAnchor_NotStationary() {
        StationaryDetector.State s = feed(StationaryDetector.State.initial(), 0, 3);
        assertThat(s.hasAnchor, is(true));
        assertThat(s.count, is(1));
        assertThat(s.stationary, is(false));
    }

    @Test
    public void becomesStationary_AfterExactlyRequiredPointsWithinRadius() {
        StationaryDetector.State s = StationaryDetector.State.initial();
        s = feed(s, 0, 3);
        s = feed(s, 10, 3);
        assertThat(s.stationary, is(false));
        s = feed(s, -10, 3);
        assertThat(s.stationary, is(true));
        assertThat(s.count, is(3));
    }

    @Test
    public void jitterWithinRadius_StaysStationary() {
        StationaryDetector.State s = StationaryDetector.State.initial();
        double[] jitter = {0, 20, -30, 45, -45, 10, 49, -49, 0};
        for (double j : jitter) {
            s = feed(s, j, 3);
        }
        assertThat(s.stationary, is(true));
        assertThat(s.count, is(jitter.length));
    }

    @Test
    public void pointOutsideRadius_ExitsAndReanchors() {
        StationaryDetector.State s = StationaryDetector.State.initial();
        s = feed(s, 0, 3);
        s = feed(s, 5, 3);
        s = feed(s, 5, 3);
        assertThat(s.stationary, is(true));

        s = feed(s, 120, 3);
        assertThat(s.stationary, is(false));
        assertThat(s.count, is(1));
        assertThat(Math.abs(s.anchorLat - (LAT + 120 * M)) < 1e-9, is(true));
    }

    @Test
    public void movingTrack_NeverStationary() {
        StationaryDetector.State s = StationaryDetector.State.initial();
        for (int i = 0; i < 20; i++) {
            s = feed(s, i * 60, 3);
            assertThat(s.stationary, is(false));
        }
    }

    @Test
    public void requiredCountBelowMinimum_StillNeedsTwoPoints() {
        StationaryDetector.State s = feed(StationaryDetector.State.initial(), 0, 1);
        assertThat(s.stationary, is(false));
        s = feed(s, 5, 1);
        assertThat(s.stationary, is(true));

        // Moving away must not immediately count as stationary again
        s = feed(s, 500, 1);
        assertThat(s.stationary, is(false));
    }
}
