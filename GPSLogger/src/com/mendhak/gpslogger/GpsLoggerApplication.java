package com.mendhak.gpslogger;

import java.util.Locale;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.text.TextUtils;

@SuppressWarnings("ucd")
public class GpsLoggerApplication extends Application
{
  @Override
  public void onCreate()
  {
    updateLanguage(this);
    super.onCreate();
  }

  public static void updateLanguage(Context ctx)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    String lang = prefs.getString("locale_override", "");
    updateLanguage(ctx, lang);
  }

  public static void updateLanguage(Context ctx, String lang)
  {
    Configuration cfg = new Configuration();
    if (!TextUtils.isEmpty(lang))
    {
      cfg.locale = new Locale(lang);
    }
    else
    {
      cfg.locale = Locale.getDefault();
    }

    ctx.getResources().updateConfiguration(cfg, null);
  }
}

