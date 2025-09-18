# Java Layer-4 (TCP) Load Balancer

## 1. Overview
A minimal Layer-4 (transport level) TCP load balancer written in Java. It listens on a single port, accepts multiple concurrent client connections, picks a backend server using a pluggable selection strategy, and proxies raw bidirectional byte streams between the client and the chosen backend.

---
## 2. Main Features
| Capability | Description                                                |
|------------|------------------------------------------------------------|
| Multiple backends | Loaded at startup from a JSON config file.                 |
| Pluggable strategies | Round‑robin (default), random, least-connections.          |
| Active connection counting | Tracks live connections per backend for least-connections. |
| Failure marking | Backend marked unhealthy if initial connect fails.         |
| Graceful shutdown | CTRL+C triggers a shutdown hook that stops accept loop.    |
| Simple logging | Uses System.out / System.err for simplicity.               |


---
### Key Classes
| Class | Role |
|-------|------|
| `LoadBalancer` | Loads config, accepts sockets, selects backend, spawns handlers. |
| `ConnectionHandler` | Proxies bytes both ways and manages active connection count. |
| `Server` | Backend definition (host, port) + connection & health state. |
| `ServerSelectionStrategy` | SPI for selection algorithms. |
| `RoundRobinSelectionStrategy` | Cycles through backend list in order. |
| `RandomSelectionStrategy` | Uniform random selection. |
| `LeastConnectionsSelectionStrategy` | Chooses backend with fewest active connections. |
| `LoadBalancerApplication` | CLI entrypoint & argument parsing. |

---
## 4. Project Structure
```
src/main/java/com/payroc/interviews/
  ConnectionHandler.java
  LoadBalancer.java
  LoadBalancerApplication.java
  Server.java
  ServerSelectionStrategy.java
  RoundRobinSelectionStrategy.java
  RandomSelectionStrategy.java
  LeastConnectionsSelectionStrategy.java

src/test/java/com/payroc/interviews/
  *Test.java  (unit + integration tests)
```
Build: Gradle (Kotlin DSL). Tests: JUnit 5.

---
## 5. Configuration File (backends.json)
A JSON array of backend definitions:
```json
[
  { "host": "127.0.0.1", "port": 9101 },
  { "host": "127.0.0.1", "port": 9102 }
]
```
Place it in the project root (or any path you pass via `--config`).

**Validation Notes**
- Missing or empty file => load balancer starts with zero backends and closes new client connections immediately.
- Malformed JSON => startup error with message from Jackson.

---
## 6. Building & Testing
### Clean build & run tests
```bash
./gradlew clean test
```
Test report: `build/reports/tests/test/index.html`.

### Assemble runnable JAR
```bash
./gradlew clean build
ls build/libs
```
Produces: `LoadBalancerApplication-<version>.jar`

---
## 7. Running the Load Balancer
### Via Gradle (development)
Round-robin (default):
```bash
./gradlew run --args="--config backends.json --port 9000"
```
Specify strategy:
```bash
./gradlew run --args="--config backends.json --port 9000 --strategy leastconn"
./gradlew run --args="--config backends.json --strategy random"
```
Environment override:
```bash
LB_PORT=7000 ./gradlew run --args="--config backends.json"
```
(Explicit `--port` always wins over `LB_PORT`.)

### Via Jar
```bash
java -jar build/libs/LoadBalancerApplication-1.0-SNAPSHOT.jar \
  --config backends.json --port 9000 --strategy roundrobin
```

### Quick Manual Test
1. Start two servers (example using `nc`):
   ```bash
   nc -l 9101 &   # Terminal 1
   nc -l 9102 &   # Terminal 2
   ```
2. Start the load balancer.
3. Connect multiple times:
   ```bash
   nc 127.0.0.1 9000
   ```
   Type text, press CTRL+D (Unix) or CTRL+Z then Enter (Windows) to close.
4. Observe alternating (round-robin) distribution if you add distinguishing banners on backends.

---
## 8. Command-Line Arguments
| Flag | Required | Description | Default |
|------|----------|-------------|---------|
| `--config <path>` | Yes | JSON backend list file | N/A |
| `--port <n>` | No | Listening port | 8080 or `LB_PORT` env |
| `--strategy <name>` | No | `roundrobin`, `random`, `leastconn` | roundrobin |
| `--help` | No | Show usage | - |

Invalid / unknown flags produce usage help and exit.

---
## 9. Strategies
| Strategy | Selection Logic | Use Case |
|----------|-----------------|----------|
| roundrobin | Index cycles through list | Even distribution, similar backends |
| random | Random index | Quick smoke tests / distribution variance |
| leastconn | Minimum `activeConnections` | Mixed performance or bursty loads |

**Health**: Currently only marked unhealthy on initial connect failure inside `ConnectionHandler`. (Strategies do **not** skip unhealthy servers yet—extension point.)

---
## 14. Testing Summary
Run `./gradlew test` to execute:
- Strategy unit tests
- Server state tests
- Connection pipeline test
- Load balancer integration test with ephemeral servers

---
## 16. Quick Start (Copy/Paste)
```bash
cat > backends.json <<'JSON'
[
  { "host": "127.0.0.1", "port": 9101 },
  { "host": "127.0.0.1", "port": 9102 }
]
JSON

# Start dummy servers (Unix example)
nc -l 9101 &
nc -l 9102 &

# Run load balancer
./gradlew run --args="--config backends.json --port 9000 --strategy roundrobin"

# In a new terminal, test
printf 'hello1\n' | nc 127.0.0.1 9000
```
