import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;

import java.util.HashMap;
import com.virtenio.driver.device.at86rf231.*;

public class ClusterHead extends Thread {
	private static int COMMON_CHANNEL = 24;
	private static int COMMON_PANID = 0xCAFF;
	private static int[] node_list = new int[] { 0xABFE, 0xDAAA, 0xDAAB, 0xDAAC, 0xDAAD, 0xDAAE, 0xEAAA };

	private static int ADDR_NODE1 = node_list[0]; // NODE DIATASNYA
	private static int ADDR_NODE2 = node_list[6]; // NODE DIRINYA
	
//	CH1
	private static int[] ADDR_NODE3 = new int[] { 0xDAAA, 0xDAAB }; // NODE DIBAWAHNYA
//	CH2
//	private static int ADDR_NODE3 = 0xDAAC; //NODE DIBAWAHNYA
	
	private static String message;
	private static sensing s = new sensing();
	private static int sn = 1;
	private static int count = 1;
	private static long end;
	private static boolean isSensing = false;
	private static boolean isSend = false;

	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>();
	private static HashMap<Long, Integer> hmapCOUNT = new HashMap<Long, Integer>();
	private static HashMap<Long, Boolean> hmapACK = new HashMap<Long, Boolean>();
	
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
						if (str.equalsIgnoreCase("ON")) {
//							Dpt "ON" dari BS, Send ke BS dirinya ON
							boolean isOK = false;
							while (!isOK) {
								try {
									message = "Node " + Integer.toHexString(ADDR_NODE2) + "ONLINE";
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
//							Nerusin "ON" ke node dibawahnya
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								boolean isOK2 = false;
								while (!isOK2) {
									try {
										message = "ON";
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
						} else if (str.equalsIgnoreCase("DETECT")) {
							end = System.currentTimeMillis()+30000;
//							Sense Cluster Head 15x dan Simpan di HashMap
							for (int i = 0; i < 15; i++) {
								try {
									message = "SENSE " + sn + " | " +Integer.toHexString(ADDR_NODE2)+" "+ s.sense();
									hmap.put(count, message);
								} catch (Exception e) {
								}
								sn++;
								count++;
							}
//							Nerusin "DETECT" ke node dibawahnya
							for (int i = 0; i < ADDR_NODE3.length; i++) {
								boolean isOK2 = false;
								while (!isOK2) {
									try {
										message = "DETECT";
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
							isSensing = true;
						} else {
//							Kalau dapat Str akhiran "E" berarti dpt dari node sensor di bawahnya yang ONLINE, langsung kirim ke BS
							if (str.charAt(str.length() - 1) == 'E') {
								boolean isOK = false;
								while (!isOK) {
									try {
										message = str;
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
							}
//							Kalau dapat str awalan 'S' Berarti dpt hasil sense dr node di bwhnya mskin ke hmap buat cadangan
							if (str.charAt(0) == 'S') {
//								buat ngitung berapa kali sensing yang udah dilakuin sama node sensornya
								hmapCOUNT.put(f.getSrcAddr(), hmapCOUNT.get(f.getSrcAddr()) + 1);
								hmap.put(count, str);
								count++;
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
								hmapACK.put(f.getSrcAddr(),true);
							}
							else if (str.charAt(0) == 'E' && hmapCOUNT.get(f.getSrcAddr()) != 15) {
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
							}
//							dpt ack/nack dr bs
							if (str.equalsIgnoreCase("ACK")) {
								hmap = new HashMap<Integer, String>();
								hmapACK = new HashMap<Long, Boolean>();
								isSensing = false;
								isSend = false;
							} else {
								for (int i = 1; i <= hmap.size(); i++) {
									boolean isOK = false;
									while (!isOK) {
										try {
											message = hmap.get(i);
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
				if(hmapACK.get(ADDR_NODE3[i])==false) {
					AllClear = false;
				}
			}
			if(AllClear == true && isSend == false) {
				for(int i = 1;i<=hmap.size();i++) {
					boolean isOK = false;
					while (!isOK) {
						try {
							message = hmap.get(i);
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
			}
//			TIMEOUT
			if (isSensing == true) {
				if (System.currentTimeMillis() > end) {
//					System.out.println("TIME OUT NO ACK !! RESEND !");
					for (int i = 1; i <= hmap.size(); i++) {
						boolean isOK = false;
						while (!isOK) {
							try {
								message = hmap.get(i);
								Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
										| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
								frame.setSrcAddr(ADDR_NODE2);
								frame.setSrcPanId(COMMON_PANID);
								frame.setDestAddr(ADDR_NODE1); // TUJUAN
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

	public static void main(String[] arg) throws Exception {
		receive_send();
	}
}
