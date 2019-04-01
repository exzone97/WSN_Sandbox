import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.vm.Time;

import java.util.HashMap;
import com.virtenio.driver.device.at86rf231.*;

public class nodeSensor {

	private int COMMON_CHANNEL = 16;
	private int COMMON_PANID = 0xCAFF;
	private int[] node_list = new int[] { 0xABFE, 0xDAAA, 0xDAAB, 0xDAAC, 0xDAAD, 0xDAAE };

	private int ADDR_NODE1 = node_list[0]; // NODE DIATASNYA
	private int ADDR_NODE2 = node_list[1]; // NODE DIRINYA
	private sensing s = new sensing();
	private int sn = 1;
	private static HashMap<Integer, Frame> hmap = new HashMap<Integer, Frame>();
//	private String message;
	private long end;
	private boolean isSensing = false;
//	private boolean exit = false;

	public void receiver_sender() throws Exception {
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
//						dpt time dr basestation dan set waktu dirinya.
						if(str.charAt(0)=='T') {
							String tm = str.substring(1);
							long currTime = Long.parseLong(tm);
							Time.setCurrentTimeMillis(currTime);
						}
						else if(str.equalsIgnoreCase("EXIT")) {
							System.out.println(str);
							try {
								radio.reset();
								radio.setChannel(COMMON_CHANNEL);
								radio.setPANId(COMMON_PANID);
								radio.setShortAddress(ADDR_NODE2); // receiver
							}
							catch(Exception e) {
							}
							hmap.clear();
							isSensing = false;
//							exit = true;
							break;
						}
						else if(str.equalsIgnoreCase("WAKTU")) {
							System.out.println(str);
							boolean isOK = false;
							while (!isOK) {
								try {
									String message = "Time " + Integer.toHexString(ADDR_NODE2) +" "+ Time.currentTimeMillis();
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
											| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(ADDR_NODE1);
									frame.setDestPanId(COMMON_PANID);
									radio.setState(AT86RF231.STATE_TX_ARET_ON);
									frame.setPayload(message.getBytes());
									System.out.println(message);
									radio.transmitFrame(frame);
									isOK = true;
								} catch (Exception e) {
								}
							}
						}
						else if (str.equalsIgnoreCase("ON")) {
							boolean isOK = false;
							while (!isOK) {
								try {
									String message = "Node " + Integer.toHexString(ADDR_NODE2) + " ONLINE";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
											| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(ADDR_NODE1);
									frame.setDestPanId(COMMON_PANID);
									radio.setState(AT86RF231.STATE_TX_ARET_ON);
									frame.setPayload(message.getBytes());
									System.out.println(message);
									radio.transmitFrame(frame);
									isOK = true;
								} catch (Exception e) {
								}
							}
						} else if (str.equalsIgnoreCase("DETECT")) {
							System.out.println(str);
//							end = System.currentTimeMillis() + 30000;
							end = Time.currentTimeMillis() + 35000;
							int i = 0;
							String message = "";
							while (i <= 15 ) {
								boolean isOK = false;
								while (!isOK) {
									try {
										if (i == 15) {
											message = "END";
										} else {
											message = "SENSE " +Integer.toHexString(ADDR_NODE2)+" "+ sn +" "+Time.currentTimeMillis()+" "+ s.sense();
										}
										Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
												| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
										frame.setSrcAddr(ADDR_NODE2);
										frame.setSequenceNumber(sn);
										frame.setSrcPanId(COMMON_PANID);
										frame.setDestAddr(ADDR_NODE1);
										frame.setDestPanId(COMMON_PANID);
										radio.setState(AT86RF231.STATE_TX_ARET_ON);
										frame.setPayload(message.getBytes());
										hmap.put(i, frame);
										System.out.println(message);
										radio.transmitFrame(frame);
										isOK = true;
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								sn++;
								i++;
								isSensing = true;
							}
						} else {
							if (str.equalsIgnoreCase("ACK")) {
								System.out.println(str);
								hmap.clear();
								isSensing = false;
							} else {
								System.out.println(str);
								for (int j = 0; j < hmap.size() ; j++) {
									boolean isOK = false;
									while (!isOK) {
										try {
											Frame frame = hmap.get(j);
											radio.setState(AT86RF231.STATE_TX_ON);
											radio.transmitFrame(frame);
											isOK = true;
											Thread.sleep(1000);
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
			if (isSensing == true) {
				if (Time.currentTimeMillis() > end) {
					System.out.println("Timeout");
					for (int i = 0; i <hmap.size(); i++) {
						boolean isOK = false;
						while (!isOK) {
							try {
								Frame frame = hmap.get(i);
								radio.setState(AT86RF231.STATE_TX_ON);
								radio.transmitFrame(frame);
								isOK = true;
							} catch (Exception e) {
							}
						}
						Thread.sleep(1000);
					}
//					end = System.currentTimeMillis()+30000;
					end = Time.currentTimeMillis()+35000;
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new nodeSensor().receiver_sender();
	}
}
