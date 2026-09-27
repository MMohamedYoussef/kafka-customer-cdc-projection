import java.util.List;

public class AppTest {
    private static void same(List<String> expected, List<String> actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected=" + expected + " actual=" + actual);
    }

    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("invalid change was accepted");
    }

    public static void main(String[] args) {
        same(List.of("customer_id,version,segment", "C2,1,VIP"), App.process(List.of(
                "a,C1,1,UPSERT,BASIC", "b,C1,3,DELETE,", "c,C1,2,UPSERT,VIP", "d,C2,1,UPSERT,VIP")));
        rejects(() -> App.process(List.of("a,C1,1,MERGE,BASIC")));
        rejects(() -> App.process(List.of("a,C1,1,UPSERT,BASIC", "a,C1,1,UPSERT,VIP")));
        rejects(() -> App.process(List.of("a,C1,1,UPSERT,BASIC", "b,C1,1,UPSERT,VIP")));
        same(List.of("customer_id,version,segment"), App.process(List.of()));
        System.out.println("cdc tests passed");
    }
}
