package framework.reporter;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.relevantcodes.extentreports.ExtentReports;
import com.relevantcodes.extentreports.ExtentTest;
import com.relevantcodes.extentreports.LogStatus;

import base.BaseSuite;
import corelibrary.CommonActions;
import framework.input.Configuration;
import framework.setup.SetUp;

/**
 * 
 * This class contains the responsibility of reporting
 * 
 *
 */
public class Reporter {

	// set up reporting
	public static ExtentReports report;
	private String reportName = StringUtils.EMPTY, reportPath, screenshotPath, screenshotFolderPath;

	// added it public & static for sending the screenshot on failures | health
	// check
	public static String lastScreenshotPath;

	public static ExtentTest logger;
	String timeStamp;

	// count for css 1 time
	private static int count = 0;
	public static String test_name;
	public static WebDriver driver;

	// logger object taking ref from logutil logger
	// public static Logger log = BaseSuite.log;

	// to hold the headless browser
	private static boolean isHeadLessBrowser = false;

	// to hold the node if grid execution
	private static boolean gridExecution = false;

	// to get the fail status of current executed test
	public static boolean isTestCaseFail = false;
	// to get the warning status of current executed test
	public static boolean isTestCaseWarning = false;

	// to only get the screenshots apart from pass & info
	private boolean skipPassInfoScreenshot = false;

	public Reporter(String timeStamp, String testSuiteName, String hub) {

		// move if any existing results to archive folder
		if (!Boolean.valueOf(Configuration.getProperty("parallelExecution")))
			moveToArchive();

		// initialize reporting
		this.timeStamp = timeStamp;

		// attach report name from user if available
		String userReportName = Configuration.getProperty("reportName").trim();
		if (userReportName != null && !userReportName.isEmpty())
			reportName += userReportName + "_";

		// add browser & time stamp to name
		reportName += testSuiteName + "_" + Configuration.getProperty("browser").trim().toUpperCase() + "_" + timeStamp;

		BaseSuite.log.debug("Generated report name as - " + reportName);

		// create a reportpath based on user given path handling ending '/' or
		// not
		reportPath = Configuration.getProperty("reportPath").trim();
		if (reportPath.endsWith("/") || reportPath.endsWith("\\"))
			reportPath = reportPath + reportName + File.separator;
		else
			reportPath = reportPath + File.separator + reportName + File.separator;

		// make the path separator as per the platform
		BaseSuite.log.debug("Generated report path at - " + reportPath);

		screenshotFolderPath = reportPath + "screenshots/";
		BaseSuite.log.debug("Generated report screenshot path at - " + screenshotFolderPath);

		report = new ExtentReports(reportPath + reportName + ".html", false);
		BaseSuite.log.debug("Initialized Report");

		report.loadConfig(new File("./libs/configs/extent-config.xml"));
		BaseSuite.log.debug("Loaded reporting configuration");

		// for taking screenshot we need driver object
		driver = SetUp.driver;

		// add log file name
		String execLogFileName = BaseSuite.logUtil.getLogFileName();
		report.addSystemInfo("Log File Name", execLogFileName);
		BaseSuite.log.debug("Added system property Log File Name with value - " + execLogFileName);

		// add browser system property
		String browserName = BaseSuite.setUp.browser.getBrowserName();
		String browserVersion = BaseSuite.setUp.browser.getBrowserVersion(driver);
		String browser = browserName + " " + browserVersion;
		report.addSystemInfo("Browser", browser);
		BaseSuite.log.debug("Added system property Browser - " + browser);

		// add browser driver version
		String browserDriverVersion = BaseSuite.setUp.browser.getBrowserDriverVersion(driver);
		report.addSystemInfo("Driver Version", browserDriverVersion);
		BaseSuite.log.debug("Added system property Browser Driver Version - " + browserDriverVersion);

		// add is browser headless or not
		boolean isBrowserHeadless = BaseSuite.setUp.browser.isBrowserHeadless(driver);
		report.addSystemInfo("Headless", String.valueOf(isBrowserHeadless));
		BaseSuite.log.debug("Added system property Browser Headless - " + isBrowserHeadless);

		// add user profile path for the browser
		String browserUserProfilePath = BaseSuite.setUp.browser.getBrowserUserProfilePath(driver);
		report.addSystemInfo("User Profile Path", browserUserProfilePath);
		BaseSuite.log.debug("Added system property Browser User Profile Pah - " + browserUserProfilePath);

		// get the dependency versions for adding the report environment details
		DependencyDetails depDetail = new DependencyDetails(BaseSuite.log);
		String seleniumVersion = depDetail.getDependencyVersion("selenium-java");
		report.addSystemInfo("Selenium-java", seleniumVersion);
		BaseSuite.log.debug("Added system property Selenium version - " + seleniumVersion);

		// enable the flag for headless browser for screenshot
		// if ("NON_GUI".equals(SetUp.browserName))
		if (BaseSuite.setUp.headless)
			isHeadLessBrowser = true;

		// store the node detail to identify grid execution for skipping full screenshot
		if (!hub.equals("hub"))
			gridExecution = true;

		// get the skipPassInfoScreenshot value from config
		String skipPassInfoScreenshotConfig = Configuration.getProperty("skipPassInfoScreenshot");
		skipPassInfoScreenshot = skipPassInfoScreenshotConfig != null ? Boolean.valueOf(skipPassInfoScreenshotConfig)
				: false;

	}

	// function to move already existing reports to the archive folder
	private void moveToArchive() {
		// get reports folder
		File reportsFolder;

		// check if report path ends with a / to get the appropriate folder of
		// report
		String existingReportPath = Configuration.getProperty("reportPath").toString().trim();
		// if(existingReportPath.endsWith("\\") ||
		// existingReportPath.endsWith("/"))
		// reportsFolder = new File(existingReportPath).getParentFile();
		// else
		reportsFolder = new File(existingReportPath);

		// to get the directory size apart from 'archive' folder, apply its
		// filter
		FileFilter fileFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() && !file.getName().contains("archive");
			}
		};

		// if there is any result folder available apart from archive before new
		// execution
		if (reportsFolder.exists() && reportsFolder.listFiles(fileFilter).length >= 1) {

			// then move the existing reports to archive folder

			// generate the archive folder based on user given path ends with
			// '/' or not
			File archiveFolder;
			if (existingReportPath.endsWith("\\") || existingReportPath.endsWith("/"))
				archiveFolder = new File(Configuration.getProperty("reportPath").trim() + "archive");
			else
				archiveFolder = new File(Configuration.getProperty("reportPath").trim() + File.separator + "archive");

			// loop if more then 1 report folder
			for (File f : reportsFolder.listFiles(fileFilter)) {
				try {
					FileUtils.moveToDirectory(f, archiveFolder, true);
				} catch (IOException e) {
					System.out.println("Unable to move the existing reports");
				}
			}

			// recycle archivefolder object
			archiveFolder = null;

		}

		// recycle reportsFolder object
		reportsFolder = null;

	}// end of moveToArchive

	/**
	 * Test name to be added in report and start logging step on it
	 * 
	 * @param test_name Test name to be added in report
	 * 
	 * @param desc      Test description coming from testng
	 * 
	 */
	public void addTest(String test_name, String... desc) {
		Reporter.test_name = test_name;
		if (desc.length > 0) {
			logger = report.startTest(Reporter.test_name, desc[0]);
			BaseSuite.log.debug("Added a new test to report named - " + test_name + "With description - " + desc);
		} else {
			logger = report.startTest(Reporter.test_name);
			BaseSuite.log.debug("Added a new test to report named - " + test_name);
		}
		String author = getAuthorName();
		logger.assignAuthor(author);
		BaseSuite.log.debug("Assigned author as - " + author);
		// flush the report for user
		terminate();
	}

	/**
	 * 
	 * Logging the pass step for a given test
	 * 
	 * @param description    pass step description
	 * 
	 * @param screenshot     do you need screenshot or not? takes 'true' or 'false'
	 * 
	 * @param screenshotType if 'screenshot' is true then specify ScreenshotType
	 *                       value form interface i.e ScreenshotType.browser or
	 *                       ScreenshotType.fullScreen
	 * 
	 */
	public void PASS(String description, boolean screenshot, String screenshotType) {
		if (screenshot && !skipPassInfoScreenshot
				&& !(isHeadLessBrowser && ScreenshotType.fullScreen.equalsIgnoreCase(screenshotType)))
			logger.log(LogStatus.PASS, description + ": " + attchScreenShot(screenshotType));
		else
			logger.log(LogStatus.PASS, description);

		BaseSuite.log.log(Level.DEBUG, "PASS: " + description);
		// flush the report for user
		terminate();
	}

	/**
	 * 
	 * Logging the fail step for a given test
	 * 
	 * @param description        fail step description
	 * 
	 * @param screenshot         do you need screenshot or not? takes 'true' or
	 *                           'false'
	 * 
	 * @param screenshotType     if 'screenshot' is true then specify ScreenshotType
	 *                           value form interface i.e ScreenshotType.browser or
	 *                           ScreenshotType.fullScreen
	 * 
	 * @param commonFunctionExit optional argument for framework usage for common
	 *                           functions exitApplication(). It will not terminate
	 *                           the current test, pass true here.
	 * 
	 */
	public void FAIL(String description, boolean screenshot, String screenshotType, boolean... commonFunctionExit) {
		if (screenshot && !(isHeadLessBrowser && ScreenshotType.fullScreen.equalsIgnoreCase(screenshotType)))
			logger.log(LogStatus.FAIL, description + ": " + attchScreenShot(screenshotType));
		else
			logger.log(LogStatus.FAIL, description);

		BaseSuite.log.log(Level.ERROR, "FAIL: " + description);

		// flush the report for user
		terminate();

		// change the status of test case flag
		isTestCaseFail = true;

		// global configuration to fail test for a single step
		if (Boolean.parseBoolean(Configuration.getProperty("onFailNextTest"))) {
			boolean exitTest = true;
			// check if fail from common function exit.. method
			if (commonFunctionExit.length > 0)
				exitTest = !commonFunctionExit[0]; // then don't exit from here

			if (exitTest)
				CommonActions.assertTest(description);
		}
	}

	/**
	 * 
	 * Logging the error step for a given test
	 * 
	 * @param description    error step description
	 * 
	 * @param screenshot     do you need screenshot or not? takes 'true' or 'false'
	 * 
	 * @param screenshotType if 'screenshot' is true then specify ScreenshotType
	 *                       value form interface i.e ScreenshotType.browser or
	 *                       ScreenshotType.fullScreen
	 * 
	 */
	public void ERROR(String description, Exception e, boolean screenshot, String screenshotType) {
		if (screenshot && !(isHeadLessBrowser && ScreenshotType.fullScreen.equalsIgnoreCase(screenshotType)))
			logger.log(LogStatus.ERROR, description + " - " + e.getClass() + " : " + attchScreenShot(screenshotType));
		else
			logger.log(LogStatus.ERROR, description + " - " + e.getClass());

		BaseSuite.log.log(Level.ERROR, "ERROR: " + description, e);

		// flush the report for user
		terminate();
		// change the status of test case flag
		isTestCaseFail = true;
		// global configuration to fail test for a single step
		if (Boolean.parseBoolean(Configuration.getProperty("onFailNextTest")))
			CommonActions.assertTest(description);
	}

	/**
	 * 
	 * Logging the info step for a given test
	 * 
	 * @param description    info step description
	 * 
	 * @param screenshot     do you need screenshot or not? takes 'true' or 'false'
	 * 
	 * @param screenshotType if 'screenshot' is true then specify ScreenshotType
	 *                       value form interface i.e ScreenshotType.browser or
	 *                       ScreenshotType.fullScreen
	 * 
	 */
	public void INFO(String description, boolean screenshot, String screenshotType) {
		if (screenshot && !skipPassInfoScreenshot
				&& !(isHeadLessBrowser && ScreenshotType.fullScreen.equalsIgnoreCase(screenshotType)))
			logger.log(LogStatus.INFO, description + ": " + attchScreenShot(screenshotType));
		else
			logger.log(LogStatus.INFO, description);

		BaseSuite.log.log(Level.DEBUG, "INFO: " + description);

		// flush the report for user
		terminate();
	}

	/**
	 * 
	 * Logging the warning step for a given test
	 * 
	 * @param description    warning step description
	 * 
	 * @param screenshot     do you need screenshot or not? takes 'true' or 'false'
	 * 
	 * @param screenshotType if 'screenshot' is true then specify ScreenshotType
	 *                       value form interface i.e ScreenshotType.browser or
	 *                       ScreenshotType.fullScreen
	 * 
	 */
	public void WARNING(String description, boolean screenshot, String screenshotType) {

		if (screenshot && !(isHeadLessBrowser && ScreenshotType.fullScreen.equalsIgnoreCase(screenshotType)))
			logger.log(LogStatus.WARNING, description + ": " + attchScreenShot(screenshotType));
		else
			logger.log(LogStatus.WARNING, description);

		BaseSuite.log.log(Level.WARN, "WARNING: " + description);

		// flush the report for user
		terminate();

		// change the status of test case flag
		isTestCaseWarning = true;
	}

	/**
	 * 
	 * Logging the skip step for a given test
	 * 
	 * @param description    skip step description
	 * 
	 * @param screenshot     do you need screenshot or not? takes 'true' or 'false'
	 * 
	 * @param screenshotType if 'screenshot' is true then specify ScreenshotType
	 *                       value form interface i.e ScreenshotType.browser or
	 *                       ScreenshotType.fullScreen
	 * 
	 */
	public void SKIP(String description, boolean screenshot, String screenshotType) {
		if (screenshot && !(isHeadLessBrowser && ScreenshotType.fullScreen.equalsIgnoreCase(screenshotType)))
			logger.log(LogStatus.SKIP, description + ": " + attchScreenShot(screenshotType));
		else
			logger.log(LogStatus.SKIP, description);

		BaseSuite.log.log(Level.DEBUG, "SKIP: " + description);

		// flush the report for user
		terminate();
	}

	/**
	 * To capture the screenshot of a webpage and modify to make it display properly
	 * 
	 * @param screenshotType screenshot type browser or desktop
	 * 
	 * @return String html of the captured screenshot
	 * 
	 */
	private String attchScreenShot(String screenshotType) {
		// for the rotation of the image in steps
		String style = "style=\"-ms-transform: rotate(0deg);-webkit-transform: rotate(0deg);transform: rotate(0deg);\" ";

		// for the rotation of preview image
		String preview_style = "/>";

		// append style for preview rotation only 1 time
		String screenshot_html = "";

		// take screenshot skipping the grid execution - FullScreen
		if (!(screenshotType.equals(ScreenshotType.fullScreen) && gridExecution)) {

			if (count == 0) {
				screenshot_html = logger.addScreenCapture(takeScreenShot(screenshotType))
						.replace("<img ", "<img " + style).replace("/>", preview_style);
				count++;
			} else
				screenshot_html = logger.addScreenCapture(takeScreenShot(screenshotType)).replace("<img ",
						"<img " + style);

			// make absolute path of screenshot to relative - for moving the result
			// in future
			if (screenshot_html.contains("file:///"))
				screenshot_html = screenshot_html.replace("file:///" + screenshotPath,
						"screenshots" + screenshotPath.split("screenshots")[1]);
			else
				screenshot_html = screenshot_html.replace(screenshotPath,
						"screenshots" + screenshotPath.split("screenshots")[1]);
			BaseSuite.log.debug("Attaching the screenshot - " + screenshot_html);
		}

		// re-initialize the screenshots path
		screenshotPath = StringUtils.EMPTY;

		return screenshot_html;
	}

	/**
	 * To capture the screenshot of a webpage
	 * 
	 * @param screenshotType screenshot type browser or desktop
	 * 
	 * @return String path of the captured screenshot
	 * 
	 */
	private String takeScreenShot(String screenshotType) {
		String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss_SSS").format(new Date());

		// for limiting the screenshotname
		String partialName = test_name.replaceAll("[//,/,:,*,?,\",<,>,|]+", "_");
		if (partialName.length() > 105)
			partialName = partialName.substring(0, 105);

		// replace special character in test name for report screenshot
		screenshotPath = screenshotFolderPath + partialName + "_" + timeStamp + ".jpg";

		if (screenshotType.equals(ScreenshotType.browser)) {

			// take screenshot
			File file = null;
			try {
				if (driver instanceof FirefoxDriver)
					file = ((FirefoxDriver) driver).getFullPageScreenshotAs(OutputType.FILE);
				else
					file = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

				// store to local file path
				FileUtils.copyFile(file, new File(screenshotPath));
				BaseSuite.log.debug("Screenshot taken at - " + screenshotPath);
			} catch (WebDriverException e) {
				BaseSuite.log.trace("Error encountered while capturing the screenshot type: " + screenshotType, e);
			} catch (IOException e) {
				BaseSuite.log.trace("Error encountered while capturing & Moving the screenshot type: " + screenshotType,
						e);
			}
		} else if (screenshotType.equals(ScreenshotType.fullScreen)) {
			Robot robot;
			try {
				// using robot object to take the fullscreen shot
				robot = new Robot();
				// screenshot format to be passed to writer
				String format = "jpg";
				// creating the image
				Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
				BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

				// storing at a path
				File file = new File(screenshotPath);
				file.getParentFile().mkdirs();
				// moving the screenshot to path file
				ImageIO.write(screenFullImage, format, file);
				BaseSuite.log.debug("Screenshot taken at - " + screenshotPath);
			} catch (Exception e) {
				BaseSuite.log.trace("Error encountered while capturing the screenshot type: " + screenshotType, e);
			}
		} else
			BaseSuite.log.error("screenshot type should be 'browser' or 'desktop");

		// save for usage in health check
		lastScreenshotPath = screenshotPath;

		return screenshotPath;
	}

	/**
	 * To dump the logged events into report
	 * 
	 */
	public void terminate() {
		// close report
		// BaseSuite.log.debug("Dumping the report");
		report.flush();
	}

	/**
	 * To end the logging of a started test
	 * 
	 */
	public void endTest() {
		// add report to be logged in HTML
		BaseSuite.log.debug("Ending and dumping the result for a test");
		report.endTest(logger);
		terminate();
	}

	/**
	 * To get the author name from logged in PC user
	 * 
	 * @return String Author name from as a logged in user
	 *
	 */
	public String getAuthorName() {
		BaseSuite.log.debug("Taking the system username as the author name");
		String author = System.getProperty("user.name");
		BaseSuite.log.debug("Returning the author name - " + author);
		return author;
	}

	/**
	 * To get the report path of the current execution
	 * 
	 * @return
	 */
	public String getReportPath() {
		return reportPath;
	}

	/**
	 * To get the report name of the current execution
	 * 
	 * @return
	 */
	public String getReportName() {
		return reportName;
	}
}
