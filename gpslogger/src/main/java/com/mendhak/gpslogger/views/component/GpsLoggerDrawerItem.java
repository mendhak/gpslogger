package com.mendhak.gpslogger.views.component;


import com.mendhak.gpslogger.R;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;


public class GpsLoggerDrawerItem {


    public static PrimaryDrawerItem newPrimary(int resTitle, int resSummary, int resIcon, int identifier) {

        return new PrimaryDrawerItem()
        .withName(resTitle)
        .withDescription(resSummary)
        .withIcon(resIcon)
        .withIdentifier(identifier)
        .withTextColorRes(R.color.primaryColorText)
        .withDescriptionTextColorRes(R.color.secondaryColorText)
        .withSelectable(false);

    }

    public static SecondaryDrawerItem newSecondary(int resTitle, int resIcon, int identifier) {

        return new SecondaryDrawerItem()
                .withName(resTitle)
                .withIcon(resIcon)
                .withIdentifier(identifier)
                .withTextColorRes(R.color.secondaryColorText)
                .withDescriptionTextColorRes(R.color.secondaryColorText)
                .withSelectable(false);

    }


}

