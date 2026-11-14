/* =====================================================================
 *  VoiceChat.java  -  push-to-talk voice over UDP
 * ---------------------------------------------------------------------
 *  While the player holds the HOLD TO TALK button, a thread reads the
 *  microphone (javax.sound TargetDataLine) and sends small packets
 *  (20 ms of sound each) to the server's VoiceRelay on UDP port 1203.
 *  The server sends them to everyone else, and a receiving thread here
 *  plays them on the speakers (SourceDataLine).
 *
 *  Why UDP (DatagramSocket) and not TCP? For live voice a late packet is
 *  useless. UDP never waits to resend lost packets, so the voice stays
 *  live; a lost packet is just a tiny gap in the sound.
 *
 *  packet = [type byte][name (writeUTF)][audio bytes]
 *    HELLO (1)  register our address with our name
 *    AUDIO (2)  sound
 *    STOP  (3)  we let go of the button
 *  Sound format: 16000 samples/second, 16-bit, mono (32 KB per second).
 * ===================================================================== */

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.sound.sampled.*;
import javax.swing.SwingUtilities;

public class VoiceChat {

	public static final int VOICE_PORT = 1203;     // same as SpyServer.VOICE_PORT
	private static final byte HELLO = 1, AUDIO = 2, STOP = 3;
	private static final AudioFormat FORMAT = new AudioFormat(16000f, 16, 1, true, false);
	private static final int CHUNK = 640;          // 20 ms of sound = 640 bytes

	// tells the window who is talking, and about problems
	public interface Listener {
		void talking(String name, boolean on);
		void problem(String langKey);
	}

	private final Listener listener;
	private DatagramSocket socket;
	private InetAddress server;
	private String myName = "";
	private volatile boolean sending = false;
	private volatile boolean running = false;
	private SourceDataLine speaker = null;
	private boolean speakerProblemShown = false;
	private final HashMap<String, Long> lastHeard = new HashMap<String, Long>();
	private javax.swing.Timer housekeeping;

	public VoiceChat(Listener listener) {
		this.listener = listener;
	}

	// connects to the server's voice relay (call after joining the game)
	public void start(String host, String name) {
		try {
			myName = name;
			server = InetAddress.getByName(host);
			socket = new DatagramSocket();     // any free port on this computer
			running = true;
			for (int i = 0; i < 3; i++) {
				sendPacket(HELLO, new byte[0], 0);   // UDP can lose packets, so say hello 3 times
			}

			Thread receiver = new Thread(new Runnable() {
				public void run() {
					receiveLoop();
				}
			});
			receiver.setDaemon(true);
			receiver.start();

			// every 300 ms: mark people silent if we stopped hearing them,
			// and every ~5 s say HELLO again (in case it was lost)
			final int[] ticks = { 0 };
			housekeeping = new javax.swing.Timer(300, new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					checkSilence();
					ticks[0]++;
					if (ticks[0] % 16 == 0) {
						sendPacket(HELLO, new byte[0], 0);
					}
				}
			});
			housekeeping.start();
		} catch (IOException ioEx) {
			listener.problem("voice_failed");
		}
	}

	public void stop() {
		running = false;
		sending = false;
		if (housekeeping != null) {
			housekeeping.stop();
		}
		if (socket != null) {
			socket.close();
		}
		if (speaker != null) {
			speaker.close();
			speaker = null;
		}
	}

	// ---------------- talking (button pressed) ----------------
	public boolean startTalking() {
		if (!running || sending) {
			return false;
		}
		final TargetDataLine mic;
		try {
			mic = AudioSystem.getTargetDataLine(FORMAT);
			mic.open(FORMAT, CHUNK * 4);
			mic.start();
		} catch (Exception ex) {
			listener.problem("no_mic");
			return false;
		}
		sending = true;
		Thread micThread = new Thread(new Runnable() {
			public void run() {
				byte[] buffer = new byte[CHUNK];
				while (sending) {
					int n = mic.read(buffer, 0, buffer.length);   // waits for 20 ms of sound
					if (n > 0) {
						sendPacket(AUDIO, buffer, n);
					}
				}
				mic.stop();
				mic.close();
				sendPacket(STOP, new byte[0], 0);
			}
		});
		micThread.setDaemon(true);
		micThread.start();
		return true;
	}

	public void stopTalking() {
		sending = false;     // the mic thread sends STOP and closes the mic
	}

	private void sendPacket(byte type, byte[] audio, int length) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeByte(type);
			out.writeUTF(myName);
			out.write(audio, 0, length);
			byte[] data = bytes.toByteArray();
			socket.send(new DatagramPacket(data, data.length, server, VOICE_PORT));
		} catch (IOException ioEx) {
			// one lost packet doesn't matter
		}
	}

	// ---------------- listening ----------------
	private void receiveLoop() {
		byte[] buffer = new byte[4096];
		while (running) {
			try {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				DataInputStream in = new DataInputStream(
						new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
				byte type = in.readByte();
				String name = in.readUTF();
				if (type == AUDIO) {
					byte[] audio = new byte[in.available()];
					in.readFully(audio);
					heard(name);
					play(audio);
				} else if (type == STOP) {
					silent(name);
				}
			} catch (IOException ioEx) {
				// socket closed (we left) or a broken packet
			}
		}
	}

	private void play(byte[] audio) {
		try {
			if (speaker == null) {
				speaker = AudioSystem.getSourceDataLine(FORMAT);
				speaker.open(FORMAT, CHUNK * 16);
				speaker.start();
			}
			speaker.write(audio, 0, audio.length);
		} catch (Exception ex) {
			if (!speakerProblemShown) {
				speakerProblemShown = true;
				listener.problem("no_speaker");
			}
		}
	}

	// ---------------- who is talking ----------------
	private void heard(final String name) {
		boolean isNew;
		synchronized (lastHeard) {
			isNew = !lastHeard.containsKey(name);
			lastHeard.put(name, System.currentTimeMillis());
		}
		if (isNew) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					listener.talking(name, true);
				}
			});
		}
	}

	private void silent(final String name) {
		boolean was;
		synchronized (lastHeard) {
			was = lastHeard.remove(name) != null;
		}
		if (was) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					listener.talking(name, false);
				}
			});
		}
	}

	// the STOP packet can be lost too: 600 ms without sound = stopped talking
	private void checkSilence() {
		long now = System.currentTimeMillis();
		synchronized (lastHeard) {
			Iterator<Map.Entry<String, Long>> it = lastHeard.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Long> entry = it.next();
				if (now - entry.getValue() > 600) {
					it.remove();
					listener.talking(entry.getKey(), false);   // already on the Swing thread
				}
			}
		}
	}
}
