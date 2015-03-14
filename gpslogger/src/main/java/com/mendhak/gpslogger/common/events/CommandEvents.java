package com.mendhak.gpslogger.common.events;


public class CommandEvents {
    /**
     * Requests starting or stopping the logging service.
     * Called from the fragment button click events
     */
    public static class RequestToggle {
    }

    /**
     * Requests starting the logging service
     */
    public static class RequestStartStop {
        public boolean start;
        public RequestStartStop(boolean start){
            this.start = start;
        }
    }
}
