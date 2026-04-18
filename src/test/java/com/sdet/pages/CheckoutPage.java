package com.sdet.pages;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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
     * Key detail: after typing in the postal-code field (the last field),
     * we send Keys.TAB to move focus away.  SauceDemo's React form updates
     * its internal controlled-component state on the native 'input' event
     * (fired by sendKeys per character) but some validation/state-flush logic
     * runs on 'blur'.  Without the TAB, React may not have committed all
     * three field values by the time Continue is clicked, causing the submit
     * handler to see stale (empty) state and silently do nothing.
     */
    public CheckoutPage fillShippingInfo(String firstName, String lastName, String zip) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfElementLocated(By.id("first-name")));

        WebElement fn = driver.findElement(By.id("first-name"));
        fn.clear();
        if (firstName != null && !firstName.isEmpty()) {
            fn.sendKeys(firstName);
        }

        WebElement ln = driver.findElement(By.id("last-name"));
        ln.clear();
        if (lastName != null && !lastName.isEmpty()) {
            ln.sendKeys(lastName);
        }

        WebElement pc = driver.findElement(By.id("postal-code"));
        pc.clear();
        if (zip != null && !zip.isEmpty()) {
            pc.sendKeys(zip);
        }
        // TAB away from the last field: fires onBlur on postal-code, which
        // flushes React's controlled-input state before we hit Continue.
        // Guarded because sendKeys on an empty-value field can still need
        // the blur event, but only if the field currently has focus.
        try {
            pc.sendKeys(Keys.TAB);
        } catch (Exception ignored) {
            // non-fatal: blur will still occur on the next click
        }

        return this;
    }

    /**
     * Clicks Continue and blocks until one of two outcomes is observable:
     *
     *  (a) URL advances to checkout-step-two.html  — happy path
     *  (b) The error element appears in the DOM     — validation failure
     *
     * Button interaction strategy (defence-in-depth for headless CI):
     *   1. Try native WebElement.click() after scrolling into view.
     *   2. If no navigation / no error appears within a short grace period,
     *      fall back to JavaScript click (this proven strategy is already
     *      used successfully by CartPage.proceedToCheckout()).
     *   3. If still nothing, fall back to Actions.moveToElement().click()
     *      for good measure.
     *
     * Why this change: on GitHub Actions' ubuntu-latest + headless=new
     * Chrome, Actions.moveToElement().click() was observed to silently drop
     * the click on the Continue <input type="submit"> — neither a navigation
     * nor a validation error ever occurred, causing a 15s TimeoutException.
     * JS click bypasses the Chrome DevTools input-pipeline quirks entirely.
     *
     * Wait implementation: a raw ExpectedCondition lambda with implicit-wait
     * temporarily set to zero.  This is necessary because driver.findElements()
     * with a non-zero implicit wait blocks for up to implicit-wait ms before
     * returning an empty list, making each poll of the error-element check
     * consume the full implicit-wait budget rather than completing in
     * milliseconds.
     */
    public CheckoutPage clickContinue() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_LOCATOR));

        // Make sure the button is in viewport before clicking (headless has no
        // real scroll, but some event pipelines still care about visibility).
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", btn);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            // Attempt 1: JavaScript click — the most reliable option in
            // headless Chrome on CI. Mirrors CartPage.proceedToCheckout().
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

            if (waitForContinueOutcome(Duration.ofSeconds(5))) return this;

            // Attempt 2: native click
            try { btn.click(); } catch (Exception ignored) { /* fall through */ }
            if (waitForContinueOutcome(Duration.ofSeconds(5))) return this;

            // Attempt 3: Actions click (original strategy)
            try {
                new Actions(driver).moveToElement(btn).click().perform();
            } catch (Exception ignored) { /* fall through to final wait */ }

            // Final wait for outcome; lets the 15s budget be spent intelligently
            wait.until(d -> {
                if (d.getCurrentUrl().contains("checkout-step-two.html")) return true;
                return !d.findElements(ERROR_LOCATOR).isEmpty();
            });
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }

        return this;
    }

    /**
     * Polls for up to {@code timeout} for a "Continue" outcome (navigation
     * to step-two OR validation error). Returns true if one was observed,
     * false if the timeout elapsed with no outcome (caller will retry).
     *
     * Caller MUST set implicit wait to 0 before invoking this.
     */
    private boolean waitForContinueOutcome(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d -> {
                if (d.getCurrentUrl().contains("checkout-step-two.html")) return true;
                return !d.findElements(ERROR_LOCATOR).isEmpty();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public CheckoutPage clickFinish() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(FINISH_LOCATOR)).click();
        wait.until(ExpectedConditions.urlContains("checkout-complete.html"));
        return this;
    }

    /**
     * Clicks Cancel and waits for the browser to navigate away from the
     * current checkout page before returning.
     *
     * Without this wait, the caller (e.g. testCancelCheckout) can reach
     * isOnInventoryPage() before the navigation to /inventory.html completes,
     * causing an immediate false from getCurrentUrl() even though Cancel did
     * work correctly.
     */
    public CheckoutPage clickCancel() {
        String urlBeforeCancel = driver.getCurrentUrl();

        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.elementToBeClickable(cancelButton))
            .click();

        // Block until the URL actually changes (Cancel navigates away)
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.not(ExpectedConditions.urlToBe(urlBeforeCancel)));

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