package com.danielkao.autoscreenonoff;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.*;
import android.webkit.WebView;

import java.util.Calendar;

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
    // schedule
    AlarmManager am;

    // ad service
    /*
    private static final String MY_AD_UNIT_ID = "a1519f30be4a645";
    private AdView adView;
    */
    // ---

    private AlarmManager getAlarmManager(){
        if(am==null)
        {
            am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        return am;
    }

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
        super.onStart();
        // for receiving pref change callbacks
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        updatePrefState();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.widget_configure);
        setContentView(R.layout.activity_settings);

        showChangelogDialogCheck();
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
            case R.id.menu_changelog:
            {
                showChangelogDialog();
                return true;
            }
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

        if(key.equals(CV.PREF_AUTO_ON)){
            if(CV.getPrefAutoOnoff(this)==false)
                cancelSchedule();
            else{
                if(CV.getPrefSleeping(this)){
                    setSchedule();
                }
            }

            // send intent to service
            Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
            i.putExtra(CV.SERVICEACTION,
                    CV.SERVICEACTION_TOGGLE);
            i.putExtra(CV.SERVICETYPE,
                    CV.SERVICETYPE_SETTING);
            startService(i);
        }
        else if(key.equals(CV.PREF_CHARGING_ON)){
            Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
            i.putExtra(CV.SERVICEACTION,
                    (sharedPreferences.getBoolean(key,false))
                            ? CV.SERVICEACTION_TURNON
                            : CV.SERVICEACTION_TURNOFF);
                startService(i);
        }
        else if(key.equals(CV.PREF_SHOW_NOTIFICATION)){
            // if it's on mode, then should notify service to enable onOrientationListener
            Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
            i.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_SHOW_NOTIFICATION);
            startService(i);
        }
        //notify service when Pref of temp disable in land is changed.
        else if(key.equals(CV.PREF_DISABLE_IN_LANDSCAPE)){
            // if it's on mode, then should notify service to enable onOrientationListener
            Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
            i.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_UPDATE_DISABLE_IN_LANDSCAPE);
            startService(i);

        }else if(key.equals(CV.PREF_TIMEOUT_LOCK)){
            // for updating list preference summary
            ListPreference lp = (ListPreference) findPreference(CV.PREF_TIMEOUT_LOCK);
            String str = getString(R.string.pref_summary_timeout_lock);
            lp.setSummary(String.format(str,lp.getEntry()));
            //lp.setSummary(str);
        }else if(key.equals(CV.PREF_TIMEOUT_UNLOCK)){
            // for updating list preference summary
            ListPreference lp = (ListPreference) findPreference(CV.PREF_TIMEOUT_UNLOCK);
            String str = getString(R.string.pref_summary_timeout_unlock);
            lp.setSummary(String.format(str,lp.getEntry()));
        }else if(key.equals(CV.PREF_SLEEPING)){
            // not turned on: just igonre the change
            if(CV.getPrefAutoOnoff(this)==false)
                return;

            if(CV.getPrefSleeping(this)){
                // on:register the alarmmanager
                CV.logv("set schedule");
                setSchedule();
            }else{
                // off: cancel alarmmanager
                CV.logv("cancel schedule");
                cancelSchedule();

                // if autoOn is on, should turn it on again
                Intent i = new Intent(CV.SERVICE_INTENT_ACTION);
                i.putExtra(CV.SERVICEACTION,
                        CV.SERVICEACTION_TOGGLE);
                i.putExtra(CV.SERVICETYPE,
                        CV.SERVICETYPE_SETTING);
                startService(i);
            }


        }else if(key.equals(CV.PREF_SLEEP_START)||key.equals(CV.PREF_SLEEP_STOP)){
            // re-register alarm manager
            CV.logv("isInSleepTime:%b",CV.isInSleepTime(this));
            if(CV.getPrefSleeping(this)){
                setSchedule();
            }

        }
    }

    //<editor-fold description="schedule related">
    private void setSchedule() {
        cancelSchedule();
        //alarm: sleep start
        int hour = TimePreference.getHour(CV.getPrefSleepStart(this));
        int minute = TimePreference.getMinute(CV.getPrefSleepStart(this));

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent(this, SensorMonitorService.class);
        intent.setData(Uri.parse("timer://1")); // identifier for this alarm
        intent.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_MODE_SLEEP);
        intent.putExtra(CV.SLEEP_MODE_START,true);
        PendingIntent pi = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP
                ,calendar.getTimeInMillis()
                ,AlarmManager.INTERVAL_DAY, pi);
        //alarm: sleep stop
        hour = TimePreference.getHour(CV.getPrefSleepStop(this));
        minute = TimePreference.getMinute(CV.getPrefSleepStop(this));

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        intent = new Intent(this, SensorMonitorService.class);
        intent.setData(Uri.parse("timer://2"));
        intent.putExtra(CV.SERVICEACTION, CV.SERVICEACTION_MODE_SLEEP);
        intent.putExtra(CV.SLEEP_MODE_START,false);
        pi = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);

        getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP
                ,calendar.getTimeInMillis()
                ,AlarmManager.INTERVAL_DAY, pi);
    }

    private void cancelSchedule() {
        Intent intent = new Intent(this, SensorMonitorService.class);
        intent.setData(Uri.parse("timer://1"));
        PendingIntent pi = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        getAlarmManager().cancel(pi);

        intent.setData(Uri.parse("timer://2"));
        pi = PendingIntent.getService(this, 0, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        getAlarmManager().cancel(pi);
    }
    //</editor-fold>

    // when auto on is turned on; user can't set charging mode
    // only when auto on is turned on, use can set landscape mode
    private void updatePrefState(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SwitchPreference spCharging = (SwitchPreference) findPreference(CV.PREF_CHARGING_ON);
        SwitchPreference spAuto = (SwitchPreference) findPreference(CV.PREF_AUTO_ON);

        if(sp.getBoolean(CV.PREF_AUTO_ON, false)){
            spAuto.setEnabled(false);
            spCharging.setEnabled(false);
        }else{
            spCharging.setEnabled(true);
        }

        if(sp.getBoolean(CV.PREF_CHARGING_ON, false)){
            spCharging.setEnabled(true);
            spAuto.setEnabled(false);
        }else{
            spAuto.setEnabled(true);
        }
    }

    //<editor-fold description: changlog dialog>
    private void showChangelogDialogCheck(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int currentVersionCode = 0;
        int viewedVersionCode = sp.getInt(CV.PREF_VIEWED_VERSION_CODE,0);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionCode = pi.versionCode;
        } catch (Exception e) {e.printStackTrace();}

        if (currentVersionCode > viewedVersionCode) {
            showChangelogDialog();

            SharedPreferences.Editor editor   = sp.edit();
            editor.putInt(CV.PREF_VIEWED_VERSION_CODE, currentVersionCode);
            editor.commit();
        }
    }

    private void showChangelogDialog(){
        //load some kind of a view
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.changelogdlg, null);
        WebView wv = (WebView) view.findViewById(R.id.wv_changelog);
        wv.loadData(getString(R.string.changelog_html),"text/html; charset=UTF-8", null);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_changelog))
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setView(view)
                .setNegativeButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //
                    }
                }).show();
    }
    //</editor-fold>
}