package io.github.manorcompanion;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public final class OverlayService extends Service {
    public static boolean active;
    private static final String CHANNEL = "manual_timers";
    private static final String STOP = "io.github.manorcompanion.STOP";
    private WindowManager manager;
    private WindowManager.LayoutParams params;
    private LinearLayout panel;
    private AppStore store;
    private boolean collapsed;
    private String structure = "";
    private final List<Runnable> labels = new ArrayList<>();
    private final Runnable observer = this::refresh;

    @android.annotation.SuppressLint("RtlHardcoded") // Drag coordinates intentionally use the physical left edge.
    @Override public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        store = AppStore.get(this);
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"悬浮计时",NotificationManager.IMPORTANCE_LOW));
        Intent open = new Intent(this,MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this,1,new Intent(this,OverlayService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this,CHANNEL).setSmallIcon(io.github.manorcompanion.R.drawable.ic_timer)
            .setContentTitle("庄园手记 · 悬浮计时已开启").setContentText("手动点按计时，点此返回应用")
            .setContentIntent(content).setOngoing(true).addAction(new Notification.Action.Builder(null,"关闭悬浮窗",stop).build()).build();
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(1,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(1,notification);
            params = new WindowManager.LayoutParams(width(),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP|Gravity.LEFT;
            android.content.SharedPreferences prefs = getSharedPreferences("overlay",MODE_PRIVATE);
            params.x = prefs.getInt("x",16); params.y = prefs.getInt("y",120);
            panel = Ui.column(this); panel.setPadding(dp(10),dp(10),dp(10),dp(10));
            panel.setBackground(Ui.shape(Ui.BG,dp(18))); panel.setElevation(dp(8));
            buildPanel(); manager.addView(panel,params); active=true; clamp(); store.observe(observer);
        } catch (RuntimeException e) {
            Toast.makeText(this,"无法显示悬浮窗，请检查系统权限后重试",Toast.LENGTH_LONG).show(); stopSelf();
        }
    }
    private int dp(float value) { return Ui.dp(this,value); }
    private int width() { return Math.min(dp(collapsed ? 190 : 276), getResources().getDisplayMetrics().widthPixels - dp(16)); }
    private String signature() {
        StringBuilder s = new StringBuilder();
        for (TimerState t : store.timers) s.append(t.id).append(':').append(t.name).append(':').append(t.durationMs).append(';');
        return s.toString();
    }
    @android.annotation.SuppressLint("ClickableViewAccessibility") // ACTION_UP calls performClick; TalkBack uses its click listener.
    private void buildPanel() {
        panel.removeAllViews(); labels.clear(); structure=signature();
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView drag = Ui.text(this,"⠿ 手记",14,Ui.ACCENT); drag.setGravity(Gravity.CENTER_VERTICAL); drag.setMinHeight(dp(48));
        drag.setContentDescription("拖动此处移动悬浮窗，点按收起或展开");
        header.addView(drag,new LinearLayout.LayoutParams(0,-2,1));
        drag.setOnClickListener(v -> togglePanel()); drag.setOnTouchListener(new View.OnTouchListener() {
            float downX,downY; int x,y; boolean moved;
            @Override public boolean onTouch(View v,MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX=event.getRawX(); downY=event.getRawY(); x=params.x; y=params.y; moved=false; return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=event.getRawX()-downX, dy=event.getRawY()-downY;
                        if (Math.abs(dx)+Math.abs(dy)>dp(6)) moved=true;
                        if (moved) { params.x=x+(int)dx; params.y=y+(int)dy; clamp(); } return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) v.performClick(); else savePosition(); return true;
                    case MotionEvent.ACTION_CANCEL: savePosition(); return true;
                    default: return false;
                }
            }
        });
        if (!collapsed) header.addView(Ui.button(this,"收起",false,v -> togglePanel()));
        Button close = Ui.button(this,"×",false,v -> stopSelf()); close.setContentDescription("关闭悬浮窗");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(48),dp(48)); cp.leftMargin=dp(6); header.addView(close,cp);
        panel.addView(header);
        if (collapsed) {
            TextView summary = Ui.text(this,"",14,Ui.TEXT); summary.setPadding(dp(4),dp(8),dp(4),dp(6)); panel.addView(summary);
            labels.add(() -> {
                TimerState nearest=null; long now=SystemClock.elapsedRealtime(); int count=0;
                for (TimerState t : store.timers) if (t.running) { count++; if (nearest==null || t.remaining(now)<nearest.remaining(now)) nearest=t; }
                summary.setText(nearest==null ? "点击手记展开" : count+" 项计时 · "+TimerState.format(nearest.seconds(now)));
            });
        } else {
            Ui.gap(panel,8); ScrollView scroll = new ScrollView(this); LinearLayout body=Ui.column(this);
            for (TimerState t : store.timers) {
                Button row = Ui.button(this,"",false,v -> store.toggle(t)); row.setTextSize(13);
                row.setOnLongClickListener(v -> { t.reset(); store.changed(); return true; });
                LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(56)); rp.bottomMargin=dp(6); body.addView(row,rp);
                labels.add(() -> {
                    String text=t.name+"  "+TimerState.format(t.seconds(SystemClock.elapsedRealtime()))+"  "+(t.running?"Ⅱ":"▷");
                    if (!row.getText().toString().equals(text)) row.setText(text);
                    row.setContentDescription(t.name+"，剩余 "+t.seconds(SystemClock.elapsedRealtime())+" 秒，点按"+(t.running?"暂停":"开始")+"，长按重置");
                });
            }
            scroll.addView(body);
            int maxHeight = Math.max(dp(62),getResources().getDisplayMetrics().heightPixels-dp(180));
            panel.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(Math.min(4,store.timers.size())*62),maxHeight)));
            Ui.gap(panel,6); panel.addView(Ui.text(this,"点按开始 / 暂停 · 长按重置",11,Ui.MUTED));
        }
        for (Runnable r : labels) r.run();
        panel.post(this::clamp);
    }
    private void togglePanel() { collapsed=!collapsed; params.width=width(); buildPanel(); clamp(); }
    private void refresh() {
        if (panel==null) return;
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        if (!structure.equals(signature())) buildPanel();
        for (Runnable r : labels) r.run();
    }
    private void clamp() {
        if (panel==null || !panel.isAttachedToWindow()) return;
        int w=getResources().getDisplayMetrics().widthPixels,h=getResources().getDisplayMetrics().heightPixels;
        params.x=Math.max(0,Math.min(params.x,Math.max(0,w-params.width)));
        params.y=Math.max(dp(24),Math.min(params.y,Math.max(dp(24),h-panel.getHeight()-dp(40))));
        try { manager.updateViewLayout(panel,params); } catch (IllegalArgumentException ignored) { stopSelf(); }
    }
    private void savePosition() { getSharedPreferences("overlay",MODE_PRIVATE).edit().putInt("x",params.x).putInt("y",params.y).apply(); }
    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config); if (panel!=null) { params.width=width(); buildPanel(); clamp(); }
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if (intent!=null && STOP.equals(intent.getAction())) stopSelf(); return START_NOT_STICKY;
    }
    @Override public void onDestroy() {
        active=false;
        if (store!=null) store.unobserve(observer);
        if (panel!=null && panel.isAttachedToWindow()) { savePosition(); manager.removeView(panel); }
        panel=null; stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
