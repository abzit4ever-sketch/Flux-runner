const http = require("http");
const crypto = require("crypto");

const PORT = Number(process.env.PORT || 8080);
const HOST = process.env.HOST || "0.0.0.0";
const DUEL_DURATION_MS = 90_000;

const waiting = [];
const matches = new Map();

function id(prefix) {
  return `${prefix}_${crypto.randomBytes(8).toString("hex")}`;
}

function now() {
  return Date.now();
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    req.on("data", chunk => {
      body += chunk;
      if (body.length > 64_000) {
        reject(new Error("Request body too large"));
        req.destroy();
      }
    });
    req.on("end", () => {
      if (!body) return resolve({});
      try {
        resolve(JSON.parse(body));
      } catch (error) {
        reject(error);
      }
    });
  });
}

function send(res, status, payload) {
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type"
  });
  res.end(JSON.stringify(payload));
}

function publicMatch(match, playerId) {
  const opponent = match.players.find(p => p.playerId !== playerId);
  const me = match.players.find(p => p.playerId === playerId);
  return {
    matchId: match.matchId,
    seed: match.seed,
    status: match.status,
    startedAt: match.startedAt,
    endsAt: match.endsAt,
    playerId,
    displayName: me?.displayName || "Player",
    opponentId: opponent?.playerId || "",
    opponentName: opponent?.displayName || "Waiting",
    players: match.players.map(player => ({
      playerId: player.playerId,
      displayName: player.displayName,
      score: player.score,
      distance: player.distance,
      alive: player.alive,
      finished: player.finished,
      lastSeenAt: player.lastSeenAt
    })),
    winnerId: match.winnerId || "",
    tokenReward: match.tokenReward
  };
}

function createWaitingMatch(player) {
  const match = {
    matchId: id("match"),
    seed: Math.floor(Math.random() * 1_000_000_000),
    status: "waiting",
    players: [player],
    startedAt: 0,
    endsAt: 0,
    winnerId: "",
    tokenReward: 10
  };
  matches.set(match.matchId, match);
  waiting.push(match.matchId);
  return match;
}

function joinMatch(player) {
  while (waiting.length > 0) {
    const matchId = waiting.shift();
    const match = matches.get(matchId);
    if (!match || match.status !== "waiting" || match.players.length !== 1) continue;
    if (match.players[0].playerId === player.playerId) continue;

    match.players.push(player);
    match.status = "active";
    match.startedAt = now();
    match.endsAt = match.startedAt + DUEL_DURATION_MS;
    return match;
  }

  return createWaitingMatch(player);
}

function settleIfNeeded(match) {
  if (match.status === "finished") return;
  const expired = match.endsAt > 0 && now() >= match.endsAt;
  const bothFinished = match.players.length === 2 && match.players.every(p => p.finished || !p.alive);
  if (!expired && !bothFinished) return;

  match.status = "finished";
  const ranked = [...match.players].sort((a, b) => {
    if (b.score !== a.score) return b.score - a.score;
    return b.distance - a.distance;
  });
  match.winnerId = ranked[0]?.playerId || "";
}

function updatePlayer(match, playerId, patch) {
  const player = match.players.find(p => p.playerId === playerId);
  if (!player) return false;

  player.score = Math.max(player.score, Number(patch.score || 0));
  player.distance = Math.max(player.distance, Number(patch.distance || 0));
  player.alive = Boolean(patch.alive);
  player.finished = Boolean(patch.finished);
  player.lastSeenAt = now();
  settleIfNeeded(match);
  return true;
}

async function route(req, res) {
  if (req.method === "OPTIONS") return send(res, 200, { ok: true });

  const url = new URL(req.url, `http://${req.headers.host}`);
  const parts = url.pathname.split("/").filter(Boolean);

  if (req.method === "GET" && url.pathname === "/health") {
    return send(res, 200, { ok: true, waiting: waiting.length, matches: matches.size });
  }

  if (req.method === "POST" && url.pathname === "/matchmaking/queue") {
    const body = await readBody(req);
    const player = {
      playerId: String(body.playerId || id("player")),
      displayName: String(body.displayName || "Runner").slice(0, 18),
      score: 0,
      distance: 0,
      alive: true,
      finished: false,
      lastSeenAt: now()
    };
    const match = joinMatch(player);
    return send(res, 200, publicMatch(match, player.playerId));
  }

  if (parts[0] === "matches" && parts[1]) {
    const match = matches.get(parts[1]);
    if (!match) return send(res, 404, { error: "Match not found" });

    if (req.method === "GET" && parts[2] === "state") {
      settleIfNeeded(match);
      return send(res, 200, publicMatch(match, url.searchParams.get("playerId") || ""));
    }

    if (req.method === "POST" && parts[2] === "state") {
      const body = await readBody(req);
      const ok = updatePlayer(match, String(body.playerId || ""), body);
      if (!ok) return send(res, 403, { error: "Player not in match" });
      return send(res, 200, publicMatch(match, String(body.playerId || "")));
    }
  }

  send(res, 404, { error: "Not found" });
}

const server = http.createServer((req, res) => {
  route(req, res).catch(error => {
    send(res, 500, { error: error.message || "Server error" });
  });
});

server.listen(PORT, HOST, () => {
  console.log(`Flux Runner PvP server listening on http://${HOST}:${PORT}`);
});
