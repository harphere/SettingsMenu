package dev.chet.settingshider;

import android.content.Context;
import android.content.Intent;
import android.os.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** No Settings/AndroidX classes are referenced at module class initialization. */
public final class MenuHook implements IXposedHookLoadPackage {
    private final Map<Object,Boolean> tracked = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Object,Boolean> roots = Collections.synchronizedMap(new WeakHashMap<>());
    private final Set<Object> pending = Collections.newSetFromMap(new WeakHashMap<>());
    private volatile Set<String> hidden = Collections.emptySet();
    private volatile boolean enabled = false;
    private volatile boolean busy;
    private Handler main;
    private ExecutorService worker;
    private Class<?> groupType, categoryType;
    private int logCount;
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.android.settings".equals(lp.packageName)) return;
        try {
            main = new Handler(Looper.getMainLooper());
            worker = Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"MenuHider-IPC");t.setDaemon(true);return t;});
            ClassLoader cl=lp.classLoader;
            Class<?> pref = XposedHelpers.findClass("androidx.preference.Preference",cl);
            groupType = XposedHelpers.findClass("androidx.preference.PreferenceGroup",cl);
            categoryType = XposedHelpers.findClass("androidx.preference.PreferenceCategory",cl);
            Class<?> top=XposedHelpers.findClass("com.android.settings.homepage.TopLevelSettings",cl);
            hookLifecycle(top,"onCreatePreferences",android.os.Bundle.class,String.class);
            hookLifecycle(top,"onResume");
            XposedHelpers.findAndHookMethod(groupType,"addPreference",pref,new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (p.hasThrowable() || busy) return;
                    try { Object root=rootOf(p.thisObject); if(root!=null) schedule(root); } catch(Throwable t) { log(t); }
                }
            });
            XposedHelpers.findAndHookMethod(pref,"setVisible",boolean.class,new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if(busy || !tracked.containsKey(p.thisObject))return;
                    try {
                        tracked.put(p.thisObject,(Boolean)p.args[0]);
                        if(enabled && hidden.contains(id(p.thisObject))) p.args[0]=false;
                    } catch(Throwable t){log(t);}
                }
            });
            XposedBridge.log("SettingsMenuHider: homepage hooks installed; scope=com.android.settings");
        } catch(Throwable t) { log(t); }
    }
    // Find the declaring method: XposedHelpers does not search inherited hook targets.
    private void hookLifecycle(Class<?> start,String name,Class<?>... types) throws Exception {
        Class<?> c=start; Method found=null;
        while(c!=null) {
            try { found=c.getDeclaredMethod(name,types);break; }
            catch(NoSuchMethodException e){c=c.getSuperclass();}
        }
        if(found==null)throw new NoSuchMethodException(name);
        XposedBridge.hookMethod(found,new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if(p.hasThrowable() || !start.isInstance(p.thisObject))return;
                try {
                    Object root=XposedHelpers.callMethod(p.thisObject,"getPreferenceScreen");
                    if(root!=null){roots.put(root,true);schedule(root);}
                } catch(Throwable t){log(t);}
            }
        });
    }
    private Object rootOf(Object group) {
        Object cur=group;
        for(int i=0;i<20 && cur!=null;i++) {
            if(roots.containsKey(cur))return cur;
            cur=XposedHelpers.callMethod(cur,"getParent");
        }
        return null;
    }
    private void schedule(Object root) {
        // All preference mutations are kept on the UI thread.
        if(Looper.myLooper()!=Looper.getMainLooper()){main.post(()->schedule(root));return;}
        if(!pending.add(root))return;
        WeakReference<Object> ref=new WeakReference<>(root);
        main.postDelayed(()->{Object r=ref.get();if(r!=null){pending.remove(r);scan(r);}},120);
    }
    private void scan(Object root) {
        try {
            Context ctx=(Context)XposedHelpers.callMethod(root,"getContext");
            JSONArray inventory=new JSONArray();
            apply(root,inventory,0);
            String raw=inventory.toString();
            WeakReference<Object> ref=new WeakReference<>(root);
            worker.execute(()->{
                try {
                    Bundle config=ctx.getContentResolver().call(ConfigProvider.URI,"config",null,null);
                    if(config==null)throw new IllegalStateException("No provider response");
                    ArrayList<String> ids=config.getStringArrayList("hidden");
                    hidden=ids==null?Collections.emptySet():Collections.unmodifiableSet(new HashSet<>(ids));
                    enabled=config.getBoolean("enabled",false);
                    Bundle extra=new Bundle();extra.putString("entries",raw);
                    ctx.getContentResolver().call(ConfigProvider.URI,"discovery",null,extra);
                } catch(Throwable t) {
                    // Missing/stopped provider must never leave Settings broken.
                    enabled=false;hidden=Collections.emptySet();log(t);
                }
                main.post(()->{Object r=ref.get();if(r!=null)try{apply(r,null,0);}catch(Throwable t){log(t);}});
            });
        } catch(Throwable t){log(t);}
    }
    private void apply(Object group,JSONArray inventory,int depth) throws Exception {
        if(depth>8)return;
        int count=(Integer)XposedHelpers.callMethod(group,"getPreferenceCount");
        for(int i=0;i<count;i++) {
            Object p=XposedHelpers.callMethod(group,"getPreference",i);
            // Categories are layout containers. Nested screens remain intact.
            if(categoryType.isInstance(p)){apply(p,inventory,depth+1);continue;}
            String id=id(p); if(id.isEmpty())continue;
            if(!tracked.containsKey(p))tracked.put(p,(Boolean)XposedHelpers.callMethod(p,"isVisible"));
            if(inventory!=null) {
                Object title=XposedHelpers.callMethod(p,"getTitle");
                Object key=XposedHelpers.callMethod(p,"getKey");
                JSONObject entry=new JSONObject();entry.put("id",id);entry.put("title",title==null?"":title.toString());
                entry.put("key",key==null?"":key.toString()); inventory.put(entry);
            }
            boolean visible=Boolean.TRUE.equals(tracked.get(p)) && !(enabled && hidden.contains(id));
            busy=true;
            try{XposedHelpers.callMethod(p,"setVisible",visible);}finally{busy=false;}
        }
    }
    private String id(Object p) {
        Object key=XposedHelpers.callMethod(p,"getKey");
        if(key!=null && !key.toString().isEmpty())return "key:"+key;
        Intent intent=(Intent)XposedHelpers.callMethod(p,"getIntent");
        if(intent!=null)return "intent:"+intent.toUri(0);
        Object fragment=XposedHelpers.callMethod(p,"getFragment");
        if(fragment!=null && !fragment.toString().isEmpty())return "fragment:"+fragment;
        return ""; // Never hide an unidentified item by a potentially translated title.
    }
    private synchronized void log(Throwable t) {
        if(logCount++<10)XposedBridge.log("SettingsMenuHider: "+t);
    }
}
