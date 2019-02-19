/*
 * Copyright (c) 2011., Virtenio GmbH
 * All rights reserved.
 *
 * Commercial software license.
 * Only for test and evaluation purposes.
 * Use in commercial products prohibited.
 * No distribution without permission by Virtenio.
 * Ask Virtenio for other type of license at info@virtenio.de
 *
 * Kommerzielle Softwarelizenz.
 * Nur zum Test und Evaluierung zu verwenden.
 * Der Einsatz in kommerziellen Produkten ist verboten.
 * Ein Vertrieb oder eine Veröffentlichung in jeglicher Form ist nicht ohne Zustimmung von Virtenio erlaubt.
 * Für andere Formen der Lizenz nehmen Sie bitte Kontakt mit info@virtenio.de auf.
 */

import java.util.Arrays;

import com.virtenio.driver.device.ADXL345;
import com.virtenio.driver.gpio.GPIO;
import com.virtenio.driver.gpio.NativeGPIO;
import com.virtenio.driver.spi.NativeSPI;

/**
 * Example for the Triple Axis Accelerometer sensor ADXL345
 */
public class AccelerationSensorA {
	private ADXL345 accelerationSensor;
//	private GPIO accelIrqPin1;
//	private GPIO accelIrqPin2;
	private GPIO accelCs;

	private boolean isInit = false;
	private String temp = "";
	
	public void init() throws Exception {
//		accelIrqPin1 = NativeGPIO.getInstance(37);
//		accelIrqPin2 = NativeGPIO.getInstance(25);
		accelCs = NativeGPIO.getInstance(20);

		NativeSPI spi = NativeSPI.getInstance(0);
		spi.open(ADXL345.SPI_MODE, ADXL345.SPI_BIT_ORDER, ADXL345.SPI_MAX_SPEED);

		accelerationSensor = new ADXL345(spi, accelCs);
		accelerationSensor.open();
		accelerationSensor.setDataFormat(ADXL345.DATA_FORMAT_RANGE_2G);
		accelerationSensor.setDataRate(ADXL345.DATA_RATE_3200HZ);
		accelerationSensor.setPowerControl(ADXL345.POWER_CONTROL_MEASURE);
		
		isInit = true;
	}

	public void run() throws Exception {
		if(isInit == false) {
			init();	
		}
		else {
			short[] values = new short[3];	
			accelerationSensor.getValuesRaw(values, 0);
			temp = ("Acceleration: " + Arrays.toString(values));
		}
	}
	public String getTemp(){
		return this.temp;
	}
}