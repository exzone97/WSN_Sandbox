
import com.virtenio.preon32.examples.common.USARTConstants;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;

import com.virtenio.driver.device.at86rf231.AT86RF231;
import com.virtenio.driver.device.at86rf231.AT86RF231RadioDriver;
import com.virtenio.misc.PropertyHelper;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.FrameIO;
import com.virtenio.radio.ieee_802_15_4.RadioDriver;
import com.virtenio.radio.ieee_802_15_4.RadioDriverFrameIO;

import java.io.OutputStream;
import java.util.HashMap;
import com.virtenio.driver.usart.NativeUSART;
import com.virtenio.driver.usart.USART;
import com.virtenio.driver.usart.USARTException;
import com.virtenio.driver.usart.USARTParams;

public class BaseStationH extends Thread {

	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xCAAA) };
//	DAAA, CAAA = Cluster Head
	private static int ADDR_NODE2 = node_list[0]; // NODE DIRINYA (BS)

	private static int ADDR_NODE_CH1[] = { PropertyHelper.getInt("radio.panid", 0xDAAA),
			PropertyHelper.getInt("radio.panid", 0xDABA), PropertyHelper.getInt("radio.panid", 0xDABB) };

	private static int ADDR_NODE_CH2[] = { PropertyHelper.getInt("radio.panid", 0xCAAA),
			PropertyHelper.getInt("radio.panid", 0xCABA) };

	private static HashMap<Integer, Integer> hmapCOUNT = new HashMap<Integer, Integer>();
	private static HashMap<Integer, String> hmap1 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap2 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap3 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap4 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap5 = new HashMap<Integer, String>();
	private static int a, b, c, d, e;
	private static USART usart;
	private static OutputStream out;
	private static boolean exit;
	private static boolean firstSense;

	public static void runs() {
		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, ADDR_NODE2, ADDR_NODE2, false);
			final RadioDriver radioDriver = new AT86RF231RadioDriver(t);
			final FrameIO fio = new RadioDriverFrameIO(radioDriver);
			Thread thread = new Thread() {
				public void run() {
					try {
						send(fio);
						receive(fio);
					} catch (Exception e) {
					}
				}
			};
			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void send(final FrameIO fio) throws Exception {
		new Thread() {
			public void run() {
				while (true) {
					int temp = 100;
					try {
						temp = usart.read();
					} catch (USARTException e1) {
						e1.printStackTrace();
					}
					if (temp == 0) {
						for (int i = 1; i < node_list.length; i++) {
							try {
								sends("EXIT", node_list[i], fio);
							} catch (Exception e) {
							}
						}
						exit = true;
						a = 1;
						b = 1;
						firstSense = false;
						hmapCOUNT.clear();
						hmap1.clear();
						hmap2.clear();
						break;
					} else if (temp == 1) {
						for (int i = 1; i < node_list.length; i++) {
							try {
								sends("ON", node_list[i], fio);
							} catch (Exception e) {
							}
						}
					} else if (temp == 2) {
						long currTime = Time.currentTimeMillis();
						for (int i = 1; i < node_list.length; i++) {
							try {
								sends(("Q" + currTime), node_list[i], fio);
							} catch (Exception e) {
							}
						}
					} else if (temp == 3) {
						for (int i = 1; i < node_list.length; i++) {
							try {
								sends("WAKTU", node_list[i], fio);
							} catch (Exception e) {
							}
						}
					} else if (temp == 4) {
						firstSense = true;
						for (int i = 1; i < node_list.length; i++) {
							try {
								sends("DETECT", node_list[i], fio);
							} catch (Exception e) {
							}
						}
					}
				}
			}
		}.start();
	}

	public static void receive(final FrameIO fio) throws Exception {
		Thread receive = new Thread() {
			public void run() {
				Frame frame = new Frame();
				while (true) {
					try {
						fio.receive(frame);
						byte[] dg = frame.getPayload();
						String str = new String(dg, 0, dg.length);
						if (str.charAt(str.length() - 1) == 'E') {
							String msg = "#" + str + "#";
							try {
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (str.charAt(0) == 'T') {
							String msg = "#" + str + "#";
							try {
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (str.charAt(0) == 'S') {
							if (frame.getSrcAddr() == node_list[1]) {
//								String ss[] = str.split(" ");
//								if (Integer.parseInt(ss[1]) == ADDR_NODE_CH1[0]) {
//									hmap1.put(a, str);
//									hmapCOUNT.put(ADDR_NODE_CH1[0], a);
//									a++;
//								} else if (Integer.parseInt(ss[1]) == ADDR_NODE_CH1[1]) {
//									hmap2.put(b, str);
//									hmapCOUNT.put(ADDR_NODE_CH1[1], b);
//									b++;
//								} else if (Integer.parseInt(ss[1]) == ADDR_NODE_CH1[2]) {
//									hmap3.put(c, str);
//									hmapCOUNT.put(ADDR_NODE_CH1[2], c);
//									c++;
//								}
							} else if (frame.getSrcAddr() == node_list[2]) {
//								String ss[] = str.split(" ");
//								if (Integer.parseInt(ss[1]) == ADDR_NODE_CH2[0]) {
//									hmap4.put(d, str);
//									hmapCOUNT.put(ADDR_NODE_CH2[0], d);
//									d++;
//								} else if (Integer.parseInt(ss[1]) == ADDR_NODE_CH2[1]) {
//									hmap5.put(e, str);
//									hmapCOUNT.put(ADDR_NODE_CH2[1], e);
//									e++;
//								}
							}
						} else if (str.charAt(0) == 'E') {
							if (str.equalsIgnoreCase("END1")) {
								if (hmapCOUNT.get(ADDR_NODE_CH1[0]) == 5) {
									for (int i = 1; i <= 5; i++) {
										String s = hmap1.get(i);
										String msg = "#" + s + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											usart.flush();
											Thread.sleep(50);
										} catch (Exception e) {
										}
									}
									a = 1;
									hmap1.clear();
									sends("ACK1", node_list[1], fio);
								} else {
									sends("NACK1", node_list[1], fio);
								}
								hmapCOUNT.put(ADDR_NODE_CH1[0], 0);
							} else if (str.equalsIgnoreCase("END2")) {
								if (hmapCOUNT.get(ADDR_NODE_CH1[1]) == 5) {
									for (int i = 1; i <= 5; i++) {
										String s = hmap2.get(i);
										String msg = "#" + s + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											out.flush();
											Thread.sleep(50);
										} catch (Exception e) {
										}
									}
									b = 1;
									hmap2.clear();
									sends("ACK2", node_list[1], fio);
								} else {
									sends("NACK2", node_list[1], fio);
								}
								hmapCOUNT.put(ADDR_NODE_CH1[1], 0);
							} else if (str.equalsIgnoreCase("END3")) {
								if (hmapCOUNT.get(ADDR_NODE_CH1[2]) == 5) {
									for(int i = 1;i<=5;i++) {
										String s = hmap3.get(i);
										String msg = "#" +s +"#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											out.flush();
											Thread.sleep(50);
										}
										catch(Exception e) {
										}
									}
									c = 1;
									hmap3.clear();
									sends("ACK3", node_list[1], fio);
								} else {
									sends("NACK3", node_list[1], fio);
								}
								hmapCOUNT.put(ADDR_NODE_CH1[2], 0);
							} else if (str.equalsIgnoreCase("END4")) {
								if (hmapCOUNT.get(ADDR_NODE_CH2[0]) == 5) {
									for(int i = 1;i<=5;i++) {
										String s = hmap4.get(i);
										String msg = "#" + s+"#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											out.flush();
											Thread.sleep(50);
										}
										catch(Exception e) {
										}
									}
									d = 1;
									hmap4.clear();
									sends("ACK4", node_list[2], fio);
								} else {
									sends("NACK4", node_list[2], fio);
								}
								hmapCOUNT.put(ADDR_NODE_CH2[0], 0);
							} else if (str.equalsIgnoreCase("END5")) {
								if (hmapCOUNT.get(ADDR_NODE_CH2[1]) == 5) {
									for(int i =1;i<=5;i++) {
										String s = hmap5.get(i);
										String msg = "#" + s +"#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											out.flush();
											Thread.sleep(50);
										}
										catch(Exception e) {
										}
									}
									e = 1;
									hmap5.clear();
									sends("ACK5", node_list[2], fio);
								} else {
									sends("NACK5", node_list[2], fio);
								}
								hmapCOUNT.put(ADDR_NODE_CH2[1], 0);
							}
						}
					} catch (Exception e) {
					}
				}
			}
		};
		receive.start();
	}

	public static void sends(String msg, long address, final FrameIO fio) throws Exception {
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

	private static USART configUSART() {
		USARTParams params = USARTConstants.PARAMS_115200;
		NativeUSART usart = NativeUSART.getInstance(0);
		try {
			usart.close();
			usart.open(params);
			return usart;
		} catch (Exception e) {
			return null;
		}
	}

	private static void startUSART() {
		usart = configUSART();
	}

	public static void main(String[] args) throws Exception {
		a = 1;
		b = 1;
		c = 1;
		d = 1;
		e = 1;
		try {
			startUSART();
			out = usart.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		runs();
	}
}