package framework.setup.browser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.util.Strings;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import framework.input.Configuration;

public abstract class Browser {

	/**
	 * Below Methods to create the driver for execution
	 * 
	 */
	public abstract void setDriver(String driverPath);

	public abstract AbstractDriverOptions<?> getCapabilities(String dowloadFolder, boolean autoDownload,
			boolean headless);

	public abstract WebDriver createDriver(boolean getLogs, AbstractDriverOptions<?> options);

	/**
	 * To generate the browser driver log file path based on the config log path &
	 * browser for
	 * automation logs
	 * 
	 * @return String
	 */
	public String generateLogFilePath() {
		String logFilePath = Strings.EMPTY;

		// for appending date time stamp to logs
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM_dd_yyyy_HH_mm_ss");
		LocalDateTime now = LocalDateTime.now();
		// Formatting LocalDateTime to string
		String dateTimeString = now.format(formatter);

		// log folder from config
		String logFolder = Configuration.getProperty("logsFilePath");

		// browser from config
		String browserName = Configuration.getProperty("browser");

		String logFileFolder = (logFolder != null && !logFolder.trim().isEmpty())
				? logFolder.endsWith("/") ? logFolder : logFolder + "/"
				: "./logs/";

		logFilePath = logFileFolder + browserName + "Logs_" + dateTimeString + ".log";

		return logFilePath;
	}

	/**
	 * To get the browser & driver information
	 * 
	 */
	public abstract String getBrowserName();

	/**
	 * To get the browser version using it's driver object
	 * 
	 * @param driver
	 *        WebDriver object
	 * @return String
	 */
	public String getBrowserVersion(WebDriver driver) {
		// storing the driver capabilities object
		Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
		return caps.getBrowserVersion();
	}

	/**
	 * To get the browser driver version and use wherever needed. i.e Report
	 * environment section
	 * 
	 * @param driver
	 *        WebDriver object
	 * @return String
	 */
	public abstract String getBrowserDriverVersion(WebDriver driver);

	/**
	 * To get the browser user profile path and use wherever needed. i.e Report
	 * environment section
	 * 
	 * @param driver
	 *        WebDriver object
	 * @return String
	 */
	public abstract String getBrowserUserProfilePath(WebDriver driver);

	/**
	 * To get is browser headless or not and use wherever needed. i.e Report
	 * environment section
	 * 
	 * @param driver
	 *        WebDriver object
	 * @return boolean
	 */
	public abstract boolean isBrowserHeadless(WebDriver driver);
}
