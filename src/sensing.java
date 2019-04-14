import sensors.AccelerationSensor;
import sensors.HumiditySensor;
//import sensors.PressureSensor;
import sensors.TemperatureSensor;

import com.virtenio.driver.i2c.I2C;
import com.virtenio.driver.i2c.NativeI2C;

public class sensing extends Thread {

	private TemperatureSensor TS = new TemperatureSensor();
	private AccelerationSensor AS = new AccelerationSensor();
//	private PressureSensor PS = new PressureSensor();
	private HumiditySensor HS = new HumiditySensor();

	private NativeI2C i2c = NativeI2C.getInstance(1);

	public String sense() throws Exception{
		if(i2c.isOpened()) {
			
		}
		else {
			i2c.open(I2C.DATA_RATE_400);
		}
		TS.run(i2c);
//		PS.run(i2c);
		AS.run();
		HS.run(i2c);
		
		String message = TS.getTemp()+"; ";
		message += AS.getTemp()+"; ";
		message += HS.getTemp();
//		message += PS.getTemp();
		return message;
	}
}
