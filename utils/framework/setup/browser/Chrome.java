package framework.setup.browser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriverLogLevel;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import base.BaseSuite;
import framework.input.Configuration;
import io.github.bonigarcia.wdm.WebDriverManager;

public class Chrome extends Browser {

	/**
	 * To set the chromedriver system property for chrome execution.
	 * 
	 * @param driverPath
	 */
	public void setDriver(String driverPath) {
		// for removing driver dependency
		if (driverPath != null && driverPath.trim().length() > 0)
			System.setProperty("webdriver.chrome.driver", driverPath);
		else {
			WebDriverManager.chromedriver().setup();
			BaseSuite.log.info("Chrome Driver setup done using Webdriver manager");
		}
	}

	/**
	 * To get the desired capability for specific settings
	 * 
	 * @param dowloadFolder
	 * @param autoDownload
	 * @param headless
	 * 
	 * @return ChromeOptions object
	 */
	public ChromeOptions getCapabilities(String dowloadFolder, boolean autoDownload, boolean headless) {
		ChromeOptions chromeOptions = new ChromeOptions();

		Map<String, Object> prefs = new HashMap<>();
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
			chromeOptions.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200");

		chromeOptions.setExperimentalOption("prefs", prefs);

		// to disable the location pop up
		chromeOptions.addArguments("--disable-geolocation");

		// to make chrome run in test mode
		chromeOptions.addArguments("test-type");
		chromeOptions.addArguments("disable-infobars");
		chromeOptions.addArguments("--disable-popup-blocking");

		// for accepting security certificate
		// for accepting unsecure ssl
		// Based on HealthCheck/BrokenLink request
		// default false as if no property will return null
		String checkSSLCertificates = Configuration.getProperty("checkSSLCertificates");
		boolean ignoreChromeSSLCerts = !Boolean
				.parseBoolean(checkSSLCertificates == null ? "true" : checkSSLCertificates);
		chromeOptions.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, ignoreChromeSSLCerts);

		// for ignoring render timeout
		chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);

		// for chrome user profile setting if given
		String chromeProfilePath = Configuration.getProperty("userProfilePath");
		if (chromeProfilePath != null && !chromeProfilePath.trim().isEmpty())
			chromeOptions.addArguments("user-data-dir=" + chromeProfilePath);

		BaseSuite.log.debug("Chrome browser options generated and returning");

		return chromeOptions;
	}

	/**
	 * To create the chrome driver with given options and if logs if need or not
	 * 
	 * @param getLogs
	 *        boolean
	 * @param options
	 *        ChromeOptions
	 */
	@Override
	public WebDriver createDriver(boolean getLogs, AbstractDriverOptions<?> options) {
		WebDriver driver;
		/**
		 * Create the service to gather the chromedriver logs
		 * https://www.selenium.dev/documentation/webdriver/browsers/chrome/
		 */
		if (getLogs) {
			File logLocation = new File(generateLogFilePath());
			ChromeDriverService chromeDriverService = new ChromeDriverService.Builder().withLogFile(logLocation)
					.withLogLevel(ChromiumDriverLogLevel.SEVERE).build();

			driver = new ChromeDriver(chromeDriverService, ((ChromeOptions) options));
		} else {
			driver = new ChromeDriver(((ChromeOptions) options));
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
		Map<String, String> chrome = ((Map<String, String>) caps.getCapability("chrome"));
		browserDriverVersion = chrome.get("chromedriverVersion").toString().split(" ")[0];
		return browserDriverVersion;
	}

	@Override
	public String getBrowserUserProfilePath(WebDriver driver) {
		String browserUserProfilePath = Strings.EMPTY;
		// storing the driver capabilities object
		Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
		@SuppressWarnings("unchecked")
		Map<String, String> chrome = ((Map<String, String>) caps.getCapability("chrome"));
		browserUserProfilePath = chrome.get("userDataDir").toString();
		return browserUserProfilePath;
	}

	@Override
	public boolean isBrowserHeadless(WebDriver driver) {
		boolean isChromeHeadless = false;
		// storing the driver capabilities object
		Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
		// check if it's headless
		if (caps.getBrowserName().toString().contains("headless"))
			isChromeHeadless = true;
		return isChromeHeadless;
	}

}
