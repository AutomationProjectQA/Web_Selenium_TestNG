package framework.setup.browser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.ChromiumDriverLogLevel;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeDriverService;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import base.BaseSuite;
import framework.input.Configuration;
import io.github.bonigarcia.wdm.WebDriverManager;

public class Edge extends Browser {

	/**
	 * To set the edgedriver system property for edge execution.
	 * 
	 * @param driverPath
	 */
	public void setDriver(String driverPath) {
		// for removing driver dependency
		if (driverPath != null && driverPath.trim().length() > 0)
			System.setProperty("webdriver.edge.driver", driverPath);
		else {
			WebDriverManager.edgedriver().setup();
			BaseSuite.log.info("Edge Driver setup done using Webdriver manager");
		}
	}

	/**
	 * To get the desired capability for specific settings
	 * 
	 * @param dowloadFolder
	 * @param autoDownload
	 * @param headless
	 * 
	 * @return edgeOptions object
	 */
	public EdgeOptions getCapabilities(String dowloadFolder, boolean autoDownload, boolean headless) {
		EdgeOptions edgeOptions = new EdgeOptions();

		Map<String, Object> prefs = new HashMap<>();
		// to use chromium based edge
		// prefs.put(edgeOptions.USE_CHROMIUM, true);

		// to switch off browser notification
		prefs.put("profile.default_content_setting_values.notifications", 2);
		// to disable the password pop up
		prefs.put("credentials_enable_service", false);
		prefs.put("password_manager_enabled", false);

		// to disable the file download safety - keep/discard options
		prefs.put("profile.default_content_settings.popups", 0);
		prefs.put("safebrowsing.enabled", true);

		// for auto download and folder
		// set if auto download is enable
		if (autoDownload) {
			prefs.put("download.default_directory", dowloadFolder);
			prefs.put("download.prompt_for_download", false);
			prefs.put("plugins.always_open_pdf_externally", true);
			// to handle multiple file download notification
			prefs.put("profile.default_content_setting_values.automatic_downloads", 1);
		}

		// for headless(without gui) execution
		if (headless)
			edgeOptions.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200");

		edgeOptions.setExperimentalOption("prefs", prefs);

		// to disable the location pop up
		edgeOptions.addArguments("--disable-geolocation");

		// to make chrome run in test mode
		edgeOptions.addArguments("test-type");
		edgeOptions.addArguments("disable-infobars");
		edgeOptions.addArguments("--disable-popup-blocking");

		// for accepting security certificate
		// for accepting unsecure ssl
		// Based on HealthCheck/BrokenLink request
		// default false as if no property will return null
		String checkSSLCertificates = Configuration.getProperty("checkSSLCertificates");
		boolean ignoreChromeSSLCerts = !Boolean
				.parseBoolean(checkSSLCertificates == null ? "true" : checkSSLCertificates);
		edgeOptions.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, ignoreChromeSSLCerts);

		BaseSuite.log.debug("Edge browser options generated and returning");
		
		return edgeOptions;
	}

	/**
	 * To create the edge driver with given options and if logs if need or not
	 * 
	 * @param getLogs
	 *        boolean
	 * @param options
	 *        EdgeOptions
	 */
	@Override
	public WebDriver createDriver(boolean getLogs, AbstractDriverOptions<?> options) {
		WebDriver driver;
		/**
		 * Create the service to gather the edgedriver logs
		 * https://www.selenium.dev/documentation/webdriver/browsers/edge/#service
		 */
		if (getLogs) {
			File logLocation = new File(generateLogFilePath());
			EdgeDriverService edgeDriverService = new EdgeDriverService.Builder().withLogFile(logLocation)
					.withLoglevel(ChromiumDriverLogLevel.SEVERE).build();

			driver = new EdgeDriver(edgeDriverService, ((EdgeOptions) options));
		} else {
			driver = new EdgeDriver(((EdgeOptions) options));
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
		@SuppressWarnings("unchecked")
		Map<String, String> chrome = ((Map<String, String>) caps.getCapability("msedge"));
		browserDriverVersion = chrome.get("msedgedriverVersion").toString();
		return browserDriverVersion;
	}

	@Override
	public String getBrowserUserProfilePath(WebDriver driver) {
		String browserUserProfilePath = Strings.EMPTY;
		// storing the driver capabilities object
		Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
		@SuppressWarnings("unchecked")
		Map<String, String> chrome = ((Map<String, String>) caps.getCapability("msedge"));
		browserUserProfilePath = chrome.get("userDataDir").toString();
		return browserUserProfilePath;
	}

	@Override
	public boolean isBrowserHeadless(WebDriver driver) {
		boolean isEdgeHeadless = false;
		// storing the driver capabilities object
		Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
		// check if it's headless
		if (caps.getBrowserName().toString().contains("headless"))
			isEdgeHeadless = true;
		return isEdgeHeadless;
	}

}
