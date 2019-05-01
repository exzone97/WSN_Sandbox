
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
import com.virtenio.driver.usart.NativeUSART;
import com.virtenio.driver.usart.USART;
import com.virtenio.driver.usart.USARTException;
import com.virtenio.driver.usart.USARTParams;

public class BS_Testing extends Thread {

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
private static int ADDR_NODE2[] = { PropertyHelper.getInt("radio.panid", 0xDAAA),PropertyHelper.getInt("radio.panid", 0xDAAc)};
//=================================================================================================
	private static USART usart;
	private static OutputStream out;
	private static boolean exit;

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
						receive(fio);
						sender(fio);
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
		while (true) {
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
						send(("T" + currTime), ADDR_NODE2[i], fio);
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
				while (exit != true) {
					try {
						for (int i = 0; i < ADDR_NODE2.length; i++) {
							send("DETECT", ADDR_NODE2[i], fio);
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					Thread.sleep(50);
				}
			}
		}
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
						// DPT NODE YANG ONLINE
						if (str.charAt(str.length() - 1) == 'E') {
							String msg = "#" + str + "#";
							try {
								Thread.sleep(200);
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						// DPT WAKTU DR SETIAP NODE
						else if (str.charAt(0) == 'T') {
							String msg = "#" + str + "#";
							try {
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
								Thread.sleep(200);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (str.charAt(0) == 'S') {
							String msg = "#" + str + "#";
							try {
								out.write(msg.getBytes(), 0, msg.length());
								usart.flush();
//								Thread.sleep(200);
							} catch (Exception e) {
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
		try {
			startUSART();
			out = usart.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		runs();
	}
}