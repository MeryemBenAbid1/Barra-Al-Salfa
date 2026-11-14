# Al-Dakheel (الدخيل) — game client

CpE 458 Network Programming project: `SpyServer.java` (server) and `SpyClient.java` + screen classes (client).

## What changed in this version

- **English / Arabic**: the عربي / EN button in the top bar switches the language and flips the whole window right-to-left. The choice is remembered. All text lives in `Lang.java`, including Arabic for the server's word bank, categories and ranks.
- **Server renamed** to Al-Dakheel (`ROLE:You are Al-Dakheel!`, window title, messages). The client also still accepts the old `You are Brra Al-Salfa!` text.
- **Duplicate names**: the server refuses a nickname that is already in use (not case sensitive) by sending `NAME_TAKEN` and closing the connection. The client goes back to sign-in and shows an error.

Messages the server writes as sentences (INFO lines, the reason in RESULT) are still in English.

### New: the host picks the file, less text

- **The host** picks the topic and opens the case. If a TV host screen is connected, the host screen is the host. Otherwise the first player who joined is the host (their ID card says HOST). If the host leaves, the next one takes over. The server tells everyone with `HOST:<name>` (`HOST:#SCREEN` = the TV) and refuses `PICK` / `START_GAME` from anyone else.
- **The drawer of files** (`FilePopup.java` + `FileCabinet.java`): when the room is created (you become host), the screen darkens and an open filing-cabinet drawer slides up, only for the host. The files stand one behind the other and the **tag** of each file is the topic (plus `? RANDOM`). The host clicks a tag: that file slides up out of the drawer, a red **CHOSEN** stamp lands, and the pop-up closes by itself. The lobby then shows the chosen file (`ChosenFolder.java`); the host can click it (**CHANGE FILE**) to open the drawer again. Other players just see the chosen file and "Host: <name>".
- **Less text**: shorter labels in English and Arabic, no title/photo/leaderboard in the lobby, no "EXHIBIT A" photo, no "SUSPECT Nº" tags, no "[ REDACTED ]" slots, and the chat hides server lines the screen already shows (joined, voted, round started...). Missing images now show a "?" silhouette instead of the file name.
- **Host screen address**: the TV now shows the Wi-Fi address (it used to pick a virtual adapter like `bridge100`), and the font shrinks so the whole address fits.

### New: voice, host screen, category files

- **Push-to-talk voice**: hold **HOLD TO TALK** under the chat. Sound is sent over **UDP port 1203** to the server's `VoiceRelay`, which passes it to everyone else (see `VoiceChat.java`). A red "… TALKING" line shows who is speaking.
  - macOS asks for **microphone permission** the first time (for Terminal / VS Code). Allow it, then restart the client.
  - The server computer may ask to **allow incoming connections** for Java: allow it, or other laptops can't connect.
- **Host screen for a gathering**: on the sign-in screen press **OPEN AS HOST SCREEN** (or run `java SpyClient --host-screen <server-ip>`). Put that window on the TV / projector. It shows the join address, the agents, a huge timer, the question order, who has voted and the final reveal. It never receives the secret word. Players join from their own laptops as usual.
- **Category files**: the server reads `categories/*.txt` (one file per category, see the top of `SpyServer.java` for the format). The host picks a file in the lobby, or RANDOM. Add a new `.txt` file any time; it shows up when the next round starts.

### Playing in a gathering

1. On the computer connected to the TV: `java SpyServer` → Start Server, then `java SpyClient` → **OPEN AS HOST SCREEN**.
2. Everyone on the same Wi-Fi runs `java SpyClient` on their laptop and types the address shown on the TV.
3. At the TV computer, click a file and press OPEN CASE, then play.

## Run

Put all the `.java` files and the `fonts/`, `images/`, `sounds/` folders in the same folder as `SpyServer.java`, then:

```bash
javac -encoding UTF-8 *.java
java SpyServer            # in one terminal
java SpyClient            # one per player
```

Quick start for testing (skips the sign-in screen):

```bash
java SpyClient Nouri localhost
```

Run from the project folder, so the game can find `fonts/`, `images/` and `sounds/`.

## Files

| File | What it is |
|---|---|
| `SpyServer.java` | the game server (unchanged design, renamed + duplicate-name check) |
| `SpyClient.java` | main window, networking, message handling, screen switching |
| `Lang.java` | every English and Arabic text, plus the word-bank translations |
| `VoiceChat.java` | push-to-talk voice over UDP |
| `HostScreen.java` | the big TV view for gatherings |
| `categories/` | the word files (Food, Places, Jobs, Animals, Activities, Occasions) |
| `ConnectScreen.java` | sign-in (codename + server IP) |
| `LobbyScreen.java` | the chosen file, the agents, OPEN CASE (host only), the drawer pop-up |
| `FilePopup.java` | the pop-up that slides the drawer up for the host |
| `FileCabinet.java` | the drawer: files one behind the other, the topic on each tag (painted + animated) |
| `ChosenFolder.java` | the chosen file shown in the lobby (host clicks it to change) |
| `RoundScreen.java` | secret card, case status, persons of interest |
| `VoteScreen.java` | WHO IS AL-DAKHEEL?, accuse + file accusation |
| `ResultScreen.java` | verdict stamp, case report, agent report, XP |
| `Hud.java` | top strip: evidence, phase stamp, timer, SFX, agent badge |
| `ChatPanel.java` | "INTERCEPTED" chat with typewriter effect |
| `IdCard.java`, `SecretCard.java`, `Polaroid.java`, `Stamp.java`, `DossierButton.java`, `Leaderboard.java`, `DashedBorder.java` | painted components |
| `Theme.java` | colors, fonts, images, sounds |
| `Anim.java` | animation helper (javax.swing.Timer) |

## Images (optional, `images/`)

Missing images show a dark photo with a "?" silhouette.

| File | Used for |
|---|---|
| `mascot.png` | sign-in suspect photo |
| `mugshot.png` | verdict screen mugshot |
| `avatar_<name>.png` | optional photo for one player's ID card, e.g. `avatar_meryem.png` |

## Sounds (optional, `sounds/`, .wav)

`stamp`, `click`, `type`, `reveal`, `flip`, `tick`, `marker`, `scratch`, `swoosh`, `win`, `lose`

Missing sounds are silent. The SFX button in the top bar mutes everything.

## Fonts

Black Ops One, Courier Prime, Lalezar (SIL Open Font License) and Special Elite (Apache 2.0), from Google Fonts. Licenses are in `fonts/`.
