package dev.chet.settingshider;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import org.json.*;
import java.util.*;

public final class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout rows;
    private String filter = "";
    private Set<String> hidden;
    private TextView status;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p,k) -> {
        if ("inventory".equals(k) || "last_seen".equals(k)) handler.post(this::render);
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(ConfigProvider.PREFS, 0);
        hidden = new HashSet<>(prefs.getStringSet("hidden", Collections.emptySet()));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,48,28,24); setContentView(root);
        TextView title = new TextView(this); title.setText("Settings Menu Hider"); title.setTextSize(25); root.addView(title);
        TextView help = new TextView(this); help.setText("1. Enable this module in LSPosed; scope only Settings.\n2. Reboot, then open the main Settings menu.\n3. Return here and select entries to hide.\nAfter changes, close Settings from Recents and reopen it.\n\nHide only: search and direct links can still open pages."); root.addView(help);
        Switch master = new Switch(this); master.setText("Enable hiding"); master.setChecked(prefs.getBoolean("enabled",true)); root.addView(master);
        master.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean("enabled",v).apply());
        Button open = new Button(this); open.setText("Open Settings to discover entries"); root.addView(open);
        open.setOnClickListener(v->startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)));
        Button reset = new Button(this); reset.setText("Restore all menu entries"); root.addView(reset);
        reset.setOnClickListener(v->{ hidden.clear(); save(); render(); });
        EditText search = new EditText(this); search.setSingleLine(); search.setHint("Search discovered titles or keys"); root.addView(search);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { filter=s.toString().toLowerCase(Locale.ROOT); render(); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        status = new TextView(this); root.addView(status);
        ScrollView scroll = new ScrollView(this); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        rows = new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); scroll.addView(rows);
    }
    @Override protected void onResume() { super.onResume(); prefs.registerOnSharedPreferenceChangeListener(listener); render(); }
    @Override protected void onPause() { prefs.unregisterOnSharedPreferenceChangeListener(listener); super.onPause(); }
    private void save() { prefs.edit().putStringSet("hidden", new HashSet<>(hidden)).apply(); }
    private void render() {
        if (rows==null) return; rows.removeAllViews();
        List<JSONObject> entries = new ArrayList<>();
        try {
            JSONObject inventory = new JSONObject(prefs.getString("inventory","{}"));
            Iterator<String> keys = inventory.keys();
            while(keys.hasNext()) entries.add(inventory.getJSONObject(keys.next()));
        } catch(JSONException ignored) {}
        entries.sort(Comparator.comparing(o->o.optString("title"),String.CASE_INSENSITIVE_ORDER));
        long seen=prefs.getLong("last_seen",0);
        status.setText(entries.size()+" entries discovered · "+hidden.size()+" selected to hide\n"+
            (seen==0 ? "No Settings inventory yet. Enable, reboot and open Settings." : "Last scan: "+android.text.format.DateFormat.format("MMM d, HH:mm:ss",seen)));
        for(JSONObject entry:entries) {
            String id=entry.optString("id"), title=entry.optString("title"), key=entry.optString("key");
            if (!(title+" "+key).toLowerCase(Locale.ROOT).contains(filter)) continue;
            CheckBox box=new CheckBox(this); box.setText((title.isEmpty()?"Untitled entry":title)+"\n"+(key.isEmpty()?id:key));
            box.setChecked(hidden.contains(id)); rows.addView(box);
            box.setOnCheckedChangeListener((b,v)->{ if(v)hidden.add(id);else hidden.remove(id);save();
                status.setText(entries.size()+" entries discovered · "+hidden.size()+" selected to hide\nClose Settings from Recents and reopen to apply."); });
        }
    }
}
