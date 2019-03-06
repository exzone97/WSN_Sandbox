import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;
import java.util.HashMap;
import com.virtenio.driver.device.at86rf231.*;

public class nodeSensorBU {

	/**
	 * NODE LIST :
	 * 0xAFFE = BS - COM3
	 * 0xDAAA = Node 1 - COM6
	 * 0xDAAB = Node 2 - COM5
	 */
	
	private int COMMON_CHANNEL = 24;
	private int COMMON_PANID = 0xCAFF;
	private int [] node_list  = new int [] {0xABFE, 0xDAAA, 0xDAAB};
	
	private int ADDR_NODE1 = node_list[0]; //NODE DIATASNYA
	private int ADDR_NODE2 = node_list[1]; //NODE DIRINYA
//	private int ADDR_NODE3; //NODE DIBAWAHNYA
	private sensing s = new sensing();
	private int sn = 1;
	private static HashMap<Integer, String> hmap = new HashMap<Integer, String>();
	private int howManySense;
	
	public void receiver_sender() throws Exception{
		final AT86RF231 radio = RadioInit.initRadio();
		radio.reset();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2); //receiver

		Thread reader = new Thread() {
			@Override
			public void run() {
				while(true) {
					Frame f = null;
					try {
						f = new Frame();
						radio.setState(AT86RF231.STATE_RX_AACK_ON);
						radio.waitForFrame(f);
					}
					catch(Exception e) {
					}
					if(f!=null) {		
						byte[] dg = f.getPayload();
						String str = new String(dg, 0, dg.length);
						if(str.equalsIgnoreCase("ON")) {
							boolean isOK = false;
							while(!isOK) {
								try {
									String message = Integer.toHexString(ADDR_NODE2) + " ONLINE";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
											| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(ADDR_NODE1); //TUJUAN
									frame.setDestPanId(COMMON_PANID);
									radio.setState(AT86RF231.STATE_TX_ARET_ON);
									frame.setPayload(message.getBytes());
									radio.transmitFrame(frame);
//									System.out.println("SEND RETURN: " + message);
									isOK = true;
								}
								catch(Exception e) {
								}
							}
						}
						else if(str.equalsIgnoreCase("SENSE")){
							int i = 0;
							while(i<20) {
								boolean isOK = false;
								while(!isOK) {
									try {
										String message = "SENSE "+s.sense();
										hmap.put(i,message);
										Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
												| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
										frame.setSequenceNumber(sn);
										frame.setSrcAddr(ADDR_NODE2);
										frame.setSrcPanId(COMMON_PANID);
										frame.setDestAddr(ADDR_NODE1); //TUJUAN
										frame.setDestPanId(COMMON_PANID);
										radio.setState(AT86RF231.STATE_TX_ARET_ON);
										frame.setPayload(message.getBytes());
										radio.transmitFrame(frame);
//										System.out.println("SEND RETURN: " + message);
										isOK = true;
									}
									catch(Exception e) {
									}
								}
								sn++;
								i++;
							}
							howManySense++;
						}
						else {
							if(str.equalsIgnoreCase("ACK")) {
								hmap = new HashMap<Integer, String>();
							}
							else{
								for(int i = 0;i<(20*howManySense);i++) {
									boolean isOK = false;
									while(!isOK) {
										try {
											String message = hmap.get(i);
											Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
													| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
											frame.setSequenceNumber(sn);
											frame.setSrcAddr(ADDR_NODE2);
											frame.setSrcPanId(COMMON_PANID);
											frame.setDestAddr(ADDR_NODE1); //TUJUAN
											frame.setDestPanId(COMMON_PANID);
											radio.setState(AT86RF231.STATE_TX_ARET_ON);
											frame.setPayload(message.getBytes());
											radio.transmitFrame(frame);
											isOK = true;
										}
										catch(Exception e) {
										}
									}
								}
							}
//							3. ADA TIMER, KALO ADA GA DPT ACK / NACK = SEND LG DR FLASH MEMORY.
						}
						
					}
				}
			}
		};
		reader.start();
	}
	
	public static void main(String[] args) throws Exception{
		new nodeSensorBU().receiver_sender();
	}
	
}
