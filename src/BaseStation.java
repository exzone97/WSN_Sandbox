
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

public class BaseStation extends Thread {

	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };
	private static int BROADCAST = PropertyHelper.getInt("radio.panid", 0xFFFF);

	private static int ADDR_NODE2 = node_list[0]; // NODE DIRINYA (BS)

	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();
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
						sender(fio);
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

	public static void sender(final FrameIO fio) throws Exception {
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
//						for (int i = 1; i < node_list.length; i++) {
						try {
//								send("EXIT", node_list[i], fio);
							send("EXIT", BROADCAST, fio);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
//						}
						exit = true;
						a = 1;
						b = 1;
						c = 1;
						d = 1;
						e = 1;
						firstSense = false;
						hmapCOUNT.clear();
						hmap1.clear();
						hmap2.clear();
						hmap3.clear();
						hmap4.clear();
						hmap5.clear();
						break;
					} else if (temp == 1) {
//						for (int i = 1; i < node_list.length; i++) {
						try {
//								send("ON", node_list[i], fio);
							send("ON", BROADCAST, fio);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
//						}
					} else if (temp == 2) {
						long currTime = Time.currentTimeMillis();
//						for (int i = 1; i < node_list.length; i++) {
						try {
//								send(("T" + currTime), node_list[i], fio);
							send(("T" + currTime), BROADCAST, fio);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
//						}
					} else if (temp == 3) {
//						for (int i = 1; i < node_list.length; i++) {
						try {
//								send("WAKTU", node_list[i], fio);
							send("WAKTU", BROADCAST, fio);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
//						}
					} else if (temp == 4) {
						firstSense = true;
//						for (int i = 1; i < node_list.length; i++) {
						try {
//								send("DETECT", node_list[i], fio);
							send("DETECT", BROADCAST, fio);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
//						}
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
							} else if (frame.getSrcAddr() == node_list[3]) {
								hmapCOUNT.put(frame.getSrcAddr(), c);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap3.put(c, st);
								c++;
							} else if (frame.getSrcAddr() == node_list[4]) {
								hmapCOUNT.put(frame.getSrcAddr(), d);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap4.put(d, st);
								d++;
							} else if (frame.getSrcAddr() == node_list[5]) {
								hmapCOUNT.put(frame.getSrcAddr(), e);
								byte[] s = frame.getPayload();
								String st = new String(s, 0, s.length);
								hmap5.put(e, st);
								e++;
							}
						} else if (str.charAt(0) == 'E') {
							if (hmapCOUNT.get(frame.getSrcAddr()) == 5) {
								try {
									send("ACK", frame.getSrcAddr(), fio);
								} catch (Exception e) {
									e.printStackTrace();
								}
								if (frame.getSrcAddr() == node_list[1]) {
									for (int i = 1; i <= 5; i++) {
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
										send("DETECT", node_list[1], fio);
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
										send("DETECT", node_list[2], fio);
									} catch (Exception e) {
									}
								} else if (frame.getSrcAddr() == node_list[3]) {
									for (int i = 1; i <= 5; i++) {
										String s = hmap3.get(i);
										String msg = "#" + s + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length()); //
											usart.flush(); //
											Thread.sleep(50);
										} catch (Exception e) {
										}
									}
									c = 1;
									hmap3.clear();
									try {
										send("DETECT", node_list[3], fio);
									} catch (Exception e) {
									}
								} else if (frame.getSrcAddr() == node_list[4]) {
									for (int i = 1; i <= 5; i++) {
										String s = hmap4.get(i);
										String msg = "#" + s + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length()); //
											usart.flush(); //
											Thread.sleep(50);
										} catch (Exception e) {
										}
									}
									d = 1;
									hmap4.clear();
									try {
										send("DETECT", node_list[4], fio);
									} catch (Exception e) {
									}
								} else if (frame.getSrcAddr() == node_list[5]) {
									for (int i = 1; i <= 5; i++) {
										String s = hmap5.get(i);
										String msg = "#" + s + "#";
										try {
											out.write(msg.getBytes(), 0, msg.length());
											usart.flush();
											System.out.println(s);
											Thread.sleep(50);
										} catch (Exception e) {
										}
									}
									e = 1;
									hmap5.clear();
									try {
										send("DETECT", node_list[5], fio);
									} catch (Exception e) {
									}
								}
								hmapCOUNT.put(frame.getSrcAddr(), 0);
							} else {
								try {
									send("NACK", frame.getSrcAddr(), fio);
								} catch (Exception e) {
									e.printStackTrace();
								}
								if (frame.getSrcAddr() == node_list[1]) {
									a = 1;
								} else if (frame.getSrcAddr() == node_list[2]) {
									b = 1;
								} else if (frame.getSrcAddr() == node_list[3]) {
									c = 1;
								} else if (frame.getSrcAddr() == node_list[4]) {
									d = 1;
								} else if (frame.getSrcAddr() == node_list[5]) {
									e = 1;
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