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

/*
 * Copyright Geeksville Industries LLC, a California limited liability corporation.
 * Copyright Marc Poulhiès <dkm@kataplop.net>
 * Code is also published with GPL2
 * http://github.com/geeksville/Gaggle
 */

package com.mendhak.gpslogger.loggers;

import android.location.Location;
import android.util.Base64;
import android.util.Log;
import com.mendhak.gpslogger.common.Utilities;

import java.io.*;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import java.text.SimpleDateFormat;

/**
 * Writes an IGC file suitable for most flight related
 * tools.
 */
public class IgcFileLogger implements IFileLogger
{

    private OutputStream output = null;
    private SignatureOutputStream sos = null;
    private String privateKeyB64 = null;
    private Signature sig;

    private File file;

    public static final String name = "IGC";
    private Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

    private String pilotName;
    private String gliderType;
    private String pilotId;
    private String versionString;

    private static HashMap<String, IgcFileLogger> instances = new HashMap<String, IgcFileLogger>();

    public static IgcFileLogger getIgcFileLogger(File file, final String privateKey) throws IOException {
        final String fname = file.getAbsolutePath();
        if(instances.containsKey(fname) && privateKey.equals(instances.get(fname).privateKeyB64)) {
            return instances.get(fname);
        } else {
            IgcFileLogger ifl = new IgcFileLogger(file, privateKey);
            instances.put(fname, ifl);
            return ifl;
        }
    }


    private boolean initSignature(){
        try {
            PrivateKey pk = getPrivateKey();
            sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(pk);
            return true;
        } catch (InvalidKeySpecException e) {
        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidKeyException e) {
        }
        Utilities.LogDebug("Error when initializing IGC signature");
        return false;
    }

    private IgcFileLogger(File file, String privateKeyB64) throws IOException {
        this.privateKeyB64 = privateKeyB64;
        boolean alreadyExists = false;
        this.file = file;

//        final boolean signatureEnabled = initSignature();
        final boolean signatureEnabled = false;
        File previousContent = null;

        if (!file.exists()) {
            file.createNewFile();
        } else {
            alreadyExists = true;

            if (signatureEnabled) {
                previousContent = File.createTempFile(file.getName(), null, file.getParentFile());

                if (!file.renameTo(previousContent)){
                    Utilities.LogDebug("error when moving orig file");
                } else {
                    file.createNewFile();
                }
            }
        }

        FileOutputStream writer = new FileOutputStream(file, true);

        if (signatureEnabled){
            sos = new SignatureOutputStream(writer, sig);
            output = sos;

            if (previousContent != null){
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(previousContent)));
                String line = in.readLine();

                while(line != null){
                    if (!line.startsWith("G")){
                        output.write(line.getBytes());
                        output.write("\r\n".getBytes());
                    }
                    line = in.readLine();
                }
                previousContent.delete();
            }
        } else {
            sos = null;
            sig = null;
            output = new BufferedOutputStream(writer);
        }

        // appending to existing file, skip header and signature stuff
        if (alreadyExists) {
            Utilities.LogDebug("skipping IGC header as file already exists. Appending, no matter what...");
            return;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("AXGL1\r\n");

        SimpleDateFormat sdf = new SimpleDateFormat("FFMMyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        String s = sdf.format(new Date());
/*
            String dstr = String.format(Locale.US, "HFDTE%02d%02d%02d",
                    cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1,
                    (cal.get(Calendar.YEAR) - 1900) % 100);*/

        sb.append("HFDTE" + s + "\r\n");
        sb.append("HFFXA100\r\n"); // accuracy in meters - required
        sb.append("HFPLTPILOT:" + pilotName + "\r\n"); // pilot (required)
        sb.append("HFGTYGLIDERTYPE:" + gliderType + "\r\n"); // glider type (required)
        sb.append("HFGIDGLIDERID:" + pilotId + "\r\n"); // glider ID required
        sb.append("HFDTM100GPSDATUM:WGS84" + "\r\n"); // datum required - must be wgs84
        sb.append("HFGPSGPS:" + android.os.Build.MODEL + "\r\n"); // info on gps
        // manufacturer
        sb.append("HFRFWFIRMWAREVERSION:" + versionString + "\r\n"); // sw version of app
        sb.append("HFRHWHARDWAREVERSION:" + versionString + "\r\n"); // hw version
        sb.append("HFFTYFRTYPE:GpsLogger" + "\r\n"); // required: manufacturer
        sb.append("I013638GSP" + "\r\n");

        output.write(sb.toString().getBytes());
    }

    private PrivateKey getPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory fac = KeyFactory.getInstance("RSA");
		EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKeyB64, Base64.DEFAULT));
		return fac.generatePrivate(privKeySpec);
	}

    public class SignatureOutputStream extends OutputStream {

		private OutputStream target;
		private Signature sig;

		/**
		 * creates a new SignatureOutputStream which writes to
		 * a target OutputStream and updates the Signature object.
		 */
		public SignatureOutputStream(OutputStream target, Signature sig) {
			this.target = target;
			this.sig = sig;
		}

		@Override
		public void write(int b) throws IOException {
			write(new byte[] { (byte) b });
		}

		@Override
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}

		@Override
		public void write(byte[] b, int offset, int len) throws IOException {
			target.write(b, offset, len);
			try {
				for (int i = 0; i < len; i++)
					if (b[offset+i] != '\r' && b[offset+i] != '\n')
						sig.update(b, offset + i, 1);
			} catch (SignatureException ex) {
				throw new IOException(ex);
			}
		}

		@Override
		public void flush() throws IOException {
			target.flush();
		}

		@Override
		public void close() throws IOException {
			target.close();
		}
	}

    private static String degreeStr(double degIn, boolean isLatitude) {
		boolean isPos = degIn >= 0;
		char dirLetter = isLatitude ? (isPos ? 'N' : 'S') : (isPos ? 'E' : 'W');

		degIn = Math.abs(degIn);
		double minutes = 60 * (degIn - Math.floor(degIn));
		degIn = Math.floor(degIn);
		int minwhole = (int) minutes;
		int minfract = (int) ((minutes - minwhole) * 1000);

		// DDMMmmmN(or S) latitude
		// DDDMMmmmE(or W) longitude
		String s = String.format(Locale.US, (isLatitude ? "%02d" : "%03d")
				+ "%02d%03d%c", (int) degIn, minwhole, minfract, dirLetter);
		return s;
	}
    @Override
    public void Write(Location loc) throws Exception
    {
        String dateTimeString = Utilities.GetIsoDateTime(new Date(loc.getTime()));
        cal.setTimeInMillis(loc.getTime());
        final int hours = cal.get(Calendar.HOUR_OF_DAY);

        String line = String.format(Locale.US, "B%02d%02d%02d%s%s%c%05d%05d%03d\r\n", hours,
                cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND),
                degreeStr(loc.getLatitude(), true),
                degreeStr(loc.getLongitude(), false),
                'A', (int) loc.getAltitude(),
                (int) loc.getAltitude(),
                (int) (loc.getSpeed()*3.6) // convert m/s -> km/h
        );

        output.write(line.getBytes());
//        output.flush();
    }

    @Override
    public void close() throws Exception {
        instances.remove(this.file.getAbsolutePath());

        // sect 3.2, G=security record
        if (sig != null) {
            StringBuffer sb = new StringBuffer();

            try {
                final byte[] signature = sig.sign();
                final String sigStr = Base64.encodeToString(signature, Base64.DEFAULT).replaceAll("[\\r\\n]", "");

                for (int i = 0; i < sigStr.length() / 75; i++) {
                    sb.append("G" + sigStr.substring(i * 75, i * 75 + 75) + "\r\n");
                }
                if (sigStr.length() % 75 > 0) {
                    sb.append("G" + sigStr.substring(((int) (sigStr.length() / 75)) * 75) + "\r\n");
                }
            } catch (SignatureException e) {
                Utilities.LogDebug("IGC : Error when signing...");
                Log.e("IGC", "Error when signing", e);
                sb = new StringBuffer();
                sb.append("GGaggleFailedToSign\r\n");
            }
            output.write(sb.toString().getBytes());
        }
        output.close();
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
