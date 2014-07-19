package com.danielkao.autoscreenonoff.ui;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import com.danielkao.autoscreenonoff.util.CV;

import java.util.*;

/**
 * Created by plateau on 2014/07/19.
 */
public class AppMultiselectListPreference extends ListPreference {

    private class AppInfo {
        public AppInfo(String name, String appPackageName) {
            this.appName = name;
            this.appPackageName = appPackageName;
        }

        public String appName;
        public String appPackageName;
    }

    private String separator;
    private static final String DEFAULT_SEPARATOR = "\u0001\u0007\u001D\u0007\u0001";
    private boolean[] entryChecked;
    private CharSequence[] entries;
    private CharSequence[] entryValues;

    public AppMultiselectListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        separator = DEFAULT_SEPARATOR;
        setSummary(prepareSummary(null));
    }

    public AppMultiselectListPreference(Context context) {
        this(context, null);
        setSummary(prepareSummary(null));
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        List<AppInfo> appList = getInstalledComponentList();
        entries = new CharSequence[appList.size()];
        entryValues = new CharSequence[appList.size()];
        for (int i = 0 ; i < appList.size(); i++) {
            entries[i] = appList.get(i).appName;
        }
        for (int i = 0 ; i < appList.size(); i++) {
            entryValues[i] = appList.get(i).appPackageName;
        }

        entryChecked = new boolean[appList.size()];

        restoreCheckedEntries();

        OnMultiChoiceClickListener listener = new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int which, boolean val) {
                entryChecked[which] = val;
                Log.v("applist", entryValues[which].toString());
            }
        };

        builder.setMultiChoiceItems(entries, entryChecked, listener);
    }

    private CharSequence[] unpack(CharSequence val) {
        if (val == null || "".equals(val)) {
            return new CharSequence[0];
        } else {
            return ((String) val).split(separator);
        }
    }
    /**
     * Gets the entries values that are selected
     *
     * @return the selected entries values
     */
    public CharSequence[] getCheckedValues() {
        return unpack(getValue());
    }

    private List<AppInfo> getInstalledComponentList(){
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        Context context = getContext();
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ril = context.getPackageManager().queryIntentActivities(mainIntent, 0);
        if(ril == null) return null;

        List<AppInfo> componentList = new ArrayList<AppInfo>();
        String name = null;

        for (ResolveInfo ri : ril) {
                if (ri.activityInfo != null) {
                        try {
                        Resources res = context.getPackageManager().getResourcesForApplication(ri.activityInfo.applicationInfo);
                        if (ri.activityInfo.labelRes != 0) {
                            name = res.getString(ri.activityInfo.labelRes);
                        } else {
                            name = ri.activityInfo.applicationInfo.loadLabel(
                                    context.getPackageManager()).toString();
                    }

                    Log.v("applist", name);
                    componentList.add(new AppInfo(name, ri.activityInfo.processName));
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
        }
        Collections.sort(componentList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return lhs.appName.compareTo(rhs.appName);
            }
        });
        return componentList;
    }

    private void restoreCheckedEntries() {
        // Explode the string read in sharedpreferences
        CharSequence[] vals = unpack(getValue());

        if (vals != null) {
            List<CharSequence> valuesList = Arrays.asList(vals);
            for (int i = 0; i < entryValues.length; i++) {
                CharSequence entry = entryValues[i];
                entryChecked[i] = valuesList.contains(entry);
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        List<CharSequence> values = new ArrayList<CharSequence>();

        if (positiveResult && entryValues != null) {
            for (int i = 0; i < entryValues.length; i++) {
                if (entryChecked[i] == true) {
                    String val = (String) entryValues[i];
                    values.add(val);
                }
            }

            String value = join(values, separator);
            String names = (String)prepareSummary(values);
            setSummary(names);
            /* set summary value to preference too */
            CV.setExcludeAppNameList(getContext(), names);
            /* */
            setValueAndEvent(value);
        }
    }

    private void setValueAndEvent(String value) {
        if (callChangeListener(unpack(value))) {
            setValue(value);
        }
    }

    private CharSequence prepareSummary(List<CharSequence> joined) {
        List<String> titles = new ArrayList<String>();
        CharSequence[] entryTitle = entries;
        if (entries == null || entries.length == 0) {
            return CV.getExcludeAppNameList(getContext());
        }

        int ix = 0;
        for (CharSequence value : entryValues) {
            if (joined.contains(value)) {
                titles.add((String) entryTitle[ix]);
            }
            ix += 1;
        }
        return join(titles, ", ");
    }

    /*
    @Override
    protected Object onGetDefaultValue(TypedArray typedArray, int index) {
        return typedArray.getTextArray(index);
    }
    */

    /*
    @Override
    protected void onSetInitialValue(boolean restoreValue,
                                     Object rawDefaultValue) {
        setSummary(prepareSummary(null));
    }
    */

    /**
     * Joins array of object to single string by separator
     *
     * Credits to kurellajunior on this post
     * http://snippets.dzone.com/posts/show/91
     *
     * @param iterable
     * any kind of iterable ex.: <code>["a", "b", "c"]</code>
     * @param separator
     * separetes entries ex.: <code>","</code>
     * @return joined string ex.: <code>"a,b,c"</code>
     */
    protected static String join(Iterable<?> iterable, String separator) {
        Iterator<?> oIter;
        if (iterable == null || (!(oIter = iterable.iterator()).hasNext()))
            return "";
        StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
        while (oIter.hasNext())
            oBuilder.append(separator).append(oIter.next());
        return oBuilder.toString();
    }

}
