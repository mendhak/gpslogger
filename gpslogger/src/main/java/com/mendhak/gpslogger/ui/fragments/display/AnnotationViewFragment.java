/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.ui.fragments.display;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mendhak.gpslogger.GpsLoggingService;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.IntentConstants;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;


public class AnnotationViewFragment extends GenericViewFragment implements View.OnClickListener, SimpleDialog.OnDialogResultListener {

    Context context;
    private static final Logger LOG = Logs.of(AnnotationViewFragment.class);
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private final Session session = Session.getInstance();

    List<ButtonObj> btnList = new ArrayList<>();

    @Override
    public void onClick(View v) {

    }

    static class ButtonObj {

        String color;
        Button btn;

        ButtonObj(Button btn)
        {
            this.btn = btn;
        }

        String getText()
        {
            return String.valueOf(btn.getText());
        }

        void setText(String str)
        {
            btn.setText(str);
        }

        public void setColor(String color) {
            this.color = color;
            if(color == null || "".compareTo(color) == 0){
                setColor("#808080");
            } else
            {
                btn.setBackgroundColor(Color.parseColor(this.color));
            }
        }

        public  String getColor()
        {
            return color;
        }

    }

    public AnnotationViewFragment() {

    }

    private void SaveSettings()
    {
        StringBuilder settings = new StringBuilder("{");
        settings.append("\"buttons\" : [");
        Iterator<ButtonObj> itr = btnList.iterator();
        int idx = 0;
        while (itr.hasNext())
        {
            ButtonObj btn = itr.next();
            settings.append("{");
            settings.append("\"idx\":").append(idx).append(",");
            settings.append("\"label\":\"").append(btn.getText()).append("\",");
            settings.append("\"color\":\"").append(btn.getColor()).append("\"");
            settings.append("}");
            idx++;
            if(itr.hasNext())
                settings.append(",");
        }
        settings.append("]");
        settings.append("}");
        preferenceHelper.setAnnotationButtonSettings(settings.toString());
    }

    private void LoadSettings()
    {
        String settings = preferenceHelper.getAnnotationButtonSettings();
        if(settings == null || "".compareTo(settings) == 0)
            return;

        try {
            JSONObject settingsObject = new JSONObject(settings);
            JSONArray buttonArrays = settingsObject.getJSONArray("buttons");
            for(int i = 0; i < buttonArrays.length() && i < btnList.size(); ++i)
            {
                JSONObject btnObj = buttonArrays.getJSONObject(i);
                int idx = btnObj.getInt("idx");
                String txt = btnObj.getString("label");
                String color = btnObj.getString("color");

                if(idx < 0 || idx >= btnList.size())
                {
                    LOG.error("Wrong index in settings: " + idx);
                    continue;
                }

                ButtonObj btn = btnList.get(idx);
                btn.setText(txt);
                btn.setColor(color);
            }

        } catch (Exception e)
        {
            LOG.error("Exception loading annotation settings: " + e.getMessage(), e);
        }
    }

    public static AnnotationViewFragment newInstance() {

        AnnotationViewFragment fragment = new AnnotationViewFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt("a_number", 1);

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if(which == BUTTON_POSITIVE) {

            Integer idx = null;
            try {
                idx = Integer.valueOf(dialogTag.substring(3));
            } catch (NumberFormatException ignored)
            {
            }
            if(idx == null || idx < 0 || idx >= btnList.size())
            {
                LOG.error("Could not find button " + dialogTag);
                return true;
            }

            ButtonObj btn = btnList.get(idx);
            String enteredText = extras.getString("annotations");
            String color = extras.getString("color");
            btn.setText(enteredText);
            btn.setColor(color);
            SaveSettings();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_annotation_view, container, false);


        if (getActivity() != null) {
            this.context = getActivity().getApplicationContext();

        }

        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b11)));
        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b12)));
        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b13)));
        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b21)));
        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b22)));
        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b23)));
        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b31)));
        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b32)));
        btnList.add(new ButtonObj(rootView.findViewById(R.id.an_b33)));

        int i = 0;
        AnnotationViewFragment fragment = this;
        for( ButtonObj btn : btnList) {
            btn.setText("Annotation " + i);
            btn.setColor("#808080");
            final int btnIdx = i;
            btn.btn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    SimpleFormDialog.build()
                            .title(R.string.annotation_edit_btn_title)
                            .msg(R.string.annotation_edit_btn_msg)
                            .fields(
                                    Input.plain("annotations")
                                            .hint(R.string.annotation_value)
                                            .text(String.valueOf(btn.getText())),
                                    Input.plain("color")
                                            .text(String.valueOf(btn.getColor()))
                                            .hint(R.string.annotation_color)
                                            .validatePattern("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
                                                             R.string.annotation_color_error)
                            ).show(fragment, "btn" + btnIdx);
                    return true;
                }
            });
            btn.btn.setOnClickListener((b) -> onBtnClick(btn));
            i++;

        }
        LoadSettings();

        return rootView;
    }

    private void onBtnClick(ButtonObj  btn) {
        LOG.info("Notification Annotation entered : " + btn.getText());
        Intent serviceIntent = new Intent(getContext(), GpsLoggingService.class);
        serviceIntent.putExtra(IntentConstants.SET_DESCRIPTION, btn.getText());
        ContextCompat.startForegroundService(getContext(),  serviceIntent);
    }

    void updateButtons(boolean enable)
    {
        for(ButtonObj btnObj : btnList)
        {
            btnObj.btn.setEnabled(enable);
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation){
        //updateButtons(!waitingForLocation.waiting);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus){
        //updateButtons(loggingStatus.loggingStarted);
    }
}
