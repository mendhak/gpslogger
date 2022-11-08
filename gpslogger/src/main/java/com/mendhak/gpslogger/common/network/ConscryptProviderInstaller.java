package com.mendhak.gpslogger.common.network;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.lang.reflect.Method;

public class ConscryptProviderInstaller {
    private static final Logger LOG = Logs.of(ConscryptProviderInstaller.class);
    private static boolean installed = false;

    public static boolean shouldPromptUserForInstallation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Android 10 has TLS 1.3 enabled.
            return false;
        }

        return !installed;
    }

    @SuppressWarnings("unchecked")
    public static void installIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Android 10 has TLS 1.3 enabled.
            return;
        }

        if (installed) {
            return;
        }

        //Threat modelling - we should ensure that only a package with a trusted signature provides Conscrypt.
        //In this case, either the same signature as this package (eg debugging locally)
        //Or, the F-droid/github signatures.
        boolean signatureMatch = false;

        if (Systems.isPackageInstalled("com.mendhak.conscryptprovider", context)) {

            try {

                if (context.getPackageManager().checkSignatures(context.getPackageName(), "com.mendhak.conscryptprovider") == PackageManager.SIGNATURE_MATCH) {
                    signatureMatch = true;
                }

                if (!signatureMatch) {
                    //Get signature to compare - either Github or F-Droid versions
                    String signature = Systems.getPackageSignature("com.mendhak.conscryptprovider", context);
                    if (
                            signature.equalsIgnoreCase("C7:90:8D:17:33:76:1D:F3:CD:EB:56:67:16:C8:00:B5:AF:C5:57:DB")
                            || signature.equalsIgnoreCase("9D:E1:4D:DA:20:F0:5A:58:01:BE:23:CC:53:34:14:11:48:76:B7:5E")
                    ) {
                        signatureMatch = true;
                    }
                }

                if (!signatureMatch) {
                    LOG.error("com.mendhak.conscryptprovider found, but with an invalid signature. Ignoring.");
                    return;
                }

                //https://gist.github.com/ByteHamster/f488f9993eeb6679c2b5f0180615d518
                Context targetContext = context.createPackageContext("com.mendhak.conscryptprovider",
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                ClassLoader classLoader = targetContext.getClassLoader();
                Class installClass = classLoader.loadClass("com.mendhak.conscryptprovider.ConscryptProvider");
                Method installMethod = installClass.getMethod("install", new Class[]{});
                installMethod.invoke(null);
                installed = true;
                LOG.info("Conscrypt Provider installed");
            } catch (Exception e) {
                LOG.error("Could not install Conscrypt Provider", e);
            }
        } else {
            LOG.debug("GPSLogger Conscrypt Provider is not installed.");
        }
    }

    public static Intent getConscryptInstallationIntent(Context context) {
        // Default to Github
        Intent conscryptIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/en/packages/com.mendhak.conscryptprovider/"));

        // If F-Droid client is installed, go straight to app listing
        if(Systems.isPackageInstalled("org.fdroid.fdroid", context)){
            conscryptIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("fdroid.app://details?id=com.mendhak.conscryptprovider"));
        }

        conscryptIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        return conscryptIntent;

    }
}