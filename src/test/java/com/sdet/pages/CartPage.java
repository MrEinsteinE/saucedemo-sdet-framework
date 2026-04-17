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

    public CartPage removeFirstItem() {
        // Wait for at least one remove button to appear
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        List<WebElement> removeButtons = wait.until(d -> d.findElements(REMOVE_BUTTONS));
        
        if (!removeButtons.isEmpty()) {
            removeButtons.get(0).click();
        }
        return this;
    }

    public boolean isOnCartPage() {
        return driver.getCurrentUrl().contains("cart.html");
    }

    public int getCartItemCount() {
        return cartItems.size();
    }

    public List<String> getCartItemNames() {
        return cartItemNames.stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public boolean isCartEmpty() {
        return cartItems.isEmpty();
    }
}
