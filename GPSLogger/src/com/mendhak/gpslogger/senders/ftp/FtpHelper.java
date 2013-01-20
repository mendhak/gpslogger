/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
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


package com.mendhak.gpslogger.senders.ftp;

import com.mendhak.gpslogger.senders.IFileSender;

import java.io.File;
import java.util.List;

public class FtpHelper implements IFileSender
{

    @Override
    public void UploadFile(List<File> files)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean accept(File file, String s)
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
