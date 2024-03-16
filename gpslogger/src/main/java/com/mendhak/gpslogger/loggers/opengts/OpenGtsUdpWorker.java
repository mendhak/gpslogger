package com.mendhak.gpslogger.loggers.opengts;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.senders.GpxReader;
import com.mendhak.gpslogger.senders.opengts.OpenGTSManager;

import org.slf4j.Logger;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import de.greenrobot.event.EventBus;

public class OpenGtsUdpWorker extends Worker{

    public static final Logger LOG = Logs.of(OpenGtsUdpWorker.class);
    public OpenGtsUdpWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {

        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        String server = preferenceHelper.getOpenGTSServer();
        int port = Integer.valueOf(preferenceHelper.getOpenGTSServerPort());
        String accountName = preferenceHelper.getOpenGTSAccountName();
        String deviceId = preferenceHelper.getOpenGTSDeviceId();

        String gpxFilePath = getInputData().getString("gpxFilePath");
        String[] serializedLocations = getInputData().getStringArray("locations");

        try {
            if(!Strings.isNullOrEmpty(gpxFilePath)){
                List<SerializableLocation> locations = GpxReader.getPoints(new File(gpxFilePath));
                sendRAW(deviceId, accountName, server, port, locations.toArray(new SerializableLocation[0]));
            }
            else if (serializedLocations != null && serializedLocations.length > 0){
                SerializableLocation[] locations = new SerializableLocation[serializedLocations.length];
                for (int i = 0; i < serializedLocations.length; i++) {
                    locations[i] = Strings.deserializeFromJson(serializedLocations[i], SerializableLocation.class);
                }
                sendRAW(deviceId, accountName, server, port, locations);
            }
            EventBus.getDefault().post(new UploadEvents.OpenGTS().succeeded());
        }
        catch(Exception ex){
            LOG.error("Could not send to OpenGTS", ex);
            EventBus.getDefault().post(new UploadEvents.OpenGTS().failed("Could not send to OpenGTS", ex));
            return Result.failure();
        }
        return Result.success();
    }

    private void sendRAW(String id, String accountName, String server, int port, SerializableLocation[] locations) throws Exception {
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
