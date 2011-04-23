//package com.mendhak.gpslogger.helpers;
//
//import android.app.ProgressDialog;
//import android.preference.EditTextPreference;
//
//import com.mendhak.gpslogger.AutoEmailActivity;
//import com.mendhak.gpslogger.R;
//import com.mendhak.gpslogger.Utilities;
//
//public class AutoEmailSetupHelper implements IAutoEmailHelper
//{
//
//	ProgressDialog pd;
//	AutoEmailActivity setupActivity;
//
//	public AutoEmailSetupHelper(AutoEmailActivity activity)
//	{
//		setupActivity = activity;
//	}
//
//	public void RegisterEmailSettings()
//	{
//		//EditText txtTarget = (EditText) setupActivity.findViewById(R.id.txtTarget);
//		//String targetEmail = txtTarget.getText().toString();
//		
//		EditTextPreference txtTarget = (EditTextPreference) setupActivity.findPreference("autoemail_target");
//		String targetEmail = txtTarget.getText();
//		
//		if (!Utilities.IsValidEmailAddress(targetEmail))
//		{
//			Utilities.MsgBox(setupActivity.getString(R.string.autoemail_invalid_title),
//					setupActivity.getString(R.string.autoemail_invalid_summary), setupActivity);
//			return;
//		}
//
//		pd = new ProgressDialog(setupActivity, ProgressDialog.STYLE_HORIZONTAL);
//		pd.setMax(100);
//		pd.setIndeterminate(true);
//		pd = ProgressDialog.show(setupActivity, setupActivity.getString(R.string.autoemail_registering),
//				setupActivity.getString(R.string.please_wait), true, true);
//
//		Thread t = new Thread(new AutoEmailRegisterHandler(Utilities.GetPersonId(setupActivity),
//				targetEmail, this));
//		t.start();
//	}
//
//	public void OnAutoEmailRegistered(boolean connectionSuccess)
//	{
//		if (connectionSuccess)
//		{
//			setupActivity.handler.post(setupActivity.updateResultsSettingsRegistered);
//		}
//		else
//		{
//			setupActivity.handler.post(setupActivity.updateResultsConnectionFail);
//		}
//		pd.dismiss();
//
//	}
//
//}
//
//interface IAutoEmailHelper
//{
//	public void OnAutoEmailRegistered(boolean connectionSuccess);
//}
//
//class AutoEmailRegisterHandler implements Runnable
//{
//	String personId;
//	String targetEmail;
//	IAutoEmailHelper helper;
//
//	public AutoEmailRegisterHandler(String personId, String targetEmail, IAutoEmailHelper helper)
//	{
//		this.personId = personId;
//		this.targetEmail = targetEmail;
//		this.helper = helper;
//	}
//
//	public void run()
//	{
//		boolean success = false;
//		try
//		{
//			Utilities.GetUrl(Utilities.GetRegisterSettingUrl(personId, targetEmail));
//			success = true;
//		}
//		catch (Exception e)
//		{
//			Utilities.LogError("AutoEmailRegisterHandler - run", e);
//			success = false;
//		}
//		finally
//		{
//			helper.OnAutoEmailRegistered(success);
//		}
//
//	}
//
//}
