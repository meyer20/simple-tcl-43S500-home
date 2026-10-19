package br.com.meyer.tclminimalhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvContract;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Lists only the TV's physical inputs, never content providers such as Play Filmes. */
public final class InputsActivity extends Activity {
    static final String EXTRA_INPUT_ID = "input_id";
    private final int lime = Color.rgb(216, 243, 106);
    private boolean showingInput;
    private TimeBackgroundView background;
    private Preferences preferences;
    private boolean editingNames;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = new Preferences(this);
        String inputId = getIntent().getStringExtra(EXTRA_INPUT_ID);
        if (inputId != null && openInputById(inputId)) return;
        showList();
    }

    private void showList() {
        showingInput = false;
        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        background = new TimeBackgroundView(this);
        root.addView(background, new FrameLayout.LayoutParams(-1, -1));
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(46), dp(30), dp(46), dp(34));
        page.setClipChildren(false);
        page.setClipToPadding(false);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text("Entradas", 38, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1));
        TextView rename = text("✎  Renomear", 16, Color.WHITE);
        rename.setGravity(Gravity.CENTER);
        rename.setPadding(dp(16), 0, dp(16), 0);
        rename.setBackground(glassSurface(false));
        rename.setFocusable(true);
        rename.setFocusableInTouchMode(true);
        rename.setOnFocusChangeListener((v, focused) -> v.setBackground(glassSurface(focused)));
        rename.setOnClickListener(v -> showRenameList());
        titleRow.addView(rename, new LinearLayout.LayoutParams(dp(188), dp(48)));
        page.addView(titleRow);
        TextView hint = text("Escolha uma fonte", 17, Color.rgb(202, 215, 234));
        hint.setPadding(0, dp(8), 0, dp(30));
        page.addView(hint);

        LinearLayout sources = new LinearLayout(this);
        sources.setOrientation(LinearLayout.VERTICAL);
        sources.setClipChildren(false);
        sources.setClipToPadding(false);
        page.addView(sources, new LinearLayout.LayoutParams(dp(700), -2));
        TvInputManager manager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        List<TvInputInfo> inputs = physicalInputs(manager);
        int count = 0;
        View firstCard = null;
        for (TvInputInfo input : inputs) {
            View card = inputCard(input);
            if (firstCard == null) firstCard = card;
            sources.addView(card, new LinearLayout.LayoutParams(-1, dp(100)));
            count++;
            Space gap = new Space(this);
            sources.addView(gap, new LinearLayout.LayoutParams(1, dp(14)));
        }
        if (count == 0) page.addView(text("Nenhuma entrada física está disponível.", 18, Color.WHITE));
        if (firstCard != null) firstCard.requestFocus();
        setContentView(root);
        loadOnlineWallpaper();
    }

    private View inputCard(TvInputInfo input) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackground(glassSurface(false));
        card.setElevation(dp(3));
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(1f).scaleY(1f).translationY(focused ? -dp(3) : 0).setDuration(150).start();
            v.setBackground(glassSurface(focused));
            v.setElevation(dp(focused ? 20 : 3));
        });
        boolean tuner = input.getServiceInfo().packageName.equals("com.tcl.tvinput");
        String defaultLabel = tuner ? "TV" : input.loadLabel(this).toString();
        String label = preferences.inputName(input.getId(), defaultLabel);
        TextView icon = text(sourceIcon(label, tuner), tuner ? 18 : 28, lime);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(glassBubble());
        card.addView(icon, new LinearLayout.LayoutParams(dp(60), dp(60)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(20), 0, 0, 0);
        words.addView(text(label, 20, Color.WHITE));
        TextView sub = text(tuner ? "Televisão ao vivo" : "Abrir fonte", 15, Color.rgb(202, 215, 234));
        sub.setPadding(0, dp(5), 0, 0);
        words.addView(sub);
        card.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = text("›", 34, Color.WHITE);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(38), -1));
        card.setContentDescription("Abrir " + label);
        card.setOnClickListener(v -> {
            preferences.setLastUsed("input", input.getId(), label);
            if (tuner) openLiveTv(input); else tune(input, false);
        });
        card.setOnLongClickListener(v -> {
            editInputName(input);
            return true;
        });
        return card;
    }

    private String sourceIcon(String label, boolean tuner) {
        if (tuner) return "TV";
        String normalized = label.toUpperCase();
        if (normalized.contains("HDMI")) return "⇥";
        if (normalized.equals("AV") || normalized.startsWith("AV ")) return "◉";
        return "▣";
    }

    private List<TvInputInfo> physicalInputs(TvInputManager manager) {
        java.util.ArrayList<TvInputInfo> results = new java.util.ArrayList<>();
        for (TvInputInfo input : manager.getTvInputList()) {
            String packageName = input.getServiceInfo().packageName;
            boolean isTclSource = packageName.startsWith("com.tcl.") &&
                    (input.isPassthroughInput() || packageName.equals("com.tcl.tvinput"));
            if (isTclSource) results.add(input);
        }
        return results;
    }

    private boolean openInputById(String inputId) {
        TvInputManager manager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        for (TvInputInfo input : physicalInputs(manager)) {
            if (!inputId.equals(input.getId())) continue;
            preferences.setLastUsed("input", input.getId(), inputName(input));
            if (input.getServiceInfo().packageName.equals("com.tcl.tvinput")) openLiveTv(input); else tune(input, false);
            return true;
        }
        return false;
    }

    private String inputName(TvInputInfo input) {
        String fallback = input.getServiceInfo().packageName.equals("com.tcl.tvinput") ? "TV" : input.loadLabel(this).toString();
        return preferences.inputName(input.getId(), fallback);
    }

    private void showRenameList() {
        editingNames = true;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(56), dp(42), dp(56), dp(42));
        page.setBackgroundColor(Color.rgb(10, 17, 29));
        TextView title = text("Renomear entradas", 34, Color.WHITE);
        page.addView(title);
        TextView hint = text("Selecione uma entrada para alterar seu nome.", 17, Color.rgb(202, 215, 234));
        hint.setPadding(0, dp(12), 0, dp(26));
        page.addView(hint);
        TvInputManager manager = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        for (TvInputInfo input : physicalInputs(manager)) {
            TextView row = text(inputName(input), 22, Color.WHITE);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(24), 0, dp(24), 0);
            row.setBackground(glassSurface(false));
            row.setFocusable(true);
            row.setFocusableInTouchMode(true);
            row.setOnFocusChangeListener((v, focused) -> v.setBackground(glassSurface(focused)));
            row.setOnClickListener(v -> editInputName(input));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(70));
            rowParams.bottomMargin = dp(14);
            page.addView(row, rowParams);
        }
        setContentView(page);
    }

    private void editInputName(TvInputInfo input) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setText(inputName(input));
        field.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("Nome da entrada")
                .setView(field)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String name = field.getText().toString().trim();
                    if (!name.isEmpty()) preferences.setInputName(input.getId(), name);
                    if (editingNames) showRenameList(); else showList();
                }).show();
    }

    private void tune(TvInputInfo input, boolean tuner) {
        showingInput = true;
        TvView tv = new TvView(this);
        setContentView(tv);
        Uri channel = tuner ? null : TvContract.buildChannelUriForPassthroughInput(input.getId());
        tv.tune(input.getId(), channel);
    }

    private void openLiveTv(TvInputInfo input) {
        Intent liveTv = new Intent(Intent.ACTION_MAIN);
        liveTv.setComponent(new android.content.ComponentName("com.tcl.tv", "com.tcl.tv.TVActivity"));
        liveTv.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        startActivity(liveTv);
    }

    @Override public void onBackPressed() {
        if (showingInput || editingNames) {
            editingNames = false;
            showList();
        } else super.onBackPressed();
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return view;
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    private GradientDrawable glassSurface(boolean focused) {
        GradientDrawable surface = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                focused
                        ? new int[]{Color.argb(218, 76, 103, 146), Color.argb(202, 36, 48, 74)}
                        : new int[]{Color.argb(170, 48, 61, 83), Color.argb(154, 20, 29, 43)});
        surface.setCornerRadius(dp(24));
        surface.setStroke(dp(focused ? 2 : 1), Color.argb(focused ? 190 : 82, 235, 243, 255));
        return surface;
    }

    private GradientDrawable glassBubble() {
        GradientDrawable bubble = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(95, 157, 183, 226), Color.argb(48, 66, 87, 128)});
        bubble.setShape(GradientDrawable.OVAL);
        bubble.setStroke(dp(1), Color.argb(80, 255, 255, 255));
        return bubble;
    }

    private void loadOnlineWallpaper() {
        new Thread(() -> {
            Bitmap cached = WallpaperCache.read(this);
            if (cached != null) runOnUiThread(() -> background.setOnlineImage(cached));
            try {
                URL metadataUrl = new URL("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=pt-BR");
                HttpURLConnection metadataConnection = (HttpURLConnection) metadataUrl.openConnection();
                metadataConnection.setConnectTimeout(6000);
                metadataConnection.setReadTimeout(6000);
                String metadata = readText(metadataConnection.getInputStream());
                String marker = "\"url\":\"";
                int start = metadata.indexOf(marker);
                if (start < 0) return;
                start += marker.length();
                int end = metadata.indexOf('"', start);
                if (end < 0) return;
                String path = metadata.substring(start, end).replace("\\/", "/");
                HttpURLConnection imageConnection = (HttpURLConnection) new URL("https://www.bing.com" + path + "&w=1920&h=1080&rs=1&c=4").openConnection();
                imageConnection.setConnectTimeout(8000);
                imageConnection.setReadTimeout(8000);
                Bitmap image = BitmapFactory.decodeStream(imageConnection.getInputStream());
                if (image != null) {
                    WallpaperCache.write(this, image);
                    runOnUiThread(() -> background.setOnlineImage(image));
                }
            } catch (Exception ignored) { }
        }).start();
    }

    private String readText(InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) text.append(line);
        reader.close();
        return text.toString();
    }
}
