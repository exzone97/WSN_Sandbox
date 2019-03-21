import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;
import java.util.HashMap;
import com.virtenio.driver.device.at86rf231.*;

public class nodeSensor {

	private int COMMON_CHANNEL = 24;
	private int COMMON_PANID = 0xCAFF;
	private int[] node_list = new int[] { 0xABFE, 0xDAAA, 0xDAAB, 0xDAAC, 0xDAAD, 0xDAAE };

	private int ADDR_NODE1 = node_list[0]; // NODE DIATASNYA
	private int ADDR_NODE2 = node_list[1]; // NODE DIRINYA
	private sensing s = new sensing();
	private int sn = 1;
	private static HashMap<Integer, Frame> hmap = new HashMap<Integer, Frame>();
	private String message;
	private long end;
	private boolean isSensing = false;

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
						if (str.equalsIgnoreCase("ON")) {
							boolean isOK = false;
							while (!isOK) {
								try {
									message = "Node " + Integer.toHexString(ADDR_NODE2) + " ONLINE";
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
						} else if (str.equalsIgnoreCase("DETECT")) {
							end = System.currentTimeMillis() + 17000;
							int i = 0;
							while (i <= 15) {
								boolean isOK = false;
								while (!isOK) {
									try {
										if (i == 15) {
											message = "END";
										} else {
											message = "SENSE " + " | " +Integer.toHexString(ADDR_NODE2)+" "+ s.sense();
										}
										Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST | Frame.DST_ADDR_16
												| Frame.INTRA_PAN | Frame.SRC_ADDR_16);
										frame.setSrcAddr(ADDR_NODE2);
										frame.setSequenceNumber(sn);
										frame.setSrcPanId(COMMON_PANID);
										frame.setDestAddr(ADDR_NODE1);
										frame.setDestPanId(COMMON_PANID);
										radio.setState(AT86RF231.STATE_TX_ON);
										frame.setPayload(message.getBytes());
										hmap.put(i, frame);
										radio.transmitFrame(frame);
										isOK = true;
									} catch (Exception e) {
									}
								}
								sn++;
								i++;
								isSensing = true;
							}
						} else {
							if (str.equalsIgnoreCase("ACK")) {
								hmap = new HashMap<Integer, Frame>();
								isSensing = false;
							} else {
								for (int j = 0; j < hmap.size() ; j++) {
									boolean isOK = false;
									while (!isOK) {
										try {
											Frame frame = hmap.get(j);
											radio.setState(AT86RF231.STATE_TX_ON);
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
			if (isSensing == true) {
				if (System.currentTimeMillis() > end) {
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
					}
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new nodeSensor().receiver_sender();
	}
}
