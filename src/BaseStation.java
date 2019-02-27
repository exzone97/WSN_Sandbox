//import com.virtenio.preon32.examples.common.Misc;
import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.io.Console;
import com.virtenio.driver.device.at86rf231.*;

public class BaseStation extends Thread{

	private static int COMMON_CHANNEL = 24;
	private static int COMMON_PANID = 0xCAFF;
	private static int [] node_list  = new int [] {0xABFE, 0xDAAA, 0xDAAB};
	
//	private int ADDR_NODE1 = node_list[1]; //NODE DIBAWAHNYA
	private static int ADDR_NODE2 = node_list[0]; //NODE DIRINYA (BS)
//	private static int BROADCAST = 0xFFFF;

	public static void pSender() throws Exception{
		final AT86RF231 radio = RadioInit.initRadio();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2);
		
		Console console = new Console();
		while(true) {
			System.out.println("1. Check Online Node");
			System.out.println("2. Sense");
			String mode = console.readLine("\n");
			int temp = Integer.parseInt(mode);
			if(temp == 1) {
//				for(int i = 1;i<node_list.length;i++) {
					boolean isOK = false;
					while(!isOK) {
						try {
							String message = 1+"";
							Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
									| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
							frame.setSrcAddr(ADDR_NODE2);
							frame.setSrcPanId(COMMON_PANID);
							frame.setDestAddr(node_list[2]);
							frame.setDestPanId(COMMON_PANID);
							radio.setState(AT86RF231.STATE_TX_ARET_ON);
							frame.setPayload(message.getBytes());
							radio.transmitFrame(frame);
							isOK = true;
						}
						catch(Exception e) {
							e.printStackTrace();
						}
					}
//				}
			}
			else {
//				for(int i = 1;i<node_list.length;i++) {
					boolean isOK = false;
					while(!isOK) {
						try {
							String message = "SENSE";
							Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
									| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
							frame.setSrcAddr(ADDR_NODE2);
							frame.setSrcPanId(COMMON_PANID);
							frame.setDestAddr(node_list[2]);
							frame.setDestPanId(COMMON_PANID);
							radio.setState(AT86RF231.STATE_TX_ARET_ON);
							frame.setPayload(message.getBytes());
							radio.transmitFrame(frame);
							System.out.println("SEND: " + message);
							isOK = true;
						}
						catch(Exception e) {
							e.printStackTrace();
						}
					}
//				}
			}
			pReceiver();
		}
	}
	
	public static void pReceiver() throws Exception {
		final AT86RF231 radio = RadioInit.initRadio();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2);
		
		Thread reader = new Thread() {
			public void run() {				
				while(true) {
					Frame f = null;
					try {
						f = new Frame();
						radio.setState(AT86RF231.STATE_RX_AACK_ON);
						radio.waitForFrame(f);
					}
					catch(Exception e) {
						e.printStackTrace();
					}
					if(f!=null) {
						byte[] dg = f.getPayload();
						String str = new String(dg, 0, dg.length);
						String hex_addr = Integer.toHexString((int) f.getSrcAddr());
						System.out.println("FROM(" + hex_addr + "): " + str);
					}
				}
			}
		};	
		reader.start();
	}
	
	public static void main(String[] args) throws Exception{
//		new BaseStation().pSender();
		pSender();
	}
}