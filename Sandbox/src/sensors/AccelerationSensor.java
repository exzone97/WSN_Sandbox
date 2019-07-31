package sensors;

import java.util.Arrays;

import com.virtenio.driver.device.ADXL345;
import com.virtenio.driver.gpio.GPIO;
import com.virtenio.driver.gpio.NativeGPIO;
import com.virtenio.driver.spi.NativeSPI;

public class AccelerationSensor {
	private ADXL345 accelerationSensor;
	private GPIO accelCs;

	private String temp;

	public void run() throws Exception {
		accelCs = NativeGPIO.getInstance(20);
		NativeSPI spi = NativeSPI.getInstance(0);
		if(spi.isOpened()) {
			
		}
		else {
			spi.open(ADXL345.SPI_MODE, ADXL345.SPI_BIT_ORDER, ADXL345.SPI_MAX_SPEED);
		}
		accelerationSensor = new ADXL345(spi, accelCs);
		if(accelerationSensor.isOpened()) {
			
		}
		else {
			accelerationSensor.open();
			accelerationSensor.setDataFormat(ADXL345.DATA_FORMAT_RANGE_2G);
			accelerationSensor.setDataRate(ADXL345.DATA_RATE_3200HZ);
			accelerationSensor.setPowerControl(ADXL345.POWER_CONTROL_MEASURE);
		}
		short[] values = new short[3];	
		accelerationSensor.getValuesRaw(values, 0);
		temp = "A:"+Arrays.toString(values);
	}
	public String getTemp(){
		return this.temp;
	}
}