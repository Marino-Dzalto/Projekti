# Swiss Tournament Manager

A desktop application for managing **Swiss-format trading card game tournaments**, with real-time online synchronization, judge call support, and live standings.

---

## Features

- **Swiss pairing algorithm** with proper tie-breakers (OMW%, OOW%)
- **Top-cut elimination bracket** (single elimination after Swiss rounds)
- **Real-time sync** — judges see live pairings and standings on any device via ngrok tunnel
- **Judge call system** — players request a judge from the pairing sheet; judge is notified on desktop
- **Timer** — per-round countdown with prep phase, visible to all connected clients
- **Player database** — persistent local DB with guest and registered player support
- **Tournament history** — full standings snapshot saved after each tournament
- **Web reports** — fetch and apply results submitted via the online interface

---

## Architecture

```
swiss-tournament-manager/
├── SwissTournamentApp.java   # Desktop app (Java Swing)
├── SwissOnlineServer/        # Sync server (Spring Boot)
├── start_all.command         # One-click launcher (macOS)
├── config.properties         # Configuration
└── lib/                      # External libraries (Gson)
```

```
┌─────────────────────┐        HTTP/JSON       ┌──────────────────────┐
│   Desktop App        │ ◄────────────────────► │  Spring Boot Server  │
│  (SwingTournament)   │                         │  (localhost:8080)    │
└─────────────────────┘                         └──────────┬───────────┘
                                                           │ ngrok tunnel
                                                           ▼
                                                ┌──────────────────────┐
                                                │   Judge / Player     │
                                                │   (any browser)      │
                                                └──────────────────────┘
```

---

## Requirements

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ (bundled via `mvnw`) |
| ngrok | any |

---

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/marinodzalto/swiss-tournament-manager.git
cd swiss-tournament-manager
```

### 2. Configure (optional)

```bash
cp config.properties config.local.properties
# Edit config.local.properties to change server URL or port
```

### 3. Launch

**macOS** — double-click `Swiss Turnir` on the Desktop, or run:

```bash
./start_all.command
```

This will:
1. Start the Spring Boot sync server
2. Open an ngrok tunnel
3. Compile and launch the desktop app

---

## Configuration

| Key | Default | Description |
|-----|---------|-------------|
| `server.url` | `http://localhost:8080` | URL of the online sync server |

---

## How It Works

### Swiss Rounds
Players are paired by score group each round. The algorithm avoids rematches and handles byes for odd player counts. Tie-breakers use **Opponent Match Win % (OMW)** and **Opponent's Opponent Win % (OOW)** with a 33% floor, following standard tournament rules.

### Online Sync
The desktop app acts as the **source of truth**. Each tournament gets a unique key. The Spring Boot server holds the current state in memory; judges and players connect via the ngrok URL to see live pairings, report judge calls, and submit results via web forms.

### Top Cut
After Swiss rounds, the organizer can start a top-cut elimination bracket (top 8, top 4, etc.). Match results are entered directly; the bracket auto-advances winners.

---

## Development

### Build desktop app

```bash
# Compile (requires lib/gson.jar)
javac -cp "lib/gson.jar" SwissTournamentApp.java

# Package
jar cfm SwissTurnir.jar manifest.mf *.class lib/gson.jar
```

### Build server

```bash
cd SwissOnlineServer
./mvnw package
```

---

## License

MIT
