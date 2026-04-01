import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SwissTournamentApp {

    // ====== UI PALETA ======
    static final Color UI_BG        = new Color(0xF2F2F7);
    static final Color UI_SURFACE   = Color.WHITE;
    static final Color UI_BORDER    = new Color(0xD1D1D6);
    static final Color UI_TEXT      = new Color(0x1C1C1E);
    static final Color UI_TEXT2     = new Color(0x6E6E73);
    static final Color UI_ACCENT    = new Color(0x007AFF);
    static final Color UI_HDR_BG    = new Color(0x1D1D1F);
    static final Color UI_HDR_FG    = new Color(0xF5F5F7);
    static final Color UI_SEL_BG    = new Color(0x007AFF);
    static final Color UI_SEL_FG    = Color.WHITE;
    static final Color UI_ROW_ALT   = new Color(0xF7F8FA);
    static final Color UI_JUDGE     = new Color(0xFFF0D4);
    static final Color UI_TOOLBAR   = new Color(0xFCFCFD);

    // ====== KONSTANTE ======
    static final class TournamentConstants {
        static final String PLAYERS_DB_FILE   = "players_db.txt";
        static final String HISTORY_DIR_NAME  = "tournament_history";
        static final String PLAYER_DB_DELIMITER = "\t";

        static final double MIN_WIN_PCT        = 0.33;
        static final int    DEFAULT_PREP_SECONDS  = 3 * 60;
        static final int    DEFAULT_ROUND_SECONDS = 55 * 60;
        static final int    MAX_PLAYERS        = 100;

        private TournamentConstants() {}
    }

    // ====== PERSISTENCIJA BAZE IGRAČA ======
    private static final String PLAYERS_DB_FILE = TournamentConstants.PLAYERS_DB_FILE;
    private static final String HISTORY_DIR_NAME = TournamentConstants.HISTORY_DIR_NAME;

    private static List<Player> loadPlayerDatabase() {
        List<Player> list = new ArrayList<>();
        File f = new File(PLAYERS_DB_FILE);
        if (!f.exists()) {
            return list; // prazna baza prvi put
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\t");
                if (parts.length < 3) continue;
                String id = parts[0];
                String firstName = parts[1];
                String lastName = parts[2];
                Player p = new Player(firstName, lastName, id, false);
                list.add(p);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to load player database from '" + PLAYERS_DB_FILE + "': " + e.getMessage());
        }
        return list;
    }

    public static void savePlayerDatabase(List<Player> db) {
        File f = new File(PLAYERS_DB_FILE);
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            for (Player p : db) {
                if (p.isGuest()) continue;
                pw.println(p.getId() + "\t" + p.getFirstName() + "\t" + p.getLastName());
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to save player database to '" + PLAYERS_DB_FILE + "': " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                "Greška pri spremanju baze igrača:\n" + e.getMessage(),
                "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Moderni styling za tablice
    private static void styleTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xE5E7EB));
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(UI_HDR_BG);
        table.getTableHeader().setForeground(UI_HDR_FG);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.getTableHeader().setOpaque(true);
        table.setSelectionBackground(UI_SEL_BG);
        table.setSelectionForeground(UI_SEL_FG);
        table.setBorder(BorderFactory.createEmptyBorder());
        table.setBackground(UI_SURFACE);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UI_SURFACE : UI_ROW_ALT);
                    c.setForeground(UI_TEXT);
                } else {
                    c.setBackground(UI_SEL_BG);
                    c.setForeground(UI_SEL_FG);
                }
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);

        // Fix header renderer colours (Nimbus override)
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = new JLabel(value == null ? "" : value.toString());
                lbl.setOpaque(true);
                lbl.setBackground(UI_HDR_BG);
                lbl.setForeground(UI_HDR_FG);
                lbl.setFont(tbl.getTableHeader().getFont());
                lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x3A3A3C)),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
                return lbl;
            }
        });
    }

    
    private static void installMacTextShortcuts() {
        // Na macOS-u očekuje se CMD+A/C/V/X. Swing to ponekad ne mapira ovisno o LAF-u,
        // pa ručno dodajemo key bindings u InputMap za TextField/TextArea.
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        Object tfObj = UIManager.get("TextField.focusInputMap");
        if (tfObj instanceof InputMap) {
            InputMap im = (InputMap) tfObj;
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask), DefaultEditorKit.selectAllAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask), DefaultEditorKit.cutAction);
        }
        Object taObj = UIManager.get("TextArea.focusInputMap");
        if (taObj instanceof InputMap) {
            InputMap im = (InputMap) taObj;
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask), DefaultEditorKit.selectAllAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask), DefaultEditorKit.cutAction);
        }
        Object tpObj = UIManager.get("PasswordField.focusInputMap");
        if (tpObj instanceof InputMap) {
            InputMap im = (InputMap) tpObj;
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask), DefaultEditorKit.selectAllAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask), DefaultEditorKit.cutAction);
        }
    }

public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            Font base = new Font("SF Pro Text", Font.PLAIN, 13);
            Font bold = new Font("SF Pro Text", Font.BOLD, 13);
            UIManager.put("Label.font", base);
            UIManager.put("Button.font", base);
            UIManager.put("Table.font", base);
            UIManager.put("TextField.font", base);
            UIManager.put("ComboBox.font", base);
            UIManager.put("TabbedPane.font", base);
            UIManager.put("TableHeader.font", bold);
            UIManager.put("Panel.background", UI_BG);
            UIManager.put("TabbedPane.background", UI_BG);
            UIManager.put("TabbedPane.contentAreaColor", UI_SURFACE);
        } catch (Exception ignored) {}

        installMacTextShortcuts();

        SwingUtilities.invokeLater(() -> {
            List<Player> sharedDatabase = loadPlayerDatabase();
            HomeFrame home = new HomeFrame(sharedDatabase);
            home.setVisible(true);
        });
    }

    // ==== MODEL KLASA: Igrač ====

    static class Player {
        private String id;
        private String firstName;
        private String lastName;
        private boolean guest;
        private int basePoints;
        private boolean dropped;

        public Player(String firstName, String lastName, String id, boolean guest) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.id = id;
            this.guest = guest;
            this.basePoints = 0;
            this.dropped = false;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getFullName() { return firstName + " " + lastName; }
        public boolean isGuest() { return guest; }

        public int getBasePoints() { return basePoints; }
        public void setBasePoints(int basePoints) { this.basePoints = basePoints; }

        public boolean isDropped() { return dropped; }
        public void setDropped(boolean dropped) { this.dropped = dropped; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Player)) return false;
            Player player = (Player) o;
            return Objects.equals(id, player.id)
                    && Objects.equals(firstName, player.firstName)
                    && Objects.equals(lastName, player.lastName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, firstName, lastName);
        }
    }

    // ==== MODEL: Meč / rezultat ====

    enum MatchResult {
        UNDECIDED,
        P1_WIN,
        P2_WIN,
        BOTH_LOSE
    }

    static class Match {
        private int tableNumber;
        private Player p1;
        private Player p2; // null ako je BYE
        private MatchResult result;
        private int timeExtensionMinutes; // time extension

        public Match(int tableNumber, Player p1, Player p2) {
            this.tableNumber = tableNumber;
            this.p1 = p1;
            this.p2 = p2;
            this.result = MatchResult.UNDECIDED;
            this.timeExtensionMinutes = 0;
        }

        public int getTableNumber() { return tableNumber; }
        public Player getP1() { return p1; }
        public Player getP2() { return p2; }
        public MatchResult getResult() { return result; }
        public void setResult(MatchResult result) { this.result = result; }
        public boolean isBye() { return p2 == null; }

        public int getTimeExtensionMinutes() { return timeExtensionMinutes; }
        public void setTimeExtensionMinutes(int minutes) { this.timeExtensionMinutes = minutes; }
    }

    // ==== MODEL: Turnir (Swiss dio) ====

    static class Tournament {

        static class StandingSnapshot {
            String id;
            String firstName;
            String lastName;
            int points;

            // Tie-breaker komponente (KTS stil): OMW% i OOW% u rasponu 0..1
            double oppMatchWinPct;        // YYY (opponents' match-win %)
            double oppOppMatchWinPct;     // ZZZ (opponents' opponents' match-win %)

            // Prikaz u formatu XXYYYZZZ (npr 33 726 677 -> 33726677)
            String tieBreakerCode;
        }

        private final String name;

        private List<Player> players = new ArrayList<>();
        private List<List<Match>> allRounds = new ArrayList<>();
        private List<Match> currentRoundMatches = new ArrayList<>();

        private int finishedRounds = 0;
        private boolean roundInProgress = false;

        public static final int MAX_PLAYERS = TournamentConstants.MAX_PLAYERS;

        public Tournament(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        public List<Player> getPlayers() { return players; }
        public List<List<Match>> getAllRounds() { return allRounds; }
        public List<Match> getCurrentRoundMatches() { return currentRoundMatches; }

        public int getFinishedRounds() { return finishedRounds; }
        public boolean isRoundInProgress() { return roundInProgress; }

        public boolean hasRounds() { return !allRounds.isEmpty(); }

        public boolean addPlayer(Player p) {
            if (players.size() >= MAX_PLAYERS) return false;
            if (!p.isGuest()) {
                for (Player existing : players) {
                    if (!existing.isGuest()
                            && existing.getId().equals(p.getId())
                            && existing.getFirstName().equalsIgnoreCase(p.getFirstName())
                            && existing.getLastName().equalsIgnoreCase(p.getLastName())) {
                        return false;
                    }
                }
            }
            players.add(p);
            return true;
        }

        private boolean hasHadBye(Player p) {
            for (List<Match> round : allRounds) {
                for (Match m : round) {
                    if (m.isBye() && m.getP1().equals(p) && m.getResult() == MatchResult.P1_WIN) return true;
                }
            }
            return false;
        }

        private boolean havePlayedEachOther(Player a, Player b) {
            for (List<Match> round : allRounds) {
                for (Match m : round) {
                    if (!m.isBye()) {
                        if ((m.getP1().equals(a) && m.getP2().equals(b)) ||
                                (m.getP1().equals(b) && m.getP2().equals(a))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public int getCurrentTotalPoints(Player p) {
            return getTotalPointsUpToRound(p, allRounds.size());
        }

        public int getTotalPointsUpToRound(Player p, int roundNumber) {
            int pts = p.getBasePoints();
            int maxRound = Math.min(roundNumber, allRounds.size());
            for (int r = 0; r < maxRound; r++) {
                for (Match m : allRounds.get(r)) {
                    if (m.isBye()) {
                        if (m.getP1().equals(p) && m.getResult() == MatchResult.P1_WIN) {
                            pts += 3;
                        }
                    } else {
                        if (m.getP1().equals(p) && m.getResult() == MatchResult.P1_WIN) pts += 3;
                        else if (m.getP2().equals(p) && m.getResult() == MatchResult.P2_WIN) pts += 3;
                    }
                }
            }
            return pts;
        }

        public List<StandingSnapshot> computeStandingsForRound(int roundNumber) {
            // Tie-breakeri se računaju na temelju mečeva do tražene runde (roundNumber).
            // Implementacija slijedi uobičajenu praksu: Points -> OMW% -> OOW%
            // (uz minimalni floor od 33% kako bi se izbjegle ekstremne vrijednosti u malom broju rundi).
            Map<Player, Record> rec = computeRecordsUpToRound(roundNumber);
            Map<Player, Double> mwPct = new HashMap<>();
            for (Player p : players) {
                Record r = rec.getOrDefault(p, new Record());
                mwPct.put(p, clampWinPct(r.matchWinPct()));
            }

            // OMW%: prosjek MW% svih protivnika (s floorom), ignorira BYE (nema protivnika)
            Map<Player, Double> omw = new HashMap<>();
            for (Player p : players) {
                List<Player> opps = rec.getOrDefault(p, new Record()).opponents;
                if (opps.isEmpty()) {
                    omw.put(p, TournamentConstants.MIN_WIN_PCT);
                } else {
                    double avg = 0.0;
                    for (Player o : opps) avg += mwPct.getOrDefault(o, TournamentConstants.MIN_WIN_PCT);
                    avg /= opps.size();
                    omw.put(p, clampWinPct(avg));
                }
            }

            // OOW%: prosjek OMW% svih protivnika
            Map<Player, Double> oow = new HashMap<>();
            for (Player p : players) {
                List<Player> opps = rec.getOrDefault(p, new Record()).opponents;
                if (opps.isEmpty()) {
                    oow.put(p, TournamentConstants.MIN_WIN_PCT);
                } else {
                    double avg = 0.0;
                    for (Player o : opps) avg += omw.getOrDefault(o, TournamentConstants.MIN_WIN_PCT);
                    avg /= opps.size();
                    oow.put(p, clampWinPct(avg));
                }
            }

            List<StandingSnapshot> snap = new ArrayList<>();
            for (Player p : players) {
                StandingSnapshot s = new StandingSnapshot();
                s.id = p.getId();
                s.firstName = p.getFirstName();
                s.lastName = p.getLastName();
                s.points = getTotalPointsUpToRound(p, roundNumber);
                s.oppMatchWinPct = omw.getOrDefault(p, TournamentConstants.MIN_WIN_PCT);
                s.oppOppMatchWinPct = oow.getOrDefault(p, TournamentConstants.MIN_WIN_PCT);
                s.tieBreakerCode = formatTieBreakerCode(s.points, s.oppMatchWinPct, s.oppOppMatchWinPct);
                snap.add(s);
            }

            snap.sort((a, b) -> {
                int cmp = Integer.compare(b.points, a.points);
                if (cmp != 0) return cmp;

                cmp = Double.compare(b.oppMatchWinPct, a.oppMatchWinPct);
                if (cmp != 0) return cmp;

                cmp = Double.compare(b.oppOppMatchWinPct, a.oppOppMatchWinPct);
                if (cmp != 0) return cmp;

                cmp = a.lastName.compareToIgnoreCase(b.lastName);
                if (cmp != 0) return cmp;
                return a.firstName.compareToIgnoreCase(b.firstName);
            });

            return snap;
        }

        private static class Record {
            int wins = 0;
            int draws = 0;
            int losses = 0;
            List<Player> opponents = new ArrayList<>();

            double matchWinPct() {
                int played = wins + draws + losses;
                if (played <= 0) return TournamentConstants.MIN_WIN_PCT;
                // Win=1, Draw=0.5
                return (wins + 0.5 * draws) / played;
            }
        }

        private Map<Player, Record> computeRecordsUpToRound(int roundNumber) {
            Map<Player, Record> map = new HashMap<>();
            int maxRound = Math.min(roundNumber, allRounds.size());

            for (int r = 0; r < maxRound; r++) {
                for (Match m : allRounds.get(r)) {
                    if (m == null) continue;
                    Player p1 = m.getP1();
                    Player p2 = m.getP2();

                    map.computeIfAbsent(p1, k -> new Record());
                    if (p2 != null) map.computeIfAbsent(p2, k -> new Record());

                    // BYE se računa u record za p1 (win/loss ovisno o postavci), ali nema protivnika
                    if (m.isBye()) {
                        Record r1 = map.get(p1);
                        if (m.getResult() == MatchResult.P1_WIN) r1.wins++;
                        else if (m.getResult() == MatchResult.BOTH_LOSE) r1.losses++;
                        else {
                            // UNDECIDED: ne računamo kao odigrano
                        }
                        continue;
                    }

                    // Regular match
                    Record r1 = map.get(p1);
                    Record r2 = map.get(p2);
                    r1.opponents.add(p2);
                    r2.opponents.add(p1);

                    MatchResult res = m.getResult();
                    if (res == MatchResult.P1_WIN) {
                        r1.wins++; r2.losses++;
                    } else if (res == MatchResult.P2_WIN) {
                        r2.wins++; r1.losses++;
                    } else if (res == MatchResult.BOTH_LOSE) {
                        r1.losses++; r2.losses++;
                    } else {
                        // UNDECIDED: ne računamo kao odigrano
                    }
                }
            }
            return map;
        }

        private static double clampWinPct(double v) {
            if (Double.isNaN(v) || Double.isInfinite(v)) return TournamentConstants.MIN_WIN_PCT;
            if (v < TournamentConstants.MIN_WIN_PCT) return TournamentConstants.MIN_WIN_PCT;
            if (v > 1.0) return 1.0;
            return v;
        }

        private static String formatTieBreakerCode(int points, double omw, double oow) {
            // XXYYYZZZ (bez točaka) - YYY i ZZZ su postotci * 1000 (npr. 0.726 -> 726)
            int y = (int) Math.round(omw * 1000.0);
            int z = (int) Math.round(oow * 1000.0);
            // Points može prijeći 99 na većim eventima; ne režemo, ali preferiramo barem 2 znamenke.
            String xx = String.format("%02d", points);
            String yyy = String.format("%03d", Math.max(0, Math.min(999, y)));
            String zzz = String.format("%03d", Math.max(0, Math.min(999, z)));
            return xx + yyy + zzz;
        }

        private List<Player> getActivePlayers() {
            List<Player> active = new ArrayList<>();
            for (Player p : players) {
                if (!p.isDropped()) active.add(p);
            }
            return active;
        }

        // GENERIRANJE SWISS U PAR-POINT GRUPAMA (random unutar grupe)
        private List<Match> generateSwissPairings(List<Player> active) {
            if (active.size() < 2) {
                throw new IllegalStateException("Nedovoljno aktivnih igrača (drop?).");
            }

            Map<Integer, List<Player>> byPoints = new HashMap<>();
            for (Player p : active) {
                int pts = getCurrentTotalPoints(p);
                byPoints.computeIfAbsent(pts, k -> new ArrayList<>()).add(p);
            }

            List<Integer> pointBuckets = new ArrayList<>(byPoints.keySet());
            pointBuckets.sort(Collections.reverseOrder()); // više bodova gore

            List<Player> ordered = new ArrayList<>();
            Random rnd = new Random(System.nanoTime());
            for (int pts : pointBuckets) {
                List<Player> group = byPoints.get(pts);
                Collections.shuffle(group, rnd); // random unutar points grupe
                ordered.addAll(group);
            }

            List<Player> toPair = new ArrayList<>(ordered);
            Player byePlayer = null;

            if (toPair.size() % 2 == 1) {
                ListIterator<Player> it = toPair.listIterator(toPair.size());
                while (it.hasPrevious()) {
                    Player candidate = it.previous();
                    if (!hasHadBye(candidate)) {
                        byePlayer = candidate;
                        it.remove();
                        break;
                    }
                }
                if (byePlayer == null && !toPair.isEmpty()) {
                    byePlayer = toPair.remove(toPair.size() - 1);
                }
            }

            List<Match> newMatches = new ArrayList<>();
            int table = 1;
            while (!toPair.isEmpty()) {
                Player p1 = toPair.remove(0);
                Player opponent = null;
                int oppIndex = -1;
                for (int i = 0; i < toPair.size(); i++) {
                    Player candidate = toPair.get(i);
                    if (!havePlayedEachOther(p1, candidate)) {
                        opponent = candidate;
                        oppIndex = i;
                        break;
                    }
                }
                if (opponent == null) {
                    opponent = toPair.get(0);
                    oppIndex = 0;
                }
                toPair.remove(oppIndex);
                newMatches.add(new Match(table++, p1, opponent));
            }

            if (byePlayer != null) {
                Match byeMatch = new Match(table, byePlayer, null);
                newMatches.add(byeMatch);
            }

            return newMatches;
        }

        public List<Match> startNextRound() {
            if (roundInProgress) throw new IllegalStateException("Runda je već u tijeku.");
            List<Player> active = getActivePlayers();
            List<Match> matches = generateSwissPairings(active);
            currentRoundMatches = matches;
            allRounds.add(matches);
            roundInProgress = true;
            return matches;
        }

        public List<Match> regenerateCurrentRoundPairings() {
            if (!roundInProgress) throw new IllegalStateException("Nema aktivne runde za RePair.");
            if (allRounds.isEmpty()) throw new IllegalStateException("Nema rundi za RePair.");

            allRounds.remove(allRounds.size() - 1);
            List<Player> active = getActivePlayers();
            List<Match> matches = generateSwissPairings(active);
            currentRoundMatches = matches;
            allRounds.add(matches);
            return matches;
        }

        public void applyResultsAndFinishRound() {
            if (!roundInProgress) throw new IllegalStateException("Nema aktivne runde.");
            finishedRounds++;
            roundInProgress = false;
        }

        public boolean allResultsEntered() {
            for (Match m : currentRoundMatches) {
                if (m.getResult() == MatchResult.UNDECIDED) return false;
            }
            return true;
        }

        public void undoLastRound() {
            if (allRounds.isEmpty()) throw new IllegalStateException("Nema rundi za poništiti.");
            allRounds.remove(allRounds.size() - 1);
            currentRoundMatches = new ArrayList<>();
            if (roundInProgress) {
                roundInProgress = false;
            } else {
                if (finishedRounds > 0) finishedRounds--;
            }
        }
    }

    // ==== HISTORY STRUKTURE & MANAGER ====

    static class HistoryEntry {
        String baseName;
        String key;
        String date;
        String displayName;
        List<Tournament.StandingSnapshot> standings = new ArrayList<>();
        String topCutText;
        File sourceFile;

        public boolean hasTopCut() {
            return topCutText != null && !topCutText.trim().isEmpty();
        }
    }

    static class HistoryManager {

        private static void ensureDir() {
            File dir = new File(HISTORY_DIR_NAME);
            if (!dir.exists()) dir.mkdirs();
        }

        public static void saveSwissOnlyHistory(String baseName, String key,
                                                List<Tournament.StandingSnapshot> standings) {
            saveHistoryInternal(baseName, key, standings, null);
        }

        public static void saveSwissTopCutHistory(String baseName, String key,
                                                  List<Tournament.StandingSnapshot> standings,
                                                  String topCutText) {
            saveHistoryInternal(baseName, key, standings, topCutText);
        }

        private static void saveHistoryInternal(String baseName, String key,
                                                List<Tournament.StandingSnapshot> standings,
                                                String topCutText) {
            ensureDir();
            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
            String dateStr = df.format(new Date());
            String displayName = baseName + "-" + dateStr + "-" + key;

            String fileName = "tournament_" + System.currentTimeMillis() + ".txt";
            File file = new File(HISTORY_DIR_NAME, fileName);
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("NAME:" + baseName);
                pw.println("KEY:" + key);
                pw.println("DATE:" + dateStr);
                pw.println("FULL_NAME:" + displayName);
                pw.println("HAS_TOPCUT:" + (topCutText != null && !topCutText.trim().isEmpty()));
                pw.println("STANDINGS_BEGIN");
                for (int i = 0; i < standings.size(); i++) {
                    Tournament.StandingSnapshot s = standings.get(i);
                    int pos = i + 1;
                    pw.println(pos + "\t" + s.id + "\t" + s.firstName + "\t" + s.lastName + "\t" + s.points + "\t" + (s.tieBreakerCode == null ? "" : s.tieBreakerCode));
                }
                pw.println("STANDINGS_END");
                if (topCutText != null && !topCutText.trim().isEmpty()) {
                    pw.println("TOPCUT_BEGIN");
                    for (String line : topCutText.split("\n")) {
                        pw.println(line);
                    }
                    pw.println("TOPCUT_END");
                }
            } catch (IOException e) {
                System.err.println("[ERROR] Failed to save tournament history to '" + file.getName() + "': " + e.getMessage());
                JOptionPane.showMessageDialog(null,
                    "Greška pri spremanju historije turnira:\n" + e.getMessage(),
                    "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }

        public static List<HistoryEntry> loadHistory() {
            List<HistoryEntry> list = new ArrayList<>();
            File dir = new File(HISTORY_DIR_NAME);
            if (!dir.exists() || !dir.isDirectory()) return list;

            File[] files = dir.listFiles();
            if (files == null) return list;

            for (File f : files) {
                if (!f.isFile()) continue;
                if (!f.getName().startsWith("tournament_")) continue;
                HistoryEntry entry = parseHistoryFile(f);
                if (entry != null) {
                    entry.sourceFile = f;
                    list.add(entry);
                }
            }

            return list;
        }

        private static HistoryEntry parseHistoryFile(File f) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                HistoryEntry e = new HistoryEntry();
                String line;
                boolean inStandings = false;
                boolean inTopCut = false;
                StringBuilder topCutSb = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    if (line.startsWith("NAME:")) {
                        e.baseName = line.substring("NAME:".length()).trim();
                    } else if (line.startsWith("KEY:")) {
                        e.key = line.substring("KEY:".length()).trim();
                    } else if (line.startsWith("DATE:")) {
                        e.date = line.substring("DATE:".length()).trim();
                    } else if (line.startsWith("FULL_NAME:")) {
                        e.displayName = line.substring("FULL_NAME:".length()).trim();
                    } else if (line.equals("STANDINGS_BEGIN")) {
                        inStandings = true;
                    } else if (line.equals("STANDINGS_END")) {
                        inStandings = false;
                    } else if (line.equals("TOPCUT_BEGIN")) {
                        inTopCut = true;
                    } else if (line.equals("TOPCUT_END")) {
                        inTopCut = false;
                    } else if (inStandings) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 5) {
                            Tournament.StandingSnapshot s = new Tournament.StandingSnapshot();
                            s.id = parts[1];
                            s.firstName = parts[2];
                            s.lastName = parts[3];
                            try { s.points = Integer.parseInt(parts[4]); }
                            catch (NumberFormatException ex) { s.points = 0; }
                            if (parts.length >= 6) {
                                s.tieBreakerCode = parts[5];
                            }
                            e.standings.add(s);
                        }
                    } else if (inTopCut) {
                        topCutSb.append(line).append("\n");
                    }
                }

                String tc = topCutSb.toString().trim();
                e.topCutText = tc.isEmpty() ? null : tc;
                return e;
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }

        public static boolean deleteHistoryEntry(HistoryEntry entry) {
            if (entry == null || entry.sourceFile == null) return false;
            File f = entry.sourceFile;
            if (f.exists()) {
                return f.delete();
            }
            return false;
        }
    }

    // ==== TABLE: Igrači na turniru ====

    static class PlayersTableModel extends AbstractTableModel {
        private Tournament tournament;
        private final String[] columns = {"ID", "Ime", "Prezime", "Bodovi", "DROP"};

        public PlayersTableModel(Tournament tournament) { this.tournament = tournament; }
        public void setTournament(Tournament t) { this.tournament = t; fireTableDataChanged(); }

        @Override public int getRowCount() { return tournament.getPlayers().size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Player p = tournament.getPlayers().get(rowIndex);
            switch (columnIndex) {
                case 0: return p.isGuest() ? "000000000" : p.getId();
                case 1: return p.getFirstName();
                case 2: return p.getLastName();
                case 3: return tournament.getCurrentTotalPoints(p);
                case 4: return p.isDropped() ? "DA" : "NE";
                default: return "";
            }
        }

        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    // ==== TABLE: Baza igrača ====

    static class DatabasePlayersTableModel extends AbstractTableModel {
        private final List<Player> allPlayers;
        private final List<Player> filteredPlayers = new ArrayList<>();
        private final String[] columns = {"ID", "Ime", "Prezime"};

        public DatabasePlayersTableModel(List<Player> allPlayers) {
            this.allPlayers = allPlayers;
            refilter("", "", "");
        }

        public void refilter(String fnFilter, String lnFilter, String idFilter) {
            String fn = fnFilter == null ? "" : fnFilter.trim().toLowerCase();
            String ln = lnFilter == null ? "" : lnFilter.trim().toLowerCase();
            String id = idFilter == null ? "" : idFilter.trim().toLowerCase();

            filteredPlayers.clear();
            for (Player p : allPlayers) {
                boolean ok = true;
                if (!fn.isEmpty() && !p.getFirstName().toLowerCase().contains(fn)) ok = false;
                if (!ln.isEmpty() && !p.getLastName().toLowerCase().contains(ln)) ok = false;
                if (!id.isEmpty() && !p.getId().toLowerCase().contains(id)) ok = false;
                if (ok) filteredPlayers.add(p);
            }
            fireTableDataChanged();
        }

        public Player getPlayerAt(int row) {
            if (row < 0 || row >= filteredPlayers.size()) return null;
            return filteredPlayers.get(row);
        }

        @Override public int getRowCount() { return filteredPlayers.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Player p = filteredPlayers.get(rowIndex);
            switch (columnIndex) {
                case 0: return p.getId();
                case 1: return p.getFirstName();
                case 2: return p.getLastName();
                default: return "";
            }
        }

        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    // ==== TABLE: Parovi za Swiss rundu ====

    static class RoundPairingsTableModel extends AbstractTableModel {

        interface ResultsChangeListener { void onResultsChanged(); }

        private final String[] columns = {"Stol", "Igrač 1", "Bodovi 1", "Igrač 2", "Bodovi 2", "Rezultat", "Time ext."};
        private final List<Match> matches;
        private final Tournament tournament;
        private boolean editable;
        private final ResultsChangeListener listener;

        public RoundPairingsTableModel(Tournament tournament,
                                       List<Match> matches,
                                       boolean editable,
                                       ResultsChangeListener listener) {
            this.tournament = tournament;
            this.matches = matches;
            this.editable = editable;
            this.listener = listener;
        }

        public void setEditable(boolean editable) { this.editable = editable; fireTableDataChanged(); }

        public Match getMatchAt(int row) {
            if (row < 0 || row >= matches.size()) return null;
            return matches.get(row);
        }

        @Override public int getRowCount() { return matches.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }

        @Override
        public Class<?> getColumnClass(int c) {
            if (c == 0 || c == 2 || c == 4) return Integer.class;
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Match m = matches.get(rowIndex);
            Player p1 = m.getP1();
            Player p2 = m.getP2();
            switch (columnIndex) {
                case 0: return m.getTableNumber();
                case 1: return p1.getFullName();
                case 2: return tournament.getCurrentTotalPoints(p1);
                case 3: return (p2 == null) ? "BYE" : p2.getFullName();
                case 4: return (p2 == null) ? "" : tournament.getCurrentTotalPoints(p2);
                case 5:
                    if (m.isBye()) {
                        if (m.getResult() == MatchResult.P1_WIN) {
                            return "BYE WIN (3 boda)";
                        } else if (m.getResult() == MatchResult.BOTH_LOSE) {
                            return "BYE LOSS (0 bodova)";
                        } else {
                            return "BYE (nije postavljeno)";
                        }
                    } else {
                        switch (m.getResult()) {
                            case P1_WIN: return p1.getFullName() + " WIN";
                            case P2_WIN: return p2.getFullName() + " WIN";
                            case BOTH_LOSE: return "DOUBLE LOSS";
                            case UNDECIDED:
                            default: return "";
                        }
                    }
                case 6:
                    if (m.getTimeExtensionMinutes() > 0) {
                        return "TIME EXTENSION: " + m.getTimeExtensionMinutes() + " min";
                    } else {
                        return "";
                    }
                default: return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return editable && columnIndex == 5;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 5) return;
            Match m = matches.get(rowIndex);
            Player p1 = m.getP1();
            Player p2 = m.getP2();
            String value = (aValue == null) ? "" : aValue.toString();

            if (m.isBye()) {
                if (value.startsWith("BYE WIN")) {
                    m.setResult(MatchResult.P1_WIN);
                } else if (value.startsWith("BYE LOSS")) {
                    m.setResult(MatchResult.BOTH_LOSE);
                } else {
                    m.setResult(MatchResult.UNDECIDED);
                }
            } else {
                if (value.endsWith("WIN")) {
                    if (value.startsWith(p1.getFullName())) m.setResult(MatchResult.P1_WIN);
                    else if (p2 != null && value.startsWith(p2.getFullName())) m.setResult(MatchResult.P2_WIN);
                    else m.setResult(MatchResult.UNDECIDED);
                } else if (value.equals("DOUBLE LOSS")) {
                    m.setResult(MatchResult.BOTH_LOSE);
                } else {
                    m.setResult(MatchResult.UNDECIDED);
                }
            }
            fireTableCellUpdated(rowIndex, columnIndex);
            if (listener != null) listener.onResultsChanged();
        }
    }

    // ==== TABLE: Standings ====

    static class StandingsTableModel extends AbstractTableModel {
        private final String[] columns = {"Pozicija", "ID", "Ime", "Prezime", "Bodovi", "Tie-breaker"};
        private List<Tournament.StandingSnapshot> data = new ArrayList<>();

        public void setData(List<Tournament.StandingSnapshot> data) {
            this.data = new ArrayList<>(data);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Tournament.StandingSnapshot s = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return rowIndex + 1;
                case 1: return s.id;
                case 2: return s.firstName;
                case 3: return s.lastName;
                case 4: return s.points;
                case 5: return s.tieBreakerCode;
                default: return "";
            }
        }

        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    // ==== HOME SCREEN + HISTORY GUMB ====

    static class HomeFrame extends JFrame {
        private final List<Player> playerDatabase;

        public HomeFrame(List<Player> playerDatabase) {
            this.playerDatabase = playerDatabase;
            setTitle("Swiss Turnir");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(460, 340);
            setLocationRelativeTo(null);
            setResizable(false);
            getContentPane().setBackground(UI_BG);
            initUi();
        }

        private void initUi() {
            JPanel root = new JPanel(new GridBagLayout());
            root.setBackground(UI_BG);
            add(root);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(UI_SURFACE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 1, UI_BORDER),
                    BorderFactory.createEmptyBorder(0, 0, 24, 0)
            ));

            // Accent header strip
            JPanel accentStrip = new JPanel();
            accentStrip.setBackground(UI_HDR_BG);
            accentStrip.setPreferredSize(new Dimension(0, 6));
            accentStrip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            accentStrip.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel headerPanel = new JPanel();
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            headerPanel.setOpaque(false);
            headerPanel.setBorder(BorderFactory.createEmptyBorder(22, 28, 0, 28));
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel title = new JLabel("Swiss Turnir");
            title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
            title.setForeground(UI_TEXT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel subtitle = new JLabel("Swiss + Top Cut tournament manager");
            subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 13f));
            subtitle.setForeground(UI_TEXT2);
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            headerPanel.add(title);
            headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            headerPanel.add(subtitle);

            // Divider
            JPanel divider = new JPanel();
            divider.setBackground(UI_BORDER);
            divider.setPreferredSize(new Dimension(0, 1));
            divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            divider.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton newTournamentButton = primaryButton("  Novi turnir  ");
            JButton historyButton = ghostButton("History");
            JButton exitButton = ghostButton("Isključi aplikaciju");

            Dimension btnSize = new Dimension(Integer.MAX_VALUE, 40);
            newTournamentButton.setMaximumSize(btnSize);
            historyButton.setMaximumSize(btnSize);
            exitButton.setMaximumSize(btnSize);
            newTournamentButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            historyButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            exitButton.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
            buttonsPanel.setBorder(BorderFactory.createEmptyBorder(18, 28, 0, 28));
            buttonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            buttonsPanel.add(newTournamentButton);
            buttonsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            buttonsPanel.add(historyButton);
            buttonsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            buttonsPanel.add(exitButton);

            card.add(accentStrip);
            card.add(headerPanel);
            card.add(Box.createRigidArea(new Dimension(0, 18)));
            card.add(divider);
            card.add(buttonsPanel);

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.weightx = 1.0;
            gc.insets = new Insets(0, 24, 0, 24);
            root.add(card, gc);

            newTournamentButton.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(this, "Unesi naziv turnira (npr. YGO):");
                if (name == null || name.trim().isEmpty()) return;
                TournamentFrame tf = new TournamentFrame(playerDatabase, name.trim());
                tf.setVisible(true);
            });

            historyButton.addActionListener(e -> {
                HistoryDialog hd = new HistoryDialog(this);
                hd.setVisible(true);
            });

            exitButton.addActionListener(e -> System.exit(0));
        }

        private JButton primaryButton(String text) {
            JButton b = new JButton(text);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setForeground(Color.WHITE);
            b.setBackground(UI_ACCENT);
            b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
            b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            return b;
        }

        private JButton ghostButton(String text) {
            JButton b = new JButton(text);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setForeground(UI_TEXT);
            b.setBackground(UI_SURFACE);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UI_BORDER),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
            ));
            return b;
        }
    }

    // ==== PANEL za jednu Swiss rundu ====

    static class RoundPanel extends JPanel {
        private final JTable table;
        private volatile Set<Integer> judgeCallTables = Collections.emptySet();
        private final RoundPairingsTableModel model;

        static class ResultCellEditor extends AbstractCellEditor implements TableCellEditor {
            private JComboBox<String> combo;
            private final List<Match> matches;

            public ResultCellEditor(List<Match> matches) {
                this.matches = matches;
            }

            @Override public Object getCellEditorValue() { return combo.getSelectedItem(); }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                                                         boolean isSelected, int row, int column) {
                Match m = matches.get(row);
                Player p1 = m.getP1();
                Player p2 = m.getP2();

                combo = new JComboBox<>();
                combo.addItem("");

                if (m.isBye()) {
                    combo.addItem("BYE WIN (3 boda)");
                    combo.addItem("BYE LOSS (0 bodova)");
                } else {
                    combo.addItem(p1.getFullName() + " WIN");
                    combo.addItem(p2.getFullName() + " WIN");
                    combo.addItem("DOUBLE LOSS");
                }

                String selected;
                if (m.isBye()) {
                    if (m.getResult() == MatchResult.P1_WIN) selected = "BYE WIN (3 boda)";
                    else if (m.getResult() == MatchResult.BOTH_LOSE) selected = "BYE LOSS (0 bodova)";
                    else selected = "";
                } else {
                    switch (m.getResult()) {
                        case P1_WIN: selected = p1.getFullName() + " WIN"; break;
                        case P2_WIN: selected = p2.getFullName() + " WIN"; break;
                        case BOTH_LOSE: selected = "DOUBLE LOSS"; break;
                        case UNDECIDED:
                        default: selected = "";
                    }
                }
                combo.setSelectedItem(selected);
                return combo;
            }
        }

        public RoundPanel(Tournament tournament,
                          List<Match> matches,
                          boolean editable,
                          RoundPairingsTableModel.ResultsChangeListener listener) {
            super(new BorderLayout());
            setOpaque(false);
            model = new RoundPairingsTableModel(tournament, matches, editable, listener);
            table = new JTable(model);
            table.getColumnModel().getColumn(5).setCellEditor(new ResultCellEditor(matches));
            styleTable(table);
            installJudgeRenderer();

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createEmptyBorder());

            add(scroll, BorderLayout.CENTER);
        }

        public void setEditable(boolean editable) { model.setEditable(editable); }
        public void refresh() { model.fireTableDataChanged(); }

        public JTable getTable() { return table; }
        public Match getMatchAt(int row) { return model.getMatchAt(row); }

        public Integer getSelectedTableNumber() {
            int row = table.getSelectedRow();
            if (row < 0) return null;
            Match m = model.getMatchAt(row);
            return (m == null) ? null : m.getTableNumber();
        }

        public void updateJudgeCalls(Set<Integer> tables) {
            this.judgeCallTables = tables;
            model.fireTableDataChanged();
        }

        private void installJudgeRenderer() {
            DefaultTableCellRenderer judgeRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                    Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                    if (!isSelected) {
                        Match m = model.getMatchAt(row);
                        if (m != null && judgeCallTables.contains(m.getTableNumber())) {
                            c.setBackground(UI_JUDGE);
                            c.setForeground(UI_TEXT);
                        } else {
                            c.setBackground(row % 2 == 0 ? UI_SURFACE : UI_ROW_ALT);
                            c.setForeground(UI_TEXT);
                        }
                    }
                    setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                    return c;
                }
            };
            table.setDefaultRenderer(Object.class, judgeRenderer);
        }
    }

    // ==== TOP CUT DIALOG ====

    static class TopCutDialog extends JDialog {

        static class TopCutTableModel extends AbstractTableModel {
            private final String[] columns = {"Stol", "Igrač 1", "Igrač 2", "Pobjednik"};
            private final List<Match> matches;

            public TopCutTableModel(List<Match> matches) {
                this.matches = matches;
            }

            @Override public int getRowCount() { return matches.size(); }
            @Override public int getColumnCount() { return columns.length; }
            @Override public String getColumnName(int c) { return columns[c]; }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Match m = matches.get(rowIndex);
                Player p1 = m.getP1();
                Player p2 = m.getP2();
                switch (columnIndex) {
                    case 0: return m.getTableNumber();
                    case 1: return p1.getFullName();
                    case 2: return (p2 == null) ? "BYE" : p2.getFullName();
                    case 3:
                        if (m.isBye()) return p1.getFullName() + " (BYE)";
                        switch (m.getResult()) {
                            case P1_WIN: return p1.getFullName();
                            case P2_WIN: return p2.getFullName();
                            default: return "";
                        }
                    default: return "";
                }
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                Match m = matches.get(rowIndex);
                return columnIndex == 3 && !m.isBye();
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (columnIndex != 3) return;
                Match m = matches.get(rowIndex);
                Player p1 = m.getP1();
                Player p2 = m.getP2();
                String v = (aValue == null) ? "" : aValue.toString();
                if (v.equals(p1.getFullName())) m.setResult(MatchResult.P1_WIN);
                else if (p2 != null && v.equals(p2.getFullName())) m.setResult(MatchResult.P2_WIN);
                else m.setResult(MatchResult.UNDECIDED);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        static class WinnerCellEditor extends AbstractCellEditor implements TableCellEditor {
            private JComboBox<String> combo;
            private final List<Match> matches;

            public WinnerCellEditor(List<Match> matches) {
                this.matches = matches;
            }

            @Override public Object getCellEditorValue() { return combo.getSelectedItem(); }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                                                         boolean isSelected, int row, int column) {
                Match m = matches.get(row);
                Player p1 = m.getP1();
                Player p2 = m.getP2();
                combo = new JComboBox<>();
                combo.addItem("");
                if (!m.isBye()) {
                    combo.addItem(p1.getFullName());
                    combo.addItem(p2.getFullName());
                }
                String current = "";
                if (!m.isBye()) {
                    if (m.getResult() == MatchResult.P1_WIN) current = p1.getFullName();
                    if (m.getResult() == MatchResult.P2_WIN) current = p2.getFullName();
                }
                combo.setSelectedItem(current);
                return combo;
            }
        }

        private final Tournament tournament;
        private final List<Player> topCutPlayers;
        private final List<Tournament.StandingSnapshot> finalSwissStandings;

        private final List<List<Match>> topCutRounds = new ArrayList<>();
        private List<Match> currentRoundMatches = new ArrayList<>();
        private int currentRoundNumber = 0;

        private final JLabel lblRound = new JLabel("Top Cut - Runda 1");
        private final JTable table;
        private TopCutTableModel model;
        private final JButton btnNextRound = new JButton("Spremi rundu / sljedeća runda");

        private boolean completed = false;
        private String bracketText = "";

        public boolean isCompleted() { return completed; }
        public String getBracketText() { return bracketText; }

        public TopCutDialog(JFrame owner,
                            Tournament tournament,
                            List<Player> topCutPlayers,
                            List<Tournament.StandingSnapshot> finalSwissStandings) {
            super(owner, "Top Cut - " + tournament.getName(), true);
            this.tournament = tournament;
            this.topCutPlayers = new ArrayList<>(topCutPlayers);
            this.finalSwissStandings = finalSwissStandings;

            setSize(650, 520);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(new Color(0xF5F5F7));

            currentRoundMatches = generateRound(this.topCutPlayers, 1);
            topCutRounds.add(currentRoundMatches);
            currentRoundNumber = 1;

            model = new TopCutTableModel(currentRoundMatches);
            table = new JTable(model);
            styleTable(table);
            table.getColumnModel().getColumn(3).setCellEditor(new WinnerCellEditor(currentRoundMatches));

            JPanel top = new JPanel(new BorderLayout());
            top.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
            top.setOpaque(false);
            lblRound.setFont(lblRound.getFont().deriveFont(Font.BOLD, 16f));
            top.add(lblRound, BorderLayout.WEST);

            JScrollPane sc = new JScrollPane(table);
            sc.setBorder(BorderFactory.createEmptyBorder());
            JPanel center = new JPanel(new BorderLayout());
            center.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
            center.setOpaque(false);
            center.add(sc, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.setBorder(BorderFactory.createEmptyBorder(8, 16, 12, 16));
            bottom.setOpaque(false);
            btnNextRound.setFocusPainted(false);
            btnNextRound.setBackground(new Color(0x007AFF));
            btnNextRound.setForeground(Color.WHITE);
            btnNextRound.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            bottom.add(btnNextRound);

            setLayout(new BorderLayout());
            add(top, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);

            btnNextRound.addActionListener(e -> onNextRound());

            updateRoundLabel();
        }

        private void updateRoundLabel() {
            lblRound.setText("Top Cut - Runda " + currentRoundNumber);
        }

        private List<Match> generateRound(List<Player> players, int roundNo) {
            List<Player> list = new ArrayList<>(players);
            Collections.shuffle(list);

            List<Match> matches = new ArrayList<>();
            int table = 1;
            while (!list.isEmpty()) {
                Player p1 = list.remove(0);
                Player p2 = null;
                if (!list.isEmpty()) p2 = list.remove(0);
                Match m = new Match(table++, p1, p2);
                if (p2 == null) m.setResult(MatchResult.P1_WIN);
                matches.add(m);
            }
            return matches;
        }

        private void onNextRound() {
            for (Match m : currentRoundMatches) {
                if (!m.isBye() && (m.getResult() != MatchResult.P1_WIN && m.getResult() != MatchResult.P2_WIN)) {
                    JOptionPane.showMessageDialog(this,
                            "Za svaki dvoboj moraš odabrati pobjednika.",
                            "Greška", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            List<Player> winners = new ArrayList<>();
            for (Match m : currentRoundMatches) {
                if (m.isBye()) {
                    winners.add(m.getP1());
                } else if (m.getResult() == MatchResult.P1_WIN) {
                    winners.add(m.getP1());
                } else if (m.getResult() == MatchResult.P2_WIN) {
                    winners.add(m.getP2());
                }
            }

            if (winners.size() <= 1) {
                showSummaryAndClose(winners.isEmpty() ? null : winners.get(0));
                return;
            }

            currentRoundNumber++;
            currentRoundMatches = generateRound(winners, currentRoundNumber);
            topCutRounds.add(currentRoundMatches);
            model = new TopCutTableModel(currentRoundMatches);
            table.setModel(model);
            styleTable(table);
            table.getColumnModel().getColumn(3).setCellEditor(new WinnerCellEditor(currentRoundMatches));
            updateRoundLabel();
        }

        private void showSummaryAndClose(Player champion) {
            StringBuilder sb = new StringBuilder();
            sb.append("Top Cut bracket za turnir: ").append(tournament.getName()).append("\n\n");
            if (champion != null) {
                sb.append("Pobjednik TopCuta: ").append(champion.getFullName()).append("\n\n");
            }
            for (int r = 0; r < topCutRounds.size(); r++) {
                sb.append("Runda ").append(r + 1).append(":\n");
                for (Match m : topCutRounds.get(r)) {
                    Player p1 = m.getP1();
                    Player p2 = m.getP2();
                    String line;
                    if (m.isBye()) {
                        line = "  " + p1.getFullName() + " (BYE, prolazi dalje)";
                    } else {
                        String winner;
                        if (m.getResult() == MatchResult.P1_WIN) winner = p1.getFullName();
                        else if (m.getResult() == MatchResult.P2_WIN) winner = p2.getFullName();
                        else winner = "?";
                        line = "  " + p1.getFullName() + " vs " + p2.getFullName()
                                + "  -> Pobjednik: " + winner;
                    }
                    sb.append(line).append("\n");
                }
                sb.append("\n");
            }
            bracketText = sb.toString();
            completed = true;

            JDialog summary = new JDialog(this, "Top Cut rezultat", true);
            summary.setSize(720, 540);
            summary.setLocationRelativeTo(this);
            summary.getContentPane().setBackground(new Color(0xF5F5F7));
            summary.setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();

            String[] cols = {"Pozicija", "ID", "Ime", "Prezime", "Bodovi", "Tie-breaker"};
            Object[][] rows = new Object[finalSwissStandings.size()][cols.length];
            for (int i = 0; i < finalSwissStandings.size(); i++) {
                Tournament.StandingSnapshot s = finalSwissStandings.get(i);
                rows[i][0] = i + 1;
                rows[i][1] = s.id;
                rows[i][2] = s.firstName;
                rows[i][3] = s.lastName;
                rows[i][4] = s.points;
                rows[i][5] = s.tieBreakerCode;
                rows[i][5] = s.tieBreakerCode;
            }
            JTable swissTable = new JTable(rows, cols);
            styleTable(swissTable);
            JScrollPane swissScroll = new JScrollPane(swissTable);
            swissScroll.setBorder(BorderFactory.createEmptyBorder());
            tabs.addTab("Swiss standings", swissScroll);

            JTextArea area = new JTextArea(bracketText);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane bracketScroll = new JScrollPane(area);
            bracketScroll.setBorder(BorderFactory.createEmptyBorder());
            tabs.addTab("TopCut grafikon", bracketScroll);

            summary.add(tabs, BorderLayout.CENTER);

            JButton close = new JButton("Zatvori");
            close.setFocusPainted(false);
            close.setBackground(Color.WHITE);
            close.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE5E5EA)),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
            bottom.setOpaque(false);
            bottom.add(close);
            summary.add(bottom, BorderLayout.SOUTH);

            close.addActionListener(e -> summary.dispose());

            summary.setVisible(true);
            dispose();
        }
    }

    // ==== HISTORY DIALOG ====

    static class HistoryTableModel extends AbstractTableModel {
        private final String[] columns = {"Naziv", "Datum", "Ključ", "Igrača", "TopCut"};
        private List<HistoryEntry> data = new ArrayList<>();

        public void setData(List<HistoryEntry> data) {
            this.data = new ArrayList<>(data);
            fireTableDataChanged();
        }

        public HistoryEntry getEntryAt(int row) {
            if (row < 0 || row >= data.size()) return null;
            return data.get(row);
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            HistoryEntry e = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return e.displayName;
                case 1: return e.date;
                case 2: return e.key;
                case 3: return e.standings.size();
                case 4: return e.hasTopCut() ? "DA" : "NE";
                default: return "";
            }
        }

        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    static class HistoryDialog extends JDialog {
        private final JTextField txtFilter = new JTextField(20);
        private final JTable table;
        private final HistoryTableModel model = new HistoryTableModel();
        private List<HistoryEntry> allEntries = new ArrayList<>();

        public HistoryDialog(JFrame owner) {
            super(owner, "History turnira", true);
            setSize(760, 520);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(new Color(0xF5F5F7));
            setLayout(new BorderLayout());

            allEntries = HistoryManager.loadHistory();
            model.setData(allEntries);

            table = new JTable(model);
            styleTable(table);

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
            JLabel lbl = new JLabel("Filter (ime / datum / ključ):");
            lbl.setForeground(new Color(0x6B7280));
            JPanel topInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            topInner.setOpaque(false);
            topInner.add(lbl);
            topInner.add(txtFilter);
            top.add(topInner, BorderLayout.CENTER);

            JScrollPane sc = new JScrollPane(table);
            sc.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            bottom.setOpaque(false);
            JButton btnDelete = new JButton("Obriši odabrani turnir");
            btnDelete.setFocusPainted(false);
            btnDelete.setBackground(Color.WHITE);
            btnDelete.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE5E5EA)),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
            bottom.add(btnDelete);

            add(top, BorderLayout.NORTH);
            add(sc, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);

            txtFilter.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { refilter(); }
                @Override public void removeUpdate(DocumentEvent e) { refilter(); }
                @Override public void changedUpdate(DocumentEvent e) { refilter(); }
            });

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = table.getSelectedRow();
                        if (row >= 0) {
                            HistoryEntry entry = model.getEntryAt(row);
                            if (entry != null) {
                                HistoryDetailDialog hd = new HistoryDetailDialog(HistoryDialog.this, entry);
                                hd.setVisible(true);
                            }
                        }
                    }
                }
            });

            btnDelete.addActionListener(e -> onDeleteSelected());
        }

        private void refilter() {
            String f = txtFilter.getText().trim().toLowerCase();
            if (f.isEmpty()) {
                model.setData(allEntries);
                return;
            }
            List<HistoryEntry> filtered = new ArrayList<>();
            for (HistoryEntry e : allEntries) {
                if ((e.displayName != null && e.displayName.toLowerCase().contains(f)) ||
                        (e.date != null && e.date.toLowerCase().contains(f)) ||
                        (e.key != null && e.key.toLowerCase().contains(f)) ||
                        (e.baseName != null && e.baseName.toLowerCase().contains(f))) {
                    filtered.add(e);
                }
            }
            model.setData(filtered);
        }

        private void onDeleteSelected() {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this,
                        "Odaberi turnir u tablici za brisanje.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            HistoryEntry entry = model.getEntryAt(row);
            if (entry == null) return;

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Stvarno obrisati turnir iz baze?\n" + entry.displayName,
                    "Brisanje turnira",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            boolean ok = HistoryManager.deleteHistoryEntry(entry);
            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        "Ne mogu obrisati datoteku turnira.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            allEntries = HistoryManager.loadHistory();
            refilter();
        }
    }

    static class HistoryDetailDialog extends JDialog {

        public HistoryDetailDialog(JDialog owner, HistoryEntry entry) {
            super(owner, "Rezultati - " + entry.displayName, true);
            setSize(720, 540);
            setLocationRelativeTo(owner);
            getContentPane().setBackground(new Color(0xF5F5F7));
            setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();

            String[] cols = {"Pozicija", "ID", "Ime", "Prezime", "Bodovi", "Tie-breaker"};
            Object[][] rows = new Object[entry.standings.size()][cols.length];
            for (int i = 0; i < entry.standings.size(); i++) {
                Tournament.StandingSnapshot s = entry.standings.get(i);
                rows[i][0] = i + 1;
                rows[i][1] = s.id;
                rows[i][2] = s.firstName;
                rows[i][3] = s.lastName;
                rows[i][4] = s.points;
                rows[i][5] = s.tieBreakerCode;
            }
            JTable swissTable = new JTable(rows, cols);
            styleTable(swissTable);
            JScrollPane swissScroll = new JScrollPane(swissTable);
            swissScroll.setBorder(BorderFactory.createEmptyBorder());
            tabs.addTab("Swiss standings", swissScroll);

            if (entry.hasTopCut()) {
                JTextArea area = new JTextArea(entry.topCutText);
                area.setEditable(false);
                area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                JScrollPane bracketScroll = new JScrollPane(area);
                bracketScroll.setBorder(BorderFactory.createEmptyBorder());
                tabs.addTab("TopCut grafikon", bracketScroll);
            }

            add(tabs, BorderLayout.CENTER);

            JButton close = new JButton("Zatvori");
            close.setFocusPainted(false);
            close.setBackground(Color.WHITE);
            close.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE5E5EA)),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
            bottom.setOpaque(false);
            bottom.add(close);
            add(bottom, BorderLayout.SOUTH);

            close.addActionListener(e -> dispose());
        }
    }

    // ==== GLAVNI PROZOR JEDNOG TURNIRA ====

    static class TournamentFrame extends JFrame {

        private javax.swing.Timer publishDebounceTimer;
        private boolean publishPending = false;

        private final Tournament tournament;
        private final List<Player> playerDatabase;

        private final PlayersTableModel playersTableModel;
        private final DatabasePlayersTableModel dbTableModel;

        private final JTable dbTable;
        private final JTable playersTable;

        private final StandingsTableModel standingsTableModel = new StandingsTableModel();
        private final JTable standingsTable = new JTable(standingsTableModel);
        private final JComboBox<Integer> cbStandingsRound = new JComboBox<>();

        private final JLabel statusLabel;
        private final JLabel syncLabel = new JLabel("● Offline");

        private final JTextField txtFirstNameFilter = new JTextField(10);
        private final JTextField txtLastNameFilter = new JTextField(10);
        private final JTextField txtIdFilter = new JTextField(10);

        private final JTabbedPane bottomTabs = new JTabbedPane();
        private final List<RoundPanel> roundPanels = new ArrayList<>();

        private boolean tournamentFinished = false;

        private final String tournamentKey;
        private final String tournamentBaseName;

        private JLabel lblTimer;
        private javax.swing.Timer roundTimer;
        private javax.swing.Timer judgePollTimer;
        private JButton btnEndJudgeCall;
        private Set<Integer> judgeCallTables = new java.util.HashSet<>();
        private final int prepSecondsDefault = TournamentConstants.DEFAULT_PREP_SECONDS;
        private int roundSecondsDefault = TournamentConstants.DEFAULT_ROUND_SECONDS;
        private int remainingPrepSeconds = 0;
        private int remainingRoundSeconds = 0;

        private void onWebReports() {
        try {
            List<WebReportEntry> list = onlineFetchReports();
            WebReportsDialog d = new WebReportsDialog(this);
            d.setReports(list);
            d.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Ne mogu dohvatiti reportove: " + ex.getMessage(),
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
}

public List<WebReportEntry> onlineFetchReports() throws Exception {
    return online.fetchReports(tournamentKey);
}

/**
 * Primijeni report u trenutnu rundu (samo ako je rezultat još UNDECIDED).
 */
public boolean applyWebReport(WebReportEntry rep) {
    if (!tournament.isRoundInProgress()) {
        JOptionPane.showMessageDialog(this,
                "Nema aktivne runde (nema gdje primijeniti rezultat).",
                "Info", JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    Match target = null;
    for (Match m : tournament.getCurrentRoundMatches()) {
        if (m.getTableNumber() == rep.table) { target = m; break; }
    }

    if (target == null) {
        JOptionPane.showMessageDialog(this,
                "Ne postoji stol " + rep.table + " u trenutnoj rundi.",
                "Greška", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    if (target.getResult() != MatchResult.UNDECIDED) {
        JOptionPane.showMessageDialog(this,
                "Admin je već upisao rezultat za stol " + rep.table + ".",
                "Info", JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    MatchResult mr;
    switch (rep.result) {
        case "P1_WIN": mr = MatchResult.P1_WIN; break;
        case "P2_WIN": mr = MatchResult.P2_WIN; break;
        case "BOTH_LOSE": mr = MatchResult.BOTH_LOSE; break;
        default:
            JOptionPane.showMessageDialog(this,
                    "Nepoznat result: " + rep.result,
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return false;
    }

    target.setResult(mr);

    // refresh trenutnog tab-a runde
    if (!roundPanels.isEmpty()) {
        RoundPanel rp = roundPanels.get(roundPanels.size() - 1);
        rp.refresh();
    }

    // standings live update
    onAnyResultsChanged();

    JOptionPane.showMessageDialog(this,
            "Primijenjeno: stol " + rep.table + " -> " + rep.result,
            "OK", JOptionPane.INFORMATION_MESSAGE);

    return true;
}
                // ===== ONLINE SYNC (player web view) =====
        private final OnlineSyncClient online = new OnlineSyncClient(readServerUrl());

        private static String readServerUrl() {
            for (String name : new String[]{"config.local.properties", "config.properties"}) {
                File f = new File(name);
                if (!f.exists()) continue;
                try (InputStream in = new FileInputStream(f)) {
                    Properties props = new Properties();
                    props.load(in);
                    String url = props.getProperty("server.url", "").trim();
                    if (!url.isEmpty()) return url;
                } catch (Exception ignored) {}
            }
            return "http://localhost:8080";
        }

        private void setSyncOk(String action) {
            SwingUtilities.invokeLater(() -> {
                syncLabel.setText("● Online");
                syncLabel.setForeground(Color.decode("#27ae60"));
                syncLabel.setToolTipText("Zadnja sinkronizacija: " + action);
            });
        }

        private void setSyncFailed(String action, Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            SwingUtilities.invokeLater(() -> {
                syncLabel.setText("● Sync greška");
                syncLabel.setForeground(Color.decode("#e74c3c"));
                syncLabel.setToolTipText(action + " nije uspio: " + msg);
            });
        }

        private void onlineCreateTournamentIfPossible() {
            try {
                online.createTournament(tournamentKey, tournamentBaseName);
                setSyncOk("createTournament");
            } catch (Exception ex) {
                setSyncFailed("createTournament", ex);
            }
        }

        private void onlineSyncPlayersIfPossible() {
            try {
                online.upsertPlayers(tournamentKey, tournament.getPlayers());
                setSyncOk("upsertPlayers");
            } catch (Exception ex) {
                setSyncFailed("upsertPlayers", ex);
            }
        }

        private void onlinePublishPairingsIfPossible(int roundNumber, List<Match> matches) {
            try {
                online.publishPairings(tournamentKey, roundNumber, matches);
                setSyncOk("publishPairings r" + roundNumber);
            } catch (Exception ex) {
                setSyncFailed("publishPairings r" + roundNumber, ex);
            }
        }

        private void onlineStartTimerIfPossible(int prepSeconds, int roundSeconds) {
            try {
                online.startTimer(tournamentKey, prepSeconds, roundSeconds);
                setSyncOk("startTimer");
            } catch (Exception ex) {
                setSyncFailed("startTimer", ex);
            }
        }

        private void onlineStopTimerIfPossible() {
            try {
                online.stopTimer(tournamentKey);
                setSyncOk("stopTimer");
            } catch (Exception ex) {
                setSyncFailed("stopTimer", ex);
            }
        }

        private static String generateTournamentKey() {
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";
            Random rnd = new Random();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            return sb.toString();
        }

        public TournamentFrame(List<Player> playerDatabase, String tournamentName) {
            this.playerDatabase = playerDatabase;
            this.tournamentBaseName = tournamentName;
            this.tournamentKey = generateTournamentKey();
            this.tournament = new Tournament(tournamentName);

            this.playersTableModel = new PlayersTableModel(tournament);
            this.dbTableModel = new DatabasePlayersTableModel(this.playerDatabase);
            this.dbTable = new JTable(dbTableModel);
            this.playersTable = new JTable(playersTableModel);

            setTitle("Swiss Turnir - " + tournamentBaseName + " [" + tournamentKey + "]");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(1200, 750);
            setLocationRelativeTo(null);
            getContentPane().setBackground(UI_BG);

            statusLabel = new JLabel(
                    "Turnir: " + tournamentBaseName + " | Key: " + tournamentKey + " | Runda: 0 (još nije započela)"
            );
            statusLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, UI_BORDER),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
            statusLabel.setForeground(UI_TEXT2);
            statusLabel.setBackground(UI_TOOLBAR);
            statusLabel.setOpaque(true);

            syncLabel.setFont(syncLabel.getFont().deriveFont(Font.BOLD, 11f));
            syncLabel.setForeground(Color.decode("#888888"));
            syncLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, UI_BORDER),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
            syncLabel.setBackground(UI_TOOLBAR);
            syncLabel.setOpaque(true);

            initUi();
            // Poll judge calls so desktop can highlight tables that requested a judge
            judgePollTimer = new javax.swing.Timer(1500, e -> refreshJudgeCallsSilently());
            judgePollTimer.setRepeats(true);
            judgePollTimer.start();

            onlineCreateTournamentIfPossible();
            onlineSyncPlayersIfPossible();
        }

        private JButton toolbarButton(String text) {
            JButton b = new JButton(text);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setBackground(UI_SURFACE);
            b.setForeground(UI_TEXT);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UI_BORDER),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            return b;
        }

        private JButton toolbarPrimaryButton(String text) {
            JButton b = new JButton(text);
            b.setFocusPainted(false);
            b.setOpaque(true);
            b.setBackground(UI_ACCENT);
            b.setForeground(Color.WHITE);
            b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
            b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
            return b;
        }

        private void initUi() {
            JButton btnNewRound = toolbarPrimaryButton("Nova runda");
            JButton btnSaveRound = toolbarButton("Spremi rezultate");
            JButton btnEndTournament = toolbarButton("Završi / TOP CUT");
            JButton btnNewTournamentWindow = toolbarButton("Novi turnir");
            JButton btnDropPlayer = toolbarButton("Drop");
            JButton btnTimeExtension = toolbarButton("Time ext.");
            JButton btnSetTimer = toolbarButton("Timer");
            JButton btnUndoRound = toolbarButton("Undo runda");
            JButton btnRePair = toolbarButton("RePair");
            JButton btnWebReports = toolbarButton("Web reports");

            JPanel topPanelInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            topPanelInner.setBackground(UI_TOOLBAR);
            topPanelInner.add(btnNewRound);
            topPanelInner.add(btnSaveRound);
            topPanelInner.add(btnUndoRound);
            topPanelInner.add(btnRePair);
            topPanelInner.add(btnEndTournament);
            topPanelInner.add(btnNewTournamentWindow);
            topPanelInner.add(btnDropPlayer);
            topPanelInner.add(btnTimeExtension);
            topPanelInner.add(btnWebReports);

            btnEndJudgeCall = toolbarButton("End judge call");
            btnEndJudgeCall.addActionListener(ev -> {
                try {
                    RoundPanel rp = currentRoundPanel();
                    Integer tableNo = (rp == null) ? null : rp.getSelectedTableNumber();
                    if (tableNo == null) {
                        JOptionPane.showMessageDialog(this, "Odaberi red (stol) u rundi koji želiš očistiti.", "Judge call", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    online.clearJudgeCall(tournamentKey, tableNo);
                    judgeCallTables.remove(tableNo);
                    applyJudgeCallsToRounds();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ne mogu očistiti judge call: " + ex.getMessage(), "Judge call", JOptionPane.ERROR_MESSAGE);
                }
            });
            topPanelInner.add(btnEndJudgeCall);

            
            JTextField txtKeyDisplay = new JTextField(tournamentKey);
            txtKeyDisplay.setEditable(false);
            txtKeyDisplay.setColumns(10);
            txtKeyDisplay.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            txtKeyDisplay.setBackground(Color.WHITE);

            JButton btnCopyKey = toolbarButton("Copy key");
            btnCopyKey.addActionListener(ev -> {
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(tournamentKey), null);
                } catch (Exception ignored) {}
            });

lblTimer = new JLabel("Timer: --:--");
            lblTimer.setFont(lblTimer.getFont().deriveFont(Font.BOLD, 13f));
            lblTimer.setForeground(UI_TEXT);
            JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
            topRight.setBackground(UI_TOOLBAR);
            topRight.add(new JLabel("Key:"));
            topRight.add(txtKeyDisplay);
            topRight.add(btnCopyKey);
            topRight.add(lblTimer);
            topRight.add(btnSetTimer);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(UI_TOOLBAR);
            topPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UI_BORDER),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            topPanel.add(topPanelInner, BorderLayout.WEST);
            topPanel.add(topRight, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);

            styleTable(dbTable);
            JScrollPane dbScroll = new JScrollPane(dbTable);
            dbScroll.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 1, 1, 0, new Color(0xE5E5EA)),
                    BorderFactory.createEmptyBorder()
            ));

            JLabel lblDbTitle = new JLabel("Baza igrača");
            lblDbTitle.setFont(lblDbTitle.getFont().deriveFont(Font.BOLD, 14f));
            lblDbTitle.setForeground(UI_TEXT);

            JPanel searchPanel = new JPanel(new GridBagLayout());
            searchPanel.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 2, 2, 2);
            c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.LINE_END;
            searchPanel.add(new JLabel("Ime:"), c);
            c.gridx = 1; c.anchor = GridBagConstraints.LINE_START;
            searchPanel.add(txtFirstNameFilter, c);

            c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.LINE_END;
            searchPanel.add(new JLabel("Prezime:"), c);
            c.gridx = 1; c.anchor = GridBagConstraints.LINE_START;
            searchPanel.add(txtLastNameFilter, c);

            c.gridx = 0; c.gridy = 2; c.anchor = GridBagConstraints.LINE_END;
            searchPanel.add(new JLabel("ID:"), c);
            c.gridx = 1; c.anchor = GridBagConstraints.LINE_START;
            searchPanel.add(txtIdFilter, c);

            JButton btnAddSelected = toolbarButton("Dodaj iz baze");
            JButton btnAddNew = toolbarButton("Registriraj novog");
            JButton btnAddGuest = toolbarButton("Dodaj gosta");

            JPanel addButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            addButtonsPanel.setOpaque(false);
            addButtonsPanel.add(btnAddSelected);
            addButtonsPanel.add(btnAddNew);
            addButtonsPanel.add(btnAddGuest);

            JPanel leftPanel = new JPanel(new BorderLayout(0, 4));
            leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 6));
            leftPanel.setOpaque(false);
            leftPanel.add(lblDbTitle, BorderLayout.NORTH);
            leftPanel.add(dbScroll, BorderLayout.CENTER);

            JPanel leftBottom = new JPanel(new BorderLayout());
            leftBottom.setOpaque(false);
            leftBottom.add(searchPanel, BorderLayout.CENTER);
            leftBottom.add(addButtonsPanel, BorderLayout.SOUTH);
            leftPanel.add(leftBottom, BorderLayout.SOUTH);

            styleTable(playersTable);
            JScrollPane playersScroll = new JScrollPane(playersTable);
            playersScroll.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 1, 1, new Color(0xE5E5EA)),
                    BorderFactory.createEmptyBorder()
            ));

            JLabel lblPlayersTitle = new JLabel("Igrači na turniru");
            lblPlayersTitle.setFont(lblPlayersTitle.getFont().deriveFont(Font.BOLD, 14f));
            lblPlayersTitle.setForeground(UI_TEXT);

            JPanel rightPanel = new JPanel(new BorderLayout(0, 4));
            rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 12));
            rightPanel.setOpaque(false);
            rightPanel.add(lblPlayersTitle, BorderLayout.NORTH);
            rightPanel.add(playersScroll, BorderLayout.CENTER);

            JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
            topSplit.setResizeWeight(0.5);
            topSplit.setBorder(null);

            styleTable(standingsTable);
            JScrollPane standingsScroll = new JScrollPane(standingsTable);
            standingsScroll.setBorder(BorderFactory.createEmptyBorder());

            JPanel standingsPanel = new JPanel(new BorderLayout());
            standingsPanel.setOpaque(false);
            JPanel standingsTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
            standingsTop.setOpaque(false);
            JLabel lblStandTitle = new JLabel("Standings");
            lblStandTitle.setFont(lblStandTitle.getFont().deriveFont(Font.BOLD, 14f));
            lblStandTitle.setForeground(new Color(0x111827));
            standingsTop.add(lblStandTitle);
            standingsTop.add(Box.createHorizontalStrut(16));
            standingsTop.add(new JLabel("Runda:"));
            cbStandingsRound.setEnabled(false);
            standingsTop.add(cbStandingsRound);

            standingsPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            standingsPanel.add(standingsTop, BorderLayout.NORTH);
            standingsPanel.add(standingsScroll, BorderLayout.CENTER);

            bottomTabs.addTab("Standings", standingsPanel);
            bottomTabs.setBorder(BorderFactory.createEmptyBorder());

            JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, bottomTabs);
            mainSplit.setResizeWeight(0.52);
            mainSplit.setBorder(null);

            JPanel bottomBar = new JPanel(new BorderLayout());
            bottomBar.add(statusLabel, BorderLayout.CENTER);
            bottomBar.add(syncLabel, BorderLayout.EAST);

            add(mainSplit, BorderLayout.CENTER);
            add(bottomBar, BorderLayout.SOUTH);

            DocumentListener filterListener = new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { refilterDb(); }
                @Override public void removeUpdate(DocumentEvent e) { refilterDb(); }
                @Override public void changedUpdate(DocumentEvent e) { refilterDb(); }
            };
            txtFirstNameFilter.getDocument().addDocumentListener(filterListener);
            txtLastNameFilter.getDocument().addDocumentListener(filterListener);
            txtIdFilter.getDocument().addDocumentListener(filterListener);

            dbTable.addMouseListener(new MouseAdapter() {
                private void showPopup(MouseEvent e) {
                    int row = dbTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < dbTable.getRowCount()) {
                        dbTable.setRowSelectionInterval(row, row);
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem editItem = new JMenuItem("Promijeni podatke");
                        editItem.addActionListener(ev -> editDatabasePlayer(row));
                        JMenuItem deleteItem = new JMenuItem("Obriši igrača");
                        deleteItem.addActionListener(ev -> deleteDatabasePlayer(row));
                        menu.add(editItem);
                        menu.add(deleteItem);
                        menu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
                @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showPopup(e); }
                @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showPopup(e); }
            });

            btnAddSelected.addActionListener(e -> onAddSelectedFromDb());
            btnAddNew.addActionListener(e -> onRegisterNewAndAdd());
            btnAddGuest.addActionListener(e -> onAddGuest());

            btnDropPlayer.addActionListener(e -> onDropPlayer());

            btnNewRound.addActionListener(e -> onStartRound());
            btnSaveRound.addActionListener(e -> onSaveResults());
            btnUndoRound.addActionListener(e -> onUndoLastRound());
            btnRePair.addActionListener(e -> onRePairCurrentRound());
            btnEndTournament.addActionListener(e -> onEndTournament());
            btnWebReports.addActionListener(e -> onWebReports());

            btnNewTournamentWindow.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(this, "Naziv novog turnira:");
                if (name == null || name.trim().isEmpty()) return;
                new TournamentFrame(playerDatabase, name.trim()).setVisible(true);
            });

            btnTimeExtension.addActionListener(e -> onTimeExtension());
            btnSetTimer.addActionListener(e -> onSetTimer());
            cbStandingsRound.addActionListener(e -> onStandingsRoundChanged());

            roundTimer = new javax.swing.Timer(1000, e -> onTimerTick());
            publishDebounceTimer = new javax.swing.Timer(600, e -> {
                publishDebounceTimer.stop();
                if (!publishPending) return;
                publishPending = false;

                if (tournament.isRoundInProgress()) {
                    int roundNo = tournament.getAllRounds().size();
                    onlinePublishPairingsIfPossible(roundNo, tournament.getCurrentRoundMatches());
                }
            });
            publishDebounceTimer.setRepeats(false);

        }

        private void refilterDb() {
            dbTableModel.refilter(
                    txtFirstNameFilter.getText(),
                    txtLastNameFilter.getText(),
                    txtIdFilter.getText()
            );
        }

        private void editDatabasePlayer(int row) {
            Player p = dbTableModel.getPlayerAt(row);
            if (p == null) return;

            JTextField fnField = new JTextField(p.getFirstName(), 15);
            JTextField lnField = new JTextField(p.getLastName(), 15);
            JTextField idField = new JTextField(p.getId(), 15);

            JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
            panel.add(new JLabel("Ime:"));
            panel.add(fnField);
            panel.add(new JLabel("Prezime:"));
            panel.add(lnField);
            panel.add(new JLabel("ID:"));
            panel.add(idField);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Promijeni podatke igrača",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (result != JOptionPane.OK_OPTION) return;

            String newFn = fnField.getText().trim();
            String newLn = lnField.getText().trim();
            String newId = idField.getText().trim();

            if (newFn.isEmpty() || newLn.isEmpty() || newId.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Ime, prezime i ID ne smiju biti prazni.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (Player other : playerDatabase) {
                if (other != p && other.getId().equals(newId)) {
                    JOptionPane.showMessageDialog(this,
                            "Igrač " + other.getFirstName() + " " + other.getLastName() +
                                    " već koristi taj ID.",
                            "Greška", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            p.setFirstName(newFn);
            p.setLastName(newLn);
            p.setId(newId);

            savePlayerDatabase(playerDatabase);
            refilterDb();
        }

        private void deleteDatabasePlayer(int row) {
            Player p = dbTableModel.getPlayerAt(row);
            if (p == null) return;
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Stvarno obrisati igrača iz baze?\n" +
                            p.getFirstName() + " " + p.getLastName() + " (" + p.getId() + ")",
                    "Brisanje igrača",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;
            playerDatabase.remove(p);
            savePlayerDatabase(playerDatabase);
            refilterDb();
        }

        private void onAddSelectedFromDb() {
            int row = dbTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Odaberi igrača iz baze.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Player fromDb = dbTableModel.getPlayerAt(row);
            if (fromDb == null) return;

            Player copy = new Player(fromDb.getFirstName(), fromDb.getLastName(), fromDb.getId(), false);
            if (!addPlayerToTournamentWithStartingPoints(copy)) {
                JOptionPane.showMessageDialog(this, "Igrač je već u turniru ili nema mjesta.", "Greška",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        private void onRegisterNewAndAdd() {
            String fn = txtFirstNameFilter.getText().trim();
            String ln = txtLastNameFilter.getText().trim();
            String id = txtIdFilter.getText().trim();

            if (fn.isEmpty() || ln.isEmpty() || id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Za registraciju trebaš ime, prezime i ID.", "Greška",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (Player p : playerDatabase) {
                if (p.getId().equals(id)) {
                    JOptionPane.showMessageDialog(this,
                            "Igrač " + p.getFirstName() + " " + p.getLastName() +
                                    " već koristi taj ID.",
                            "Greška", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            Player newP = new Player(fn, ln, id, false);
            playerDatabase.add(newP);
            savePlayerDatabase(playerDatabase);
            refilterDb();

            Player copy = new Player(newP.getFirstName(), newP.getLastName(), newP.getId(), false);
            if (!addPlayerToTournamentWithStartingPoints(copy)) {
                JOptionPane.showMessageDialog(this,
                        "Ne mogu dodati igrača u turnir (duplikat ili nema mjesta).",
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void onAddGuest() {
            String fn = txtFirstNameFilter.getText().trim();
            String ln = txtLastNameFilter.getText().trim();

            if (fn.isEmpty() || ln.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Za gosta unesi barem ime i prezime.", "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Player guest = new Player(fn, ln, "000000000", true);
            if (!addPlayerToTournamentWithStartingPoints(guest)) {
                JOptionPane.showMessageDialog(this,
                        "Ne mogu dodati gosta (nema mjesta?).", "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }

        private boolean addPlayerToTournamentWithStartingPoints(Player p) {
        if (tournament.getPlayers().size() >= Tournament.MAX_PLAYERS) {
            JOptionPane.showMessageDialog(this,
                    "Dosegnut maksimalan broj igrača (" + Tournament.MAX_PLAYERS + ").",
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return false;
        }

    int finished = tournament.getFinishedRounds();
    int basePts = 0;
    if (finished > 0) {
        int maxPoints = finished * 3;
        List<String> optionsList = new ArrayList<>();
        for (int i = 0; i <= maxPoints; i += 3) optionsList.add(String.valueOf(i));
        String[] opts = optionsList.toArray(new String[0]);

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "S koliko bodova ulazi igrač?\n(Nakon " + finished + ". runde max " + maxPoints + " bodova)",
                "Početni bodovi",
                JOptionPane.QUESTION_MESSAGE,
                null,
                opts,
                opts[0]
        );
        if (selected == null) return false;

        try { basePts = Integer.parseInt(selected); }
        catch (NumberFormatException ex) { basePts = 0; }
    }

    p.setBasePoints(basePts);

    boolean added = tournament.addPlayer(p);
    if (added) {
        playersTableModel.fireTableDataChanged();

        // ONLINE SYNC: ažuriraj roster na serveru (samo ako je uspješno dodan)
        onlineSyncPlayersIfPossible();
    }
    return added;
}

        private void onDropPlayer() {
            int row = playersTable.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Odaberi igrača u tablici desno.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Player p = tournament.getPlayers().get(row);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Želiš li dropati igrača " + p.getFullName() + " iz turnira?\n" +
                            "(Neće više biti uparivan, ali zadržava bodove.)",
                    "Drop igrača",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                p.setDropped(true);
                playersTableModel.fireTableDataChanged();
            }
        }

        private void onStartRound() {
            if (tournamentFinished) {
                JOptionPane.showMessageDialog(this,
                        "Turnir je završen. Ne možeš više pokretati Swiss runde.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (tournament.isRoundInProgress()) {
                JOptionPane.showMessageDialog(this,
                        "Trenutna runda još traje. Najprije spremi rezultate.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (tournament.getPlayers().size() < 2) {
                JOptionPane.showMessageDialog(this,
                        "Za Swiss runde treba barem 2 igrača.", "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                List<Match> matches = tournament.startNextRound();
                int roundNo = tournament.getAllRounds().size();

                onlinePublishPairingsIfPossible(roundNo, matches);

                RoundPanel rp = new RoundPanel(
                        tournament,
                        matches,
                        true,
                        this::onAnyResultsChanged
                );
                roundPanels.add(rp);
                bottomTabs.addTab("Runda " + roundNo, rp);
                bottomTabs.setSelectedComponent(rp);

                statusLabel.setText("Turnir: " + tournamentBaseName + " | Key: " + tournamentKey +
                        " | Runda " + roundNo + " je u tijeku.");
                updateStandingsRoundCombo();
                onAnyResultsChanged();

                startRoundTimer();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Greška pri generiranju parova: " + ex.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void onSaveResults() {
            if (!tournament.isRoundInProgress()) {
                JOptionPane.showMessageDialog(this,
                        "Nema aktivne runde za spremanje.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (!tournament.allResultsEntered()) {
                JOptionPane.showMessageDialog(this,
                        "Nisu uneseni svi rezultati (uključujući BYE ishode).", "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                tournament.applyResultsAndFinishRound();
                playersTableModel.fireTableDataChanged();

                int roundsFinished = tournament.getFinishedRounds();
                statusLabel.setText("Turnir: " + tournamentBaseName + " | Key: " + tournamentKey +
                        " | Runda " + roundsFinished + " je završena.");

                updateStandingsRoundCombo();
                onAnyResultsChanged();

                if (roundTimer != null) {
                    roundTimer.stop();
                    lblTimer.setText("Timer: --:--");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Greška pri spremanju rezultata: " + ex.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void onUndoLastRound() {
            if (!tournament.hasRounds()) {
                JOptionPane.showMessageDialog(this,
                        "Nema rundi za poništiti.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Poništiti zadnju rundu? (parovi i rezultati te runde će biti obrisani)",
                    "Poništi rundu",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            int lastRoundIndex = tournament.getAllRounds().size() - 1;
            int tabIndex = 1 + lastRoundIndex;

            if (tabIndex >= 0 && tabIndex < bottomTabs.getTabCount()) {
                bottomTabs.remove(tabIndex);
            }
            if (lastRoundIndex >= 0 && lastRoundIndex < roundPanels.size()) {
                roundPanels.remove(lastRoundIndex);
            }

            try {
                tournament.undoLastRound();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Greška pri poništavanju runde: " + ex.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (roundTimer != null) {
                roundTimer.stop();
                lblTimer.setText("Timer: --:--");
            }

            updateStandingsRoundCombo();
            onAnyResultsChanged();

            statusLabel.setText("Turnir: " + tournamentBaseName + " | Key: " + tournamentKey +
                    " | Zadnja runda poništena. Odigrano rundi: " + tournament.getFinishedRounds());
        }

        private void onRePairCurrentRound() {
            if (!tournament.isRoundInProgress()) {
                JOptionPane.showMessageDialog(this,
                        "Nema aktivne runde za RePair.\nRePair radi samo dok je runda u tijeku.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Ponovno upariti trenutačnu rundu?\nSvi već uneseni rezultati te runde će biti izgubljeni.",
                    "RePair runde",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            try {
                    List<Match> matches = tournament.regenerateCurrentRoundPairings();

                    int roundNo = tournament.getAllRounds().size();      // npr. 3 (runda broj 3)
                    int currentRoundIndex = roundNo - 1;                 // index u listi (0-based)

                    onlinePublishPairingsIfPossible(roundNo, matches);

                    int tabIndex = 1 + currentRoundIndex;

                    RoundPanel rp = new RoundPanel(
                            tournament,
                            matches,
                            true,
                            this::onAnyResultsChanged
                );

                if (currentRoundIndex >= 0 && currentRoundIndex < roundPanels.size()) {
                    roundPanels.set(currentRoundIndex, rp);
                }
                if (tabIndex >= 0 && tabIndex < bottomTabs.getTabCount()) {
                    bottomTabs.setComponentAt(tabIndex, rp);
                    bottomTabs.setSelectedIndex(tabIndex);
                }

                onAnyResultsChanged();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Greška pri RePairu runde: " + ex.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void onEndTournament() {
            if (tournamentFinished) {
                JOptionPane.showMessageDialog(this,
                        "Turnir je već završen. Rezultate možeš vidjeti u History-ju.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (tournament.isRoundInProgress()) {
                JOptionPane.showMessageDialog(this,
                        "Najprije završi aktivnu rundu (Spremi rezultate).",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int totalRounds = tournament.getAllRounds().size();
            if (totalRounds == 0) {
                JOptionPane.showMessageDialog(this,
                        "Nema odigranih rundi, nema smisla završavati turnir.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int nPlayers = tournament.getPlayers().size();

            if (nPlayers < 4) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Premalo igrača za TopCut (" + nPlayers + ").\nŽeliš li svejedno završiti turnir (samo Swiss)?",
                        "Završi turnir",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    List<Tournament.StandingSnapshot> finalStandings =
                            tournament.computeStandingsForRound(totalRounds);
                    HistoryManager.saveSwissOnlyHistory(tournamentBaseName, tournamentKey, finalStandings);
                    tournamentFinished = true;
                    statusLabel.setText("Turnir: " + tournamentBaseName + " | Key: " + tournamentKey +
                            " | Završio (samo Swiss). Spremljen u History.");
                    JOptionPane.showMessageDialog(this,
                            "Turnir završen (samo Swiss) i spremljen u History.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
                }
                return;
            }

            Object[] options = {"Završi bez TopCuta", "Pokreni TopCut", "Odustani"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "Kako želiš završiti turnir?",
                    "Završi turnir / TOP CUT",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]
            );

            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }

            if (choice == 0) {
                List<Tournament.StandingSnapshot> finalStandings =
                        tournament.computeStandingsForRound(totalRounds);
                HistoryManager.saveSwissOnlyHistory(tournamentBaseName, tournamentKey, finalStandings);
                tournamentFinished = true;
                statusLabel.setText("Turnir: " + tournamentBaseName + " | Key: " + tournamentKey +
                        " | Završio (samo Swiss). Spremljen u History.");
                JOptionPane.showMessageDialog(this,
                        "Turnir završen (samo Swiss) i spremljen u History.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int[] cuts = {4, 6, 8, 12, 32};
            List<Integer> allowed = new ArrayList<>();
            for (int cut : cuts) {
                if (cut < nPlayers && cut <= nPlayers) allowed.add(cut);
            }
            if (allowed.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Nema valjanih opcija TOP CUT za broj igrača: " + nPlayers,
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Integer cutSize = (Integer) JOptionPane.showInputDialog(
                    this,
                    "Odaberi TOP CUT (broj igrača koji ulaze u knock-out):",
                    "TOP CUT",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    allowed.toArray(new Integer[0]),
                    allowed.get(0)
            );
            if (cutSize == null) return;

            List<Tournament.StandingSnapshot> finalStandings =
                    tournament.computeStandingsForRound(totalRounds);
            if (finalStandings.size() < cutSize) {
                JOptionPane.showMessageDialog(this,
                        "Nedovoljno igrača za odabrani TOP CUT.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Player> topCutPlayers = new ArrayList<>();
            for (int i = 0; i < cutSize; i++) {
                Tournament.StandingSnapshot s = finalStandings.get(i);
                Player p = null;
                for (Player candidate : tournament.getPlayers()) {
                    if (candidate.getId().equals(s.id)
                            && candidate.getFirstName().equals(s.firstName)
                            && candidate.getLastName().equals(s.lastName)) {
                        p = candidate;
                        break;
                    }
                }
                if (p != null) topCutPlayers.add(p);
            }

            if (topCutPlayers.size() < cutSize) {
                JOptionPane.showMessageDialog(this,
                        "Greška pri formiranju TopCut liste.", "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            TopCutDialog topCutDialog = new TopCutDialog(this, tournament, topCutPlayers, finalStandings);
            topCutDialog.setVisible(true);

            if (topCutDialog.isCompleted()) {
                HistoryManager.saveSwissTopCutHistory(
                        tournamentBaseName,
                        tournamentKey,
                        finalStandings,
                        topCutDialog.getBracketText()
                );
                tournamentFinished = true;
                statusLabel.setText("Turnir: " + tournamentBaseName + " | Key: " + tournamentKey +
                        " | Završio (TopCut). Spremljen u History.");
                JOptionPane.showMessageDialog(this,
                        "Turnir (TopCut) završen i spremljen u History.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "TopCut nije završen, turnir još nije spremljen u History.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        }

        private void updateStandingsRoundCombo() {
            cbStandingsRound.removeAllItems();
            int totalRounds = tournament.getAllRounds().size();
            for (int i = 1; i <= totalRounds; i++) cbStandingsRound.addItem(i);
            cbStandingsRound.setEnabled(totalRounds > 0);
            if (totalRounds > 0) {
                cbStandingsRound.setSelectedItem(totalRounds);
                updateStandingsTable(totalRounds);
            } else {
                standingsTableModel.setData(Collections.emptyList());
            }
        }

        private void onStandingsRoundChanged() {
            Integer round = (Integer) cbStandingsRound.getSelectedItem();
            if (round != null) updateStandingsTable(round);
        }

        private void updateStandingsTable(int round) {
            List<Tournament.StandingSnapshot> data = tournament.computeStandingsForRound(round);
            standingsTableModel.setData(data);
        }

        private void onAnyResultsChanged() {
            playersTableModel.fireTableDataChanged();
            Integer round = (Integer) cbStandingsRound.getSelectedItem();
            if (round != null) updateStandingsTable(round);
            for (RoundPanel rp : roundPanels) rp.refresh();
            publishPending = true;
            publishDebounceTimer.restart();
        }

        private void startRoundTimer() {
            remainingPrepSeconds = prepSecondsDefault;
            remainingRoundSeconds = roundSecondsDefault;
            updateTimerLabel();
            if (roundTimer != null) {
                roundTimer.stop();
            }
            roundTimer.start();
            onlineStartTimerIfPossible(prepSecondsDefault, roundSecondsDefault);
        }

        private void onTimerTick() {
            if (remainingPrepSeconds > 0) {
                remainingPrepSeconds--;
            } else if (remainingRoundSeconds > 0) {
                remainingRoundSeconds--;
            } else {
                roundTimer.stop();
                Toolkit.getDefaultToolkit().beep();
            }
            updateTimerLabel();
        }

        private void updateTimerLabel() {
            String text;
            if (remainingPrepSeconds > 0) {
                text = "Prep: " + formatTime(remainingPrepSeconds);
            } else if (remainingRoundSeconds > 0) {
                text = "Round: " + formatTime(remainingRoundSeconds);
            } else {
                text = "Timer: 00:00";
            }
            lblTimer.setText(text);
        }

        private String formatTime(int seconds) {
            int m = seconds / 60;
            int s = seconds % 60;
            return String.format("%02d:%02d", m, s);
        }

        private void onSetTimer() {
            String input = JOptionPane.showInputDialog(this,
                    "Unesi trajanje runde u minutama (bez pripremnih 3 min):",
                    roundSecondsDefault / 60);
            if (input == null) return;
            try {
                int mins = Integer.parseInt(input.trim());
                if (mins <= 0) throw new NumberFormatException();
                roundSecondsDefault = mins * 60;
                JOptionPane.showMessageDialog(this,
                        "Novo trajanje runde: " + mins + " min (+ 3 min pripreme).",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Unesi pozitivan cijeli broj.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }

        private RoundPanel currentRoundPanel() {
            Component comp = bottomTabs.getSelectedComponent();
            return (comp instanceof RoundPanel) ? (RoundPanel) comp : null;
        }

        private void applyJudgeCallsToRounds() {
            for (RoundPanel rp : roundPanels) {
                rp.updateJudgeCalls(judgeCallTables);
            }
        }

        private void refreshJudgeCallsSilently() {
            try {
                List<Integer> tables = online.getJudgeCalls(tournamentKey);
                judgeCallTables = new java.util.HashSet<>(tables);
                applyJudgeCallsToRounds();
            } catch (Exception ignored) {}
        }

        private void onTimeExtension() {
            Component comp = bottomTabs.getSelectedComponent();
            if (!(comp instanceof RoundPanel)) {
                JOptionPane.showMessageDialog(this,
                        "Odaberi tab runde za koju želiš dodati time extension.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            RoundPanel rp = (RoundPanel) comp;
            JTable t = rp.getTable();
            int row = t.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this,
                        "Odaberi stol (red) u rundi.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Match m = rp.getMatchAt(row);
            if (m == null) return;

            String input = JOptionPane.showInputDialog(this,
                    "Unesi time extension u minutama za stol " + m.getTableNumber() + ":\n(0 za uklanjanje)",
                    m.getTimeExtensionMinutes());
            if (input == null) return;
            try {
                int mins = Integer.parseInt(input.trim());
                if (mins < 0) throw new NumberFormatException();
                m.setTimeExtensionMinutes(mins);
                rp.refresh();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Unesi 0 ili pozitivan cijeli broj.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }

            if (tournament.isRoundInProgress()) {
                int roundNo = tournament.getAllRounds().size();
                onlinePublishPairingsIfPossible(roundNo, tournament.getCurrentRoundMatches());
            }
        }
    }
        // ===== ONLINE SYNC CLIENT (HTTP JSON) =====

        static class OnlineSyncClient {
    private final String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    OnlineSyncClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    void createTournament(String key, String name) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("key", key);
        body.addProperty("name", name);
        postJson("/api/tournaments", gson.toJson(body));
    }

    void upsertPlayers(String key, List<Player> players) throws Exception {
        JsonArray arr = new JsonArray();
        for (Player p : players) {
            if (p == null || p.isGuest()) continue;
            arr.add(playerJson(p));
        }
        JsonObject body = new JsonObject();
        body.add("players", arr);
        putJson("/api/tournaments/" + url(key) + "/players", gson.toJson(body));
    }

    void publishPairings(String key, int roundNumber, List<Match> matches) throws Exception {
        JsonArray arr = new JsonArray();
        for (Match m : matches) {
            if (m == null) continue;
            JsonObject obj = new JsonObject();
            obj.addProperty("table", m.getTableNumber());
            obj.add("p1", playerJson(m.getP1()));
            obj.add("p2", m.getP2() == null ? JsonNull.INSTANCE : playerJson(m.getP2()));
            obj.addProperty("result", m.getResult() == null ? "UNDECIDED" : m.getResult().name());
            obj.addProperty("timeExtensionMin", m.getTimeExtensionMinutes());
            arr.add(obj);
        }
        JsonObject body = new JsonObject();
        body.addProperty("roundNumber", roundNumber);
        body.add("matches", arr);
        putJson("/api/tournaments/" + url(key) + "/pairings", gson.toJson(body));
    }

    void startTimer(String key, int prepSeconds, int roundSeconds) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("prepSeconds", prepSeconds);
        body.addProperty("roundSeconds", roundSeconds);
        postJson("/api/tournaments/" + url(key) + "/timer/start", gson.toJson(body));
    }

    void stopTimer(String key) throws Exception {
        postJson("/api/tournaments/" + url(key) + "/timer/stop", "{}");
    }

    // =========================
    // WEB REPORTS (admin fetch)
    // =========================
    List<WebReportEntry> fetchReports(String key) throws Exception {
        String path = "/api/tournaments/" + url(key) + "/reports";
        String body = getText(path);
        return parseReportsJson(body);
    }

    // ---- Judge calls (player -> admin assistance) ----
    List<Integer> getJudgeCalls(String key) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/api/tournaments/" + url(key) + "/judgecalls"))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) return Collections.emptyList();

        List<Integer> out = new ArrayList<>();
        try {
            String body = resp.body() == null ? "" : resp.body();
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            JsonArray tables = obj.getAsJsonArray("tables");
            if (tables != null) {
                for (JsonElement el : tables) {
                    out.add(el.getAsInt());
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    // Clears judge call for a specific table (admin action).
    void clearJudgeCall(String key, int table) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("table", table);
        postJson("/api/tournaments/" + url(key) + "/judgecall/clear", gson.toJson(body));
    }

    private JsonObject playerJson(Player p) {
        JsonObject obj = new JsonObject();
        if (p == null) {
            obj.addProperty("id", "");
            obj.addProperty("firstName", "");
            obj.addProperty("lastName", "");
        } else {
            obj.addProperty("id", p.isGuest() ? "000000000" : p.getId());
            obj.addProperty("firstName", p.getFirstName());
            obj.addProperty("lastName", p.getLastName());
        }
        return obj;
    }

    private void postJson(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("POST " + path + " -> HTTP " + resp.statusCode() + ": " + resp.body());
        }
    }

    private void putJson(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("PUT " + path + " -> HTTP " + resp.statusCode() + ": " + resp.body());
        }
    }

    private String getText(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + path))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("GET " + path + " -> HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return resp.body();
    }

    private String url(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private List<WebReportEntry> parseReportsJson(String json) {
        List<WebReportEntry> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                WebReportEntry e = new WebReportEntry();
                e.table          = obj.has("table")           ? obj.get("table").getAsInt()              : 0;
                e.playerId       = obj.has("playerId")        ? obj.get("playerId").getAsString()        : null;
                e.result         = obj.has("result")          ? obj.get("result").getAsString()          : null;
                e.timeEpochMillis= obj.has("timeEpochMillis") ? obj.get("timeEpochMillis").getAsLong()   : 0L;
                if (e.table > 0 && e.playerId != null && !e.playerId.isBlank()) {
                    out.add(e);
                }
            }
        } catch (Exception ignored) {}
        return out;
    }
}

         // ===== WEB REPORTS (desktop admin) =====
static class WebReportEntry {
    int table;
    String playerId;
    String result; // "P1_WIN" | "P2_WIN" | "BOTH_LOSE"
    long timeEpochMillis;
}

static class WebReportsTableModel extends AbstractTableModel {
    private final String[] cols = {"Stol", "Player ID", "Rezultat", "Vrijeme"};
    private final List<WebReportEntry> data = new ArrayList<>();

    public void setData(List<WebReportEntry> list) {
        data.clear();
        if (list != null) data.addAll(list);
        data.sort(Comparator.comparingInt(a -> a.table));
        fireTableDataChanged();
    }

    public WebReportEntry getAt(int row) {
        if (row < 0 || row >= data.size()) return null;
        return data.get(row);
    }

    public void remove(WebReportEntry e) {
        data.remove(e);
        fireTableDataChanged();
    }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }
    @Override public boolean isCellEditable(int r, int c) { return false; }

    @Override
    public Object getValueAt(int r, int c) {
        WebReportEntry e = data.get(r);
        switch (c) {
            case 0: return e.table;
            case 1: return e.playerId;
            case 2: return e.result;
            case 3:
                if (e.timeEpochMillis <= 0) return "";
                Date d = new Date(e.timeEpochMillis);
                return new SimpleDateFormat("HH:mm:ss").format(d);
            default: return "";
        }
    }
}

static class WebReportsDialog extends JDialog {
    private final JTable table;
    private final WebReportsTableModel model = new WebReportsTableModel();

    public WebReportsDialog(JFrame owner) {
        super(owner, "Web reports", true);
        setSize(720, 420);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(new Color(0xF5F5F7));
        setLayout(new BorderLayout());

        table = new JTable(model);
        styleTable(table);

        JScrollPane sc = new JScrollPane(table);
        sc.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(sc, BorderLayout.CENTER);

        JButton btnRefresh = new JButton("Osvježi");
        JButton btnApply = new JButton("Primijeni odabrani");
        JButton btnClose = new JButton("Zatvori");

        for (JButton b : new JButton[]{btnRefresh, btnApply, btnClose}) {
            b.setFocusPainted(false);
            b.setBackground(Color.WHITE);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE5E5EA)),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
        }

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bottom.setOpaque(false);
        bottom.add(btnRefresh);
        bottom.add(btnApply);
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);

        btnClose.addActionListener(e -> dispose());

        // owner mora biti TournamentFrame da bi mogao pozvati callbacke
        TournamentFrame tf = (TournamentFrame) owner;

        btnRefresh.addActionListener(e -> {
            try {
                List<WebReportEntry> list = tf.onlineFetchReports();
                model.setData(list);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Ne mogu dohvatiti reportove: " + ex.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnApply.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this,
                        "Odaberi report u tablici.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            WebReportEntry entry = model.getAt(row);
            if (entry == null) return;

            boolean ok = tf.applyWebReport(entry);
            if (ok) {
                model.remove(entry); // lokalno makni da se ne primjenjuje opet
            }
        });
    }

    public void setReports(List<WebReportEntry> list) {
        model.setData(list);
    }
}
}