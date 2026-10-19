package br.com.meyer.tclminimalhome;

import android.app.Activity;
import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final int LIME = Color.rgb(216, 243, 106);
    private static final int LOCATION_REQUEST = 42;
    private final List<AppEntry> apps = new ArrayList<>();
    private Preferences preferences;
    private LinearLayout appsRow;
    private TextView emptyState;
    private ScrollView pageScroll;
    private View appsSection;
    private TimeBackgroundView background;
    private View firstAppCard;
    private TextView weatherChip;
    private LinearLayout.LayoutParams weatherChipParams;
    private String[] forecastCodes, forecastHighs, forecastLows;
    private int forecastCount;
    private String currentTemperature, currentWeatherCode;
    private TextView backgroundStatus;
    private View initialHomeFocus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = new Preferences(this);
        createUi();
    }

    @Override public void onResume() {
        super.onResume();
        if (appsRow != null) refreshApps();
    }

    private void createUi() {
        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        background = new TimeBackgroundView(this);
        root.addView(background, match());

        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setVerticalScrollBarEnabled(false);
        pageScroll.setClipChildren(false);
        pageScroll.setClipToPadding(false);
        root.addView(pageScroll, match());

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(32), dp(24), dp(32), dp(24));
        page.setClipChildren(false);
        page.setClipToPadding(false);
        pageScroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Space spacer = new Space(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        weatherChip = label("Tempo…", 16, Color.WHITE);
        weatherChip.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        weatherChip.setLines(2);
        weatherChip.setLineSpacing(dp(2), 1f);
        weatherChip.setText("Tempo…");
        weatherChip.setPadding(0, 0, 0, 0);
        weatherChip.setContentDescription("Previsão do tempo");
        weatherChipParams = new LinearLayout.LayoutParams(dp(372), dp(58));
        header.addView(weatherChip, weatherChipParams);
        Space weatherGap = new Space(this);
        header.addView(weatherGap, new LinearLayout.LayoutParams(dp(28), 1));
        TextView appsSettings = headerIcon("▦", "Ajustar aplicativos");
        appsSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        TextView settings = headerIcon("⚙", "Configurações da TV");
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
        header.addView(appsSettings, new LinearLayout.LayoutParams(dp(54), dp(50)));
        Space headerGap = new Space(this);
        header.addView(headerGap, new LinearLayout.LayoutParams(dp(14), 1));
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(dp(58), dp(50));
        settingsParams.rightMargin = dp(18);
        header.addView(settings, settingsParams);
        TextView clock = label(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), 26, Color.WHITE);
        clock.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(clock);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(62)));

        Space titleGap = new Space(this);
        page.addView(titleGap, new LinearLayout.LayoutParams(1, dp(24)));
        TextView title = label(greeting(), 50, Color.WHITE);
        page.addView(title);
        backgroundStatus = label("Atualizando plano de fundo…", 14, Color.rgb(190, 204, 222));
        backgroundStatus.setPadding(0, dp(10), 0, 0);
        page.addView(backgroundStatus);
        View continuation = continuationCard();
        if (continuation != null) {
            initialHomeFocus = continuation;
            LinearLayout.LayoutParams continuationParams = new LinearLayout.LayoutParams(dp(680), dp(112));
            continuationParams.topMargin = dp(28);
            page.addView(continuation, continuationParams);
        }
        Space topSpace = new Space(this);
        page.addView(topSpace, new LinearLayout.LayoutParams(1, dp(continuation == null ? 48 : 34)));

        TextView inputsTitle = label("ENTRADAS", 15, Color.rgb(202, 215, 234));
        inputsTitle.setLetterSpacing(.16f);
        page.addView(inputsTitle);
        Space inputTitleGap = new Space(this);
        page.addView(inputTitleGap, new LinearLayout.LayoutParams(1, dp(16)));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.addView(entryCard());
        page.addView(quick);

        Space middle = new Space(this);
        page.addView(middle, new LinearLayout.LayoutParams(1, dp(42)));
        TextView appsTitle = label("SEUS APLICATIVOS", 15, Color.rgb(202, 215, 234));
        appsTitle.setLetterSpacing(.16f);
        page.addView(appsTitle);
        appsSection = appsTitle;
        Space smallGap = new Space(this);
        page.addView(smallGap, new LinearLayout.LayoutParams(1, dp(18)));

        HorizontalScrollView appsScroller = new HorizontalScrollView(this);
        appsScroller.setHorizontalScrollBarEnabled(false);
        appsScroller.setClipChildren(false);
        appsScroller.setClipToPadding(false);
        appsRow = new LinearLayout(this);
        appsRow.setGravity(Gravity.CENTER_VERTICAL);
        appsRow.setClipChildren(false);
        appsRow.addView(label("Carregando aplicativos…", 17, Color.rgb(205, 215, 232)));
        appsScroller.addView(appsRow, new HorizontalScrollView.LayoutParams(-2, -1));
        appsScroller.setPadding(dp(10), dp(10), dp(10), dp(14));
        page.addView(appsScroller, new LinearLayout.LayoutParams(-1, dp(122)));

        emptyState = label("Nenhum aplicativo foi selecionado. Abra Ajustar Casa.", 18, Color.WHITE);
        emptyState.setVisibility(View.GONE);
        page.addView(emptyState);
        setContentView(root);
        if (initialHomeFocus != null) root.post(() -> initialHomeFocus.requestFocus());
        loadOnlineWallpaper();
        loadWeatherForTvLocation();
        backgroundStatus.postDelayed(() -> {
            if (backgroundStatus != null) backgroundStatus.setVisibility(View.GONE);
        }, 8000);
    }

    private void refreshApps() {
        apps.clear();
        apps.addAll(loadLaunchableApps());
        Set<String> hidden = preferences.hiddenApps();
        appsRow.removeAllViews();
        firstAppCard = null;
        int displayed = 0;
        for (AppEntry app : apps) {
            if (hidden.contains(app.packageName)) continue;
            View card = appCard(app);
            if (firstAppCard == null) firstAppCard = card;
            appsRow.addView(card);
            Space gap = new Space(this);
            appsRow.addView(gap, new LinearLayout.LayoutParams(dp(18), 1));
            displayed++;
        }
        emptyState.setVisibility(displayed == 0 ? View.VISIBLE : View.GONE);
    }

    List<AppEntry> loadLaunchableApps() {
        return AppCatalog.forContext(this);
    }

    private View entryCard() {
        LinearLayout card = card();
        card.setOnFocusChangeListener((v, focused) -> {
            setCardFocus(v, focused);
            if (focused) resetHomeScroll();
        });
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = label("⇄", 34, LIME);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(glassBubble());
        card.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(24), 0, 0, 0);
        words.addView(label("ENTRADAS", 22, Color.WHITE));
        words.addView(label("Trocar HDMI ou TV", 16, Color.rgb(200, 210, 200)));
        card.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = label("›", 40, Color.WHITE);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(44), -1));
        card.setLayoutParams(new LinearLayout.LayoutParams(dp(680), dp(148)));
        card.setOnClickListener(v -> openInputs());
        return card;
    }

    private View appCard(AppEntry app) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(0, 0, 0, 0);
        card.setBackground(appTileSurface(app.packageName, false));
        card.setElevation(dp(3));
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        ImageView icon = new ImageView(this);
        int bundledLogo = bundledLogo(app.packageName);
        if (bundledLogo != 0) icon.setImageResource(bundledLogo);
        else icon.setImageDrawable(app.icon);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setAdjustViewBounds(false);
        icon.setContentDescription(null);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        iconParams.gravity = Gravity.CENTER;
        card.addView(icon, iconParams);
        card.setContentDescription("Abrir " + app.label);
        card.setOnFocusChangeListener((v, focused) -> {
            setAppTileFocus(v, focused, app.packageName);
            if (focused && pageScroll != null && appsSection != null) {
                pageScroll.post(() -> pageScroll.smoothScrollTo(0, Math.max(0, appsSection.getTop() - dp(18))));
            }
        });
        card.setOnClickListener(v -> {
            preferences.setLastUsed("app", app.packageName, app.label);
            Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            launch.setComponent(app.component);
            try { startActivity(launch); } catch (Exception ignored) { }
        });
        card.setLayoutParams(new LinearLayout.LayoutParams(dp(156), dp(92)));
        return card;
    }

    private void openInputs() {
        startActivity(new Intent(this, InputsActivity.class));
    }

    private View continuationCard() {
        String type = preferences.lastType();
        String lastLabel = preferences.lastLabel();
        if (type.isEmpty() || lastLabel.isEmpty()) return null;
        LinearLayout card = card();
        card.setOnFocusChangeListener((v, focused) -> {
            setCardFocus(v, focused);
            if (focused) resetHomeScroll();
        });
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(24), dp(14), dp(24), dp(14));
        TextView marker = label("▶", 26, LIME);
        marker.setGravity(Gravity.CENTER);
        marker.setBackground(glassBubble());
        card.addView(marker, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(18), 0, 0, 0);
        TextView heading = label("CONTINUAR", 14, Color.rgb(202, 215, 234));
        heading.setLetterSpacing(.14f);
        words.addView(heading);
        words.addView(label(lastLabel, 21, Color.WHITE));
        card.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = label("›", 38, Color.WHITE);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(42), -1));
        card.setOnClickListener(v -> {
            if ("app".equals(type)) launchLastApp(preferences.lastValue());
            else if ("input".equals(type)) {
                Intent intent = new Intent(this, InputsActivity.class);
                intent.putExtra(InputsActivity.EXTRA_INPUT_ID, preferences.lastValue());
                startActivity(intent);
            }
        });
        return card;
    }

    private void resetHomeScroll() {
        if (pageScroll != null) pageScroll.post(() -> pageScroll.scrollTo(0, 0));
    }

    private void launchLastApp(String packageName) {
        for (AppEntry app : AppCatalog.forContext(this)) {
            if (!packageName.equals(app.packageName)) continue;
            Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            launch.setComponent(app.component);
            try { startActivity(launch); } catch (Exception ignored) { }
            return;
        }
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(24), dp(18), dp(24), dp(18));
        view.setBackground(glassSurface(false));
        view.setElevation(dp(3));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnFocusChangeListener((v, focused) -> setCardFocus(v, focused));
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(250), dp(148)));
        return view;
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return view;
    }

    private TextView navLabel(String text, boolean selected) {
        TextView view = label(text, 17, selected ? Color.WHITE : Color.rgb(195, 202, 198));
        view.setGravity(Gravity.CENTER);
        if (selected) view.setBackground(glassSurface(true));
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnFocusChangeListener((v, focused) -> {
            if (focused || selected) v.setBackground(glassSurface(true));
            else v.setBackgroundColor(Color.TRANSPARENT);
        });
        return view;
    }

    private LinearLayout.LayoutParams navParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(116), dp(50));
        params.leftMargin = dp(12);
        return params;
    }

    private LinearLayout.LayoutParams homeParams() {
        return new LinearLayout.LayoutParams(dp(148), dp(50));
    }

    private TextView headerIcon(String icon, String description) {
        TextView view = label(icon, 29, Color.WHITE);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnFocusChangeListener((v, focused) -> v.setBackground(focused ? glassSurface(true) : glassBubble()));
        view.setBackground(glassBubble());
        return view;
    }

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
        GradientDrawable surface = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.argb(95, 157, 183, 226), Color.argb(48, 66, 87, 128)});
        surface.setShape(GradientDrawable.OVAL);
        surface.setStroke(dp(1), Color.argb(80, 255, 255, 255));
        return surface;
    }

    private int bundledLogo(String packageName) {
        if ("com.netflix.ninja".equals(packageName)) return R.drawable.brand_netflix_hd;
        if ("com.google.android.youtube.tv".equals(packageName)) return R.drawable.brand_youtube_hd;
        if ("com.spotify.tv.android".equals(packageName)) return R.drawable.brand_spotify_hd;
        return 0;
    }

    private GradientDrawable appTileSurface(String packageName, boolean focused) {
        int start = Color.rgb(26, 34, 48);
        int end = Color.rgb(13, 19, 29);
        if ("com.netflix.ninja".equals(packageName)) {
            start = Color.rgb(30, 5, 8); end = Color.rgb(10, 5, 7);
        } else if ("com.google.android.youtube.tv".equals(packageName)) {
            start = Color.rgb(255, 35, 35); end = Color.rgb(205, 10, 18);
        } else if ("com.spotify.tv.android".equals(packageName)) {
            start = Color.rgb(31, 205, 101); end = Color.rgb(18, 132, 66);
        } else if (packageName.startsWith("com.google.android")) {
            start = Color.rgb(46, 99, 194); end = Color.rgb(23, 61, 132);
        } else if (packageName.startsWith("com.tcl.")) {
            start = Color.rgb(24, 100, 118); end = Color.rgb(15, 55, 73);
        }
        if (focused) {
            start = brighten(start, 1.16f);
            end = brighten(end, 1.16f);
        }
        GradientDrawable surface = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        surface.setCornerRadius(dp(18));
        surface.setStroke(dp(focused ? 2 : 1), Color.argb(focused ? 165 : 62, 235, 243, 255));
        return surface;
    }

    private int brighten(int color, float factor) {
        return Color.rgb(Math.min(255, (int) (Color.red(color) * factor)),
                Math.min(255, (int) (Color.green(color) * factor)),
                Math.min(255, (int) (Color.blue(color) * factor)));
    }

    private void setCardFocus(View view, boolean focused) {
        view.animate().scaleX(1f).scaleY(1f).translationY(focused ? -dp(3) : 0).setDuration(150).start();
        view.setBackground(glassSurface(focused));
        view.setElevation(dp(focused ? 20 : 3));
    }

    private void setAppTileFocus(View view, boolean focused, String packageName) {
        view.animate().scaleX(focused ? 1.03f : 1f).scaleY(focused ? 1.03f : 1f)
                .translationY(focused ? -dp(3) : 0).setDuration(150).start();
        view.setBackground(appTileSurface(packageName, focused));
        view.setElevation(dp(focused ? 22 : 3));
    }

    private FrameLayout.LayoutParams match() { return new FrameLayout.LayoutParams(-1, -1); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    private String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "Bom dia,";
        if (hour >= 12 && hour < 18) return "Boa tarde,";
        return "Boa noite,";
    }

    private void loadOnlineWallpaper() {
        new Thread(() -> {
            Bitmap cached = WallpaperCache.read(this);
            if (cached != null) runOnUiThread(() -> {
                background.setOnlineImage(cached);
                if (backgroundStatus != null) backgroundStatus.setVisibility(View.GONE);
            });
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
                URL imageUrl = new URL("https://www.bing.com" + path + "&w=1920&h=1080&rs=1&c=4");
                HttpURLConnection imageConnection = (HttpURLConnection) imageUrl.openConnection();
                imageConnection.setConnectTimeout(8000);
                imageConnection.setReadTimeout(8000);
                Bitmap image = BitmapFactory.decodeStream(imageConnection.getInputStream());
                if (image != null) {
                    WallpaperCache.write(this, image);
                    runOnUiThread(() -> {
                    background.setOnlineImage(image);
                    if (backgroundStatus != null) backgroundStatus.setVisibility(View.GONE);
                    });
                }
            } catch (Exception ignored) {
                // The local time-based background remains visible while offline.
            }
        }).start();
    }

    private void loadWeatherForTvLocation() {
        if (showForecastFromJson(preferences.cachedWeather())) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) return;
        Location location = latestLocation(manager);
        if (location != null) {
            loadForecast(location);
            return;
        }
        String provider = isProviderAvailable(manager, "fused") ? "fused" : LocationManager.NETWORK_PROVIDER;
        try {
            manager.requestSingleUpdate(provider, new LocationListener() {
                @Override public void onLocationChanged(Location value) { loadForecast(value); }
                @Override public void onProviderDisabled(String provider) { }
                @Override public void onProviderEnabled(String provider) { }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            }, Looper.getMainLooper());
        } catch (Exception ignored) { }
    }

    private Location latestLocation(LocationManager manager) {
        String[] providers = {"fused", LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER};
        Location newest = null;
        for (String provider : providers) {
            try {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (newest == null || candidate.getTime() > newest.getTime())) newest = candidate;
            } catch (Exception ignored) { }
        }
        return newest;
    }

    private boolean isProviderAvailable(LocationManager manager, String provider) {
        try { return manager.isProviderEnabled(provider); }
        catch (Exception ignored) { return false; }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadWeatherForTvLocation();
        }
    }

    private void loadForecast(Location location) {
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();
        new Thread(() -> {
            try {
                String endpoint = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" + longitude +
                        "&current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=3&timezone=auto";
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(6000);
                String json = readText(connection.getInputStream());
                preferences.setCachedWeather(json);
                runOnUiThread(() -> showForecastFromJson(json));
            } catch (Exception ignored) { }
        }).start();
    }

    private boolean showForecastFromJson(String json) {
        if (json == null || json.isEmpty()) return false;
        String[] dates = jsonValues(json, "time");
        String[] codes = jsonValues(json, "weather_code");
        String[] highs = jsonValues(json, "temperature_2m_max");
        String[] lows = jsonValues(json, "temperature_2m_min");
        String temperature = jsonNumber(json, "temperature_2m");
        String weatherCode = jsonNumber(json, "weather_code");
        int count = Math.min(3, Math.min(Math.min(dates.length, codes.length), Math.min(highs.length, lows.length)));
        if (count == 0) return false;
        showForecast(codes, highs, lows, count, temperature, weatherCode);
        return true;
    }

    private String[] jsonValues(String json, String key) {
        String marker = "\"" + key + "\":[";
        int start = json.indexOf(marker);
        if (start < 0) return new String[0];
        start += marker.length();
        int end = json.indexOf(']', start);
        if (end < 0) return new String[0];
        String values = json.substring(start, end).replace("\"", "");
        return values.isEmpty() ? new String[0] : values.split(",");
    }

    private String jsonNumber(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = start;
        while (end < json.length() && "0123456789.-".indexOf(json.charAt(end)) >= 0) end++;
        return json.substring(start, end);
    }

    private void showForecast(String[] codes, String[] highs, String[] lows, int count, String temperature, String weatherCode) {
        forecastCodes = codes;
        forecastHighs = highs;
        forecastLows = lows;
        forecastCount = count;
        currentTemperature = temperature;
        currentWeatherCode = weatherCode;
        weatherChip.setVisibility(View.VISIBLE);
        updateWeatherHeader();
    }

    private void updateWeatherHeader() {
        if (weatherChip == null || forecastCount == 0) return;
        String code = currentWeatherCode == null || currentWeatherCode.isEmpty() ? forecastCodes[0] : currentWeatherCode;
        String temperature = currentTemperature == null || currentTemperature.isEmpty()
                ? roundTemperature(forecastHighs[0]) : roundTemperature(currentTemperature);
        int tomorrow = Math.min(1, forecastCount - 1);
        int afterTomorrow = Math.min(2, forecastCount - 1);
        String top = "AGORA                 AMANHÃ                DEPOIS";
        String values = weatherSymbol(code) + "  " + temperature + "°"
                + "          " + weatherSymbol(forecastCodes[tomorrow]) + "  "
                + roundTemperature(forecastHighs[tomorrow]) + "°/" + roundTemperature(forecastLows[tomorrow]) + "°"
                + "          " + weatherSymbol(forecastCodes[afterTomorrow]) + "  "
                + roundTemperature(forecastHighs[afterTomorrow]) + "°/" + roundTemperature(forecastLows[afterTomorrow]) + "°";
        weatherChip.setText(top + "\n" + values);
        weatherChip.setTextSize(14);
        weatherChipParams.width = dp(372);
        weatherChip.setLayoutParams(weatherChipParams);
    }

    private String roundTemperature(String value) {
        try { return String.valueOf(Math.round(Double.parseDouble(value.trim()))); }
        catch (Exception ignored) { return value.trim(); }
    }

    private String weatherCondition(String code) {
        int value;
        try { value = Integer.parseInt(code.trim()); } catch (Exception ignored) { return "Tempo"; }
        if (value == 0) return "Ensolarado";
        if (value <= 3) return "Parcialmente nublado";
        if (value == 45 || value == 48) return "Neblina";
        if (value >= 51 && value <= 67) return "Chuva";
        if (value >= 71 && value <= 77) return "Neve";
        if (value >= 80 && value <= 82) return "Pancadas";
        if (value >= 95) return "Tempestade";
        return "Nublado";
    }

    private String weatherSymbol(String code) {
        int value;
        try { value = Integer.parseInt(code.trim()); } catch (Exception ignored) { return "○"; }
        if (value == 0) return "☀";
        if (value <= 3) return "☁";
        if (value == 45 || value == 48) return "≋";
        if (value >= 51 && value <= 82) return "☂";
        if (value >= 95) return "ϟ";
        return "☁";
    }

    private String readText(InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder text = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) text.append(line);
        reader.close();
        return text.toString();
    }

    static final class AppEntry {
        final String label, packageName;
        final ComponentName component;
        final Drawable icon;
        AppEntry(String label, String packageName, ComponentName component, Drawable icon) {
            this.label = label; this.packageName = packageName; this.component = component; this.icon = icon;
        }
    }
}
