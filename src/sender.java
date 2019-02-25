//import com.virtenio.preon32.examples.common.Misc;
import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;

import sensors.AccelerationSensor;
import sensors.HumiditySensor;
import sensors.PressureSensor;
import sensors.TemperatureSensor;

import com.virtenio.io.Console;
import com.virtenio.driver.device.at86rf231.*;
import com.virtenio.driver.i2c.I2C;
import com.virtenio.driver.i2c.NativeI2C;


public class sender extends Thread{

	private int COMMON_CHANNEL = 24;
	private int COMMON_PANID = 0xCAFE;
	private int ADDR_NODE1 = 0xAFFE; //sender
	private int ADDR_NODE2 = 0xBABE; //receiver
	
//	import 4 sensor
	TemperatureSensor TS;
	AccelerationSensor AS;
	PressureSensor PS;
	HumiditySensor HS;
	
//	Inisialisasi NativeI2C yg dipake di Temperature, Humidity, dan Pressure
	NativeI2C i2c;
	
	
	public void pSender() throws Exception{
		i2c = NativeI2C.getInstance(1);
		i2c.open(I2C.DATA_RATE_400);
		
		
		TS = new TemperatureSensor();
		PS = new PressureSensor();
		AS = new AccelerationSensor();
		HS = new HumiditySensor();
		
//		final Shuttle shuttle = Shuttle.getInstance();
		
		final AT86RF231 radio = RadioInit.initRadio();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE1);
		
		Console console = new Console();
		
		int i = 1;

		TS.init(i2c);
		PS.init(i2c);
		AS.init();
		HS.init(i2c);
		
		while(true) {
			String msg = console.readLine("How Many Sense");
//			Parse msg jadi integer
			int temp = Integer.parseInt(msg);
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
						
						Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
								| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
						frame.setSrcAddr(ADDR_NODE1);
						frame.setSrcPanId(COMMON_PANID);
						frame.setDestAddr(ADDR_NODE2);
						frame.setDestPanId(COMMON_PANID);
						radio.setState(AT86RF231.STATE_TX_ARET_ON);
						
						frame.setPayload(message.getBytes()); //ngasih paket ke frame
						radio.transmitFrame(frame);
						
						System.out.println(TS.getTemp()+PS.getTemp()+HS.getTemp() + AS.getTemp());
						isOK = true;
						
						i++;
				}
				
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
				Thread.sleep(1000);
				temp--;
			}
		}
	}
	
	public static void main(String[] args) throws Exception{
		new sender().pSender();
	}
}