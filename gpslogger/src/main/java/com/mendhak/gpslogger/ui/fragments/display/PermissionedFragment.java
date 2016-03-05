package com.mendhak.gpslogger.ui.fragments.display;


import android.app.Fragment;
import com.canelmas.let.DeniedPermission;
import com.canelmas.let.Let;
import com.canelmas.let.RuntimePermissionListener;
import com.canelmas.let.RuntimePermissionRequest;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.ui.Dialogs;

import java.util.List;

public class PermissionedFragment extends Fragment implements RuntimePermissionListener {

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Let.handle(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onShowPermissionRationale(List<String> list, final RuntimePermissionRequest runtimePermissionRequest) {
        Dialogs.alert(getString(R.string.gpslogger_permissions_rationale_title), getString(R.string.gpslogger_permissions_rationale_message_basic), getActivity(), new Dialogs.MessageBoxCallback() {
            @Override
            public void messageBoxResult(int which) {
                runtimePermissionRequest.retry();
            }
        });
    }

    @Override
    public void onPermissionDenied(List<DeniedPermission> list) {
        boolean anyPermanentlyDeniedPermissions = false;
        for(DeniedPermission denial :list){
            if(denial.isNeverAskAgainChecked()){
                anyPermanentlyDeniedPermissions = true;
            }
        }

        if(anyPermanentlyDeniedPermissions){
            Dialogs.alert(getString(R.string.gpslogger_permissions_rationale_title), getString(R.string.gpslogger_permissions_permanently_denied), getActivity());
        }

    }
}
