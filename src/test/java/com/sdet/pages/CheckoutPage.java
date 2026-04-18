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

    /**
     * React-safe value setter. SauceDemo's checkout form is a React
     * controlled component — setting input.value via Selenium/JS alone
     * does NOT notify React (React tracks its own internal value cache).
     *
     * The standard workaround is to invoke the HTMLInputElement prototype's
     * native `value` setter, then dispatch a bubbling 'input' event. React's
     * onChange sees this as a legitimate user edit and updates its state.
     *
     * Refs:
     *   https://github.com/facebook/react/issues/10135
     *   https://stackoverflow.com/q/23892547  (how React patches the setter)
     */
    private static final String REACT_SET_VALUE_JS =
        "const el = arguments[0];" +
        "const value = arguments[1];" +
        "const proto = Object.getPrototypeOf(el);" +
        "const valueSetter = Object.getOwnPropertyDescriptor(proto, 'value').set;" +
        "valueSetter.call(el, value);" +
        "el.dispatchEvent(new Event('input',  { bubbles: true }));" +
        "el.dispatchEvent(new Event('change', { bubbles: true }));" +
        "el.dispatchEvent(new Event('blur',   { bubbles: true }));";

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    // ── Step 1 actions ────────────────────────────────────────────

    public CheckoutPage enterFirstName(String firstName) {
        setReactInputValue(By.id("first-name"), firstName);
        return this;
    }

    public CheckoutPage enterLastName(String lastName) {
        setReactInputValue(By.id("last-name"), lastName);
        return this;
    }

    public CheckoutPage enterPostalCode(String zip) {
        setReactInputValue(By.id("postal-code"), zip);
        return this;
    }

    /**
     * Fills the step-1 shipping form using a defence-in-depth strategy that
     * is proven reliable in headless Chrome on GitHub Actions runners.
     *
     * Why the belt-and-braces approach? SauceDemo uses React controlled
     * inputs.  Selenium's sendKeys() fires native `input` events, which
     * React normally sees — BUT in headless Chrome (especially on CI) the
     * ordering of native events vs. React's reconciliation is occasionally
     * racy, and the Continue-button click can fire before React has
     * committed the last character to state.  The symptom is a spurious
     * "First Name is required" error (or silent no-op) when fields visibly
     * contain the right values.
     *
     * Strategy:
     *   1. sendKeys() — user-like typing; fires all native keyboard events.
     *   2. React-native value setter + synthetic 'input'/'change'/'blur'
     *      events — guarantees React's controlled-input state is in sync
     *      with the DOM value, even if (1) had a timing glitch.
     *   3. Keys.TAB away from the last field as a final belt — fires blur
     *      via the real input pipeline.
     *
     * This combination is idempotent: if sendKeys already worked, the
     * React setter re-setting the same value is a no-op.
     */
    public CheckoutPage fillShippingInfo(String firstName, String lastName, String zip) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.visibilityOfElementLocated(By.id("first-name")));

        setReactInputValue(By.id("first-name"),  firstName);
        setReactInputValue(By.id("last-name"),   lastName);
        setReactInputValue(By.id("postal-code"), zip);

        // Final TAB away from postal-code to guarantee blur is fired via
        // the real event pipeline as well (in case synthetic blur above is
        // ignored by any downstream listener).
        try {
            WebElement pc = driver.findElement(By.id("postal-code"));
            pc.sendKeys(Keys.TAB);
        } catch (Exception ignored) {
            // non-fatal — synthetic blur was already dispatched
        }

        return this;
    }

    /**
     * Core helper: types a value into a field reliably on any browser
     * (headed, headless, CI). Combines sendKeys with React's native
     * value setter to handle controlled-input quirks.
     */
    private void setReactInputValue(By locator, String value) {
        WebElement el = driver.findElement(locator);

        // Step 1: user-like interaction — click to focus + clear existing.
        try {
            el.click();
        } catch (Exception ignored) { /* some headless quirks; continue */ }
        try {
            el.clear();
        } catch (Exception ignored) { /* ignore */ }

        // Guard: sendKeys("") is a no-op; for empty/null values we still
        // want to fire React's events so validation triggers as expected.
        if (value != null && !value.isEmpty()) {
            el.sendKeys(value);
        }

        // Step 2: React-safe value sync — forces React's state to match DOM.
        try {
            ((JavascriptExecutor) driver).executeScript(
                REACT_SET_VALUE_JS, el, value == null ? "" : value);
        } catch (Exception ignored) {
            // If JS fails, sendKeys above is still in place — not fatal.
        }
    }

    /**
     * Clicks Continue and blocks until one of two outcomes is observable:
     *
     *  (a) URL advances to checkout-step-two.html  — happy path
     *  (b) The error element appears in the DOM     — validation failure
     *
     * Button interaction strategy (defence-in-depth for headless CI):
     *   1. Native WebElement.click() after scrolling into view — this is
     *      the most React-friendly option because it fires the full event
     *      sequence (pointerdown, mousedown, focus, pointerup, mouseup,
     *      click) that React's synthetic event system expects.
     *   2. If no outcome within a short grace period, fall back to
     *      JavaScript click (bypasses any pointer-event interception).
     *   3. As a last resort, submit the enclosing <form> element directly.
     *
     * Wait implementation: raw ExpectedCondition lambda with implicit-wait
     * temporarily set to zero, so findElements() returns instantly.
     */
    public CheckoutPage clickContinue() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(CONTINUE_LOCATOR));

        // Scroll into view — headless Chrome sometimes cares about viewport
        // visibility for pointer-event dispatch.
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", btn);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            // Attempt 1: native click — fires the full event sequence React expects.
            try {
                btn.click();
            } catch (Exception ignored) { /* fall through */ }
            if (waitForContinueOutcome(Duration.ofSeconds(5))) return this;

            // Attempt 2: JavaScript click — bypasses any event-interception layer.
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            } catch (Exception ignored) { /* fall through */ }
            if (waitForContinueOutcome(Duration.ofSeconds(5))) return this;

            // Attempt 3: Actions click — mimics human mouse movement.
            try {
                new Actions(driver).moveToElement(btn).click().perform();
            } catch (Exception ignored) { /* fall through */ }
            if (waitForContinueOutcome(Duration.ofSeconds(3))) return this;

            // Attempt 4: submit the enclosing form directly as last resort.
            try {
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].form && arguments[0].form.requestSubmit " +
                    "? arguments[0].form.requestSubmit() " +
                    ": (arguments[0].form && arguments[0].form.submit());",
                    btn);
            } catch (Exception ignored) { /* fall through to final wait */ }

            // Final wait — let whatever the above actions triggered resolve.
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(FINISH_LOCATOR));

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", btn);

        // Native click first, then JS fallback — mirrors clickContinue() pattern
        try {
            btn.click();
        } catch (Exception ignored) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            } catch (Exception alsoIgnored) { /* fall through to wait */ }
        }

        // Give navigation a chance; if still not on complete page, JS-click retry
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("checkout-complete.html"));
        } catch (Exception first) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
            } catch (Exception ignored) { /* ignore */ }
            wait.until(ExpectedConditions.urlContains("checkout-complete.html"));
        }
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

        // Use the By locator for a fresh element reference, then JS-click it.
        // In headless Chrome, plain WebElement.click() on the Cancel button
        // silently no-ops without throwing (the click is intercepted or lost),
        // so a try/catch fallback never fires. JS click bypasses hit-testing
        // and fires the React onClick handler directly.
        WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(10))
            .until(ExpectedConditions.elementToBeClickable(By.id("cancel")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

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
