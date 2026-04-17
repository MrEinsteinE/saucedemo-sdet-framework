package com.sdet.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sdet.utils.WaitUtils;

/**
 * CheckoutPage — covers the full checkout flow:
 *   Step 1: Customer info  (/checkout-step-one.html)
 *   Step 2: Order summary  (/checkout-step-two.html)
 *   Complete: Confirmation (/checkout-complete.html)
 */
public class CheckoutPage extends BasePage {

    // ── Step 1 ────────────────────────────────────────────────────
    @FindBy(id = "first-name")
    private WebElement firstNameField;

    @FindBy(id = "last-name")
    private WebElement lastNameField;

    @FindBy(id = "postal-code")
    private WebElement postalCodeField;

    @FindBy(id = "continue")
    private WebElement continueButton;

    @FindBy(css = "[data-test='error']")
    private WebElement errorMessage;

    // ── Step 2 ────────────────────────────────────────────────────
    @FindBy(className = "summary_total_label")
    private WebElement orderTotal;

    @FindBy(className = "summary_tax_label")
    private WebElement taxLabel;

    @FindBy(id = "finish")
    private WebElement finishButton;

    @FindBy(id = "cancel")
    private WebElement cancelButton;

    // ── Confirmation ──────────────────────────────────────────────
    @FindBy(className = "complete-header")
    private WebElement confirmationHeader;

    @FindBy(className = "complete-text")
    private WebElement confirmationText;

    @FindBy(id = "back-to-products")
    private WebElement backToProductsButton;

    private static final By ERROR_LOCATOR = By.cssSelector("[data-test='error']");

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    // ── Step 1 actions ────────────────────────────────────────────

    public CheckoutPage enterFirstName(String firstName) {
        firstNameField.clear();
        firstNameField.sendKeys(firstName);
        return this;
    }

    public CheckoutPage enterLastName(String lastName) {
        lastNameField.clear();
        lastNameField.sendKeys(lastName);
        return this;
    }

    public CheckoutPage enterPostalCode(String zip) {
        postalCodeField.clear();
        postalCodeField.sendKeys(zip);
        return this;
    }

    public CheckoutPage fillShippingInfo(String firstName, String lastName, String zip) {
        return enterFirstName(firstName).enterLastName(lastName).enterPostalCode(zip);
    }

            public CheckoutPage clickContinue() {
        // Using wait from BasePage logic or manual wait
        WaitUtils.waitForClickable(driver, By.id("continue")).click();
        return this;
    }

    public CheckoutPage clickFinish() {
        WaitUtils.waitForClickable(driver, By.id("finish")).click();
        return this;
    }



    public CheckoutPage clickCancel() {
        cancelButton.click();
        return this;
    }

    // ── Assertions / Getters ──────────────────────────────────────

        public boolean isOnCheckoutStep1() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.urlContains("checkout-step-one.html"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isOnCheckoutStep2() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.urlContains("checkout-step-two.html"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isOrderConfirmed() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.urlContains("checkout-complete.html"));
        } catch (Exception e) {
            return false;
        }
    }


    public String getConfirmationHeader() {
        return confirmationHeader.getText();
    }

    public String getOrderTotal() {
        return orderTotal.getText();
    }

    public boolean isErrorDisplayed() {
        try {
            return driver.findElement(ERROR_LOCATOR).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getErrorMessage() {
        waitAndFind(ERROR_LOCATOR);
        return errorMessage.getText();
    }

    public InventoryPage backToProducts() {
        backToProductsButton.click();
        return new InventoryPage(driver);
    }
}
