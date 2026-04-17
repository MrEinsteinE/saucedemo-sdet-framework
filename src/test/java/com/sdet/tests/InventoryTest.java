package com.sdet.tests;

import com.sdet.pages.InventoryPage;
import com.sdet.pages.LoginPage;
import com.sdet.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * InventoryTest — validates product listing page behaviour.
 *
 * Covers: item count, add-to-cart badge, sort (A→Z, Z→A, price low→high, high→low).
 */
public class InventoryTest extends BaseTest {

    private InventoryPage inventoryPage;

    @BeforeMethod(alwaysRun = true)
    public void loginFirst() {
        // Log in before each test using standard_user
        inventoryPage = new LoginPage(getDriver()).login("standard_user", "secret_sauce");
    }

    // ── TC-101: Inventory page loads ──────────────────────────────

    @Test(priority = 1, groups = {"smoke", "regression"},
          description = "TC-101: Inventory page should load with 6 products")
    public void testInventoryPageLoads() {
        createTest("TC-101: Inventory Loads", "Verify 6 products are displayed on inventory page");

        Assert.assertTrue(inventoryPage.isOnInventoryPage(), "Should be on /inventory.html");
        Assert.assertEquals(inventoryPage.getInventoryItemCount(), 6,
                "SauceDemo should display exactly 6 products");
        Assert.assertEquals(inventoryPage.getPageHeaderText(), "Products");
    }

    // ── TC-102: Add single item to cart ───────────────────────────

    @Test(priority = 2, groups = {"smoke", "regression"},
          description = "TC-102: Adding one item should show badge count of 1")
    public void testAddSingleItemToCart() {
        createTest("TC-102: Add 1 Item to Cart", "Verify cart badge increments to 1");

        inventoryPage.addItemsToCart(1);

        Assert.assertTrue(inventoryPage.isCartBadgeDisplayed(), "Cart badge should be visible");
        Assert.assertEquals(inventoryPage.getCartCount(), 1, "Cart count should be 1");
    }

    // ── TC-103: Add multiple items to cart ────────────────────────

    @Test(priority = 3, groups = {"regression"},
          description = "TC-103: Adding 3 items should show badge count of 3")
    public void testAddMultipleItemsToCart() {
        createTest("TC-103: Add 3 Items to Cart", "Verify cart badge increments to 3");

        inventoryPage.addItemsToCart(3);

        Assert.assertEquals(inventoryPage.getCartCount(), 3, "Cart count should be 3");
    }

    // ── TC-104: Sort A→Z ──────────────────────────────────────────

    @Test(priority = 4, groups = {"regression"},
          description = "TC-104: Sort Name (A to Z) should reorder items alphabetically")
    public void testSortNameAZ() {
        createTest("TC-104: Sort A→Z", "Verify alphabetical ascending sort");

        inventoryPage.sortBy("Name (A to Z)");

        Assert.assertTrue(inventoryPage.isNameSortedAZ(),
                "Items should be sorted A→Z. Actual order: " + inventoryPage.getItemNames());
    }

    // ── TC-105: Sort Z→A ──────────────────────────────────────────

    @Test(priority = 5, groups = {"regression"},
          description = "TC-105: Sort Name (Z to A) should reverse alphabetical order")
    public void testSortNameZA() {
        createTest("TC-105: Sort Z→A", "Verify alphabetical descending sort");

        inventoryPage.sortBy("Name (Z to A)");

        // Z→A is just the reverse of A→Z
        java.util.List<String> names = inventoryPage.getItemNames();
        java.util.List<String> sorted = new java.util.ArrayList<>(names);
        sorted.sort(java.util.Comparator.reverseOrder());
        Assert.assertEquals(names, sorted, "Items should be sorted Z→A");
    }

    // ── TC-106: Sort Price Low→High ───────────────────────────────

    @Test(priority = 6, groups = {"regression"},
          description = "TC-106: Sort Price (low to high) should order by ascending price")
    public void testSortPriceLowToHigh() {
        createTest("TC-106: Sort Price Low→High", "Verify price ascending sort");

        inventoryPage.sortBy("Price (low to high)");

        Assert.assertTrue(inventoryPage.isPriceSortedLowHigh(),
                "Prices should be sorted low→high. Actual: " + inventoryPage.getItemPrices());
    }

    // ── TC-107: Sort Price High→Low ───────────────────────────────

    @Test(priority = 7, groups = {"regression"},
          description = "TC-107: Sort Price (high to low) should order by descending price")
    public void testSortPriceHighToLow() {
        createTest("TC-107: Sort Price High→Low", "Verify price descending sort");

        inventoryPage.sortBy("Price (high to low)");

        java.util.List<Double> prices = inventoryPage.getItemPrices();
        java.util.List<Double> sorted = new java.util.ArrayList<>(prices);
        sorted.sort(java.util.Comparator.reverseOrder());
        Assert.assertEquals(prices, sorted, "Prices should be sorted high→low");
    }

    // ── TC-108: Navigate to cart ──────────────────────────────────

    @Test(priority = 8, groups = {"smoke", "regression"},
          description = "TC-108: Clicking cart icon should navigate to cart page")
    public void testNavigateToCart() {
        createTest("TC-108: Navigate to Cart", "Verify cart icon click navigates to /cart.html");

        com.sdet.pages.CartPage cartPage = inventoryPage.goToCart();

        Assert.assertTrue(cartPage.isOnCartPage(), "Should be on /cart.html");
    }
}
