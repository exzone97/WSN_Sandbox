import com.virtenio.preon32.examples.common.RadioInit;
import com.virtenio.preon32.node.Node;
import com.virtenio.radio.ieee_802_15_4.Frame;

import sensors.AccelerationSensor;
import sensors.HumiditySensor;
import sensors.PressureSensor;
import sensors.TemperatureSensor;

import com.virtenio.driver.device.at86rf231.*;
import com.virtenio.driver.flash.Flash;
import com.virtenio.driver.i2c.I2C;
import com.virtenio.driver.i2c.NativeI2C;

//import com.virtenio.driver.flash.Flash;
//import com.virtenio.driver.flash.FlashSectorEraseStrategy;
//import com.virtenio.preon32.node.Node;
//import com.virtenio.vm.Time;


public class sensing extends Thread{
	
//	import 4 sensor
	TemperatureSensor TS = new TemperatureSensor();
	AccelerationSensor AS = new AccelerationSensor();
	PressureSensor PS = new PressureSensor();
	HumiditySensor HS = new HumiditySensor();

	
//	Inisialisasi NativeI2C yg dipake di Temperature, Humidity, dan Pressure
	NativeI2C i2c = NativeI2C.getInstance(1);
	
	public void sense(int COMMON_CHANNEL, int COMMON_PANID, int ADDR_NODE1, int ADDR_NODE2) throws Exception{
//		INIT FLASH MEMORY
		Flash flash = Node.getInstance().getFlash();
		flash.open();
		flash.eraseChip();
		flash.waitWhileBusy();
		if(i2c.isOpened()) {
			
		}
		else {
			i2c.open(I2C.DATA_RATE_400);
		}
		final AT86RF231 radio = RadioInit.initRadio();
		radio.reset();
		radio.setChannel(COMMON_CHANNEL);
		radio.setPANId(COMMON_PANID);
		radio.setShortAddress(ADDR_NODE2);
		
		int i = 0;
		
		while(i<20) {
			flash.eraseSector(i);
			boolean isOK = false;
			while(!isOK) {
				TS.run(i2c);
				PS.run(i2c);
				AS.run();
				HS.run(i2c);
				
				String message = TS.getTemp()+";";
				message += AS.getTemp()+";";
				message += HS.getTemp()+";";
				message += PS.getTemp();
				
				byte [] b1 = message.getBytes();
				flash.write(i, b1);
				
				Frame frame = new Frame(Frame.TYPE_DATA | Frame.ACK_REQUEST
						| Frame.DST_ADDR_16 | Frame.INTRA_PAN | Frame.SRC_ADDR_16);
				frame.setSequenceNumber(i);
//				System.out.println(frame.getSequenceNumber());
				frame.setSrcAddr(ADDR_NODE2);
				frame.setSrcPanId(COMMON_PANID);
				frame.setDestAddr(ADDR_NODE1);
				frame.setDestPanId(COMMON_PANID);
				radio.setState(AT86RF231.STATE_TX_ARET_ON);
				frame.setPayload(message.getBytes()); //ngasih paket ke frame
				radio.transmitFrame(frame);
				frame.setSequenceNumber(frame.getSequenceNumber() + 1);
//				System.out.println(frame.getSequenceNumber());
				isOK = true;
			}
			Thread.sleep(1000);
			i++;
		}
	}
}