package com.swissonline.core.model;

public class PlayerInfo {
  public String id;
  public String firstName;
  public String lastName;

  public PlayerInfo() {}
  public PlayerInfo(String id, String firstName, String lastName) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String fullName() {
    return (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
  }
}