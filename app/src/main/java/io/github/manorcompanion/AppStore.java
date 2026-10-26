package io.github.manorcompanion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Activity and overlay share one main-thread store; mutations are persisted immediately. */
public final class AppStore {
    // Only the application context enters the singleton, never an Activity or Service.
    @android.annotation.SuppressLint("StaticFieldLeak")
    private static AppStore instance;
    public static synchronized AppStore get(Context context) {
        if (instance == null) instance = new AppStore(context.getApplicationContext());
        return instance;
    }
    private final Context context;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Runnable> observers = new ArrayList<>();
    public final List<TimerState> timers = new ArrayList<>();
    public final List<JSONObject> matches = new ArrayList<>();
    public boolean vibrate;
    public static final int MAX_TIMERS = 8;
    public static final int MAX_MATCHES = 200;

    private AppStore(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("manor", Context.MODE_PRIVATE);
        vibrate = prefs.getBoolean("vibrate", true);
        try {
            JSONArray saved = new JSONArray(prefs.getString("timers", "[]"));
            boolean sameBoot = prefs.getInt("boot", -1) == boot();
            for (int i = 0; i < saved.length() && timers.size() < MAX_TIMERS; i++) {
                JSONObject o = saved.getJSONObject(i);
                TimerState t = new TimerState(o.getString("id"), o.getString("name"), o.getLong("duration"));
                t.pausedMs = Math.max(0, Math.min(t.durationMs, o.optLong("paused", t.durationMs)));
                t.deadlineMs = o.optLong("deadline", 0);
                t.running = sameBoot && o.optBoolean("running");
                if (!sameBoot && o.optBoolean("running")) t.reset();
                t.finishIfDue(SystemClock.elapsedRealtime());
                timers.add(t);
            }
        } catch (JSONException | IllegalArgumentException ignored) { timers.clear(); }
        if (timers.isEmpty()) {
            timers.add(new TimerState(UUID.randomUUID().toString(), "技能计时 A", 60000));
            timers.add(new TimerState(UUID.randomUUID().toString(), "技能计时 B", 90000));
            timers.add(new TimerState(UUID.randomUUID().toString(), "自定义事件", 120000));
        }
        try {
            JSONArray saved = new JSONArray(prefs.getString("matches", "[]"));
            for (int i = 0; i < saved.length() && i < MAX_MATCHES; i++) matches.add(saved.getJSONObject(i));
        } catch (JSONException ignored) { matches.clear(); }
    }
    private int boot() { return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -2); }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            boolean completed = false;
            for (TimerState t : timers) completed |= t.finishIfDue(SystemClock.elapsedRealtime());
            if (completed) {
                save();
                if (vibrate) {
                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator())
                        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }
            notifyObservers();
            if (!observers.isEmpty()) handler.postDelayed(this, 250);
        }
    };
    public void observe(Runnable observer) {
        if (observers.contains(observer)) return;
        observers.add(observer);
        if (observers.size() == 1) { handler.removeCallbacks(tick); handler.post(tick); }
    }
    public void unobserve(Runnable observer) {
        observers.remove(observer);
        if (observers.isEmpty()) handler.removeCallbacks(tick);
    }
    private void notifyObservers() { for (Runnable r : new ArrayList<>(observers)) r.run(); }
    public void changed() { save(); notifyObservers(); }
    public void toggle(TimerState t) {
        long now = SystemClock.elapsedRealtime();
        if (t.running) t.pause(now); else t.start(now);
        changed();
    }
    public void resetAll() { for (TimerState t : timers) t.reset(); changed(); }
    public void addMatch(JSONObject match) {
        if (matches.size() >= MAX_MATCHES) throw new IllegalStateException("Record limit");
        matches.add(0, match);
        changed();
    }
    private void save() {
        JSONArray ts = new JSONArray();
        try {
            for (TimerState t : timers) ts.put(new JSONObject().put("id", t.id).put("name", t.name)
                .put("duration", t.durationMs).put("paused", t.pausedMs)
                .put("deadline", t.deadlineMs).put("running", t.running));
        } catch (JSONException e) { throw new IllegalStateException(e); }
        prefs.edit().putString("timers", ts.toString()).putInt("boot", boot())
            .putString("matches", new JSONArray(matches).toString()).putBoolean("vibrate", vibrate).apply();
    }
}
