
//CLUSTER HEAD A
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

public class ClusterHeadA extends Thread {
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xCAAA),
			PropertyHelper.getInt("radio.panid", 0xDABA), PropertyHelper.getInt("radio.panid", 0xDABB),
			PropertyHelper.getInt("radio.panid", 0xCABA) };

	private static int ADDR_NODE1 = node_list[0]; // NODE DIATASNYA
	private static int ADDR_NODE2 = node_list[1]; // NODE DIRINYA

//	Node dibwh CH1
	private static int[] ADDR_NODE3 = new int[] { PropertyHelper.getInt("radio.panid", 0xDABA),
			PropertyHelper.getInt("radio.panid", 0xDABB) }; // NODE DIBAWAHNYA

	private static sensing s = new sensing();
	private static int sn = 1;
	private static long end_0;
	private static long end_1;
	private static long end_2;
	private static boolean firstSense = false;
	private static boolean exit = false;
	private static boolean isSend = false;

	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();

	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap1 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap2 = new HashMap<Integer, String>();

//	Count SN untuk node dibwh CH1
	private static int a = 1;
	private static int b = 1;

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
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								send(("T" + currTime), ADDR_NODE3[i], fio);
							}
						} else if (str.charAt(0) == 'T') {
							send(str, ADDR_NODE1, fio);
						} else if (str.equalsIgnoreCase("EXIT")) {
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								send("EXIT", ADDR_NODE3[i], fio);
							}
							exit = true;
							hmapCOUNT.clear();
							a = 1;
							b = 2;
							hmap.clear();
							hmap1.clear();
							hmap2.clear();
							break;
						} else if (str.equalsIgnoreCase("WAKTU")) {
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								send("WAKTU", ADDR_NODE3[i], fio);
							}
							String message = "Time " + Integer.toHexString(ADDR_NODE2) + "(CH) "
									+ Time.currentTimeMillis();
							send(message, ADDR_NODE1, fio);
						} else if (str.equalsIgnoreCase("ON")) {
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								send("ON", ADDR_NODE3[i], fio);
							}
							String message = "Node " + Integer.toHexString(ADDR_NODE2) + "(CH) ONLINE";
							send(message, ADDR_NODE1, fio);
						} else if (str.equalsIgnoreCase("DETECT")) {
							end_0 = Time.currentTimeMillis() + 8000;
							for (int i = 1; i <= 5; i++) {
								try {
									String message = "SENSE " + ADDR_NODE2 + " " + sn + " " + Time.currentTimeMillis()
											+ " " + s.sense();
									sn++;
									send(message, ADDR_NODE1, fio);
									hmap.put(i, message);
								} catch (Exception e) {
								}
							}
							send("END1", ADDR_NODE1, fio);
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								send("DETECT", ADDR_NODE3[i], fio);
							}
							firstSense = true;
						} else if (str.charAt(str.length() - 1) == 'E') {
							send(str, ADDR_NODE1, fio);
						} else if (str.charAt(0) == 'S') {
							if (frame.getSrcAddr() == ADDR_NODE3[0]) {
								hmapCOUNT.put(frame.getSrcAddr(), a);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap1.put(a, st);
								a++;
							} else if (frame.getSrcAddr() == ADDR_NODE3[1]) {
								hmapCOUNT.put(frame.getSrcAddr(), b);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap2.put(b, st);
								b++;
							}
						} else if (str.charAt(0) == 'E') {
							if (hmapCOUNT.get(frame.getSrcAddr()) == 5) {
								send("ACK", frame.getSrcAddr(), fio);
								long temp = frame.getSrcAddr();
								if (temp == ADDR_NODE3[0]) {
									for (int i = 1; i <= 5; i++) {
										String s = hmap1.get(i);
										send(s, ADDR_NODE1, fio);
										end_1 = Time.currentTimeMillis() + 8000;
									}
									send("END2", ADDR_NODE1, fio);
								} else if (temp == ADDR_NODE3[1]) {
									for (int i = 1; i <= 5; i++) {
										String s = hmap2.get(i);
										send(s, ADDR_NODE1, fio);
										end_2 = Time.currentTimeMillis() + 8000;
									}
									send("END3", ADDR_NODE1, fio);
								}
							} else {
								if (frame.getSrcAddr() == ADDR_NODE3[0]) {
									a = 1;
								} else if (frame.getSrcAddr() == ADDR_NODE3[1]) {
									b = 1;
								}
								send("NACK", frame.getSrcAddr(), fio);
							}
						} else {
							if (str.equalsIgnoreCase("ACK1")) {
								hmap.clear();
								end_0 = Time.currentTimeMillis() + 8000;
								for (int i = 1; i <= 5; i++) {
									try {
										String message = "SENSE " + ADDR_NODE2 + " " + sn + " "
												+ Time.currentTimeMillis() + " " + s.sense();
										sn++;
										hmap.put(i, message);
										send(message, ADDR_NODE1, fio);
									} catch (Exception e) {
									}
								}
								send("END1", ADDR_NODE1, fio);
							} else if (str.equalsIgnoreCase("ACK2")) {
								hmap1.clear();
								send("DETECT", ADDR_NODE3[0], fio);
							} else if (str.equalsIgnoreCase("ACK3")) {
								hmap2.clear();
								send("DETECT", ADDR_NODE3[1], fio);
							} else if (str.equalsIgnoreCase("NACK1")) {
								end_0 = Time.currentTimeMillis() + 8000;
								for (int i = 1; i <= 5; i++) {
									send(hmap.get(i), ADDR_NODE1, fio);
								}
								send("END1", ADDR_NODE1, fio);
							} else if (str.equalsIgnoreCase("NACK2")) {
								end_1 = Time.currentTimeMillis() + 8000;
								for (int i = 1; i <= 5; i++) {
									send(hmap1.get(i), ADDR_NODE1, fio);
								}
								send("END2", ADDR_NODE1, fio);
							} else if (str.equalsIgnoreCase("NACK3")) {
								end_2 = Time.currentTimeMillis() + 8000;
								for (int i = 1; i <= 5; i++) {
									send(hmap2.get(i), ADDR_NODE1, fio);
								}
								send("END3", ADDR_NODE1, fio);
							}
						}

					} catch (Exception e) {
					}
				}
			}
		};
		reader.start();
		while (reader.isAlive()) {
			if (Time.currentTimeMillis() > end_0) {
				end_0 = Time.currentTimeMillis() + 8000;
				for (int i = 1; i <= 5; i++) {
					send(hmap.get(i), ADDR_NODE1, fio);
				}
			}
			if (Time.currentTimeMillis() > end_1) {
				end_1 = Time.currentTimeMillis() + 8000;
				for (int i = 1; i <= 5; i++) {
					send(hmap1.get(i), ADDR_NODE1, fio);
				}
			}
			if (Time.currentTimeMillis() > end_2) {
				end_2 = Time.currentTimeMillis() + 8000;
				for (int i = 1; i <= 5; i++) {
					send(hmap2.get(i), ADDR_NODE1, fio);
				}
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
		b = 1;
		sn = 1; //
		exit = false;
		runs();
	}
}
