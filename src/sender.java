//import com.virtenio.preon32.examples.common.Misc;
import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.radio.ieee_802_15_4.Frame;
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
	TemperatureSensorA TSA;
	AccelerationSensorA ASA;
	PressureSensorA PSA;
	HumiditySensorA HSA;
	
//	Inisialisasi NativeI2C yg dipake di Temperature, Humidity, dan Pressure
	NativeI2C i2c;
	
	
	public void pSender() throws Exception{
		i2c = NativeI2C.getInstance(1);
		i2c.open(I2C.DATA_RATE_400);
		
		
		TSA = new TemperatureSensorA();
		PSA = new PressureSensorA();
		ASA = new AccelerationSensorA();
		HSA = new HumiditySensorA();
		
//		final Shuttle shuttle = Shuttle.getInstance();
		
		final AT86RF231 radio = RadioInit.initRadio();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE1);
		
		Console console = new Console();
		
		int i = 1;

		TSA.init(i2c);
		PSA.init(i2c);
		ASA.init();
		HSA.init(i2c);
		
		while(true) {
			String msg = console.readLine("How Many Sense");
//			Parse msg jadi integer
			int temp = Integer.parseInt(msg);
			while(temp>0) {
				boolean isOK = false;
				while(!isOK) {
//					try {
						TSA.run(i2c);
						PSA.run(i2c);
						ASA.run();
						HSA.run(i2c);
						
						String message = i + TSA.getTemp();
						message += ASA.getTemp();
						message += HSA.getTemp();
						message += PSA.getTemp();
						System.out.println("a");
//						String message = i + "=" + TSA.getTemp() +  " | "  + "|" + ASA.getTemp() + " | "+ HSA.getTemp();
						Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
								| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
						System.out.println("b");
						frame.setSrcAddr(ADDR_NODE1);
						System.out.println("c");
						frame.setSrcPanId(COMMON_PANID);
						System.out.println("d");
						frame.setDestAddr(ADDR_NODE2);
						System.out.println("e");
						frame.setDestPanId(COMMON_PANID);
						System.out.println("f");
						radio.setState(AT86RF231.STATE_TX_ARET_ON);
						System.out.println("g");
						
						frame.setPayload(message.getBytes()); //ngasih paket ke frame
						System.out.println("h");
						radio.transmitFrame(frame);
						System.out.println("i");
//						System.out.println("(" + i + ") SEND = " + TSA.getTemp() +  " | "  + PSA.getTemp()+ "|" + ASA.getTemp() + " | "+ HSA.getTemp());
//						System.out.println(TSA.getTemp() + HSA.getTemp() + ASA.getTemp() );
						System.out.println(TSA.getTemp()+PSA.getTemp()+HSA.getTemp() + ASA.getTemp());
						System.out.println("j");
						isOK = true;
						System.out.println("k");
	
						i++;
//					}
//					catch(Exception e) {
//						System.out.println("(" + i + ") ERROR: no receiver");
//					}
	
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