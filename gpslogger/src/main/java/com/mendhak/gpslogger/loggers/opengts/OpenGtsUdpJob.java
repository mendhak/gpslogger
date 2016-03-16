package com.mendhak.gpslogger.loggers.opengts;

import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.opengts.OpenGTSManager;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.net.*;


public class OpenGtsUdpJob extends Job {

    String server;
    int port ;
    String accountName ;
    String path ;
    String deviceId ;
    String communication;
    SerializableLocation[] locations;
    private static final Logger LOG = Logs.of(OpenGtsUdpJob.class);

    public OpenGtsUdpJob(String server, int port, String accountName, String path, String deviceId, String communication, SerializableLocation[] locations){
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

        LOG.debug("Running OpenGTS Job");
        sendRAW(deviceId, accountName, locations);
        EventBus.getDefault().post(new UploadEvents.OpenGTS().succeeded());
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        LOG.error("Could not send to OpenGTS", throwable);
        EventBus.getDefault().post(new UploadEvents.OpenGTS().failed("Could not send to OpenGTS", throwable));
        return false;
    }



    public void sendRAW(String id, String accountName, SerializableLocation[] locations) throws Exception {
        for (SerializableLocation loc : locations) {
            if(Strings.isNullOrEmpty(accountName)){
                accountName = id;
            }
            String message = accountName + "/" + id + "/" + OpenGTSManager.gprmcEncode(loc);
            DatagramSocket socket = new DatagramSocket();
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(server), port);
            LOG.debug("Sending UDP " + message);
            socket.send(packet);
            socket.close();
        }
    }





}
