package framework.input;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import base.BaseSuite;

/**
 * 
 * This class is used to access the framework/global configurations from
 * config.properties file
 *
 */

public class Configuration {

	static Properties prop = new Properties();
	static InputStream input = null;
	static {
		try {

			input = new FileInputStream("config.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			// System.out.println(prop.getProperty("database"));

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static String getProperty(String propertyName) {
		String value = null;
		try {
			value = prop.getProperty(propertyName).trim();
		} catch (Exception e) {
			String logMsg = "Property not found with key - " + propertyName;
			if (BaseSuite.log != null)
				BaseSuite.log.warn(logMsg);
			else
				System.out.println(logMsg);
		}
		return value;
	}

}
