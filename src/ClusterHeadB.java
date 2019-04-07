
//CLUSTER HEAD B
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;
import java.util.HashMap;

public class ClusterHeadB extends Thread {
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xCAAA),
			PropertyHelper.getInt("radio.panid", 0xDABA), PropertyHelper.getInt("radio.panid", 0xDABB),
			PropertyHelper.getInt("radio.panid", 0xCABA) };

	private static int ADDR_NODE1 = node_list[0]; // NODE DIATASNYA
	private static int ADDR_NODE2 = node_list[2]; // NODE DIRINYA

	private static int ADDR_NODE3 = PropertyHelper.getInt("radio.panid", 0xCABA); // NODE DIBAWAHNYA

	private static sensing s = new sensing();
	private static int sn = 1;
	private static long end;
	private static boolean firstSense = false;
	private static boolean exit = false;
	private static boolean isSend = false;

	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();
	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap1 = new HashMap<Integer, String>();
	private static HashMap<Long, Boolean> ack = new HashMap<Long, Boolean>();

	private static int a = 1;
//	private static int SN_A = 0;

	public static void runs() {
		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, ADDR_NODE2, ADDR_NODE2, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);

			receive_send(fio);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void receive_send(final FrameIO fio) throws Exception {
		Thread reader = new Thread() {
			public void run() {
				Frame frame = new Frame();
				while (true) {
					try {
						fio.receive(frame);
						byte[] dg = frame.getPayload();
						String str = new String(dg, 0, dg.length);
						if (str.charAt(0) == 'Q') {
							String tm = str.substring(1);
							long currTime = Long.parseLong(tm);
							Time.setCurrentTimeMillis(currTime);
							send(("T" + currTime), ADDR_NODE3, fio);
						} else if (str.charAt(0) == 'T') {
							send(str, ADDR_NODE1, fio);
						} else if (str.equalsIgnoreCase("EXIT")) {
							send("EXIT", ADDR_NODE3, fio);
							exit = true;
							hmapCOUNT.clear();
							a = 1;
							hmap.clear();
							hmap1.clear();
							break;
						} else if (str.equalsIgnoreCase("WAKTU")) {
							send("WAKTU", ADDR_NODE3, fio);
							String msg = "Time " + Integer.toHexString(ADDR_NODE2) + "(CH) " + Time.currentTimeMillis();
							send(msg, ADDR_NODE1, fio);
						} else if (str.equalsIgnoreCase("ON")) {
							send("ON", ADDR_NODE3, fio);
							String msg = "Node " + Integer.toHexString(ADDR_NODE2) + "(CH) ONLINE";
							send(msg, ADDR_NODE1, fio);
						} else if (str.equalsIgnoreCase("DETECT")) {
							for (int i = 1; i <= 5; i++) {
								try {
									String message = "SENSE " + Integer.toHexString(ADDR_NODE2) + " " + sn + " "
											+ Time.currentTimeMillis() + " " + s.sense();
									sn++;
									hmap.put(i, message);
								} catch (Exception e) {
								}
							}
							send("DETECT", ADDR_NODE3, fio);
							firstSense = true;
						} else if (str.charAt(str.length() - 1) == 'E') {
							send(str, ADDR_NODE1, fio);
						} else if (str.charAt(0) == 'S') {
//							if (frame.getSequenceNumber() > SN_A) {
//								SN_A = frame.getSequenceNumber();
							hmapCOUNT.put(frame.getSrcAddr(), a);
							byte[] s = frame.getPayload();
							String st = new String(s, 0, s.length);
							hmap1.put(a, st);
							a++;
//							}
						} else if (str.charAt(0) == 'E') {
							if (hmapCOUNT.get(frame.getSrcAddr()) == 5) {
								send("ACK", frame.getSrcAddr(), fio);
								ack.put(frame.getSrcAddr(), true);
							} else {
								a = 1;
								send("NACK", frame.getSrcAddr(), fio);
							}
						} else {
							if (str.equalsIgnoreCase("ACK")) {
								hmap.clear();
								hmap1.clear();
								ack.clear();
								for (int i = 1; i <= 5; i++) {
									try {
										String message = "SENSE " + Integer.toHexString(ADDR_NODE2) + " " + sn + " "
												+ Time.currentTimeMillis() + " " + s.sense();
										sn++;
										hmap.put(i, message);
									} catch (Exception e) {
									}
								}
								send("DETECT", ADDR_NODE3, fio);
							} else if (str.equalsIgnoreCase("NACK")) {
								sendAll(fio);
							}
						}

					} catch (Exception e) {
					}
				}
			}
		};
		reader.start();
		while (reader.isAlive()) {
			if (firstSense == true && exit != false && Time.currentTimeMillis() <= end) {
				if (ack.get(ADDR_NODE3) == true) {
					end = Time.currentTimeMillis() + 15000;
					sendAll(fio);
					send("END", ADDR_NODE1, fio);
					isSend = true;
				}
			}
			if (firstSense == true && isSend == true && exit != false && Time.currentTimeMillis() > end) {
				end = Time.currentTimeMillis() + 15000;
				sendAll(fio);
			}
		}
	}

	public static void sendAll(final FrameIO fio) {
		for (int i = 1; i <= 5; i++) {
			String message = hmap.get(i);
			String message1 = hmap1.get(i);
			try {
				send(message, ADDR_NODE1, fio);
				send(message1, ADDR_NODE1, fio);

			} catch (Exception e) {
			}
		}
	}

	public static void send(String msg, long address, final FrameIO fio) throws Exception {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16;
		final Frame testFrame = new Frame(frameControl);
		testFrame.setDestPanId(COMMON_PANID);
		testFrame.setDestAddr(address);
		testFrame.setSrcAddr(ADDR_NODE2);
		testFrame.setPayload(msg.getBytes());
		try {
			fio.transmit(testFrame);
			Thread.sleep(50);
		} catch (Exception e) {
		}
	}

	public static void main(String[] arg) throws Exception {
		a = 1;
		sn = 1; //
		exit = false;
		runs();
	}
}
