import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.io.Console;
import java.util.HashMap;
import com.virtenio.driver.device.at86rf231.*;

public class BaseStationBU extends Thread{

	private static int COMMON_CHANNEL = 24;
	private static int COMMON_PANID = 0xCAFF;
	private static int [] node_list  = new int [] {0xABFE, 0xDAAA};
	
//	private int ADDR_NODE1 = node_list[1]; //NODE DIBAWAHNYA
	private static int ADDR_NODE2 = node_list[0]; //NODE DIRINYA (BS)
 //	private static int BROADCAST = 0xFFFF;
	
	private static HashMap<Long, Integer> hmap = new HashMap<Long, Integer>();
	private static boolean isSensing = false;
	private static int howManySense = 0;
	
	public static void pSender() throws Exception{
		final AT86RF231 radio = RadioInit.initRadio();
		radio.reset();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2);
		
		Console console = new Console();
		while(true) {
			String mode = console.readLine(
					"1. Check Online Node\n"
					+ "2. Sense\n"
					+ "3. Check Data\n"
					+ "4. Get Data\n");
			int temp = Integer.parseInt(mode);
			if(temp == 1) {
				for(int i = 1;i<node_list.length;i++) {
					boolean isOK = false;
					while(!isOK) {
						try {
							String message = "ON";
							Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
									| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
							frame.setSrcAddr(ADDR_NODE2);
							frame.setSrcPanId(COMMON_PANID);
							frame.setDestAddr(node_list[i]);
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
			else if(temp == 2) {
				for(int i = 1;i<node_list.length;i++) {
					boolean isOK = false;
					while(!isOK) {
						try {
							String message = "SENSE";
							Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
									| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
							frame.setSrcAddr(ADDR_NODE2);
							frame.setSrcPanId(COMMON_PANID);
							frame.setDestAddr(node_list[i]);
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
				}
				isSensing = true;
				howManySense++;
			}
			else if(temp==3){
				if(isSensing == true) {
//				if belum sense maka akan error;
					for(int i = 1;i<node_list.length;i++) {
						if(hmap.get((long)node_list[i])==(20*howManySense)) {
							boolean isOK = false;
							while(!isOK) {
								try {
									String message = "ACK";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
											| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(node_list[i]);
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
							System.out.println(Integer.toHexString(node_list[i])+" Lengkap, Kirim ACK");
							hmap.put((long)node_list[i], 0);
						}
						else {
							boolean isOK = false;
							while(!isOK) {
								try {
									String message = "NACK";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
											| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(node_list[i]);
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
							System.out.println(Integer.toHexString(node_list[i])+" Belum Lengkap, Kirim NACK");
						}
					}
				}
				else {
					System.out.println("Belum Pernah Sensing!");
				}
			}
			else {
//				print ke txt
			}
			pReceiver();
		}
	}
	
	public static void pReceiver() throws Exception {
		final AT86RF231 radio = RadioInit.initRadio();
		radio.reset();
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
						if(str.charAt(str.length()-1)=='E') {
							System.out.println("Node "+ hex_addr +" is Online");
						}
						
						if(str.charAt(0)=='S') {
							System.out.println("FROM " + hex_addr + " : " + f.getSequenceNumber()+"|"+str);
							hmap.put(f.getSrcAddr(), hmap.get(f.getSrcAddr())+1);
						}
					}
				}
			}
		};	
		reader.start();
	}
	
	public static void main(String[] args) throws Exception{
		for(int i = 1;i<node_list.length;i++) {
			hmap.put((long) node_list[i],0);
		}
		pSender();
	}
}