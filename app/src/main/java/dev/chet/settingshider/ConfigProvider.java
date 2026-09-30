package dev.chet.settingshider;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.util.*;

/** Private configuration; exported transport authorizes the Settings UID only. */
public final class ConfigProvider extends ContentProvider {
    public static final Uri URI = Uri.parse("content://dev.chet.settingshider.config");
    static final String PREFS = "menu_config";
    @Override public boolean onCreate() { return true; }
    private void authorize() {
        int uid = Binder.getCallingUid();
        if (uid == android.os.Process.myUid()) return;
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        if (packages != null) for (String p : packages)
            if ("com.android.settings".equals(p)) return;
        throw new SecurityException("Settings UID required");
    }
    @Override public synchronized Bundle call(String method, String arg, Bundle extras) {
        authorize();
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, 0);
        Bundle out = new Bundle();
        if ("config".equals(method)) {
            out.putBoolean("enabled", prefs.getBoolean("enabled", true));
            out.putStringArrayList("hidden", new ArrayList<>(prefs.getStringSet("hidden", Collections.emptySet())));
        } else if ("discovery".equals(method) && extras != null) {
            // Callers can publish inventory, never modify hide selections.
            String raw = extras.getString("entries", "[]");
            if (raw.length() > 200000) throw new IllegalArgumentException("Inventory too large");
            try {
                JSONArray input = new JSONArray(raw);
                JSONObject merged = new JSONObject(prefs.getString("inventory", "{}"));
                for (int i=0; i<input.length() && i<300; i++) {
                    JSONObject entry = input.getJSONObject(i);
                    String id = entry.getString("id");
                    if (id.length() <= 4096 && (merged.length() < 600 || merged.has(id)))
                        merged.put(id, entry);
                }
                prefs.edit().putString("inventory", merged.toString())
                    .putLong("last_seen", System.currentTimeMillis()).apply();
            } catch (JSONException e) { throw new IllegalArgumentException("Invalid inventory", e); }
        } else throw new IllegalArgumentException("Unknown method");
        return out;
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String order) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
