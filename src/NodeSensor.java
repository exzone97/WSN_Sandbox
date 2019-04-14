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

public class NodeSensor {
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	// ADDR_NODE1 = Node diatas
//	private static int ADDR_NODE1 = node_list[0];
	private static int ADDR_NODE1 = node_list[1];

	// ADDR_NODE2 = Node dibawah
//	 kalau tidak ada node dibawahnya = new int[0]
//	private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAB) };
	private static int ADDR_NODE2[] = new int[0];

	// ADDR_NODE3 = Node dirinya
//	private static int ADDR_NODE3 = node_list[1];
	private static int ADDR_NODE3 = node_list[2];

	private static sensing s = new sensing();
	private static int sn = 1; // sequence number

	private static String myTemp; // Dr node sensor ke node sensor atas
	private static String myTempEnd; //
	private static long end; // timeout
	private static boolean isSensing = false;
	private static boolean exit = false;
	private static int isSendToBS;
	private static boolean isMiddleSensor = false;
	private static long timeout;

	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>(); // penyimpanan sementara
	private static HashMap<Integer, String> hmapTemp = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmapEnd = new HashMap<Integer, String>(); // penyimpanan sementara
	private static HashMap<Integer, Boolean> hmapACK = new HashMap<Integer, Boolean>();
	private static HashMap<Integer, Boolean> hmapOK = new HashMap<Integer, Boolean>();

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
						System.out.println(str);
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

						}
						// Kalau dpt 'ON' kirim status ke node diatasnya dan kirim "ON" ke node di
						// bwhnya
						else if (str.equalsIgnoreCase("ON")) {
							String msg = "Node " + Integer.toHexString(ADDR_NODE3) + " ONLINE";
							send(msg, ADDR_NODE3, ADDR_NODE1, fio);
							System.out.println("Dirinya : " + msg);
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send("ON", ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
						}
						// Kalau dpt akhiran 'E' (status online dr node di bwhnya) terusin ke node
						// diatasnya.
						else if (str.charAt(str.length() - 1) == 'E') {
							System.out.println("Node bwh : " + str);
							send(str, ADDR_NODE3, ADDR_NODE1, fio);
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
							hmap.put(ADDR_NODE3, message);

							Thread.sleep(50);
							myTempEnd = "END<" + sn + ">" + ADDR_NODE3;
							hmapEnd.put(ADDR_NODE3, myTempEnd);
							System.out.println("MY SENSE");
							System.out.println(hmap.get(ADDR_NODE3));
							System.out.println(myTempEnd);
							System.out.println("=========================");
							sn++;
							hmapACK.put(ADDR_NODE3, true);
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send("DETECT", ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
							if (ADDR_NODE1 == node_list[0]) {
								isSensing = false;
							} else {
								send(myTemp, ADDR_NODE3, ADDR_NODE1, fio);
								Thread.sleep(500);
								send(myTempEnd, ADDR_NODE3, ADDR_NODE1, fio);
								isSensing = true;
							}
						} else if (str.charAt(0) == 'S') {
							int startIndex = str.indexOf('<');
							int endIndex = str.indexOf('>');
							int node = Integer.parseInt(str.substring(startIndex + 1, endIndex));
//							System.out.println("Node s:"+node);
							hmap.put(node, str);//
							hmapTemp.put(node, str);
							System.out.println("Receive data");
							System.out.println(hmap.get(node));
							
						} else if (str.startsWith("END")) {
//							int startIndex = str.indexOf('<');
							int endIndex = str.indexOf('>');
							int node = Integer.parseInt(str.substring(endIndex + 1));
							System.out.println("Node E:"+hmap.get(node));
//							int seq = Integer.parseInt(str.substring(startIndex + 1, endIndex));
							System.out.println("END");
							if (hmapTemp.get(node) != null) {
								send("ACK", ADDR_NODE3, node, fio);
								hmapACK.put(node, true);
//								hmap.put(node, hmapTemp.get(node));
								System.out.println("SEND ACK KE NODE BWH " + hmap.get(node));
							} else {
								send("NACK", ADDR_NODE3, node, fio);
								System.out.println("Send NACK ke bwh");
							}
						} else if (str.equalsIgnoreCase("ACK")) {
							hmapTemp.clear();
							isSensing = false;
							System.out.println("ACK");
						} else if (str.equalsIgnoreCase("NACK")) {
							System.out.println("NACK");
							send(myTemp, ADDR_NODE3, ADDR_NODE1, fio);
							System.out.println(myTemp);
							send(myTempEnd, ADDR_NODE3, ADDR_NODE1, fio);
							System.out.println(myTempEnd);
						}
						if (str.equalsIgnoreCase("OK")) {
							hmapACK.clear();
							hmap.clear();
							System.out.println("DETECT");
							end = Time.currentTimeMillis() + 2000;
							String message = "SENSE<" + ADDR_NODE3 + ">" + sn + "?" + Time.currentTimeMillis() + " "
									+ s.sense();
							myTemp = message;
							hmap.put(ADDR_NODE3, message);
							System.out.println(hmap.size());
							Thread.sleep(50);
							myTempEnd = "END<" + sn + ">" + ADDR_NODE3;
							hmapEnd.put(ADDR_NODE3, myTempEnd);
							System.out.println("MY SENSE");
							System.out.println(hmap.get(ADDR_NODE3));
							System.out.println(myTempEnd);
							System.out.println("=========================");
							sn++;
							hmapACK.put(ADDR_NODE3, true);
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send("DETECT", ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
							if (ADDR_NODE1 == node_list[0]) {
								isSensing = false;
							} else {
								send(myTempEnd, ADDR_NODE3, ADDR_NODE1, fio);
								isSensing = true;
							}
							isSendToBS = 0;
							System.out.println("DETECT END");
						}

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
					send(myTempEnd, ADDR_NODE3, ADDR_NODE1, fio);
					System.out.println(myTempEnd);
					end = Time.currentTimeMillis() + 2000;
				}
			}
			if (isMiddleSensor == true) {
				if (hmapACK.size() == ADDR_NODE2.length + 1 && isSendToBS == 0) {
					System.out.println("Send Count");
					send("COUNT" + hmapACK.size(), ADDR_NODE3, ADDR_NODE1, fio);
					for (int key : hmap.keySet()) {
						System.out.println("Send to bs");
						System.out.println(hmap.get(key));
						send("100" + ADDR_NODE3 + "/" + hmap.get(key), ADDR_NODE3, ADDR_NODE1, fio);
						Thread.sleep(200);
					}
					System.out.println("End Count");
					send("AKHIR" + ADDR_NODE3, ADDR_NODE3, ADDR_NODE1, fio);
					System.out.println("1000" + ADDR_NODE3);
					timeout = Time.currentTimeMillis() + 4000;
					isSendToBS = 1;
				}
				if (isSendToBS == 1 && Time.currentTimeMillis() > timeout) {
					System.out.println("Timeout.. Resend To BS");
					send("COUNT" + hmapACK.size(), ADDR_NODE3, ADDR_NODE1, fio);
					for (int key : hmap.keySet()) {
						System.out.println("Resend to BS:");
						send("100" + ADDR_NODE3 + "/" + hmap.get(key), ADDR_NODE3, ADDR_NODE1, fio);
						Thread.sleep(200);
					}
					System.out.println("End Count");
					send("AKHIR", ADDR_NODE3, ADDR_NODE1, fio);
					System.out.println("2000" + ADDR_NODE3);
					timeout = Time.currentTimeMillis() + 4000;
				}
			}
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
			isMiddleSensor = true;
		} else {
			isMiddleSensor = false;
		}
		runs();
	}

}
