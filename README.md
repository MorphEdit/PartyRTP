# PartyRTP

**PartyRTP** is a lightweight controller plugin that allows players to **RTP as a party**, without replacing existing RTP plugins.

It works by teleporting the party leader using the server’s RTP provider, then safely pulling party members after the teleport succeeds.

## Features
- Party system (create / invite / accept)
- Party RTP (`/prtp go`)
- Hardened RTP flow (token, distance check, timeout)
- Party persistence (save/load + autosave)
- RTP provider abstraction (COMMAND-based, plugin-agnostic)
- Fully configurable (world rules, limits, cooldowns)
- Clean UX with configurable messages

## How it works
1. Leader triggers RTP using the server’s existing RTP command  
2. PartyRTP detects a successful leader teleport  
3. Party members are teleported to the leader’s new location  

## Requirements
- Paper / Purpur
- Any RTP plugin (via command)

## Commands
- `/prtp` – show help
- `/prtp create`
- `/prtp invite <player>`
- `/prtp accept <leader>`
- `/prtp list`
- `/prtp leave`
- `/prtp disband`
- `/prtp go`

## Configuration
RTP command and behavior are fully configurable via `config.yml`.

---

**PartyRTP does not replace RTP plugins — it orchestrates them.**
