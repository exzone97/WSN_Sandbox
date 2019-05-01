
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

public class BS extends Thread {
	private static int COMMON_PANID = PropertyHelper.getInt("radio.panid", 0xCAFF);
	private static int[] node_list = new int[] { PropertyHelper.getInt("radio.panid", 0xABFE),
			PropertyHelper.getInt("radio.panid", 0xDAAA), PropertyHelper.getInt("radio.panid", 0xDAAB),
			PropertyHelper.getInt("radio.panid", 0xDAAC), PropertyHelper.getInt("radio.panid", 0xDAAD),
			PropertyHelper.getInt("radio.panid", 0xDAAE) };

	private static int ADDR_NODE3 = node_list[0]; // NODE DIRINYA (BS)

//	private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAA),
//	PropertyHelper.getInt("radio.panid", 0xDAAB), PropertyHelper.getInt("radio.panid", 0xDAAC),
//	PropertyHelper.getInt("radio.panid", 0xDAAD) };
//=================================================================================================
private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAA),PropertyHelper.getInt("radio.panid", 0xDAAC)};

	private static HashMap<Integer, Integer> hmapSN = new HashMap<Integer, Integer>();
	private static USART usart;
	private static OutputStream out;
	private static boolean exit;
	private static boolean firstSense;

	private static Console console;

	public static void runs() {
		try {
			AT86RF231 t = Node.getInstance().getTransceiver();
			t.open();
			t.setAddressFilter(COMMON_PANID, ADDR_NODE3, ADDR_NODE3, false);
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
//			console = new Console();
//			int temp = console.readInt("Input");
					int temp = 100;
					try {
						temp = usart.read();
					} catch (USARTException e1) {
						e1.printStackTrace();
					}
					if (temp == 0) {
						try {
							for (int i = 0; i < ADDR_NODE2.length; i++) {
								send("EXIT", ADDR_NODE2[i], fio);
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						exit = true;
						firstSense = false;

						break;
					} else if (temp == 1) {
						try {
							for (int i = 0; i < ADDR_NODE2.length; i++) {
								send("ON", ADDR_NODE2[i], fio);
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					} else if (temp == 2) {
						long currTime = Time.currentTimeMillis();
						try {
							for (int i = 0; i < ADDR_NODE2.length; i++) {
								send(("Q" + currTime), ADDR_NODE2[i], fio);
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					} else if (temp == 3) {
						try {
							for (int i = 0; i < ADDR_NODE2.length; i++) {
								send("WAKTU", ADDR_NODE2[i], fio);
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					} else if (temp == 4) {
						firstSense = true;
						try {
							for (int i = 0; i < ADDR_NODE2.length; i++) {
								send("DETECT", ADDR_NODE2[i], fio);
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
//			receive(fio);
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
//						System.out.println("ASD"+str);
						// DPT NODE YANG ONLINE
						if (str.charAt(str.length() - 1) == 'E') {
//							System.out.println(str);
							String msg = "#" + str + "#";
							try {
//								System.out.println(msg);
//								Thread.sleep(500);
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						// DPT WAKTU DR SETIAP NODE
						else if (str.charAt(0) == 'T') {
//							System.out.println(str);
							String msg = "#" + str + "#";
							try {
//								System.out.println(msg);
//								Thread.sleep(500);
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (str.startsWith("SENSE")) {

							int beginNode = str.indexOf('<');
							int beginSN = str.indexOf('>');
							int endSN = str.indexOf('?');
							int node = Integer.parseInt(str.substring(beginNode + 1, beginSN));
							int sn = Integer.parseInt(str.substring(beginSN + 1, endSN));
//							System.out.println(node + " " + (int) frame.getSrcAddr());
//							System.out.println(node + " " + sn);
//							System.out.println(hmapSN.get(node));
							//Nulis sekali.. biar ga duplikat data
							if (hmapSN.get(node) == sn) {
//								System.out.println("Here!");//
//								hmapSN.put(node, sn);
//								System.out.println(str);

								String msg = "#" + str + "#";
								try {
									out.write(msg.getBytes(), 0, msg.length());
									usart.flush();
									Thread.sleep(50);
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								hmapSN.put(node, sn+1);
							}
							send("ACK" + node+"."+sn, frame.getSrcAddr(), fio);//
						}
					} catch (Exception e) {
					}
				}
			}
		};
		receive.start();
	}

	public static void send(String msg, long address, final FrameIO fio) throws Exception {
		int frameControl = Frame.TYPE_DATA | Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.ACK_REQUEST
				| Frame.SRC_ADDR_16;
		final Frame testFrame = new Frame(frameControl);
		testFrame.setDestPanId(COMMON_PANID);
		testFrame.setDestAddr(address);
		testFrame.setSrcAddr(ADDR_NODE3);
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
		for (int i = 1; i < node_list.length; i++) {
			hmapSN.put(node_list[i], 1);
		}
		try {
			startUSART();
			out = usart.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		runs();
	}
}