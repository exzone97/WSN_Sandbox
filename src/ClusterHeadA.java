
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

	private static long end_0;
	private static long end_1;
	private static long end_2;

	private static boolean exit = false;
	private static boolean isSensing = false;

	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();

	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap1 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap2 = new HashMap<Integer, String>();

	private static int sn = 1;

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
//						System.out.println("Receive " + str);
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
							sn = 1;
							exit = true;
							hmapCOUNT.clear();
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
							end_0 = Time.currentTimeMillis() + 3000;
							end_1 = Time.currentTimeMillis() + 3000;
							end_2 = Time.currentTimeMillis() + 3000;
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								send("DETECT", ADDR_NODE3[i], fio);
							}
							String message = "SENSE<" + ADDR_NODE2 + ">" + sn + "?" + Time.currentTimeMillis() + " "
									+ s.sense();
							sn++;
							send(message, ADDR_NODE1, fio);
							hmap.put(1, message);
							send("END1", ADDR_NODE1, fio);
//							System.out.println("END1");
//							System.out.println(message);
							isSensing = true;
						} else if (str.charAt(str.length() - 1) == 'E') {
							send(str, ADDR_NODE1, fio);
						} else if (str.charAt(0) == 'S') {
							if (frame.getSrcAddr() == ADDR_NODE3[0]) {
								System.out.println(str);
								hmapCOUNT.put(frame.getSrcAddr(), 1);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap1.put(1, st);
								System.out.println(hmap1.get(1));
							} else if (frame.getSrcAddr() == ADDR_NODE3[1]) {
								hmapCOUNT.put(frame.getSrcAddr(), 1);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap2.put(1, st);
							}
						} else if (str.startsWith("END")) {
							if (hmapCOUNT.get(frame.getSrcAddr()) == 1) {
								long temp = frame.getSrcAddr();
								if (temp == ADDR_NODE3[0]) {
									if (a == Integer.parseInt(str.substring(4))) {
										send("ACK", frame.getSrcAddr(), fio);
										String s = hmap1.get(1);
										send(s, ADDR_NODE1, fio);
										send("END2", ADDR_NODE1, fio);
										end_1 = Time.currentTimeMillis() + 3000;
										a++;
									}
								} else if (temp == ADDR_NODE3[1]) {
									if (b == Integer.parseInt(str.substring(4))) {
										send("ACK", frame.getSrcAddr(), fio);
										String s = hmap2.get(1);
										send(s, ADDR_NODE1, fio);
										send("END3", ADDR_NODE1, fio);
										end_2 = Time.currentTimeMillis() + 3000;
										b++;
									}
								}
							} else {
								send("NACK", frame.getSrcAddr(), fio);
							}
						}
						if (str.equalsIgnoreCase("ACK1")) {
							hmap.clear();
							hmapCOUNT.put((long) ADDR_NODE2, 0);
							String message = "SENSE<" + ADDR_NODE2 + ">" + sn + "?" + Time.currentTimeMillis() + " "
									+ s.sense();
							sn++;
							hmap.put(1, message);
							send(message, ADDR_NODE1, fio);
							send("END1", ADDR_NODE1, fio);
							end_0 = Time.currentTimeMillis() + 3000;
						} else if (str.equalsIgnoreCase("ACK2")) {
							hmap1.clear();
							hmapCOUNT.put((long) ADDR_NODE3[0], 0);
							send("DETECT", ADDR_NODE3[0], fio);
							end_1 = Time.currentTimeMillis() + 3000;
						} else if (str.equalsIgnoreCase("ACK3")) {
							hmap2.clear();
							hmapCOUNT.put((long) ADDR_NODE3[1], 0);
							send("DETECT", ADDR_NODE3[1], fio);
							end_2 = Time.currentTimeMillis() + 3000;
						} else if (str.equalsIgnoreCase("NACK1")) {
							send(hmap.get(1), ADDR_NODE1, fio);
							send("END1", ADDR_NODE1, fio);
							end_0 = Time.currentTimeMillis() + 3000;
						} else if (str.equalsIgnoreCase("NACK2")) {
							send(hmap1.get(1), ADDR_NODE1, fio);
							send("END2", ADDR_NODE1, fio);
							end_1 = Time.currentTimeMillis() + 3000;
						} else if (str.equalsIgnoreCase("NACK3")) {
							send(hmap2.get(1), ADDR_NODE1, fio);
							send("END3", ADDR_NODE1, fio);
							end_2 = Time.currentTimeMillis() + 3000;
						}

					} catch (Exception e) {
					}
				}
			}
		};
		reader.start();
		while (reader.isAlive()) {
			if (isSensing == true && exit == false) {
				if (Time.currentTimeMillis() > end_0) {
					if (hmap.get(1) != null) {
						// System.out.println("Timeout END_0");
						send(hmap.get(1), ADDR_NODE1, fio);
						send("END1", ADDR_NODE1, fio);
						end_0 = Time.currentTimeMillis() + 3000;
					}
				} else if (Time.currentTimeMillis() > end_1) {
					if (hmap1.get(1) != null) {
//					System.out.println("Timeout END_1");
						send(hmap1.get(1), ADDR_NODE1, fio);
						send("END2", ADDR_NODE1, fio);
						end_1 = Time.currentTimeMillis() + 3000;
					}
				} else if (Time.currentTimeMillis() > end_2) {
					if (hmap2.get(1) != null) {
						send(hmap2.get(1), ADDR_NODE1, fio);
						send("END3", ADDR_NODE1, fio);
						end_2 = Time.currentTimeMillis() + 3000;
					}
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
		sn = 1; //
		a = 1;
		b = 1;
		exit = false;
		runs();
	}
}
