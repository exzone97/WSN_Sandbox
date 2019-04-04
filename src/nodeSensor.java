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

public class nodeSensor {
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);

	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	private static int ADDR_NODE1 = node_list[0]; // NODE DIATASNYA
	private static int ADDR_NODE2 = node_list[1]; // NODE DIRINYA
	private static sensing s = new sensing();
	private static int sn = 1;
	private static HashMap<Integer, Frame> hmap = new HashMap<Integer, Frame>();
	private static long end;
	private static boolean isSensing = false;
	private static boolean exit = false;

	public static void runs() {
		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, ADDR_NODE2, ADDR_NODE2, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);

			receive(fio);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void receive(final FrameIO fio) throws Exception {
		Thread reader = new Thread() {
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
						} else if (str.equalsIgnoreCase("EXIT")) {
							System.out.println(str);
							hmap.clear();
							isSensing = false;
							exit = true;
							break;
						} else if (str.equalsIgnoreCase("WAKTU")) {
							String message = "Time " + Integer.toHexString(ADDR_NODE2) + " " + Time.currentTimeMillis();
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(ADDR_NODE1);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload(message.getBytes());
							System.out.println(message);
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
							} catch (Exception e) {
							}
						} else if (str.equalsIgnoreCase("ON")) {
							String message = "Node " + Integer.toHexString(ADDR_NODE2) + " ONLINE";
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(ADDR_NODE1);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload(message.getBytes());
							System.out.println(message);
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
							} catch (Exception e) {
							}
						} else if (str.equalsIgnoreCase("DETECT")) {
							end = Time.currentTimeMillis() + 8000;
							System.out.println(str);
							String message = "";
							int i = 0;
							while (i <= 5 && exit == false) {
								try {
									if (i == 5) {
										message = "END";
									} else {
										message = "SENSE " + Integer.toHexString(ADDR_NODE2) + " " + sn + " "
												+ Time.currentTimeMillis() + " " + s.sense();
										sn++;
									}
									int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
											| Frame.SRC_ADDR_16;
									final Frame testFrame = new Frame(frameControl);
									if(!message.equals("END")) {
										testFrame.setSequenceNumber(sn);	
									}
									testFrame.setDestPanId(COMMON_PANID);
									testFrame.setDestAddr(ADDR_NODE1);
									testFrame.setSrcAddr(ADDR_NODE2);
									testFrame.setPayload(message.getBytes());
									System.out.println(message);
									hmap.put(i, testFrame);
									try {
										fio.transmit(testFrame);
										Thread.sleep(50);
									} catch (Exception e) {
									}
									i++;
									isSensing = true;
								} catch (Exception e) {
								}
							}
						} else {
							System.out.println(str);
							if (str.equalsIgnoreCase("ACK")) {
								hmap.clear();
								isSensing = false;
							} else {
								for (int j = 0; j < hmap.size(); j++) {
									final Frame testFrame = hmap.get(j);
									Thread.sleep(50);
									try {
										fio.transmit(testFrame);
										Thread.sleep(50);
									} catch (Exception e) {
									}
								}
							}
						}
					} catch (Exception e) {
					}
				}
			}
		};
		reader.start();
		while (reader.isAlive()) {
			if (isSensing == true && exit == false) {
				if (Time.currentTimeMillis() > end) {
					System.out.println("Timeout");
					for (int i = 0; i < hmap.size(); i++) {
						final Frame testFrame = hmap.get(i);
						Thread.sleep(50);
						try {
							fio.transmit(testFrame);
							Thread.sleep(50);
						} catch (Exception e) {
						}
					}
					end = Time.currentTimeMillis() + 8000;
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		exit = false;
		runs();
	}
}
