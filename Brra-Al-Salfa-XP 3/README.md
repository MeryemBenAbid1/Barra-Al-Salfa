# Brra Al-Salfa (برا السالفة): Windows XP edition

CpE 458 Network Programming, Dr. Mohamad Awad.
This is the multiplayer "outsider" word game: a Java Swing client and server using TCP sockets on port 1202, plus UDP on port 1203 for voice. It is built in the Windows XP design from `game-design-xp/`.

## How to run

```
javac -encoding UTF-8 *.java
java SpyServer            (the server; listens on port 1202 right away)
java SpyServer 60         (same, with 60 seconds of questions instead of 120 for a short demo)
java SpyClient            (one per player; 3 to 8 players)
```

Keep the `fonts/`, `categories/` and `images/` folders next to the `.java` files. In the sign-in pop-up, leave **Server** empty to use this computer, or type the server's IP address (for example `192.168.1.24`). To show the game on a projector, run one more `SpyClient` on the computer connected to the projector and click the **TV screen** icon on the desktop. A box first explains what the TV screen is; **Open TV screen** connects it. The TV shows the round, the timer, who is asking and the votes, but never the secret word. Close the TV window (the red X) to go back to sign in.

## Controls

* Every button, icon, topic, face and color works with **one click**.
* **How to play**, **Rules** and **Scores** (desktop icons) and **Help** (the menu of every window) open on top of the game. The red X or **Back** brings back exactly what was open before.
* Only windows you can really close have a red X. There are no fake minimize or maximize buttons.
* Smooth screen changes: windows that overlap always paint in the right order (the chat never draws over the Vote window), the lobby is rebuilt once when someone joins (not 3 times), every window is laid out before it is first drawn, and the pop-ups open without a black flash.
* The **عربي / EN** button in the taskbar switches the language at any moment. In Arabic every window reads right to left, with Changa for headings and IBM Plex Sans Arabic for text (both free fonts, licenses in `fonts/`).

## Files

| File | What it is |
|---|---|
| `SpyServer.java` | Server: accept loop, one `ClientHandler` thread per player, game rules, `GameTimer` thread, `VoiceRelay` (UDP), and files (`players.dat`, `history.txt`, `server_log.txt`) |
| `SpyClient.java` | Client: connects, runs the receiving thread, handles each message by its prefix, and shows the right windows |
| `CmdWindow.java` | The server log, drawn as the XP command prompt with the BRRA AL-SALFA banner |
| `SignInDialog.java`, `XpDialog.java`, `ErrorDialog.java` | The nickname pop-up (a real `JDialog`) and the XP error box |
| `OnlineWindow.java`, `CharacterWindow.java` | Who is online, Tip, Choose your character, Preview |
| `LobbyWindow.java`, `ChatWindow.java` | Game Lobby (topics, Start round) and the Messenger-style chat with Hold to talk |
| `WordWindow.java`, `SuspectsWindow.java`, `TimeWindow.java` | The round: role label / secret word, suspects with "asking now", time bar |
| `VoteWindow.java`, `GuessWindow.java`, `ResultWindow.java`, `ScoresWindow.java` | Vote (cards + `JComboBox`), Last chance, Round result, Scores + Round history |
| `HelpWindow.java`, `TvWindow.java` | How to play + Points, and the TV screen |
| `Xp.java`, `XpWindow.java`, `XpButton.java`, `XpParts.java`, `Desktop.java`, `Avatar.java`, `Lang.java` | The design kit: colors, fonts, pixel art, XP windows and buttons, desktop and taskbar, characters, English/Arabic texts |
| `VoiceChat.java` | Push-to-talk microphone over UDP |

Every file starts with a comment that explains it. The two main files have a section map, so you can search for a tag like `[S7]` or `[C6]` to jump to that part.

## Brief requirements, and where to find them

| Requirement | Where |
|---|---|
| Nickname pop-up | `SignInDialog` (a `JDialog`, the same kind of window `JOptionPane` uses) |
| Nickname is the first line sent | `SpyClient [C4] connectToServer`, `ClientHandler.run` |
| `Vector` of clients, one thread each | `SpyServer [S2]`, `[S5]`, `ClientHandler` |
| Prefix protocol (`CHAT:`, `START_GAME`, `VOTE:`, `PLAYERS:`, `ROLE:`, `RESULT:`) | `ClientHandler.run`, `SpyClient [C6] handleMessage` |
| Start Game button | Lobby: **Start round** sends `START_GAME` |
| Role label | `WordWindow` ("Your secret word" / "You are برا السالفة") |
| Voting drop-down (`JComboBox`) | `VoteWindow` ("or pick from the list") |
| Random word and random outsider | `SpyServer [S7] startGame` |
| Vote count and result broadcast | `SpyServer [S9]`, `[S11]` |

## Bonus features (17)

Character picker (face, shirt, hair and background color), host chooses the topic (6 topic files in `categories/` plus Random), Arabic support with a language switch, the outsider's last guess (8 choices), 3 rounds with points, final scoreboard and round history, timers, turn order with "asking now", voice chat (hold to talk), TV screen, How to play, live vote tracking with "X voted" balloons, who is online before joining, the connection error dialog, the server status window, server controls (start/end round, kick, stop), and the server log saved to a file.

## Points

* +1 to every player who votes for the outsider
* +2 to the outsider if not caught
* +1 to the outsider if caught but they guess the word

After 3 rounds, the player with the most points wins. **Play again** (host) starts a new game from 0.

## Tested

A full game ran end to end on one machine: a real server, 4 player clients and 1 TV client, 3 rounds.
* Round 1: the outsider is caught and makes a wrong guess.
* Round 2: the outsider escapes.
* Round 3: the outsider is caught but guesses the word.

The run also covered the blocked secret word in chat, the connection error, kick, the Arabic switch and the help pages. Screenshots of every screen from that run are in `screenshots/`.

A second test clicks through the whole game, all 3 rounds to Play again, with a robot mouse (real single clicks, like a person), once in English (47 checks) and once in Arabic (48 checks), while recording every frame to look for glitches between screens: sign in, help and rules, the TV question (Cancel and Open), character with all color rows, lobby topics, Help menu, Scores on top of the lobby and of a round, chat Send, Start round, vote, last guess, result, scores Back and X, Next round, the language switch, and closing the TV screen. All checks pass.
