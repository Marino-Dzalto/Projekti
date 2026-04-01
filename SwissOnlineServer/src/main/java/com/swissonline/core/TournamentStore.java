package com.swissonline.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swissonline.core.model.TournamentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TournamentStore {

  private static final Logger log = LoggerFactory.getLogger(TournamentStore.class);

  private final Map<String, TournamentState> tournaments = new ConcurrentHashMap<>();
  private final Map<String, List<SseEmitter>> emittersByKey = new ConcurrentHashMap<>();
  private final ObjectMapper om = new ObjectMapper();
  private final ExecutorService persistExecutor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "tournament-persist");
      t.setDaemon(true);
      return t;
  });

  private final File dataDir;

  public TournamentStore() {
    dataDir = new File("online_data");
    if (!dataDir.exists()) dataDir.mkdirs();
    loadAll();
  }

  public synchronized TournamentState createOrGet(String key, String name) {
    TournamentState st = tournaments.get(key);
    if (st == null) {
      st = new TournamentState(key, name);
      tournaments.put(key, st);
      persist(st);
    } else {
      // ako je name poslan, ažuriraj naziv
      if (name != null && !name.isBlank()) st.name = name;
      persist(st);
    }
    return st;
  }

  public TournamentState get(String key) {
    return tournaments.get(key);
  }

  public void persist(TournamentState st) {
    final String key = st.key;
    persistExecutor.submit(() -> {
      try {
        File f = new File(dataDir, "tournament_" + safe(keyOrEmpty(key)) + ".json");
        om.writerWithDefaultPrettyPrinter().writeValue(f, st);
      } catch (Exception e) {
        log.error("Failed to persist tournament '{}': {}", key, e.getMessage(), e);
      }
    });
  }

  private String safe(String s) {
    return s.replaceAll("[^a-zA-Z0-9_#-]", "_");
  }
  private String keyOrEmpty(String k) { return k == null ? "unknown" : k; }

  private void loadAll() {
    try {
      File[] files = dataDir.listFiles((dir, name) -> name.startsWith("tournament_") && name.endsWith(".json"));
      if (files == null) return;
      for (File f : files) {
        try {
          byte[] b = Files.readAllBytes(f.toPath());
          TournamentState st = om.readValue(b, TournamentState.class);
          if (st != null && st.key != null) {
            tournaments.put(st.key, st);
          } else {
            log.warn("Skipping corrupt tournament file (null state or key): {}", f.getName());
          }
        } catch (Exception e) {
          log.error("Failed to load tournament file '{}': {}", f.getName(), e.getMessage(), e);
        }
      }
    } catch (Exception e) {
      log.error("Failed to scan data directory '{}': {}", dataDir.getPath(), e.getMessage(), e);
    }
  }

  // SSE
  public SseEmitter subscribe(String key) {
    SseEmitter emitter = new SseEmitter(0L); // bez timeouta
    emittersByKey.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(emitter);

    emitter.onCompletion(() -> removeEmitter(key, emitter));
    emitter.onTimeout(() -> removeEmitter(key, emitter));
    emitter.onError(ex -> removeEmitter(key, emitter));

    // odmah pošalji "ping"
    try {
      emitter.send(SseEmitter.event().name("PING").data("ok"));
    } catch (Exception e) {
      removeEmitter(key, emitter);
    }
    return emitter;
  }

  private void removeEmitter(String key, SseEmitter emitter) {
    List<SseEmitter> list = emittersByKey.get(key);
    if (list != null) list.remove(emitter);
  }

  public void broadcast(String key, String eventName, Object payload) {
    List<SseEmitter> list = emittersByKey.get(key);
    if (list == null) return;

    List<SseEmitter> copy;
    synchronized (list) { copy = new ArrayList<>(list); }

    for (SseEmitter em : copy) {
      try {
        em.send(SseEmitter.event().name(eventName).data(payload));
      } catch (Exception e) {
        removeEmitter(key, em);
      }
    }
  }
}