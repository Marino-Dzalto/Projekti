package com.swissonline.api.dto;

import com.swissonline.core.model.MatchInfo;
import java.util.*;

public class PublishPairingsRequest {
  public int roundNumber;
  public List<MatchInfo> matches = new ArrayList<>();
}