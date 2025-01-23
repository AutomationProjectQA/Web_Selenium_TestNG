package framework.setup.browser;

import java.io.File;

import org.apache.logging.log4j.util.Strings;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxDriverService;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.GeckoDriverService;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import base.BaseSuite;
import framework.input.Configuration;
import io.github.bonigarcia.wdm.WebDriverManager;

public class Firefox extends Browser {

	/**
	 * To set the geckodriver system property for firefox execution.
	 * 
	 * @param driverPath
	 * 
	 * @param getLogs
	 *        boolean
	 */
	public void setDriver(String driverPath) {

		// for removing driver dependency
		if (driverPath != null && driverPath.trim().length() > 0) {
			System.setProperty("webdriver.gecko.driver", driverPath);
			BaseSuite.log.info("Firefox Driver is setup for the driver on path -" + driverPath);
		} else {
			WebDriverManager.firefoxdriver().setup();
			BaseSuite.log.info("Firefox Driver setup done using Webdriver manager");
		}

		/**
		 * Deprected settting logs of gecko driver through system property in v4.12.0
		 * https://www.selenium.dev/blog/2023/selenium-4-12-0-released/
		 */
		/*
		 * if (getLogs) {
		 * // for appending date time stemp to logs
		 * DateTimeFormatter formatter =
		 * DateTimeFormatter.ofPattern("MM_dd_yyyy_HH_mm_ss");
		 * LocalDateTime now = LocalDateTime.now();
		 * // Formatting LocalDateTime to string
		 * String dateTimeString = now.format(formatter);
		 * 
		 * // log folder from config
		 * String logFolder = Configuration.getProperty("logsFilePath");
		 * 
		 * String logFileFolder = (logFolder != null && !logFolder.trim().isEmpty())
		 * ? logFolder.endsWith("/") ? logFolder : logFolder + "/"
		 * : "./logs/";
		 * 
		 * System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE,
		 * logFileFolder + "Firefoxlogs_" + dateTimeString + ".txt");
		 * }
		 */
	}

	/**
	 * To get the desired capability for specific settings
	 * 
	 * @param dowloadFolder
	 * @param autoDownload
	 * @param headless
	 * 
	 * @return DesiredCapabilities object
	 */
	public FirefoxOptions getCapabilities(String dowloadFolder, boolean autoDownload, boolean headless) {
		FirefoxOptions firefoxOptions = new FirefoxOptions();
		firefoxOptions.setLogLevel(FirefoxDriverLogLevel.TRACE);
		/*
		 * FirefoxProfile profile = new FirefoxProfile();
		 * profile.setPreference("dom.successive_dialog_time_limit", 0);
		 * profile.setPreference("dom.webnotifications.enabled", false);
		 * 
		 * // set download folder location
		 * // set if auto download is enable
		 * if (autoDownload) {
		 * profile.setPreference("browser.download.folderList", 2);
		 * profile.setPreference("browser.download.manager.showWhenStarting", false);
		 * profile.setPreference("browser.download.dir", dowloadFolder);
		 * profile.setPreference("browser.download.manager.focusWhenStarting", false);
		 * profile.setPreference("browser.download.useDownloadDir", true);
		 * profile.setPreference("browser.helperApps.alwaysAsk.force", false);
		 * profile.setPreference("browser.download.manager.closeWhenDone", true);
		 * profile.setPreference("browser.download.manager.showAlertOnComplete", false);
		 * profile.setPreference("browser.download.manager.useWindow", false);
		 * profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
		 * "application/vnd.ms-excel, application/x-zip-compressed, application/application/text, application/image/tiff, attachment/csv, application/pdf, application/x-www-form-urlencoded, application/xml, text/xml, application/csv, text/csv, application/zip, application/x-msexcel, application/excel, image/jpeg, image/png, image/gif, image/bmp, image/tiff, text/plain, text/html, application/comma-separated-values, application/download, application/force-download, application/octet-stream doc xls pdf txt multipart/encrypted application/application/pdf, charset=utf-8"
		 * );
		 * profile.setPreference("pdfjs.disabled", true);
		 * }
		 * 
		 * // for allowing location
		 * profile.setPreference("permissions.default.geo", 1);
		 */

		firefoxOptions.addPreference("dom.successive_dialog_time_limit", 0);
		firefoxOptions.addPreference("dom.webnotifications.enabled", false);

		// set download folder location
		// set if auto download is enable
		if (autoDownload) {
			firefoxOptions.addPreference("browser.download.folderList", 2);
			firefoxOptions.addPreference("browser.download.manager.showWhenStarting", false);
			firefoxOptions.addPreference("browser.download.dir", dowloadFolder);
			firefoxOptions.addPreference("browser.download.manager.focusWhenStarting", false);
			firefoxOptions.addPreference("browser.download.useDownloadDir", true);
			firefoxOptions.addPreference("browser.helperApps.alwaysAsk.force", false);
			firefoxOptions.addPreference("browser.download.manager.closeWhenDone", true);
			firefoxOptions.addPreference("browser.download.manager.showAlertOnComplete", false);
			firefoxOptions.addPreference("browser.download.manager.useWindow", false);
			firefoxOptions.addPreference("browser.download.alwaysOpenPanel", false);
			firefoxOptions.addPreference("browser.helperApps.neverAsk.saveToDisk",
					"application/ms-excel, application/vnd.ms-excel, application/x-zip-compressed, application/application/text, application/image/tiff, attachment/csv, application/pdf, application/x-www-form-urlencoded, application/xml, text/xml, application/csv, text/csv, application/zip, application/x-msexcel, application/excel, image/jpeg, image/png, image/gif, image/bmp, image/tiff, text/plain, text/html, application/comma-separated-values, application/download, application/force-download, application/octet-stream doc xls pdf txt multipart/encrypted application/application/pdf, charset=utf-8, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			firefoxOptions.addPreference("pdfjs.disabled", true);
		}

		// for headless(without gui) execution
		if (headless)
			firefoxOptions.addArguments("--headless", "--window-size=1920,1200");

		// for allowing location
		firefoxOptions.addPreference("permissions.default.geo", 1);

		// for accepting unsecure ssl
		// Based on HealthCheck/BrokenLink request
		// default false as if no property will return null
		String checkSSLCertificates = Configuration.getProperty("checkSSLCertificates");
		boolean ignoreFirefoxSSLCerts = !Boolean
				.parseBoolean(checkSSLCertificates == null ? "true" : checkSSLCertificates);
		firefoxOptions.setAcceptInsecureCerts(ignoreFirefoxSSLCerts);
		firefoxOptions.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, ignoreFirefoxSSLCerts);

		// add created profile of firefox
		// firefoxOptions.setProfile(profile);
		// for firefox user profile setting if given
		String firefoxProfilePath = Configuration.getProperty("userProfilePath");
		if (firefoxProfilePath != null && !firefoxProfilePath.trim().isEmpty())
			firefoxOptions.addArguments("-profile", firefoxProfilePath);

		// for avoid getting security error for pageXOffset while taking screenshot
		firefoxOptions.addPreference("privacy.trackingprotection.enabled", false);

		BaseSuite.log.debug("Firefox browser options generated and returning");

		return firefoxOptions;
	}

	/**
	 * To create the gecko driver with given options and if logs if need or not
	 * 
	 * @param getLogs
	 *        boolean
	 * @param options
	 *        FirefoxOptions
	 */
	@Override
	public WebDriver createDriver(boolean getLogs, AbstractDriverOptions<?> options) {
		WebDriver driver;
		/**
		 * Create the service to gather the firefoxdriver logs
		 * https://www.selenium.dev/documentation/webdriver/browsers/firefox/#service
		 */
		if (getLogs) {
			File logLocation = new File(generateLogFilePath());
			FirefoxDriverService firefoxLogService = new GeckoDriverService.Builder().withLogFile(logLocation)
					.withLogLevel(FirefoxDriverLogLevel.TRACE).build();

			driver = new FirefoxDriver(firefoxLogService, ((FirefoxOptions) options));
		} else {
			driver = new FirefoxDriver(((FirefoxOptions) options));
		}
		return driver;
	}

	@Override
	public String getBrowserName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getBrowserDriverVersion(WebDriver driver) {
		String browserDriverVersion = Strings.EMPTY;
		// storing the driver capabilities object
		Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
		browserDriverVersion = caps.getCapability("moz:geckodriverVersion").toString();
		return browserDriverVersion;
	}

	@Override
	public String getBrowserUserProfilePath(WebDriver driver) {
		String browserUserProfilePath = Strings.EMPTY;
		// storing the driver capabilities object
		Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
		browserUserProfilePath = caps.getCapability("moz:profile").toString();
		return browserUserProfilePath;
	}

	@Override
	public boolean isBrowserHeadless(WebDriver driver) {
		boolean isFirefoxHeadless = false;
		// storing the driver capabilities object
		Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
		isFirefoxHeadless = Boolean.valueOf(caps.getCapability("moz:headless").toString());
		return isFirefoxHeadless;
	}

}
