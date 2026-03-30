package com.swissonline.core.model;

public class TimerState {
  public boolean running;
  public long startEpochMillis;   // kada je admin startao timer
  public int prepSeconds;         // npr 180
  public int roundSeconds;        // npr 3300
  public boolean paused;
  public long pauseEpochMillis;   // opcionalno (ako želiš pauzu kasnije)

  public TimerState() {}
}