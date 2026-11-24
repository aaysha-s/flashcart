# Live stock updates: latency with many viewers (24 Nov 2026)

**Path measured:** stock change in inventory-service (timestamp taken just before the DB commit) →
Kafka `stock.changed` → live-service → Redis pub/sub → WebSocket → viewer.

**Setup:** laptop (i5-10210U, 8 GB), all services and k6 in Docker Compose on one host, so they share
one clock. `loadtest/live-viewers.js`: N WebSocket viewers connect over 10 s; buyers then place
5 orders/s for 2 min, and every viewer records `receive time − changedAtEpochMs` for each update.

| Viewers | Connected | Latency median | **Latency p95** | Updates delivered | Messages/s | Failed orders |
|---|---|---|---|---|---|---|
| 200 | 200/200 | 21 ms | **47 ms** | 130,200 (651 per viewer) | ~700 | 0 |
| 500 | 500/500 | 23 ms | **39 ms** | 331,500 (663 per viewer) | ~1,980 | 0 |

Every viewer received every update. Latency stayed flat from 200 to 500 viewers on a single
live-service instance.

**Lessons from the first attempt:**
- Connecting all viewers in the same millisecond overflowed Tomcat's accept backlog (100), so half
  were refused. Spreading connections over 10 s fixed it.
- A forgotten kind cluster was running in the background and pushed the laptop into swap, adding
  about 80 s of lag. Stopping it restored normal latency.
- The Kafka health check inherited Kafka's 512 MB heap setting and timed out under memory pressure.
  It now runs with `-Xmx64m`.