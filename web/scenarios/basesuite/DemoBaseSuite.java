package scenarios.basesuite;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import base.BaseSuite;
import pages.CommonFunction;

public class DemoBaseSuite extends BaseSuite {

	protected CommonFunction cf;

	@BeforeSuite
	public void login() {

		// for adding the test in report
		String methodName = "login";
		setUpProjectTest(methodName);

		cf = new CommonFunction();
		cf.openPortal("https://weathershopper.pythonanywhere.com");

		// for ending the test in report
		tearDownProjectTest(methodName);

	}

	@BeforeClass
	public void creationObject() {

		cf = new CommonFunction();
	}

}
