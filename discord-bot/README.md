# FlintFix Discord bot

Slash commands for the FlintFix Discord server, plus automatic release announcements.

| Command | What it does |
|---|---|
| `/download` | Latest FlintFix Client installer from GitHub Releases |
| `/status` | Latest version and whether friends & chat (the FlintFix backend) are online |
| `/versions` | Supported Minecraft versions and what's limited on 26.2/26.3 |
| `/report` | Sends a bug report to the staff channel (one per user every 5 minutes) |
| `/help` | Lists the commands |

When a new release is published on GitHub, the bot posts it in the announcement channel
(it checks every 10 minutes).

## Setup

1. Go to https://discord.com/developers/applications, click **New Application**, name it "FlintFix".
2. **Bot** tab: click **Reset Token** and copy it. Keep it secret.
3. **OAuth2 → URL Generator**: tick `bot` and `applications.commands`; under bot permissions tick
   **Send Messages**, **Embed Links** and **View Channels**. Open the generated link and add the bot to your server.
4. Install Node.js 20.12 or newer, then in this folder:

   ```
   npm install
   copy .env.example .env
   ```

5. Fill in `.env` (token, application ID, server ID, channel IDs).
6. Start it:

   ```
   npm start
   ```

The bot only runs while that window is open. To keep it online 24/7, run it on an always-on
machine or a small VPS (the server that hosts the FlintFix backend works), for example with
`npx pm2 start index.js --name flintfix-bot`.

`state.json` (created automatically) remembers the last announced release and report cooldowns.
