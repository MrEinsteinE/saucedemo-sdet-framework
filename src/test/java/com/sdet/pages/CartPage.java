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
     * Removes the first item from the cart and waits until it is gone from
     * the DOM before returning.
     *
     * Two problems addressed here:
     *
     * 1. The remove button click must use JavaScript execution.  In headless
     *    Chrome on CI, a plain Selenium .click() on the SauceDemo cart remove
     *    button sometimes fails silently (no exception, but no DOM change),
     *    possibly because the element is intercepted by an invisible overlay
     *    that exists briefly after page load.  A JS click bypasses the
     *    hit-testing layer and fires the React onClick handler directly.
     *
     * 2. The post-removal wait must run with implicit-wait = 0.
     *    driver.findElements() with a non-zero implicit wait blocks for up to
     *    implicit-wait milliseconds before returning an empty list, so
     *    numberOfElementsToBe(locator, 0) can never observe "zero elements"
     *    within its polling window — it always reports the stale count of 1
     *    and times out.  Setting implicit-wait to 0 makes findElements()
     *    return instantaneously with the actual current DOM count.
     */
    public CartPage removeFirstItem() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Snapshot count before removal
        int countBefore = driver.findElements(CART_ITEM).size();

        // Wait for at least one remove button, then JS-click the first one
        WebElement removeBtn = wait.until(
                ExpectedConditions.elementToBeClickable(REMOVE_BUTTONS));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", removeBtn);

        // Poll for DOM update with implicit-wait = 0 so findElements() is instant
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        try {
            wait.until(d -> d.findElements(CART_ITEM).size() < countBefore);
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }

        return this;
    }

    public boolean isOnCartPage() {
        return driver.getCurrentUrl().contains("cart.html");
    }

    /**
     * Always queries the live DOM — never the cached @FindBy proxy list —
     * so the count is correct immediately after a remove action.
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