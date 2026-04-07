package com.swissonline.core.model;

import java.util.*;

public class TournamentState {
  public String key;
  public String name;

  public int currentRound;
  public Map<String, PlayerInfo> playersById = new HashMap<>();
  public List<MatchInfo> currentPairings = new ArrayList<>();
  public TimerState timer = new TimerState();

  // NOVO:
  public Set<Integer> judgeCallTables = new HashSet<>();

  public TournamentState() {}

  public TournamentState(String key, String name) {
    this.key = key;
    this.name = name;
    this.currentRound = 0;
    this.timer.running = false;
    this.timer.prepSeconds = 180;
    this.timer.roundSeconds = 55 * 60;
  }
}