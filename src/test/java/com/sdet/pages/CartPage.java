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

    @FindBy(className = "cart_item")
    private List<WebElement> cartItems;

    @FindBy(className = "inventory_item_name")
    private List<WebElement> cartItemNames;

    @FindBy(id = "checkout")
    private WebElement checkoutButton;

    @FindBy(id = "continue-shopping")
    private WebElement continueShoppingButton;

    private static final By REMOVE_BUTTONS = By.cssSelector("[data-test^='remove']");
    private static final By CART_ITEM      = By.className("cart_item");

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
     * Clicks the Remove button for the first cart item and waits until
     * that item is no longer present in the DOM before returning.
     * This prevents getCartItemCount() from reading a stale element list.
     */
    public CartPage removeFirstItem() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Snapshot how many items are present before removal
        List<WebElement> itemsBefore = driver.findElements(CART_ITEM);
        int countBefore = itemsBefore.size();

        List<WebElement> removeButtons = wait.until(
                ExpectedConditions.numberOfElementsToBeMoreThan(REMOVE_BUTTONS, 0));
        removeButtons.get(0).click();

        // Wait until the DOM actually reflects one fewer item
        if (countBefore > 0) {
            wait.until(ExpectedConditions.numberOfElementsToBe(CART_ITEM, countBefore - 1));
        }

        return this;
    }

    public boolean isOnCartPage() {
        return driver.getCurrentUrl().contains("cart.html");
    }

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