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

package com.mendhak.gpslogger.ui.components;


import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.mendhak.gpslogger.R;
import com.ms.square.android.expandabletextview.ExpandableTextView;
import java.util.List;


public class FaqExpandableListAdapter extends ArrayAdapter<String> {


    private final Context context;
    private final List<String> values;

    public FaqExpandableListAdapter(Context context, List<String> values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater infalInflater = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = infalInflater.inflate(R.layout.activity_faq_list_item, null);

        // sample code snippet to set the text content on the ExpandableTextView
        final ExpandableTextView expTv1 = (ExpandableTextView) convertView.findViewById(R.id.expand_text_view);
        expTv1.setText(Html.fromHtml(values.get(position)));
        TextView tv = (TextView)expTv1.findViewById(R.id.expandable_text);
        tv.setMovementMethod(LinkMovementMethod.getInstance());

        return convertView;
    }
}