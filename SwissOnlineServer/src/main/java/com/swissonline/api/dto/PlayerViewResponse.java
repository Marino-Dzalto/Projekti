package com.swissonline.api.dto;

import com.swissonline.core.model.MatchInfo;
import com.swissonline.core.model.PlayerInfo;
import com.swissonline.core.model.TimerState;

import java.util.*;

public class PlayerViewResponse {
  public String tournamentKey;
  public String tournamentName;
  public int currentRound;

  public boolean registered;
  public PlayerInfo me;

  // cijeli pairing
  public List<MatchInfo> pairings = new ArrayList<>();

  // moja utakmica (ako postoji)
  public MatchInfo myMatch;

  public TimerState timer;
}