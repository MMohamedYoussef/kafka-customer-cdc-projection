# Customer CDC Profile Projection

A customer-service view needs the newest profile state even when database change events arrive late or are retried. This is a reproducible, synthetic-data Java 21 + Apache Kafka 4.3.1 portfolio project.

```mermaid
flowchart LR
    F[CSV fixture events] --> P[Kafka producer]
    P --> T[Single-partition topic]
    T --> C[Kafka consumer]
    C --> R[Deterministic reducer]
    R --> O[CSV decision output]
```

The offline path feeds the same reducer directly and needs no broker. The live path creates a uniquely named one-partition topic, publishes each fixture record, consumes those records, and prints the decision output. A unique topic prevents prior runs from contaminating a demonstration.

## Data contract

Input: `event_id,customer_id,version,operation,segment`, where operation is `UPSERT` or `DELETE`.

Output: `customer_id,version,segment`. See [the checked-in example](examples/result.csv).

- Apply only a newer version per customer; keep delete tombstones so an older upsert cannot resurrect a deleted profile.
- Reject conflicting changes at the same version and conflicting duplicate event IDs.
- Output active profiles sorted by customer ID; an empty stream yields only the CSV header.

## Run

Install a Java 21 JDK and [Apache Kafka 4.3.1](https://kafka.apache.org/downloads/), then set `JAVA_HOME` to the JDK directory and `KAFKA_HOME` to the Kafka distribution directory containing `libs/`. From this repository's root in PowerShell:

```powershell
.\build.ps1
.\build.ps1 -Demo
```

The first command compiles the Java source and runs the deterministic behavior tests. The second also prints the fixture result. Build classes remain under ignored `build/`. To exercise a running local broker:

```powershell
.\build.ps1 -Broker 'localhost:9092'
```

The Kafka path is a bounded demo, not a long-running production service. It assumes ordered per-key events in one partition, uses synthetic data, and has no authentication or retention policy. Adapt those choices before connecting real data.
