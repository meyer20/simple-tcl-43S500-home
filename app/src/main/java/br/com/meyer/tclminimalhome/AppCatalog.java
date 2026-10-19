package br.com.meyer.tclminimalhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class AppCatalog {
    static List<MainActivity.AppEntry> forContext(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        PackageManager manager = context.getPackageManager();
        List<MainActivity.AppEntry> results = new ArrayList<>();
        for (ResolveInfo info : manager.queryIntentActivities(intent, 0)) {
            if (context.getPackageName().equals(info.activityInfo.packageName)) continue;
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            String label = info.loadLabel(manager).toString();
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && label.equals("Android TV Home")) continue;
            results.add(new MainActivity.AppEntry(label, info.activityInfo.packageName,
                    new ComponentName(info.activityInfo.packageName, info.activityInfo.name), info.loadIcon(manager)));
        }
        Collections.sort(results, Comparator.comparing(entry -> entry.label.toLowerCase(Locale.getDefault())));
        List<String> order = new Preferences(context).appOrder();
        Collections.sort(results, (left, right) -> {
            int leftIndex = order.indexOf(left.packageName);
            int rightIndex = order.indexOf(right.packageName);
            if (leftIndex < 0) leftIndex = Integer.MAX_VALUE;
            if (rightIndex < 0) rightIndex = Integer.MAX_VALUE;
            if (leftIndex != rightIndex) return Integer.compare(leftIndex, rightIndex);
            return left.label.compareToIgnoreCase(right.label);
        });
        return results;
    }
    private AppCatalog() { }
}
