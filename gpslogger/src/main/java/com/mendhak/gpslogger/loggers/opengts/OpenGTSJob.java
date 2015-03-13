package com.mendhak.gpslogger.loggers.opengts;

import com.mendhak.gpslogger.common.OpenGTSClient;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.LoggerFactory;

public class OpenGTSJob extends Job {

    String server;
    int port ;
    String accountName ;
    String path ;
    String deviceId ;
    String communication;
    SerializableLocation[] locations;
    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OpenGTSJob.class.getSimpleName());

    public OpenGTSJob(String server, int port, String accountName, String path, String deviceId, String communication, SerializableLocation[] locations){
        super(new Params(1).requireNetwork().persist());

        this.server = server;
        this.port = port;
        this.accountName = accountName;
        this.path = path;
        this.deviceId = deviceId;
        this.communication = communication;
        this.locations = locations;
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        tracer.debug("Running OpenGTS Job");

        OpenGTSClient openGTSClient = new OpenGTSClient(server, port, path);
        if(communication.equalsIgnoreCase("UDP")){
            openGTSClient.sendRAW(deviceId, accountName, locations);
        }
        else{
            openGTSClient.sendHTTP(deviceId, accountName, locations);
        }

        EventBus.getDefault().post(new UploadEvents.OpenGTSLoggedEvent(true));
    }

    @Override
    protected void onCancel() {
        EventBus.getDefault().post(new UploadEvents.OpenGTSLoggedEvent(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not send to OpenGTS", throwable);
        return false;
    }
}
