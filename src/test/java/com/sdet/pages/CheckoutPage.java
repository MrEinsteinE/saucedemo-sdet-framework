package com.sdet.pages;

import java.time.Duration;
import org.openqa.selenium.By;
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
     * Fills the step-1 shipping form using plain sendKeys via fresh
     * driver.findElement() lookups (bypasses any stale PageFactory proxy).
     *
     * No JavaScript injection — the nativeInputValueSetter approach
     * corrupts React's internal form state when called with an empty string,
     * causing the Continue button to silently no-op.
     */
    public CheckoutPage fillShippingInfo(String firstName, String lastName, String zip) {
        // Wait for the form to be ready
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfElementLocated(By.id("first-name")));

        WebElement fn = driver.findElement(By.id("first-name"));
        fn.clear();
        fn.sendKeys(firstName);

        WebElement ln = driver.findElement(By.id("last-name"));
        ln.clear();
        ln.sendKeys(lastName);

        WebElement pc = driver.findElement(By.id("postal-code"));
        pc.clear();
        pc.sendKeys(zip);

        return this;
    }

    /**
     * Clicks Continue and blocks until one of two outcomes is observable:
     *
     *  (a) URL advances to checkout-step-two.html  — happy path
     *  (b) The error element appears in the DOM     — validation failure
     *
     * Implementation uses a raw lambda (not ExpectedConditions.or) with the
     * driver implicit-wait set to zero during polling.  This is necessary
     * because:
     *
     *  • ExpectedConditions.visibilityOfElementLocated throws
     *    NoSuchElementException when the element is absent; FluentWait
     *    suppresses that and retries — but with implicit-wait > 0 each
     *    driver.findElement() call already blocks for up to 10 s before
     *    throwing, making the or() condition take 10 s per poll rather than
     *    500 ms.
     *
     *  • driver.findElements() (plural) never throws; it returns an empty
     *    list immediately when no elements match.  Setting implicit-wait = 0
     *    makes it truly instantaneous so the polling loop runs every 500 ms
     *    as intended.
     */
    public CheckoutPage clickContinue() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Fresh By-locator click avoids stale PageFactory proxy
        wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_LOCATOR)).click();

        // Temporarily disable implicit wait so findElements() is non-blocking
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            wait.until(d -> {
                // Condition (a): navigation succeeded
                if (d.getCurrentUrl().contains("checkout-step-two.html")) {
                    return true;
                }
                // Condition (b): validation error is present in DOM
                // findElements() returns [] immediately when nothing matches
                return !d.findElements(ERROR_LOCATOR).isEmpty();
            });
        } finally {
            // Always restore the suite-wide implicit wait
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

    /**
     * Checks error visibility without implicit-wait interference.
     * findElements() with implicit=0 returns [] immediately if absent.
     */
    public boolean isErrorDisplayed() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            return !driver.findElements(ERROR_LOCATOR).isEmpty()
                    && driver.findElement(ERROR_LOCATOR).isDisplayed();
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