package sensors;

import com.virtenio.driver.device.ADT7410;
import com.virtenio.driver.i2c.NativeI2C;

public class TemperatureSensor {
	private ADT7410 temperatureSensor;
	
	private boolean isInit = false;
	private String temp;
	
	public void init(NativeI2C i2c) throws Exception {
		temperatureSensor = new ADT7410(i2c, ADT7410.ADDR_0, null, null);
		temperatureSensor.open();
		temperatureSensor.setMode(ADT7410.CONFIG_MODE_CONTINUOUS);

		isInit = true;
	}

	public void run(NativeI2C i2c) throws Exception {
		if(isInit == false) {
			init(i2c);
		}
		else {
			float celsius = temperatureSensor.getTemperatureCelsius();
			temp = "T: " + celsius + " [°C]";
		}
	}
	
	public String getTemp() {
		return this.temp;
	}
}