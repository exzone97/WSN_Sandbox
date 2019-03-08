import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;
import com.virtenio.io.Console;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import com.virtenio.driver.device.at86rf231.*;

public class BaseStation extends Thread{

	private static int COMMON_CHANNEL = 24;
	private static int COMMON_PANID = 0xCAFF;
	private static int [] node_list  = new int [] {0xABFE, 0xDAAA};
	
//	private int ADDR_NODE1 = node_list[1]; //NODE DIBAWAHNYA
	private static int ADDR_NODE2 = node_list[0]; //NODE DIRINYA (BS)
 //	private static int BROADCAST = 0xFFFF;
	
	private static HashMap<Long, Integer> hmap = new HashMap<Long, Integer>();
	private static HashMap<Long, Boolean> hmapACK = new HashMap<Long, Boolean>();
	private static boolean isSensing = false;
	
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
					+ "3. Get Data\n");
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
				if(isSensing == false) {
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
				}
				else {
					System.out.println("Sudah Pernah Sensing, Silahkan Ambil Data");
				}
				
			}
			else{	
//				Print to TXT
				boolean allClear = false;
				for(int i = 0;i<hmapACK.size();i++) {
					if(hmapACK.get((long)node_list[i])==true) {
						allClear = true;
					}
					else {
						allClear = false;
					}
				}
				if(allClear == true) {
					try {
						File file = new File("data.txt");
						FileOutputStream fos = new FileOutputStream(file);
						ObjectOutputStream oos = new ObjectOutputStream(fos);
						oos.writeObject(hmap);
						oos.flush();
						oos.close();
						fos.close();
					}catch(Exception e) {
						
					}
					isSensing = false;
					hmap = new HashMap<>();
					hmapACK = new HashMap<>();
				}	
				else {
					System.out.println("Ada data yang belum lengkap");
				}
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
						if(str.charAt(0)=='E' && hmap.get(f.getSrcAddr())==20) {
							boolean isOK = false;
							while(!isOK) {
								try {
									String message = "ACK";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
											| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(f.getSrcAddr());
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
							System.out.println(Long.toHexString(f.getSrcAddr())+" Lengkap, Kirim ACK");
							hmap.put(f.getSrcAddr(), 0);
							hmapACK.put(f.getSrcAddr(), true);
						}
						else if(str.charAt(0)=='E' && hmap.get(f.getSrcAddr())!=20) {
							boolean isOK = false;
							while(!isOK) {
								try {
									String message = "NACK";
									Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
											| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
									frame.setSrcAddr(ADDR_NODE2);
									frame.setSrcPanId(COMMON_PANID);
									frame.setDestAddr(f.getSrcAddr());
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
					}
				}
			}
		};	
		reader.start();	
	}
	
	public static void main(String[] args) throws Exception{
		for(int i = 1;i<node_list.length;i++) {
			hmap.put((long) node_list[i],0);
			hmapACK.put((long) node_list[i],false);
		}
		pSender();
	}
}