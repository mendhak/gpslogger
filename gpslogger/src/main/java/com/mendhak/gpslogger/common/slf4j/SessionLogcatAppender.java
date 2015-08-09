package com.mendhak.gpslogger.common.slf4j;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.mendhak.gpslogger.common.FifoDeque;
import com.mendhak.gpslogger.common.events.ServiceEvents;

public class SessionLogcatAppender extends AppenderBase<ILoggingEvent> {

    public static FifoDeque<ServiceEvents.StatusMessage> Statuses = new FifoDeque<>(25);

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if(eventObject.getLevel().toInt() < Level.INFO.toInt()){ return; }

        Statuses.add(new ServiceEvents.StatusMessage(eventObject.getMessage(), eventObject.getLevel() != Level.ERROR));
    }
}
