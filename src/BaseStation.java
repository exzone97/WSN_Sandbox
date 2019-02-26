//import com.virtenio.preon32.examples.common.Misc;
import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.RadioDriverException;
import com.virtenio.radio.ieee_802_15_4.Frame;

import sensors.AccelerationSensor;
import sensors.HumiditySensor;
import sensors.PressureSensor;
import sensors.TemperatureSensor;

import com.virtenio.io.ChannelBusyException;
import com.virtenio.io.Console;
import com.virtenio.io.NoAckException;
import com.virtenio.driver.device.at86rf231.*;
import com.virtenio.driver.i2c.I2C;
import com.virtenio.driver.i2c.NativeI2C;


public class BaseStation extends Thread{

	private int COMMON_CHANNEL = 24;
	private int COMMON_PANID = 0xCAFE;
	private int ADDR_NODE1 = 0xAFFE; //sender (Base Station)
	private int [] ADDR_NODE2 = new int [] {0xDAAA, 0xDAAB}; //receiver 
	
	public void pSender() throws Exception{
		
		final int jumlahNode = ADDR_NODE2.length;
		
		final AT86RF231 radio = RadioInit.initRadio();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE1);
		
		Console console = new Console();
		
		System.out.println("1. Check online node");
		System.out.println("2. Sense");
		String mode = console.readLine("Mode ?");
		int temp = Integer.parseInt(mode);
		while(temp != 1 | temp !=2) {
			if(temp == 1) {
				for(int j=0; j<jumlahNode; j++) {
					boolean isOK = false;
					while(!isOK) {
						try {
							String message = "ON";
							Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
									| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
							frame.setSrcAddr(ADDR_NODE1);
							frame.setSrcPanId(COMMON_PANID);
							frame.setDestAddr(ADDR_NODE2[j]);
							frame.setDestPanId(COMMON_PANID);
							radio.setState(AT86RF231.STATE_TX_ARET_ON);
							frame.setPayload(message.getBytes());
							radio.transmitFrame(frame);
							System.out.println("SEND: " + message);
							isOK = true;
						}
						catch(Exception e) {
							
						}
					}
				}
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
			else {
				String numberOfSense = console.readLine("How Many Sense?");
				for(int j = 0; j<jumlahNode;j++) {
					boolean isOK = false;
					while(!isOK) {
						String message = numberOfSense;
						Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
								| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
						frame.setSrcAddr(ADDR_NODE1);
						frame.setSrcPanId(COMMON_PANID);
						frame.setDestAddr(ADDR_NODE2[j]);
						frame.setDestPanId(COMMON_PANID);
						radio.setState(AT86RF231.STATE_TX_ARET_ON);
						frame.setPayload(message.getBytes());
						radio.transmitFrame(frame);
//						System.out.println("SEND: " + message);
						isOK = true;
					}
				}
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
		}
	}
	
	public static void main(String[] args) throws Exception{
		new BaseStation().pSender();
	}
}