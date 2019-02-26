import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;

import sensors.AccelerationSensor;
import sensors.HumiditySensor;
import sensors.PressureSensor;
import sensors.TemperatureSensor;

import com.virtenio.driver.device.at86rf231.*;
import com.virtenio.driver.i2c.I2C;
import com.virtenio.driver.i2c.NativeI2C;


public class sensing extends Thread{
	
//	import 4 sensor
	TemperatureSensor TS = new TemperatureSensor();
	AccelerationSensor AS = new AccelerationSensor();
	PressureSensor PS = new PressureSensor();
	HumiditySensor HS = new HumiditySensor();
	
//	Inisialisasi NativeI2C yg dipake di Temperature, Humidity, dan Pressure
	NativeI2C i2c;
	String [] Data;
	
	public void sense(int COMMON_CHANNEL, int COMMON_PANID, int ADDR_NODE1, int ADDR_NODE2, int numberOfSense) throws Exception{
		Data = new String[numberOfSense];
		i2c = NativeI2C.getInstance(1);
		i2c.open(I2C.DATA_RATE_400);
		
		final AT86RF231 radio = RadioInit.initRadio();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2);
		
		int i = 0;

		TS.init(i2c);
		PS.init(i2c);
		AS.init();
		HS.init(i2c);
		
		int temp = numberOfSense;
		while(temp>0) {
			boolean isOK = false;
			while(!isOK) {
				TS.run(i2c);
				PS.run(i2c);
				AS.run();
				HS.run(i2c);
				
				String message = i + TS.getTemp();
				message += AS.getTemp();
				message += HS.getTemp();
				message += PS.getTemp();
				
				Data[i] = message;
				
				Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
						| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
				frame.setSrcAddr(ADDR_NODE2);
				frame.setSrcPanId(COMMON_PANID);
				frame.setDestAddr(ADDR_NODE1);
				frame.setDestPanId(COMMON_PANID);
				radio.setState(AT86RF231.STATE_TX_ARET_ON);
				frame.setPayload(message.getBytes()); //ngasih paket ke frame
				radio.transmitFrame(frame);
				
//				System.out.println(i + TS.getTemp()+PS.getTemp()+HS.getTemp() + AS.getTemp());
				isOK = true;
			
				i++;
			}
			
//			Frame f = null;
//			try {
//				f = new Frame();
//				radio.setState(AT86RF231.STATE_RX_AACK_ON);
//				radio.waitForFrame(f);
//			}
//			catch(Exception e) {
//				
//			}
//			if(f!=null) {
//				byte[] dg = f.getPayload();
//				String str = new String(dg, 0, dg.length);
//				String hex_addr = Integer.toHexString((int) f.getSrcAddr());
//				System.out.println("FROM(" + hex_addr + "): " + str);
//			}
//			Thread.sleep(1000);
//			temp--;
		}
	}
	
//	public static void main(String[] args) throws Exception{
//		new sender().pSender();
//	}
}