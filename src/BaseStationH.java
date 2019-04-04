
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
//import com.virtenio.io.Console;

public class BaseStationH extends Thread {

	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xCAAA) };

	private static int ADDR_NODE2 = node_list[0]; // NODE DIRINYA (BS)

	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();
	private static HashMap<Integer, String> hmap1 = new HashMap<Integer, String>();
	private static HashMap<Integer, String> hmap2 = new HashMap<Integer, String>();
	private static int a, b;
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
//					int temp = 100;
					int temp = 0;
					try {
						temp = usart.read();
					} catch (USARTException e1) {
						e1.printStackTrace();
					}
					if (temp == 0) {
						for (int i = 1; i < node_list.length; i++) {
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(node_list[i]);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload("EXIT".getBytes());
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
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
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(node_list[i]);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload("ON".getBytes());
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
							} catch (Exception e) {
							}
						}
					} else if (temp == 2) {
						long currTime = Time.currentTimeMillis();
						for (int i = 1; i < node_list.length; i++) {
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(node_list[i]);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload(("Q" + currTime).getBytes());
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
							} catch (Exception e) {
							}
						}
					} else if (temp == 3) {
						for (int i = 1; i < node_list.length; i++) {
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(node_list[i]);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload("WAKTU".getBytes());
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
							} catch (Exception e) {
							}
						}
					} else if (temp == 4) {
						firstSense = true;
						for (int i = 1; i < node_list.length; i++) {
							int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
									| Frame.SRC_ADDR_16;
							final Frame testFrame = new Frame(frameControl);
							testFrame.setDestPanId(COMMON_PANID);
							testFrame.setDestAddr(node_list[i]);
							testFrame.setSrcAddr(ADDR_NODE2);
							testFrame.setPayload("DETECT".getBytes());
							try {
								fio.transmit(testFrame);
								Thread.sleep(50);
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
								hmapCOUNT.put(frame.getSrcAddr(), a);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap1.put(a, st);
								a++;
							} else if (frame.getSrcAddr() == node_list[2]) {
								hmapCOUNT.put(frame.getSrcAddr(), b);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap2.put(b, st);
								b++;
							}
						} else if (str.charAt(0) == 'E') {
							if (hmapCOUNT.get(frame.getSrcAddr()) == 10) {
								int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
										| Frame.SRC_ADDR_16;
								final Frame testFrame = new Frame(frameControl);
								testFrame.setDestPanId(COMMON_PANID);
								testFrame.setDestAddr(frame.getSrcAddr());
								testFrame.setSrcAddr(ADDR_NODE2);
								testFrame.setPayload("ACK".getBytes());
								try {
									fio.transmit(testFrame);
									Thread.sleep(50);
								} catch (Exception e) {
								}
								if (frame.getSrcAddr() == node_list[1]) {
									for (int i = 1; i <= 10; i++) {
										String s = hmap1.get(i);
										String msg = "#" + s + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length()); //
											usart.flush(); //
											Thread.sleep(50);
										} catch (Exception e) {
										}
									}
									a = 1;
									hmap1.clear();
									try {
										singleNodeSense(node_list[1], fio);
									} catch (Exception e) {
									}
								} else if (frame.getSrcAddr() == node_list[2]) {
									for (int i = 1; i <= 5; i++) {
										String s = hmap2.get(i);
										String msg = "#" + s + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length()); //
											usart.flush(); //
											Thread.sleep(50);
										} catch (Exception e) {
										}
									}
									b = 1;
									hmap2.clear();
									try {
										singleNodeSense(node_list[2], fio);
									} catch (Exception e) {
									}
								}
								hmapCOUNT.put(frame.getSrcAddr(), 0);
							} else {
								System.out.println("NACK");
								int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN
										| Frame.SRC_ADDR_16;
								final Frame testFrame = new Frame(frameControl);
								testFrame.setSequenceNumber(0);
								testFrame.setDestPanId(COMMON_PANID);
								testFrame.setDestAddr(frame.getSrcAddr());
								testFrame.setSrcAddr(ADDR_NODE2);
								testFrame.setPayload("NACK".getBytes());
								try {
									fio.transmit(testFrame);
									Thread.sleep(50);
								} catch (Exception e) {
								}
								if (frame.getSrcAddr() == node_list[1]) {
									a = 1;
								} else if (frame.getSrcAddr() == node_list[2]) {
									b = 1;
								}
							}
						}
					} catch (Exception e) {
					}
				}
			}
		};
		receive.start();
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
		try {
			startUSART();
			out = usart.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		runs();
	}
}