package com.swissonline.api;

import com.swissonline.api.dto.*;
import com.swissonline.core.TournamentStore;
import com.swissonline.core.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins="*")
public class TournamentController {

  private final TournamentStore store;

  // ===== Player reports (web -> server) =====
  // tournamentKey -> (tableNumber -> report)
  private final Map<String, Map<Integer, ReportEntry>> reportsByTournament = new ConcurrentHashMap<>();

  public TournamentController(TournamentStore store) {
    this.store = store;
  }

  // ===== DTOs for reporting =====
  public static class ReportRequest {
    public String playerId;   // tko prijavljuje (ID igrača)
    public String result;     // "P1_WIN" | "P2_WIN" | "BOTH_LOSE"
  }

  public static class ReportEntry {
    public int table;
    public String playerId;
    public String result;
    public long timeEpochMillis;
  }

  private Map<Integer, ReportEntry> reportsMapFor(String key) {
    return reportsByTournament.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
  }

  private static boolean isDecided(String result) {
    if (result == null) return false;
    String r = result.trim();
    return !r.isEmpty() && !"UNDECIDED".equalsIgnoreCase(r);
  }

  private static boolean isValidReportResult(String r) {
    if (r == null) return false;
    String v = r.trim();
    return v.equals("P1_WIN") || v.equals("P2_WIN") || v.equals("BOTH_LOSE");
  }

  private static boolean matchContainsPlayer(MatchInfo m, String playerId) {
    return m != null && playerId != null
        && ((m.p1 != null && playerId.equals(m.p1.id)) || (m.p2 != null && playerId.equals(m.p2.id)));
  }

  // ===== EXISTING API =====

  @PostMapping("/tournaments")
  public ResponseEntity<TournamentState> createTournament(@RequestBody CreateTournamentRequest req) {
    if (req == null || req.key == null || req.key.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    if (req.name == null || req.name.isBlank()) req.name = "Tournament";

    TournamentState st = store.createOrGet(req.key, req.name);

    // init reports map
    reportsMapFor(req.key);

    store.broadcast(req.key, "TOURNAMENT", st);
    return ResponseEntity.ok(st);
  }

  @PutMapping("/tournaments/{key}/players")
  public ResponseEntity<TournamentState> upsertPlayers(@PathVariable String key, @RequestBody UpsertPlayersRequest req) {
    TournamentState st = store.get(key);
    if (st == null) return ResponseEntity.notFound().build();
    if (req == null || req.players == null) return ResponseEntity.badRequest().build();

    // st.playersById je mapa (id -> PlayerInfo)
    // možeš odlučiti želiš li clear ili merge; ovdje je merge kao i prije
    for (PlayerInfo p : req.players) {
      if (p == null || p.id == null || p.id.isBlank()) continue;
      st.playersById.put(p.id, p);
    }

    store.persist(st);
    store.broadcast(key, "PLAYERS_UPDATED", st.playersById.values());
    return ResponseEntity.ok(st);
  }

  @PutMapping("/tournaments/{key}/pairings")
  public ResponseEntity<TournamentState> publishPairings(@PathVariable String key, @RequestBody PublishPairingsRequest req) {
    TournamentState st = store.get(key);
    if (st == null) return ResponseEntity.notFound().build();
    if (req == null || req.matches == null) return ResponseEntity.badRequest().build();

    st.currentRound = req.roundNumber;
    st.currentPairings = req.matches;

    // ===== housekeeping reports =====
    // - obriši reportove za stolove koji više ne postoje
    // - obriši reportove za stolove gdje je admin već upisao rezultat
    Map<Integer, ReportEntry> rep = reportsMapFor(key);
    Set<Integer> currentTables = new HashSet<>();

    if (st.currentPairings != null) {
      for (MatchInfo m : st.currentPairings) {
        if (m == null) continue;
        currentTables.add(m.table);

        // ako admin rezultat postoji, report za taj stol nema smisla
        if (isDecided(m.result)) {
          rep.remove(m.table);
        }
      }
    }

    rep.keySet().removeIf(t -> !currentTables.contains(t));

    store.persist(st);

    Map<String, Object> payload = new HashMap<>();
    payload.put("round", st.currentRound);
    payload.put("pairings", st.currentPairings);

    store.broadcast(key, "PAIRINGS_UPDATED", payload);
    return ResponseEntity.ok(st);
  }

  @PostMapping("/tournaments/{key}/timer/start")
  public ResponseEntity<TimerState> startTimer(@PathVariable String key, @RequestBody StartTimerRequest req) {
    TournamentState st = store.get(key);
    if (st == null) return ResponseEntity.notFound().build();
    if (req == null) return ResponseEntity.badRequest().build();

    st.timer.running = true;
    st.timer.paused = false;
    st.timer.startEpochMillis = System.currentTimeMillis();
    st.timer.prepSeconds = req.prepSeconds > 0 ? req.prepSeconds : 180;
    st.timer.roundSeconds = req.roundSeconds > 0 ? req.roundSeconds : (55 * 60);

    store.persist(st);
    store.broadcast(key, "TIMER_STARTED", st.timer);
    return ResponseEntity.ok(st.timer);
  }

  @PostMapping("/tournaments/{key}/timer/stop")
  public ResponseEntity<TimerState> stopTimer(@PathVariable String key) {
    TournamentState st = store.get(key);
    if (st == null) return ResponseEntity.notFound().build();

    st.timer.running = false;
    st.timer.paused = false;
    store.persist(st);
    store.broadcast(key, "TIMER_STOPPED", st.timer);
    return ResponseEntity.ok(st.timer);
  }

  @GetMapping("/tournaments/{key}/player/{playerId}")
  public ResponseEntity<PlayerViewResponse> getPlayerView(@PathVariable String key, @PathVariable String playerId) {
    TournamentState st = store.get(key);
    if (st == null) return ResponseEntity.notFound().build();

    PlayerViewResponse res = new PlayerViewResponse();
    res.tournamentKey = st.key;
    res.tournamentName = st.name;
    res.currentRound = st.currentRound;
    res.timer = st.timer;
    res.pairings = st.currentPairings == null ? new ArrayList<>() : st.currentPairings;

    PlayerInfo me = st.playersById.get(playerId);
    res.registered = (me != null);
    res.me = me;

    if (me != null && st.currentPairings != null) {
      for (MatchInfo m : st.currentPairings) {
        if (m == null) continue;
        if (matchContainsPlayer(m, playerId)) {
          res.myMatch = m;
          break;
        }
      }
    }

    return ResponseEntity.ok(res);
  }

  @GetMapping("/tournaments/{key}/events")
  public SseEmitter events(@PathVariable String key) {
    return store.subscribe(key);
  }

  // ===== NEW: Player report result =====

  @PostMapping("/tournaments/{key}/report")
  public ResponseEntity<?> reportResult(@PathVariable String key, @RequestBody ReportRequest req) {
    TournamentState st = store.get(key);
    if (st == null) return ResponseEntity.notFound().build();

    if (req == null || req.playerId == null || req.playerId.trim().isEmpty()
        || req.result == null || req.result.trim().isEmpty()) {
      return ResponseEntity.badRequest().body("Invalid report");
    }

    String playerId = req.playerId.trim();
    String r = req.result.trim();

    if (!isValidReportResult(r)) {
      return ResponseEntity.badRequest().body("Invalid result");
    }

    // find match in current pairings
    MatchInfo match = null;
    if (st.currentPairings != null) {
      for (MatchInfo m : st.currentPairings) {
        if (m == null) continue;
        if (matchContainsPlayer(m, playerId)) {
          match = m;
          break;
        }
      }
    }

    if (match == null) {
      return ResponseEntity.status(404).body("Player is not in any current match");
    }

    // block if admin already set result
    if (isDecided(match.result)) {
      return ResponseEntity.status(409).body("Admin already set result");
    }

    // BYE match -> nema smisla report (p2==null)
    if (match.p2 == null) {
      return ResponseEntity.badRequest().body("Cannot report BYE match");
    }

    ReportEntry entry = new ReportEntry();
    entry.table = match.table;
    entry.playerId = playerId;
    entry.result = r;
    entry.timeEpochMillis = System.currentTimeMillis();

    reportsMapFor(key).put(entry.table, entry);

    // notify listeners (nije obavezno za web; admin će dohvatiti /reports u koraku 3)
    store.broadcast(key, "REPORT_RECEIVED", entry);

    return ResponseEntity.ok(entry);
  }

  @GetMapping("/tournaments/{key}/reports")
  public ResponseEntity<List<ReportEntry>> getReports(@PathVariable String key) {
    TournamentState st = store.get(key);
    if (st == null) return ResponseEntity.notFound().build();

    Map<Integer, ReportEntry> rep = reportsMapFor(key);
    List<ReportEntry> list = new ArrayList<>(rep.values());
    list.sort(Comparator.comparingInt(a -> a.table));
    return ResponseEntity.ok(list);
  }
}