package com.sdet.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CartPage — Page Object for the shopping cart (/cart.html).
 */
public class CartPage extends BasePage {

    @FindBy(className = "inventory_item_name")
    private List<WebElement> cartItemNames;

    @FindBy(id = "checkout")
    private WebElement checkoutButton;

    @FindBy(id = "continue-shopping")
    private WebElement continueShoppingButton;

    private static final By CART_ITEM      = By.className("cart_item");
    private static final By REMOVE_BUTTONS = By.cssSelector("[data-test^='remove']");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public CheckoutPage proceedToCheckout() {
        WebElement btn = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id("checkout")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        return new CheckoutPage(driver);
    }

    public InventoryPage continueShopping() {
        continueShoppingButton.click();
        return new InventoryPage(driver);
    }

    /**
     * Clicks the Remove button for the first cart item and waits until that
     * item disappears from the DOM.
     *
     * IMPORTANT: the driver-level implicit wait must be set to 0 before using
     * numberOfElementsToBe(locator, 0).  If implicit wait is > 0, WebDriver
     * waits up to that timeout for *any* element to appear before returning an
     * empty list, so the explicit-wait condition can never observe "0 elements"
     * and always times out.
     */
    public CartPage removeFirstItem() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Snapshot count before removal
        int countBefore = driver.findElements(CART_ITEM).size();

        // Click the first remove button
        List<WebElement> removeButtons = wait.until(
                ExpectedConditions.numberOfElementsToBeMoreThan(REMOVE_BUTTONS, 0));
        removeButtons.get(0).click();

        // Disable implicit wait so that "no cart_item elements" is detected
        // immediately by the explicit wait, rather than blocking per-poll.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            wait.until(ExpectedConditions.numberOfElementsToBe(CART_ITEM, countBefore - 1));
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }

        return this;
    }

    public boolean isOnCartPage() {
        return driver.getCurrentUrl().contains("cart.html");
    }

    /**
     * Always queries live DOM — never uses the cached @FindBy proxy list —
     * so the count is accurate immediately after a remove action.
     */
    public int getCartItemCount() {
        return driver.findElements(CART_ITEM).size();
    }

    public List<String> getCartItemNames() {
        return cartItemNames.stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public boolean isCartEmpty() {
        return driver.findElements(CART_ITEM).isEmpty();
    }
}