/* =====================================================================
 *  Lang.java  -  every text of the game in English and Arabic (bonus)
 * ---------------------------------------------------------------------
 *  Lang.t("send")          -> "Send" or "أرسل"
 *  Lang.f("is_talking", x) -> "Nouri is talking" ({0} is replaced)
 *  The language button in the taskbar switches between the two.
 *  The server messages (RESULT, INFO) stay in English, like the brief.
 * ===================================================================== */

import java.util.HashMap;

public class Lang {

	public static final int EN = 0, AR = 1;
	private static int current = EN;

	// key -> { English, Arabic }
	private static final HashMap<String, String[]> texts = new HashMap<String, String[]>();
	// Arabic names of the topics and words (sent by the server)
	private static final HashMap<String, String> arabicNames = new HashMap<String, String>();

	private static void put(String key, String english, String arabic) {
		texts.put(key, new String[] { english, arabic });
	}

	static {
		// ---- general ----
		put("game", "Brra Al-Salfa", "برا السالفة");
		put("tagline", "Everyone knows the word. Except one.", "الكل يعرف السالفة إلا واحد.");
		put("m_file", "File", "ملف");
		put("m_edit", "Edit", "تحرير");
		put("m_view", "View", "عرض");
		put("m_help", "Help", "مساعدة");
		put("m_game", "Game", "اللعبة");
		put("m_players", "Players", "اللاعبين");
		put("ok", "OK", "تمام");
		put("cancel", "Cancel", "إلغاء");
		put("back", "Back", "رجوع");
		put("next", "Next", "التالي");
		// ---- desktop icons ----
		put("i_help", "How to play", "طريقة اللعب");
		put("i_tv", "TV screen", "شاشة التلفزيون");
		put("i_rules", "Rules", "القوانين");
		put("i_scores", "Scores", "النقاط");
		// ---- sign in ----
		put("sign_in", "Sign in", "تسجيل الدخول");
		put("your_name", "Your name", "اسمك");
		put("server", "Server", "السيرفر");
		put("server_hint", "empty = this computer", "فاضي = هذا الجهاز");
		put("who_online", "Who is online", "مين موجود");
		put("in_lobby", "in the lobby", "في الانتظار");
		put("online_count", "{0} of 8 players. Room for {1} more.", "{0} من 8 لاعبين. باقي {1} أماكن.");
		put("online_none", "Nobody is here yet. Be the first!", "ما في أحد للحين. كن الأول!");
		put("online_off", "Looking for the server...", "ندور على السيرفر...");
		put("tip", "Tip", "نصيحة");
		put("tip_text", "Ask questions that only someone who knows the word can answer.", "اسأل أسئلة ما يجاوبها إلا اللي يعرف السالفة.");
		put("err_name", "Please type your name first.", "اكتب اسمك أول.");
		put("err_title", "Connection failed", "فشل الاتصال");
		put("err_connect", "Could not connect to {0} on port 1202.", "ما قدرنا نتصل بـ {0} على المنفذ 1202.");
		put("err_connect2", "Make sure the server is running and that you are on the same Wi-Fi.", "تأكد إن السيرفر شغال وإنكم على نفس الواي فاي.");
		put("err_taken", "The name {0} is already taken.", "الاسم {0} مستخدم.");
		put("err_taken2", "Please choose another name.", "اختر اسم ثاني.");
		put("err_lost", "The connection to the server was lost.", "انقطع الاتصال بالسيرفر.");
		put("err_full", "The game is full (8 players).", "اللعبة مليانة (8 لاعبين).");
		put("err_kicked", "The server removed you from the game.", "السيرفر طلعك من اللعبة.");
		// ---- character ----
		put("choose_char", "Choose your character", "اختر شخصيتك");
		put("your_char", "Your character", "شخصيتك");
		put("pick_char", "Pick a character", "اختر شخصية");
		put("colors", "Colors", "الألوان");
		put("shirt", "Shirt", "القميص");
		put("hair", "Hair", "الشعر");
		put("background", "Background", "الخلفية");
		put("preview", "Preview", "معاينة");
		put("preview_text", "This is how you'll look in chat.", "هكذا راح تبين في الشات.");
		put("player_n", "Player {0} of 8", "اللاعب {0} من 8");
		put("taken_hidden", "Taken by others are hidden.", "المأخوذة مخفية.");
		put("join_lobby", "Join lobby", "ادخل اللعبة");
		put("char_hint", "Click a character, then pick colors.", "اضغط على شخصية واختر الألوان.");
		// ---- lobby ----
		put("lobby", "Game Lobby", "غرفة الانتظار");
		put("pick_topic", "Pick a topic", "اختر الموضوع");
		put("host_tasks", "Host tasks", "مهام المضيف");
		put("start_round", "Start round", "ابدأ الجولة");
		put("players_count", "Players ({0} of 8)", "اللاعبين ({0} من 8)");
		put("empty", "empty", "فاضي");
		put("host_hint", "You are the host. Pick a topic on the left, then press Start round.", "أنت المضيف. اختر موضوع من اليمين واضغط ابدأ الجولة.");
		put("guest_hint", "{0} is the host. Waiting for {0} to start the round.", "{0} هو المضيف. ننتظر {0} يبدأ الجولة.");
		put("topic_is", "Topic: {0}", "الموضوع: {0}");
		put("random", "Random", "عشوائي");
		put("need_players", "You need at least 3 players.", "لازم 3 لاعبين على الأقل.");
		// ---- round ----
		put("secret_word", "Your secret word", "كلمتك السرية");
		put("dont_say", "Don't say it. Ask questions and find who doesn't know it.", "لا تقولها. اسأل واكتشف مين ما يعرفها.");
		put("you_are", "You are", "أنت");
		put("outsider_text", "Everyone else knows the word. You don't.", "الكل يعرف الكلمة إلا أنت.");
		put("outsider_text2", "Listen carefully, blend in, and don't get caught.", "اسمع زين، اندمج معهم، ولا ينكشف أمرك.");
		put("watching", "You joined during a round. You are watching.", "دخلت في نص الجولة. أنت تتفرج.");
		put("suspects", "Suspects", "المشتبه فيهم");
		put("you", "you", "أنت");
		put("asking_now", "asking now", "يسأل الحين");
		put("time_left", "Time left", "الوقت المتبقي");
		put("round_of", "Round {0} of {1}", "الجولة {0} من {1}");
		put("remaining", "{0} remaining", "باقي {0}");
		put("order", "Order: {0}", "الترتيب: {0}");
		// ---- chat ----
		put("chat", "Chat", "الشات");
		put("type_msg", "Type a message...", "اكتب رسالة...");
		put("send", "Send", "أرسل");
		put("hold_talk", "Hold to talk", "اضغط وتكلم");
		put("says", "says:", "يقول:");
		put("is_talking", "{0} is talking", "{0} يتكلم");
		put("you_talking", "You are talking...", "أنت تتكلم...");
		put("voice_failed", "Voice chat is off (the server voice port is closed).", "الصوت مطفي (منفذ الصوت مسكر).");
		put("no_mic", "No microphone found.", "ما لقينا مايكروفون.");
		put("no_speaker", "No speaker found.", "ما لقينا سماعة.");
		put("blocked", "Careful! You cannot say the secret word.", "انتبه! ما تقدر تقول الكلمة السرية.");
		// ---- vote ----
		put("vote", "Vote", "التصويت");
		put("who_is", "Who is", "مين");
		put("pick_one", "Pick one. You can't change it later.", "اختر واحد. ما تقدر تغير بعدين.");
		put("or_list", "or pick from the list:", "أو اختر من القائمة:");
		put("votes_of", "Votes {0} of {1}", "صوت {0} من {1}");
		put("confirm_vote", "Confirm vote", "أكد التصويت");
		put("voted_wait", "Your vote is in. Waiting for the others...", "تم تصويتك. ننتظر الباقين...");
		put("not_playing", "You are watching this round.", "أنت تتفرج على هذه الجولة.");
		put("x_voted", "{0} voted", "{0} صوّت");
		put("left_to_vote", "{0} players left to vote.", "باقي {0} لاعبين.");
		put("left_to_vote1", "1 player left to vote.", "باقي لاعب واحد.");
		put("all_voted", "Everyone has voted!", "الكل صوّت!");
		// ---- last guess ----
		put("last_chance", "Last chance", "الفرصة الأخيرة");
		put("caught_you", "They caught you!", "انكشفت!");
		put("guess_text", "Guess the secret word. If you're right, you still get a point.", "خمّن الكلمة السرية. لو صح تاخذ نقطة.");
		put("submit_guess", "Submit guess", "أرسل التخمين");
		put("x_caught", "{0} was caught!", "{0} انكشف!");
		put("x_guessing", "{0} is choosing a word...", "{0} يختار كلمة...");
		put("everyone_watching", "Everyone is watching", "الكل يتابع");
		put("time", "Time", "الوقت");
		// ---- result ----
		put("round_result", "Round {0} result", "نتيجة الجولة {0}");
		put("x_was", "{0} was", "{0} كان");
		put("players_win", "Caught! Players win this round.", "انمسك! اللاعبين فازوا.");
		put("outsider_wins", "Not caught! Brra Al-Salfa wins.", "ما انمسك! برا السالفة فاز.");
		put("outsider_steals", "Caught, but guessed the word!", "انمسك، بس عرف الكلمة!");
		put("the_word_was", "The word was", "الكلمة كانت");
		put("last_guess_of", "{0}'s last guess", "تخمين {0} الأخير");
		put("no_guess", "No last guess", "ما في تخمين");
		put("right", "Right! +1 point.", "صح! +1 نقطة.");
		put("wrong", "Wrong, no bonus point.", "غلط، ما في نقطة.");
		put("voted_for", "voted {0}", "صوّت لـ {0}");
		put("no_vote", "did not vote", "ما صوّت");
		put("see_scores", "See scores", "شوف النقاط");
		put("next_round", "Next round", "الجولة الجاية");
		put("final_scores", "Final scores", "النتيجة النهائية");
		put("wait_host_next", "Waiting for {0} to start round {1}.", "ننتظر {0} يبدأ الجولة {1}.");
		put("game_over_b", "That was the last round!", "هذي كانت آخر جولة!");
		put("see_final", "See the final scores.", "شوف النتيجة النهائية.");
		// ---- scores ----
		put("scores", "Scores", "النقاط");
		put("x_wins", "{0} wins!", "{0} فاز!");
		put("rounds_played", "{0} rounds played", "{0} جولات");
		put("col_player", "Player", "اللاعب");
		put("col_caught", "Caught", "كشف");
		put("col_outsider", "Was outsider", "كان برا");
		put("col_points", "Points", "النقاط");
		put("exit", "Exit", "خروج");
		put("play_again", "Play again", "العب مرة ثانية");
		put("to_lobby", "Back to lobby", "رجوع للانتظار");
		put("round_history", "Round history", "سجل الجولات");
		put("round_n", "Round {0}", "الجولة {0}");
		put("was_outsider", "{0} was the outsider", "{0} كان برا السالفة");
		put("caught", "caught", "انمسك");
		put("escaped", "escaped", "هرب");
		put("no_rounds", "No rounds yet.", "ما في جولات للحين.");
		// ---- help ----
		put("contents", "Contents", "المحتويات");
		put("the_goal", "The goal", "الهدف");
		put("h_topics", "Topics", "المواضيع");
		put("h_asking", "Asking", "الأسئلة");
		put("h_voting", "Voting", "التصويت");
		put("h_last", "Last guess", "التخمين الأخير");
		put("h_points", "Points", "النقاط");
		put("h_players", "3 to 8 players. One computer can be the TV screen.", "من 3 إلى 8 لاعبين. جهاز واحد يصير شاشة التلفزيون.");
		put("step1", "Everyone gets the same secret word, except one player.", "الكل ياخذ نفس الكلمة السرية إلا لاعب واحد.");
		put("step1b", "That player is Brra Al-Salfa, the outsider.", "هذا اللاعب هو برا السالفة.");
		put("step2", "The host picks a topic.", "المضيف يختار الموضوع.");
		put("step2b", "The word always comes from that topic, like Food or Places.", "الكلمة دايماً من هذا الموضوع، مثل أكل أو أماكن.");
		put("step3", "Take turns asking questions.", "كل واحد يسأل بدوره.");
		put("step3b", "Answer so friends know you know, without saying the word.", "جاوب بطريقة تبين إنك تعرف، بدون ما تقول الكلمة.");
		put("step4", "Vote for who you think is the outsider.", "صوّت للي تظن إنه برا السالفة.");
		put("step4b", "If caught, the outsider gets one last guess at the word.", "لو انمسك، ياخذ فرصة أخيرة يخمن الكلمة.");
		put("points", "Points", "النقاط");
		put("p_catch", "Vote for the outsider", "صوّت لبرا السالفة");
		put("p_escape", "Outsider not caught", "برا السالفة ما انمسك");
		put("p_guess", "Outsider guesses word", "برا السالفة عرف الكلمة");
		put("each", "+1 each", "+1 لكل واحد");
		put("t1", "The host picks one of the topics.", "المضيف يختار موضوع.");
		put("t1b", "Food, Places, Jobs, Animals, Sports, Events, or Random.", "أكل، أماكن، وظايف، حيوانات، رياضة، مناسبات، أو عشوائي.");
		put("t2", "Everyone sees the topic, even the outsider.", "الكل يشوف الموضوع، حتى برا السالفة.");
		put("t2b", "So the outsider can still try to blend in.", "عشان يقدر يحاول يندمج معهم.");
		put("a1", "Players ask in turn.", "اللاعبين يسألون بالدور.");
		put("a1b", "The Suspects window shows who is asking now.", "نافذة المشتبه فيهم تبين مين يسأل الحين.");
		put("a2", "Type in the chat, or hold to talk.", "اكتب في الشات أو اضغط وتكلم.");
		put("a2b", "Never say the secret word: the chat blocks it.", "لا تقول الكلمة السرية: الشات يمنعها.");
		put("v1", "When the time is up, everyone votes.", "لما يخلص الوقت، الكل يصوت.");
		put("v1b", "Click a face, or pick a name from the list.", "اضغط على وجه أو اختر اسم من القائمة.");
		put("v2", "The player with the most votes is caught.", "اللي ياخذ أكثر أصوات ينمسك.");
		put("v2b", "A tie means the outsider escapes.", "لو تعادل، برا السالفة يهرب.");
		put("l1", "A caught outsider gets one last chance.", "برا السالفة لو انمسك ياخذ فرصة أخيرة.");
		put("l1b", "Pick the word from 8 choices in 20 seconds.", "يختار الكلمة من 8 خيارات في 20 ثانية.");
		put("l2", "A right guess still gives +1 point.", "التخمين الصح ياخذ +1 نقطة.");
		put("l2b", "Everybody sees the guess on the result.", "الكل يشوف التخمين في النتيجة.");
		put("pp1", "+1 to every player who votes for the outsider.", "+1 لكل لاعب يصوت لبرا السالفة.");
		put("pp1b", "Look at the Caught column in the scores.", "شوف عمود كشف في النقاط.");
		put("pp2", "+2 to the outsider if not caught, +1 for the word.", "+2 لبرا السالفة لو ما انمسك، +1 للكلمة.");
		put("pp2b", "After 3 rounds, the most points wins.", "بعد 3 جولات، صاحب أكثر نقاط يفوز.");
		put("scores_after", "After round {0}", "بعد الجولة {0}");
		put("tv_vote_count", "{0} of {1} have voted", "{0} من {1} صوّتوا");
		put("tv_guess", "{0} has one last guess...", "{0} عنده تخمين أخير...");
		put("tv_lobby", "{0} players in the lobby", "{0} لاعبين في الانتظار");
		put("tv_caught", "{0} was Brra Al-Salfa!", "{0} كان برا السالفة!");
		put("close", "Close", "إغلاق");
		// ---- TV ----
		put("tv_title", "TV screen  -  Brra Al-Salfa", "شاشة التلفزيون  -  برا السالفة");
		put("tv_join", "Join: run SpyClient and type {0}", "للدخول: شغل SpyClient واكتب {0}");
		put("tv_waiting", "Waiting for the host to start...", "ننتظر المضيف يبدأ...");
		put("tv_vote", "Vote now on your laptop!", "صوّت الحين من جهازك!");
		put("tv_asking", "{0} is asking {1}...", "{0} يسأل {1}...");
		put("tv_q_title", "Use this computer as the TV screen?", "نخلي هذا الجهاز شاشة التلفزيون؟");
		put("tv_q1", "The TV screen is the big shared screen for the room (a projector or a TV). It shows the round, the timer, who is asking and the votes, but never the secret word.",
				"شاشة التلفزيون هي الشاشة الكبيرة اللي يشوفها الكل (بروجكتر أو تلفزيون). تعرض الجولة والوقت ومنو يسأل والتصويت، بس ما تعرض الكلمة السرية أبداً.");
		put("tv_q2", "Players join from their own laptops. Close the TV window (X) to come back here.",
				"اللاعبين يدخلون من أجهزتهم. سكّر نافذة التلفزيون (X) عشان ترجع هنا.");
		put("tv_go", "Open TV screen", "افتح شاشة التلفزيون");
		put("tv_busy", "You are playing on this computer. The TV screen runs on another computer (sign in there and click TV screen).",
				"أنت تلعب على هذا الجهاز. شاشة التلفزيون تشتغل على جهاز ثاني (افتح اللعبة هناك واضغط شاشة التلفزيون).");
		put("tv_no_word", "No secret words are shown on this screen.", "الكلمات السرية ما تطلع على هذه الشاشة.");
		put("listening", "listening", "يسمع");
		put("answering", "answering", "يجاوب");
		put("topic", "Topic", "الموضوع");
	}

	// the text for a key in the current language
	public static String t(String key) {
		String[] pair = texts.get(key);
		if (pair == null) {
			return key;                 // a missing key shows itself (easy to spot)
		}
		return pair[current];
	}

	// the text with {0}, {1} replaced by the values
	public static String f(String key, Object... values) {
		String text = t(key);
		for (int i = 0; i < values.length; i++) {
			text = text.replace("{" + i + "}", String.valueOf(values[i]));
		}
		return text;
	}

	public static void set(int language) {
		current = language;
	}

	public static boolean arabic() {
		return current == AR;
	}

	// remembers the Arabic name of a topic or a word (from the server)
	public static void remember(String english, String arabic) {
		if (arabic != null && arabic.trim().length() > 0) {
			arabicNames.put(english, arabic.trim());
		}
	}

	// a topic / word in the current language
	public static String name(String english) {
		if (current == AR && arabicNames.containsKey(english)) {
			return arabicNames.get(english);
		}
		return english;
	}

	// the Arabic of a word (or "" if we don't know it)
	public static String arabicOf(String english) {
		String ar = arabicNames.get(english);
		return ar == null ? "" : ar;
	}
	// the server's INFO notes are in English; in Arabic we translate the
	// ones we know (the pattern) and put the names / numbers back in
	private static final String[][] INFO = {
		{ "The server was stopped\\.", "السيرفر توقف." },
		{ "(.+) joined the game!", "{0} دخل اللعبة!" },
		{ "A round is in progress\\. You will play in the next round\\.", "في جولة شغالة الحين. بتلعب في الجولة الجاية." },
		{ "(.+) has left the game\\.", "{0} طلع من اللعبة." },
		{ "Only the host can start the round\\.", "بس المضيف يقدر يبدأ الجولة." },
		{ "You need at least (\\d+) players to start\\.", "لازم {0} لاعبين على الأقل عشان نبدأ." },
		{ "Round (\\d+) started! Topic: (.+)\\.", "بدأت الجولة {0}! الموضوع: {1}." },
		{ "Time is up! Vote for the player you think is Brra Al-Salfa\\.", "خلص الوقت! صوّت للي تحسبه برا السالفة." },
		{ "The server ended the round\\.", "السيرفر أنهى الجولة." },
		{ "The outsider \\((.+)\\) left, so the round was cancelled\\. The word was: (.+)", "برا السالفة ({0}) طلع، فانلغت الجولة. الكلمة كانت: {1}" },
		{ "Not enough players left, so the round was cancelled\\.", "ما بقى لاعبين كفاية، فانلغت الجولة." },
		{ "Welcome back, (.+)! (\\d+) games, (\\d+) wins so far\\.", "هلا {0} نورت مرة ثانية! لعبت {1} ألعاب وفزت {2} مرات لين الحين." },
	};

	public static String info(String english) {
		if (current != AR) {
			return english;
		}
		for (int i = 0; i < INFO.length; i++) {
			java.util.regex.Matcher m = java.util.regex.Pattern.compile(INFO[i][0]).matcher(english);
			if (m.matches()) {
				String text = INFO[i][1];
				for (int g = 1; g <= m.groupCount(); g++) {
					String part = name(m.group(g));                       // topics / words in Arabic too
					text = text.replace("{" + (g - 1) + "}", Xp.keepLtr(part));
				}
				return text;
			}
		}
		return english;                      // a note we don't know: stays English
	}
}
