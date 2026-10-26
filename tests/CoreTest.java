import io.github.manorcompanion.TimerState;
import io.github.manorcompanion.Csv;

/** No Android runtime required: test actual countdown transitions and exported CSV fields. */
public class CoreTest {
    private static int checks;
    private static void eq(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        TimerState t = new TimerState("a","test",60000);
        eq(60L,t.seconds(0)); eq(false,t.running);
        t.start(1000); eq(60L,t.seconds(1000)); eq(60L,t.seconds(1001)); eq(59L,t.seconds(2000));
        t.start(5000); eq(61000L,t.deadlineMs); // Repeated start must not extend the countdown.
        t.pause(31000); eq(30000L,t.pausedMs); eq(30L,t.seconds(100000));
        t.start(100000); eq(130000L,t.deadlineMs);
        eq(false,t.finishIfDue(129999)); eq(1L,t.seconds(129999));
        eq(true,t.finishIfDue(130000)); eq(0L,t.seconds(130000)); eq(false,t.finishIfDue(140000));
        t.start(150000); eq(60L,t.seconds(150000));
        t.reset(); eq(false,t.running); eq(60000L,t.pausedMs);
        t.start(0); eq(0L,t.remaining(Long.MAX_VALUE)); t.pause(Long.MAX_VALUE); eq(0L,t.pausedMs);
        TimerState shortTimer = new TimerState("b","one second",1000);
        shortTimer.start(100); eq(1L,shortTimer.seconds(1099)); eq(true,shortTimer.finishIfDue(1100));
        boolean invalid=false;
        try { new TimerState("c","bad",0); } catch (IllegalArgumentException ex) { invalid=true; }
        eq(true,invalid);
        invalid=false;
        try { new TimerState("c","bad",3600001); } catch (IllegalArgumentException ex) { invalid=true; }
        eq(true,invalid);
        eq("00:00",TimerState.format(0)); eq("01:30",TimerState.format(90)); eq("60:00",TimerState.format(3600));
        eq("\"普通笔记\"",Csv.cell("普通笔记"));
        eq("\"a,\"\"b\"\"\n第二行\"",Csv.cell("a,\"b\"\n第二行"));
        eq("\"'=1+1\"",Csv.cell("=1+1")); eq("\"'  @SUM(A1)\"",Csv.cell("  @SUM(A1)"));
        eq("\"'\t-1\"",Csv.cell("\t-1")); eq("\"\"",Csv.cell(null));
        System.out.println("PASS: " + checks + " core checks");
    }
}
