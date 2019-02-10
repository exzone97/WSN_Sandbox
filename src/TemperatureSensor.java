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
import com.virtenio.driver.i2c.I2C;
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
public class TemperatureSensor {
	private NativeI2C i2c;
	private ADT7410 temperatureSensor;
	
//	private int counter;
	private float tempData[];
	
	private void init(int i) throws Exception {
		tempData= new float[i];
		System.out.println("I2C(Init)");
		i2c = NativeI2C.getInstance(1);
		i2c.open(I2C.DATA_RATE_400);

		System.out.println("ADT7410(Init)");
		temperatureSensor = new ADT7410(i2c, ADT7410.ADDR_0, null, null);
		temperatureSensor.open();
		temperatureSensor.setMode(ADT7410.CONFIG_MODE_CONTINUOUS);

		System.out.println("Done(Init)");
	}

	public void run() throws Exception {
		int counter=0;
		int arrayLength = tempData.length;
		init(arrayLength);
		while (arrayLength>0) {
			try {
				int raw = temperatureSensor.getTemperatureRaw();
				float celsius = temperatureSensor.getTemperatureCelsius();
				System.out.println("ADT7410: raw=" + raw + "; " + celsius + " [°C]");
				tempData[counter] = celsius;
				Thread.sleep(1000);
				counter++;
			} catch (Exception e) {
				System.out.println("ADT7410 error");
			}
			arrayLength--;
		}
	}
	public float getTemperature(int i) {
		return (tempData[i]); 
	}


//	public static void main(String[] args) throws Exception {
//		new TemperatureSensor().run();
//	}
}