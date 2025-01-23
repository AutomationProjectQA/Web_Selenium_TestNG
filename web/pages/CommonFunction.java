package pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import base.BaseComponent;
import framework.input.Configuration;
import framework.reporter.ScreenshotType;

public class CommonFunction extends BaseComponent implements Shared_OR {

	public static int explicitLowWait = Integer.parseInt(Configuration.getProperty("explicitLowWait"));
	public static int explicitHighWait = Integer.parseInt(Configuration.getProperty("explicitHighWait"));

	/**
	 * Open Web Portal
	 * 
	 * @param url
	 */
	public void openPortal(String url) {

		driver.navigate().to(url);

		if (waitForElement(temperatureLabel, explicitLowWait, WaitType.visibilityOfElementLocated, true))
			RESULT.PASS("Web portal open successfully", true, ScreenshotType.browser);
		else
			exitApplication("Failed to open web portal", true);
	}

	public boolean clickAndVerify(By clickLocator, By verifyLocator) {

		boolean verifyElement = false;

		if (isElementDisplayed(clickLocator, true)) {

			click(clickLocator);

			if (waitForElement(verifyLocator, explicitHighWait, WaitType.visibilityOfElementLocated, true)) {

				RESULT.PASS("Successfully verify locator - " + getLocatorName(verifyLocator), false,
						ScreenshotType.browser);
				verifyElement = true;
			}
		}
		return verifyElement;

	}

	public String navigateToProductPage(int retryCount) {

		if (retryCount > 5)
			return "";

		String navigatedPage = "";

		if (waitForElement(temperatureLabel, explicitHighWait, WaitType.visibilityOfElementLocated, true)) {

			RESULT.PASS("Temperature label is visible", false, ScreenshotType.browser);

			String temperature = getTextWebelement(temperatureLabel).split(" ")[0];

			int temp = Integer.parseInt(temperature);

			if (temp < 19) {

				navigatedPage = clickAndVerify(buyMoisturizersButton, moisturizerLabel) ? "Moisturizers"
						: navigatedPage;

			} else if (temp > 34) {

				navigatedPage = clickAndVerify(buySunscreensButton, sunscreenLabel) ? "Sunscreens" : navigatedPage;

			} else {
				refreshPage();
				navigateToProductPage(++retryCount);
			}

		} else {
			exitApplication("Failed to find temperature label", true);
		}

		return navigatedPage;
	}

	public String[] addCheapestProduct(String productContains) {

		String[] productDetails = new String[2];

		List<WebElement> priceElement = getList(getLocator(pricesElement, productContains, ""));
		int lowestValue = Integer.MAX_VALUE;
		String price;

		if (!priceElement.isEmpty()) {

			for (int i = 1; i <= priceElement.size(); i++) {
				String priceString = getTextWebelement(getLocator(pricesElement, productContains, "[" + i + "]"));

				if (!priceString.isEmpty()) {

					price = priceString.contains("Rs.") ? priceString.split("Rs. ")[1].trim()
							: priceString.split("Price: ")[1].trim();
					lowestValue = Math.min(lowestValue, Integer.parseInt(price));
				} else {
					RESULT.FAIL("Error occuring while getting price from element", true, ScreenshotType.browser);
				}
			}

			productDetails[0] = getTextWebelement(
					getLocator(productNamelabel, String.valueOf(lowestValue), productContains));
			productDetails[1] = String.valueOf(lowestValue);

			click(getLocator(addProductButton, productDetails[0]));

		}

		return productDetails;
	}

	public void payBillWithCard() {

		if (isElementDisplayed(payWithCardButton)) {

			click(payWithCardButton);

			switchToFrame("stripe_checkout_app");

			waitForElement(paymentForm, explicitLowWait, WaitType.visibilityOfElementLocated, true);

			setValue(emailInput, "fderfef@ffe.co");
			javaScriptSetValue(cardNumberInput, "5425233430109903");
			javaScriptSetValue(cardExperiyInpuyt, "12/24");
			setValue(cvcInput, "212");

			click(submitButton);

			switchToDefaultContent();

			if (waitForElement(successLabel, explicitHighWait, WaitType.visibilityOfElementLocated, true)) {
				RESULT.PASS("Successfully payment done", true, ScreenshotType.browser);
			} else {
				RESULT.FAIL("Error occuring while doing payment", true, ScreenshotType.browser);

			}

		} else {
		}
	}

}
