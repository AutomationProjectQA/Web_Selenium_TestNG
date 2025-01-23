package framework.setup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.sikuli.script.Screen;

import base.BaseSuite;
import framework.input.Configuration;
import framework.setup.browser.Browser;
import framework.setup.browser.Chrome;
import framework.setup.browser.Edge;
import framework.setup.browser.Firefox;

/**
 * This class takes the configuration and initializes the driver and terminates
 * the driver session
 * 
 * @author bhavikt
 *
 */
public class SetUp {

	// global objects
	public static WebDriver driver;
	// moved from commonactions as happens on every object throughout execution
	public static WebDriverWait wait;
	public static Actions actions;
	public static Screen screen;

	// get the browser based on config
	public Browser browser = null;

	private Logger log = BaseSuite.log;
	public boolean headless;

	/**
	 * allowed browsers for testing
	 * 
	 */
	private enum BrowserType {
		CHROME, FIREFOX, EDGE;
	}

	/**
	 * To validate browser given in config and terminate the execution if browser
	 * given is not in BrowserType.
	 * 
	 * @param browserName
	 */
	private void validateBrowserGiven(String browserName, String browserDriver) {
		if ("FIREFOX".equals(browserName)) {
			// for firefox apart from geckodriver is given
			// not null for key & not empty for value check if config not given for
			// webdrivermanager usage
			if (browserDriver != null && !browserDriver.trim().isEmpty() && !browserDriver.contains("geckodriver")) {
				log.error("For FIREFOX browser the driver must be 'geckodriver'. Given value is - " + browserDriver);
				System.exit(0);
			}
		} else if ("CHROME".equals(browserName)) {
			// for chrome apart from chromedriver is given
			// not null for key & not empty for value check if config not given for
			// webdrivermanager usage
			if (browserDriver != null && !browserDriver.trim().isEmpty() && !browserDriver.contains("chromedriver")) {
				log.error("For CHROME browser the driver must be 'chromedriver'. Given value is - " + browserDriver);
				System.exit(0);
			}
		} else if ("EDGE".equals(browserName)) {
			// for chrome apart from chromedriver is given
			// not null for key & not empty for value check if config not given for
			// webdrivermanager usage
			if (browserDriver != null && !browserDriver.trim().isEmpty() && !browserDriver.contains("edgedriver")) {
				log.error("For EDGE browser the driver must be 'edgedriver'. Given value is - " + browserDriver);
				System.exit(0);
			}
		} else {
			// for browser name apart form firefox and chrome
			log.error(
					"Configuration 'browser' can have 'firefox' or 'chrome' or 'edge' as value in config.properties file. Given value is - "
							+ browserName);
			System.exit(0);
		}
	}

	/**
	 * To create the download folder given in config and return the absolute path of
	 * the same.
	 * 
	 * @param folderPath
	 * 
	 * 
	 * @return String
	 */
	private String createDownloadFolder(String folderPath) {
		// get the path
		Path path = Paths.get(folderPath);
		folderPath = path.toString();

		// generate absolute path
		folderPath = FileSystems.getDefault().getPath(folderPath).normalize().toAbsolutePath().toString();

		// if directory exists?
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
				log.debug("Download folder created at - " + folderPath);
			} catch (IOException e) {
				log.debug("Error while creating the download folder - " + folderPath);
			}
		} else {
			log.debug("Given Download folder already exists at - " + folderPath);
		}

		return folderPath;
	}

	/**
	 * To create the remote webdriver for execution in grid on given node with given
	 * capabilities.
	 * 
	 * @param hub          HTTP url with port
	 * 
	 * @param capabilities Options object
	 * 
	 * @return RemoteWebDriver object
	 */
	private RemoteWebDriver getGridDriver(String hub, Capabilities capabilities) {
		RemoteWebDriver driver = null;

		try {
			driver = new RemoteWebDriver(new URL(hub), capabilities);

			// for upload to identify the local file in docker
			driver.setFileDetector(new LocalFileDetector());
		} catch (MalformedURLException e1) {
			log.fatal("Error creating driver for grid execution", e1);
			System.exit(0);
		} catch (SessionNotCreatedException e1) {
			log.fatal("Grid session not created for hub - '" + hub + "' and capabilities '" + capabilities.toString()
					+ "'", e1);
			System.exit(0);
		}
		log.debug("Created remote driver for grid execution on - " + hub);

		return driver;
	}

	/**
	 * To set the zalenium capabilites
	 * 
	 * @param caps      firefoxOptions or chromeOptions to add the capabilities
	 * 
	 * @param suiteName suite name for test name to be shown in zalenium
	 * 
	 * @return MutableCapabilites parent object of options
	 */
	private <T extends MutableCapabilities> T addZaleniumCaps(T caps, String suiteName) {

		// currently supporting LINUX only as using zalenium of docker
		caps.setCapability(CapabilityType.BROWSER_NAME, "firefox");
		caps.setCapability(CapabilityType.PLATFORM_NAME, Platform.LINUX);

		// for test name
		caps.setCapability("name", suiteName + "_" + new SimpleDateFormat("dd_MMMM_yyyy_HH_mm_ss").format(new Date()));

		// for idle timeout for download file
		// timeout in seconds to wait for browser idel time between actions
		int idleTimeout = 660;
		caps.setCapability("idleTimeout", idleTimeout);

		return caps;
	}

	/**
	 * To kill the driver running in windows for cleaning the current or next
	 * executions based on executionTimeout
	 * 
	 * @param executionTimeout
	 */
	public void cleanDrivers(boolean executionTimeout) {
		// kill the previous driver processes
		// in windows
		// plus execution is not parallel
		if (System.getProperty("os.name").contains("Windows")
				&& !Boolean.valueOf(Configuration.getProperty("parallelExecution"))
				&& Boolean.valueOf(Configuration.getProperty("debugging"))) {
			if (executionTimeout)
				log.debug("Killing the driver instance running if any as execution timeout");
			else
				log.debug("Killing past driver instance running if any");
			isProcessRunningAndKill("geckodriver.exe");
			isProcessRunningAndKill("chromedriver.exe *32");
			isProcessRunningAndKill("chromedriver.exe");
			isProcessRunningAndKill("IEDriverServer.exe");
		}
	}

	/**
	 * To initialize the driver based on global configuration
	 * 
	 * @param browser
	 * @param hub
	 * @param nodeDownloadFolder
	 * @param nodeName
	 */
	public void setUp(String xmlBrowserParam, String hub, String nodeDownloadFolder, String nodeName) {

		log.debug("Entered into setup for core");

		// for using the java 11+ http client -
		// https://www.selenium.dev/blog/2022/using-java11-httpclient/
		System.setProperty("webdriver.http.factory", "jdk-http-client");

		boolean needSikuli = Boolean.parseBoolean(Configuration.getProperty("sikuli"));
		log.debug("User wants to use sikuli? - " + needSikuli);

		log.debug("Entered into web driver set up");

		// kill the previous driver processes
		// in windows
		// plus execution is not parallel
		cleanDrivers(false);

		// get the browserDriver
		String browserDriver = Configuration.getProperty("driver");

		String browserName;
		// if local get browser name from config
		if (hub.equals("hub")) {
			browserName = Configuration.getProperty("browser").toUpperCase();

			// validation for the browser and given driver path
			validateBrowserGiven(browserName, browserDriver);
		} else // take it from the testng parameter passed to function
			browserName = xmlBrowserParam.toUpperCase();
		log.debug("Browser name is - " + browserName);

		headless = Boolean.parseBoolean(Configuration.getProperty("headless"));
		log.debug("User wants run in NON_GUI mode in firefox or chrome? - " + headless);

		int waitTime = Integer.parseInt(Configuration.getProperty("waitInSeconds"));
		log.debug("Default wait time - " + waitTime);

		boolean wantToDeleteCookies = Boolean.parseBoolean(Configuration.getProperty("deleteCookies"));
		log.debug("User wants to delete cookies? - " + wantToDeleteCookies);

		boolean autoDownload = Boolean.parseBoolean(Configuration.getProperty("autoDownload"));
		log.debug("User wants to auto download files? - " + autoDownload);

		// Set Capabilities and launch the WebDriver.
		log.debug("Initializing the driver object");

		// create a folder based on local or grid execution
		String downloadFolder = !nodeDownloadFolder.equals("nodeDownloadFolder") ? nodeDownloadFolder
				: Configuration.getProperty("downloadFolder");
		String folderPath = createDownloadFolder(downloadFolder);

		// for getting the browser logs
		String getFirefoxLogs = Configuration.getProperty("getBrowserLogs");
		boolean getLogs = getFirefoxLogs == null ? false : Boolean.valueOf(getFirefoxLogs);

		// create browser object based on given value in config or testng xml
		switch (BrowserType.valueOf(browserName)) {

		case FIREFOX:
			browser = new Firefox();
			break;

		case CHROME:
			browser = new Chrome();
			break;

		case EDGE:
			browser = new Edge();
			break;
		}

		if (browser != null) {
			// get the browser capabilities
			AbstractDriverOptions<?> options = browser.getCapabilities(folderPath, autoDownload, headless);

			// set the driver in system if local
			if (hub.equals("hub")) {// it's local execution without grid
				browser.setDriver(browserDriver);

				// check the get logs config
				String getDriverLogsConfig = Configuration.getProperty("getBrowserDriverLogs");
				boolean getDriverLogs = getDriverLogsConfig == null ? false : Boolean.valueOf(getDriverLogsConfig);

				// create driver
				driver = browser.createDriver(getDriverLogs, options);

				log.debug("Created driver for local execution");
			} else {

				if (nodeName == null)
					System.out.println("yes");

				// add the node name capability which will run on given node
				options.setCapability("nodename:applicationName", "node_" + nodeName);
				options.setCapability("networkname:applicationName", "network_" + nodeName);

				// add capability for grid
				// chromeOptions = addZaleniumCaps(chromeOptions, suiteName);
				driver = getGridDriver(hub, options);
			}
			log.debug("Created driver for local execution");
		} else {
			log.error("Browser in config or XML not given from the allowed options - CHROME, FIREFOX, EDGE");
			System.exit(0);
		}

		// initialize wait & actions
		int waitSeconds = Integer.parseInt(Configuration.getProperty("waitInSeconds")) * 10;
		wait = new WebDriverWait(driver, Duration.ofSeconds(waitSeconds));
		actions = new Actions(driver);

		// default waits
		log.debug("Assigning defailt wait to " + waitTime);
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(waitTime));
		driver.manage().window().maximize(); // To maximize the window

		// for headless set window size
		if (headless)
			driver.manage().window().setSize(new Dimension(1920, 1080));

		// delete cookies if user choosen to
		if (wantToDeleteCookies) {
			log.debug("Driver will delete the cookies");
			driver.manage().deleteAllCookies();
		}

		log.debug("Returning the driver object to suite");

		// initializing the sikuli screen based on configuration
		if (needSikuli) {
			log.debug("Initiating Sikuli Screen Object");
			try {
				int sikuliTimeout = Integer.parseInt(Configuration.getProperty("sikuliTimeout"));
				log.debug("Sikuli default timeout is: " + sikuliTimeout);

				// Initializing the object
				screen = new Screen();
				screen.setAutoWaitTimeout(sikuliTimeout);
			} catch (Exception e) {
				log.error("Error initializing the Sikuli");
			}
		}

	}// end of setup

	/**
	 * To terminate the browser session
	 * 
	 */
	public void tearDown() {
		try {
			log.debug("Terminating the driver session");
			driver.quit();
			screen = null;
		} catch (Exception e) {
			log.trace("Error encounter while terminating the driver session", e);
		}
	}// end of tear down

	/**
	 * Check the running process and kill it
	 * 
	 * @param serviceName Give name of the process that you want to kill
	 * 
	 * @return Boolean
	 */
	public boolean isProcessRunningAndKill(String serviceName) {
		boolean boolRunning = false;
		try {

			String line;
			Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				// System.out.println("Task : " + line);
				if (line.contains(serviceName)) {
					boolRunning = true;
					Runtime.getRuntime().exec("taskkill /FI \"USERNAME eq %USERNAME%\" /f /IM " + serviceName);
				}
			}
			input.close();
		} catch (IOException e) {
			log.trace("Process killing got failed", e);
		}
		return boolRunning;
	}// end of isProcessRunningAndKill

}
