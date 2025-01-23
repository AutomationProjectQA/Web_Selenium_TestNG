package pages;

import org.openqa.selenium.By;

public interface Shared_OR {

	By temperatureLabel = By.id("temperature");

	By buyMoisturizersButton = By.xpath("//button[text()='Buy moisturizers']");
	By buySunscreensButton = By.xpath("//button[text()='Buy sunscreens']");

	By moisturizerLabel = By.xpath("//h2[text()='Moisturizers']");
	By sunscreenLabel = By.xpath("//h2[text()='Sunscreens']");
	By checkOutLabel = By.xpath("//h2[text()='Checkout']");

	By pricesElement = By.xpath("(//p[contains(text(),'%s')]/following-sibling::p)%s");
	By productNamelabel = By
			.xpath("//p[contains(text(),'Price:') and contains(text(),'%s')]/preceding::p[contains(text(),'%s')][1]");
	By addProductButton = By.xpath("//p[contains(text(),'%s')]/following-sibling::button");

	By cartButton = By.xpath("//button[contains(text(),'Cart')]");

	By addedProdect = By.xpath("//table/tbody/tr/td[text()='%s']/following-sibling::td[text()='%s']");
	By totalPriceLabel = By.id("total");
	By payWithCardButton = By.xpath("//span[text()='Pay with Card']/parent::button");

	By paymentForm = By.xpath("//form[contains(@class,'checkoutView')]");
	By emailInput = By.id("email");
	By cardNumberInput = By.id("card_number");
	By cardExperiyInpuyt = By.id("cc-exp");
	By cvcInput = By.id("cc-csc");
	By submitButton = By.id("submitButton");

	By successLabel = By.xpath("//h2[text()='PAYMENT SUCCESS']");
}
