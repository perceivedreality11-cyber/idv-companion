package io.github.manorcompanion;

/** Pure Java countdown model. All timestamps use a monotonic clock, in milliseconds. */
public final class TimerState {
    public final String id;
    public String name;
    public long durationMs;
    public long pausedMs;
    public long deadlineMs;
    public boolean running;

    public TimerState(String id, String name, long durationMs) {
        if (durationMs < 1000 || durationMs > 3600000) throw new IllegalArgumentException("Duration: 1–3600 seconds");
        this.id = id;
        this.name = name;
        this.durationMs = durationMs;
        reset();
    }

    public long remaining(long now) { return running ? Math.max(0, deadlineMs - now) : pausedMs; }
    public long seconds(long now) { return (remaining(now) + 999) / 1000; }
    public void start(long now) {
        if (running) return;
        if (pausedMs == 0) pausedMs = durationMs;
        deadlineMs = now + pausedMs;
        running = true;
    }
    public void pause(long now) { pausedMs = remaining(now); running = false; }
    public void reset() { running = false; pausedMs = durationMs; deadlineMs = 0; }
    public boolean finishIfDue(long now) {
        if (!running || remaining(now) > 0) return false;
        running = false;
        pausedMs = 0;
        return true;
    }
    public static String format(long seconds) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
