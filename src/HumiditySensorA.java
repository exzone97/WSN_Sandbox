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

import com.virtenio.driver.device.SHT21;
import com.virtenio.driver.i2c.NativeI2C;

/**
 * Test den Zugriff auf den Sensor SHT21 von Sensirion über I2C.
 *
 * <p/>
 * <b> Datenblatt des Sensors: </b> <a href=
 * "http://www.sensirion.com/en/pdf/product_information/Datasheet-humidity-sensor-SHT21.pdf"
 * target="_blank">
 * http://www.sensirion.com/en/pdf/product_information/Datasheet
 * -humidity-sensor-SHT21.pdf</a> (Stand: 29.03.2011)
 */
public class HumiditySensorA {
	private SHT21 sht21;
	
	private boolean isInit;
	private String temp;

	public void init(NativeI2C i2c) throws Exception {

		sht21 = new SHT21(i2c);
		sht21.open();
		sht21.setResolution(SHT21.RESOLUTION_RH12_T14);
		
		this.isInit = true;
	}

	public void run(NativeI2C i2c) throws Exception {
		if(isInit == false) {
			init(i2c);
		}
		else {
			// humidity conversion
			sht21.startRelativeHumidityConversion();
			Thread.sleep(100);
			int rawRH = sht21.getRelativeHumidityRaw();
			float rh = SHT21.convertRawRHToRHw(rawRH);

			this.temp = ("Humidity : rawRH=" + rawRH + ", RH=" + rh);

		}
	}
	
	public String getTemp() {
		return this.temp;
	}
}
