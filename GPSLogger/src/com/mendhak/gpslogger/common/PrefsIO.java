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

package com.mendhak.gpslogger.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.app.Activity;
import com.mendhak.gpslogger.GpsSettingsActivity;
import net.kataplop.gpslogger.R;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class PrefsIO {

    private Context context;
    private SharedPreferences sharedPrefs;
    private String defFileName;     // Name only
    private String defPath;         // Path only
    private String curFileName;     // FQDN
    private String extension;
    private String separator;
    private String commentPrefix;

    public final int ACTIVITY_CHOOSE_FILE = 2;

    private String filter="[^a-zA-Z0-9]";

    public PrefsIO(Context Cntx, SharedPreferences Prefs, String FileName, String Path, String Ext, String Separ, String Comm) {
        context=Cntx;
        sharedPrefs=Prefs;
        defFileName=FileName;
        defPath=Path;
        extension=Ext;
        separator=Separ;
        commentPrefix=Comm;
        curFileName=defFileName;
//        curPath=defPath;
    }

    public PrefsIO(Context Cntx, SharedPreferences Prefs, String FileName, String Path) {
        context=Cntx;
        sharedPrefs=Prefs;
        defFileName=FileName;
        curFileName=defFileName;
//        curPath=defPath;
        defPath=Path;
        extension="csv";
        separator=";";
        commentPrefix="#";
    }

    public void SetCurFileName(String filename) {
        curFileName=filename;
    }

    public void ExportFile() {
        Object val=null;
        String str="";
        String type="unknown";
        int ind=0;
        Date date=new Date();
        String strdate;
        SimpleDateFormat dateformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        strdate=dateformat.format(date);
        Utilities.LogDebug("Trying to export settings to file: "+curFileName);
        File mySetFile=new File(curFileName);

        try {
            if(!mySetFile.exists()) mySetFile.createNewFile();
                else {
//                    Toast.makeText(context, R.string.ExportFailed, Toast.LENGTH_LONG).show();
                      Utilities.LogDebug("File exists, asking another filename");
                      ReAskFileName();
                      return;
                }
            FileWriter fw = new FileWriter(mySetFile);
            PrintWriter pw = new PrintWriter(fw);
            Map<String,?> prefsMap = sharedPrefs.getAll();
            pw.println(commentPrefix+" Settings Dump "+strdate);
            for(Map.Entry<String,?> entry : prefsMap.entrySet())
            {
                val=entry.getValue();
                str="";
                str+=val.getClass();
                ind=str.lastIndexOf(".");
                if(ind>0) type=str.substring(ind+1);
                pw.println(entry.getKey() + separator + val.toString() + separator + type);
            }
            pw.close();
            fw.close();
            Toast.makeText(context, R.string.ExportSuccess, Toast.LENGTH_LONG).show();
        }
        catch(Throwable t) {
            Toast.makeText(context, "Exception: "+t.toString(), Toast.LENGTH_LONG).show();
            Toast.makeText(context, R.string.ExportFailed, Toast.LENGTH_LONG).show();
        }
    }

    public void ImportFile() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        String str="";
        String strBoolean="Boolean";
        String strString="String";
        String[] params;
        String regexp="["+separator+"]";

        Utilities.LogDebug("Trying to import settings from file: "+curFileName);

        if(curFileName.length()>0) {
            File mySetFile=new File(curFileName);
            try {
                if(mySetFile.exists()) {
                    FileReader fr = new FileReader(mySetFile);
                    BufferedReader br = new BufferedReader(fr);
                    while( (str=br.readLine()) != null ) {
                        if(str.startsWith(commentPrefix)) continue;
                        params=str.split(regexp);
                        if(params[2].endsWith(strBoolean)) editor.putBoolean(params[0], Boolean.parseBoolean(params[1]) );
                        else if(params[2].endsWith(strString)) editor.putString(params[0], params[1] );
                        editor.commit();
                    }
                    br.close();
                    Toast.makeText(context, R.string.ImportSuccess, Toast.LENGTH_LONG).show();
                    Intent settingsActivity = new Intent(context, GpsSettingsActivity.class);
                    context.startActivity(settingsActivity);
                }
                else Toast.makeText(context, R.string.ImportFailed, Toast.LENGTH_LONG).show();
            }
            catch(Throwable t) {
                Toast.makeText(context, "Exception: "+t.toString(), Toast.LENGTH_LONG).show();
                Toast.makeText(context, R.string.ImportFailed, Toast.LENGTH_LONG).show();
            }
        }
        else Toast.makeText(context, R.string.ImportFailed, Toast.LENGTH_LONG).show();
    }

    private void BrowseFile() {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("file/*");
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(chooseFile, 0);
        if(list.size()>0) {
            intent = Intent.createChooser(chooseFile, context.getString(R.string.ChooseFile));
            Utilities.LogDebug("Trying to start file browser...");
            ((Activity)context).startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
        }
        else Toast.makeText(context, context.getString(R.string.NoFSBrowser), Toast.LENGTH_SHORT).show();
    }

    public Dialog ChooseFileDialog() {
        File myDir = new File(defPath);
        if(!myDir.exists()) return null;

        Utilities.LogDebug("Asking user the file to use for import of settings");

        File[] enumeratedFiles = myDir.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith("."+extension);
                    }
                });
        final int len=enumeratedFiles.length;
        List<String> fileList = new ArrayList<String>(len);
        for (File f : enumeratedFiles)
        {
            fileList.add(f.getName());
        }
        fileList.add(context.getString(R.string.Browse));
        final String[] files = fileList.toArray(new String[fileList.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.SelectFile));

        builder.setItems(files, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if(item<len) {
                    curFileName=defPath+File.separator+files[item];
                    ImportFile();
                }
                else BrowseFile();
            }
        });
        builder.setCancelable(true);
        return builder.create();
    }

    private void ReAskFileName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.FileExists);
        builder.setMessage(R.string.AskAnotherFile);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AskFileName();
            }
        });
        builder.show();
    }

    public void AskFileName() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.AskFileName);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        int pos=curFileName.lastIndexOf(File.separator)+1;
        String filename="";
        if(pos>0) filename=curFileName.substring(pos);
        pos=filename.lastIndexOf(extension);
        if(pos>1) input.setText(filename.substring(0,pos-1));
            else input.setText(defFileName);

        Utilities.LogDebug("Asking user the filename to use for export of settings");

        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String str = input.getText().toString();
                String fname="";
                if(str.length()>0) fname=str.replaceAll(filter,"");
                if(fname.length()!=0) {
                    curFileName=defPath+File.separator+fname+"."+extension;
                    ExportFile();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
