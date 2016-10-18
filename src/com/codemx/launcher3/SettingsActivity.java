/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codemx.launcher3;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.android.launcher3.R;
import com.mxlibrary.utils.UtilDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new LauncherSettingsFragment())
                .commit();

        boolean isDefault = LauncherSettingsFragment.isMyAppLauncherDefault(this);
        String toast;
        if (isDefault) {
            toast = "is default launcher";
        } else {
            toast = "is not default launcher";
        }
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragment
            implements OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.launcher_preferences);

            SwitchPreference pref = (SwitchPreference) findPreference(Utilities.ALLOW_ROTATION_PREFERENCE_KEY);
            pref.setKey(Utilities.ALLOW_ROTATION_PREFERENCE_KEY);
            pref.setPersistent(false);

            Bundle extras = new Bundle();
            extras.putBoolean(LauncherSettings.Settings.EXTRA_VALUE_DEFAULT, false);
            Bundle value = getActivity().getContentResolver().call(
                    LauncherSettings.Settings.CONTENT_URI,
                    LauncherSettings.Settings.METHOD_GET_BOOLEAN,
                    Utilities.ALLOW_ROTATION_PREFERENCE_KEY, extras);
            pref.setChecked(value.getBoolean(LauncherSettings.Settings.EXTRA_VALUE));

            pref.setOnPreferenceChangeListener(this);

            //add by mx 20160615 set default launcher
            pref = (SwitchPreference) findPreference(Utilities.SET_DEFAULT_PREFERENCE_KEY);
            pref.setKey(Utilities.SET_DEFAULT_PREFERENCE_KEY);
            pref.setPersistent(false);
            extras.putBoolean(LauncherSettings.Settings.EXTRA_VALUE_DEFAULT, false);
            value = getActivity().getContentResolver().call(
                    LauncherSettings.Settings.CONTENT_URI,
                    LauncherSettings.Settings.METHOD_GET_BOOLEAN,
                    Utilities.SET_DEFAULT_PREFERENCE_KEY, extras);
            pref.setChecked(value.getBoolean(LauncherSettings.Settings.EXTRA_VALUE_SET_DEFAULT));
            pref.setOnPreferenceChangeListener(this);

//            SwitchPreference prefClear = (SwitchPreference) findPreference(Utilities.CLEAR_DEFAULT_PREFERENCE_KEY);
//            prefClear.setKey(Utilities.CLEAR_DEFAULT_PREFERENCE_KEY);
//            prefClear.setPersistent(false);
//            extras.putBoolean(LauncherSettings.Settings.EXTRA_VALUE_DEFAULT, false);
//            value = getActivity().getContentResolver().call(
//                    LauncherSettings.Settings.CONTENT_URI,
//                    LauncherSettings.Settings.METHOD_GET_BOOLEAN,
//                    Utilities.CLEAR_DEFAULT_PREFERENCE_KEY, extras);
//            prefClear.setChecked(value.getBoolean(LauncherSettings.Settings.EXTRA_DEFAULT_VALUE_CLEAR_DEFAULT));
//            prefClear.setOnPreferenceChangeListener(this);

        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            Bundle extras = new Bundle();
            if (Utilities.ALLOW_ROTATION_PREFERENCE_KEY.equals(key)) {
                extras.putBoolean(LauncherSettings.Settings.EXTRA_VALUE, (Boolean) newValue);
            } else if (Utilities.SET_DEFAULT_PREFERENCE_KEY.equals(key)) {
//                extras.putBoolean(LauncherSettings.Settings.EXTRA_VALUE_SET_DEFAULT, (Boolean) newValue);
                if ((Boolean) newValue) {
                    clearDefaultLauncher();
//                    setDefault();
//                    resetPreferredLauncherAndOpenChooser(preference.getContext());
                }
            } else if (Utilities.CLEAR_DEFAULT_PREFERENCE_KEY.equals(key)) {
                clearDefaultLauncher();
            }
            getActivity().getContentResolver().call(
                    LauncherSettings.Settings.CONTENT_URI,
                    LauncherSettings.Settings.METHOD_SET_BOOLEAN,
                    preference.getKey(), extras);
            return true;
        }

        //清理默认桌面
        private void clearDefaultLauncher() {
            Context context = getActivity().getApplicationContext();
            PackageManager pm = context.getPackageManager();
            String pn = context.getPackageName();
            String hn = Launcher.class.getName();
            ComponentName cn = new ComponentName(pn, hn);
            Intent homeIntent = new Intent("android.intent.action.MAIN");
            homeIntent.addCategory("android.intent.category.HOME");
            homeIntent.addCategory("android.intent.category.DEFAULT");
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pm.setComponentEnabledSetting(cn, 1, 1);
            context.startActivity(homeIntent);
            pm.setComponentEnabledSetting(cn, 0, 1);
        }

        //设置默认桌面
        private void setDefault() {
            String pkgName = "android";
            String clsName = "com.android.internal.app.ResolverActivity";
            if ("huawei".equals(UtilDevice.getPhoneModel())) {
                pkgName = "com.huawei.android.internal.app";
                clsName = "com.huawei.android.internal.app.HwResolverActivity";
            }
            Intent paramIntent = new Intent("android.intent.action.MAIN");
            paramIntent.setComponent(new ComponentName(pkgName, clsName));
            paramIntent.addCategory("android.intent.category.DEFAULT");
            paramIntent.addCategory("android.intent.category.HOME");
            getActivity().startActivity(paramIntent);
        }

        public static void resetPreferredLauncherAndOpenChooser(Context context) {
            PackageManager packageManager = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, Launcher.class);
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            Intent selector = new Intent(Intent.ACTION_MAIN);
            selector.addCategory(Intent.CATEGORY_HOME);
            selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(selector);

            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
        }

        /**
         * method checks to see if app is currently set as default launcher
         *
         * @return boolean true means currently set as default, otherwise false
         */
        public static boolean isMyAppLauncherDefault(Context context) {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
            filter.addCategory(Intent.CATEGORY_HOME);

            List<IntentFilter> filters = new ArrayList<IntentFilter>();
            filters.add(filter);

            final String myPackageName = context.getPackageName();
            List<ComponentName> activities = new ArrayList<ComponentName>();
            final PackageManager packageManager = context.getPackageManager();

            packageManager.getPreferredActivities(filters, activities, null);
            for (ComponentName activity : activities) {
                if (myPackageName.equals(activity.getPackageName())) {
                    return true;
                }
            }
            return false;
        }

    }


}
