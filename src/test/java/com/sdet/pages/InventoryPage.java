package com.sdet.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * InventoryPage — Page Object for the product listing page (/inventory.html).
 */
public class InventoryPage extends BasePage {

    // ── Locators ──────────────────────────────────────────────────
    @FindBy(className = "title")
    private WebElement pageTitle;

    @FindBy(className = "inventory_item")
    private List<WebElement> inventoryItems;

    @FindBy(className = "inventory_item_name")
    private List<WebElement> itemNames;

    @FindBy(className = "inventory_item_price")
    private List<WebElement> itemPrices;

    @FindBy(className = "product_sort_container")
    private WebElement sortDropdown;

    @FindBy(className = "shopping_cart_badge")
    private WebElement cartBadge;

    @FindBy(className = "shopping_cart_link")
    private WebElement cartIcon;

    @FindBy(id = "react-burger-menu-btn")
    private WebElement menuButton;

    private static final By ADD_TO_CART_BUTTONS = By.cssSelector("[data-test^='add-to-cart']");
    private static final By CART_BADGE = By.className("shopping_cart_badge");

    // ── Constructor ───────────────────────────────────────────────
    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    // ── Actions ───────────────────────────────────────────────────

    public InventoryPage sortBy(String visibleText) {
        new Select(sortDropdown).selectByVisibleText(visibleText);
        return this;
    }

    public InventoryPage addItemsToCart(int count) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    for (int i = 0; i < count; i++) {
        // 1. Find all "Add to Cart" buttons
        List<WebElement> buttons = driver.findElements(By.xpath("//button[text()='Add to cart']"));
        if (buttons.isEmpty()) break;

        WebElement target = buttons.get(0);
        
        // 2. Click using JS
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);
        
        // 3. CRITICAL: Wait for the clicked button to disappear or change text
        // This ensures the backend registered the addition
        int expectedCount = i + 1;
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
            By.className("shopping_cart_badge"), String.valueOf(expectedCount)));
    }
    return this;
}




    public InventoryPage addItemToCartByName(String productName) {
    String dataTestId = "add-to-cart-" + productName.toLowerCase().replaceAll("\\s+", "-");
    driver.findElement(By.id(dataTestId)).click(); // Most SauceDemo elements use ID for data-test
    return this;
}


    public CartPage goToCart() {
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.elementToBeClickable(cartIcon));
    
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cartIcon);
    
    // Wait for URL to actually change to cart.html
    new WebDriverWait(driver, Duration.ofSeconds(10))
        .until(ExpectedConditions.urlContains("cart.html"));
        
    return new CartPage(driver);
}


    public void openMenu() {
        menuButton.click();
    }

    // ── Assertions / Getters ──────────────────────────────────────

    public boolean isOnInventoryPage() {
        return driver.getCurrentUrl().contains("inventory.html");
    }

    public String getPageHeaderText() {
        return pageTitle.getText();
    }

    public int getInventoryItemCount() {
        return inventoryItems.size();
    }

    public List<String> getItemNames() {
        return itemNames.stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public List<Double> getItemPrices() {
        return itemPrices.stream()
                .map(el -> Double.parseDouble(el.getText().replace("$", "")))
                .collect(Collectors.toList());
    }

    public int getCartCount() {
    List<WebElement> badges = driver.findElements(CART_BADGE);
    if (badges.isEmpty()) {
        return 0;
    }
    return Integer.parseInt(badges.get(0).getText());
}


    public boolean isCartBadgeDisplayed() {
        try {
            return driver.findElement(CART_BADGE).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Verify items are sorted A→Z by name. */
    public boolean isNameSortedAZ() {
        List<String> names = getItemNames();
        for (int i = 0; i < names.size() - 1; i++) {
            if (names.get(i).compareTo(names.get(i + 1)) > 0) return false;
        }
        return true;
    }

    /** Verify items are sorted low→high price. */
    public boolean isPriceSortedLowHigh() {
        List<Double> prices = getItemPrices();
        for (int i = 0; i < prices.size() - 1; i++) {
            if (prices.get(i) > prices.get(i + 1)) return false;
        }
        return true;
    }
}
