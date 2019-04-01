import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;

import java.util.HashMap;
import com.virtenio.driver.device.at86rf231.*;

public class ClusterHead extends Thread {
	private static int COMMON_CHANNEL = 16;
	private static int COMMON_PANID = 0xCAFF;
	private static int[] node_list = new int[] { 0xABFE, 0xDAAA, 0xEAAA, 0xDAAB, 0xDAAC, 0xEAAB };

	private static int ADDR_NODE1 = node_list[0]; // NODE DIATASNYA
	private static int ADDR_NODE2 = node_list[6]; // NODE DIRINYA
	
//	Node dibwh CH1
	private static int[] ADDR_NODE3 = new int[] { 0xDAAB, 0xDAAC }; // NODE DIBAWAHNYA
//	Node dibwh CH2
//	private static int ADDR_NODE3 = 0xEAAB; //NODE DIBAWAHNYA

	private static sensing s = new sensing();
	private static int sn = 1;
	private static long end;
	private static boolean firstSense = false;
	private static boolean isSend = false;
	private static boolean exit = false;

	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>();
	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();
	
	private static HashMap<Integer, Frame> hmap1 = new HashMap<Integer, Frame>();
	private static HashMap<Integer, Frame> hmap2 = new HashMap<Integer, Frame>();
	
//	private static HashMap<Integer, Frame> hmap3 = new HashMap<Integer, Frame>();
	
//	Count SN untuk node dibwh CH
	private static int a = 1;
	private static int b = 1;
//	private static int c = 1;

	
	public static void receive_send() throws Exception {
		final AT86RF231 radio = RadioInit.initRadio();
		radio.reset();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2); // receiver

		Thread reader = new Thread() {
			@Override
			public void run() {
				while (true) {
					Frame f = null;
					try {
						f = new Frame();
						radio.setState(AT86RF231.STATE_RX_AACK_ON);
						radio.waitForFrame(f);
					} catch (Exception e) {
					}
					if (f != null) {
						byte[] dg = f.getPayload();
						String str = new String(dg, 0, dg.length);
						if(str.equalsIgnoreCase("EXIT")) {
							exit = true;
							hmapCOUNT.clear();
							a = 1;
							b = 1;
							hmap.clear();
							hmap1.clear();
							hmap2.clear();
							break;
						}
						else if (str.equalsIgnoreCase("ON") && exit == false) {
							boolean isOK = false;
							while (!isOK) {
								try {
									String message = "Node " + Integer.toHexString(ADDR_NODE2) + "ONLINE";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
											| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(ADDR_NODE1);
									frame.setDestPanId(COMMON_PANID);
									radio.setState(AT86RF231.STATE_TX_ON);
									frame.setPayload(message.getBytes());
									radio.transmitFrame(frame);
									isOK = true;
								} catch (Exception e) {
								}
							}
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								boolean isOK2 = false;
								while (!isOK2) {
									try {
										String message = "ON";
										Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
												| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
										frame.setSrcAddr(ADDR_NODE2);
										frame.setSrcPanId(COMMON_PANID);
										frame.setDestAddr(ADDR_NODE3[i]);
										frame.setDestPanId(COMMON_PANID);
										radio.setState(AT86RF231.STATE_TX_ON);
										frame.setPayload(message.getBytes());
										radio.transmitFrame(frame);
										isOK2 = true;
									} catch (Exception e) {
									}
								}
							}
						} else if (str.equalsIgnoreCase("DETECT") && exit == false && firstSense == false) {
							end = System.currentTimeMillis()+30000;
							
//							Sense Cluster Head 15x dan Simpan di HashMap
							for (int i = 0; i < 15; i++) {
								try {
									String message = "SENSE " +Integer.toHexString(ADDR_NODE2)+" "+ sn +" "+Time.currentTimeMillis()+" "+ s.sense();
									hmap.put(i, message);
								} catch (Exception e) {
								}
								sn++;
							}
//							Nerusin "DETECT" ke node dibawahnya
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								boolean isOK = false;
								while (!isOK) {
									try {
										String message = "DETECT";
										Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
												| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
										frame.setSrcAddr(ADDR_NODE2);
										frame.setSrcPanId(COMMON_PANID);
										frame.setDestAddr(ADDR_NODE3[i]);
										frame.setDestPanId(COMMON_PANID);
										radio.setState(AT86RF231.STATE_TX_ON);
										frame.setPayload(message.getBytes());
										radio.transmitFrame(frame);
										isOK = true;
									} catch (Exception e) {
									}
								}
							}
							firstSense = true;
						} else {
//							Kalau dapat Str akhiran "E" berarti dpt dari node sensor di bawahnya yang ONLINE, langsung kirim ke BS
							if (str.charAt(str.length() - 1) == 'E') {
								boolean isOK = false;
								while (!isOK) {
									try {
										String message = str;
										Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
												| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
										frame.setSrcAddr(ADDR_NODE2);
										frame.setSrcPanId(COMMON_PANID);
										frame.setDestAddr(ADDR_NODE1);
										frame.setDestPanId(COMMON_PANID);
//										radio.setState(AT86RF231.STATE_TX_ON);
										radio.setState(AT86RF231.STATE_TX_ARET_ON);
										frame.setPayload(message.getBytes());
										radio.transmitFrame(frame);
										isOK = true;
									} catch (Exception e) {
									}
								}
							}
//							Kalau dapat str awalan 'S' Berarti dpt hasil sense dr node di bwhnya mskin ke hmap buat cadangan
							if (str.charAt(0) == 'S') {
//								buat ngitung berapa kali sensing yang udah dilakuin sama node sensornya
								if(f.getSrcAddr() == node_list[3]) {
									hmapCOUNT.put(f.getSrcAddr(), a);
									hmap1.put(a, f);
									a++;
								}
								else if(f.getSrcAddr() == node_list[4]) {
									hmapCOUNT.put(f.getSrcAddr(),b);
									hmap2.put(b, f);
									b++;
								}
//								else if(f.getSrcAddr() == node_list[5]) {
//									hmapCOUNT.put(f.getSrcAddr(),c);
//									hmap3.put(c, f);
//									c++;
//								}
							}
							if (str.charAt(0) == 'E' && hmapCOUNT.get(f.getSrcAddr()) == 15) {
								boolean isOK = false;
								while (!isOK) {
									try {
										String message = "ACK";
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
							}
							else if (str.charAt(0) == 'E' && hmapCOUNT.get(f.getSrcAddr()) != 15) {
								boolean isOK = false;
								while (!isOK) {
									try {
										String message = "NACK";
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
								if (f.getSrcAddr() == ADDR_NODE3[0]) {
									a = 1;
								} else if (f.getSrcAddr() == ADDR_NODE3[1]) {
									b = 1;
								}
							}
//							dpt ack/nack dr bs
							if (str.equalsIgnoreCase("ACK")) {
								hmap.clear();
								hmap1.clear();
								hmap2.clear();
//								hmap3.clear();
								isSend = false;
							} else {
								for (int i = 1; i <= hmap.size(); i++) {
									boolean isOK = false;
									while (!isOK) {
										try {
											String message = hmap.get(i);
											Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
													| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
											frame.setSrcAddr(ADDR_NODE2);
											frame.setSrcPanId(COMMON_PANID);
											frame.setDestAddr(ADDR_NODE1);
											frame.setDestPanId(COMMON_PANID);
											radio.setState(AT86RF231.STATE_TX_ON);
											frame.setPayload(message.getBytes());
											radio.transmitFrame(frame);
											isOK = true;
										} catch (Exception e) {
										}
									}
								}
							}
						}
					}
				}
			}
		};
		reader.start();
		while (reader.isAlive()) {
//			Kalau semua data udah ada di hashmap, kirim ke BS.
			boolean AllClear = true;
			for(int i = 0;i<ADDR_NODE3.length;i++) {
				if(hmapCOUNT.get(ADDR_NODE3[i])!=15) {
					AllClear = false;
				}
			}
			if(AllClear == true && isSend == false) {
				for(int i = 1;i<=hmap.size();i++) {
					boolean isOK = false;
					while (!isOK) {
						try {
							String message = hmap.get(i);
							Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
									| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
							frame.setSrcAddr(ADDR_NODE2);
							frame.setSrcPanId(COMMON_PANID);
							frame.setDestAddr(ADDR_NODE1);
							frame.setDestPanId(COMMON_PANID);
							radio.setState(AT86RF231.STATE_TX_ON);
							frame.setPayload(message.getBytes());
							radio.transmitFrame(frame);
							isOK = true;
						} catch (Exception e) {
						}
					}
				}
				isSend = true;
				hmapCOUNT.clear();
			}
//			TIMEOUT
//			if (isSensing == true) {
//				if (System.currentTimeMillis() > end) {
////					System.out.println("TIME OUT NO ACK !! RESEND !");
//					for (int i = 1; i <= hmap.size(); i++) {
//						boolean isOK = false;
//						while (!isOK) {
//							try {
//								message = hmap.get(i);
//								Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
//										| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
//								frame.setSrcAddr(ADDR_NODE2);
//								frame.setSrcPanId(COMMON_PANID);
//								frame.setDestAddr(ADDR_NODE1); // TUJUAN
//								frame.setDestPanId(COMMON_PANID);
//								radio.setState(AT86RF231.STATE_TX_ON);
//								frame.setPayload(message.getBytes());
//								radio.transmitFrame(frame);
//								isOK = true;
//							} catch (Exception e) {
//							}
//						}
//					}
//				}
//			}
		}
	}

	public static void main(String[] arg) throws Exception {
		receive_send();
	}
}
