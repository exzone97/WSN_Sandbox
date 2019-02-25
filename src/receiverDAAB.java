
import com.virtenio.preon32.examples.common.RadioInit;
//import com.virtenio.preon32.shuttle.Shuttle;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.driver.device.at86rf231.*;

public class receiverDAAB {

	private int COMMON_CHANNEL = 24;
	private int COMMON_PANID = 0xCAFE;
	private int ADDR_NODE1 = 0xAFFE; //Base Station
	private int ADDR_NODE2 = 0xDAAB; //receiver
	
	public void pReceiver() throws Exception{
//		final Shuttle shuttle = Shuttle.getInstance();
		
		final AT86RF231 radio = RadioInit.initRadio();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2); //receiver
		
		System.out.println("ON DAAA");
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
						//RECEIVE THE MESSAGE			
						byte[] dg = f.getPayload();
						String str = new String(dg, 0, dg.length);
						String hex_addr = Integer.toHexString((int) f.getSrcAddr());
//						System.out.println("FROM(" + hex_addr + "): " + str);
						
			
						//Bikin If Else klo str == 1 ini buat check online
						//str == 2 manggil method sense aja
						System.out.println(str);
						if(str.equals("ON")) {
							boolean isOK = false;
							while(!isOK) {
								try {
									String message = ADDR_NODE2 + " ONLINE";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
											| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(ADDR_NODE1);
									frame.setDestPanId(COMMON_PANID);
									radio.setState(AT86RF231.STATE_TX_ARET_ON);
									frame.setPayload(message.getBytes());
									radio.transmitFrame(frame);
									System.out.println("SEND RETURN: " + message);
									isOK = true;
								}
								catch(Exception e) {
									System.out.println("ERROR: no receiver");
								}
							}
						}
						else {
							
						}
						
					}
				}
			}
		};
		reader.start();
	}
	
	public static void main(String[] args) throws Exception{
		new receiverDAAA().pReceiver();
	}
	
}
