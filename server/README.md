# Flux Runner PvP Server

This is a small HTTP matchmaking server for local and deployable PvP testing.

Run it locally:

```powershell
cd server
npm start
```

For a USB-connected Android phone, forward phone localhost to this computer:

```powershell
adb reverse tcp:8080 tcp:8080
```

The Android app uses `http://127.0.0.1:8080` by default, which works with `adb reverse`.

Endpoints:

- `GET /health`
- `POST /matchmaking/queue`
- `GET /matches/:matchId/state?playerId=...`
- `POST /matches/:matchId/state`

This server is for skill-based free-entry PvP.

Deploy options:

- Render: create a new Blueprint from `server/render.yaml`, then use the generated `https://...onrender.com` URL.
- Fly.io: from the `server` folder run `fly launch` or use the included `fly.toml`, then use the generated `https://...fly.dev` URL.
- Any Docker host: build from `server/Dockerfile` and expose port `8080`.

After deployment, update Android:

```xml
<string name="pvp_server_url">https://YOUR-LIVE-SERVER.example.com</string>
```

Then rebuild and reinstall the app.
