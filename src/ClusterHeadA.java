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
//	Node dibwh CH2
//	private static int ADDR_NODE3 = PropertyHelper.getInt("radio.panid", 0xCABA); //NODE DIBAWAHNYA

	private static sensing s = new sensing();
	private static int sn = 1;
	private static long end;
	private static boolean firstSense = false;
	private static boolean exit = false;

	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>();
	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();

	private static HashMap<Integer, String> hmap1 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap2 = new HashMap<Integer, String>();

//	private static HashMap<Integer, Frame> hmap1 = new HashMap<Integer, Frame>();

//	Count SN untuk node dibwh CH1
	private static int a = 1;
	private static int b = 1;
//	Count SN untuk node dibwh CH2	
//	private static int a = 1;
	private static int SN_A = 0;
	private static int SN_B = b;

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
								int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
										| Frame.SRC_ADDR_16;
								final Frame testFrame = new Frame(frameControl);
								testFrame.setDestPanId(COMMON_PANID);
								testFrame.setDestAddr(ADDR_NODE3[i]);
								testFrame.setSrcAddr(ADDR_NODE2);
								testFrame.setPayload(("T" + currTime).getBytes());
								try {
									fio.transmit(testFrame);
									Thread.sleep(50);
								} catch (Exception e) {
								}
							}
						} else if (str.equalsIgnoreCase("EXIT")) {
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								String message = "EXIT";
								int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
										| Frame.SRC_ADDR_16;
								final Frame testFrame = new Frame(frameControl);
								testFrame.setDestPanId(COMMON_PANID);
								testFrame.setDestAddr(ADDR_NODE3[i]);
								testFrame.setSrcAddr(ADDR_NODE2);
								testFrame.setPayload(message.getBytes());
								try {
									fio.transmit(testFrame);
									Thread.sleep(50);
								} catch (Exception e) {
								}
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
								String message = "WAKTU";
								int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
										| Frame.SRC_ADDR_16;
								final Frame testFrame = new Frame(frameControl);
								testFrame.setDestPanId(COMMON_PANID);
								testFrame.setDestAddr(ADDR_NODE3[i]);
								testFrame.setSrcAddr(ADDR_NODE2);
								testFrame.setPayload(message.getBytes());
								try {
									fio.transmit(testFrame);
									Thread.sleep(50);
								} catch (Exception e) {
								}
							}
							String message = "Time " + Integer.toHexString(ADDR_NODE2) + "(CH) "
									+ Time.currentTimeMillis();
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(ADDR_NODE1);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload(message.getBytes());
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
							} catch (Exception e) {
							}
						} else if (str.equalsIgnoreCase("ON")) {
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								String message = "ON";
								int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
										| Frame.SRC_ADDR_16;
								final Frame testFrame = new Frame(frameControl);
								testFrame.setDestPanId(COMMON_PANID);
								testFrame.setDestAddr(ADDR_NODE3[i]);
								testFrame.setSrcAddr(ADDR_NODE2);
								testFrame.setPayload(message.getBytes());
								try {
									fio.transmit(testFrame);
									Thread.sleep(50);
								} catch (Exception e) {
								}
							}
							String message = "Node " + Integer.toHexString(ADDR_NODE2) + "(CH) ONLINE";
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(ADDR_NODE1);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload(message.getBytes());
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
							} catch (Exception e) {
							}
						} else if (str.equalsIgnoreCase("DETECT")) {
							end = Time.currentTimeMillis() + 20000;
							for (int i = 0; i < 5; i++) {
								try {
									String message = "SENSE " + Integer.toHexString(ADDR_NODE2) + " " + sn + " "
											+ Time.currentTimeMillis() + " " + s.sense();
									sn++;
									hmap.put(i, message);
								} catch (Exception e) {
								}
							}
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								String message = "DETECT";
								int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
										| Frame.SRC_ADDR_16;
								final Frame testFrame = new Frame(frameControl);
								testFrame.setDestPanId(COMMON_PANID);
								testFrame.setDestAddr(ADDR_NODE3[i]);
								testFrame.setSrcAddr(ADDR_NODE2);
								testFrame.setPayload(message.getBytes());
								try {
									fio.transmit(testFrame);
									Thread.sleep(50);
								} catch (Exception e) {
								}
							}
							firstSense = true;
						} else {
							if (str.charAt(str.length() - 1) == 'E') {
								int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
										| Frame.SRC_ADDR_16;
								final Frame testFrame = new Frame(frameControl);
								testFrame.setDestPanId(COMMON_PANID);
								testFrame.setDestAddr(ADDR_NODE1);
								testFrame.setSrcAddr(ADDR_NODE2);
								testFrame.setPayload(str.getBytes());
								try {
									fio.transmit(testFrame);
									Thread.sleep(50);
								} catch (Exception e) {
								}
							} else if (str.charAt(0) == 'S') {
								if (frame.getSrcAddr() == ADDR_NODE3[1]) {
									if (frame.getSequenceNumber() > SN_A) {
										SN_A = frame.getSequenceNumber();
										hmapCOUNT.put(frame.getSrcAddr(), a);
										byte[] s = frame.getPayload();
										String st = new String(s, 0, s.length);
										hmap1.put(a, st);
										a++;
									}
								} else if (frame.getSrcAddr() == ADDR_NODE3[2]) {
									if (frame.getSequenceNumber() > SN_B) {
										SN_B = frame.getSequenceNumber();
										hmapCOUNT.put(frame.getSrcAddr(), b);
										byte[] s = frame.getPayload();
										String st = new String(s, 0, s.length);
										hmap2.put(b, st);
										b++;
									}
								}
							} else if (str.charAt(0) == 'E') {
								if (hmapCOUNT.get(frame.getSrcAddr()) == 5) {
									String message = "ACK";
									int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
											| Frame.SRC_ADDR_16;
									final Frame testFrame = new Frame(frameControl);
									testFrame.setDestPanId(COMMON_PANID);
									testFrame.setDestAddr(frame.getSrcAddr());
									testFrame.setSrcAddr(ADDR_NODE2);
									testFrame.setPayload(message.getBytes());
									try {
										fio.transmit(testFrame);
										Thread.sleep(50);
									} catch (Exception e) {
									}
								} else {
									String message = "NACK";
									int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
											| Frame.SRC_ADDR_16;
									final Frame testFrame = new Frame(frameControl);
									testFrame.setDestPanId(COMMON_PANID);
									testFrame.setDestAddr(frame.getSrcAddr());
									testFrame.setSrcAddr(ADDR_NODE2);
									testFrame.setPayload(message.getBytes());
									try {
										fio.transmit(testFrame);
										Thread.sleep(50);
									} catch (Exception e) {
									}
									if (frame.getSrcAddr() == ADDR_NODE3[0]) {
										a = 1;
									} else if (frame.getSrcAddr() == ADDR_NODE3[1]) {
										b = 1;
									}
								}
							} else {
								if (str.equalsIgnoreCase("ACK")) {
									hmap.clear();
									hmap1.clear();
									hmap2.clear();
									end = Time.currentTimeMillis()+20000;
									singleNodeSense(ADDR_NODE3[0], fio);
									singleNodeSense(ADDR_NODE3[1], fio);
									for (int i = 0; i < 5; i++) {
										try {
											String message = "SENSE " + Integer.toHexString(ADDR_NODE2) + " " + sn + " "
													+ Time.currentTimeMillis() + " " + s.sense();
											sn++;
											hmap.put(i, message);
										} catch (Exception e) {
										}
									}
								} else if (str.equalsIgnoreCase("NACK")) {
									resendAll(fio);
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
			if(firstSense == true && exit !=false && Time.currentTimeMillis()>end) {
				resendAll(fio);
			}
		}
	}
	
	public static void resendAll(final FrameIO fio) {
		for (int i = 1; i <= 5; i++) {
			String message = hmap.get(i);
			int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
					| Frame.SRC_ADDR_16;
			final Frame testFrame = new Frame(frameControl);
			testFrame.setDestPanId(COMMON_PANID);
			testFrame.setDestAddr(ADDR_NODE1);
			testFrame.setSrcAddr(ADDR_NODE2);
			testFrame.setPayload(message.getBytes());
			try {
				fio.transmit(testFrame);
				Thread.sleep(50);
			} catch (Exception e) {
			}
			String message1 = hmap1.get(i);
			int frameControl1 = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
					| Frame.SRC_ADDR_16;
			final Frame testFrame1 = new Frame(frameControl1);
			testFrame1.setDestPanId(COMMON_PANID);
			testFrame1.setDestAddr(ADDR_NODE1);
			testFrame1.setSrcAddr(ADDR_NODE2);
			testFrame1.setPayload(message1.getBytes());
			try {
				fio.transmit(testFrame1);
				Thread.sleep(50);
			} catch (Exception e) {
			}
			String message2 = hmap2.get(i);
			int frameControl2 = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
					| Frame.SRC_ADDR_16;
			final Frame testFrame2 = new Frame(frameControl2);
			testFrame2.setDestPanId(COMMON_PANID);
			testFrame2.setDestAddr(ADDR_NODE1);
			testFrame2.setSrcAddr(ADDR_NODE2);
			testFrame2.setPayload(message2.getBytes());
			try {
				fio.transmit(testFrame2);
				Thread.sleep(50);
			} catch (Exception e) {
			}
		}
	}

	public static void singleNodeSense(int address, final FrameIO fio) throws Exception {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16;
		final Frame testFrame = new Frame(frameControl);
		testFrame.setDestPanId(COMMON_PANID);
		testFrame.setDestAddr(address);
		testFrame.setSrcAddr(ADDR_NODE2);
		testFrame.setPayload("DETECT".getBytes());
		try {
			fio.transmit(testFrame);
			Thread.sleep(50);
		} catch (Exception e) {
		}
	}

	public static void main(String[] arg) throws Exception {
		exit = false;
		runs();
	}
}
