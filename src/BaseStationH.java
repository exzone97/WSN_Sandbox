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
import com.virtenio.io.Console;

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
	private static HashMap<Integer, String> hmap4 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap5 = new HashMap<Integer, String>();
	private static USART usart;
	private static OutputStream out;
	private static boolean exit;
	private static boolean firstSense;

	private static int curr_SN_a = 0;
	private static int curr_SN_b = 0;
	private static int curr_SN_d = 0;
	private static int curr_SN_e = 0;

//	private Console console;

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
//					Console co = new Console();
//					String s = co.readLine("asd");
//					int temp = Integer.parseInt(s);
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
						firstSense = false;
						hmapCOUNT.clear();
						hmap1.clear();
						hmap2.clear();
						hmap4.clear();
						hmap5.clear();
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
//								Thread.sleep(900);
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
//							System.out.println(str);
						} else if (str.charAt(0) == 'T') {
							String msg = "#" + str + "#";
							try {
//								Thread.sleep(900);
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
//							System.out.println(str);
						} else if (str.charAt(0) == 'S') {
							if (frame.getSrcAddr() == node_list[1]) {
								int awal = str.indexOf("<");
								int akhir = str.indexOf(">");
								String node = str.substring(awal + 1, akhir);
								if (Integer.parseInt(node) == ADDR_NODE_CH1[0]) {
									hmap1.put(1, str);
									hmapCOUNT.put(ADDR_NODE_CH1[0], 1);
								} else if (Integer.parseInt(node) == ADDR_NODE_CH1[1]) {
									hmap2.put(1, str);
									hmapCOUNT.put(ADDR_NODE_CH1[1], 1);
								}
							} else if (frame.getSrcAddr() == node_list[2]) {
								int awal = str.indexOf("<");
								int akhir = str.indexOf(">");
								String node = str.substring(awal + 1, akhir);
								if (Integer.parseInt(node) == ADDR_NODE_CH2[0]) {
									hmap4.put(1, str);
									hmapCOUNT.put(ADDR_NODE_CH2[0], 1);
								} else if (Integer.parseInt(node) == ADDR_NODE_CH2[1]) {
									hmap5.put(1, str);
									hmapCOUNT.put(ADDR_NODE_CH2[1], 1);
								}
							}
						} else if (str.charAt(0) == 'E') {
							if (str.equalsIgnoreCase("END1")) {
								if (hmapCOUNT.get(ADDR_NODE_CH1[0]) == 1) {
									String ss = hmap1.get(1);
									int akhir = ss.indexOf(">");
									int akhir_sn = ss.indexOf("?");
									int seq = Integer.parseInt(ss.substring(akhir + 1, akhir_sn));
									if (curr_SN_a == seq) {
										String msg = "#" + ss + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											usart.flush();
											Thread.sleep(100);
										} catch (Exception e) {
										}
										curr_SN_a++;
//										System.out.println(ss);
									}
									hmap1.clear();
									sends("ACK1", node_list[1], fio);
								} else {
									sends("NACK1", node_list[1], fio);
//									System.out.println("NACK1");
								}
								hmapCOUNT.put(ADDR_NODE_CH1[0], 0);
							} else if (str.equalsIgnoreCase("END2")) {
								if (hmapCOUNT.get(ADDR_NODE_CH1[1]) == 1) {
									String ss = hmap2.get(1);
									int akhir = ss.indexOf(">");
									int akhir_sn = ss.indexOf("?");
									int seq = Integer.parseInt(ss.substring(akhir + 1, akhir_sn));
									if (curr_SN_b == seq) {
										String msg = "#" + ss + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											out.flush();
											Thread.sleep(100);
										} catch (Exception e) {
										}
										curr_SN_b++;
//										System.out.println(ss);
									}
									hmap2.clear();
									sends("ACK2", node_list[1], fio);
								} else {
									sends("NACK2", node_list[1], fio);
//									System.out.println("NACK2");
								}
								hmapCOUNT.put(ADDR_NODE_CH1[1], 0);
							} else if (str.equalsIgnoreCase("END4")) {
								if (hmapCOUNT.get(ADDR_NODE_CH2[0]) == 1) {
									String ss = hmap4.get(1);
									int akhir = ss.indexOf(">");
									int akhir_sn = ss.indexOf("?");
									int seq = Integer.parseInt(ss.substring(akhir + 1, akhir_sn));
									if (curr_SN_d == seq) {
										String msg = "#" + ss + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											out.flush();
											Thread.sleep(100);
										} catch (Exception e) {
										}
										curr_SN_d++;
									}
									hmap4.clear();
									sends("ACK4", node_list[2], fio);
								} else {
									sends("NACK4", node_list[2], fio);
								}
								hmapCOUNT.put(ADDR_NODE_CH2[0], 0);
							} else if (str.equalsIgnoreCase("END5")) {
								if (hmapCOUNT.get(ADDR_NODE_CH2[1]) == 1) {
									String ss = hmap5.get(1);
									int akhir = ss.indexOf(">");
									int akhir_sn = ss.indexOf("?");
									int seq = Integer.parseInt(ss.substring(akhir + 1, akhir_sn));
									if (curr_SN_e == seq) {
										String msg = "#" + ss + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											out.flush();
											Thread.sleep(100);
										} catch (Exception e) {
										}
										curr_SN_e++;
									}
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

		curr_SN_a = 1;
		curr_SN_b = 1;
		curr_SN_d = 1;
		curr_SN_e = 1;

		try {
			startUSART();
			out = usart.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		runs();
	}
}