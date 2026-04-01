package com.swissonline.api;

import com.swissonline.core.TournamentStore;
import com.swissonline.core.model.MatchInfo;
import com.swissonline.core.model.TournamentState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@RestController
@RequestMapping("/api/tournaments/{key}")
public class JudgeCallController {

    private final TournamentStore store;

    public JudgeCallController(TournamentStore store) {
        this.store = store;
    }

    public static class PlayerIdRequest {
        public String playerId;
    }

    public static class TableRequest {
        public int table;
    }

    public static class JudgeCallsResponse {
        public String tournamentKey;
        public List<Integer> tables = new ArrayList<>();
    }

    @GetMapping("/judgecalls")
    public ResponseEntity<JudgeCallsResponse> getJudgeCalls(@PathVariable("key") String key) {
        TournamentState st = store.get(key);
        if (st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(toResp(key, st));
    }

    @PostMapping("/judgecall/start")
    public ResponseEntity<JudgeCallsResponse> startJudgeCall(@PathVariable("key") String key,
                                                             @RequestBody PlayerIdRequest req) {
        if (req == null || req.playerId == null || req.playerId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        TournamentState st = store.get(key);
        if (st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        MatchInfo match = findPlayersMatch(st, req.playerId.trim());
        if (match == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        if (match.result != null && !match.result.equals("UNDECIDED")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        st.judgeCallTables.add(match.table);
        broadcast(st);
        return ResponseEntity.ok(toResp(key, st));
    }

    @PostMapping("/judgecall/end")
    public ResponseEntity<JudgeCallsResponse> endJudgeCall(@PathVariable("key") String key,
                                                           @RequestBody PlayerIdRequest req) {
        if (req == null || req.playerId == null || req.playerId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        TournamentState st = store.get(key);
        if (st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Integer table = findPlayersTable(st, req.playerId.trim());
        if (table == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        st.judgeCallTables.remove(table);
        broadcast(st);
        return ResponseEntity.ok(toResp(key, st));
    }

    @PostMapping("/judgecall/clear")
    public ResponseEntity<JudgeCallsResponse> clearJudgeCall(@PathVariable("key") String key,
                                                             @RequestBody TableRequest req) {
        TournamentState st = store.get(key);
        if (st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        st.judgeCallTables.remove(req.table);
        broadcast(st);
        return ResponseEntity.ok(toResp(key, st));
    }

    private void broadcast(TournamentState st) {
        Map<String, Object> payload = new LinkedHashMap<>();
        List<Integer> tables = new ArrayList<>(st.judgeCallTables);
        Collections.sort(tables);
        payload.put("tables", tables);

        // IMPORTANT: pretpostavljam da ovo postoji kod tebe (već ga koristiš za SSE)
        store.broadcast(st.key, "JUDGE_CALLS_UPDATED", payload);
    }

    private JudgeCallsResponse toResp(String key, TournamentState st) {
        JudgeCallsResponse resp = new JudgeCallsResponse();
        resp.tournamentKey = key;
        resp.tables = new ArrayList<>(st.judgeCallTables);
        Collections.sort(resp.tables);
        return resp;
    }

    private MatchInfo findPlayersMatch(TournamentState st, String playerId) {
        if (playerId == null || playerId.isBlank() || st.currentPairings == null) return null;
        for (MatchInfo m : st.currentPairings) {
            if (m == null) continue;
            if (m.p1 != null && playerId.equals(m.p1.id)) return m;
            if (m.p2 != null && playerId.equals(m.p2.id)) return m;
        }
        return null;
    }

    private Integer findPlayersTable(TournamentState st, String playerId) {
        MatchInfo m = findPlayersMatch(st, playerId);
        return m == null ? null : m.table;
    }
}