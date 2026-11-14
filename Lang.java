/* =====================================================================
 *  Lang.java  -  English / Arabic text for the whole client
 * ---------------------------------------------------------------------
 *  Every text on screen is looked up here with a key:
 *      Lang.t("open_case")          -> "OPEN CASE" or "افتح القضية"
 *      Lang.f("present", 3, 8)      -> "3 / 8 PRESENT" (with numbers)
 *  Lang.rtl() is true in Arabic, so screens can flip right-to-left.
 *  The chosen language is remembered for the next time (Preferences).
 *
 *  The server still sends English (INFO messages, words, categories).
 *  Words, categories and ranks are translated with the maps at the end.
 * ===================================================================== */

import java.awt.ComponentOrientation;
import java.awt.Component;
import java.awt.Container;
import java.util.HashMap;
import java.util.prefs.Preferences;
import javax.swing.JComponent;

public final class Lang {

	public static final String EN = "en", AR = "ar";

	private static final Preferences PREFS = Preferences.userNodeForPackage(Lang.class);
	private static String current = PREFS.get("language", EN);

	private static final HashMap<String, String> en = new HashMap<String, String>();
	private static final HashMap<String, String> ar = new HashMap<String, String>();

	public static String get() { return current; }
	public static boolean rtl() { return current.equals(AR); }

	public static void set(String language) {
		current = language;
		PREFS.put("language", language);
	}

	public static ComponentOrientation orientation() {
		return rtl() ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
	}

	// text for a key in the current language
	public static String t(String key) {
		String text = (rtl() ? ar : en).get(key);
		if (text == null) {
			text = en.get(key);
		}
		return text == null ? key : text;
	}

	// text with numbers / names filled in (uses String.format)
	public static String f(String key, Object... values) {
		return String.format(t(key), values);
	}

	// multi-line label text (the lines are separated by \n)
	public static String html(String key, Object... values) {
		String body = f(key, values).replace("\n", "<br>");
		return rtl() ? "<html><div align='right'>" + body + "</div></html>" : "<html>" + body + "</html>";
	}

	// in Arabic, BoxLayout columns should line up on the right
	public static void alignColumn(Container column) {
		for (Component c : column.getComponents()) {
			if (c instanceof JComponent) {
				((JComponent) c).setAlignmentX(rtl() ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
			}
		}
		column.revalidate();
	}

	// ---------- server words -> Arabic ----------
	private static final HashMap<String, String> words = new HashMap<String, String>();

	public static String word(String english) {
		if (!rtl()) {
			return english;
		}
		String w = words.get(english.trim().toLowerCase());
		return w == null ? english : w;
	}

	// a word's Arabic that came from a category file (WORD_AR / RESULT)
	public static void rememberWord(String english, String arabic) {
		if (arabic != null && arabic.trim().length() > 0) {
			words.put(english.trim().toLowerCase(), arabic.trim());
		}
	}

	// ---------- category names ----------
	private static final HashMap<String, String> categories = new HashMap<String, String>();

	// the Arabic name of a category file (from the server's CATEGORIES message)
	public static void rememberCategory(String name, String arabic) {
		if (arabic != null && arabic.trim().length() > 0) {
			categories.put(name, arabic.trim());
		}
	}

	// a category name in the current language
	public static String category(String name) {
		if (name.equals("RANDOM")) {
			return t("random");
		}
		if (!rtl()) {
			return name;
		}
		if (categories.containsKey(name)) {
			return categories.get(name);
		}
		return ar.containsKey(name) ? ar.get(name) : name;
	}

	// the Arabic of a word even in English mode (for the second line on the card)
	public static String arabicWord(String english) {
		String w = words.get(english.trim().toLowerCase());
		return w == null ? "" : w;
	}

	private static void e(String key, String english, String arabic) {
		en.put(key, english);
		ar.put(key, arabic);
	}

	private static void w(String english, String arabic) {
		words.put(english.toLowerCase(), arabic);
	}

	static {
		// ----- sign-in -----
		e("tagline", "Everyone knows the word,\nexcept one.", "الكل يعرف الكلمة،\nإلا واحد.");
		e("signin_header", "AGENT SIGN-IN", "تسجيل الدخول");
		e("codename", "CODENAME", "الاسم الحركي");
		e("server_address", "SERVER ADDRESS", "عنوان السيرفر");
		e("server_hint", "Empty = this computer.", "فاضي = نفس الجهاز.");
		e("report_duty", "REPORT FOR DUTY", "سجّل حضورك");
		e("connecting", "CONNECTING…", "جاري الاتصال…");
		e("err_name", "Enter a codename first.", "اكتب اسمك الحركي أول.");
		e("err_host", "No such host: %s", "ما في جهاز بهالعنوان: %s");
		e("err_connect", "Could not connect. Is the server running?", "ما قدرنا نتصل. السيرفر شغّال؟");
		e("err_taken", "That codename is taken. Pick another one.", "هالاسم مأخوذ، اختار اسم ثاني.");

		// ----- lobby -----
		e("subject", "SUBJECT: THE INTRUDER\nSTATUS: AMONG US", "المشتبه: الدخيل\nالحالة: بيننا");
		e("top_secret", "TOP SECRET", "سري للغاية");
		e("agents_on_file", "AGENTS ON FILE", "العملاء في الملف");
		e("present", "%d / %d", "%d / %d");
		e("min_needed", "Minimum %d agents required.\nWaiting for %d more…", "نحتاج %d عملاء على الأقل.\nننتظر %d بعد…");
		e("ready", "%d agents present.\nAny agent may open the case.", "%d عملاء حاضرين.\nأي عميل يقدر يفتح القضية.");
		e("open_case", "OPEN CASE", "افتح القضية");
		e("agent_on_file", "AGENT ON FILE", "عميل في الملف");
		e("lvl", "LVL %d · %s", "مستوى %d · %s");
		e("you", "YOU", "أنت");
		e("redacted", "[ REDACTED ]", "[ محذوف ]");
		e("classified", "CLASSIFIED", "سري");

		// ----- leaderboard + chat -----
		e("most_wanted", "MOST WANTED", "المطلوبين");
		e("no_records", "No records yet.", "ما في سجلات للحين.");
		e("intercepted", "INTERCEPTED", "رسائل ملتقطة");
		e("last_chance", "LAST CHANCE", "الفرصة الأخيرة");
		e("msg_hint", "Transmit message…", "اكتب رسالة…");
		e("guess_hint", "Type your guess for the secret word…", "اكتب تخمينك للكلمة السرية…");
		e("send", "SEND", "إرسال");
		e("guess", "GUESS", "خمّن");
		e("secure_line", "Secure line open: %s port %d", "خط آمن مفتوح: %s منفذ %d");
		e("you_accused", "You accused %s", "اتهمت %s");
		e("case_closed_alert", "Case closed", "انقفلت القضية");
		e("conn_lost", "Connection lost. Disconnected from the server.", "انقطع الاتصال بالسيرفر.");
		e("promoted_alert", "Promoted! You are now a %s", "ترقية! صرت %s");
		e("levelup_alert", "Level up! Clearance %d", "مستوى جديد! تصريح %d");

		// ----- HUD -----
		e("evidence", "", "");
		e("ph_interrogation", "INTERROGATION", "استجواب");
		e("ph_accusation", "ACCUSATION", "اتهام");
		e("ph_last_chance", "LAST CHANCE", "فرصة أخيرة");
		e("ph_verdict", "VERDICT", "الحكم");
		e("ph_disconnected", "DISCONNECTED", "انقطع الاتصال");
		e("sfx_on", "SFX ON", "الصوت: شغّال");
		e("sfx_off", "SFX OFF", "الصوت: طافي");
		e("lang_button", "عربي", "EN");
		e("agent", "AGENT %s", "العميل %s");
		e("clearance_rank", "LVL %d · %s", "مستوى %d · %s");
		e("xp_of", "%d / 100 XP", "%d / 100 نقطة");

		// ----- folder -----
		e("tab_file", "CASE FILE Nº 458", "ملف القضية ٤٥٨");
		e("tab_active", "CASE Nº 458 · ACTIVE", "القضية ٤٥٨ · نشطة");
		e("tab_accusation", "CASE Nº 458 · ACCUSATION", "القضية ٤٥٨ · اتهام");
		e("tab_verdict", "CASE Nº 458 · VERDICT", "القضية ٤٥٨ · الحكم");
		e("cover_title", "CASE Nº 458", "قضية ٤٥٨");

		// ----- round -----
		e("case_status", "CASE STATUS", "حالة القضية");
		e("st_interrogation", "Ask questions. Don't say the word.", "اسألوا، ولا تقولون الكلمة.");
		e("st_progress", "Case in progress.", "القضية شغّالة.");
		e("st_observer", "Watching. You play next round.", "تتفرج. تلعب الجولة الجاية.");
		e("st_caught_you", "Caught! Guess the word to win.", "انمسكت! خمّن الكلمة وتفوز.");
		e("st_guessing", "Al-Dakheel gets one last guess…", "الدخيل عنده تخمين أخير…");
		e("st_guess_sent", "Guess sent…", "انرسل التخمين…");
		e("persons_of_interest", "SUSPECTS", "المشتبهين");
		e("thats_you", "YOU", "أنت");
		e("under_watch", "UNDER WATCH", "تحت المراقبة");
		e("exhibit_a", "EXHIBIT A", "الدليل أ");

		// ----- secret card -----
		e("hdr_confidential", "TOP SECRET", "سري للغاية");
		e("hdr_classified", "TOP SECRET", "سري للغاية");
		e("hdr_observer", "OBSERVER", "مراقب");
		e("lbl_word", "YOUR SECRET WORD:", "كلمتك السرية:");
		e("lbl_role", "YOUR ROLE:", "دورك:");
		e("lbl_last", "LAST CHANCE:", "الفرصة الأخيرة:");
		e("lbl_status", "STATUS:", "الحالة:");
		e("big_outsider", "YOU ARE AL-DAKHEEL", "أنت الدخيل");
		e("big_guess", "GUESS THE WORD", "خمّن الكلمة");
		e("big_observing", "OBSERVING", "تراقب");
		e("hint_word", "Never say it.", "لا تقولها أبداً.");
		e("hint_outsider", "Blend in. Find the word.", "اندمج واكتشف الكلمة.");
		e("hint_guess", "Type it in the chat box.", "اكتبها في خانة الرسائل.");
		e("hint_observer", "You play next round.", "تلعب الجولة الجاية.");
		e("awaiting", "AWAITING ASSIGNMENT", "بانتظار التكليف");
		e("sealed", "SEALED ORDERS", "أوامر مختومة");

		// ----- vote -----
		e("vote_title", "WHO IS AL-DAKHEEL?", "مين الدخيل؟");
		e("vote_title_other", "مين الدخيل؟", "WHO IS AL-DAKHEEL?");
		e("vote_sub", "Pick one. No take-backs.", "اختار واحد. ما في رجعة.");
		e("vote_observer", "Only players in this round vote.", "بس اللي في الجولة يصوتون.");
		e("accuse", "ACCUSE", "اتهم");
		e("marked", "MARKED", "متهم");
		e("suspect_no", "SUSPECT Nº %s", "مشتبه رقم %s");
		e("accusations_filed", "VOTES", "الأصوات");
		e("file_accusation", "CONFIRM", "تأكيد");
		e("accusation_filed", "SENT", "انرسل");

		// ----- result -----
		e("case_report", "CASE REPORT", "تقرير القضية");
		e("rep_outsider", "AL-DAKHEEL:", "الدخيل:");
		e("rep_word", "THE WORD:", "الكلمة:");
		e("rep_winner", "WINNER:", "الفايز:");
		e("winner_players", "The players", "اللاعبين");
		e("winner_outsider", "Al-Dakheel (%s)", "الدخيل (%s)");
		e("new_case", "NEW CASE", "قضية جديدة");
		e("lobby", "LOBBY", "اللوبي");
		e("v_case_closed", "CASE CLOSED", "انقفلت القضية");
		e("v_case_failed", "CASE FAILED", "فشلت القضية");
		e("v_escaped", "ESCAPED", "هرب");
		e("v_caught", "CAUGHT", "انمسك");
		e("v_case_filed", "CASE FILED", "تم الحفظ");
		e("v_case_report", "CASE REPORT", "تقرير");
		e("agent_report", "%s", "%s");
		e("mission_won", "MISSION WON", "فزت بالمهمة");
		e("mission_lost", "MISSION LOST", "خسرت المهمة");
		e("awaiting_report", "AWAITING REPORT", "بانتظار التقرير");
		e("clearance", "CLEARANCE %d", "تصريح %d");
		e("stamp_level_up", "LEVEL UP: CLEARANCE %d", "مستوى جديد: تصريح %d");
		e("stamp_promoted", "PROMOTED: %s", "ترقية: %s");
		e("al_dakheel", "AL-DAKHEEL", "الدخيل");

		// ----- categories in the lobby -----
		e("evidence_file", "CHOOSE THE EVIDENCE FILE", "اختاروا ملف القضية");
		e("random", "RANDOM", "عشوائي");
		e("picked_by", "%s picked the file: %s", "%s اختار الملف: %s");

		// ----- host + file cabinet (lobby) -----
		e("choose_file", "Choose a file", "اختار ملف");
		e("is_choosing", "%s is choosing…", "%s يختار…");
		e("the_host", "The host", "المضيف");
		e("host_tag", "HOST", "المضيف");
		e("need_more", "Need %d more agent(s)…", "نحتاج %d بعد…");
		e("waiting_for", "Waiting for %s…", "ننتظر %s…");
		e("chosen", "CHOSEN", "تم الاختيار");
		e("drawer_label", "CASE FILES", "ملفات القضايا");
		e("you_are_host", "You are the host", "أنت المضيف");
		e("host_is", "Host: %s", "المضيف: %s");
		e("change_file", "CHANGE FILE", "غيّر الملف");
		e("close", "CLOSE", "إغلاق");

		// ----- voice -----
		e("talk", "HOLD TO TALK", "اضغط وتكلّم");
		e("on_air", "ON AIR…", "على الهواء…");
		e("talking", "%s TALKING…", "%s يتكلم…");
		e("no_mic", "No microphone found (or no permission to use it).", "ما لقينا مايك (أو ما في إذن تستخدمه).");
		e("no_speaker", "No speakers found: you can't hear voice.", "ما لقينا سماعات: ما تقدر تسمع الصوت.");
		e("voice_failed", "Voice chat is not available.", "المحادثة الصوتية مو متوفرة.");

		// ----- host screen -----
		e("open_host", "OPEN AS HOST SCREEN", "افتح شاشة المضيف");
		e("host_hint", "For a TV screen.", "لشاشة التلفزيون.");
		e("tab_host", "CASE Nº 458 · HOST SCREEN", "القضية ٤٥٨ · شاشة المضيف");
		e("join_case", "JOIN THE CASE", "انضموا للقضية");
		e("join_how", "Join at:", "ادخلوا على:");
		e("host_waiting", "Waiting for agents…", "ننتظر العملاء…");
		e("host_file", "EVIDENCE FILE: %s", "ملف القضية: %s");
		e("host_round", "ASK YOUR QUESTIONS", "اسألوا بعض");
		e("host_order", "QUESTION ORDER", "ترتيب الأسئلة");
		e("host_asks", "%s asks %s", "%s يسأل %s");
		e("voted", "VOTED", "صوّت");
		e("host_vote_count", "%d / %d VOTED", "%d / %d صوّتوا");
		e("host_caught", "%s WAS CAUGHT!", "انمسك %s!");
		e("host_last", "One last chance to guess the word…", "عنده فرصة أخيرة يخمّن الكلمة…");
		e("host_was", "WAS AL-DAKHEEL", "كان الدخيل");
		e("host_word", "THE WORD WAS", "الكلمة كانت");
		e("host_agents_win", "THE AGENTS WIN", "فاز العملاء");
		e("host_outsider_wins", "AL-DAKHEEL WINS", "فاز الدخيل");

		// ----- ranks + categories from the server -----
		e("Rookie", "Rookie", "مبتدئ");
		e("Detective", "Detective", "محقق");
		e("Mastermind", "Mastermind", "عقل مدبر");
		e("Legend", "Legend", "أسطورة");
		e("Food", "Food", "أكل");
		e("Place", "Place", "مكان");
		e("Job", "Job", "وظيفة");
		e("Animal", "Animal", "حيوان");
		e("Activity", "Activity", "نشاط");
		e("Event", "Event", "مناسبة");

		// ----- the server's word bank -----
		w("Shawarma", "شاورما");       w("Machboos", "مجبوس");      w("Falafel", "فلافل");
		w("Kunafa", "كنافة");          w("Pizza", "بيتزا");          w("Karak Tea", "شاي كرك");
		w("Kuwait Towers", "أبراج الكويت"); w("Airport", "مطار");   w("Hospital", "مستشفى");
		w("University", "جامعة");      w("Cinema", "سينما");         w("Beach", "بحر");
		w("Doctor", "دكتور");          w("Pilot", "طيار");           w("Teacher", "مدرس");
		w("Chef", "طباخ");             w("Firefighter", "إطفائي");   w("Engineer", "مهندس");
		w("Camel", "بعير");            w("Falcon", "صقر");           w("Cat", "قطوة");
		w("Shark", "قرش");             w("Horse", "حصان");           w("Penguin", "بطريق");
		w("Football", "كورة");         w("Swimming", "سباحة");       w("Chess", "شطرنج");
		w("Bowling", "بولينغ");        w("Diwaniya", "ديوانية");     w("Wedding", "عرس");
	}

	private Lang() { }
}
