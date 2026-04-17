package com.sdet.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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

    @FindBy(id = "finish")
    private WebElement finishButton;

    @FindBy(id = "cancel")
    private WebElement cancelButton;

    // ── Confirmation ──────────────────────────────────────────────
    @FindBy(className = "complete-header")
    private WebElement confirmationHeader;

    @FindBy(id = "back-to-products")
    private WebElement backToProductsButton;

    private static final By ERROR_LOCATOR    = By.cssSelector("[data-test='error']");
    private static final By CONTINUE_LOCATOR = By.id("continue");
    private static final By FINISH_LOCATOR   = By.id("finish");

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

    /**
     * Fills the step-1 shipping form.
     *
     * Uses JavaScript value injection + a synthetic 'input'/'change' event
     * after each sendKeys so that React controlled-component state is updated.
     * Plain sendKeys alone is sometimes swallowed in headless Chrome when the
     * driver's implicit-wait is non-zero, leaving fields empty when Continue
     * is clicked.
     */
    public CheckoutPage fillShippingInfo(String firstName, String lastName, String zip) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("first-name")));

        fillField(By.id("first-name"),  firstName);
        fillField(By.id("last-name"),   lastName);
        fillField(By.id("postal-code"), zip);

        return this;
    }

    /**
     * Clears a field, types into it via sendKeys, then fires JS input/change
     * events to ensure React's controlled-component state is in sync.
     */
    private void fillField(By locator, String value) {
        WebElement el = driver.findElement(locator);
        el.clear();
        el.sendKeys(value);
        ((JavascriptExecutor) driver).executeScript(
            "var el = arguments[0];" +
            "var setter = Object.getOwnPropertyDescriptor(" +
            "    window.HTMLInputElement.prototype, 'value').set;" +
            "setter.call(el, arguments[1]);" +
            "el.dispatchEvent(new Event('input',  { bubbles: true }));" +
            "el.dispatchEvent(new Event('change', { bubbles: true }));",
            el, value
        );
    }

    /**
     * Clicks Continue and waits for one of two outcomes before returning:
     *   (a) URL advances to checkout-step-two.html  — happy path
     *   (b) A validation error banner becomes visible — negative tests
     *
     * The implicit wait is set to zero during the or() poll so that
     * visibilityOfElementLocated(ERROR_LOCATOR) fails fast when the error
     * element is absent, instead of blocking for 10 s per poll cycle and
     * never allowing the URL condition to win.
     */
    public CheckoutPage clickContinue() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Fresh By-locator click (avoids stale PageFactory proxy)
        wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_LOCATOR)).click();

        // Disable implicit wait while polling for absence/presence
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("checkout-step-two.html"),
                ExpectedConditions.visibilityOfElementLocated(ERROR_LOCATOR)
            ));
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }

        return this;
    }

    public CheckoutPage clickFinish() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(FINISH_LOCATOR)).click();
        wait.until(ExpectedConditions.urlContains("checkout-complete.html"));
        return this;
    }

    public CheckoutPage clickCancel() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.elementToBeClickable(cancelButton))
            .click();
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
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            return driver.findElement(ERROR_LOCATOR).isDisplayed();
        } catch (Exception e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
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