package com.danielkao.autoscreenonoff.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;
import com.danielkao.autoscreenonoff.R;
import com.danielkao.autoscreenonoff.receiver.TurnOffReceiver;
import com.danielkao.autoscreenonoff.util.CV;
import com.danielkao.autoscreenonoff.util.EdgeToEdge;
import com.danielkao.autoscreenonoff.util.EnvironmentInfoUtils;

/**
 * Created by plateau on 2013/05/20.
 */
@SuppressWarnings("deprecation")
public class AutoScreenOnOffPreferenceActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
    private static String CONFIGURE_ACTION="android.appwidget.action.APPWIDGET_CONFIGURE";

    private static final int REQUEST_CODE_DISABLE_ADMIN = 1;
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 2;

    private DevicePolicyManager deviceManager;
    private ComponentName mDeviceAdmin;

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
        EdgeToEdge.padSystemBars(this);

        requestNotificationPermissionIfNeeded();

        showChangelogDialogCheck();
    }

    // the foreground-service notification is invisible on 13+ until the
    // user grants POST_NOTIFICATIONS
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33)
            return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_CODE_POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

    }

    //<editor-fold desc="menu handlings">
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // resource IDs are non-final under AGP: no switch/case here
        int id = item.getItemId();
        if (id == R.id.menu_uninstall_app) {
            uninstallApp(null);
            return true;
        } else if (id == R.id.menu_changelog) {
            showChangelogDialog();
            return true;
        } else if (id == R.id.menu_playstore_url) {
            String url = getString(R.string.playstore_url);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            return true;
        } else if (id == R.id.menu_code_url) {
            String url = getString(R.string.author_url);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            return true;
        } else if (id == R.id.menu_send_feedback) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"leinadkao@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
            i.putExtra(Intent.EXTRA_TEXT   , "\n\n\n- - - - - - -\n" + EnvironmentInfoUtils.getApplicationInfo(this));
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, getString(R.string.no_mail_client_warning), Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.menu_about) {
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
        return super.onOptionsItemSelected(item);
    }
    //</editor-fold>

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

    /**
     * uninstall button clicked or from menu item
      * @param view indicate which button is pressed
     */
    public void uninstallApp(View view){
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_uninstall_title))
                .setMessage(getString(R.string.dlg_uninstall_message))
                .setIcon(android.R.drawable.ic_menu_delete)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        deviceManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                        mDeviceAdmin = new ComponentName(AutoScreenOnOffPreferenceActivity.this, TurnOffReceiver.class);

                        // handle activeAdmin previlige
                        if(isActiveAdmin()) {
                            deviceManager.removeActiveAdmin(mDeviceAdmin);
                        }
                        Uri packageUri = Uri.parse("package:" + getPackageName());
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
            Intent i = CV.serviceIntent(this, CV.SERVICEACTION_TOGGLE);
            i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_SETTING);
            CV.startService(this, i);
        }
        else if(key.equals(CV.PREF_CHARGING_ON)){
            int action = sharedPreferences.getBoolean(key,false)
                    ? CV.SERVICEACTION_TURNON
                    : CV.SERVICEACTION_TURNOFF;
            CV.startService(this, CV.serviceIntent(this, action));
        }
        else if(key.equals(CV.PREF_SHOW_NOTIFICATION)){
            CV.startService(this, CV.serviceIntent(this, CV.SERVICEACTION_SHOW_NOTIFICATION));
        }
        //notify service when Pref of temp disable in land is changed.
        else if(key.equals(CV.PREF_DISABLE_IN_LANDSCAPE)){
            // if it's on mode, then should notify service to enable onOrientationListener
            CV.startService(this, CV.serviceIntent(this, CV.SERVICEACTION_UPDATE_DISABLE_IN_LANDSCAPE));
        }else if(key.equals(CV.PREF_TIMEOUT_LOCK)){
            // for updating list preference summary
            ListPreference lp = (ListPreference) findPreference(CV.PREF_TIMEOUT_LOCK);
            String str = getString(R.string.pref_summary_timeout_lock);
            lp.setSummary(String.format(str,lp.getEntry()));
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
                Intent i = CV.serviceIntent(this, CV.SERVICEACTION_TOGGLE);
                i.putExtra(CV.SERVICETYPE, CV.SERVICETYPE_SETTING);
                CV.startService(this, i);
            }


        }else if(key.equals(CV.PREF_SLEEP_START)||key.equals(CV.PREF_SLEEP_STOP)){
            // re-register alarm manager
            CV.logv("isInSleepTime:%b",CV.isInSleepTime(this));
            if(CV.getPrefSleeping(this)){
                setSchedule();
            }

        }else if(key.equals(CV.PREF_NO_PARTIAL_LOCK)){
            CV.logv("change whether to use partial lock");
            CV.startService(this, CV.serviceIntent(this, CV.SERVICEACTION_PARTIALLOCK_TOGGLE));
        }
    }

    //<editor-fold description="schedule related">
    private void setSchedule() {
        CV.startService(this, CV.serviceIntent(this, CV.SERVICEACTION_SET_SCHEDULE));
    }

    private void cancelSchedule() {
        CV.startService(this, CV.serviceIntent(this, CV.SERVICEACTION_CANCEL_SCHEDULE));
    }
    //</editor-fold>

    // when auto on is turned on; user can't set charging mode
    // only when auto on is turned on, use can set landscape mode
    private void updatePrefState(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Preference spCharging, spAuto;
         spCharging = findPreference(CV.PREF_CHARGING_ON);
         spAuto = findPreference(CV.PREF_AUTO_ON);


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
        String changelogs = getString(R.string.changelog_28)
                            + getString(R.string.changelog_27)
                            + getString(R.string.changelog_26)
                            + getString(R.string.old_changelog_html);
        wv.loadData(changelogs,"text/html; charset=UTF-8", null);

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
