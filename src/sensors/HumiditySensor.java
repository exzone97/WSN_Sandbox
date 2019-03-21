package sensors;

import com.virtenio.driver.device.SHT21;
import com.virtenio.driver.i2c.NativeI2C;

public class HumiditySensor {
	private SHT21 sht21;
	
	private String temp;

	public void run(NativeI2C i2c) throws Exception {
		sht21 = new SHT21(i2c);
		if(sht21.isOpened()) {
			
		}
		else {
			sht21.open();
			sht21.setResolution(SHT21.RESOLUTION_RH12_T14);
		}
		sht21.startRelativeHumidityConversion();
		Thread.sleep(100);
		int rawRH = sht21.getRelativeHumidityRaw();
		float rh = SHT21.convertRawRHToRHw(rawRH);
		this.temp = "H: "+ rh;
	}
	
	public String getTemp() {
		return this.temp;
	}
}