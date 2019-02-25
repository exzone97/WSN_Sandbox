package sensors;

import com.virtenio.driver.device.MPL115A2;
import com.virtenio.driver.gpio.GPIO;
import com.virtenio.driver.gpio.NativeGPIO;
import com.virtenio.driver.i2c.NativeI2C;

public class PressureSensor {
	private MPL115A2 pressureSensor;
	
	private boolean isInit = false;
	private String temp;
	
	public void init(NativeI2C i2c) throws Exception {
		GPIO resetPin = NativeGPIO.getInstance(24);
		GPIO shutDownPin = NativeGPIO.getInstance(12);
		pressureSensor = new MPL115A2(i2c, resetPin, shutDownPin);

		pressureSensor.open();
		pressureSensor.setReset(false);
		pressureSensor.setShutdown(false);
		isInit = true;
	}

	public void run(NativeI2C i2c) throws Exception {
		if(isInit == false) {
			init(i2c);
		}
		else {
			pressureSensor.startBothConversion();
			Thread.sleep(MPL115A2.BOTH_CONVERSION_TIME);
			int pressurePr = pressureSensor.getPressureRaw();
			int tempRaw = pressureSensor.getTemperatureRaw();
			float pressure = pressureSensor.compensate(pressurePr, tempRaw);
			temp = "P: " + pressure;
			Thread.sleep(1000 - MPL115A2.BOTH_CONVERSION_TIME);
		}
	}
	
	public String getTemp() {
		return this.temp;
	}
}