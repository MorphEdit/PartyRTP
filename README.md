# PartyRTP

**PartyRTP** is a lightweight **single-server** controller plugin that allows players to **RTP as a party**, without replacing existing RTP plugins.

The plugin runs entirely on **one Paper/Purpur server** and works by teleporting the party leader using the server’s configured RTP command, then safely pulling party members after the teleport succeeds.

PartyRTP does **not** handle random teleport logic itself — it orchestrates an existing RTP plugin.

---

## Features
- Party system (create / invite / accept)
- Party RTP (`/prtp go`)
- Hardened RTP flow (token, distance check, timeout)
- Party persistence (save/load + autosave)
- RTP provider abstraction (command-based, plugin-agnostic)
- Fully configurable (world rules, limits, cooldowns)
- Clean UX with configurable messages

---

## How it works
1. The party leader triggers `/prtp go`
2. PartyRTP executes the configured RTP command for the leader
3. PartyRTP detects a successful leader teleport
4. Party members are teleported to the leader’s new location

All logic is handled **locally on the same server**.

---

## Requirements
- Paper / Purpur (single server)
- Any RTP plugin that provides a command (e.g. `/rtp`)

---

## Commands
- `/prtp` – show help
- `/prtp create`
- `/prtp invite <player>`
- `/prtp accept <leader>`
- `/prtp list`
- `/prtp leave`
- `/prtp disband`
- `/prtp go`

---

## Configuration
- RTP command execution
- World restrictions
- Distance limits
- Cooldowns and timeouts
- Messages and UX

All behavior is configurable via `config.yml`.

---

**PartyRTP does not replace RTP plugins — it orchestrates them on a single server.**
