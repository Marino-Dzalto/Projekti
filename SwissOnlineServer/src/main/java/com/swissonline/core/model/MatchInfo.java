package com.swissonline.core.model;

public class MatchInfo {
  public int table;
  public PlayerInfo p1;
  public PlayerInfo p2; // null = BYE
  public String result; // "UNDECIDED" | "P1_WIN" | "P2_WIN" | "BOTH_LOSE"
  public int timeExtensionMin;

  public MatchInfo() {}
}