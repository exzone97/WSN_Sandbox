import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;
import java.util.HashMap;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

public class Test {
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	// ADDR_NODE1 = Node diatas
	private static int ADDR_NODE1 = node_list[0];

	// ADDR_NODE2 = Node dibawah
	// kalau tidak ada node dibawahnya = new int[0]
	private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAB) };
//	private static int ADDR_NODE2[] = new int[0];

	// ADDR_NODE3 = Node dirinya
	private static int ADDR_NODE3 = node_list[1];

	private static sensing s = new sensing();
	private static int sn = 1; // sequence number

	private static String myTemp; // Dr node sensor ke node sensor atas
//	private static String myTemp1; //
	private static String myTempEnd1; //
	private static long end; // timeout
	private static long end_L;
	private static boolean isSensing = false;
	private static boolean exit = false;

//	harus bikin penyimpanan sebanyak jumlah node di bwhnya.
	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>(); // penyimpanan sementara
	private static HashMap<Integer, String> hmapEnd = new HashMap<Integer, String>(); // penyimpanan sementara

	private static HashMap<Integer, Long> hmapTimeoutL = new HashMap<Integer, Long>();
	private static HashMap<Integer, Integer> hmapOK = new HashMap<Integer, Integer>();
	private static HashMap<Integer, Boolean> hmapSendL = new HashMap<Integer, Boolean>();

//	private static HashMap<Integer, Integer> hmapCurr_SN = new HashMap<Integer, Integer>(); // nyimpan sn untuk setiap
	// node dibwhnya klo ada

	public static void runs() {
		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, ADDR_NODE3, ADDR_NODE3, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);
			send_receive(fio);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void send_receive(final FrameIO fio) throws Exception {
		Thread thread = new Thread() {
			public void run() {
				Frame frame = new Frame();
				while (true) {
					try {
						fio.receive(frame);
						byte[] dg = frame.getPayload();
						String str = new String(dg, 0, dg.length);
						// Kalau dpt yang awalan 'T' berarti isinya waktu dari node diatasnya
						// set waktu dirinya.
						if (str.charAt(0) == 'Q') {
							String tm = str.substring(1);
							long currTime = Long.parseLong(tm);
							Time.setCurrentTimeMillis(currTime);
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									String message = "Q" + Time.currentTimeMillis();
									send(message, ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
						} else if (str.charAt(0) == 'T') {
							send(str, ADDR_NODE3, ADDR_NODE3, fio);
							System.out.println(str);
						}
						// Kalau dpt 'EXIT' stop dirinya dan kirim 'EXIT' ke node di bwhnya
						else if (str.equalsIgnoreCase("EXIT")) {
							isSensing = false;
							exit = true;
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									String message = "EXIT";
									send(message, ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
							break;

						}
						// Kalau dpt 'WAKTU', kirim waktu dirinya ke node diatasnya, dan kirim 'WAKTU'
						// ke node di bwhnya.
						else if (str.equalsIgnoreCase("WAKTU")) {
							String msg = "Time " + Integer.toHexString(ADDR_NODE3) + " " + Time.currentTimeMillis();
							send(msg, ADDR_NODE3, ADDR_NODE1, fio);
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									String message = "WAKTU";
									send(message, ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
							System.out.println(msg);

						} // Kalau dpt 'ON' kirim status ke node diatasnya dan kirim "ON" ke node di
							// bwhnya
						else if (str.equalsIgnoreCase("ON")) {
							String msg = "Node " + Integer.toHexString(ADDR_NODE3) + " ONLINE";
							send(msg, ADDR_NODE3, ADDR_NODE1, fio);
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send("ON", ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
							System.out.println(msg);
							// Kalau dpt akhiran 'E' (status online dr node di bwhnya) terusin ke node
							// diatasnya.
						} else if (str.charAt(str.length() - 1) == 'E') {
							send(str, ADDR_NODE3, ADDR_NODE1, fio);
							System.out.println(str);
						}
						// kalau dpt 'Detect', dia set end, sensing, sn++, simpen ke myTemp, kirim ke
						// node diatasnya
						// kirim juga END+ ADDR_NODE3
						// kirim 'DETECT' ke node di bwhnya
						else if (str.equalsIgnoreCase("DETECT")) {
							System.out.println("DETECT");
							end = Time.currentTimeMillis() + 2000;
							String message = "SENSE<" + ADDR_NODE3 + ">" + sn + "?" + Time.currentTimeMillis() + " "
									+ s.sense();
							myTemp = message;
//							myTemp1 = message;
							send(message, ADDR_NODE3, ADDR_NODE1, fio);
							Thread.sleep(50);
							myTempEnd1 = "END<" + sn + ">" + ADDR_NODE3;
							System.out.println("MY SENSE");
//							System.out.println(myTemp1);
							System.out.println(myTempEnd1);
							System.out.println("=========================");
							send(myTempEnd1, ADDR_NODE1, ADDR_NODE3, fio);
							sn++;
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send("DETECT", ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
							isSensing = true;
							// kalau dpt data sense.... dr node dbwhnya, hrs simpan sementara
						} else if (str.charAt(0) == 'S') {
							int startIndex = str.indexOf('<');
							int endIndex = str.indexOf('>');
							int node = Integer.parseInt(str.substring(startIndex + 1, endIndex));
							hmap.put(node, str);

							System.out.println("Receive data");
							System.out.println(str);
							//
						} else if (str.startsWith("END")) {
							int startIndex = str.indexOf('<');
							int endIndex = str.indexOf('>');
							int node = Integer.parseInt(str.substring(endIndex + 1));
							int seq = Integer.parseInt(str.substring(startIndex + 1, endIndex));
							System.out.println("END");
							if (hmap.get(node) != null) {
//								cek duplikasi, kalau sesuai kirim ack + data sense + end ke node diatasnya.
//								if (hmapCurr_SN.get(node) == seq) {
								send("ACK", ADDR_NODE3, node, fio);
								System.out.println("SEND ACK KE NODE BWH");
								String s = hmap.get(node);
								send(s, ADDR_NODE3, ADDR_NODE1, fio);
								System.out.println("SEND Data ke atas");
								System.out.println(s);
								Thread.sleep(100);
								send("END<" + seq + ">" + node, ADDR_NODE3, ADDR_NODE1, fio);
								hmapEnd.put(node, "END<" + seq + ">" + node);
								System.out.println("Send END ke atas");
								System.out.println("END<" + seq + ">" + node);
//									hmapCurr_SN.put(node, hmapCurr_SN.get(node) + 1);
//								}
							} else {
								send("NACK", ADDR_NODE3, node, fio);
								System.out.println("Send NACK ke bwh");
							}
						} else if (str.equalsIgnoreCase("ACK")) {
//							myTemp = "";
							isSensing = false;
							System.out.println("ACK");
						} else if (str.equalsIgnoreCase("NACK")) {
							System.out.println("NACK");
							send(myTemp, ADDR_NODE3, ADDR_NODE1, fio);
							System.out.println(myTemp);
							send(myTempEnd1, ADDR_NODE3, ADDR_NODE1, fio);
							System.out.println(myTempEnd1);
						}
						// time out
//						if (str.startsWith("L")) {
//							System.out.println(str);
//							int node = Integer.parseInt(str.substring(1));
//							send("OK" + node, ADDR_NODE3, ADDR_NODE1, fio);
////							hmapASDF.put();
//							System.out.println(node);
//							System.out.println(ADDR_NODE3);
//							if (node == ADDR_NODE3) {
//								String message = "SENSE<" + ADDR_NODE3 + ">" + sn + "?" + Time.currentTimeMillis() + " "
//										+ s.sense();
//								myTemp = message;
////								myTemp1= message;
//								System.out.println("Send" + message);
//								send(message, ADDR_NODE3, ADDR_NODE1, fio);
//								Thread.sleep(50);
//								System.out.println("Send" + " END<" + sn + ">" + ADDR_NODE3);
//								send("END<" + sn + ">" + ADDR_NODE3, ADDR_NODE1, ADDR_NODE3, fio);
//								myTempEnd1 = "END<" + sn + ">" + ADDR_NODE3;
//								sn++;
//								isSensing = true;
//								end = Time.currentTimeMillis() + 2000;
//							} else {
//								if (ADDR_NODE2.length > 0) {
//									System.out.println("Terusin L ke bwh");
//									hmapSendL.put(node, true);
//									hmapTimeoutL.put(node, Time.currentTimeMillis() + 1500);
//									for (int i = 0; i < ADDR_NODE2.length; i++) {
//										send(str, ADDR_NODE3, ADDR_NODE2[i], fio);
//										Thread.sleep(100);
//									}
//								}
//							}
//						} else if (str.startsWith("K")) {
//							System.out.println(str);
//							int node = Integer.parseInt(str.substring(1));
//							System.out.println("Resend ke atas");
//							System.out.println(hmap.get(node));
//							send(hmap.get(node), ADDR_NODE3, ADDR_NODE1, fio);
//							Thread.sleep(50);
//							System.out.println("");
//							send(hmapEnd.get(node), ADDR_NODE3, ADDR_NODE1, fio);
//						} else if (str.startsWith("OK")) {
//							int node = Integer.parseInt(str.substring(2));
//							hmapOK.put(node, hmapOK.get(node) + 1);
////							if (Time.currentTimeMillis() > hmapTimeoutL.get(node)
////									&& hmapOK.get(node) != ADDR_NODE2.length) {
////								hmapOK.put(node, 0);
////								hmapTimeoutL.put(node, Time.currentTimeMillis() + 1500);
////								for (int i = 0; i < ADDR_NODE2.length; i++) {
////									send("L" + node, ADDR_NODE3, ADDR_NODE2[i], fio);
////								}
////							}
//						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();

		while (thread.isAlive()) {
			if (isSensing == true && exit == false) {
				if (Time.currentTimeMillis() > end) {
					System.out.println("Timeout");

					send(myTemp, ADDR_NODE3, ADDR_NODE1, fio);
					System.out.println(myTemp);
					send(myTempEnd1, ADDR_NODE3, ADDR_NODE1, fio);
					System.out.println(myTempEnd1);
					end = Time.currentTimeMillis() + 2000;
				}
			}
//			for (int i = 0; i < ADDR_NODE2.length; i++) {
//				if (hmapTimeoutL.get(ADDR_NODE2[i]) != null && hmapOK.get(ADDR_NODE2[i]) != null
//						&& hmapSendL.get(ADDR_NODE2[i]) != null) {
//					if (hmapTimeoutL.get(ADDR_NODE2[i]) < Time.currentTimeMillis()
//							&& hmapOK.get(ADDR_NODE2[i]) != ADDR_NODE2.length) {
//						hmapOK.put(ADDR_NODE2[i], 0);
//						hmapTimeoutL.put(ADDR_NODE2[i], Time.currentTimeMillis() + 1500);
//						for (int j = 0; j < ADDR_NODE2.length; j++) {
//							send("L" + ADDR_NODE2[i], ADDR_NODE3, ADDR_NODE2[j], fio);
//						}
//					}
//				}
//			}
		}
	}

	public static void send(String message, int source, int destination, final FrameIO fio) {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16;
		final Frame testFrame = new Frame(frameControl);
		testFrame.setDestPanId(COMMON_PANID);
		testFrame.setDestAddr(destination);
		testFrame.setSrcAddr(source);
		testFrame.setPayload(message.getBytes());
		try {
			fio.transmit(testFrame);
			Thread.sleep(50);
		} catch (Exception e) {
		}
	}

	public static void main(String[] args) throws Exception {
		exit = false;
		if (ADDR_NODE2.length > 0) {
//			for (int i = 0; i < ADDR_NODE2.length; i++) {
//				hmapOK.put(ADDR_NODE2[i], 0);
//				hmapSendL.put(ADDR_NODE2[i], false);
//				hmapTimeoutL.put(ADDR_NODE2[i], 0l);
//			}
		}
		runs();
	}

}
