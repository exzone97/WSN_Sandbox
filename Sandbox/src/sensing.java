import sensors.AccelerationSensor;
import sensors.HumiditySensor;
import sensors.TemperatureSensor;

import com.virtenio.driver.i2c.I2C;
import com.virtenio.driver.i2c.NativeI2C;

public class sensing extends Thread {

	private TemperatureSensor TS = new TemperatureSensor();
	private AccelerationSensor AS = new AccelerationSensor();
	private HumiditySensor HS = new HumiditySensor();

	private NativeI2C i2c = NativeI2C.getInstance(1);

	public String sense() throws Exception{
		if(i2c.isOpened()) {
			
		}
		else {
			i2c.open(I2C.DATA_RATE_400);
		}
		TS.run(i2c);
		AS.run();
		HS.run(i2c);
		
		String message = TS.getTemp()+"; ";
		message += AS.getTemp()+"; ";
		message += HS.getTemp();
		return message;
	}
}
