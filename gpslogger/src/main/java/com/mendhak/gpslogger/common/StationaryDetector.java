package com.mendhak.gpslogger.common;

/**
 * Decides whether the user is stationary for smart stationary mode.
 * <p>
 * The first point becomes an anchor. Each following point within the radius of the anchor
 * adds to the count; once the count reaches the required number the user is stationary.
 * A point outside the radius ends stationary mode and becomes the new anchor.
 * <p>
 * Kept free of Android classes so it can be unit tested.
 */
public final class StationaryDetector {

    public static final int MIN_REQUIRED_COUNT = 2;

    private StationaryDetector() { }

    public static final class State {
        public final boolean stationary;
        public final boolean hasAnchor;
        public final double anchorLat;
        public final double anchorLon;
        public final int count;

        public State(boolean stationary, boolean hasAnchor, double anchorLat, double anchorLon, int count) {
            this.stationary = stationary;
            this.hasAnchor = hasAnchor;
            this.anchorLat = anchorLat;
            this.anchorLon = anchorLon;
            this.count = count;
        }

        public static State initial() {
            return new State(false, false, 0, 0, 0);
        }
    }

    public static double distanceFromAnchor(State state, double lat, double lon) {
        return Maths.calculateDistance(state.anchorLat, state.anchorLon, lat, lon);
    }

    public static State next(State state, double lat, double lon, double radiusMeters, int requiredCount) {
        // The count includes the anchor, and one point alone can't show the user has stopped
        int required = Math.max(MIN_REQUIRED_COUNT, requiredCount);

        if (state == null || !state.hasAnchor) {
            return new State(false, true, lat, lon, 1);
        }

        if (distanceFromAnchor(state, lat, lon) <= radiusMeters) {
            int count = state.count + 1;
            return new State(state.stationary || count >= required, true, state.anchorLat, state.anchorLon, count);
        }

        // Moved outside the radius: start a new cluster here
        return new State(false, true, lat, lon, 1);
    }
}
