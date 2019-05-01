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

public class NS {
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

//	private static int ADDR_NODE1 = node_list[0];
//	private static int ADDR_NODE2[] = new int[0];
//	private static int ADDR_NODE3 = node_list[2];
	// =======================================================================================================
		private static int ADDR_NODE1 = node_list[0];
		private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAB)};
		private static int ADDR_NODE3 = node_list[1];

//		private static int ADDR_NODE1 = node_list[1];
//		private static int ADDR_NODE2[] = new int[0];
//		private static int ADDR_NODE3 = node_list[2];

//		private static int ADDR_NODE1 = node_list[0];
//		private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAD)};
//		private static int ADDR_NODE3 = node_list[3];
//
//		private static int ADDR_NODE1 = node_list[3];
//		private static int ADDR_NODE2[] = new int[0];
//		private static int ADDR_NODE3 = node_list[4];
	// =======================================================================================================


	private static sensing s = new sensing();
	private static int sn = 1; // sequence number

	private static String myTemp; // Dr node sensor ke node sensor atas
	private static long end; // timeout
	private static boolean isSensing = false;
	private static boolean exit = false;

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
							send(str, ADDR_NODE3, ADDR_NODE1, fio);
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
//							Thread.sleep(200);
							System.out.println("Node bwh : " + str);
							send(str, ADDR_NODE3, ADDR_NODE1, fio);
						}
						// kalau dpt 'Detect', dia set end, sensing, sn++, simpen ke myTemp, kirim ke
						// node diatasnya
						// kirim juga END+ ADDR_NODE3
						// kirim 'DETECT' ke node di bwhnya
						else if (str.equalsIgnoreCase("DETECT")) {
							System.out.println("DETECT");
							end = Time.currentTimeMillis() + 4000;
							String message = "SENSE<" + ADDR_NODE3 + ">" + sn + "?" + Time.currentTimeMillis() + " "
									+ s.sense();
							myTemp = message;
							Thread.sleep(50);
							System.out.println("MY SENSE");
							System.out.println(myTemp);
							System.out.println("=========================");
							sn++;
							// Send to anak-anaknya
							if (ADDR_NODE2.length > 0) {
								System.out.println("Send DETECT ke ADDR_NODE2[]");
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send("DETECT", ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
							System.out.println("SEND DATA & END TO ADDR_NODE1");
							send(myTemp, ADDR_NODE3, ADDR_NODE1, fio);
							Thread.sleep(50);
							isSensing = true;
						} else if (str.charAt(0) == 'S') {
							System.out.println("Receive SENSE");
							System.out.println(str);
							send(str, ADDR_NODE3, ADDR_NODE1, fio);
							System.out.println("SEND " + str);
						} else if (str.startsWith("ACK")) {
							int indexDot = str.indexOf(".");
							int node = Integer.parseInt(str.substring(3, indexDot));
							System.out.println(node);
							if (node == ADDR_NODE3) {

								int se = Integer.parseInt(str.substring(indexDot + 1));
								if (se == sn - 1) {
									System.out.println("RECEIVE ACK");
									isSensing = false;
									end = Time.currentTimeMillis() + 4000;
									String message = "SENSE<" + ADDR_NODE3 + ">" + sn + "?" + Time.currentTimeMillis()
											+ " " + s.sense();
									myTemp = message;
									Thread.sleep(50);
									System.out.println("MY SENSE");
									System.out.println(myTemp);
									System.out.println("=========================");
									sn++;
									send(myTemp, ADDR_NODE3, ADDR_NODE1, fio);
									Thread.sleep(50);
									isSensing = true;
								}
								else {
									send(myTemp, ADDR_NODE3, ADDR_NODE1, fio);

									end = Time.currentTimeMillis() + 4000;
								}
							} else {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send(str, ADDR_NODE3, ADDR_NODE2[i], fio);
								}
							}
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
					end = Time.currentTimeMillis() + 4000;
				}
			}
		}
	}

	public static void send(String message, int source, int destination, final FrameIO fio) {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.ACK_REQUEST
				| Frame.SRC_ADDR_16;
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
		runs();
	}
}