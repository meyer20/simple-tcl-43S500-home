package br.com.meyer.tclminimalhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

final class Preferences {
    private static final String FILE = "casa";
    private static final String HIDDEN = "hidden_apps";
    private static final String APP_ORDER = "app_order";
    private static final String LAST_TYPE = "last_type";
    private static final String LAST_VALUE = "last_value";
    private static final String LAST_LABEL = "last_label";
    private static final String INPUT_NAMES = "input_names";
    private static final String WEATHER_JSON = "weather_json";
    private static final String WEATHER_TIME = "weather_time";
    private final SharedPreferences storage;

    Preferences(Context context) {
        storage = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    Set<String> hiddenApps() {
        return new HashSet<>(storage.getStringSet(HIDDEN, Collections.emptySet()));
    }

    void setHiddenApps(Set<String> packages) {
        storage.edit().putStringSet(HIDDEN, new HashSet<>(packages)).apply();
    }

    List<String> appOrder() {
        String saved = storage.getString(APP_ORDER, "");
        return saved.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(saved.split("\\n")));
    }

    void setAppOrder(List<String> packages) {
        storage.edit().putString(APP_ORDER, android.text.TextUtils.join("\n", packages)).apply();
    }

    void setLastUsed(String type, String value, String label) {
        storage.edit().putString(LAST_TYPE, type).putString(LAST_VALUE, value).putString(LAST_LABEL, label).apply();
    }

    String lastType() { return storage.getString(LAST_TYPE, ""); }
    String lastValue() { return storage.getString(LAST_VALUE, ""); }
    String lastLabel() { return storage.getString(LAST_LABEL, ""); }

    String inputName(String inputId, String fallback) {
        return storage.getString(INPUT_NAMES + inputId, fallback);
    }

    void setInputName(String inputId, String name) {
        storage.edit().putString(INPUT_NAMES + inputId, name.trim()).apply();
    }

    String cachedWeather() {
        long savedAt = storage.getLong(WEATHER_TIME, 0);
        return System.currentTimeMillis() - savedAt < 60 * 60 * 1000
                ? storage.getString(WEATHER_JSON, "") : "";
    }

    void setCachedWeather(String json) {
        storage.edit().putString(WEATHER_JSON, json).putLong(WEATHER_TIME, System.currentTimeMillis()).apply();
    }
}
