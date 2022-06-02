package com.mendhak.gpslogger.ui.components;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;


import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

// Originally from: https://gist.github.com/marchold/45e22839eb94aa14dfb5
// Modified to work with SwitchPreferenceCompat

/**
 * Custom preference for handling a switch with a clickable preference area as well
 */
public class SwitchPlusClickPreference extends SwitchPreferenceCompat {

    //
    // Public interface
    //

    /**
     * Sets listeners for the switch and the background container preference view cell
     * @param listener A valid SwitchPlusClickListener
     */
    public void setSwitchClickListener(SwitchPlusClickListener listener){
        this.listener = listener;
    }
    private SwitchPlusClickListener listener = null;

    /**
     * Interface gives callbacks in to both parts of the preference
     */
    public interface SwitchPlusClickListener {
        /**
         * Called when the switch is switched
         * @param buttonView
         * @param isChecked
         */
        public void onCheckedChanged(SwitchCompat buttonView, boolean isChecked);

        /**
         * Called when the preference view is clicked
         * @param view
         */
        public void onClick(View view);
    }

    public SwitchPlusClickPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SwitchPlusClickPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchPlusClickPreference(Context context) {
        super(context);
    }


    //
    // Internal Functions
    //

    /**
     * Recursively go through view tree until we find an android.widget.Switch
     * @param view Root view to start searching
     * @return A Switch class or null
     */
    private SwitchCompat findSwitchWidget(View view){
        if (view instanceof  SwitchCompat){
            return (SwitchCompat)view;
        }
        if (view instanceof ViewGroup){
            ViewGroup viewGroup = (ViewGroup)view;
            for (int i = 0; i < viewGroup.getChildCount();i++){
                View child = viewGroup.getChildAt(i);
                if (child instanceof ViewGroup){
                    SwitchCompat result = findSwitchWidget(child);
                    if (result!=null) return result;
                }
                if (child instanceof SwitchCompat){
                    return (SwitchCompat)child;
                }
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final SwitchCompat switchView = findSwitchWidget(holder.itemView);
        if (switchView!=null){
            switchView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null)
                        listener.onCheckedChanged((SwitchCompat) v, ((SwitchCompat)v).isChecked());
                }
            });
            switchView.setChecked(getSharedPreferences().getBoolean(getKey(),false));
            switchView.setFocusable(true);
            switchView.setEnabled(true);
            //Set the thumb drawable here if you need to. Seems like this code makes it not respect thumb_drawable in the xml.
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener!=null) listener.onClick(v);
            }
        });
    }

//    //Get a handle on the 2 parts of the switch preference and assign handlers to them
//    @Override
//    protected void onBindView (View view){
//        super.onBindView(view);
//
//        final Switch switchView = findSwitchWidget(view);
//        if (switchView!=null){
//            switchView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (listener != null)
//                        listener.onCheckedChanged((Switch) v, ((Switch)v).isChecked());
//                }
//            });
//            switchView.setChecked(getSharedPreferences().getBoolean(getKey(),false));
//            switchView.setFocusable(true);
//            switchView.setEnabled(true);
//            //Set the thumb drawable here if you need to. Seems like this code makes it not respect thumb_drawable in the xml.
//        }
//
//        view.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (listener!=null) listener.onClick(v);
//            }
//        });
//    }
}