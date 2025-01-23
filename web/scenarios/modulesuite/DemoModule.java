package scenarios.modulesuite;

import org.testng.annotations.Test;

import framework.reporter.ScreenshotType;
import pages.Shared_OR;
import scenarios.basesuite.DemoBaseSuite;

public class DemoModule extends DemoBaseSuite {

	protected String navigatedPage = "";

	@Test(priority = 0)
	public void navigateToProduct() {

		navigatedPage = cf.navigateToProductPage(0);

		if (!navigatedPage.isEmpty()) {
			RESULT.PASS("Successfully navigate to product Page", false, ScreenshotType.browser);

		} else {
			RESULT.FAIL("Failed to navigate to product page", true, ScreenshotType.browser);
		}

	}

	@Test(priority = 1, dependsOnMethods = "navigateToProduct")
	public void addProductInCard() {

		String[] firstProduct = null, secondProduct = null;

		if (navigatedPage.equals("Moisturizers")) {
			firstProduct = cf.addCheapestProduct("Almond");
			secondProduct = cf.addCheapestProduct("Aloe");
		} else if (navigatedPage.equals("Sunscreens")) {
			firstProduct = cf.addCheapestProduct("SPF-30");
			secondProduct = cf.addCheapestProduct("SPF-50");
		}

		if (cf.clickAndVerify(Shared_OR.cartButton, Shared_OR.checkOutLabel)) {

			RESULT.PASS("Successfully navigate to checkout Page", false, ScreenshotType.browser);

			boolean checkFirst = cf.isElementDisplayed(cf.getLocator(Shared_OR.addedProdect, firstProduct));
			boolean checkSecond = cf.isElementDisplayed(cf.getLocator(Shared_OR.addedProdect, secondProduct));

			if (checkFirst && checkSecond) {
				RESULT.PASS("Successfully added both prodcut", false, ScreenshotType.browser);

				String totalPriceValue = cf.getTextWebelement(Shared_OR.totalPriceLabel);

				int total = Integer.parseInt(firstProduct[1]) + Integer.parseInt(secondProduct[1]);

				if (!totalPriceValue.isEmpty() && totalPriceValue.contains(String.valueOf(total)))
					RESULT.PASS("Total price of product is verified successfully", true, ScreenshotType.browser);
				else
					RESULT.FAIL("Total price of product is mismatch", true, ScreenshotType.browser);

			} else
				RESULT.FAIL("Failed to add prodcut", true, ScreenshotType.browser);

		} else {
			RESULT.FAIL("Failed to navigate to checkout page", true, ScreenshotType.browser);

		}
	}

	@Test(priority = 2, dependsOnMethods = "addProductInCard")
	public void payWithCard() {

		cf.payBillWithCard();
	}

}
