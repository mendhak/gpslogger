package com.mendhak.gpslogger.common;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.internal.view.WindowCallbackWrapper;
import android.view.KeyEvent;
import android.view.Window;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

public class AppCompatActivityMenuKeyInterceptor {

    private static final String FIELD_NAME_DELEGATE = "mDelegate";
    private static final String FIELD_NAME_WINDOW = "mWindow";

    public static void intercept(AppCompatActivity appCompatActivity) {
        new AppCompatActivityMenuKeyInterceptor(appCompatActivity);
    }

    private AppCompatActivityMenuKeyInterceptor(AppCompatActivity activity) {
        try {
            Field mDelegateField = AppCompatActivity.class.getDeclaredField(FIELD_NAME_DELEGATE);
            mDelegateField.setAccessible(true);
            Object mDelegate = mDelegateField.get(activity);

            Field mWindowField = mDelegate.getClass().getSuperclass().getSuperclass().getDeclaredField(FIELD_NAME_WINDOW);
            mWindowField.setAccessible(true);
            Window mWindow = (Window) mWindowField.get(mDelegate);

            Window.Callback mOriginalWindowCallback = mWindow.getCallback();
            mWindow.setCallback(new AppCompatWindowCallbackCustom(mOriginalWindowCallback, activity));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private class AppCompatWindowCallbackCustom extends WindowCallbackWrapper {

        private WeakReference<AppCompatActivity> mActivityWeak;

        public AppCompatWindowCallbackCustom(Window.Callback wrapped, AppCompatActivity appCompatActivity) {
            super(wrapped);

            mActivityWeak = new WeakReference<AppCompatActivity>(appCompatActivity);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            final int keyCode = event.getKeyCode();
            final int action = event.getAction();

            AppCompatActivity appCompatActivity = mActivityWeak.get();

            if (appCompatActivity != null) {
                if (keyCode == KeyEvent.KEYCODE_MENU && action == KeyEvent.ACTION_DOWN) {
                    if (mActivityWeak.get().onKeyDown(event.getKeyCode(), event))
                        return true;
                } else if (keyCode == KeyEvent.KEYCODE_MENU && action == KeyEvent.ACTION_UP) {
                    if (mActivityWeak.get().onKeyUp(event.getKeyCode(), event))
                        return true;
                }
            }

            return super.dispatchKeyEvent(event);
        }
    }
}