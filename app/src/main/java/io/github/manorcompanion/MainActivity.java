package io.github.manorcompanion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MainActivity extends Activity {
    private AppStore store;
    private LinearLayout content;
    private final List<Runnable> timerLabels = new ArrayList<>();
    private final Runnable observer = () -> { for (Runnable r : new ArrayList<>(timerLabels)) r.run(); };
    private int page;
    private String pendingCsv;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = AppStore.get(this);
        if (state != null) { page = state.getInt("page"); pendingCsv = state.getString("csv"); }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putInt("page", page); state.putString("csv", pendingCsv);
    }
    @Override protected void onResume() { super.onResume(); store.observe(observer); render(); }
    @Override protected void onPause() { store.unobserve(observer); super.onPause(); }

    private void render() {
        timerLabels.clear();
        LinearLayout root = Ui.column(this); root.setBackgroundColor(Ui.BG);
        // API 35 enforces edge-to-edge; consume insets around all interactive content.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets;
        });
        LinearLayout header = Ui.column(this); header.setPadding(dp(24),dp(24),dp(24),dp(16));
        header.addView(Ui.text(this,"MANOR / COMPANION",11,Ui.ACCENT)); Ui.gap(header,8);
        header.addView(Ui.heading(this,"庄园手记",30)); Ui.gap(header,6);
        header.addView(Ui.text(this,"第五人格 · 手动计时与复盘",14,Ui.MUTED)); root.addView(header);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = Ui.column(this); content.setPadding(dp(20),dp(4),dp(20),dp(24));
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if (page == 0) timersPage(); else if (page == 1) matchesPage(); else settingsPage();
        LinearLayout nav = new LinearLayout(this); nav.setPadding(dp(16),dp(10),dp(16),dp(10));
        String[] labels = {"计时台","对局记录","使用说明"};
        for (int i = 0; i < labels.length; i++) {
            final int target = i;
            Ui.weighted(nav,Ui.button(this,labels[i],page == i,v -> { page = target; render(); }));
        }
        root.addView(nav); setContentView(root); root.requestApplyInsets(); observer.run();
    }
    private int dp(float n) { return Ui.dp(this,n); }
    private void description(String text) { content.addView(Ui.text(this,text,13,Ui.MUTED)); Ui.gap(content,16); }
    private void timersPage() {
        LinearLayout hero = Ui.card(this);
        hero.addView(Ui.text(this,"手动掌握每一秒",20,Ui.TEXT)); Ui.gap(hero,8);
        hero.addView(Ui.text(this,"观察到技能使用后，点按开始。示例时长请按实际情况修改。",14,Ui.MUTED)); Ui.gap(hero,16);
        Button overlayButton = Ui.button(this,OverlayService.active ? "关闭悬浮窗" : "开启悬浮窗",true,v -> {
            if (OverlayService.active) { stopService(new Intent(this,OverlayService.class)); render(); }
            else launchOverlay();
        });
        hero.addView(overlayButton);
        timerLabels.add(() -> {
            String label = OverlayService.active ? "关闭悬浮窗" : "开启悬浮窗";
            if (!overlayButton.getText().toString().equals(label)) overlayButton.setText(label);
        });
        content.addView(hero); Ui.gap(content,20);
        for (TimerState timer : store.timers) {
            LinearLayout card = Ui.card(this);
            LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = Ui.heading(this,timer.name,17);
            top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            top.addView(Ui.button(this,"编辑",false,v -> editTimer(timer))); card.addView(top); Ui.gap(card,14);
            TextView time = Ui.text(this,"",42,Ui.ACCENT); time.setTypeface(Typeface.MONOSPACE); card.addView(time);
            TextView status = Ui.text(this,"",12,Ui.MUTED); Ui.gap(card,4); card.addView(status); Ui.gap(card,16);
            LinearLayout actions = new LinearLayout(this);
            Button toggle = Ui.button(this,"开始",true,v -> store.toggle(timer));
            Ui.weighted(actions,toggle);
            Ui.weighted(actions,Ui.button(this,"重置",false,v -> { timer.reset(); store.changed(); }));
            card.addView(actions);
            timerLabels.add(() -> {
                time.setText(TimerState.format(timer.seconds(SystemClock.elapsedRealtime())));
                status.setText(timer.running ? "计时中 · 总时长 " + timer.durationMs/1000 + " 秒" :
                    timer.pausedMs == 0 ? "计时结束" : timer.pausedMs == timer.durationMs ? "就绪 · " + timer.durationMs/1000 + " 秒" : "已暂停");
                String label = timer.running ? "暂停" : timer.pausedMs == 0 ? "重新开始" : timer.pausedMs == timer.durationMs ? "开始" : "继续";
                if (!toggle.getText().toString().equals(label)) toggle.setText(label);
            });
            content.addView(card); Ui.gap(content,12);
        }
        content.addView(Ui.button(this,"＋ 添加计时器",false,v -> editTimer(null))); Ui.gap(content,10);
        content.addView(Ui.button(this,"新一局 · 重置全部计时",false,v -> new AlertDialog.Builder(this)
            .setTitle("重置全部计时？").setMessage("所有计时器将回到初始时长。")
            .setNegativeButton("取消",null).setPositiveButton("重置",(d,w)->store.resetAll()).show()));
    }
    private EditText field(LinearLayout form,String hint,String value,int max,boolean multiline) {
        TextView label = Ui.text(this,hint,13,Ui.MUTED); form.addView(label);
        EditText e = new EditText(this); e.setText(value); e.setTextColor(Ui.TEXT); e.setTextSize(16);
        e.setSingleLine(!multiline); e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
        if (multiline) { e.setMinLines(3); e.setGravity(Gravity.TOP); }
        form.addView(e,new LinearLayout.LayoutParams(-1,-2)); Ui.gap(form,10); return e;
    }
    private LinearLayout form() {
        LinearLayout form = Ui.column(this); form.setPadding(dp(24),dp(12),dp(24),dp(8)); return form;
    }
    private ScrollView formScroll(LinearLayout form) { ScrollView s = new ScrollView(this); s.addView(form); return s; }
    private void editTimer(TimerState timer) {
        if (timer == null && store.timers.size() >= AppStore.MAX_TIMERS) { toast("最多添加 8 个计时器"); return; }
        LinearLayout form = form();
        EditText name = field(form,"计时名称",timer == null ? "" : timer.name,24,false);
        EditText seconds = field(form,"时长（1–3600 秒）",timer == null ? "60" : String.valueOf(timer.durationMs/1000),4,false);
        seconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        form.addView(Ui.text(this,"保存后，该计时器将重置为初始时长。",12,Ui.MUTED));
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(timer == null ? "添加计时器" : "编辑计时器")
            .setView(formScroll(form)).setNegativeButton("取消",null).setPositiveButton("保存",null);
        if (timer != null) builder.setNeutralButton("删除",(d,w) -> {
            if (store.timers.size() == 1) { toast("请至少保留一个计时器"); return; }
            new AlertDialog.Builder(this).setTitle("删除此计时器？").setMessage(timer.name)
                .setNegativeButton("取消",null).setPositiveButton("删除",(dialog,which)-> {
                    store.timers.remove(timer); store.changed(); render();
                }).show();
        });
        AlertDialog dialog = builder.create(); dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> {
            String label = name.getText().toString().trim();
            int duration;
            try { duration = Integer.parseInt(seconds.getText().toString()); } catch (NumberFormatException ex) { duration = 0; }
            if (label.isEmpty()) { name.setError("请输入名称"); return; }
            if (duration < 1 || duration > 3600) { seconds.setError("请输入 1–3600 秒"); return; }
            if (timer == null) store.timers.add(new TimerState(UUID.randomUUID().toString(),label,duration*1000L));
            else { timer.name = label; timer.durationMs = duration*1000L; timer.reset(); }
            store.changed(); dialog.dismiss(); render();
        })); dialog.show();
    }
    private void launchOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this).setTitle("允许显示悬浮窗")
                .setMessage("请在系统设置中允许“庄园手记”显示在其他应用上层。返回后，再点一次开启。")
                .setNegativeButton("取消",null).setPositiveButton("前往设置",(d,w)-> {
                    try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))); }
                    catch (android.content.ActivityNotFoundException e) { toast("请在系统设置中搜索“显示在其他应用上层”"); }
                }).show(); return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
        try {
            startForegroundService(new Intent(this,OverlayService.class));
            content.postDelayed(() -> { if (!isFinishing() && !isDestroyed()) render(); },500);
        } catch (RuntimeException e) { toast("悬浮窗启动失败，请检查系统权限"); }
    }
    private void matchesPage() {
        description("记录每局的选择与收获。所有数据仅存储在本机。");
        LinearLayout row = new LinearLayout(this);
        Ui.weighted(row,Ui.button(this,"＋ 记录一局",true,v -> editMatch(null)));
        Ui.weighted(row,Ui.button(this,"导出 CSV",false,v -> exportMatches())); content.addView(row); Ui.gap(content,20);
        int wins = 0, draws = 0;
        for (JSONObject m : store.matches) { if (m.optString("result").equals("胜利")) wins++; if (m.optString("result").equals("平局")) draws++; }
        LinearLayout stats = Ui.card(this);
        stats.addView(Ui.heading(this,store.matches.size()+" 局  /  "+wins+" 胜  /  "+draws+" 平",20)); Ui.gap(stats,8);
        stats.addView(Ui.text(this,"胜率 " + (store.matches.isEmpty() ? "—" : Math.round(100f*wins/store.matches.size())+"%") + " · 最多保存 200 条",13,Ui.MUTED));
        content.addView(stats); Ui.gap(content,16);
        if (store.matches.isEmpty()) description("还没有记录。打完一局，记下一个下次想改进的细节。");
        for (JSONObject match : store.matches) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.text(this,match.optString("result")+"  ·  "+match.optString("side"),13,Ui.ACCENT)); Ui.gap(card,10);
            card.addView(Ui.heading(this,match.optString("character","未填写角色")+" / "+match.optString("map","未填写地图"),18)); Ui.gap(card,8);
            card.addView(Ui.text(this,date(match.optLong("date")),12,Ui.MUTED));
            if (!match.optString("notes").isEmpty()) { Ui.gap(card,12); card.addView(Ui.text(this,match.optString("notes"),14,Ui.TEXT)); }
            Ui.gap(card,14); card.addView(Ui.button(this,"编辑记录",false,v -> editMatch(match)));
            content.addView(card); Ui.gap(content,12);
        }
    }
    private Spinner select(LinearLayout form,String label,String[] values,String current) {
        form.addView(Ui.text(this,label,13,Ui.MUTED)); Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);
        s.setAdapter(a); if (current != null) s.setSelection(Math.max(0,Arrays.asList(values).indexOf(current)));
        form.addView(s,new LinearLayout.LayoutParams(-1,dp(52))); Ui.gap(form,10); return s;
    }
    private void editMatch(JSONObject match) {
        if (match == null && store.matches.size() >= AppStore.MAX_MATCHES) { toast("已达 200 条，请先导出并删除部分记录"); return; }
        LinearLayout form = form();
        Spinner side = select(form,"阵营",new String[]{"求生者","监管者"},match == null ? null : match.optString("side"));
        Spinner result = select(form,"结果",new String[]{"胜利","平局","失败"},match == null ? null : match.optString("result"));
        EditText character = field(form,"角色",match == null ? "" : match.optString("character"),40,false);
        EditText map = field(form,"地图",match == null ? "" : match.optString("map"),40,false);
        EditText notes = field(form,"复盘笔记",match == null ? "" : match.optString("notes"),1000,true);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(match == null ? "记录一局" : "编辑记录")
            .setView(formScroll(form)).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)-> {
                try {
                    JSONObject m = match == null ? new JSONObject().put("date",System.currentTimeMillis()) : match;
                    m.put("side",side.getSelectedItem()).put("result",result.getSelectedItem())
                        .put("character",character.getText().toString().trim().isEmpty() ? "未填写角色" : character.getText().toString().trim())
                        .put("map",map.getText().toString().trim().isEmpty() ? "未填写地图" : map.getText().toString().trim())
                        .put("notes",notes.getText().toString().trim());
                    if (match == null) store.addMatch(m); else store.changed(); render();
                } catch (JSONException e) { toast("保存失败，请重试"); }
            });
        if (match != null) builder.setNeutralButton("删除",(d,w)-> new AlertDialog.Builder(this)
            .setTitle("删除这条对局记录？").setMessage("删除后无法恢复，建议先导出 CSV。")
            .setNegativeButton("取消",null).setPositiveButton("删除",(a,b)-> { store.matches.remove(match); store.changed(); render(); }).show());
        builder.show();
    }
    private String date(long timestamp) { return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.CHINA).format(new Date(timestamp)); }
    private void exportMatches() {
        if (store.matches.isEmpty()) { toast("还没有可导出的记录"); return; }
        StringBuilder csv = new StringBuilder("\uFEFF日期,阵营,结果,角色,地图,复盘笔记\r\n");
        for (JSONObject m : store.matches) {
            String[] cells = {date(m.optLong("date")),m.optString("side"),m.optString("result"),m.optString("character"),m.optString("map"),m.optString("notes")};
            for (int i=0;i<cells.length;i++) { if (i>0) csv.append(','); csv.append(Csv.cell(cells[i])); } csv.append("\r\n");
        }
        pendingCsv = csv.toString();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/csv").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE,"庄园手记-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+".csv");
        try { startActivityForResult(intent,12); }
        catch (android.content.ActivityNotFoundException e) { pendingCsv = null; toast("系统没有可用的文件保存程序"); }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if (request != 12) return;
        if (result == RESULT_OK && data != null && data.getData() != null && pendingCsv != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData(),"wt")) {
                if (out == null) throw new java.io.IOException("No output stream");
                out.write(pendingCsv.getBytes(StandardCharsets.UTF_8)); toast("CSV 已导出");
            } catch (java.io.IOException | SecurityException e) { toast("导出失败，请重试并选择其他位置"); }
        }
        pendingCsv = null;
    }
    private void settingsPage() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.heading(this,"手动操作 · 本地保存",20)); Ui.gap(card,14);
        card.addView(Ui.text(this,"1. 在计时台编辑名称与秒数。\n\n2. 观察到事件后，手动点开始；可暂停、继续或重置。\n\n3. 开启悬浮窗后，拖动顶部移动，点“收起”减少遮挡；点击计时行开始或暂停，长按该行重置。\n\n4. 对局结束后，在记录页填写结果与复盘，支持 CSV 导出。",15,Ui.TEXT));
        Ui.gap(card,16); Switch vibration = new Switch(this); vibration.setText("计时结束时震动");
        vibration.setTextColor(Ui.TEXT); vibration.setChecked(store.vibrate);
        vibration.setOnCheckedChangeListener((b,checked)-> { store.vibrate=checked; store.changed(); }); card.addView(vibration);
        content.addView(card); Ui.gap(content,16);
        LinearLayout about = Ui.card(this); about.addView(Ui.heading(this,"关于庄园手记",18)); Ui.gap(about,12);
        about.addView(Ui.text(this,"版本 1.0.0 · 非官方社区工具\n\n无账号、无广告、无联网权限。不会读取或修改游戏内存、文件或网络流量，也不提供识屏、自动操作或隐藏信息显示。\n\n所有计时均由你手动触发。默认 60 / 90 / 120 秒仅作示例，不代表游戏技能数据。\n\n需要安卓 8.0 或更高版本。部分系统或游戏可能限制悬浮窗；如被限制，请使用应用内计时。使用前请确认游戏当前规则，不承诺“零封号风险”。\n\n应用没有后台保活或精确闹钟权限。锁屏、被系统结束或关闭悬浮窗后，提醒可能延迟；返回应用时会校正剩余时间，重启手机后运行中的计时重置。\n\n记录存储在本机，卸载后会删除，请先导出重要记录。导出的 CSV 用于阅读和存档，当前版本不支持导入。\n\n《第五人格》相关名称与商标归各自权利人所有，本项目未使用官方图像素材。",14,Ui.MUTED));
        content.addView(about);
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
}
