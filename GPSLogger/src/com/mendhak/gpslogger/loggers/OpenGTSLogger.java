package com.mendhak.gpslogger.loggers;

import android.location.Location;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.OpenGTSClient;

import java.util.Date;


/**
 * Send locations directly to an OpenGTS server <br/>
 *
 * @author Francisco Reynoso
 */
public class OpenGTSLogger implements IFileLogger
{

    private boolean useSatelliteTime;
    protected final String name = "OpenGTS";

    public OpenGTSLogger(boolean useSatelliteTime)
    {
        this.useSatelliteTime = useSatelliteTime;
    }

    @Override
    public void Write(Location loc) throws Exception
    {

        Location nLoc = new Location(loc);
        if (!useSatelliteTime)
        {
            Date now = new Date();
            nLoc.setTime(now.getTime());
        }

        String server = AppSettings.getOpenGTSServer();
        int port = Integer.parseInt(AppSettings.getOpenGTSServerPort());
        String path = AppSettings.getOpenGTSServerPath();
        String deviceId = AppSettings.getOpenGTSDeviceId();

        IActionListener al = new IActionListener()
        {
            @Override
            public void OnComplete()
            {
            }

            @Override
            public void OnFailure()
            {
            }
        };

        OpenGTSClient openGTSClient = new OpenGTSClient(server, port, path, al, null);
        openGTSClient.sendHTTP(deviceId, loc);

    }

    @Override
    public void Annotate(String description, Location loc) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName()
    {
        return name;
    }

}
