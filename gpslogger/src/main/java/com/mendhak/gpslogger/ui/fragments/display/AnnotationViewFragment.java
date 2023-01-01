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
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.dd.processbutton.iml.ActionProcessButton;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.events.CommandEvents;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;
import eltos.simpledialogfragment.SimpleDialog;
import eltos.simpledialogfragment.form.ColorField;
import eltos.simpledialogfragment.form.Input;
import eltos.simpledialogfragment.form.SimpleFormDialog;


public class AnnotationViewFragment extends GenericViewFragment implements SimpleDialog.OnDialogResultListener {

    Context context;
    private static final Logger LOG = Logs.of(AnnotationViewFragment.class);
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    List<ButtonWrapper> buttonList = new ArrayList<>();
    ButtonWrapper selectedButton;


    static class ButtonWrapper {

        String color;
        String text;
        ActionProcessButton actionButton;

        ButtonWrapper(ActionProcessButton actionButton) {
            this.actionButton = actionButton;
            this.actionButton.setMode(ActionProcessButton.Mode.ENDLESS);
        }

        String getText() {
            return String.valueOf(this.text);
        }

        void setText(String str) {
            this.text = Strings.cleanDescriptionForJson(str);
            this.actionButton.setText(this.text);
        }

        public void setColor(String color) {
            this.color = color;
            if (color == null || "".compareTo(color) == 0) {
                setColor("#808080");
            } else {
                this.actionButton.setBackgroundColor(Color.parseColor(this.color));
            }
        }

        public String getColor() {
            return this.color;
        }
    }

    private void saveSettings() {
        StringBuilder settings = new StringBuilder("{");
        settings.append("\"buttons\" : [");
        Iterator<ButtonWrapper> itr = buttonList.iterator();
        int idx = 0;
        while (itr.hasNext()) {
            ButtonWrapper btn = itr.next();
            settings.append("{");
            settings.append("\"idx\":").append(idx).append(",");
            settings.append("\"label\":\"").append(Strings.cleanDescriptionForJson(btn.getText())).append("\",");
            settings.append("\"color\":\"").append(btn.getColor()).append("\"");
            settings.append("}");
            idx++;
            if (itr.hasNext()) {
                settings.append(",");
            }
        }
        settings.append("]");
        settings.append("}");
        preferenceHelper.setAnnotationButtonSettings(settings.toString());
    }

    private void loadSettings() {
        String settings = preferenceHelper.getAnnotationButtonSettings();
        if (settings == null || "".compareTo(settings) == 0) {
            return;
        }

        try {
            JSONObject settingsObject = new JSONObject(settings);
            JSONArray buttonArrays = settingsObject.getJSONArray("buttons");
            for (int i = 0; i < buttonArrays.length() && i < buttonList.size(); ++i) {
                JSONObject btnObj = buttonArrays.getJSONObject(i);
                int idx = btnObj.getInt("idx");
                String txt = btnObj.getString("label");
                String color = btnObj.getString("color");

                if (idx < 0 || idx >= buttonList.size()) {
                    LOG.error("Wrong index in settings: " + idx);
                    continue;
                }

                ButtonWrapper btn = buttonList.get(idx);
                btn.setText(txt);
                btn.setColor(color);
            }

        } catch (Exception e) {
            LOG.error("Exception loading annotation settings: " + e.getMessage(), e);
        }
    }

    public static AnnotationViewFragment newInstance() {
        AnnotationViewFragment fragment = new AnnotationViewFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which == BUTTON_POSITIVE) {

            Integer idx = null;
            try {
                idx = Integer.valueOf(dialogTag.substring(3));
            } catch (NumberFormatException ignored) {

            }

            if (idx == null || idx < 0 || idx >= buttonList.size()) {
                LOG.error("Could not find button " + dialogTag);
                return true;
            }

            ButtonWrapper buttonWrapper = buttonList.get(idx);
            String enteredText = extras.getString("annotations");
            int color = extras.getInt("color");
            buttonWrapper.setText(enteredText);
            buttonWrapper.setColor(Strings.getHexColorCodeFromInt(color));
            saveSettings();
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

        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b11)));
        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b12)));
        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b13)));
        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b21)));
        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b22)));
        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b23)));
        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b31)));
        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b32)));
        buttonList.add(new ButtonWrapper(rootView.findViewById(R.id.an_b33)));

        int i = 0;
        AnnotationViewFragment fragment = this;
        for (ButtonWrapper buttonWrapper : buttonList) {
            buttonWrapper.setText("Annotation " + i);
            buttonWrapper.setColor("#808080");
            final int btnIdx = i;
            buttonWrapper.actionButton.setOnLongClickListener(v -> {
                SimpleFormDialog.build()
                        .pos(R.string.ok)
                        .neg(R.string.cancel)
                        .title(R.string.annotation_edit_button_label)
                        .fields(
                                Input.plain("annotations")
                                        .hint(R.string.letters_numbers)
                                        .text(String.valueOf(buttonWrapper.getText())),
                                ColorField.picker("color")
                                        .label(R.string.annotation_edit_button_color).allowCustom(true)
                                        .color(Color.parseColor(buttonWrapper.getColor()))
                        ).show(fragment, "btn" + btnIdx);
                return true;
            });
            buttonWrapper.actionButton.setOnClickListener((b) -> onBtnClick(buttonWrapper));
            i++;
        }
        loadSettings();

        return rootView;
    }

    private void onBtnClick(ButtonWrapper wrapper) {
        LOG.info("Notification Annotation entered : " + wrapper.getText());
        selectedButton = wrapper;
        EventBus.getDefault().post(new CommandEvents.Annotate(wrapper.getText()));
    }

    void updateButtons(boolean pending) {

        for (ButtonWrapper btnObj : buttonList) {
            if (btnObj != selectedButton) {
                btnObj.actionButton.setProgress(0);
            } else {
                btnObj.actionButton.setProgress(pending ? 1 : 0);
            }
            btnObj.setText(btnObj.getText());
            btnObj.setColor(btnObj.getColor());
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation) {
        updateButtons(waitingForLocation.waiting);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.AnnotationStatus annotationStatus) {
        if (annotationStatus.annotationWritten) {
            selectedButton = null;
        }
        updateButtons(!annotationStatus.annotationWritten);
    }
}
