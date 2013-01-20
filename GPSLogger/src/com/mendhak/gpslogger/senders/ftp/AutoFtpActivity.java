package com.mendhak.gpslogger.senders.ftp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;



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


public class AutoFtpActivity extends Activity implements IActionListener
{

    private final Handler handler = new Handler();

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        FtpHelper helper = new FtpHelper(this);
        helper.TestFtp();
    }


    private final Runnable successfullySent = new Runnable()
    {
        public void run()
        {
            SuccessfulSending();
        }
    };

    private final Runnable failedSend = new Runnable()
    {

        public void run()
        {
            FailureSending();
        }
    };

    private void FailureSending()
    {
        Utilities.HideProgress();
        Utilities.MsgBox(getString(R.string.sorry), "FTP Test Failed", this);
    }

    private void SuccessfulSending()
    {
        Utilities.HideProgress();
        Utilities.MsgBox(getString(R.string.success),
                "FTP Test Succeeded", this);
    }

    @Override
    public void OnComplete()
    {

        handler.post(successfullySent);
    }

    @Override
    public void OnFailure()
    {
        handler.post(failedSend);
    }
}