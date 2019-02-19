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


import com.virtenio.driver.device.MPL115A2;
import com.virtenio.driver.gpio.GPIO;
import com.virtenio.driver.gpio.NativeGPIO;
import com.virtenio.driver.i2c.NativeI2C;
/**
 * Test den Zugriff auf den Sensor MPL115A2 von Freescale über I2C.
 * 
 * <p/>
 * <b> Datenblatt des Sensors: </b> <a href=
 * "http://cache.freescale.com/files/sensors/doc/data_sheet/MPL115A2.pdf"
 * target="_blank">
 * http://cache.freescale.com/files/sensors/doc/data_sheet/MPL115A2.pdf</a>
 * (Stand: 24.08.2011)
 */
public class PressureSensorA {
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
			temp = "Preasure =" + pressure;
			Thread.sleep(1000 - MPL115A2.BOTH_CONVERSION_TIME);
		}
	}
	
	public String getTemp() {
		return this.temp;
	}
}