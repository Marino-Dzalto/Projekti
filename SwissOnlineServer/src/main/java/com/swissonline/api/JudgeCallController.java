package com.swissonline.api;

import com.swissonline.core.TournamentStore;
import com.swissonline.core.model.MatchInfo;
import com.swissonline.core.model.TournamentState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tournaments/{key}")
@CrossOrigin(origins = "*")
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

    private TournamentState mustGet(String key){
        // IMPORTANT: prilagodi ovoj liniji ako ti se metoda zove drugačije
        TournamentState st = store.get(key);
        return st;
    }

    @GetMapping("/judgecalls")
    public ResponseEntity<JudgeCallsResponse> getJudgeCalls(@PathVariable("key") String key) {
        TournamentState st = mustGet(key);
        if(st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(toResp(key, st));
    }

    @PostMapping("/judgecall/start")
    public ResponseEntity<JudgeCallsResponse> startJudgeCall(@PathVariable("key") String key,
                                                             @RequestBody PlayerIdRequest req) {
        TournamentState st = mustGet(key);
        if(st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        MatchInfo match = findPlayersMatch(st, safe(req.playerId));
        if (match == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        // Ako je meč već odlučen, ne dozvoljavamo judge call
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
        TournamentState st = mustGet(key);
        if(st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Integer table = findPlayersTable(st, safe(req.playerId));
        if (table == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        st.judgeCallTables.remove(table);
        broadcast(st);
        return ResponseEntity.ok(toResp(key, st));
    }

    @PostMapping("/judgecall/clear")
    public ResponseEntity<JudgeCallsResponse> clearJudgeCall(@PathVariable("key") String key,
                                                             @RequestBody TableRequest req) {
        TournamentState st = mustGet(key);
        if(st == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

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
        if (playerId == null || playerId.isBlank()) return null;
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

    private String safe(String s) { return s == null ? "" : s.trim(); }
}