package me.piebridge.brevent.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.util.ArraySet;

import java.util.Collection;
import java.util.Locale;

import me.piebridge.SimpleTrim;

/**
 * Created by thom on 2017/2/4.
 */
public class AppsInfoTask extends AsyncTask<Void, Integer, Boolean> {

    private final AppsItemAdapter mAdapter;

    AppsInfoTask(AppsItemAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    protected void onPreExecute() {
        BreventActivity activity = mAdapter.getActivity();
        if (activity != null) {
            activity.showAppProgress();
        }
    }

    @Override
    protected Boolean doInBackground(Void... args) {
        BreventActivity activity = mAdapter.getActivity();
        if (activity == null || activity.isStopped()) {
            return false;
        }

        PackageManager packageManager = LocaleUtils.getSystemContext(activity).getPackageManager();
        boolean showAllApps = PreferencesUtils.getPreferences(activity)
                .getBoolean(SettingsFragment.SHOW_ALL_APPS, SettingsFragment.DEFAULT_SHOW_ALL_APPS);

        AppsLabelLoader labelLoader = new AppsLabelLoader(activity);
        BreventApplication application = (BreventApplication) activity.getApplication();
        Collection<String> packageNames;
        synchronized (activity.mPackages) {
            packageNames = new ArraySet(activity.mPackages);
        }
        int max = packageNames.size();
        int progress = 0;
        int size = 0;
        String query = activity.getQuery();
        if (query != null) {
            query = SimpleTrim.trim(query).toString().toLowerCase(Locale.US);
        }
        for (String packageName : packageNames) {
            PackageInfo packageInfo = application.getInstantPackageInfo(packageName);
            try {
                if (packageInfo == null) {
                    packageInfo = packageManager.getPackageInfo(packageName, 0);
                }
            } catch (PackageManager.NameNotFoundException e) {
                UILog.w("Can't find package " + packageName, e);
                continue;
            }
            if (mAdapter.accept(packageManager, packageInfo, showAllApps)) {
                String label = labelLoader.getLabel(packageManager, packageInfo);
                if (query == null || acceptLabel(label, query)
                        || acceptPackageName(packageName, query)) {
                    mAdapter.addPackage(activity, packageInfo, label);
                    size++;
                }
            }
            publishProgress(++progress, max, size);
        }
        labelLoader.onCompleted();
        return true;
    }

    private boolean acceptLabel(String label, String query) {
        return label.toLowerCase(Locale.US).contains(query);
    }

    private boolean acceptPackageName(String packageName, String query) {
        return query.length() > 0x3 && packageName.toLowerCase(Locale.US).contains(query);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        BreventActivity activity = mAdapter.getActivity();
        if (activity != null) {
            activity.updateAppProgress(progress[0], progress[1], progress[2]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mAdapter.onCompleted(result == null ? false : result);
        BreventActivity activity = mAdapter.getActivity();
        if (activity != null) {
            activity.hideAppProgress();
        }
    }

}
