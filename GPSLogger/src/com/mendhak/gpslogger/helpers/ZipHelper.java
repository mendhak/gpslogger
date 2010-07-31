package com.mendhak.gpslogger.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipHelper
{
	private static final int BUFFER = 2048;

	private String[] files;
	private String zipFile;

	public ZipHelper(String[] files, String zipFile)
	{
		this.files = files;
		this.zipFile = zipFile;
	}

	public void Zip()
	{
		try
		{
			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(zipFile);

			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

			byte data[] = new byte[BUFFER];

			for (int i = 0; i < files.length; i++)
			{
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER);
				ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
				out.putNextEntry(entry);
				int count;
				while ((count = origin.read(data, 0, BUFFER)) != -1)
				{
					out.write(data, 0, count);
				}
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
