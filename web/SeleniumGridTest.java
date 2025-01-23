import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SeleniumGridTest {

	private WebDriver driver;

	@BeforeClass
	public void setUp() throws Exception {
		FirefoxOptions options = new FirefoxOptions(); // Use FirefoxOptions for Firefox browser

		// Set up RemoteWebDriver to point to the Selenium Grid Hub
		driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), options);
	}

	@Test
	public void testGoogleSearch() throws InterruptedException {
		driver.get("https://www.google.com");

		Thread.sleep(40000);
		System.out.println("Title: " + driver.getTitle());
	}

	@AfterClass
	public void tearDown() {
		if (driver != null) {
			driver.quit();
		}
	}
}
