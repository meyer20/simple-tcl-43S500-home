package br.com.meyer.tclminimalhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Keeps the home screen intentionally small without uninstalling any app. */
public final class SettingsActivity extends Activity {
    private static final int FOCUS_NONE = 0;
    private static final int FOCUS_UP = 1;
    private static final int FOCUS_DOWN = 2;
    private Preferences preferences;
    private Set<String> hidden;
    private String restorePackage;
    private int restoreFocus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = new Preferences(this);
        hidden = preferences.hiddenApps();
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(9, 16, 28));
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(64), dp(48), dp(64), dp(48));
        page.setClipChildren(false);
        page.setClipToPadding(false);
        scroll.addView(page);

        TextView title = text("Ajustar Casa", 34, Color.WHITE);
        page.addView(title);
        TextView hint = text("Escolha os aplicativos da Casa e use as setas para definir a ordem da dock.", 17, Color.rgb(202, 215, 234));
        hint.setPadding(0, dp(12), 0, dp(32));
        page.addView(hint);

        List<MainActivity.AppEntry> apps = AppCatalog.forContext(this);
        View focusTarget = null;
        for (int index = 0; index < apps.size(); index++) {
            MainActivity.AppEntry app = apps.get(index);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            CheckBox visible = new CheckBox(this);
            visible.setText(app.label);
            visible.setTextColor(Color.WHITE);
            visible.setTextSize(19);
            visible.setButtonTintList(new android.content.res.ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{Color.WHITE, Color.rgb(202, 215, 234)}));
            visible.setChecked(!hidden.contains(app.packageName));
            visible.setPadding(dp(14), dp(8), dp(14), dp(8));
            visible.setFocusable(true);
            visible.setFocusableInTouchMode(true);
            visible.setBackground(focusSurface(false));
            visible.setOnFocusChangeListener((v, focused) -> {
                v.setBackground(focusSurface(focused));
                v.setElevation(dp(focused ? 10 : 0));
            });
            visible.setOnCheckedChangeListener((button, checked) -> {
                if (checked) hidden.remove(app.packageName); else hidden.add(app.packageName);
                preferences.setHiddenApps(hidden);
            });
            row.addView(visible, new LinearLayout.LayoutParams(0, dp(60), 1));
            TextView up = action("↑", "Subir " + app.label);
            TextView down = action("↓", "Descer " + app.label);
            final int position = index;
            up.setEnabled(index > 0);
            down.setEnabled(index < apps.size() - 1);
            up.setAlpha(index > 0 ? 1f : .28f);
            down.setAlpha(index < apps.size() - 1 ? 1f : .28f);
            up.setOnClickListener(v -> move(apps, position, position - 1, app.packageName, FOCUS_UP));
            down.setOnClickListener(v -> move(apps, position, position + 1, app.packageName, FOCUS_DOWN));
            LinearLayout.LayoutParams upParams = new LinearLayout.LayoutParams(dp(62), dp(54));
            upParams.leftMargin = dp(12);
            row.addView(up, upParams);
            LinearLayout.LayoutParams downParams = new LinearLayout.LayoutParams(dp(62), dp(54));
            downParams.leftMargin = dp(8);
            row.addView(down, downParams);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(62));
            rowParams.bottomMargin = dp(10);
            page.addView(row, rowParams);
            if (app.packageName.equals(restorePackage)) {
                if (restoreFocus == FOCUS_UP && up.isEnabled()) focusTarget = up;
                else if (restoreFocus == FOCUS_DOWN && down.isEnabled()) focusTarget = down;
            }
        }
        TextView done = text("VOLTAR PARA CASA", 17, Color.rgb(216, 243, 106));
        done.setGravity(Gravity.CENTER);
        done.setBackground(focusSurface(true));
        done.setPadding(dp(24), 0, dp(24), 0);
        done.setFocusable(true);
        done.setFocusableInTouchMode(true);
        done.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(focusSurface(focused));
            ((TextView) v).setTextColor(focused ? Color.WHITE : Color.rgb(216, 243, 106));
        });
        done.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(dp(280), dp(58));
        doneParams.topMargin = dp(32);
        page.addView(done, doneParams);
        setContentView(scroll);
        if (focusTarget != null) {
            View target = focusTarget;
            target.post(target::requestFocus);
        }
    }

    private void move(List<MainActivity.AppEntry> apps, int from, int to, String packageName, int focus) {
        if (to < 0 || to >= apps.size()) return;
        MainActivity.AppEntry entry = apps.remove(from);
        apps.add(to, entry);
        List<String> order = new ArrayList<>();
        for (MainActivity.AppEntry app : apps) order.add(app.packageName);
        preferences.setAppOrder(order);
        restorePackage = packageName;
        restoreFocus = focus;
        render();
    }

    private TextView action(String symbol, String description) {
        TextView button = text(symbol, 28, Color.rgb(202, 215, 234));
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setBackground(focusSurface(false));
        button.setOnFocusChangeListener((v, focused) -> {
            v.setBackground(focusSurface(focused));
            ((TextView) v).setTextColor(focused ? Color.WHITE : Color.rgb(202, 215, 234));
            v.setElevation(dp(focused ? 12 : 2));
            v.animate().translationY(focused ? -dp(2) : 0).setDuration(120).start();
        });
        return button;
    }

    private GradientDrawable focusSurface(boolean focused) {
        GradientDrawable surface = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                focused
                        ? new int[]{Color.argb(226, 77, 105, 151), Color.argb(214, 36, 49, 76)}
                        : new int[]{Color.argb(165, 36, 49, 70), Color.argb(145, 18, 27, 43)});
        surface.setCornerRadius(dp(16));
        surface.setStroke(dp(focused ? 2 : 1), Color.argb(focused ? 195 : 76, 235, 243, 255));
        return surface;
    }

    private TextView text(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value); text.setTextSize(size); text.setTextColor(color); text.setIncludeFontPadding(false);
        return text;
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }
}
