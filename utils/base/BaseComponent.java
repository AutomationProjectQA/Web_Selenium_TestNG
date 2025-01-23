package base;

import org.openqa.selenium.WebDriver;
import org.sikuli.script.Screen;

import corelibrary.CommonActions;
import framework.reporter.Reporter;
import framework.setup.SetUp;

/**
 * 
 * Every component in template project have to extend this class for accessing
 * driver and result object
 *
 */
public class BaseComponent extends CommonActions {
	// object that will available to the components
	public WebDriver driver;

	public Reporter RESULT;

	public Screen screen;

	// default constructor initializing the objects
	public BaseComponent() {
		
		this.driver = SetUp.driver;

		this.RESULT = BaseSuite.RESULT;

		this.screen = BaseSuite.setUp.screen;
	}
}
