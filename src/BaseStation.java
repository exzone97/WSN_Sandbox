import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.preon32.examples.common.USARTConstants;
import com.virtenio.radio.ieee_802_15_4.Frame;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import com.virtenio.driver.device.at86rf231.*;
import com.virtenio.driver.usart.NativeUSART;
import com.virtenio.driver.usart.USART;
import com.virtenio.driver.usart.USARTParams;

public class BaseStation extends Thread {

	private static int COMMON_CHANNEL = 24;
	private static int COMMON_PANID = 0xCAFF;
	private static int[] node_list = new int[] { 0xABFE, 0xDAAA, 0xDAAB, 0xDAAC, 0xDAAD, 0xDAAE };

	private static int ADDR_NODE2 = node_list[0]; // NODE DIRINYA (BS)
	private static int BROADCAST = 0xFFFF;
	
	private static String message;
	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();
	private static HashMap<Integer, Frame> hmap1 = new HashMap<Integer, Frame>();
	private static HashMap<Integer, Frame> hmap2 = new HashMap<Integer, Frame>();
	private static HashMap<Integer, Frame> hmap3 = new HashMap<Integer, Frame>();
	private static HashMap<Integer, Frame> hmap4 = new HashMap<Integer, Frame>();
	private static HashMap<Integer, Frame> hmap5 = new HashMap<Integer, Frame>();
	private static int a,b,c,d,e = 1;
	private static USART usart;
	private static OutputStream out;
	private static int choice;
	private static boolean exit = false;
	
		public static void pSender(int input) throws Exception {
		final AT86RF231 radio = RadioInit.initRadio();
		radio.reset();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2);
		
		
		while (input !=0 ) {
			if(input == 0) {
				exit = true;
				break;
			}
			else if (input == 1) {
				boolean isOK = false;
				while (!isOK) {
					try {
						message = "ON";
						Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
								| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
						frame.setSrcAddr(ADDR_NODE2);
						frame.setSrcPanId(COMMON_PANID);
						frame.setDestAddr(BROADCAST);
						frame.setDestPanId(COMMON_PANID);
						radio.setState(AT86RF231.STATE_TX_ON);
						frame.setPayload(message.getBytes());
						radio.transmitFrame(frame);
						isOK = true;
					} catch (Exception e) {
					}
				}
				break;
			}else if(input == 2) {
				
			}else if(input == 3) {
				
			}else if (input == 4) {
				boolean isOK = false;
				while (!isOK) {
					try {
						message = "DETECT";
						Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
								| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
						frame.setSrcAddr(ADDR_NODE2);
						frame.setSrcPanId(COMMON_PANID);
						frame.setDestAddr(BROADCAST);
						frame.setDestPanId(COMMON_PANID);
						radio.setState(AT86RF231.STATE_TX_ON);
						frame.setPayload(message.getBytes());
						radio.transmitFrame(frame);
						isOK = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				break;
			} 
			else {
				
			}
			pReceiver();
		}
	}

	public static void singleNodeSense(int address) throws Exception {
		final AT86RF231 radio = RadioInit.initRadio();
		radio.reset();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2);
		
		boolean isOK = false;
		while (!isOK) {
			try {
				message = "DETECT";
				Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
						| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
				frame.setSrcAddr(ADDR_NODE2);
				frame.setSrcPanId(COMMON_PANID);
				frame.setDestAddr(address);
				frame.setDestPanId(COMMON_PANID);
				radio.setState(AT86RF231.STATE_TX_ON);
				frame.setPayload(message.getBytes());
				radio.transmitFrame(frame);
				isOK = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void pReceiver() throws Exception {
		out = new BufferedOutputStream(usart.getOutputStream());
		
		final AT86RF231 radio = RadioInit.initRadio();
		radio.reset();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2);

		Thread reader = new Thread() {
			public void run() {
				while (true) {
					Frame f = null;
					try {
						f = new Frame();
						radio.setState(AT86RF231.STATE_RX_AACK_ON);
						radio.waitForFrame(f);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (f != null) {
						byte[] dg = f.getPayload();
						String str = new String(dg, 0, dg.length);
//						String hex_addr = Integer.toHexString((int) f.getSrcAddr());
						if (str.charAt(str.length() - 1) == 'E') {
							String msg = "#"+str+"#";
							try {
								out.write(msg.getBytes(),0,msg.length());
								usart.flush();
							}
							catch(Exception e) {
								e.printStackTrace();
							}
						}
						if (str.charAt(0) == 'S') {
							hmapCOUNT.put(f.getSrcAddr(), hmapCOUNT.get(f.getSrcAddr()) + 1);
							if(f.getSrcAddr()==node_list[1]) {
								hmap1.put(a, f);
								a++;
							}
							else if(f.getSrcAddr()==node_list[2]) {
								hmap2.put(b, f);
								b++;
							}
							else if(f.getSrcAddr()==node_list[3]) {
								hmap3.put(c, f);
								c++;
							}
							else if(f.getSrcAddr()==node_list[4]) {
								hmap4.put(d, f);
								d++;
							}
							else {
								hmap5.put(e, f);
								e++;
							}
						}
						if (str.charAt(0) == 'E' && hmapCOUNT.get(f.getSrcAddr()) == 15) {
							boolean isOK = false;
							while (!isOK) {
								try {
									message = "ACK";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
											| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(f.getSrcAddr());
									frame.setDestPanId(COMMON_PANID);
									radio.setState(AT86RF231.STATE_TX_ON);
									frame.setPayload(message.getBytes());
									radio.transmitFrame(frame);
									isOK = true;
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							hmapCOUNT.put(f.getSrcAddr(), 0);
							if(f.getSrcAddr()==node_list[1]) {
								for(int i = 0;i<hmap1.size();i++) {
									String msg = "#"+hmap1.get(i)+"#";
									try {
										out.write(msg.getBytes(),0,msg.length());
										usart.flush();
									}
									catch(Exception e) {
										e.printStackTrace();
									}
								}
								a = 0;
								hmap1.clear();
							}
							else if(f.getSrcAddr()==node_list[2]) {
								for(int i = 0;i<hmap2.size();i++) {
									String msg = "#"+hmap2.get(i)+"#";
									try {
										out.write(msg.getBytes(),0,msg.length());
										usart.flush();
									}
									catch(Exception e) {
										e.printStackTrace();
									}
								}
								b = 0;
								hmap2.clear();
							}
							else if(f.getSrcAddr()==node_list[3]) {
								for(int i = 0;i<hmap3.size();i++) {
									String msg = "#"+hmap3.get(i)+"#";
									try {
										out.write(msg.getBytes(),0,msg.length());
										usart.flush();
									}
									catch(Exception e) {
										e.printStackTrace();
									}
								}
								c = 0;
								hmap3.clear();
							}
							else if(f.getSrcAddr()==node_list[4]) {
								for(int i = 0;i<hmap4.size();i++) {
									String msg = "#"+hmap4.get(i)+"#";
									try {
										out.write(msg.getBytes(),0,msg.length());
										usart.flush();
									}
									catch(Exception e) {
										e.printStackTrace();
									}
								}
								d = 0;
								hmap4.clear();
							}
							else {
								for(int i = 0;i<hmap5.size();i++) {
									String msg = "#"+hmap5.get(i)+"#";
									try {
										out.write(msg.getBytes(),0,msg.length());
										usart.flush();
									}
									catch(Exception e) {
										e.printStackTrace();
									}
								}
								e = 0;
								hmap5.clear();
							}
						} else if (str.charAt(0) == 'E' && hmapCOUNT.get(f.getSrcAddr()) != 15) {
							boolean isOK = false;
							while (!isOK) {
								try {
									message = "NACK";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
											| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(f.getSrcAddr());
									frame.setDestPanId(COMMON_PANID);
									radio.setState(AT86RF231.STATE_TX_ON);
									frame.setPayload(message.getBytes());
									radio.transmitFrame(frame);
									isOK = true;
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							if(f.getSrcAddr()==node_list[1]) {
								a=0;
							}
							else if(f.getSrcAddr()==node_list[2]) {
								b=0;
							}
							else if(f.getSrcAddr()==node_list[3]) {
								c=0;
							}
							else if(f.getSrcAddr()==node_list[4]) {
								d=0;
							}
							else {
								e=0;
							}
						}
					}
				}
			}
		};
		reader.start();
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
	
	public void reader() throws Exception {
		if (usart != null) {
			choice = usart.read();
			pSender(choice);
			
//			============JGN DI HAPUS==========
//			if(choice == 1) {
//				for(int i = 0;i<100;i++) {
//					String msg = "#AAA A AA AAAAA AAAAAAAAAAAAAA AAAA A"+i+"#";
//					try {
//						out.write(msg.getBytes(),0,msg.length());
//						usart.flush();
//					}
//					catch(IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//			====================================
		}
	}
	
	public void run() {
		new Thread() {
			public void run() {
				try {
					while(true) {
						reader();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public static void main(String[] args){
		for (int i = 1; i < node_list.length; i++) {
			hmapCOUNT.put((long) node_list[i], 0);
		}
		
		try {
			startUSART();
			out = usart.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		new BaseStation().run();
	}
}