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

import com.virtenio.driver.device.ADT7410;
import com.virtenio.driver.i2c.NativeI2C;

/**
 * Test den Zugriff auf den Temperatursensor ADT7410 von Analog über I2C.
 * <p/>
 * <b> Datenblatt des Temperatursensors: </b> <a href=
 * "http://www.analog.com/static/imported-files/data_sheets/ADT7410.pdf"
 * target="_blank">
 * http://www.analog.com/static/imported-files/data_sheets/ADT7410.pdf</a>
 * (Stand: 29.03.2011)
 */
public class TemperatureSensorA {
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
			temp = "Temperature : " + celsius + " [°C]";
		}
	}
	
	public String getTemp() {
		return this.temp;
	}
}