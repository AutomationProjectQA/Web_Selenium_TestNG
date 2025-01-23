package corelibrary;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Action;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.UnexpectedTagNameException;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.google.common.base.Function;

import base.BaseSuite;
import framework.input.Configuration;
import framework.reporter.Reporter;
import framework.reporter.ResultType;
import framework.reporter.ScreenshotType;
import framework.setup.SetUp;

/**
 * Class contains common actions to be used in components
 * 
 */

public class CommonActions {

	private WebDriver driver;
	private Reporter RESULT;
	protected WebDriverWait wait;

	// make global action object
	protected Actions actions;

	// for reseting the execution on exception in base suite plus useful in managing
	// windows
	public static String globalWinHandle;

	// for getting the download folder of the docker grid container to be used
	// instead of config one
	public String downloadFolder = BaseSuite.downloadFolder;

	// for OR Reflection
	private static List<String> orFiles;
	private static Map<By, String> dynamicVariableNames = new HashMap<By, String>();

	// to skip highlight/unhighlight when headless
	private boolean headlessExecution = false;

	public CommonActions() {
		this.driver = SetUp.driver;
		wait = SetUp.wait;
		actions = SetUp.actions;
		headlessExecution = BaseSuite.setUp.headless;

		this.RESULT = BaseSuite.RESULT;
	}

	public enum WaitType {
		visibilityOfElementLocated, elementToBeClickable, elementToBeEnabled, elementToBeSelected,
		invisibilityOfElementLocated, presenceOfElementLocated, frameToBeAvailableAndSwitchToIt, stalenessOf
	}

	public enum AlertAction {
		Accept, Dismiss
	}

	/**
	 * Create Object of specified page
	 * 
	 * @param pageName Page name as String and it is case-sensitive
	 * 
	 * @return Object It returns object of specified page
	 * 
	 * 
	 */
	public <pageReference extends Object> pageReference createObject(String pageName) {
		// reuse base suite method
		return BaseSuite.createObject(pageName);
	}// end of createObject

	/**
	 * click on given locator
	 * 
	 * @param locator By Locator of webElement
	 * 
	 */
	public void click(By locator) {
		// set the Element Name
		String locatorName = null;
		String elementOldStyle = "";

		try {
			locatorName = getLocatorName(locator);

			// get the webElement of locator
			WebElement element = driver.findElement(locator);

			// highlight & un-highlight quickly as it might navigate to other page
			elementOldStyle = highlightElement(element);
			unHighlightElement(element, elementOldStyle);

			// click on webElement
			element.click();

			RESULT.PASS(locatorName + " is clicked", false, ScreenshotType.browser);
		} catch (NoSuchElementException e) {
			RESULT.FAIL(("Element not found which performing click - '" + locatorName + "'"), true,
					ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in clicking '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in clicking '" + locatorName + "'", true, ScreenshotType.browser);
		}

	}// end of click

	/**
	 * click on WebElement and wait for page to be loaded
	 * 
	 * @param locator Locator of webElement
	 * 
	 */
	public void clickWithPageLoad(By locator) {
		// set the Element Name
		String locatorName = null;
		String elementOldStyle = "";
		try {
			locatorName = getLocatorName(locator);

			// get the webElement of locator
			WebElement element = driver.findElement(locator);

			// highlight & un-highlight quickly as it might navigate to other page
			elementOldStyle = highlightElement(element);
			unHighlightElement(element, elementOldStyle);

			// click on webElement
			element.click();

			// wait for page to load
			waitForPageLoad();
			RESULT.PASS(locatorName + " is clicked", false, ScreenshotType.browser);
		} catch (NoSuchElementException e) {
			RESULT.FAIL(("Element not found while performing click with page load - '" + locatorName + "'"), true,
					ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in clicking '" + locatorName + "'", e);
			RESULT.FAIL(("Error occurred in clicking '" + locatorName + "'"), true, ScreenshotType.browser);
		}

	}// end of click

	/**
	 * Enter value in input Field
	 * 
	 * @param locator locator of input Field
	 * 
	 * @param value   value that needs to be entered in input Field
	 * 
	 */

	public void setValue(By locator, String value) {
		// set the Element Name
		String locatorName = getLocatorName(locator);
		String elementOldStyle = "";

		// check element is editable or not
		boolean isEditable = isElementEditable(locator);
		if (isEditable) {
			try {
				// get the webElement of locator
				WebElement element = driver.findElement(locator);
				elementOldStyle = highlightElement(element);

				// clear the text field
				element.clear();

				// Enter keys in the text field
				element.sendKeys(value);

				// get the entered value
				String getText = getElementAttribute(locator, "value");
				if (getText.equalsIgnoreCase(value)) {
					RESULT.PASS(value + " is entered in '" + locatorName + "' Text Field", false,
							ScreenshotType.browser);
				} else {
					RESULT.FAIL(getText + " is entered in '" + locatorName + "' Text Field instead of " + value, true,
							ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);

			} catch (NoSuchElementException e) {
				RESULT.FAIL(("Element not found - '" + locatorName + "'"), true, ScreenshotType.browser);
			} catch (WebDriverException e) {
				checkInterrupted(e, executionInterrupted, ResultType.FAIL);
				BaseSuite.log.error(
						"Exception occurred in getting WebDriver of '" + locatorName + "' while performing setValue: ",
						e);
				RESULT.FAIL(("Error occurred in getting WebDriver for '" + locatorName + "' while entering value - "
						+ value), true, ScreenshotType.browser);
			}
		}

	}// end of setValue

	/**
	 * Enter URL in the Browser
	 * 
	 * @param url Specifies Url String
	 *
	 * 
	 * 
	 */
	public void launchApplication(String url) {
		try {
			driver.get(url);
			RESULT.PASS("URL: " + url + " is launched", false, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred while launching " + url, e);
			RESULT.FAIL("Error occurred while launching " + url, true, ScreenshotType.browser);
		}
	}// end of launchApplication

	/**
	 * Get attribute value
	 * 
	 * @param locator   Specifies locator
	 * 
	 * @param attribute Specifies attribute type of element i.e. value
	 * 
	 * @param useJS     Specifies boolean value for useJS i.e. true or false
	 * 
	 * @return String
	 * 
	 */
	public String getElementAttribute(By locator, String attribute, boolean... useJS) {

		WebElement element;
		JavascriptExecutor executor;
		String locatorName = getLocatorName(locator);
		String elementOldStyle = "";

		// set the Element Name
		String value = "";
		try {

			// get the webElement of locator
			element = driver.findElement(locator);

			elementOldStyle = highlightElement(element);

			// get Element attribute
			if ((useJS.length > 0 && useJS[0])) {
				executor = (JavascriptExecutor) driver;
				value = (String) executor.executeScript("return arguments[0].getAttribute('" + attribute + "')",
						element);
			} else {
				value = element.getAttribute(attribute);
			}

			unHighlightElement(element, elementOldStyle);
		} catch (NoSuchElementException e) {
			RESULT.FAIL("Element not available: " + locatorName, true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Failed to get attribute value in " + locatorName, e);
			RESULT.FAIL("Failed to get attribute value in " + locatorName, true, ScreenshotType.browser);
		}

		return value;
	}// end of get attribute method

	/**
	 * check if element exist or not
	 * 
	 * @param locator Specifies locator
	 * 
	 * @return boolean
	 * 
	 * 
	 */
	public boolean isElementExists(By locator) {
		boolean isElementExist = false;
		try {
			driver.findElement(locator);
			isElementExist = true;
		} catch (NoSuchElementException e) {
			isElementExist = false;
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception occured while checking for '" + locator + "' locator exist ", e);
		}
		return isElementExist;
	}// end of is element exists

	/**
	 * Wait for page to be fully loaded. It will take already defined explicit wait
	 * timeout given.
	 * 
	 */
	public boolean waitForPageLoad() {
		boolean loadingCompleted = false;

		try {
			loadingCompleted = wait.until(driver -> {

				String loadingStatus = String
						.valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState"));

				BaseSuite.log.debug("Wait For Page Load Status - " + loadingStatus);

				return loadingStatus.equals("complete");
			});

			// handle unexpected alerts
			handleUnexpectedAlerts(Integer.valueOf(Configuration.getProperty("waitInSeconds")), true);
		} catch (UnhandledAlertException e) {
			BaseSuite.log.error("Alert Exception", e);
		} catch (TimeoutException e) {
			// made screenshot full screen for chrome render failure
			RESULT.WARNING("Page is not loaded and status should be COMPLETE", true, ScreenshotType.fullScreen);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception occured while waiting for page to load ", e);
		}

		return loadingCompleted;
	}// end of wait for page to load

	/**
	 * Wait for page to be fully loaded within given seconds
	 * 
	 * @param waitInSeconds integer wait in seconds
	 * 
	 */
	public boolean waitForPageLoad(int waitInSeconds) {
		boolean loadingCompleted = false;

		try {
			// change wait timeout based on user parameter
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitInSeconds));

			loadingCompleted = wait.until(driver -> {

				String loadingStatus = String
						.valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState"));

				BaseSuite.log.debug("Wait For Page Load Status - " + loadingStatus);

				return loadingStatus.equals("complete");
			});

			// handle unexpected alerts
			handleUnexpectedAlerts(Integer.valueOf(Configuration.getProperty("waitInSeconds")), true);
		} catch (TimeoutException e) {
			RESULT.WARNING("Page is not loaded and status should be COMPLETE", true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			BaseSuite.log.error(
					"Exception occured while waiting for page to load till given " + waitInSeconds + " seconds", e);
		}

		return loadingCompleted;
	}// end of wait for page to load

	/**
	 * Wait for page to be intractable, use with caution.
	 * 
	 * @param waitInSeconds integer wait in seconds
	 * 
	 * 
	 */
	public void waitForPageInteractive(int waitInSeconds) {
		try {
			// change wait timeout based on user parameter
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitInSeconds));

			// wait condition
			wait.until(driver -> "interactive"
					.equals(String.valueOf(((JavascriptExecutor) driver).executeScript("return document.readyState"))));
		} catch (TimeoutException e) {
			RESULT.FAIL("Page is not intractable within given time", true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception occured while waiting for page to become interactive within given "
					+ waitInSeconds + " seconds", e);
		}

	}// end of wait for page to load

	/**
	 * Switch to a window with title
	 * 
	 * @param windowText Specifies title of the new window or any text available on
	 *                   page
	 * 
	 * @return boolean
	 * 
	 */
	public boolean switchToWindow(String windowText) {
		boolean switchedToWindow = false;
		try {
			// get the parent window handle
			String parentWindow = driver.getWindowHandle();

			// get all the window handles
			Set<String> availableWindows = driver.getWindowHandles();

			// iterate through each to check if the given window is available or not
			// and switch to it
			for (String windowId : availableWindows) {
				if (driver.switchTo().window(windowId).getTitle().contains(windowText)) {
					RESULT.PASS("Switched to given window with title - " + windowText, true, ScreenshotType.browser);
					switchedToWindow = true;
					break;
				} else if (driver.switchTo().window(windowId).getPageSource().contains(windowText)) {
					RESULT.PASS("Switched to given window with text - " + windowText, true, ScreenshotType.browser);
					switchedToWindow = true;
					break;
				}
			}

			// if given window not available switch back to parent
			driver.switchTo().window(parentWindow);

			RESULT.FAIL("Given window is not available to switch with title, given text or handle - " + windowText,
					true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			BaseSuite.log.error(
					"Unexpected exception occured while switching to window with given text - '" + windowText + "'", e);
		}

		return switchedToWindow;

	}// end of switch to window

	/**
	 * Switch to a window with window handle
	 * 
	 * @param windowHandle selenium window handle
	 * 
	 * @return boolean
	 * 
	 */
	public boolean switchToWindowUsingHandle(String windowHandle) {

		boolean switched = false;

		try {
			// if given window not available switch back to parent
			driver.switchTo().window(windowHandle);

			RESULT.PASS("Switched to given window for handle - " + windowHandle, false, ScreenshotType.browser);
			switched = true;

		} catch (Exception e) {
			BaseSuite.log.error("Exception occured - given window is not available to switch with handle - ", e);
			RESULT.FAIL("Given window is not available to switch with handle - " + windowHandle, true,
					ScreenshotType.browser);
		}

		return switched;
	}

	/**
	 * To get the current focused window handle
	 * 
	 * @return windowHandle string
	 */
	public String currentWindowHandle() {
		String handle = "";

		try {
			handle = driver.getWindowHandle();
		} catch (Exception e) {
			BaseSuite.log.error("Excdeption occured - failed to get the current window handle - ", e);
			RESULT.FAIL("Failed to get the current window handle", false, ScreenshotType.browser);
		}

		return handle;
	}

	/**
	 * To get all the window handles of opened window in current session
	 * 
	 * @return list of string containing handles
	 */
	public List<String> getWindowHandles() {

		List<String> handles = new ArrayList<>();

		// get set of handles
		Set<String> handleSet = driver.getWindowHandles();

		// add it to list
		handles = handleSet.stream().collect(Collectors.toList());

		return handles;

	}

	/**
	 * Double clicking on any element
	 * 
	 * @param locator locator of WebElement
	 * 
	 */
	public void doubleclick(By locator) {
		// set locator name
		String locatorName = null;
		String elementOldStyle = "";

		try {
			locatorName = getLocatorName(locator);

			// get WebElement of locator
			WebElement element = driver.findElement(locator);
			// highlight locator
			elementOldStyle = highlightElement(element);

			// double click on WebElement
			actions.doubleClick(element).build().perform();

			RESULT.PASS(locatorName + " is  double clicked ", false, ScreenshotType.browser);

			unHighlightElement(element, elementOldStyle);
		} catch (NoSuchElementException e) {
			RESULT.FAIL("Element not found - " + locatorName, true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in double clicking '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in double clicking '" + locatorName + "'", true, ScreenshotType.browser);

		}
	}// end of doubleclick

	/**
	 * Javascript click on WebElement
	 * 
	 * @param locator locator of WebElement
	 * 
	 * 
	 */
	public void javaScriptClick(By locator) {
		JavascriptExecutor executor;
		// set locator name
		String locatorName = null;
		String elementOldStyle = "";

		try {
			locatorName = getLocatorName(locator);

			// get WebElement of locator
			WebElement element = driver.findElement(locator);

			// highlight & un-highlight quickly as it might navigate to other page
			elementOldStyle = highlightElement(element);
			unHighlightElement(element, elementOldStyle);

			// Create an object of JavaScriptExector Class
			executor = (JavascriptExecutor) driver;

			// click on WebElement
			executor.executeScript("arguments[0].click();", element);
			RESULT.PASS(locatorName + " is  clicked ", false, ScreenshotType.browser);
		} catch (NoSuchElementException e) {
			RESULT.FAIL("Element not found while performing javascript click - " + locatorName, true,
					ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in Java clicking '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in Java clicking '" + locatorName + "'", true, ScreenshotType.browser);
		}
		executor = null;
	}// end of javaScriptClick

	/**
	 * Wait for given locator for a specified period of time
	 * 
	 * @param locator     By locator of webElement
	 * 
	 * @param waitTime    Specifies implicit wait time in integers e.g 10,20,30..
	 * 
	 * @param waitType    Type "WaitType." to get the available WaitType (enum) e.g
	 *                    WaitType.visibilityOfElementLocated
	 * @param showFailure Should show failure on timeout of not. Default true.
	 * 
	 */
	public boolean waitForElement(By locator, int waitTime, WaitType waitType, boolean... showFailure) {
		// set locator name
		String locatorName = null;
		try {
			locatorName = getLocatorName(locator);

			// Wait in seconds
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitTime));

			switch (waitType) {

			// wait until visibility of element located
			case visibilityOfElementLocated:
				wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
				return true;

			// wait until element is clickable
			case elementToBeClickable:
				wait.until(ExpectedConditions.elementToBeClickable(locator));
				return true;

			// wait until element is enabled
			case elementToBeEnabled:
				WebElement element = getWebElement(locator);
				wait.until((ExpectedCondition<Boolean>) driver -> element.isEnabled());
				return true;

			// wait until element is selected
			case elementToBeSelected:
				wait.until(ExpectedConditions.elementToBeSelected(locator));
				return true;

			// wait until element is invisible
			case invisibilityOfElementLocated:
				wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
				return true;

			// wait until presence of element located
			case presenceOfElementLocated:
				wait.until(ExpectedConditions.presenceOfElementLocated(locator));
				return true;

			// wait until frame is available and switched to frame
			case frameToBeAvailableAndSwitchToIt:
				wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
				return true;

			// wait for staleness of element
			case stalenessOf:
				wait.until(ExpectedConditions.stalenessOf(getWebElement(locator)));
				return true;
			default:
				RESULT.FAIL("Entered WaitType '" + waitType + "'is invalid", false, ScreenshotType.browser);
			}
		} catch (TimeoutException e) {
			boolean showFail = showFailure.length > 0 ? showFailure[0] : true;
			if (showFail)
				RESULT.FAIL("Timed out waiting for '" + waitType + "' on element '" + locatorName + "' after "
						+ waitTime + " seconds", true, ScreenshotType.browser);
			return false;
		} catch (UnhandledAlertException e2) {
			BaseSuite.log.error("Unexpected alert occured", e2);
			// while waiting if alert appear and throws the exception handle it
			if (isAlertPresent(3, false)) {
				alertAction(AlertAction.Accept);
			}
		} catch (WebDriverException e3) {
			checkInterrupted(e3, executionInterrupted, ResultType.FAIL);
			// webdriver problem show only in log file
			BaseSuite.log.error("Webdriver error while wait for element", e3);
		} catch (NullPointerException e4) {
			// special case due to isDisplayed() return NULL from selenium server
		}
		return false;
	}// end of waitForElement

	/**
	 * To wait for number of windows to be
	 * 
	 * @param windowCount
	 * 
	 * @param waitTime    in seconds
	 * 
	 * @return
	 */
	public boolean waitForNumberOfWindowsToBe(int windowCount, int waitTime) {
		boolean result = false;

		// Wait in seconds
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitTime));

		// wait for windows count
		try {
			wait.until(ExpectedConditions.numberOfWindowsToBe(windowCount));
			result = true;
		} catch (TimeoutException e) {
			RESULT.FAIL("Timeout after waiting for window count to be " + windowCount, true, ScreenshotType.browser);
		} catch (Exception e2) {
			BaseSuite.log.error("Exception occurred while waiting for window count to be " + windowCount, e2);
		}

		return result;
	}

	/**
	 * To wait for number of windows to be with default timeout
	 * 
	 * @param windowCount
	 * 
	 * @return
	 */
	public boolean waitForNumberOfWindowsToBe(int windowCount) {
		boolean result = false;

		// wait for windows count
		try {
			wait.until(ExpectedConditions.numberOfWindowsToBe(windowCount));
			result = true;
		} catch (TimeoutException e) {
			RESULT.FAIL("Timeout after waiting for window count to be " + windowCount, true, ScreenshotType.browser);
		} catch (Exception e2) {
			BaseSuite.log.error("Exception occurred while waiting for window count to be " + windowCount, e2);
		}

		return result;
	}

	/**
	 * Perform Action on Alert Pop-up
	 * 
	 * @param alertAction Specifies Action that should available in enum AlertAction
	 *                    e.g AlertAction.Accept and AlertAction.Dismiss
	 * 
	 * 
	 */
	public void alertAction(AlertAction alertAction) {
		try {
			// switch to alert
			Alert alert = driver.switchTo().alert();

			String alertText = getAlertText();

			switch (alertAction) {

			// Accept Alert
			case Accept:
				alert.accept();
				RESULT.PASS(alertText + " alert is '" + alertAction + "'", false, ScreenshotType.fullScreen);
				break;
			// Dismiss Alert
			case Dismiss:
				alert.dismiss();
				RESULT.PASS(alertText + " alert is '" + alertAction + "'", false, ScreenshotType.fullScreen);

				break;

			default:
				RESULT.FAIL("Entered Action '" + alertAction + "' is invalid", false, ScreenshotType.fullScreen);
			}

		} catch (NoAlertPresentException e) {
			BaseSuite.log.error("No Alert Present for " + alertAction + " Alert", e);
			RESULT.FAIL("No Alert Present for " + alertAction + " Alert", true, ScreenshotType.fullScreen);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception occured while performing alert action - " + alertAction, e);
		}
	}// end of AlertAction

	/**
	 * Verify Alert Text and Perform Action on Alert Pop-up
	 * 
	 * @param Action   Type "AlertAction." to get the available Actions to Perform
	 *                 (enum) e.g AlertAction.Accept and AlertAction.Dismiss
	 * 
	 * @param AlertMsg Alert message that needs to be verified.
	 * 
	 * 
	 */
	public void verifyAlertMsg_AlertAction(AlertAction alertAction, String AlertMsg) {
		Alert alert;
		try {
			// switch to alert
			alert = driver.switchTo().alert();

			// getting Alert Text
			String getAlertText = alert.getText();
			if (getAlertText.equalsIgnoreCase(AlertMsg)) {
				RESULT.PASS("Expected Alert message:: '" + getAlertText + "' is displayed", false,
						ScreenshotType.fullScreen);
			} else {
				RESULT.FAIL("Alert message:: '" + getAlertText + "' is displayed instead of Expected Alert message:: '"
						+ AlertMsg + "'", true, ScreenshotType.fullScreen);
			}

			alertAction(alertAction);

		} catch (NoAlertPresentException e) {
			BaseSuite.log.error("Exception occurred in verifying and '" + alertAction + "' Alert", e);
			RESULT.FAIL("Error occurred in verifying and '" + alertAction + "' Alert", true, ScreenshotType.fullScreen);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception occured while handling for alert having text '" + AlertMsg
					+ "' for performing action - " + alertAction, e);
		}
	}// end of verifyAlertMsg_AlertAction

	/**
	 * Verify Alert partial Text and Perform Action on Alert Pop-up
	 * 
	 * @param Action   Type "AlertAction." to get the available Actions to Perform
	 *                 (enum) e.g AlertAction.Accept and AlertAction.Dismiss
	 * 
	 * @param alertMsg Alert partial message that needs to be verified.
	 *
	 * 
	 */

	public void verifyAlertMsg_AlertAction_PartialText(AlertAction alertAction, String alertMsg) {
		Alert alert;
		try {
			// switch to alert
			alert = driver.switchTo().alert();

			// getting Alert Text
			String getAlertText = alert.getText();
			if (getAlertText.trim().contains(alertMsg)) {
				RESULT.PASS("Expected Alert message:: '" + getAlertText + "' is displayed", false,
						ScreenshotType.fullScreen);
			} else {
				RESULT.FAIL("Alert message:: '" + getAlertText + "' is displayed instead of Expected Alert message:: '"
						+ alertMsg + "'", true, ScreenshotType.fullScreen);
			}
			switch (alertAction) {

			// Accept Alert
			case Accept:
				alert.accept();
				RESULT.PASS("Alert is '" + alertAction + "'", false, ScreenshotType.fullScreen);
				break;

			case Dismiss:
				// Dismiss Alert
				alert.dismiss();
				RESULT.PASS("Alert is '" + alertAction + "'", false, ScreenshotType.fullScreen);
				break;
			}

		} catch (NoAlertPresentException e) {
			BaseSuite.log.error("Exception occurred in verifying and '" + alertAction + "' Alert", e);
			RESULT.FAIL("Error occurred in verifying and '" + alertAction + "' Alert", true, ScreenshotType.fullScreen);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Unexpected exception occured while handling for alert having partial text '" + alertMsg
					+ "' for performing action - " + alertAction, e);
		}
	}// end of verifyAlertMsg_AlertAction_PartialText

	/**
	 * Get Alert Text form Pop-up
	 * 
	 * @return String Return the Alert Text
	 *
	 * 
	 */
	public String getAlertText() {
		String alertText = Strings.EMPTY;
		try {
			// switch to Alert
			Alert alert = driver.switchTo().alert();

			// get Alert Text
			alertText = alert.getText();

			// parse the text to avoid having HTML in the result which disturb the report
			// HTML
			alertText = Jsoup.parse(alertText).text();
		} catch (NoAlertPresentException e) {
			BaseSuite.log.error("Exception occurred in reteriving Alert Text", e);
			RESULT.FAIL("Error occurred in reteriving Alert Text", true, ScreenshotType.fullScreen);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Unexpected exception occurred while reteriving Alert Text", e);
		}

		return alertText;
	}// end of getAlertText

	/**
	 * Check Alert is present or not
	 * 
	 * @param waitForAlert explicit wait in seconds for alert to be appear
	 * @param logAlert     to log the alert not found message in report
	 */
	public boolean isAlertPresent(int waitForAlert, boolean... logAlert) {

		boolean foundAlert = false;
		try {
			WebDriverWait wait = new WebDriverWait(this.driver,
					Duration.ofSeconds(Integer.parseInt(Configuration.getProperty("waitInSeconds")) * 10));

			// change time
			wait.withTimeout(Duration.ofSeconds(waitForAlert));
			// wait for Alert to display
			wait.until(ExpectedConditions.alertIsPresent());
			foundAlert = true;
			RESULT.INFO("Alert is present", logAlert.length > 0 ? logAlert[0] : false, ScreenshotType.fullScreen);
		} catch (TimeoutException e) {

			if (logAlert.length > 0 && logAlert[0]) {
				RESULT.INFO("Alert not present", true, ScreenshotType.fullScreen);
			}
		} catch (WebDriverException e) {
			BaseSuite.log.error("Unexpected exception occurred while checking if alert is present or not within given '"
					+ waitForAlert + "' seconds", e);
		}
		return foundAlert;
	}// end of isAlertPresent

	/**
	 * Enter keys in Alert Text Box
	 * 
	 * @param text text to write in Alert Text Box
	 * 
	 */
	public void setTextInAlert(String text) {
		try {

			// switch to Alert
			Alert alert = driver.switchTo().alert();

			// Enter Keys in the Alert Text Box
			alert.sendKeys(text);

		} catch (WebDriverException e) {
			BaseSuite.log.error("Unexpected exception occurred in setting the Alert input text - " + text, e);
			RESULT.FAIL("Error occurred in checking Alert", true, ScreenshotType.fullScreen);
		}
	}// end of setTextInAlert

	/**
	 * Checks page title matches with a given title or not
	 * 
	 * @param pageTitle title of page opened
	 * 
	 */
	public boolean verifyPageTitle(String pageTitle) {
		try {
			boolean result = false;

			if (pageTitle.equals(driver.getTitle()))
				result = true;
			else
				result = false;

			return result;

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in checking title of the page", e);
			RESULT.FAIL("Error occurred in checking title of the page", true, ScreenshotType.browser);
			return false;
		}
	}// end of verifyPageTitle

	/**
	 * Performs the drag and drop operation
	 * 
	 * @param fromLocator Specifies the locator of source element
	 * 
	 * @param toLocator   Specifies the locator of destination element
	 * 
	 * 
	 */
	public void dragAndDrop(By fromLocator, By toLocator) {
		// set fromLocator and ToLocaotor Name

		String fromLocatorName = null, toLocatorName = null;

		fromLocatorName = getLocatorName(fromLocator);
		toLocatorName = getLocatorName(toLocator);
		try {
			// get from Locator WebElement

			WebElement from = driver.findElement(fromLocator);
			try {
				// get To Locator WebElement

				WebElement to = driver.findElement(toLocator);
				try {

					// Build an object of Action
					Action dragAndDrop = actions.clickAndHold(from).moveToElement(to).release(to).build();

					// Perform action
					dragAndDrop.perform();
				} catch (WebDriverException e) {
					checkInterrupted(e, executionInterrupted, ResultType.FAIL);
					BaseSuite.log.error(
							"Exception occurred in Dragging '" + fromLocatorName + "' to '" + toLocatorName + "'", e);
					RESULT.FAIL("Error occurred in Dragging '" + fromLocatorName + "' to '" + toLocatorName + "'", true,
							ScreenshotType.browser);
				}
			} catch (NoSuchElementException e) {
				RESULT.FAIL("To element not found while performing drag and drop - '" + toLocatorName, true,
						ScreenshotType.browser);
			}
		} catch (NoSuchElementException e) {
			RESULT.FAIL("From element not found while performing drag and drop - '" + fromLocatorName, true,
					ScreenshotType.browser);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Unexpected exception occured while performing drag and drop from locator '"
					+ fromLocator + "'  to locator '" + toLocator + "'", e);
			RESULT.FAIL("Error occured while performing drag and drop from locator '" + fromLocator + "'  to locator '"
					+ toLocator + "'", true, ScreenshotType.browser);
		}

	}// end of dragAndDrop

	/**
	 * Navigate to previous page
	 * 
	 */
	public void goToPreviousPage() {
		try {
			driver.navigate().back();
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in navigating to previous page", e);
			RESULT.FAIL("Error occurred in navigating to previous page", true, ScreenshotType.browser);
		}
	}// end of goToPreviousPage

	/**
	 * Refresh the current page
	 * 
	 */
	public void refreshPage() {
		try {
			driver.navigate().refresh();
			waitForPageLoad();
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in refreshing the page", e);
			RESULT.FAIL("Error occurred in refreshing the page", true, ScreenshotType.browser);
		}
	}// end of refreshPage

	/**
	 * Get WebElement for provided locator
	 * 
	 * @param elementLocator   Locator of web element
	 * 
	 * 
	 * @param reportIfNotFound
	 * 
	 *                         optional - to display message if element not found -
	 *                         by default true
	 *
	 * @return WebElement
	 * 
	 * 
	 */
	public WebElement getWebElement(By elementLocator, boolean... reportIfNotFound) {
		// variable to hold element name
		String locatorName = null;
		boolean displayMsg = reportIfNotFound.length > 0 ? reportIfNotFound[0] : true;
		try {
			// set element name
			locatorName = getLocatorName(elementLocator);

			// get & return web element of locator
			return driver.findElement(elementLocator);
		} catch (NoSuchElementException e) {
			if (displayMsg)
				RESULT.FAIL(("Element not found - '" + locatorName + "'"), true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			RESULT.FAIL(("Error occurred in getting WebDriver for '" + locatorName + "'"), true,
					ScreenshotType.browser);
			BaseSuite.log.error("Exception occurred in getting WebDriver of '" + locatorName + "'", e);
		}
		return null;
	}

	/**
	 * Get List of WebElement
	 * 
	 * @param parentClassWebElement WebElement of parent Class
	 *
	 * @param childClassLocator     locator of child Class
	 * 
	 * @return List<WebElement> List of WebElement
	 * 
	 * 
	 */
	public List<WebElement> getList(WebElement parentClassWebElement, By childClassLocator) {
		// set child class locator name
		String childClassName = null;
		List<WebElement> childList = null;
		String parentElementOldStyle = "";
		try {
			childClassName = getLocatorName(childClassLocator);
			parentElementOldStyle = highlightElement(parentClassWebElement);

			// get a list of child WebElements
			childList = parentClassWebElement.findElements(childClassLocator);
			if (childList == null)
				// return child List
				RESULT.FAIL(childClassName + " List is null", true, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			RESULT.FAIL(("Error occurred in getting List of '" + childClassName + "'"), true, ScreenshotType.browser);
			BaseSuite.log.error(("Error occurred in getting List of '" + childClassName + "'"), e);
		}

		unHighlightElement(parentClassWebElement, parentElementOldStyle);

		return childList;
	}

	/**
	 * Get List of WebElement
	 * 
	 * @param parentLocator locator of parent Class
	 *
	 * @param childLocator  locator of child Class
	 * 
	 * @return List<WebElement> List of WebElement
	 * 
	 * 
	 */
	public List<WebElement> getList(By parentLocator, By childLocator) {
		// set child class locator name
		String childLocatorName = "";
		List<WebElement> childList = null;
		String parentElementOldStyle = "";

		try {
			childLocatorName = getLocatorName(childLocator);

			// get WebElement of parent Class
			WebElement parentClass = getWebElement(parentLocator);

			if (parentClass != null) {
				parentElementOldStyle = highlightElement(parentClass);

				// get a list of child WebElement
				childList = parentClass.findElements(childLocator);
				if (childList == null)
					RESULT.FAIL(childLocatorName + " List is empty", true, ScreenshotType.browser);

				unHighlightElement(parentClass, parentElementOldStyle);
			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			RESULT.FAIL("Error occurred in getting List of '" + childLocatorName + "'", true, ScreenshotType.browser);
			BaseSuite.log.error("Exception occurred in getting List of '" + childLocatorName + "'", e);
		}

		return childList;
	}

	/**
	 * For getting data of particular cell in web table
	 * 
	 * @param tableLocator Locator of web table to fetch data
	 * 
	 * @param rowNumber    Row number to get cell data as integer
	 * 
	 * @param columnName   Column Name to get cell data as String
	 * 
	 * @return String Desired cell data of web table as String
	 * 
	 * 
	 */
	public String getCellData(By tableLocator, int rowNumber, String columnName) {
		// variables to hold row and column index
		int currentRow = 0;
		int columnNumber = 0;
		rowNumber--;

		// variables to hold table name and cell data
		String tableName = "";
		String cellData = null;

		// locator for row data - containing tags 'td' and 'th'
		By rowDataLocator = By.xpath("./*['td' or 'th']");

		// locator for row - contains tag 'tr'
		By rowLocator = By.xpath("./tbody/*['tr']");

		try {
			// set web table name
			tableName = getLocatorName(tableLocator);

			try {

				// wait and locate web table to fetch particular cell data
				waitForElement(tableLocator, 40, WaitType.visibilityOfElementLocated);

				// get all rows data
				List<WebElement> trCollection = getList(tableLocator, rowLocator);

				// checking rows of web table are fetched or not
				if (!trCollection.isEmpty()) {

					// going through each row in web table
					for (WebElement trElement : trCollection) {

						// check first row of web table
						if (currentRow == 0) {
							// get all the td and th
							List<WebElement> tdCollection = getList(trElement, rowDataLocator);

							// checking if data of first row is fetched or not
							if (!tdCollection.isEmpty()) {
								// get column number for given column name
								for (int i = 0; i < tdCollection.size(); i++) {
									if (tdCollection.get(i).getText().contains(columnName)) {
										columnNumber = i;
										break;
									}
								}
							} else {
								RESULT.FAIL("No column name is available to get column number in web table - '"
										+ tableName + "'", false, ScreenshotType.browser);
							}
						}

						// check for particular row to fetch data
						if (currentRow == rowNumber) {

							// get data of row
							List<WebElement> td_collection = getList(trElement, rowDataLocator);

							// checking if data of row is fetched or not
							if (!td_collection.isEmpty()) {
								// get the column cell of which text needed
								WebElement cell = td_collection.get(columnNumber);
								String oldStyle = highlightElement(cell);
								// get cell data for particular row and column
								cellData = cell.getText();
								unHighlightElement(cell, oldStyle);
								break;
							} else {
								RESULT.FAIL("No row is available to fetch particular cell data in web table - '"
										+ tableName + "'", true, ScreenshotType.browser);
							}
						}
						currentRow++;
					}
				} else {
					RESULT.FAIL("No row is available in web table - " + tableName, true, ScreenshotType.browser);
				}

			} catch (NoSuchElementException e) {
				RESULT.FAIL(("Web table not found - '" + tableName + "'"), true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting cell data in web table - '" + tableName + "'", e);
			RESULT.FAIL("Error occurred in getting cell data in web table - '" + tableName + "'", true,
					ScreenshotType.browser);

		}
		return cellData;
	}// end of getCellData

	/**
	 * For getting data of particular cell in web table
	 * 
	 * @param tableLocator Locator of web table to fetch data
	 * 
	 * @param rowNumber    Row number to get cell data as integer
	 * 
	 * @param columnNumber Column number to get cell data as integer
	 * 
	 * @return String Desired cell data of web table as String
	 * 
	 * 
	 */
	public String getCellData(By tableLocator, int rowNumber, int columnNumber) {
		// variables to hold row and column index
		int currentRow = 0;
		rowNumber--;
		columnNumber--;

		// variables to hold table name and cell data
		String tableName = "", cellData = "";

		// locator for row data - containing tags 'td' and 'th'
		By rowDataLocator = By.cssSelector("td,th");

		// locator for row - contains tag 'tr'
		By rowLocator = By.tagName("tr");

		try {
			// set web table name
			tableName = getLocatorName(tableLocator);

			try {

				// wait and locate web table to fetch particular cell data
				waitForElement(tableLocator, 40, WaitType.visibilityOfElementLocated);

				// get all rows data
				List<WebElement> trCollection = getList(tableLocator, rowLocator);

				// checking rows of web table are fetched or not
				if (!trCollection.isEmpty()) {

					// going through each row in web table
					for (WebElement trElement : trCollection) {
						// check for particular row to fetch data
						if (currentRow == rowNumber) {
							// get all the td and th
							List<WebElement> tdCollection = getList(trElement, rowDataLocator);

							// checking if data of row is fetched or not
							if (!tdCollection.isEmpty()) {

								// get cell data for particular row and column
								for (int i = 0; i < tdCollection.size(); i++) {
									if (i == columnNumber) {
										WebElement cell = tdCollection.get(i);
										String oldStyle = highlightElement(cell);
										cellData = cell.getText();
										unHighlightElement(cell, oldStyle);
										break;
									}
								}
							} else {
								RESULT.FAIL("No row is available to fetch particular cell data in web table - '"
										+ tableName + "'", true, ScreenshotType.browser);
							}
						}
						currentRow++;
					}
				} else {
					RESULT.FAIL("No row is available in web table - '" + tableName + "'", true, ScreenshotType.browser);
				}
				return cellData;

			} catch (NoSuchElementException e) {
				RESULT.FAIL("Web table not found - '" + tableName + "'", true, ScreenshotType.browser);
			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting cell data in web table-'" + tableName + "'", e);
			RESULT.FAIL("Error occurred in getting cell data in web table-'" + tableName + "'", true,
					ScreenshotType.browser);
		}
		return null;
	}// end of getCellData

	/**
	 * For getting data of particular row in web table
	 * 
	 * @param tableLocator     Locator of web table to fetch data
	 * 
	 * @param rowNum           Row number to get row data as integer
	 * 
	 * @param customRowLocator Specify custom row locator relative to table locator
	 * 
	 * @return ArrayList Data of single row as list
	 * 
	 */
	public ArrayList<String> getRowData(By tableLocator, int rowNumber, By... customRowLocator) {
		// variable to hold row index
		int currentRow = 0;
		rowNumber--;

		// variable to hold table name
		String tableName = "";

		// locator for row - contains tag 'tr'
		By rowLocator = customRowLocator.length > 0 ? customRowLocator[0] : By.tagName("tr");

		// locator for row data - containing tags 'td' and 'th'
		By rowDataLocator = By.cssSelector("td,th");

		try {
			// set web table name
			tableName = getLocatorName(tableLocator);

			// List to store data of a row
			ArrayList<String> rowDataList = new ArrayList<String>();

			try {

				// wait and locate web table to fetch row
				waitForElement(tableLocator, 40, WaitType.visibilityOfElementLocated);

				// get all rows data
				List<WebElement> trCollection = getList(tableLocator, rowLocator);

				// checking rows of web table are fetched or not
				if (!trCollection.isEmpty()) {

					// going through each row in web table
					for (WebElement trElement : trCollection) {

						// check for particular row to fetch data
						if (currentRow == rowNumber) {

							// get all the td and th
							List<WebElement> tdCollection = getList(trElement, rowDataLocator);

							// checking if data of row is fetched or not
							if (!tdCollection.isEmpty()) {

								// storing each data of row in list
								for (int i = 0; i < tdCollection.size(); i++) {
									rowDataList.add(tdCollection.get(i).getText());
								}
								break;
							} else {
								RESULT.FAIL("No row is available to fetch particular data of row in web table - '"
										+ tableName + "'", true, ScreenshotType.browser);
							}
						}
						currentRow++;
					}
				} else {
					RESULT.FAIL("No row is available in web table - '" + tableName + "'", true, ScreenshotType.browser);
				}

				return rowDataList;

			} catch (NoSuchElementException e) {
				RESULT.FAIL("Web table not found - '" + tableName + "'", true, ScreenshotType.browser);
			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting data of particular row-'" + tableName + "'", e);
			RESULT.FAIL("Error occurred in getting data of particular row-'" + tableName + "'", true,
					ScreenshotType.browser);
		}
		return null;
	}// end of getRowData

	/**
	 * For getting data of particular column in web table
	 * 
	 * @param tableLocator Locator of web table to fetch data
	 * 
	 * @param columnNumber Column number to get data of particular column as integer
	 * 
	 * @return ArrayList Data of particular column of web table as List
	 * 
	 * 
	 */
	public ArrayList<String> getColumnData(By tableLocator, int columnNumber) {
		// variable to hold column index
		columnNumber--;

		// variable to hold table name
		String tableName = "";

		// locator for row - contains tag 'tr'
		By rowLocator = By.tagName("tr");

		// locator for row data - containing tags 'td' and 'th'
		By rowDataLocator = By.cssSelector("td,th");

		try {
			// set web table name
			tableName = getLocatorName(tableLocator);

			// List to store data of a column
			ArrayList<String> columnDataList = new ArrayList<String>();

			try {

				// wait and locate web table to fetch particular column data
				waitForElement(tableLocator, 40, WaitType.visibilityOfElementLocated);

				// get all rows
				List<WebElement> trCollection = getList(tableLocator, rowLocator);

				// checking rows of web table are fetched or not
				if (!trCollection.isEmpty()) {

					// going through each row in web table
					for (WebElement trElement : trCollection) {

						// get all the td and th
						List<WebElement> tdCollection = getList(trElement, rowDataLocator);

						// checking if row is fetched or not
						if (!tdCollection.isEmpty()) {

							// for each row - add a data of particular column in
							// list
							for (int i = 0; i < tdCollection.size(); i++) {
								if (i == columnNumber) {
									columnDataList.add(tdCollection.get(columnNumber).getText());
									break;
								}
							}
						} else {
							RESULT.FAIL("No row is available to fetch particular column data in web table - '"
									+ tableName + "'", true, ScreenshotType.browser);
						}

					}
				} else {
					RESULT.FAIL("No row is available in web table - '" + tableName + "'", true, ScreenshotType.browser);
				}

				return columnDataList;

			} catch (NoSuchElementException e) {
				RESULT.FAIL("Web table not found - " + tableName, true, ScreenshotType.browser);
			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error(
					"Exception occurred in getting data of particular column in web table -'" + tableName + "'", e);
			RESULT.FAIL("Error occurred in getting data of particular column in web table -'" + tableName + "'", true,
					ScreenshotType.browser);
		}
		return null;
	}// end of getColumnData

	/**
	 * For getting total number of columns for particular row in web table
	 * 
	 * @param tableLocator Locator of web table to fetch data
	 * 
	 * @param rowNumber    Row number to count columns as integer
	 * 
	 * @return integer No of columns as integer
	 * 
	 * 
	 */
	public int getColumnCount(By tableLocator, int rowNumber) {
		// variables to hold row index and column count
		int columnCount = 0;
		int currentRow = 0;

		rowNumber--;

		// variable to hold table name
		String tableName = "";

		// locator for row - contains tag 'tr'
		By rowLocator = By.tagName("tr");

		// locator for row data - containing tags 'td' and 'th'
		By rowDataLocator = By.cssSelector("td,th");

		try {
			// set web table name
			tableName = getLocatorName(tableLocator);

			try {

				// wait and locate web table to fetch column count
				waitForElement(tableLocator, 40, WaitType.visibilityOfElementLocated);

				// List to store all rows of web table
				List<WebElement> trCollection = getList(tableLocator, rowLocator);

				// checking rows of web table are fetched or not
				if (!trCollection.isEmpty()) {

					// going through each row in web table
					for (WebElement trElement : trCollection) {

						// check for particular row to count column
						if (currentRow == rowNumber) {

							// get all the td and th
							List<WebElement> tdCollection = getList(trElement, rowDataLocator);

							// checking if data of row is fetched or not
							if (!tdCollection.isEmpty()) {

								// save count of columns
								columnCount = tdCollection.size();
								break;

							} else {
								RESULT.FAIL(
										"No row is available to determine no of columns for particular row in web table - '"
												+ tableName + "'",
										true, ScreenshotType.browser);
							}
						}
						currentRow++;
					}

				} else {
					RESULT.FAIL("No row is available in web table - '" + tableName + "'", true, ScreenshotType.browser);
				}

				return columnCount;

			} catch (NoSuchElementException e) {
				RESULT.FAIL("Web table not found - '" + tableName + "'", true, ScreenshotType.browser);

			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting total number columns for particular row in web table -'"
					+ tableName + "'", e);
			RESULT.FAIL("Error occurred in getting total number columns for particular row in web table -'" + tableName
					+ "'", true, ScreenshotType.browser);
		}

		return columnCount;
	}// end of getColumnCount

	/**
	 * For getting row number for provided cell data in web table
	 * 
	 * @param tableLocator Locator of web table to fetch data
	 * 
	 * @param cellData     Particular cell value as string to get row number
	 * 
	 * @param columnName   Name of Column containing cellData
	 * 
	 * @return integer Row number as integer
	 * 
	 */
	public int getRowNumber(By tableLocator, String cellData, String columnName) {
		// variable to hold table name
		String tableName = "";

		// variables to hold row index
		int rowNumber = 1;
		int columnNumber = 0;

		// locator for row - contains tag 'tr'
		By rowLocator = By.tagName("tr");

		// locator for row data - containing tags 'td' and 'th'
		By rowDataLocator = By.cssSelector("td,th");

		try {
			// set web table name
			tableName = getLocatorName(tableLocator);

			try {

				// wait and locate web table to determine row number
				waitForElement(tableLocator, 40, WaitType.visibilityOfElementLocated);

				// List to store all rows of web table
				List<WebElement> trCollection = getList(tableLocator, rowLocator);

				// checking rows of web table are fetched or not
				if (!trCollection.isEmpty()) {

					// going through each row in web table
					Outer: for (WebElement trElement : trCollection) {

						// get all the data of first row
						List<WebElement> tdCollection = getList(trElement, rowDataLocator);

						// check first row of web table
						if (rowNumber == 1) {

							// checking if data of first row is fetched or not
							if (!tdCollection.isEmpty()) {

								// get column number for given column name
								for (int i = 0; i < tdCollection.size(); i++) {
									if (tdCollection.get(i).getText().contains(columnName)) {
										columnNumber = i;
										break;
									}
								}
							} else {

								RESULT.FAIL("No column name is available to get column number in web table - '"
										+ tableName + "'", true, ScreenshotType.browser);
							}
						}
						// checking if data of row is fetched or not
						if (!tdCollection.isEmpty()) {

							// for each row - add a data of particular column in
							// list
							for (int i = 0; i < tdCollection.size(); i++) {

								if (i == columnNumber && tdCollection.get(i).getText().contains(cellData)) {
									break Outer;
								}
							}
						} else {

							RESULT.FAIL(
									"No row is available to determine row number in web table - '" + tableName + "'",
									true, ScreenshotType.browser);
						}
						rowNumber++;
					}
				} else {

					RESULT.FAIL("No row is available in web table - '" + tableName + "'", true, ScreenshotType.browser);
				}

			} catch (NoSuchElementException e) {

				RESULT.FAIL("Web table not found - '" + tableName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error(
					"Exception occurred in getting row number of given cell data from table - '" + tableName + "'", e);
			RESULT.FAIL(
					"Error occurred in getting total row number of given cell data from table - '" + tableName + "'",
					true, ScreenshotType.browser);
		}
		return rowNumber;

	}// end of getRowNumber

	/**
	 * For getting total number of rows in table
	 * 
	 * @param tableLocator Locator of web table to determine row count
	 * 
	 * @return integer No of rows in web table as integer
	 * 
	 */
	public int getRowCount(By tableLocator) {
		// variable to hold row count
		int rowCount = 0;

		// variable to hold table name
		String tableName = "";

		// locator for row - contains tag 'tr'
		By rowLocator = By.tagName("tr");

		try {
			// set web table name
			tableName = getLocatorName(tableLocator);

			try {

				// wait and locate web table to determine no of rows
				waitForElement(tableLocator, 40, WaitType.visibilityOfElementLocated);

				// List to store all rows of web table
				List<WebElement> trCollection = getList(tableLocator, rowLocator);

				// count no of rows
				rowCount = trCollection.size();

				// checking rows of web table are fetched or not
				if (rowCount == 0) {
					RESULT.FAIL("No row is available in table - '" + tableName + "'", true, ScreenshotType.browser);
				} else {
					RESULT.INFO(rowCount + " Number of rows determined in table - '" + tableName + "'", false,
							ScreenshotType.browser);
					BaseSuite.log.debug(rowCount + " Number of rows determined in table - '" + tableName + "'");
				}

			} catch (NoSuchElementException e) {
				RESULT.FAIL("Web table not found - '" + tableName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error(
					"Exception occurred in getting total number of columns in web table -'" + tableName + "'", e);
			RESULT.FAIL("Exception occurred in getting total number of columns in web table -'" + tableName + "'", true,
					ScreenshotType.browser);
		}
		return rowCount;

	}// end of getRowCount

	/**
	 * File Upload functionality
	 * 
	 * @param filePath      Specifies path of the file which is to be uploaded
	 * 
	 * @param browseLocator Specifies locator of the Browse button
	 * 
	 * @param uploadLocator locator of the Upload button
	 * 
	 * 
	 */
	public void fileUpload(String filePath, By browseLocator, By uploadLocator) {

		// reference creation of Robot class
		Robot robot;
		String browseName = getLocatorName(browseLocator);
		String uploadName = getLocatorName(uploadLocator);

		try {
			// Instance creation of Robot class
			robot = new Robot();

			waitForElement(browseLocator, 40, WaitType.elementToBeClickable);
			if (isElementExists(browseLocator)) {
				// path of file to upload
				StringSelection fpath = new StringSelection(filePath);

				// click on browse
				try {
					click(browseLocator);
				} catch (Exception e) {
					javaScriptClick(browseLocator);
				}

				// copy path of file
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(fpath, null);
				pause(1);

				// paste path of file in window Pop-up
				robot.keyPress(KeyEvent.VK_CONTROL);
				robot.keyPress(KeyEvent.VK_V);

				robot.keyRelease(KeyEvent.VK_V);
				robot.keyRelease(KeyEvent.VK_CONTROL);

				robot.keyPress(KeyEvent.VK_ENTER);
				robot.keyRelease(KeyEvent.VK_ENTER);

				// wait and locate upload button
				waitForElement(uploadLocator, 40, WaitType.elementToBeClickable);
				if (isElementExists(browseLocator)) {
					WebElement upload = driver.findElement(uploadLocator);
					// click on upload
					upload.click();
				} else {
					RESULT.FAIL("Upload element not found- '" + uploadName + "'", true, ScreenshotType.browser);
				}
			} else {
				RESULT.FAIL("Browse element not found- '" + browseName + "'", true, ScreenshotType.browser);
			}

		} catch (AWTException e) {
			BaseSuite.log.error("Exception occurred in creating instance of Robot class", e);
			RESULT.FAIL("Error occurred in creating instance of Robot class", true, ScreenshotType.browser);
		} catch (NoSuchElementException e) {
			RESULT.FAIL("Element not found", false, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in uploading file", e);
			RESULT.FAIL("Exception occurred in uploading file", true, ScreenshotType.browser);
		}

		robot = null;

	}// end of fileUpload

	/**
	 * check if element is editable or not
	 * 
	 * @param elementLocator Locator of element
	 * 
	 * 
	 */

	public boolean isElementEditable(By elementLocator) {
		// flag to hold status of element
		boolean flag = false;

		// variable to hold element name
		String locatorName = "", elementOldStyle;

		try {
			// set web element name
			locatorName = getLocatorName(elementLocator);

			WebElement element = driver.findElement(elementLocator);

			elementOldStyle = highlightElement(element);

			// check if element get displayed and enabled
			flag = (element.isDisplayed()) && (element.isEnabled());

			if (!(element.isEnabled())) {
				RESULT.FAIL("'" + locatorName + "' is not enabled", true, ScreenshotType.browser);
			}
			if (!(element.isDisplayed())) {
				RESULT.FAIL("'" + locatorName + "' is not displayed", true, ScreenshotType.browser);
			}
			if (flag) {
				RESULT.PASS("Element is editable - '" + locatorName + "'", false, ScreenshotType.browser);
			}

			unHighlightElement(element, elementOldStyle);
		} catch (NoSuchElementException e) {

			RESULT.FAIL("Element not found - '" + locatorName + "'", true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in checking element editable -'" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in checking element editable -'" + locatorName + "'", true,
					ScreenshotType.browser);
		}
		return flag;
	}// end of isElementEditable

	/**
	 * check if element enabled or not
	 * 
	 * @param elementLocator Locator of element
	 *
	 * 
	 */

	public boolean isElementEnabled(By elementLocator) {
		// flag to hold status of element
		boolean flag = false;

		// variable to hold element name
		String locatorName = "", elementOldStyle;

		try {
			// set web element name
			locatorName = getLocatorName(elementLocator);

			WebElement element = driver.findElement(elementLocator);

			elementOldStyle = highlightElement(element);

			// check if element gets enabled
			flag = (element.isEnabled());

			if (flag) {
				RESULT.PASS("Element is enabled - " + locatorName, false, ScreenshotType.browser);
			}

			unHighlightElement(element, elementOldStyle);
		} catch (NoSuchElementException e) {

			RESULT.FAIL("Element not found - " + locatorName, true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception: Caused while checking element -'" + locatorName + "'", e);
			RESULT.FAIL("Error: Caused while checking element -'" + locatorName + "'", true, ScreenshotType.browser);
		}
		return flag;
	}// end of isElementEnabled

	/**
	 * check if element is displayed or not
	 * 
	 * @param elementLocator Locator for which to check
	 * 
	 * @param showFail       To specify if we need to show fail message in report of
	 *                       not --Optional
	 * 
	 */
	public boolean isElementDisplayed(By elementLocator, boolean... showFail) {
		// flag to hold status of element
		boolean flag = false;

		// variable to hold element name
		String locatorName = "";

		try {
			// set web element name
			locatorName = getLocatorName(elementLocator);

			WebElement element = driver.findElement(elementLocator);

			// check if element gets enabled
			flag = (element.isDisplayed());

			if (flag)
				RESULT.PASS("Element is displayed- " + locatorName, false, ScreenshotType.browser);

		} catch (NoSuchElementException e) {
			if (showFail.length > 0 && showFail[0])
				RESULT.FAIL("Element is not present - " + locatorName, true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			RESULT.FAIL("Error: Caused while checking for display of element- '" + locatorName + "'", true,
					ScreenshotType.browser);
			BaseSuite.log.error("Exception: Caused while checking for display of element- '" + locatorName + "'", e);
		}
		return flag;
	}// end of isElementDisplayed

	/**
	 * Scroll to particular web element in page
	 * 
	 * @param elementLocator Locator of element to scroll
	 * 
	 * 
	 */
	public void scrollToElement(By elementLocator) {
		// variable to hold element name
		String locatorName = "", elementOldStyle;

		try {
			// set web element name
			locatorName = getLocatorName(elementLocator);

			try {

				// wait and locate element
				waitForElement(elementLocator, 40, WaitType.presenceOfElementLocated);
				WebElement element = driver.findElement(elementLocator);

				// Moves the mouse to the middle of the element.
				actions.moveToElement(element);

				// method for performing the actions
				actions.build().perform();

				elementOldStyle = highlightElement(element);

				RESULT.PASS("Scrolled to particular web element - '" + locatorName + "'", false,
						ScreenshotType.browser);

				unHighlightElement(element, elementOldStyle);

			} catch (NoSuchElementException e) {

				RESULT.FAIL("Element not found - '" + locatorName + "'", true, ScreenshotType.browser);
			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception: Caused while scrolling to element-'" + locatorName + "'", e);
			RESULT.FAIL("Error: Caused while scrolling to element-'" + locatorName + "'", true, ScreenshotType.browser);

		}

	}// end of scrollToElement

	/**
	 * To scroll to given (x,y) coordinates
	 * 
	 * @param xCoordinate positive coordinate value to scroll
	 * 
	 * @param yCoordinate positive coordinate value to scroll
	 */
	public void scrollTo(int xCoordinate, int yCoordinate) {
		try {
			// type casting of WebDriver object to JavascriptExecutor
			JavascriptExecutor jse = (JavascriptExecutor) driver;

			// scroll to based on coordinate value
			jse.executeScript("window.scrollTo(" + xCoordinate + "," + yCoordinate + ")");

			RESULT.PASS("Scrolled To x: " + xCoordinate + "y: " + yCoordinate, false, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			RESULT.FAIL("Error: Caused while scrolling up", true, ScreenshotType.browser);
			BaseSuite.log.error("Exception: Caused while scrolling up", e);
		}

	}// end of scrollTo

	/**
	 * To scroll by given (x,y) offset
	 * 
	 * @param xOffset positive or negative coordinate value by which to scroll
	 * 
	 * @param yOffset positive or negative coordinate value by which to scroll
	 */
	public void scrollBy(int xOffset, int yOffset) {
		try {
			// type casting of WebDriver object to JavascriptExecutor
			JavascriptExecutor jse = (JavascriptExecutor) driver;

			// scroll by based on offset value
			jse.executeScript("window.scrollBy(" + xOffset + "," + yOffset + ")");

			RESULT.PASS("Scrolled By x: " + xOffset + "y: " + yOffset, false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			RESULT.FAIL("Error: Caused while scrolling down", true, ScreenshotType.browser);
			BaseSuite.log.error("Exception: Caused while scrolling down", e);
		}

	}// end of ScrollDown

	/**
	 * Checks the presence of the specified text
	 * 
	 * @param text text as String
	 * 
	 */
	public boolean verifyTextPresent(String text) {
		// variable to hold presence of element
		boolean result = false;

		try {
			WebElement textElement = getWebElement(By.tagName("body"));
			String oldStyle = highlightElement(textElement);

			// Checks for specific text
			if (textElement.getText().toLowerCase().contains(text.toLowerCase())) {
				result = true;

				RESULT.PASS("'" + text + "' - Text is present in page", false, ScreenshotType.browser);
			} else {
				RESULT.FAIL("'" + text + "' - Text is not present in page", true, ScreenshotType.browser);
			}

			unHighlightElement(textElement, oldStyle);
		} catch (NoSuchElementException e) {

			RESULT.FAIL("Body Element not found ", true, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception: Caused while Verifying the Presence of Text '" + text + "'", e);
			RESULT.FAIL("Error: Caused while Verifying the Presence of Text '" + text + "'", true,
					ScreenshotType.browser);

		}
		return result;
	}// end of verifyTextPresent

	/**
	 * Checks the presence of the specified text
	 * 
	 * @param text      text as String
	 * 
	 * @param timeInSec Check specified text till specified time in seconds
	 * 
	 */
	public boolean verifyTextPresent(String text, int timeInSec) {
		// variable to hold presence of element
		boolean result = false;
		// Wait till specific time and polling after every specific time
		Wait<WebDriver> fluentwait = new FluentWait<>(driver)

				.withTimeout(Duration.ofSeconds(timeInSec))

				.pollingEvery(Duration.ofSeconds(3))

				.ignoring(NoSuchElementException.class);
		try {
			WebElement textBody = driver.findElement(By.tagName("body"));
			String oldStyle = highlightElement(textBody);

			// Verify text
			Function<WebDriver, Boolean> verifyText = new Function<WebDriver, Boolean>() {
				// function to return true or false based on the presence of
				// text in page
				public Boolean apply(WebDriver driver) {

					boolean flag = false;

					// check if text is present in page or not
					if (textBody.getText().toLowerCase().contains(text.toLowerCase())) {
						flag = true;
					}
					return flag;
				}
			};
			// storing boolean value
			result = fluentwait.until(verifyText);

			if (result) {

				RESULT.PASS("'" + text + "' - Text is present in page", false, ScreenshotType.browser);
			} else {

				RESULT.FAIL("'" + text + "' - Text is not present in page", true, ScreenshotType.browser);
			}

			unHighlightElement(textBody, oldStyle);
		} catch (NoSuchElementException e) {

			RESULT.FAIL("Body Element not found ", true, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception: Caused while Verifying the Presence of Text '" + text + "'", e);
			RESULT.FAIL("Error: Caused while Verifying the Presence of Text '" + text + "'", true,
					ScreenshotType.browser);
		}
		return result;
	}// end of verifyTextPresent

	/**
	 * Context click on WebElement
	 * 
	 * @param elementLocator Locator of element
	 * 
	 * 
	 */

	public void rightClick(By elementLocator) {
		// variable to hold element name
		String locatorName = "", elementOldStyle;

		try {
			// set element name
			locatorName = getLocatorName(elementLocator);

			try {

				// wait and locate element name
				waitForElement(elementLocator, 40, WaitType.visibilityOfElementLocated);
				WebElement element = driver.findElement(elementLocator);

				elementOldStyle = highlightElement(element);

				// Building action for context-click
				Actions action = actions.contextClick(element);

				// Performs a context-click at the current mouse location
				action.build().perform();

				RESULT.PASS("'" + locatorName + "' is context clicked", false, ScreenshotType.browser);

				unHighlightElement(element, elementOldStyle);
			} catch (NoSuchElementException e) {

				RESULT.FAIL("Element not found - '" + locatorName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in context clicking '" + locatorName + "'", e);
			RESULT.FAIL("Exception occurred in context clicking '" + locatorName + "'", true, ScreenshotType.browser);
		}
	}// end of rightClick

	/**
	 * Get all the option values in drop-down
	 * 
	 * @param dropDownLocator Locator of drop-down
	 * 
	 * @param getValue        true/false based on the need to get value
	 * 
	 * @return ArrayList All the options text/value of drop-down as List
	 * 
	 */
	public ArrayList<String> getDropDownOptions(By dropDownLocator, boolean... getValue) {
		// variable to hold drop-down element
		String locatorName = null, elementOldStyle;

		// need to get the value of not
		boolean needValue = false;
		if (getValue.length > 0)
			needValue = getValue[0];

		Select dropDownSelect;

		// List to store all options of drop-down
		ArrayList<String> allOptionList = new ArrayList<String>();

		try {
			// set drop-down element name
			locatorName = getLocatorName(dropDownLocator);

			try {

				// wait and locate drop-down element
				waitForElement(dropDownLocator, 40, WaitType.visibilityOfElementLocated);
				WebElement element = driver.findElement(dropDownLocator);

				elementOldStyle = highlightElement(element);

				dropDownSelect = new Select(element);

				// Retrieve all options web elements as List
				List<WebElement> dropDownOptions = dropDownSelect.getOptions();

				// checking if list contains drop-down options web elements
				if (dropDownOptions != null) {

					// store each drop-down option in list
					for (WebElement webElement : dropDownOptions) {

						try {
							if (needValue)
								// Extracting the value
								allOptionList.add(webElement.getAttribute("value"));
							else
								// Extracting the label texts for each option
								allOptionList.add(webElement.getText());
						} catch (Exception e) {
							allOptionList.add("");
						}
					}

					RESULT.PASS("Drop down options retrieved", false, ScreenshotType.browser);

				} else {

					RESULT.FAIL("No option is available in drop-down - '" + locatorName + "'", true,
							ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);

			} catch (NoSuchElementException e) {

				RESULT.FAIL("Drop down element not found - '" + locatorName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting option values of '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in getting option values of '" + locatorName + "'", true,
					ScreenshotType.browser);
		}

		dropDownSelect = null;
		return allOptionList;

	}// end of getDropDownOptions

	/**
	 * Select an option from drop-down
	 * 
	 * @param dropDownLocator Locator of drop-down element
	 * 
	 * @param optionValue     Value attribute of option or visible text of option as
	 *                        String
	 * 
	 * @return boolean
	 * 
	 */

	public boolean selectDropDownOption(By dropDownLocator, String optionValue) {
		// variable to hold drop down element name
		String locatorName = null, elementOldStyle;

		Select dropDownSelect;

		// return if selected
		boolean dropdownSelected = false;

		try {
			// set drop-down element name
			locatorName = getLocatorName(dropDownLocator);

			try {

				// wait and locate drop-down element
				waitForElement(dropDownLocator, 40, WaitType.visibilityOfElementLocated);
				WebElement element = driver.findElement(dropDownLocator);

				elementOldStyle = highlightElement(element);

				dropDownSelect = new Select(element);

				// Retrieve all options web elements as List
				List<String> dropDownOptions = getDropDownOptions(dropDownLocator);

				// checking if list contains drop-down options web elements
				if (dropDownOptions != null) {
					if (dropDownOptions.contains(optionValue)) {

						// select desired option from drop down
						dropDownSelect.selectByVisibleText(optionValue);

						RESULT.PASS("Drop down option is selected with specified visible text -'" + optionValue + "'",
								false, ScreenshotType.browser);

						dropdownSelected = true;

					} else {

						try {
							// select desired option based on value attribute
							dropDownSelect.selectByValue(optionValue);

							RESULT.PASS("Drop down option is selected with specified value -'" + optionValue + "'",
									false, ScreenshotType.browser);

							dropdownSelected = true;
						} catch (NoSuchElementException e) {

							RESULT.FAIL("Specified option -'" + optionValue + "' is not available in drop down - '"
									+ locatorName + "'", true, ScreenshotType.browser);
						}
					}
				} else {
					RESULT.FAIL("No option is available in drop-down - '" + locatorName + "'", true,
							ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);

			} catch (NullPointerException e) {
				BaseSuite.log.error("Exception on dropdown option selection", e);
				RESULT.FAIL(
						"Specified option -'" + optionValue + "' is not available in drop down - '" + locatorName + "'",
						true, ScreenshotType.browser);

			} catch (NoSuchElementException e) {
				RESULT.FAIL("Element not found - '" + locatorName + "'", true, ScreenshotType.browser);
			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in selecting option value of drop down '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in selecting option value of drop down '" + locatorName + "'", true,
					ScreenshotType.browser);
		}

		return dropdownSelected;

	}// end of selectDropDownOption

	/**
	 * Select an option from drop-down
	 * 
	 * @param dropDownLocator Locator of drop-down element
	 * 
	 * @param index           Zero based index of specific drop down option
	 * 
	 * 
	 */
	public void selectDropDownOption(By dropDownLocator, int index) {
		// variable to hold drop down element
		String locatorName = null, elementOldStyle;

		Select dropDownSelect;

		// in-case of invalid option, store in variable for reporting
		int invalidIndex = 0;

		try {
			// set drop-down element name
			locatorName = getLocatorName(dropDownLocator);

			try {

				// wait and locate drop-down element
				waitForElement(dropDownLocator, 40, WaitType.visibilityOfElementLocated);
				WebElement element = getWebElement(dropDownLocator);

				elementOldStyle = highlightElement(element);

				dropDownSelect = new Select(element);

				// Retrieve all options web elements as List
				List<WebElement> dropDownOptions = dropDownSelect.getOptions();

				// checking if list contains drop-down options web elements
				if (dropDownOptions != null) {

					// for displaying index when not available
					invalidIndex = index;

					// select desired option from drop down
					dropDownSelect.selectByIndex(index);

					// refresh the dropdown options to avoid stale element
					element = getWebElement(dropDownLocator);
					dropDownSelect = new Select(element);
					dropDownOptions = dropDownSelect.getOptions();

					// get the value at index
					String expectedValue = dropDownOptions.get(index).getAttribute("value");

					// get the text at index
					String expectedText = dropDownOptions.get(index).getText();

					// get the selected value
					String selectedValue = dropDownSelect.getFirstSelectedOption().getAttribute("value");

					// get the selected text
					String selectedText = dropDownSelect.getFirstSelectedOption().getText();

					// result
					if (selectedValue.equals(expectedValue) || selectedText.equals(expectedText))
						RESULT.PASS(
								"Drop down option is selected at specified index '" + index + "'"
										+ " with option value '" + selectedValue + "' and text '" + selectedText + "'",
								false, ScreenshotType.browser);
					else
						RESULT.FAIL("Failed to select Drop down option at specified index '" + index + "'"
								+ " with option value '" + expectedValue + "' and option text '" + expectedText
								+ "' against selected value '" + selectedValue + "' and selected text '" + selectedText
								+ "'", true, ScreenshotType.browser);

				} else {

					RESULT.FAIL("No option is available in drop-down - '" + locatorName + "'", true,
							ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);
			} catch (NullPointerException e) {

				RESULT.FAIL(
						"Specified Index -'" + invalidIndex + "' is not available in drop down - '" + locatorName + "'",
						true, ScreenshotType.browser);

			} catch (NoSuchElementException e) {

				RESULT.FAIL("Element not found - '" + locatorName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in selecting option value of drop down '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in selecting option value of drop down '" + locatorName + "'", true,
					ScreenshotType.browser);
		}
		dropDownSelect = null;

	}// end of selectDropDownOption

	/**
	 * Switch to default content from frame window
	 * 
	 * 
	 */

	public void switchToDefaultContent() {
		try {
			// switching to default content
			driver.switchTo().defaultContent();

			RESULT.PASS("Switched to default content", false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in switching to default content", e);
			RESULT.FAIL("Error occurred in switching to default content", true, ScreenshotType.browser);
		}
	}// end of switchToDefaultContentick

	/**
	 * Switch to particular frame using frame Index
	 * 
	 * @param frameIndex Index of frame (zero-based) as Integer
	 *
	 * 
	 */
	public void switchToFrame(int frameIndex) {
		try {
			// switch to frame based on index
			driver.switchTo().frame(frameIndex);

			RESULT.PASS("Switched to frame with provided index -'" + frameIndex + "'", false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in switching to frame with index - '" + frameIndex + "'", e);
			RESULT.FAIL("Exception occurred in switching to frame with index - '" + frameIndex + "'", true,
					ScreenshotType.browser);
		}
	}// end of switchToFrame

	/**
	 * Switch to particular frame using name or id
	 * 
	 * @param fname Name or id of frame as String
	 * 
	 */
	public void switchToFrame(String frame) {
		try {
			// switch to frame with provided name or id
			driver.switchTo().frame(frame);

			RESULT.PASS("Switched to frame with provided name or id -'" + frame + "'", false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in switching to frame with name or id -'" + frame + "'", e);
			RESULT.FAIL("Error occurred in switching to frame with name or id -'" + frame + "'", true,
					ScreenshotType.browser);
		}
	}// end of switchToFrame

	/**
	 * Switch to particular frame using locator
	 * 
	 * @param frameLocator Locator of frame element
	 * 
	 */
	public void switchToFrame(By frameLocator) {
		// variable to hold frame name
		String fname = null;

		try {
			// set frame name
			fname = getLocatorName(frameLocator);

			try {

				// locate frame element
				WebElement frameElement = driver.findElement(frameLocator);

				highlightElement(frameElement, true);

				// switch to frame element using provided locator
				driver.switchTo().frame(frameElement);

				RESULT.PASS("Switched to frame with provided locator -'" + fname + "'", false, ScreenshotType.browser);

			} catch (NoSuchElementException e) {

				RESULT.FAIL("Frame element not found - '" + fname + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in switching frame - '" + fname + "'", e);
			RESULT.FAIL("Error occurred in switching frame - '" + fname + "'", true, ScreenshotType.browser);
		}

	}// end of switchToFrame

	/**
	 * Selects all the options in MultiSelectBox
	 * 
	 * @param multiSelectBoxLocator Locator of MultiSelectBox element
	 * 
	 */
	public void setMultiSelectBox(By multiSelectBoxLocator) {
		// variable to hold MultiSelectBox element
		String locatorName = null, elementOldStyle;

		try {
			// set multiSelectbox element name
			locatorName = getLocatorName(multiSelectBoxLocator);

			try {

				// wait and locate MultiSelectBox box
				waitForElement(multiSelectBoxLocator, 40, WaitType.visibilityOfElementLocated);
				WebElement element = driver.findElement(multiSelectBoxLocator);

				elementOldStyle = highlightElement(element);

				Select multiSelectBox = new Select(element);

				if (multiSelectBox.isMultiple()) {

					// Retrieve all options web elements as List
					List<WebElement> allOptions = multiSelectBox.getOptions();

					multiSelectBox.deselectAll();

					// checking if list contains options web elements
					if (!allOptions.isEmpty()) {

						multiSelectBox.selectByIndex(0);

						keyPress(Keys.SHIFT);

						keyPress(Keys.END);

						if (multiSelectBox.getAllSelectedOptions().size() == allOptions.size()) {
							RESULT.PASS("All multiSelectBox options get Selected ", false, ScreenshotType.browser);
						} else {
							RESULT.FAIL("All options are not selected in multiselectbox -'" + locatorName + "'", true,
									ScreenshotType.browser);
						}

					} else {
						RESULT.FAIL("No option is available in multiselectbox -'" + locatorName + "'", true,
								ScreenshotType.browser);
					}

				} else {

					RESULT.INFO("Specified select box -'" + locatorName + "' is not multi select box", true,
							ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);
			} catch (NoSuchElementException e) {
				RESULT.FAIL("Multiselectbox element not found - " + locatorName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in selecting options in multiselect box'" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in selecting options in multiselect box'" + locatorName + "'", true,
					ScreenshotType.browser);
		}
	}// end of setMultiSelectBox

	/**
	 * Selects specific options in MultiSelectBox
	 * 
	 * @param multiSelectBoxLocator Locator of multiSelectbox
	 *
	 * @param optionValues          option to be selected, it can be multiple string
	 *                              of different options
	 * 
	 */
	public void setMultiSelectBox(By multiSelectBoxLocator, String... optionValues) {
		// variable to hold MultiSelectBox element
		String locatorName = null, elementOldStyle;

		// in-case of invalid option, store in variable for reporting
		String invalidOption = null;

		// get count of options to be selected
		int countOfStringArguments = optionValues.length;

		try {
			// set multiSelectbox element name
			locatorName = getLocatorName(multiSelectBoxLocator);
			try {
				// wait and locate MultiSelectBox box
				waitForElement(multiSelectBoxLocator, 40, WaitType.visibilityOfElementLocated);
				WebElement element = driver.findElement(multiSelectBoxLocator);

				elementOldStyle = highlightElement(element);

				Select multiSelectBox = new Select(element);

				if (multiSelectBox.isMultiple()) {

					if (countOfStringArguments <= multiSelectBox.getOptions().size()) {

						multiSelectBox.deselectAll();

						// Going through each option of multiSelectbox
						for (String valueSelected : optionValues) {

							invalidOption = valueSelected;

							// select desired option
							multiSelectBox.selectByVisibleText(valueSelected);

							keyPress(Keys.CONTROL);

						}
						if (countOfStringArguments == multiSelectBox.getAllSelectedOptions().size()) {

							RESULT.PASS("Specified options get selected in multiselect box'" + locatorName + "'", false,
									ScreenshotType.browser);

						} else {

							RESULT.FAIL(
									"All specified options are not selected in multiselectbox -'" + locatorName + "'",
									true, ScreenshotType.browser);
						}

					} else {

						RESULT.INFO(
								"No of options mentioned in argument should be less or equal to total number of options available in multiselectbox -'"
										+ locatorName + "'",
								true, ScreenshotType.browser);
					}

				} else {

					RESULT.INFO("Specified select box -'" + locatorName + "' is not multi select box", true,
							ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);
			} catch (NullPointerException e) {
				RESULT.FAIL("Specified option -'" + invalidOption + "' is not available in multiselect box - '"
						+ locatorName + "'", true, ScreenshotType.browser);

			} catch (NoSuchElementException e) {
				RESULT.FAIL("Multiselectbox element not found - '" + locatorName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in selecting options in multiselect box'" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in selecting options in multiselect box'" + locatorName + "'", true,
					ScreenshotType.browser);
		}
	}// end of setMultiSelectBox

	/**
	 * Selects specific options in MultiSelectBox- Based on Index
	 * 
	 * @param multiSelectBoxLocator Locator of multiSelectbox
	 *
	 * @param multipleIndex         Integer index of option to be selected, it can
	 *                              be multiple indexes of options
	 * 
	 */
	public void setMultiSelectBox(By multiSelectBoxLocator, int... multipleIndex) {
		// variable to hold MultiSelectBox element
		String locatorName = null, elementOldStyle;

		// in-case of invalid index, store in variable for reporting
		int invalidIndex = 0;

		// get count of options to be selected
		int countOfIntegerArguments = multipleIndex.length;

		try {
			// set multiSelectbox element name
			locatorName = getLocatorName(multiSelectBoxLocator);
			try {
				// wait and locate MultiSelectBox box
				waitForElement(multiSelectBoxLocator, 40, WaitType.visibilityOfElementLocated);
				WebElement element = driver.findElement(multiSelectBoxLocator);

				elementOldStyle = highlightElement(element);

				Select multiSelectBox = new Select(element);

				if (multiSelectBox.isMultiple()) {

					if (countOfIntegerArguments <= multiSelectBox.getOptions().size()) {

						multiSelectBox.deselectAll();

						// Going through each option of multiSelectbox
						for (Integer forEachIndex : multipleIndex) {

							invalidIndex = forEachIndex;

							// select desired option
							multiSelectBox.selectByIndex(forEachIndex);

							keyPress(Keys.CONTROL);

						}
						if (countOfIntegerArguments == multiSelectBox.getAllSelectedOptions().size()) {

							RESULT.PASS("Specified options get selected in multiselect box'" + locatorName + "'", false,
									ScreenshotType.browser);

						} else {

							RESULT.FAIL(
									"All specified options are not selected in multiselectbox -'" + locatorName + "'",
									true, ScreenshotType.browser);
						}

					} else {

						RESULT.INFO(
								"No of index mentioned in argument should be less or equal to total number of options available in multiselectbox -'"
										+ locatorName + "'",
								true, ScreenshotType.browser);
					}

				} else {

					RESULT.INFO("Specified select box -'" + locatorName + "' is not multi select box", true,
							ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);
			} catch (NullPointerException e) {
				RESULT.FAIL("Specified Index -'" + invalidIndex + "' is not available in multiselect box - '"
						+ locatorName + "'", true, ScreenshotType.browser);

			} catch (NoSuchElementException e) {
				RESULT.FAIL("Multiselectbox element not found - '" + locatorName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in selecting options in multiselect box'" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in selecting options in multiselect box'" + locatorName + "'", true,
					ScreenshotType.browser);
		}
	}// end of setMultiSelectBox

	/**
	 * Saving map data to text file
	 * 
	 * @param filepath    Path of text file to save data
	 *
	 * @param dataHashMap Map data to store in text file
	 * 
	 * @param appendFlag  Flag to clear or append the data in existing file.
	 */
	public void saveData(String filePath, Map<String, String> dataHashMap, boolean appendFlag) {
		// Instance creation of JSONObject
		JSONObject jsonObj = new JSONObject();

		// variable to hold data string in json format
		String dataAsJsonString = null;

		// variable to hold status for storing data in file
		boolean flag = false;

		// to store combined data
		Map<String, String> appendedMapData = new HashMap<String, String>();

		try {

			// if want to append with existing
			if (appendFlag) {
				// get existing data
				Map<String, String> getExistingData = getData(filePath);

				// add existing data to new map
				appendedMapData.putAll(getExistingData);

			}

			// add current data to make combined map
			appendedMapData.putAll(dataHashMap);

			// save each map key and value to json object
			jsonObj.putAll(appendedMapData);

			// convert Json object to string
			dataAsJsonString = jsonObj.toJSONString();

			// save data string to text file
			flag = saveData(filePath, dataAsJsonString, false);

			if (flag) {
				RESULT.PASS("Map data is saved successfully in file", false, ScreenshotType.browser);
			} else {
				RESULT.FAIL("Map data is not saved in file", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in saving data in file", e);
			RESULT.FAIL("Error occurred in saving data in file", true, ScreenshotType.browser);
		}
		jsonObj = null;
	}// end of saveMapDataToFile

	/**
	 * Get data from text file and store in Map
	 * 
	 * @param filePath Path of text file to retrieve data
	 *
	 * @return Map file data
	 * 
	 */
	public Map<String, String> getData(String filePath) {

		// Map to store data retrieve from file
		Map<String, String> dataHashMap = new HashMap<String, String>();

		// variable to hold data string in json format
		String dataAsJsonString = null;

		try {
			// read data string(which is in json format) from text file
			dataAsJsonString = getStringData(filePath);

			// Instance creation of JSONParser
			JSONParser parser = new JSONParser();

			// converting json object from json data string
			JSONObject jsonObj = (JSONObject) parser.parse(dataAsJsonString);

			// converting json object to map
			for (Object key : jsonObj.keySet()) {

				dataHashMap.put((String) key, jsonObj.get((String) key).toString());
			}

			RESULT.PASS("Data is retrieved from text file and stored in Map", false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in retrieving data from file", e);
			RESULT.FAIL("Error occurred in retrieving data from file", true, ScreenshotType.browser);
		} catch (ParseException e) {
			BaseSuite.log.error("Exception occurred in converting json object from json data string", e);
			RESULT.FAIL("Error occurred in converting json object from json data string", true, ScreenshotType.browser);
		}

		return dataHashMap;
	}// end of retrieveMapDataFromFile

	/**
	 * Reading data from text file
	 * 
	 * @param filePath Path of text file to read data
	 *
	 * @return String Text file data as String
	 * 
	 */
	public String getStringData(String filePath) {

		// set buffer size
		byte[] contents = new byte[1024];

		// variable to hold byte index
		int bytesRead = 0;

		// variable to store file data
		String strFileData = null;

		File file = new File(filePath);

		try {
			// check file availability
			if (file.exists()) {

				// check if file is readable
				if (file.canRead()) {

					// file input stream creation
					FileInputStream fin = new FileInputStream(filePath);

					// buffer creation
					BufferedInputStream bin = new BufferedInputStream(fin);

					// read till end of file and store as string
					while ((bytesRead = bin.read(contents)) != -1) {

						strFileData = new String(contents, 0, bytesRead);
					}

					// close file
					fin.close();
					bin.close();

					RESULT.PASS("Data is read from text file", false, ScreenshotType.browser);

				} else {

					RESULT.FAIL("File is not readable", true, ScreenshotType.browser);
				}
			} else {

				RESULT.FAIL("No such file exists", true, ScreenshotType.browser);
			}

		} catch (FileNotFoundException e) {
			BaseSuite.log.error("File Not Found", e);
			RESULT.FAIL("File Not Found", true, ScreenshotType.browser);
		} catch (IOException e) {
			BaseSuite.log.error("Exception occurred in reading file", e);
			RESULT.FAIL("Error occurred in reading file", true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred while reading file", e);
			RESULT.FAIL("Error occurred while reading file", true, ScreenshotType.browser);
		}

		return strFileData;
	}// end of readDataFromTxt

	/**
	 * Writing data in text file
	 * 
	 * @param filePath   Path of text file to write
	 *
	 * @param strText    Data to store in text file
	 * 
	 * @param appendFlag Flag to clear or append the data in existing file.
	 * 
	 * @return data save or not Boolean
	 *
	 */
	public boolean saveData(String filePath, String strText, boolean appendFlag) {

		boolean dataSaved = false;

		BufferedOutputStream bufferedOutput = null;

		try {

			File file = new File(filePath);

			// check file availability
			if (file.exists()) {

				// check if file is writable
				if (file.canWrite()) {

					// Output Buffer creation
					bufferedOutput = new BufferedOutputStream(new FileOutputStream(filePath, appendFlag));

					// writing string to file
					bufferedOutput.write(strText.getBytes());

					// close file
					bufferedOutput.close();

					RESULT.PASS(strText + " - Written in text file successfully", false, ScreenshotType.browser);
					dataSaved = true;

				} else {
					RESULT.FAIL("File is not writable", true, ScreenshotType.browser);
				}
			} else {
				RESULT.FAIL("No such file exists", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in writing in file", e);
			RESULT.FAIL("Error occurred in writing in file", true, ScreenshotType.browser);
		} catch (FileNotFoundException e) {
			BaseSuite.log.error("File Not Found", e);
			RESULT.FAIL("File Not Found", true, ScreenshotType.browser);
		} catch (IOException e) {
			BaseSuite.log.error("Exception occurred in reading file", e);
			RESULT.FAIL("Error occurred in reading file", true, ScreenshotType.browser);
		}

		return dataSaved;

	}// end of writeDataToTxt

	/**
	 * Converting 2-Dimensional Array to Map
	 * 
	 * @param arrayItems String 2-Dimensional Array
	 *
	 * @return Map Map<String,String> with both key and value as String
	 * 
	 */
	public Map convertArrayToMap(String[][] arrayItems) {

		// Map declaration
		Map mapItems = new HashMap<String, String>();

		try {
			// Converting 2-dimensional array to Map
			mapItems = ArrayUtils.toMap(arrayItems);

			RESULT.PASS("Array to Map conversion completed successfully", false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in converting map", e);
			RESULT.FAIL("Error occurred in converting map", true, ScreenshotType.browser);
		}
		return mapItems;
	}// end of convertArrayToMap

	/**
	 * Key Press
	 * 
	 * @param compulsoryKey - One key that is mandatory to be pressed
	 * @param keys          - If we want to press more than one key
	 */
	public void keyPress(Keys compulsoryKey, Keys... keys) {
		Action action;
		try {

			// For storing all keys, that are to be pressed
			Keys totalKeysToBePressed[] = new Keys[keys.length + 1];

			// One Key is compulsory that will be stored as first key to be pressed
			totalKeysToBePressed[0] = compulsoryKey;

			// string to print
			String keysPressed = compulsoryKey.name();

			for (int i = 1; i < totalKeysToBePressed.length; i++) {
				// Storing other keys in array
				totalKeysToBePressed[i] = keys[i - 1];

				keysPressed += "+" + totalKeysToBePressed[i].name();
			}

			// build the action
			action = actions.sendKeys((totalKeysToBePressed)).build();

			// perform action
			action.perform();

			RESULT.PASS("Virtual Key " + keysPressed.replaceFirst("\\+", "") + " is pressed successfully", false,
					ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in pressing virtual key", e);
			RESULT.FAIL("Error occurred in pressing virtual key", true, ScreenshotType.browser);
		}
	}

	/**
	 * Key Press on specific element
	 * 
	 * @param elementLocator - Element on which we want to send keys
	 * @param compulsoryKey  - One key that is mandatory to be pressed
	 * @param keys           - If we want to press more than one key
	 */
	public void keyPress(By elementLocator, Keys compulsoryKey, Keys... keys) {
		Action action;
		String locatorName = getLocatorName(elementLocator);
		String elementOldStyle;
		try {
			// wait and scroll to particular element
			waitForElement(elementLocator, 40, WaitType.visibilityOfElementLocated);
			scrollToElement(elementLocator);

			// find the element on which to send keys
			WebElement element = getWebElement(elementLocator);

			elementOldStyle = highlightElement(element);

			// For storing all keys, that are to be pressed
			Keys totalKeysToBePressed[] = new Keys[keys.length + 1];

			// One Key is compulsory that will be stored as first key to be pressed
			totalKeysToBePressed[0] = compulsoryKey;

			// string to print
			String keysPressed = compulsoryKey.name();

			for (int i = 1; i < totalKeysToBePressed.length; i++) {

				// Storing other keys in array
				totalKeysToBePressed[i] = keys[i - 1];

				keysPressed += "+" + totalKeysToBePressed[i].name();
			}

			// build the action
			action = actions.click(element).sendKeys(totalKeysToBePressed).build();

			// perform action
			action.perform();

			RESULT.PASS(
					"Virtual Key " + keysPressed.replaceFirst("\\+", "") + " is pressed successfully on " + locatorName,
					false, ScreenshotType.browser);

			unHighlightElement(element, elementOldStyle);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in pressing virtual key", e);
			RESULT.FAIL("Error occurred in pressing virtual key", true, ScreenshotType.browser);
		}
	}// end of keyPress

	/**
	 * Get text of web element
	 * 
	 * @param locator locator of web element
	 * 
	 * @param useJS
	 * 
	 * @return String Text of web element as String
	 * 
	 * 
	 */
	public String getTextWebelement(By locator, boolean... useJS) {

		// variable to hold element name
		String locatorName = null;

		// variable to hold text of web element
		String text = "";

		try {
			// set element name
			locatorName = getLocatorName(locator);

			// check if element available
			if (isElementExists(locator)) {

				WebElement element = driver.findElement(locator);
				String oldStyle = highlightElement(element);

				if (useJS.length > 0 && useJS[0]) { // get text content
					// Create an object of JavaScriptExector Class
					JavascriptExecutor executor = (JavascriptExecutor) driver;

					// get text content if js
					text = String.valueOf(executor.executeScript("return arguments[0].textContent", element));
				} else
					// get inner text
					text = element.getText();

				// parse the text to avoid having HTML in the result to avoid disturbing the
				// HTML report
				text = Jsoup.parse(text).text();

				unHighlightElement(element, oldStyle);
			} else {
				RESULT.FAIL("Element not found -'" + locatorName + "'", true, ScreenshotType.browser);
			}
		} catch (TimeoutException e) {
			BaseSuite.log.error("Not able to get text for element'" + locatorName + "'", e);
			RESULT.FAIL("Not able to get text for element'" + locatorName + "'", true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Not able to get text for element'" + locatorName + "'", e);
			RESULT.FAIL("Not able to get text for element'" + locatorName + "'", true, ScreenshotType.browser);
		}
		return text;
	}// end of getTextWebelement

	/**
	 * Convert locator string to By type considering 2 case: By & WebElement
	 * toString()
	 * 
	 * @param strLocator element locator as String
	 * 
	 * @return By return elementLocator as By
	 * 
	 * 
	 */
	public By locatorParser(String strLocator) {

		// variable to hold element locator
		By elementLocator = null;

		// get locator string excluding locator type(i.e. xpath, id etc)
		try {

			String locatorString = strLocator.substring(strLocator.indexOf(":") + 1, strLocator.length()).trim();

			// get locator type string (i.e. xpath, id etc)
			// first index based on the locator coming
			String locatorType = strLocator
					.substring(strLocator.startsWith("By.") ? strLocator.indexOf(".") + 1 : 0, strLocator.indexOf(":"))
					.trim();

			// based on locator type string it returns element locator object
			switch (locatorType) {

			// returns By id locator
			case "id":
				elementLocator = By.id(locatorString);
				break;

			// returns By name locator
			case "name":
				elementLocator = By.name(locatorString);
				break;

			// returns By partialLinkText locator
			case "partialLinkText":
			case "partial link text":
				elementLocator = By.partialLinkText(locatorString);
				break;

			// returns By className locator
			case "className":
			case "class name":
				elementLocator = By.className(locatorString);
				break;

			// returns By cssSelector locator
			case "cssSelector":
			case "css selector":
				elementLocator = By.cssSelector(locatorString);
				break;

			// returns By cssSelector locator
			case "tag name":
			case "tagName":
				elementLocator = By.tagName(locatorString);
				break;

			// returns By linkText locator
			case "linkText":
			case "link text":
				elementLocator = By.linkText(locatorString);
				break;

			// returns By xpath locator
			case "xpath":
				elementLocator = By.xpath(locatorString);
				break;

			// invalid locator type
			default:
				RESULT.FAIL("Invalid locator type", true, ScreenshotType.browser);
			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred while parsing locator", e);
			RESULT.FAIL("Error occurred while parsing locator", true, ScreenshotType.browser);
		}
		return elementLocator;
	}

	/**
	 * Enter value in password text field
	 * 
	 * @param locator  Specifies Text Field locator
	 * 
	 * @param password Specifies value that needs to be entered in password text
	 *                 field
	 * 
	 */
	public void setPassword(By locator, String password) {

		// variable to hold name of element
		String locatorName = null, elementOldStyle;

		// variable to hold asterisk string
		String strEcho = null;

		try {
			// set element name
			locatorName = getLocatorName(locator);

			try {

				// get password text field element
				WebElement element = driver.findElement(locator);

				elementOldStyle = highlightElement(element);

				// get asterisk string
				strEcho = password.replaceAll("(?s).", "*");

				// clear password text field
				element.clear();

				// enter password in password field
				element.sendKeys(password);

				// get entered value
				String getText = element.getAttribute("value");

				// verify entered value
				if (getText.equals(password)) {
					RESULT.PASS(strEcho + " is entered in '" + locatorName + "' password text field", false,
							ScreenshotType.browser);

				} else {
					RESULT.FAIL(
							getText + " is entered in '" + locatorName + "' password text field instead of " + strEcho,
							true, ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);
			} catch (NoSuchElementException e) {
				RESULT.FAIL("Element not found - '" + locatorName + "'", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in entering " + password + " in '" + locatorName, e);
			RESULT.FAIL("Error occurred in entering " + password + " in '" + locatorName, true, ScreenshotType.browser);
		}
	}// end of setPassword

	/**
	 * Pause - ThreadSleep
	 * 
	 * @param timeInSeconds time in timeInSeconds
	 * 
	 */
	public void pause(int timeInSeconds) {
		try {
			// Thread sleep for provided time
			Thread.sleep(timeInSeconds * 1000);

		} catch (InterruptedException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error(executionInterrupted, e);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in Thread sleeping", e);
			RESULT.FAIL("Error occurred in Thread sleeping", true, ScreenshotType.browser);
		}
	}// end of threadSleep

	/**
	 * Exit from current test
	 * 
	 * @param description Failed test description
	 * 
	 * @param screenshot  to take screenshot or not
	 * 
	 * 
	 */
	public void exitTest(String description, boolean screenshot, ResultType... resultType) {

		ResultType result = ResultType.FAIL;

		// change based on user input
		if (resultType.length > 0)
			result = resultType[0];

		// show result based on the result type
		if (ResultType.FAIL.equals(result)) {
			RESULT.FAIL(description, screenshot, ScreenshotType.fullScreen);
		} else {
			RESULT.WARNING(description, screenshot, ScreenshotType.fullScreen);
			// added "WRN: " to identify warning in case of assert to skip while re run
			description = ResultType.WARNING + " " + description;
		}

		assertTest(description);

	}
	// part of exitTest function to exit from current test

	public static void assertTest(String description) {
		// get exit from current test
		Assert.assertTrue(false, description);
	}// end of exitTest

	/**
	 * Terminate execution
	 * 
	 * @param description description for test execution termination
	 * 
	 * @param screenshot  to take screenshot or not
	 * 
	 */
	public void exitApplication(String description, boolean screenshot) {

		// Log execution termination description in Report
		RESULT.FAIL(description, screenshot, ScreenshotType.fullScreen, true);

		// add report to be logged in HTML
		RESULT.endTest();

		// termination of driver session
		BaseSuite.setUp.tearDown();

		// close report
		RESULT.terminate();

		// upload and email
		BaseSuite base = new BaseSuite();
		base.uploadEmailReport();

		// terminate execution
		System.exit(0);

	}// end of exitApplication

	/**
	 * Get List of WebElement
	 * 
	 * @param parentClass Specifies WebElement of parent Class
	 *
	 * @param childClass  Specifies locator of child Class
	 * 
	 * @return List<WebElement> List of WebElement
	 * 
	 * 
	 */
	public List<WebElement> getList(By locator) {
		String locatorName = null;
		try {
			locatorName = getLocatorName(locator);
			List<WebElement> list = driver.findElements(locator);
			if (list != null) {
				return list;
			} else {
				RESULT.FAIL(locatorName + " List is null", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting List of '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in getting List of '" + locatorName + "'", true, ScreenshotType.browser);
		}
		return null;
	}

	/**
	 * click on WebElement
	 * 
	 * @param webElement Specifies WebElement
	 * 
	 */
	public void click(WebElement element) {
		String elementName = getLocatorName(getByLocator(element));
		try {
			String oldStyle = highlightElement(element);

			element.click();

			unHighlightElement(element, oldStyle);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in clicking '" + elementName + "'", e);
			RESULT.FAIL("Error occurred in clicking '" + elementName + "'", true, ScreenshotType.browser);
		}
	}// end of click

	/**
	 * This will return the currently focused page URL String
	 * 
	 */
	public String getCurrentURL() {
		String URL = null;
		try {
			URL = driver.getCurrentUrl();
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting current URL", e);
			RESULT.FAIL("Error occurred in getting current URL", true, ScreenshotType.browser);
		}
		return URL;
	}// end of getCurrentURL

	/**
	 * This will return the currently focused HTML page source in String
	 * 
	 */
	public String getCurrentPageSource() {
		String pageSource = null;
		try {
			pageSource = driver.getPageSource();
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting current page source", e);
			RESULT.FAIL("Error occurred in getting current page source", true, ScreenshotType.browser);
		}
		return pageSource;
	}// end of getCurrentPageSource

	/**
	 * This will provide you with css property of a given web element in form of
	 * string
	 * 
	 * @param locator      web element of which given css property to be returned
	 * 
	 * @param propertyName css property name for given web element
	 * 
	 * @return String - CSS property value
	 */
	public String getCSSvalue(By locator, String propertyName) {
		String locatorName = null;
		String cssValue = null;
		try {

			// to show the locator name if exception occurred
			locatorName = getLocatorName(locator);

			// check if element exist
			if (isElementExists(locator)) {
				// get webelement and return css value of same
				cssValue = getWebElement(locator).getCssValue(propertyName);
			} else {
				RESULT.FAIL(locatorName + " - does not exist to get the CSS value", true, ScreenshotType.browser);
			}

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting css value of '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in getting css value of '" + locatorName + "'", true, ScreenshotType.browser);
		}

		// return the css value
		return cssValue;
	}// end of getCSSvalue

	/**
	 * Get By element object from a given web element
	 * 
	 * @param element WebElement to be converted into By element for further use
	 * 
	 * @return By element converted from given WebElement
	 */
	public By getByLocator(WebElement element) {
		// variable to hold string value of element
		String stringElement = null;
		By byLocator = null;
		try {
			// get locator part in string
			stringElement = element.toString().substring(element.toString().indexOf("-> ") + 3,
					element.toString().length() - 1);
			// convert locator string to By
			byLocator = locatorParser(stringElement);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting By locator '" + element + "'", e);
			RESULT.FAIL("Exception occurred while getting locator of element", true, ScreenshotType.browser);
		}
		return byLocator;
	}

	/**
	 * This will return you the current opened page/window's title in form of String
	 */
	public String getPageTitle() {

		String titleOfPage = null;
		try {
			titleOfPage = driver.getTitle();
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in getting page title", e);
			RESULT.FAIL("Exception occurred while getting title of the page", true, ScreenshotType.browser);
		}

		return titleOfPage;
	}

	/**
	 * Wait for ajax control to load fully. It will take already defined explicit
	 * wait timeout given.
	 */
	public void waitForAJAXLoad() {

		try {
			// System.out.println(((JavascriptExecutor)driver).executeScript("return
			// jQuery.active"));
			wait.until(driver -> ((Long) ((JavascriptExecutor) driver).executeScript("return jQuery.active") == 0));
		} catch (TimeoutException e) {
			RESULT.FAIL("Timeout waiting for ajax to load", true, ScreenshotType.browser);
		} catch (Exception e) {
			// no jQuery present
			BaseSuite.log.info("jQuery not present on page");
		}
	}// end of waitForAJAXLoad

	/**
	 * Wait for ajax control to load fully within given seconds
	 * 
	 * @param waitInSeconds integer wait in seconds
	 * 
	 */
	public void waitForAJAXLoad(int waitInSeconds) {

		// change wait timeout based on user parameter
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitInSeconds));

		try {
			wait.until(driver -> ((Long) ((JavascriptExecutor) driver).executeScript("return jQuery.active") == 0));
		} catch (TimeoutException e) {
			RESULT.FAIL("Timeout waiting for ajax to load", true, ScreenshotType.browser);
		} catch (Exception e) {
			// no jQuery present
			BaseSuite.log.info("jQuery not present on page");
		}
	}// end of waitForAJAXLoad

	/**
	 * To close the current opened window
	 */
	public void closeWindow() {
		try {
			driver.close();
			RESULT.PASS("Closed the current focused window", false, ScreenshotType.browser);
		} catch (TimeoutException e) {
			RESULT.FAIL("Failed to close the current window", true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			RESULT.FAIL("Failed to close the current window", true, ScreenshotType.fullScreen);
			BaseSuite.log.error("Unexpected Exception occurred in closing the current window", e);
		}
	}// end of closeWindow

	/**
	 * Click using javascript and WebElement
	 * 
	 * @param WebElement
	 */
	public void javaScriptClick(WebElement element) {
		JavascriptExecutor executor;
		String elementName = getLocatorName(getByLocator(element));

		try {
			// highlight & un-highlight quickly as it might navigate to other page
			String oldStyle = highlightElement(element);
			unHighlightElement(element, oldStyle);

			// Create an object of JavaScriptExector Class
			executor = (JavascriptExecutor) driver;

			// click on WebElement
			executor.executeScript("arguments[0].click();", element);
			RESULT.PASS(elementName + " is  clicked ", false, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in Javascript clicking '" + elementName + "'", e);
			RESULT.FAIL("Error occurred in Javascript clicking '" + elementName + "'", true, ScreenshotType.browser);
		}

		executor = null;
	}// end of javaScriptClick

	/**
	 * To scroll to given element inside browser default window using By locator
	 * 
	 * @param locator: By
	 */
	public void jsScrollToElement(By locator) {
		JavascriptExecutor executor;
		String elementName = getLocatorName(locator);

		try {
			// Create instance of Javascript executor
			executor = (JavascriptExecutor) driver;

			try {
				// Identify the WebElement which will appear after scrolling down
				WebElement element = driver.findElement(locator);

				// now execute query which actually will scroll until that element is not
				// appeared on page.
				executor.executeScript(
						"arguments[0].scrollIntoView({behavior: \"instant\", block: \"center\", inline: \"center\"});",
						element);
				RESULT.PASS("Scrolled to given locator - " + elementName, false, ScreenshotType.browser);

			} catch (NoSuchElementException e1) {
				RESULT.FAIL("Given element is not available to scroll to", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in scroll to element '" + elementName + "'", e);
			RESULT.FAIL("Failed to scroll to " + elementName, true, ScreenshotType.browser);
		}

		executor = null;
	}// end of jsScrollToElement

	/**
	 * To scroll to given element inside browser default window using WebElement
	 * 
	 * @param locator: WebElement
	 */
	public void jsScrollToElement(WebElement element) {
		JavascriptExecutor executor;
		String elementName = getLocatorName(getByLocator(element));

		try {
			// Create instance of Javascript executor
			executor = (JavascriptExecutor) driver;

			// now execute query which actually will scroll until that element is not
			// appeared on page.
			executor.executeScript(
					"arguments[0].scrollIntoView({behavior: \"instant\", block: \"center\", inline: \"center\"});",
					element);
			RESULT.PASS("Scrolled to given element - " + elementName, false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in scroll to element '" + elementName + "'", e);
			RESULT.FAIL("Failed to scroll to " + elementName, true, ScreenshotType.browser);
		}

		executor = null;
	}// end of jsScrollToElement

	/**
	 * To scroll to given By locator inside given By locator
	 * 
	 * @param parentLocator:   By locator of window under which scroll available
	 * 
	 * @param scrollToLocator: By locator of the element where scroll needs to be
	 *                         done
	 * 
	 */
	public void jsScrollToElement(By parentLocator, By scrollToLocator) {
		JavascriptExecutor executor;
		String parentName = getLocatorName(parentLocator);
		String scrollToName = getLocatorName(scrollToLocator);

		try {
			// Create instance of Javascript executor
			executor = (JavascriptExecutor) driver;

			try {
				// Identify the WebElement which will appear after scrolling down
				WebElement parentElement = getWebElement(parentLocator);
				WebElement scrollToElement = getWebElement(scrollToLocator);

				// now execute query which actually will scroll until that element is not
				// appeared on page.
				executor.executeScript("arguments[0].scrollTop=arguments[1].offsetTop", parentElement, scrollToElement);
				RESULT.PASS("Scrolled to given locator - " + scrollToName, false, ScreenshotType.browser);

			} catch (NoSuchElementException e1) {
				RESULT.FAIL("Given element is not available to scroll to", true, ScreenshotType.browser);
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in scroll to element '" + scrollToName + "' in parent locator '"
					+ parentName + "'", e);
			RESULT.FAIL("Failed to scroll to " + scrollToName, true, ScreenshotType.browser);
		}

	}// end of jsScrollToElement

	/**
	 * To scroll to given WebElement inside given parent WebElement
	 * 
	 * @param parentElement:   WebElement of window under which scroll available
	 * 
	 * @param scrollToElement: WebElement of the element where scroll needs to be
	 *                         done
	 * 
	 */
	public void jsScrollToElement(WebElement parentElement, WebElement scrollToElement) {
		JavascriptExecutor executor;
		String parentName = getLocatorName(getByLocator(parentElement));
		String scrollToName = getLocatorName(getByLocator(scrollToElement));

		try {
			// Create instance of Javascript executor
			executor = (JavascriptExecutor) driver;

			// now execute query which actually will scroll until that element is not
			// appeared on page.
			executor.executeScript("arguments[0].scrollTop=arguments[1].offsetTop", parentElement, scrollToElement);
			RESULT.PASS("Scrolled to given locator - " + scrollToName, false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in scroll to element '" + scrollToName
					+ "' in given parent element '" + parentName + "'", e);
			RESULT.FAIL("Failed to scroll to " + scrollToName, true, ScreenshotType.browser);
		}

		executor = null;
	}// end of jsScrollToElement

	/**
	 * To double click on given element using javascript executor
	 * 
	 * @param locator: By
	 */
	public void jsDoubleClick(By locator) {
		JavascriptExecutor executor;
		// set locator name
		String locatorName = null, elementOldStyle;
		try {
			locatorName = getLocatorName(locator);

			// get WebElement of locator
			WebElement element = driver.findElement(locator);

			elementOldStyle = highlightElement(element);
			try {

				// Create an object of JavaScriptExector Class
				executor = (JavascriptExecutor) driver;

				// create command to double click
				String doubleClickJS = "var evObj = document.createEvent('MouseEvents'); "
						+ "evObj.initEvent('dblclick',true, false); arguments[0].dispatchEvent(evObj);";
				// double click on WebElement
				executor.executeScript(doubleClickJS, element);

				RESULT.PASS(locatorName + " is  double clicked ", false, ScreenshotType.browser);
			} catch (WebDriverException e) {
				checkInterrupted(e, executionInterrupted, ResultType.FAIL);
				BaseSuite.log.error("Exception occurred in Javascript double clicking '" + locatorName + "'", e);
				RESULT.FAIL("Error occurred in Javascript double clicking '" + locatorName + "'", true,
						ScreenshotType.browser);
			}

			unHighlightElement(element, elementOldStyle);
		} catch (NoSuchElementException e) {
			RESULT.FAIL("Element not found - " + locatorName, true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Unexpected exception occurred in Javascript double clicking '" + locatorName + "'", e);
			RESULT.FAIL("Error occurred in Javascript double clicking '" + locatorName + "'", true,
					ScreenshotType.browser);
		}
	}// end of jsDoubleClick

	/**
	 * To get the inner Text of given locator - can be used to get the text with
	 * proper spaces in statement
	 * 
	 * @param elementLocator locator of web element
	 * 
	 * @return String inner text of web element
	 * 
	 */
	public String jsGetInnerText(By elementLocator) {
		// variable to hold element name
		String elementName = null, elementOldStyle;

		// variable to hold text of web element
		String innerText = "";

		try {
			// set element name
			elementName = getLocatorName(elementLocator);

			// check if element available
			if (isElementExists(elementLocator)) {
				WebElement element = driver.findElement(elementLocator);

				elementOldStyle = highlightElement(element);

				// Create an object of JavaScriptExector Class
				JavascriptExecutor executor = (JavascriptExecutor) driver;

				// clear value
				innerText = String.valueOf(executor.executeScript("return arguments[0].innerText", element));

				RESULT.INFO("Inner Text of " + elementName + " element is - " + innerText, false,
						ScreenshotType.browser);

				unHighlightElement(element, elementOldStyle);
			} else {
				RESULT.FAIL("Element not found -'" + elementName + "' for getting inner text", true,
						ScreenshotType.browser);
			}
		} catch (TimeoutException e) {
			BaseSuite.log.error("Not able to get text using .innerText for element'" + elementName + "'", e);
			RESULT.FAIL("Not able to get text for element'" + elementName + "'", true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Not able to get text using .innerText for element'" + elementName + "'", e);
			RESULT.FAIL("Not able to get text for element'" + elementName + "'", true, ScreenshotType.browser);
		}
		return innerText;
	}// end of jsGetInnerText

	/**
	 * To change the implicit wait timeout. We can use it in the case of explicit
	 * loading.
	 * 
	 * @param implicitWait in seconds
	 */
	public void changeImplicitWait(int implicitWait) {
		try {
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
			RESULT.PASS("Default Element wait is updated to - " + implicitWait, false, ScreenshotType.browser);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception Occured while changing the imlicit wait.", e);
		}
	}// changeImplicitWait

	/**
	 * To reset the implicit wait back to normal as per the configuration wait.
	 */
	public void resetImplicitWait() {
		try {
			int implicitWait = Integer.parseInt(Configuration.getProperty("waitInSeconds"));
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
			RESULT.PASS("Default Element wait is updated to - " + implicitWait, false, ScreenshotType.browser);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception Occured while reseting the imlicit wait.", e);
		}
	}// resetImplicitWait

	/**
	 * To reset the execution if any abrupt failure occur in a scenario. This will
	 * close all window apart from the global parent widow and switch to default
	 * content in global parent window
	 * 
	 */
	public void resetExecution() {
		BaseSuite.log.info("Started to reset execution");
		// for handling on exception in resetting to ending the test properly in report
		try {
			// handle any unexpected alert
			handleUnexpectedAlerts(2, false);

			// get the window handles excluding parent win handle
			List<String> winHandles = getWindowHandles().stream().filter(e -> !e.equals(globalWinHandle))
					.collect(Collectors.toList());

			// close all window other than parent window
			if (!winHandles.isEmpty()) {

				// close all windows
				for (String handle : winHandles) {
					switchToWindowUsingHandle(handle);
					driver.close();
				}
			}

			// switch to parent window
			switchToWindowUsingHandle(globalWinHandle);

			// switch to default content
			switchToDefaultContent();
		} catch (Exception e) {
			BaseSuite.log.error("Abrupt exeption while doing reset of execution: ", e);
		}
		BaseSuite.log.info("Ended to reset execution");
	}// end of resetExecution

	/**
	 * Handle unexpected alerts and give warnings
	 * 
	 * @param wait     in seconds
	 * @param logAlert to log the alert not found message in report
	 */
	public void handleUnexpectedAlerts(int wait, boolean logAlert) {

		// checking for alert
		while (isAlertPresent(wait, logAlert)) {
			RESULT.INFO("Current alert text: " + getAlertText(), true, ScreenshotType.fullScreen);
			alertAction(AlertAction.Accept);
		}

	}// end of handleUnexpectedAlerts

	/**
	 * Function to fetch whole column data from excel sheet
	 * 
	 * @param excelFilePath Specifies excel file(.xls) path
	 *
	 * @param sheetName     Specifies sheet name
	 * @param columnName    Specifies column name
	 * 
	 * @return List<Object> column data as Objects
	 * 
	 * 
	 */
	public List<Object> getColumnDataFromExcel(String excelFilePath, String sheetName, String columnName) {
		List<Object> columnData = new ArrayList<Object>();

		try {
			// excel file object initialization
			File excelFile = new File(excelFilePath);
			FileInputStream fis = new FileInputStream(excelFile);
			HSSFWorkbook workBook = new HSSFWorkbook(fis);
			HSSFSheet sheet = workBook.getSheet(sheetName);
			HSSFRow row = sheet.getRow(0);

			// variables to hold total columns, rows, column index, data flag
			// and whole column data
			int totalColumns = row.getLastCellNum();
			int totalRows = sheet.getLastRowNum() + 1;
			int columnNumber = 0;
			boolean isColumnExist = false;

			// fetch column index based on provided column name
			for (int columnIndex = 0; columnIndex < totalColumns; columnIndex++) {

				String ColumnName = sheet.getRow(0).getCell(columnIndex).getStringCellValue();

				if (ColumnName.equalsIgnoreCase(columnName)) {
					columnNumber = columnIndex;
					break;
				} else if (columnIndex == totalColumns - 1) {
					RESULT.FAIL("Invalid Column Name", true, ScreenshotType.browser);
					isColumnExist = true;
				}
			}
			// if provided column is exist in sheet, fetch whole column data
			if (!isColumnExist) {
				for (int rowIndex = 1; rowIndex < totalRows; rowIndex++) {
					columnData.add(sheet.getRow(rowIndex).getCell(columnNumber).getStringCellValue());
				}
			}

			workBook.close();
		} catch (Exception e) {
			BaseSuite.log.error("Exception occured while getting column data from excel", e);
			RESULT.FAIL("Reading excel column data failed for file - " + excelFilePath, true, ScreenshotType.browser);
		}

		return columnData;
	}// end of getColumnDataFromExcel

	/**
	 * Function to get visible text of selected option in dropdown. if no text will
	 * take the value
	 * 
	 * @param element    dropdown element
	 * 
	 * @param goForValue make it false to ignore value attribute check when no text
	 * 
	 */
	public String getVisibleTextOfSelectedOption(WebElement element, boolean... goForValue) {
		String visibleText = "";

		// check for value flag
		boolean checkValue = true;
		if (goForValue.length > 0)
			checkValue = goForValue[0];

		if (element != null) {
			try {
				String oldStyle = highlightElement(element);
				Select dropDown = new Select(element);

				visibleText = dropDown.getFirstSelectedOption().getText().trim();

				// check if there is no text then try with 'value' attribute
				if (visibleText.length() == 0 && checkValue)
					visibleText = dropDown.getFirstSelectedOption().getAttribute("value");

				unHighlightElement(element, oldStyle);
			} catch (Exception e) {
				BaseSuite.log.error("Exception occured while getting text of selected option from the dropdown", e);
				RESULT.FAIL("Element is not a dropdown", true, ScreenshotType.browser);
			}
		} else {
			RESULT.FAIL("Drop down element not found", true, ScreenshotType.browser);
		}
		return visibleText;
	}// end of getVisibleTextOfSelectedOption

	/**
	 * Get class name of the given argument object/page
	 * 
	 * @param page
	 * @return class name of the given object
	 */
	public String screenName(Object page) {
		String pageName = page.getClass().getSimpleName();
		return pageName;
	}

	/**
	 * To get the current year in YYYY format
	 * 
	 * @return
	 */
	public String currentYear() {
		SimpleDateFormat DtFormat = new SimpleDateFormat("yyyy");
		Date date = new Date();
		String year = DtFormat.format(date).toString();
		return year;

	}

	/**
	 * To get the current date in MMddyyyy format
	 * 
	 * @return
	 */
	public String currentDate() {
		SimpleDateFormat DtFormat = new SimpleDateFormat("MM/dd/yyyy");
		Date date = new Date();
		String currDate = DtFormat.format(date).toString();
		return currDate;
	}

	/**
	 * Returns amount of files in the folder (Return '-1' if path not exist)
	 *
	 * @param path is path to target directory
	 *
	 */
	public int getFilesCount(String path) {
		int fileCount = 0;

		// if directory exists
		if (Files.exists(Paths.get(path))) {
			fileCount = new File(path).listFiles().length;
		} else {
			RESULT.FAIL(path + " directory path not exist.", false, ScreenshotType.browser);
			fileCount = -1;
		}
		return fileCount;
	}

	/**
	 * This will switch to the new window and wait for the page to load.
	 * 
	 * @param currentWinHandle
	 */

	public void switchToNewWindow(String... currentWinHandles) {

		// get the opened window handle excluding previous window handle
		List<String> windowHandles = getWindowHandles();

		for (String handle : currentWinHandles) {
			windowHandles.remove(handle);
		}

		String switchWinHanle = windowHandles.get(0);

		// switch to opened window
		switchToWindowUsingHandle(switchWinHanle);

		// wait and verify page for error
		waitForPageLoad();
	}

	/**
	 * Handle alert based on given parameters.
	 * 
	 * @param alertText
	 * @param alertAction
	 * @param timeInSeconds
	 * @return false if alert is not present or not matched with given alertText and
	 *         true if current alert text matched with the alertText.
	 */
	public boolean handleAlert(String alertText, AlertAction alertAction, int timeInSeconds) {
		boolean alertHandled = false;
		if (isAlertPresent(timeInSeconds, false)) {
			String currentAlertText = getAlertText();

			RESULT.INFO("Current alert text: " + currentAlertText + ". Expected alert text: " + alertText, true,
					ScreenshotType.fullScreen);

			if (currentAlertText.contains(alertText)) {
				alertAction(alertAction);
				alertHandled = true;
			}
		} else {
			RESULT.INFO("Alert is not present. While looking for following alert: " + alertText, true,
					ScreenshotType.fullScreen);
		}
		return alertHandled;
	}

	/**
	 * For getting data of particular cell in web table
	 * 
	 * @param tableLocator Locator of web table to fetch data
	 * 
	 * @param rowElement   Row element to get cell data
	 * 
	 * @param columnName   Column Name to get cell data as String
	 * 
	 * @return String Desired cell data of web table as String
	 * 
	 * 
	 */
	public String getCellData(By tableLocator, WebElement rowElement, String columnName) {
		// variables to hold row and column index

		int columnNumber = 0;

		// variables to hold table name and cell data
		String tableName = null, elementOldStyle;
		String cellData = null;

		// locator for row data - containing tags 'td' and 'th'
		By rowDataLocator = By.xpath("./*['td' or 'th']");

		try {
			// set web table name
			tableName = getLocatorName(tableLocator);

			try {

				// wait and locate web table to fetch particular cell data
				waitForElement(tableLocator, 40, WaitType.visibilityOfElementLocated);
				WebElement tableElement = driver.findElement(tableLocator);
				elementOldStyle = highlightElement(tableElement);

				// get all rows data
				List<WebElement> trCollection = tableElement.findElements(By.xpath("./*/*['tr']"));

				// checking rows of web table are fetched or not
				if (!trCollection.isEmpty()) {

					// going through each row in web table
					for (WebElement trElement : trCollection) {

						// get all the data of first row
						List<WebElement> td_col = trElement.findElements(rowDataLocator);

						// checking if data of first row is fetched or not
						if (!td_col.isEmpty()) {

							// get column number for given column name
							for (int i = 0; i < td_col.size(); i++) {
								if (td_col.get(i).getText().contains(columnName)) {
									columnNumber = i;
									break;
								}
							}
						} else {

							RESULT.FAIL("No column name is available to get column number in web table - '" + tableName
									+ "'", true, ScreenshotType.browser);
						}

						break;

					}
					// get data of row
					List<WebElement> td_collection = rowElement.findElements(By.xpath("./*['td' or 'th']"));

					// checking if data of row is fetched or not
					if (!td_collection.isEmpty()) {

						// get cell data for particular row and column
						cellData = td_collection.get(columnNumber).getText();

					} else {

						RESULT.FAIL(
								"No row is available to fetch particular cell data in web table - '" + tableName + "'",
								true, ScreenshotType.browser);
					}

				}

				unHighlightElement(tableElement, elementOldStyle);

				return cellData;

			} catch (NoSuchElementException e) {

				RESULT.FAIL(("Web table not found - '" + tableName + "'"), false, ScreenshotType.browser);
			}

		} catch (

		WebDriverException e) {
			BaseSuite.log.error("Exception occurred in getting cell data in web table - '" + tableName + "'", e);
			RESULT.FAIL("Exception occurred in getting cell data in web table - '" + tableName + "'", false,
					ScreenshotType.browser);
		}
		return null;
	}// end of getCellData

	/**
	 * Java enter value on WebElement
	 * 
	 * @param locator locator of WebElement
	 * 
	 * @param value   value to be entered in the locater
	 * 
	 */
	public void javaScriptSetValue(By locator, String value) {
		JavascriptExecutor executor;

		// set locator name
		String locatorName = null, elementOldStyle;

		try {
			locatorName = getLocatorName(locator);

			// check element is editable or not
			boolean isEditable = isElementEditable(locator);
			if (isEditable) {

				// get WebElement of locator
				WebElement element = driver.findElement(locator);

				elementOldStyle = highlightElement(element);

				// Create an object of JavaScriptExector Class
				executor = (JavascriptExecutor) driver;

				// clear value
				executor.executeScript("arguments[0].setAttribute('value', '')", element);

				// set value
				executor.executeScript("arguments[0].setAttribute('value', '" + value + "')", element);

				// get the entered value
				String getText = getElementAttribute(locator, "value", true);
				if (getText.equalsIgnoreCase(value)) {
					RESULT.PASS(value + " is entered in '" + locatorName + "' Text Field", false,
							ScreenshotType.browser);
				} else {
					RESULT.FAIL(getText + " is entered in '" + locatorName + "' Text Field instead of " + value, true,
							ScreenshotType.browser);
				}

				unHighlightElement(element, elementOldStyle);
			}
		} catch (NoSuchElementException e) {
			RESULT.FAIL("Element not found - " + locatorName, true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error(
					"Exception occurred in Java setting value : '" + value + "'  in element : '" + locatorName + "'",
					e);
			RESULT.FAIL("Error occurred in Java setting value : '" + value + "'  in element : '" + locatorName + "'",
					true, ScreenshotType.browser);
		}

	}// end of javaScriptSetValue

	/**
	 * 
	 * This will change the given date to the new format provided.
	 * 
	 * @param dateToFormat
	 * @param oldFormat
	 * @param newFormat
	 * @return
	 */

	public String changeDateFormat(String dateToFormat, String oldFormat, String newFormat) {

		// String containing given date
		String convertedDate = null;
		try {

			// create simpledateformat object
			SimpleDateFormat dateFormat = new SimpleDateFormat(oldFormat);

			// parse the date into date object
			Date date = dateFormat.parse(dateToFormat);

			// new date format
			SimpleDateFormat dateformat = new SimpleDateFormat(newFormat);

			// format the date with new date format
			convertedDate = dateformat.format(date);

		} catch (Exception e) {
			BaseSuite.log.error("Exception while changing date format", e);
			RESULT.WARNING("Parse Exception: ", false, ScreenshotType.browser);
		}
		return convertedDate;
	}

	/**
	 * Get difference between two dates in seconds
	 * 
	 * @param inputDateFormat format of the input date
	 * @param firstDate       value of the first date in String format
	 * @param secondDate      value of the second date in String format
	 * @return difference between given dates in seconds
	 * 
	 */
	public long compareDate(String inputDateFormat, String firstDate, String secondDate) {
		long diffInSeconds = 0;
		SimpleDateFormat formattor = new SimpleDateFormat(inputDateFormat);

		try {
			if (firstDate.isEmpty() || secondDate.isEmpty()) {
				RESULT.WARNING("Invalid input given for comparing dates", false, ScreenshotType.browser);
			} else {
				// Get difference of two dates in seconds
				diffInSeconds = (formattor.parse(firstDate).getTime() - formattor.parse(secondDate).getTime()) / 1000;
			}
		} catch (java.text.ParseException pe) {
			RESULT.FAIL(pe.getMessage(), true, ScreenshotType.browser);
		}

		return diffInSeconds;
	}

	/**
	 * This is to get random no between the range specified.
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public int getRandomNumber(int min, int max) {
		try {
			Random rand = new Random();

			// nextInt is normally exclusive of the top value,
			// so add 1 to make it inclusive
			int randomNum = rand.nextInt((max - min) + 1) + min;

			// show in logs
			BaseSuite.log.debug("Random number generated - " + randomNum);

			return randomNum;
		} catch (Exception e) {
			BaseSuite.log.error("Exception occured while generating random number", e);
			RESULT.FAIL("Error generating random number in range - " + min + " to " + max, false,
					ScreenshotType.browser);
			return 0;
		}
	}// end of get random int

	/**
	 * Get all the option values in drop-down
	 * 
	 * @param dropDownLocator Locator of drop-down
	 * 
	 * @return ArrayList All the options of drop-down as List
	 * 
	 * 
	 */
	public ArrayList<String> getDropDownOptions(WebElement dropDownElement) {
		// variable to hold drop-down element
		String elementName = getLocatorName(getByLocator(dropDownElement));

		Select dropDownSelect;
		String elementOldStyle;

		// List to store all options of drop-down
		ArrayList<String> allOptionList = new ArrayList<String>();

		try {
			elementOldStyle = highlightElement(dropDownElement);

			dropDownSelect = new Select(dropDownElement);

			// Retrieve all options web elements as List
			List<WebElement> dropDownOptions = dropDownSelect.getOptions();

			// checking if list contains drop-down options web elements
			if (dropDownOptions != null) {

				// store each drop-down option in list
				for (WebElement webElement : dropDownOptions) {

					try {
						// Extracting the label texts for each option
						allOptionList.add(webElement.getText());
					} catch (Exception e) {
						allOptionList.add("");
					}
				}

			} else {

				RESULT.FAIL("No option is available in drop-down - '" + elementName + "'", true,
						ScreenshotType.browser);
			}

			unHighlightElement(dropDownElement, elementOldStyle);
		} catch (NoSuchElementException e) {

			RESULT.FAIL("Drop down element not found - '" + elementName + "'", true, ScreenshotType.browser);
		}

		return allOptionList;

	}// end of getDropDownOptions

	/**
	 * Get dynamic string by formatting wild card in string
	 * 
	 * @param originalString Original string must contain N number of wild cards as
	 *                       '%s'
	 * 
	 * @param strReplace     N number of wild cards to be replaced with specified N
	 *                       no of arguments as string
	 * 
	 * @return String - replaced String
	 * 
	 * 
	 * 
	 */
	public String getString(String originalString, String... strReplace) {

		// variable to hold count of wild card %s in locator string
		int wildCardCount;

		String replacedString = "";

		// get count of string arguments
		int countOfStringArguments = strReplace.length;

		try {

			// get no of wild card '%s' in locator string
			wildCardCount = (originalString.length() - originalString.replace("%s", "").length()) / 2;

			// check for wild cards count
			if (wildCardCount == countOfStringArguments) {

				// replace wild cards '%s' with string[]
				replacedString = String.format(originalString, (Object[]) strReplace);

			} else {

				RESULT.FAIL("No of wild cards and No of strings passed in argument is not matched", false,
						ScreenshotType.browser);

			}
		} catch (Exception e) {
			BaseSuite.log.error("Exception occurred while getting String", e);
			RESULT.FAIL("Error occurred while getting String", false, ScreenshotType.browser);
		}

		// return dynamic replaced string
		return replacedString;
	}

	/**
	 * Switch to parent frame from frame window
	 * 
	 * 
	 */
	public void switchToParentFrame() {
		try {
			// switching to parent frame
			driver.switchTo().parentFrame();

			RESULT.PASS("Switched to parent frame", false, ScreenshotType.browser);

		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in switching to parent frame", e);
			RESULT.FAIL("Error occurred in switching to parent frame", true, ScreenshotType.browser);
		}
	}

	/**
	 * 
	 * To get the value for a given data key from .properties
	 * 
	 * @param propertyName data to be read from .properties file
	 * 
	 * @return String the value of a given data
	 * 
	 */
	public String getProperty(String filePath, String propertyName) {
		String value = null;
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(filePath);

			// load a properties file
			prop.load(input);

			value = prop.getProperty(propertyName).trim();
			RESULT.INFO("Value is - " + value, false, ScreenshotType.browser);

		} catch (Exception e) {
			BaseSuite.log.error("Exception encountered while for input + " + propertyName, e);
			RESULT.FAIL("Error encountered while for input + " + propertyName, true, ScreenshotType.browser);
		}

		return value;
	}

	/**
	 * Upload file
	 * 
	 * @param id       id of the file element
	 * 
	 * @param filePath filePath to enter
	 * 
	 */
	public void uploadFile(String id, String filePath) {

		// Get locator
		By locator = By.id(id);

		// set the Element Name
		String elementName = getLocatorName(locator);

		// check element is editable or not
		if (isElementEditable(locator)) {
			try {
				// get the webElement of locator
				WebElement element = driver.findElement(locator);
				String oldStyle = highlightElement(element);

				// clear the text field
				element.clear();

				// Enter keys in the text field
				element.sendKeys(filePath);

				// Get the entered value and get only filename from the path
				// C:\fakepath\fileName
				String fileName = getElementAttribute(locator, "value").replaceAll(".*\\\\", "");

				if (filePath.contains(fileName)) {
					RESULT.PASS(filePath + " is entered in '" + elementName + "' file upload field", false,
							ScreenshotType.browser);
				} else {
					RESULT.FAIL(fileName + " is entered in '" + elementName + "' file upload field " + filePath, false,
							ScreenshotType.browser);
				}

				unHighlightElement(element, oldStyle);
			} catch (NoSuchElementException e) {
				RESULT.FAIL(("Element not found - '" + elementName + "'"), true, ScreenshotType.browser);
			} catch (WebDriverException e) {
				checkInterrupted(e, executionInterrupted, ResultType.FAIL);
				BaseSuite.log.error("Exception occurred in entering " + filePath + " in '" + elementName, e);
				RESULT.FAIL("Error occurred in entering " + filePath + " in '" + elementName, true,
						ScreenshotType.browser);
			}
		}

	}// end of uploadFile

	/**
	 * To verify the displayed value of given input locator with expected
	 * 
	 * @param locator
	 * 
	 * @param expectedValue
	 * 
	 * @return correctValueAppears
	 */
	public boolean verifyInputValue(By locator, String expectedValue) {
		boolean correctValueAppears = true;
		String locatorName = getLocatorName(locator);
		// get input value
		String displayedValue = getElementAttribute(locator, "value");
		// check for given value
		if (displayedValue.contains(expectedValue))
			RESULT.PASS(locatorName + " displayed is as expected - " + expectedValue, false, ScreenshotType.browser);
		else {
			RESULT.FAIL(locatorName + " displayed is - " + displayedValue + " expected is - " + expectedValue, true,
					ScreenshotType.browser);
			correctValueAppears = false;
		}
		return correctValueAppears;
	}

	/**
	 * To verify the selected value of given dropdown locator with expected
	 * 
	 * @param locator
	 * 
	 * @param expectedValue
	 * 
	 * @return correctValueAppears
	 */
	public boolean verifyDropdownValue(By locator, String expectedValue) {
		boolean correctValueAppears = true;
		String locatorName = getLocatorName(locator);
		// get input value
		String displayedText = getVisibleTextOfSelectedOption(getWebElement(locator));
		String displayedValue = getValueOfSelectedOption(locator);
		// check for given value
		if (displayedValue.contains(expectedValue) || displayedText.contains(expectedValue))
			RESULT.PASS(locatorName + " selected is as expected - " + expectedValue, false, ScreenshotType.browser);
		else {
			RESULT.FAIL(
					locatorName + " selected has value - '" + displayedValue + "' and text - '" + displayedText
							+ "' against expected value or text - '" + expectedValue + "'",
					true, ScreenshotType.browser);
			correctValueAppears = false;
		}
		return correctValueAppears;
	}

	/**
	 * URL to be launched in new window
	 * 
	 * @param urlToNavigate
	 * 
	 * @return new window handle string
	 */
	public String openNewTabAndSwitch(String urlToNavigate) {

		// get all the window handles
		List<String> handlesBefore = getWindowHandles();

		JavascriptExecutor jse = (JavascriptExecutor) driver;
		String jseStatement = "javascript:void(window.open('" + urlToNavigate + "','_blank'))";
		jse.executeScript(jseStatement);

		RESULT.PASS("Opened new tab", true, ScreenshotType.browser);

		// get all the winow after click
		List<String> handlesAfter = getWindowHandles();

		handlesAfter.removeAll(handlesBefore);

		String newWinHandle = handlesAfter.stream().findFirst().get();
		RESULT.INFO("New window handle is - " + newWinHandle, false, ScreenshotType.browser);

		driver.switchTo().window(newWinHandle);

		waitForPageLoad();

		return driver.getWindowHandle();

	}// end of openNewTabAndSwitch

	/**
	 * 
	 * Trims and removes special characters from both ends of string
	 * 
	 * @param oldString
	 * @return trimmed String
	 */
	public String trimSpecialCharacters(String oldString) {

		// if string is blank
		if (oldString.length() == 0) {
			return oldString;
		} else {

			String newString = oldString.trim();

			int start = 0, end = newString.length() - 1;

			// starting from the end the word..until the first alphanumeric char is hit
			for (int j = newString.length() - 1; j > 0; j--) {
				if (Character.isDigit(newString.charAt(j)) | Character.isLetter(newString.charAt(j))) {
					end = j;
					break;
				}
			}

			// starting from the index 0... until the first alphanumeric char is hit
			for (int i = 0; i < newString.length(); i++) {
				if (Character.isDigit(newString.charAt(i)) | Character.isLetter(newString.charAt(i))) {
					start = i;
					break;
				}
			}

			// returning the substring with no special characters
			return newString.substring(start, end + 1);
		}
	}

	/**
	 * Get the past or future date
	 * 
	 * @param days       days to add or substract from todays date
	 * 
	 * @param dateformat format of the date
	 * 
	 * @return newDate
	 */
	public String getPastFutureDate(int days, String... dateformat) {

		SimpleDateFormat sdf;

		// date format
		if (dateformat.length > 0) {
			sdf = new SimpleDateFormat(dateformat[0]);
		} else {
			sdf = new SimpleDateFormat("MM/dd/yyyy");
		}

		// get calendar instance
		Calendar cal = Calendar.getInstance();

		// add or subtract number of days from todays date
		cal.add(Calendar.DATE, days);

		// get date in above format
		String newDate = sdf.format(cal.getTime());

		return newDate;
	}

	/**
	 * This will close all windows excepts the window handle passed and switch to
	 * the new window.
	 * 
	 * @param windowId window handle which is to be kept open
	 */
	public void closeAllwindowExcept(String windowId) {
		try {
			// loop through all the handle and close all ignoring given one
			for (String window : driver.getWindowHandles()) {
				if (!window.equals(windowId)) {

					driver.switchTo().window(window);

					driver.close();
					RESULT.INFO("Window closed with id - " + window, false, ScreenshotType.browser);
				}
			}

			// switch to given handle
			switchToWindowUsingHandle(windowId);
			RESULT.PASS("Switched to given window", true, ScreenshotType.browser);

		} catch (Exception e) {
			BaseSuite.log.error("Exception on closing all window expect - " + windowId, e);
			RESULT.INFO("Exception in closing all windows", true, ScreenshotType.browser);
		}
	}

	/**
	 * Switch to a window using zero based index
	 * 
	 */
	public boolean switchToWindow(int index) {
		// get all the window handles
		Set<String> availableWindows = driver.getWindowHandles();
		int currentWindowIndex = 0;
		// iterate through each to check if the given window is available or not
		for (String windowId : availableWindows) {
			if (currentWindowIndex == index) {
				driver.switchTo().window(windowId);
				RESULT.PASS("Switch to given window with index - " + index, false, ScreenshotType.browser);
				return true;
			} else {
				if (currentWindowIndex == (availableWindows.size() - 1) || index < availableWindows.size()) {
					RESULT.FAIL("Given window is not available with index - " + index, false, ScreenshotType.browser);
					return false;
				}
			}
			currentWindowIndex++;
		}
		return false;

	}// end of switch to window

	/**
	 * Verify if given container is scrollable or not
	 * 
	 * @param container i.e body
	 * @return
	 */
	public boolean isWindowScrollable(By container) {

		boolean isScrollable = false;
		try {
			String elementScrollableJS = "return arguments[0].scrollHeight > arguments[0].offsetHeight;";

			JavascriptExecutor jse = (JavascriptExecutor) driver;

			WebElement content = getWebElement(container);

			String elementOldStyle = highlightElement(content, true);

			isScrollable = (Boolean) jse.executeScript(elementScrollableJS, content);

			unHighlightElement(content, elementOldStyle);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Failed to check for window scrollable", e);
			RESULT.FAIL("Failed to check for window scrollable", true, ScreenshotType.fullScreen);
		}
		return isScrollable;
	}

	/**
	 * Enter value in Text Field using Actions class
	 * 
	 * @param locator locator of Text Field
	 * 
	 * @param value   value that needs to be entered in Text Field
	 * 
	 * 
	 */
	public void setValueAction(By locator, String value) {
		// set the Element Name
		String locatorName = getLocatorName(locator);

		// check element is editable or not
		boolean isEditable = isElementEditable(locator);
		if (isEditable) {

			// get the webElement of locator
			WebElement element = getWebElement(locator);

			String elementOldStyle = highlightElement(element);
			try {
				// clear the text field
				element.clear();

				// enter keys in the text field using actions
				actions.sendKeys(element, value).build().perform();

				// get the entered value
				String getText = getElementAttribute(locator, "value");
				if (getText.equalsIgnoreCase(value)) {
					RESULT.PASS(value + " is entered in '" + locatorName + "' Text Field", false,
							ScreenshotType.browser);
				} else {
					RESULT.FAIL(getText + " is entered in '" + locatorName + "' Text Field instead of " + value, true,
							ScreenshotType.browser);
				}
			} catch (WebDriverException e) {
				checkInterrupted(e, executionInterrupted, ResultType.FAIL);
				BaseSuite.log.error("Exception occurred in entering " + value + " in '" + locatorName, e);
				RESULT.FAIL("Error occurred in entering " + value + " in '" + locatorName, true,
						ScreenshotType.browser);
			}

			unHighlightElement(element, elementOldStyle);
		}
	}// end of setValueAction

	/**
	 * click on given locator using Actions API
	 * 
	 * @param locator Locator of webElement
	 * 
	 */
	public void clickAction(By locator) {
		// set the Element Name
		String locatorName = null;
		try {
			locatorName = getLocatorName(locator);

			// get the webElement of locator
			WebElement element = driver.findElement(locator);

			// highlight & un-highlight quickly as it might navigate to other page
			String elementOldStyle = highlightElement(element);
			unHighlightElement(element, elementOldStyle);

			try {
				// click on webElement
				actions.click(element).build().perform();

				// wait for page to load
				RESULT.PASS(locatorName + " is clicked", false, ScreenshotType.browser);
			} catch (WebDriverException e) {
				checkInterrupted(e, executionInterrupted, ResultType.FAIL);
				BaseSuite.log.error("Exception occurred in action clicking '" + locatorName + "'", e);
				RESULT.FAIL("Error occurred in clicking '" + locatorName + "'", true, ScreenshotType.browser);
			}

		} catch (NoSuchElementException e) {
			RESULT.FAIL(("Element not found - '" + locatorName + "'"), true, ScreenshotType.browser);
		}

	}// end of click

	/**
	 * Get value of selected drop-down option
	 * 
	 * @param selectLocator locator of select
	 * @return value of selected option else empty string
	 */
	public String getValueOfSelectedOption(By selectLocator) {

		String selectedValue = "";

		if (isElementExists(selectLocator)) {

			try {
				WebElement dropDownElement = getWebElement(selectLocator);
				String oldStyle = highlightElement(dropDownElement);

				// create select element and get value of selected option
				Select dropDown = new Select(dropDownElement);
				String value = dropDown.getFirstSelectedOption().getAttribute("value");
				selectedValue = (value == null) ? "" : value;

				unHighlightElement(dropDownElement, oldStyle);
			} catch (UnexpectedTagNameException e) {
				BaseSuite.log.error("Given element is not Select/Dropdown", e);
				RESULT.FAIL("Given element is not Select/Dropdown", true, ScreenshotType.browser);
			}

		} else {
			RESULT.FAIL("Given element not found", true, ScreenshotType.browser);
		}

		return selectedValue;
	}// end of getValueOfSelectedOption

	/**
	 * Hard refresh the current page
	 */
	public void hardRefreshPage() {
		Action hardRefresh;
		try {
			// build the action
			hardRefresh = actions.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "R")).build();

			// perform action
			hardRefresh.perform();
			waitForPageLoad();

			RESULT.INFO("Successfuly hard refreshed the page", false, ScreenshotType.browser);
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred in hard refreshing the page", e);
			RESULT.FAIL("Error occurred in hard refreshing the page", true, ScreenshotType.browser);
		}

	}// end of hardRefreshPage

	/**
	 * Verify window is scrollable of not
	 * 
	 * @return boolean
	 */
	public boolean isWindowScrollable() {
		boolean isScrollable = false;
		try {
			String elementScrollableJS = "return document.documentElement.scrollHeight>document.documentElement.clientHeight;";

			JavascriptExecutor jse = (JavascriptExecutor) driver;

			isScrollable = (Boolean) jse.executeScript(elementScrollableJS);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Failed to check for window scrollable", e);
			RESULT.FAIL("Failed to check for window scrollable", true, ScreenshotType.fullScreen);
		}
		return isScrollable;
	}

	/**
	 * To get the inner HTML of given locator - can be used to get the text with
	 * proper contain multiple spaces in statement
	 * 
	 * @param elementLocator locator of web element
	 * 
	 * @return String inner html of web element
	 * 
	 */
	public String jsGetInnerHTML(By elementLocator) {
		// variable to hold element name
		String elementName = null, elementOldStyle;

		// variable to hold text of web element
		String innerHTML = "";

		try {
			// set element name
			elementName = getLocatorName(elementLocator);

			// check if element available
			if (isElementExists(elementLocator)) {
				WebElement element = driver.findElement(elementLocator);

				elementOldStyle = highlightElement(element);

				// Create an object of JavaScriptExector Class
				JavascriptExecutor executor = (JavascriptExecutor) driver;

				// clear value
				innerHTML = String.valueOf(executor.executeScript("return arguments[0].innerHTML", element));

				RESULT.INFO("Inner HTML of " + elementName + " element is - " + innerHTML, false,
						ScreenshotType.browser);

				unHighlightElement(element, elementOldStyle);
			} else {
				RESULT.FAIL("Element not found -'" + elementName + "' for getting inner html", true,
						ScreenshotType.browser);
			}
		} catch (TimeoutException e) {
			BaseSuite.log.error("Not able to get text using inner html for element'" + elementName + "'", e);
			RESULT.FAIL("Not able to get text for element'" + elementName + "'", true, ScreenshotType.browser);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Not able to get text using inner html for element'" + elementName + "'", e);
			RESULT.FAIL("Not able to get text for element'" + elementName + "'", true, ScreenshotType.browser);
		}
		return innerHTML;
	}// end of jsGetInnerHTML

	private String executionInterrupted = "Execution interrupted due to timeout";

	/**
	 * To check if exception contain 'interrupted' then exit test. Introduced this
	 * due to 'timeOut'.
	 * 
	 * @param Exception object
	 * 
	 * @param String
	 */
	private void checkInterrupted(Exception e, String exitTestMsg, ResultType resultType) {
		String excMsg = e.getMessage();
		BaseSuite.log.error("exitTestMsg " + excMsg);
		if (excMsg.length() > 0 && excMsg.contains("interrupted"))
			exitTest(exitTestMsg, false, resultType);
	}

	/**
	 * This highlights the element on the screen.
	 * 
	 * @param element
	 * 
	 * @return Before update style to revert back the same String
	 */
	public String highlightElement(WebElement element, boolean... isFrame) {
		String currentStyle = "";

		// do only when execution is running in UI mode
		if (!headlessExecution) {
			try {
				JavascriptExecutor js = (JavascriptExecutor) driver;

				// store the currentStyle to revert back after operation
				currentStyle = element.getAttribute("style");

				// check if frame to avoid background highlighting
				String highlightCSS = isFrame.length > 0 && isFrame[0] ? "border: 2px solid red;"
						: "background: yellow; border: 2px solid red;";

				// if there old style then add it
				String updatedStyle = currentStyle.trim().length() > 0 ? currentStyle + highlightCSS : highlightCSS;

				// update the element style attribute using js
				js.executeScript("arguments[0].setAttribute('style', '" + updatedStyle + "');", element);
			} catch (JavascriptException e) {
				BaseSuite.log.error("Exception occured while highlighing element " + element, e);
			} catch (WebDriverException e) {
				BaseSuite.log.error("Exception occured while highlighing element " + element, e);
			}
		}

		return currentStyle;
	}

	/**
	 * This reverts highlighting done from the element on the screen.
	 * 
	 * @param element
	 * 
	 * @param elementOldStyle element old style before highlighting
	 * 
	 * @return Before update style to revert back the same
	 */
	public void unHighlightElement(WebElement element, String elementOldStyle) {
		// do only when execution is running in UI mode
		if (!headlessExecution) {
			try {
				if (!isAlertPresent(1) && element.isDisplayed()) {
					JavascriptExecutor js = (JavascriptExecutor) driver;

					// update the element style attribute using js
					if (elementOldStyle.trim().isEmpty())
						// if highlighted added style attribute then remove it
						js.executeScript("arguments[0].removeAttribute('style')", element);
					else // else add the previous style value
						js.executeScript("arguments[0].setAttribute('style', '" + elementOldStyle + "');", element);
				}
			} catch (JavascriptException e) {
				BaseSuite.log.error("Exception occured while reverting highlighing element " + element, e);
			} catch (StaleElementReferenceException e) {
				String elementName = getLocatorName(getByLocator(element));
				BaseSuite.log.info(elementName + " is not available after click to unhighlight.");
			} catch (NoSuchWindowException e) {
				String elementName = getLocatorName(getByLocator(element));
				BaseSuite.log
						.info(elementName + " after performing action not available to unhighlight as window close");
			} catch (WebDriverException e) {
				BaseSuite.log.error("Exception occured while reverting highlighing element " + element, e);
			}
		}
	}

	/**
	 * This highlights the element associated with the locator on the screen.
	 * 
	 * @param locator
	 * 
	 * @return element style String
	 */
	public String highlightLocator(By locator) {
		String elementStyle = Strings.EMPTY;
		try {
			// get the element from given locator and update style attribute of it using js
			WebElement element = getWebElement(locator);

			elementStyle = highlightElement(element);
		} catch (JavascriptException e) {
			BaseSuite.log.error("Exception occured while highlighing element " + locator, e);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception occured while highlighing element " + locator, e);
		}

		return elementStyle;
	}

	/**
	 * This reverts highlighting done from the element on the screen.
	 * 
	 * @param locator         By
	 * 
	 * @param locatorOldStyle String - element old style before highlighting
	 */
	public void unHighlighLocator(By locator, String locatorOldStyle) {
		try {
			// get the element from given locator and update style attribute of it using js
			WebElement element = getWebElement(locator);

			unHighlightElement(element, locatorOldStyle);
		} catch (JavascriptException e) {
			BaseSuite.log.error("Exception occured while unhighlighing locator " + locator, e);
		} catch (WebDriverException e) {
			BaseSuite.log.error("Exception occured while unhighlighing locator " + locator, e);
		}
	}

	/**
	 * Get dynamic locator by formatting wild card in locator string
	 * 
	 * @param elementLocator Locator must contain N no wild cards '%s'
	 *                       elementLocator as By
	 * 
	 * @param strReplace     N no of wild cards to be replaced with specified N no
	 *                       of arguments as string
	 * 
	 * @return By return locator as By
	 * 
	 * 
	 */
	public By getLocator(By elementLocator, String... strReplace) {
		// variable to hold count of wild card %s in locator string
		int wildCardCount;

		By updatedElementLocator = elementLocator;

		try {
			// get count of string arguments
			int countOfStringArguments = strReplace.length;

			// get element name
			String elementName = getLocatorName(elementLocator);

			// get string value of element locator containing two wild cards %s
			String strElementLocator = String.valueOf(elementLocator);

			// replace all '%s' with \"%s\" to induce uniformity
			strElementLocator = strElementLocator.replace("'%s'", "\"%s\"");

			// get no of wild card '%s' in locator string
			wildCardCount = (strElementLocator.length() - strElementLocator.replace("%s", "").length()) / 2;

			// check for two wild cards
			if (wildCardCount == countOfStringArguments) {

				for (int i = 0; i < wildCardCount; i++) {
					String replacement = "";

					boolean containsDoubleQuotes = strReplace[i].contains("\"");
					boolean containsSingleQuotes = strReplace[i].contains("'");

					if (containsDoubleQuotes && !containsSingleQuotes) { // wrap replacement text by '
						replacement = "'" + strReplace[i] + "'";
					} else if (!(containsSingleQuotes && containsDoubleQuotes)) { // wrap replacement text by "
						replacement = "\"" + strReplace[i] + "\"";
					} else {
						RESULT.FAIL(
								"The replacement string contains both \' and \". Please try using translate in locator to replace \".",
								false, ScreenshotType.fullScreen);
					}

					// get the index of %s
					int indexOfWildcard = strElementLocator.indexOf("%s");
					// check if it's with quotes
					boolean withQuotes = (strElementLocator.charAt(indexOfWildcard) - 1 == '\"') ? true : false;

					if (withQuotes)
						strElementLocator = strElementLocator.replaceFirst("\"%s\"", replacement);
					else
						strElementLocator = Pattern.compile("%s").matcher(strElementLocator)
								.replaceFirst(replacement.replaceAll("\"", "").replaceAll("\\$", "\\\\\\$"));
				}

				// convert locator string to By type
				updatedElementLocator = locatorParser(strElementLocator);

				// store element name
				// if locator has %s and not variable name found then
				if (elementLocator != null && elementName.trim().length() == 0
						&& elementLocator.toString().contains("%s")) {
					elementName = "<p style='font-weight: bold; color: black !important;'>"
							+ updatedElementLocator.toString() + "</p>";
				} else {
					elementName += " with ";
					for (int i = 0; i < countOfStringArguments; i++) {
						elementName = elementName + strReplace[i];
						if (!(i == countOfStringArguments - 1)) {
							elementName = elementName + ",";
						}
					}
				}

				// store locator with it's name in dynamicVariableNames
				try {
					dynamicVariableNames.put(updatedElementLocator, elementName);
				} catch (Exception e) {
					BaseSuite.log.error("Exception occurred while store variable name in dynamicVariableNames", e);
				}

			} else {

				RESULT.FAIL("No of wild cards and No of strings passed in argument is not matched", true,
						ScreenshotType.browser);

				elementLocator = null;
			}
		} catch (WebDriverException e) {
			checkInterrupted(e, executionInterrupted, ResultType.FAIL);
			BaseSuite.log.error("Exception occurred while getting dynamic locator", e);
			RESULT.FAIL("Error occurred while getting dynamic locator", true, ScreenshotType.browser);
		}
		// return dynamic element locator
		return updatedElementLocator;
	}

	/**
	 * This function find given locator's value in given class and if value found
	 * then return variable name
	 * 
	 * @param className in String type from pages package (e.g.
	 *                  pages.login.Login_OR)
	 * @param locator   for find variable name
	 * @return if variable name found then return that value else return ""
	 */
	private String getElementNameFromClass(String className, By locator) {
		String variableName = "";
		try {
			// get all fields from class
			Field[] fields = Class.forName(className).getFields();

			// try to get variable names which value is same as given
			String[] variableNames = Arrays.stream(fields).parallel().filter(n -> n.getType().equals(By.class))
					.filter(n -> {
						try {
							return n.get(By.class).equals(locator);
						} catch (Exception e) {
							return false;
						}
					}).map(n -> n.getName()).toArray(String[]::new);

			// if variable name found then return variable name
			if (variableNames.length > 0) {
				variableName = variableNames[0];
			}

			// if variable name not found then try to get variable name for By array
			else {
				variableNames = Arrays.stream(fields).parallel().filter(n -> n.getType().equals(By[].class))
						.filter(n -> n.getType().getComponentType().equals(By.class)).filter(n -> {
							try {
								return Arrays.stream((By[]) n.get(By[].class)).parallel()
										.anyMatch(by -> by.equals(locator));
							} catch (Exception e) {
								return false;
							}
						}).map(n -> n.getName()).toArray(String[]::new);

				// if variable name found for By array then return variable name
				if (variableNames.length > 0) {
					variableName = variableNames[0];
				}
			}
		} catch (Exception e) {
			BaseSuite.log.error("Exception occurred while getting variable name with class", e);
		}

		return variableName;
	}

	/**
	 * This function find given locator's value and if value found then return
	 * variable name
	 * 
	 * @param locator
	 * @return if variable name found then return that value else return ""
	 */
	public String getLocatorName(By locator) {
		String variableName = "";
		try {

			// first try to find variable name from first calling class
			// get the pages folder
			String pagesPath = "pages";
			String pagesPathGiven = Configuration.getProperty("pagesPath");
			if (pagesPathGiven != null && !pagesPathGiven.trim().isEmpty())
				pagesPath = pagesPathGiven;

			// for consistent path
			if (pagesPath.contains("\\"))
				pagesPath = pagesPath.replace("\\", File.separator);

			final String pagesFolder = pagesPath;

			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

			Object[] mm = Arrays.stream(stackTrace).map(n -> n.getClassName()).toArray();

			String classNames[] = Arrays.stream(stackTrace).map(n -> n.getClassName())
					.filter(n -> n.startsWith(pagesFolder + ".")).toArray(String[]::new);
			// String className = classNames[classNames.length - 1];
			// variableName = getElementNameFromClass(className, locator);

			// if variable name not found then try to find variable name from
			// dynamicVariableNames map
			if (variableName.equals("")) {
				variableName = dynamicVariableNames.get(locator);
				variableName = (variableName == null) ? "" : variableName;
			}

			// if variable name not found then try to find variable name from all the OR
			// files
			if (variableName.equals("")) {
				String[] variableNames = orFiles.stream().parallel().map(n -> getElementNameFromClass(n, locator))
						.filter(n -> !n.equals("")).toArray(String[]::new);

				// if variable name found then save variable name
				if (variableNames.length > 0) {
					variableName = variableNames[0];
				}
			}

			// if locator has '%s' then not attaching the link && dynamic locator coming
			// from outside .java i.e. sheet having p with styling
			if (locator != null && !locator.toString().contains("%s")
					&& !variableName.contains("font-weight: bold; color: black !important")) {
				variableName = (variableName.equals("")) ? locator.toString() : variableName;
				variableName = "<a href='javascript:void(0);' style='font-weight: bold; color: black !important;' onclick=\"alert('"
						+ locator.toString().replaceAll("(?<!\\\\)['\"]", "\\\\'") + "')\">" + variableName + "</a>";
			}

		} catch (Exception e) {
			BaseSuite.log.error("Exception occurred while getting variable name", e);
		}

		return variableName;
	}

	/**
	 * This function stores all the OR files name
	 */
	public static void storeORFiles() {
		try {

			orFiles = Files.walk(Paths.get("./web/pages/")).filter(Files::isRegularFile)
					.filter(path -> path.toString().endsWith("_OR.java"))
					.map(path -> path.toString().substring(6).replace(".java", "").replace(File.separator, "."))
					.collect(Collectors.toList());
		} catch (Exception e) {
			BaseSuite.log.error("Exception occurred while storing OR files names", e);
			orFiles = new ArrayList<String>();
		}
	}
}