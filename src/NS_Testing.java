import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

public class NS_Testing {
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

//private static int ADDR_NODE1 = node_list[0];
//private static int ADDR_NODE2[] = new int[0];
//private static int ADDR_NODE3 = node_list[4];
	// =======================================================================================================
	private static int ADDR_NODE1 = node_list[0];
	private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAB) };
	private static int ADDR_NODE3 = node_list[1];

//private static int ADDR_NODE1 = node_list[1];
//private static int ADDR_NODE2[] = new int[0];
//private static int ADDR_NODE3 = node_list[2];

//private static int ADDR_NODE1 = node_list[0];
//private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAD)};
//private static int ADDR_NODE3 = node_list[3];

//private static int ADDR_NODE1 = node_list[3];
//private static int ADDR_NODE2[] = new int[0];
//private static int ADDR_NODE3 = node_list[4];

	private sensing s = new sensing();
	private int sn = 1; // sequence number

	private boolean exit = false;

	public void runs() {
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

	public void send_receive(final FrameIO fio) throws Exception {
		Thread thread = new Thread() {
			public void run() {
				Frame frame = new Frame();
				while (true) {
					try {
						fio.receive(frame);
						byte[] dg = frame.getPayload();
						String str = new String(dg, 0, dg.length);
						if (str.charAt(0) == 'T') {
							String tm = str.substring(1);
							long currTime = Long.parseLong(tm);
							Time.setCurrentTimeMillis(currTime);
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									String message = "T" + Time.currentTimeMillis();
									send(message, ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
						} else if (str.equalsIgnoreCase("EXIT")) {
							exit = true;
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									String message = "EXIT";
									send(message, ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
							break;

						} else if (str.equalsIgnoreCase("WAKTU")) {
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
						} else if (str.equalsIgnoreCase("ON")) {
							String msg = "Node " + Integer.toHexString(ADDR_NODE3) + " ONLINE";
							System.out.println("My Node");
							System.out.println(msg);
							send(msg, ADDR_NODE3, ADDR_NODE1, fio);
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send("ON", ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(50);
								}
							}
						} else if (str.charAt(str.length() - 1) == 'E') {
							System.out.println("Node bawah");
							System.out.println(str);
							send(str, ADDR_NODE3, ADDR_NODE1, fio);
						} else if (str.equalsIgnoreCase("DETECT")) {
							System.out.println("DETECT");
							Thread.sleep(200);
							String message = "SENSE<" + ADDR_NODE3 + ">" + sn + "?" + Time.currentTimeMillis() + " "
									+ s.sense();
							send(message, ADDR_NODE3, ADDR_NODE1, fio);
							System.out.println("MY SENSE");
							System.out.println(message);
							System.out.println("=========================");
							sn++;
							if (ADDR_NODE2.length > 0) {
								for (int i = 0; i < ADDR_NODE2.length; i++) {
									send("DETECT", ADDR_NODE3, ADDR_NODE2[i], fio);
									Thread.sleep(100);
								}
							}
						} else if (str.charAt(0) == 'S') {
							System.out.println("Receive");
							System.out.println(str);
							send(str, ADDR_NODE3, ADDR_NODE1, fio);
							Thread.sleep(100);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}

	public void send(String message, int source, int destination, final FrameIO fio) {
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
		new NS_Testing().runs();
	}

}