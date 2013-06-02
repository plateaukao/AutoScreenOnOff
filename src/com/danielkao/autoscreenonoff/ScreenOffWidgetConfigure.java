package com.danielkao.autoscreenonoff;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.google.ads.AdView;

/**
 * Created by plateau on 2013/05/20.
 */
public class ScreenOffWidgetConfigure extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
    private static String CONFIGURE_ACTION="android.appwidget.action.APPWIDGET_CONFIGURE";

    private static final int REQUEST_CODE_DISABLE_ADMIN = 1;

    private DevicePolicyManager deviceManager;
    private ComponentName mDeviceAdmin;

    //service
    private SensorMonitorService sensorService;

    // ad service
    private static final String MY_AD_UNIT_ID = "a1519f30be4a645";
    private AdView adView;
    // ---

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE_DISABLE_ADMIN == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                // Has become the device administrator.
            } else {
                //Canceled or failed: turn off Enabler
            }
        }
    }

    @Override
    protected void onStart() {
        super.onResume();
        // for receiving pref change callbacks
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        updatePrefState();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.widget_configure);
        setContentView(R.layout.activity_settings);

    }

    @Override
    protected void onStop() {
        super.onStop();

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onDestroy() {
        /*
        if (adView != null) {
            adView.destroy();
        }
        */
        super.onDestroy();
    }


    //<editor-fold desc="menu handlings">
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_playstore_url:
            {
                String url = getString(R.string.playstore_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
            case R.id.menu_code_url:
            {
                String url = getString(R.string.author_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
            case R.id.menu_about:
            {
                String appName = getString(R.string.app_name);
                String version="";
                try {
                    version = getPackageManager().getPackageInfo(getPackageName(),0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                String about = String.format(getString(R.string.dialog_about_message), appName, version);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                Dialog d = builder.setMessage(about)
                        .setTitle(R.string.dialog_about_title)
                        .create();
                d.show();

                return true;

            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //</editor-fold>

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK &&
                Integer.parseInt(Build.VERSION.SDK)<5) {
            onBackPressed();
        }

        return(super.onKeyDown(keyCode, event));
    }

    @Override
    public void onBackPressed() {
        if (CONFIGURE_ACTION.equals(getIntent().getAction())) {
            Intent intent=getIntent();
            Bundle extras=intent.getExtras();

            if (extras!=null) {
                int id=extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

                Intent result=new Intent();

                result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        id);
                setResult(RESULT_OK, result);
            }
        }

        super.onBackPressed();
    }

    private boolean isActiveAdmin() {
        return deviceManager.isAdminActive(mDeviceAdmin);
    }

    // uninstall button clicked
    public void uninstallApp(View view){
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_uninstall_title))
                .setMessage(getString(R.string.dlg_uninstall_message))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        deviceManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                        mDeviceAdmin = new ComponentName(ScreenOffWidgetConfigure.this, TurnOffReceiver.class);

                        // handle activeAdmin previlige
                        if(isActiveAdmin()) {
                            deviceManager.removeActiveAdmin(mDeviceAdmin);
                        }
                        Uri packageUri = Uri.parse("package:com.danielkao.autoscreenonoff");
                        Intent uninstallIntent =
                                new Intent(Intent.ACTION_DELETE, packageUri);
                        startActivity(uninstallIntent);
                    }})
                .setNegativeButton(android.R.string.no, null).show();

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key)
    {
        // update state of pref disable in landscape
        updatePrefState();

        if(key.equals(ConstantValues.PREF_AUTO_ON)){
            // send intent to service
            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,
                    ConstantValues.SERVICEACTION_TOGGLE);
            i.putExtra(ConstantValues.SERVICETYPE,
                    ConstantValues.SERVICETYPE_SETTING);
            startService(i);
        }
        else if(key.equals(ConstantValues.PREF_CHARGING_ON)){
            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,
                    (sharedPreferences.getBoolean(key,false))
                            ?ConstantValues.SERVICEACTION_TURNON
                            :ConstantValues.SERVICEACTION_TURNOFF);
                startService(i);
        }
        //notify service when Pref of temp disable in land is changed.
        else if(key.equals(ConstantValues.PREF_DISABLE_IN_LANDSCAPE)){
            // if it's on mode, then should notify service to enable onOrientationListener
            Intent i = new Intent(ConstantValues.SERVICE_INTENT_ACTION);
            i.putExtra(ConstantValues.SERVICEACTION,ConstantValues.SERVICEACTION_UPDATE_DISABLE_IN_LANDSCAPE);
            startService(i);

        }else if(key.equals(ConstantValues.PREF_TIMEOUT_LOCK)){
            // for updating list preference summary
            ListPreference lp = (ListPreference) findPreference(ConstantValues.PREF_TIMEOUT_LOCK);
            String str = getString(R.string.pref_summary_timeout_lock);
            lp.setSummary(String.format(str,lp.getEntry()));
            //lp.setSummary(str);
        }else if(key.equals(ConstantValues.PREF_TIMEOUT_UNLOCK)){
            // for updating list preference summary
            ListPreference lp = (ListPreference) findPreference(ConstantValues.PREF_TIMEOUT_UNLOCK);
            String str = getString(R.string.pref_summary_timeout_unlock);
            lp.setSummary(String.format(str,lp.getEntry()));
        }

    }

    // when auto on is turned on; user can't set charging mode
    // only when auto on is turned on, use can set landscape mode
    private void updatePrefState(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        CheckBoxPreference cbpLandscape = (CheckBoxPreference) findPreference(ConstantValues.PREF_DISABLE_IN_LANDSCAPE);
        SwitchPreference spCharging = (SwitchPreference) findPreference(ConstantValues.PREF_CHARGING_ON);
        SwitchPreference spAuto = (SwitchPreference) findPreference(ConstantValues.PREF_AUTO_ON);

        if(sp.getBoolean(ConstantValues.PREF_AUTO_ON, false)){
            spCharging.setEnabled(false);
            cbpLandscape.setEnabled(true);
        }else{
            spCharging.setEnabled(true);
        }

        if(sp.getBoolean(ConstantValues.PREF_CHARGING_ON, false)){
            spAuto.setEnabled(false);
            cbpLandscape.setEnabled(true);
        }else{
            spAuto.setEnabled(true);
        }

        if(sp.getBoolean(ConstantValues.PREF_AUTO_ON, false) == false &&
                sp.getBoolean(ConstantValues.PREF_CHARGING_ON, false) == false){
            cbpLandscape.setEnabled(false);
        }

    }
}