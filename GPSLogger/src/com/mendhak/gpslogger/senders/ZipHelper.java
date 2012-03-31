package com.mendhak.gpslogger.senders;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper
{
	private static final int BUFFER = 2048;

	private final String[] files;
	private final String zipFile;

	public ZipHelper(String[] files, String zipFile)
	{
		this.files = files;
		this.zipFile = zipFile;
	}

	public void Zip()
	{
		try
		{
			BufferedInputStream origin;
			FileOutputStream dest = new FileOutputStream(zipFile);

			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

			byte data[] = new byte[BUFFER];

            for (String f : files)
			{
				FileInputStream fi = new FileInputStream(f);
				origin = new BufferedInputStream(fi, BUFFER);
				ZipEntry entry = new ZipEntry(f.substring(f.lastIndexOf("/") + 1));
				out.putNextEntry(entry);
				int count;
				while ((count = origin.read(data, 0, BUFFER)) != -1)
				{
					out.write(data, 0, count);
				}
                out.closeEntry();
				origin.close();
			}

			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}

}
