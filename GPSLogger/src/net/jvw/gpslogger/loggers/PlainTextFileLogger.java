package net.jvw.gpslogger.loggers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import android.location.Location;

import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.loggers.IFileLogger;

/**
 * Writes a comma separated plain text file.<br/>
 * First line of file is a header with the logged fields: time,lat,lon,elevation,accuracy,bearing,speed
 *
 * @author Jeroen van Wilgenburg
 */
public class PlainTextFileLogger implements IFileLogger {

	private File file;
	private boolean useSatelliteTime;

	public PlainTextFileLogger(File file, boolean useSatelliteTime) {
		this.file = file;
		this.useSatelliteTime = useSatelliteTime;
	}

	@Override
	public void Write(Location loc) throws Exception {
		if (!file.exists()) {
			file.createNewFile();

			FileOutputStream writer = new FileOutputStream(file, true);
			BufferedOutputStream output = new BufferedOutputStream(writer);
			String header = "time,lat,lon,elevation,accuracy,bearing,speed\n";
			output.write(header.getBytes());
			output.flush();
			output.close();
		}

		FileOutputStream writer = new FileOutputStream(file, true);
		BufferedOutputStream output = new BufferedOutputStream(writer);

		Date now;

		if (useSatelliteTime) {
			now = new Date(loc.getTime());
		} else {
			now = new Date();
		}

		String dateTimeString = Utilities.GetIsoDateTime(now);

		String outputString = String.format("%s,%f,%f,%f,%f,%f,%f\n", dateTimeString,
									loc.getLatitude(),
									loc.getLongitude(),
									loc.getAltitude(),
									loc.getAccuracy(),
									loc.getBearing(),
									loc.getSpeed());
		
		output.write(outputString.getBytes());
		output.flush();
		output.close();
	}

	@Override
	public void Annotate(String description, Location loc) throws Exception {
	    // TODO Auto-generated method stub
	    
	}

}
