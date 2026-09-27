import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    private record Profile(long version, String segment, boolean deleted) {}

    public static List<String> process(List<String> records) {
        Map<String, String> seen = new HashMap<>();
        Map<String, Profile> profiles = new HashMap<>();
        for (String record : records) {
            String[] row = record.split(",", -1);
            if (row.length != 5 || row[0].isBlank() || row[1].isBlank()) throw new IllegalArgumentException("Expected event_id,customer_id,version,operation,segment");
            long version = Long.parseLong(row[2]);
            if (version < 0 || !(row[3].equals("UPSERT") || row[3].equals("DELETE"))) throw new IllegalArgumentException("Invalid version or operation");
            if (row[3].equals("UPSERT") && row[4].isBlank()) throw new IllegalArgumentException("UPSERT requires segment");
            if (row[3].equals("DELETE") && !row[4].isBlank()) throw new IllegalArgumentException("DELETE must have empty segment");
            String oldEvent = seen.putIfAbsent(row[0], record);
            if (oldEvent != null) {
                if (!oldEvent.equals(record)) throw new IllegalArgumentException("Conflicting duplicate event ID: " + row[0]);
                continue;
            }
            Profile next = new Profile(version, row[4], row[3].equals("DELETE"));
            Profile previous = profiles.get(row[1]);
            if (previous == null || version > previous.version()) profiles.put(row[1], next);
            else if (version == previous.version() && !next.equals(previous)) throw new IllegalArgumentException("Conflicting customer version: " + row[1]);
        }
        List<String> output = new ArrayList<>(List.of("customer_id,version,segment"));
        profiles.keySet().stream().sorted().forEach(id -> {
            Profile profile = profiles.get(id);
            if (!profile.deleted()) output.add(id + "," + profile.version() + "," + profile.segment());
        });
        return output;
    }

    public static void main(String[] args) throws Exception {
        List<String> records = Files.readAllLines(Path.of("fixtures/events.csv"), StandardCharsets.UTF_8).stream().filter(line -> !line.isBlank()).toList();
        if (args.length > 0) {
            if (!"--broker".equals(args[0]) || args.length < 2) throw new IllegalArgumentException("Usage: App [--broker bootstrap-server [topic-prefix]]");
            records = BrokerReplay.replay(records, args[1], args.length > 2 ? args[2] : "customer-cdc");
        }
        for (String line : process(records)) System.out.println(line);
    }
}
