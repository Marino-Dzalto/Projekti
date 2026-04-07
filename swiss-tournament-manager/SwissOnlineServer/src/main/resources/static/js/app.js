// API je relativan na host gdje se servira web (ngrok / LAN / localhost)
const API = location.origin + "/api";

let evtSource = null;
let current = {
  key: null,
  pid: null,
  timer: null,
  registered: false,
  judgeTables: [] // active judge calls for this tournament (table numbers)
};
let myMatchCache = null;

const el = (id) => document.getElementById(id);

const LS_KEY = "swiss_key";
const LS_PID = "swiss_pid";
const LS_LIVE = "swiss_live";

function fmt2(n) { return String(n).padStart(2, "0"); }

// ---------------- TIMER ----------------

function computeRemaining(timer) {
  if (!timer || !timer.running) return null;
  const now = Date.now();
  const elapsed = Math.floor((now - timer.startEpochMillis) / 1000);
  const prep = timer.prepSeconds || 180;
  const round = timer.roundSeconds || 3300;

  if (elapsed < prep) return { phase: "Prep", sec: prep - elapsed };

  const afterPrep = elapsed - prep;
  const remainRound = round - afterPrep;
  if (remainRound > 0) return { phase: "Round", sec: remainRound };

  return { phase: "End", sec: 0 };
}

function renderTimer() {
  const r = computeRemaining(current.timer);
  if (!r) { el("timer").textContent = "--:--"; return; }
  const m = Math.floor(r.sec / 60), s = r.sec % 60;
  el("timer").textContent = `${r.phase}: ${fmt2(m)}:${fmt2(s)}`;
}
setInterval(renderTimer, 250);

// ---------------- RENDER HELPERS ----------------

function playerLine(p) {
  if (!p) return "BYE";
  // NE prikazujemo ID-eve drugih igrača
  return `${p.firstName || ""} ${p.lastName || ""}`.trim();
}

function resultText(m) {
  if (!m) return "";
  const r = (m.result) ? String(m.result) : "UNDECIDED";

  if (r === "P1_WIN") return `${playerLine(m.p1)} WIN`;
  if (r === "P2_WIN") return `${playerLine(m.p2)} WIN`;
  if (r === "BOTH_LOSE") return "DOUBLE LOSS";

  if (!m.p2 && r === "P1_WIN") return "BYE WIN";
  if (!m.p2 && r === "BOTH_LOSE") return "BYE LOSS";

  return "";
}

function findMyMatch(pairings, pid) {
  return (pairings || []).find(m =>
    (m.p1 && m.p1.id === pid) || (m.p2 && m.p2.id === pid)
  ) || null;
}

function getMyTable(myMatch) {
  if (!myMatch) return null;
  const t = myMatch.table;
  return (t === 0 || t) ? t : null;
}

// ---------------- MY MATCH + PAIRINGS ----------------

function renderMyMatch(my) {
  if (my) {
    el("mycard").style.display = "block";
    const res = resultText(my);
    el("mystatus").textContent = res ? "Rezultat: " + res : "Rezultat: -";
    el("myinfo").textContent = `Stol ${my.table} • ${playerLine(my.p1)} vs ${playerLine(my.p2)}`;
  } else {
    el("mycard").style.display = current.registered ? "block" : "none";
    el("mystatus").textContent = "Rezultat: -";
    el("myinfo").textContent = "Trenutno nema objavljenih pairinga / nema tvoje partije.";
  }
}

function renderPairings(pairings, myPid) {
  const tbody = el("pairings").querySelector("tbody");
  tbody.innerHTML = "";

  const judgeSet = new Set((current.judgeTables || []).map(Number));

  (pairings || []).slice().sort((a, b) => a.table - b.table).forEach(m => {
    const tr = document.createElement("tr");

    const isMe = (m.p1 && m.p1.id === myPid) || (m.p2 && m.p2.id === myPid);
    if (isMe) tr.classList.add("me");

    // vizualno označi stolove koji su zvali judge
    if (judgeSet.has(Number(m.table))) tr.classList.add("judge");

    const ext = (m.timeExtensionMin && m.timeExtensionMin > 0) ? `+${m.timeExtensionMin} min` : "";
    const res = resultText(m);

    tr.innerHTML = `
      <td class="mono">${m.table}</td>
      <td>${playerLine(m.p1)}</td>
      <td>${playerLine(m.p2)}</td>
      <td>${res}</td>
      <td>${ext}</td>
    `;
    tbody.appendChild(tr);
  });
}

// ---------------- LOGIN PERSIST ----------------

function persistLogin() {
  localStorage.setItem(LS_KEY, current.key || "");
  localStorage.setItem(LS_PID, current.pid || "");
}

function persistLiveState() {
  localStorage.setItem(LS_LIVE, evtSource ? "1" : "0");
}

// ---------------- REPORT RESULT (player -> server) ----------------

function resetReportUI() {
  const sel = el("reportSelect");
  sel.innerHTML = `<option value="">-- odaberi --</option>`;
  el("reportStatus").textContent = "";
}

function buildReportOptionsForMyMatch(my) {
  resetReportUI();
  const sel = el("reportSelect");

  if (!my || !my.p1 || !my.p2) return; // nema reporta za BYE

  const pid = current.pid;
  const amP1 = (my.p1 && my.p1.id === pid);

  const meName = amP1 ? playerLine(my.p1) : playerLine(my.p2);
  const oppName = amP1 ? playerLine(my.p2) : playerLine(my.p1);

  const myWin = amP1 ? "P1_WIN" : "P2_WIN";
  const oppWin = amP1 ? "P2_WIN" : "P1_WIN";

  sel.insertAdjacentHTML("beforeend", `<option value="${myWin}">${meName} WIN</option>`);
  sel.insertAdjacentHTML("beforeend", `<option value="${oppWin}">${oppName} WIN</option>`);
  sel.insertAdjacentHTML("beforeend", `<option value="BOTH_LOSE">DOUBLE LOSS</option>`);
}

function updateReportUI(myMatch) {
  myMatchCache = myMatch;

  if (!current.registered || !myMatch || !myMatch.p2) {
    el("reportcard").style.display = "none";
    return;
  }

  const decided = myMatch.result && myMatch.result !== "UNDECIDED";
  if (decided) {
    el("reportcard").style.display = "none";
    return;
  }

  el("reportcard").style.display = "block";
  buildReportOptionsForMyMatch(myMatch);
}

// ---------------- JUDGE CALL ----------------

function hideJudgeAll() {
  if (el("judgeIdle")) el("judgeIdle").style.display = "none";
  if (el("judgeConfirm")) el("judgeConfirm").style.display = "none";
  if (el("judgeActive")) el("judgeActive").style.display = "none";
}

function judgeSetStatus(msg) {
  const s = el("judgeStatus");
  if (!s) return;
  s.textContent = msg || "";
}

function shouldShowJudgeCard(myMatch) {
  if (!current.registered || !myMatch || !myMatch.p2) return false;
  // sakrij ako je rezultat već unesen
  const decided = myMatch.result && myMatch.result !== "UNDECIDED";
  return !decided;
}

function isJudgeActiveForMyTable(myMatch) {
  const t = getMyTable(myMatch);
  if (!t) return false;
  return (current.judgeTables || []).map(Number).includes(Number(t));
}

function renderJudgeUI(myMatch) {
  const card = el("judgecard");
  if (!card) return;

  if (!shouldShowJudgeCard(myMatch)) {
    card.style.display = "none";
    return;
  }

  card.style.display = "block";

  const active = isJudgeActiveForMyTable(myMatch);

  hideJudgeAll();
  if (active) {
    if (el("judgeActive")) el("judgeActive").style.display = "block";
  } else {
    if (el("judgeIdle")) el("judgeIdle").style.display = "block";
  }
}

async function fetchJudgeCallsOnce() {
  if (!current.key) return;
  try {
    const r = await fetch(`${API}/tournaments/${encodeURIComponent(current.key)}/judgecalls`);
    if (!r.ok) return;
    const data = await r.json();
    current.judgeTables = Array.isArray(data.tables) ? data.tables : [];
  } catch (_) {
    // ignore
  }
}

async function startJudgeCall() {
  judgeSetStatus("");
  try {
    const r = await fetch(`${API}/tournaments/${encodeURIComponent(current.key)}/judgecall/start`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ playerId: current.pid })
    });

    if (r.status === 404) {
      judgeSetStatus("Nemaš aktivan meč (ili nema pairinga).");
      return false;
    }
    if (!r.ok) {
      judgeSetStatus("Greška pri slanju (server).");
      return false;
    }

    const data = await r.json();
    current.judgeTables = Array.isArray(data.tables) ? data.tables : [];
    judgeSetStatus("Poziv poslan ✅");
    return true;
  } catch (e) {
    judgeSetStatus("Server nije dostupan.");
    return false;
  }
}

async function endJudgeCall() {
  judgeSetStatus("");
  try {
    const r = await fetch(`${API}/tournaments/${encodeURIComponent(current.key)}/judgecall/end`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ playerId: current.pid })
    });

    if (r.status === 404) {
      judgeSetStatus("Nemaš aktivan meč (ili nema pairinga).");
      return false;
    }
    if (!r.ok) {
      judgeSetStatus("Greška pri slanju (server).");
      return false;
    }

    const data = await r.json();
    current.judgeTables = Array.isArray(data.tables) ? data.tables : [];
    judgeSetStatus("Poziv zatvoren ✅");
    return true;
  } catch (e) {
    judgeSetStatus("Server nije dostupan.");
    return false;
  }
}

// UI handlers (judge)
if (el("judgeBtn")) {
  el("judgeBtn").addEventListener("click", () => {
    judgeSetStatus("");
    hideJudgeAll();
    if (el("judgeConfirm")) el("judgeConfirm").style.display = "block";
  });
}
if (el("judgeNo")) {
  el("judgeNo").addEventListener("click", () => {
    judgeSetStatus("");
    // vrati u idle (ili active ako je u međuvremenu aktivirano)
    renderJudgeUI(myMatchCache);
  });
}
if (el("judgeYes")) {
  el("judgeYes").addEventListener("click", async () => {
    const ok = await startJudgeCall();
    if (!ok) { renderJudgeUI(myMatchCache); return; }
    // odmah prebaci u active UI (SSE će također ažurirati)
    hideJudgeAll();
    if (el("judgeActive")) el("judgeActive").style.display = "block";
    // i osvježi označavanje stolova u tablici
    renderPairings(lastPairingsCache, current.pid);
  });
}
if (el("judgeEnd")) {
  el("judgeEnd").addEventListener("click", async () => {
    const ok = await endJudgeCall();
    if (!ok) { renderJudgeUI(myMatchCache); return; }
    // odmah prebaci u idle UI
    hideJudgeAll();
    if (el("judgeIdle")) el("judgeIdle").style.display = "block";
    renderPairings(lastPairingsCache, current.pid);
  });
}

// Cache zadnjih pairinga da možemo re-renderati kad dođu judge events (bez reload)
let lastPairingsCache = [];

// ---------------- LOAD VIEW ----------------

async function loadView({ silent = false } = {}) {
  const key = el("key").value.trim();
  const pid = el("pid").value.trim();
  if (!key || !pid) {
    if (!silent) el("status").textContent = "Unesi Tournament Key i Player ID.";
    return;
  }

  current.key = key;
  current.pid = pid;
  persistLogin();

  let res;
  try {
    res = await fetch(`${API}/tournaments/${encodeURIComponent(key)}/player/${encodeURIComponent(pid)}`);
  } catch (e) {
    el("status").textContent = "Server nije dostupan.";
    el("mycard").style.display = "none";
    el("paircard").style.display = "none";
    el("reportcard").style.display = "none";
    if (el("judgecard")) el("judgecard").style.display = "none";
    return;
  }

  if (!res.ok) {
    el("status").textContent = "Turnir ne postoji (ili server nije dostupan).";
    el("mycard").style.display = "none";
    el("paircard").style.display = "none";
    el("reportcard").style.display = "none";
    if (el("judgecard")) el("judgecard").style.display = "none";
    return;
  }

  const data = await res.json();

  current.timer = data.timer;
  current.registered = !!data.registered;

  el("sub").textContent = `${data.tournamentName} • Key: ${data.tournamentKey}`;
  el("roundpill").textContent = `Runda: ${data.currentRound || 0}`;

  if (!data.registered) {
    el("status").textContent = "Nisi registriran u ovom turniru (provjeri ID).";
    el("mycard").style.display = "none";
    el("reportcard").style.display = "none";
    if (el("judgecard")) el("judgecard").style.display = "none";
  } else {
    el("status").textContent = "Registriran ✅";
  }

  // povuci trenutno stanje judge-callova (da radi i bez Live)
  await fetchJudgeCallsOnce();

  el("paircard").style.display = "block";
  lastPairingsCache = Array.isArray(data.pairings) ? data.pairings : [];
  renderPairings(lastPairingsCache, pid);

  const my = data.myMatch || findMyMatch(lastPairingsCache, pid);
  renderMyMatch(my);
  updateReportUI(my);
  renderJudgeUI(my);
}

// ---------------- LIVE (SSE) ----------------

function setLive(on) {
  if (on) {
    if (evtSource) return;
    if (!current.key) {
      el("status").textContent = "Prvo se prijavi (Uđi).";
      return;
    }

    el("live").textContent = "Live: ON";
    el("live").classList.remove("ghost");
    el("live").classList.add("primary");

    evtSource = new EventSource(`${API}/tournaments/${encodeURIComponent(current.key)}/events`);

    evtSource.addEventListener("PAIRINGS_UPDATED", (ev) => {
      const payload = JSON.parse(ev.data);
      el("roundpill").textContent = `Runda: ${payload.round || 0}`;

      lastPairingsCache = Array.isArray(payload.pairings) ? payload.pairings : [];
      renderPairings(lastPairingsCache, current.pid);

      const my = findMyMatch(lastPairingsCache, current.pid);
      renderMyMatch(my);
      updateReportUI(my);
      renderJudgeUI(my);
    });

    evtSource.addEventListener("TIMER_STARTED", (ev) => {
      current.timer = JSON.parse(ev.data);
    });

    evtSource.addEventListener("TIMER_STOPPED", (ev) => {
      current.timer = JSON.parse(ev.data);
    });

    // JUDGE CALLS (novo)
    evtSource.addEventListener("JUDGE_CALLS_UPDATED", (ev) => {
      const payload = JSON.parse(ev.data);
      current.judgeTables = Array.isArray(payload.tables) ? payload.tables : [];

      // samo re-render tablice i judge UI (bez silent reload-a)
      renderPairings(lastPairingsCache, current.pid);
      renderJudgeUI(myMatchCache);
    });

    evtSource.onerror = () => {
      // EventSource automatski reconnecta
    };

    persistLiveState();
  } else {
    if (evtSource) { evtSource.close(); evtSource = null; }
    el("live").textContent = "Live: OFF";
    el("live").classList.add("ghost");
    el("live").classList.remove("primary");
    persistLiveState();
  }
}

// ---------------- REPORT CLICK ----------------

el("sendReport").addEventListener("click", async () => {
  const key = current.key;
  const pid = current.pid;
  const val = el("reportSelect").value;

  if (!key || !pid) { el("reportStatus").textContent = "Prvo se prijavi."; return; }
  if (!val) { el("reportStatus").textContent = "Odaberi rezultat."; return; }

  if (myMatchCache && myMatchCache.result && myMatchCache.result !== "UNDECIDED") {
    el("reportStatus").textContent = "Admin je već upisao rezultat – ne možeš ga mijenjati.";
    return;
  }

  try {
    const res = await fetch(`${API}/tournaments/${encodeURIComponent(key)}/report`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ playerId: pid, result: val })
    });

    if (res.status === 409) {
      el("reportStatus").textContent = "Admin je već upisao rezultat – ne možeš ga mijenjati.";
      return;
    }
    if (res.status === 404) {
      el("reportStatus").textContent = "Nemaš aktivan meč (ili nema pairinga).";
      return;
    }
    if (!res.ok) {
      el("reportStatus").textContent = "Greška pri slanju (server).";
      return;
    }

    el("reportStatus").textContent = "Poslano ✅ (admin će vidjeti prijavu)";
  } catch (e) {
    el("reportStatus").textContent = "Server nije dostupan.";
  }
});

// ---------------- UX ----------------

function onEnter(e) {
  if (e.key === "Enter") el("load").click();
}
el("key").addEventListener("keydown", onEnter);
el("pid").addEventListener("keydown", onEnter);

el("load").addEventListener("click", async () => { await loadView(); });

el("live").addEventListener("click", async () => {
  if (!current.key || !current.pid) {
    await loadView();
    if (!current.key) return;
  }
  setLive(!evtSource);
});

// Restore session on refresh
(function restore() {
  const k = (localStorage.getItem(LS_KEY) || "").trim();
  const p = (localStorage.getItem(LS_PID) || "").trim();
  const live = (localStorage.getItem(LS_LIVE) || "0") === "1";

  if (k) el("key").value = k;
  if (p) el("pid").value = p;

  if (k && p) {
    loadView({ silent: true }).then(() => {
      if (live) setLive(true);
    });
  }
})();