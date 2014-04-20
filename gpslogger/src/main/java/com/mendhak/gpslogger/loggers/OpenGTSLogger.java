/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger.loggers;

import android.location.Location;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.OpenGTSClient;


/**
 * Send locations directly to an OpenGTS server <br/>
 *
 * @author Francisco Reynoso
 */
public class OpenGTSLogger implements IFileLogger {

    protected final String name = "OpenGTS";

    public OpenGTSLogger() {
    }

    @Override
    public void Write(Location loc) throws Exception {


        String server = AppSettings.getOpenGTSServer();
        int port = Integer.parseInt(AppSettings.getOpenGTSServerPort());
        String path = AppSettings.getOpenGTSServerPath();
        String deviceId = AppSettings.getOpenGTSDeviceId();

        IActionListener al = new IActionListener() {
            @Override
            public void OnComplete() {
            }

            @Override
            public void OnFailure() {
            }
        };

        OpenGTSClient openGTSClient = new OpenGTSClient(server, port, path, al, null);
        openGTSClient.sendHTTP(deviceId, loc);

    }

    @Override
    public void Annotate(String description, Location loc) throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public String getName() {
        return name;
    }

}
