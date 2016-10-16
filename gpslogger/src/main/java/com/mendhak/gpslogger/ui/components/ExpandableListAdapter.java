package com.mendhak.gpslogger.ui.components;


import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import com.mendhak.gpslogger.R;
import com.ms.square.android.expandabletextview.ExpandableTextView;

import java.util.HashMap;
import java.util.List;


public class ExpandableListAdapter extends ArrayAdapter<String> {


    private final Context context;
    private final List<String> values;

    public ExpandableListAdapter(Context context, List<String> values) {
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