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
     * Removes the first cart item and waits for it to leave the DOM.
     *
     * JS click: plain .click() silently no-ops in headless Chrome on CI
     * (possibly intercepted by a brief post-navigation overlay).
     *
     * Implicit-wait = 0 during the count poll: with implicit > 0,
     * driver.findElements() blocks up to implicit-wait ms before returning [],
     * so numberOfElementsToBe(locator, 0) can never observe zero within its
     * budget. Setting it to 0 makes findElements() instantaneous.
     */
    public CartPage removeFirstItem() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        int countBefore = driver.findElements(CART_ITEM).size();

        WebElement removeBtn = wait.until(
                ExpectedConditions.elementToBeClickable(REMOVE_BUTTONS));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", removeBtn);

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