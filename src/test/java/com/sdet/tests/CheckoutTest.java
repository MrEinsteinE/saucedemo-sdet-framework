package com.sdet.tests;

import com.sdet.pages.CartPage;
import com.sdet.pages.CheckoutPage;
import com.sdet.pages.InventoryPage;
import com.sdet.pages.LoginPage;
import com.sdet.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * CheckoutTest — end-to-end tests for the cart and checkout flow.
 *
 * Flow: Login → Add items → Cart → Checkout Step 1 → Step 2 → Order Complete
 */
public class CheckoutTest extends BaseTest {

    private InventoryPage inventoryPage;

    @BeforeMethod(alwaysRun = true)
    public void loginFirst() {
        inventoryPage = new LoginPage(getDriver()).login("standard_user", "secret_sauce");
    }

    // ── TC-201: Full happy-path checkout ─────────────────────────

    @Test(priority = 1, groups = {"smoke", "regression", "e2e"},
          description = "TC-201: Full checkout flow should complete with order confirmation")
    public void testFullCheckoutFlow() {
        createTest("TC-201: Full E2E Checkout", "Add item → cart → checkout → confirm order");

        // Add a product and go to cart
        inventoryPage.addItemsToCart(1);
        CartPage cartPage = inventoryPage.goToCart();

        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have 1 item");

        // Proceed to checkout step 1
        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        Assert.assertTrue(checkoutPage.isOnCheckoutStep1(), "Should be on checkout step 1");

        // Fill shipping info and continue
        checkoutPage.fillShippingInfo("Einstein", "Ellandala", "500001");
        checkoutPage.clickContinue();
        Assert.assertTrue(checkoutPage.isOnCheckoutStep2(), "Should have moved to step 2");

        // Verify order total label is present
        Assert.assertFalse(checkoutPage.getOrderTotal().isBlank(), "Order total should not be blank");

        // Finish order
        checkoutPage.clickFinish();
        Assert.assertTrue(checkoutPage.isOrderConfirmed(), "Should be on checkout-complete page");
        Assert.assertEquals(checkoutPage.getConfirmationHeader(), "Thank you for your order!");
    }

    // ── TC-202: Checkout with missing first name ──────────────────

    @Test(priority = 2, groups = {"regression"},
          description = "TC-202: Missing first name on checkout should show error")
    public void testCheckoutMissingFirstName() {
        createTest("TC-202: Missing First Name", "Verify checkout error when first name is empty");

        inventoryPage.addItemsToCart(1);
        CartPage cartPage = inventoryPage.goToCart();
        CheckoutPage checkoutPage = cartPage.proceedToCheckout();

        // Only fill last name and postal — leave first name empty
        checkoutPage.fillShippingInfo("", "Ellandala", "500001");
        checkoutPage.clickContinue();

        Assert.assertTrue(checkoutPage.isErrorDisplayed(), "Error should appear for missing first name");
        Assert.assertTrue(checkoutPage.getErrorMessage().contains("First Name is required"),
                "Error should mention 'First Name is required'. Actual: " + checkoutPage.getErrorMessage());
    }

    // ── TC-203: Checkout with empty cart ─────────────────────────

    @Test(priority = 3, groups = {"regression"},
          description = "TC-203: Proceeding to checkout with empty cart should reach checkout page")
    public void testCheckoutEmptyCart() {
        createTest("TC-203: Empty Cart Checkout", "Verify checkout can be initiated even with empty cart");

        CartPage cartPage = inventoryPage.goToCart();
        Assert.assertTrue(cartPage.isCartEmpty(), "Cart should be empty");

        // SauceDemo allows navigating checkout with empty cart — verify we reach step 1
        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        Assert.assertTrue(checkoutPage.isOnCheckoutStep1(),
                "Should still navigate to checkout step 1 even with empty cart");
    }

    // ── TC-204: Cancel checkout returns to inventory ──────────────

    @Test(priority = 4, groups = {"regression"},
          description = "TC-204: Cancelling on step 2 should return user to inventory")
    public void testCancelCheckout() {
        createTest("TC-204: Cancel Checkout", "Verify cancel on step 2 navigates back to inventory");

        inventoryPage.addItemsToCart(1);
        CartPage cartPage = inventoryPage.goToCart();
        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        checkoutPage.fillShippingInfo("Einstein", "Ellandala", "500001").clickContinue();

        Assert.assertTrue(checkoutPage.isOnCheckoutStep2());

        // Cancel — should return to inventory
        checkoutPage.clickCancel();
        InventoryPage backToInventory = new InventoryPage(getDriver());
        Assert.assertTrue(backToInventory.isOnInventoryPage(),
                "After cancelling on step 2, user should be on /inventory.html");
    }

    // ── TC-205: Remove item from cart ────────────────────────────

    @Test(priority = 5, groups = {"regression"},
          description = "TC-205: Removing an item from cart should reduce item count to 0")
    public void testRemoveItemFromCart() {
        createTest("TC-205: Remove Cart Item", "Verify item removal empties the cart");

        inventoryPage.addItemsToCart(1);
        CartPage cartPage = inventoryPage.goToCart();

        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should start with 1 item");

        cartPage.removeFirstItem();

        Assert.assertEquals(cartPage.getCartItemCount(), 0, "Cart should be empty after removal");
        Assert.assertTrue(cartPage.isCartEmpty(), "isCartEmpty() should return true");
    }

    // ── TC-206: Multi-item checkout ───────────────────────────────

    @Test(priority = 6, groups = {"regression", "e2e"},
          description = "TC-206: Checkout with multiple items should succeed")
    public void testMultiItemCheckout() {
        createTest("TC-206: Multi-Item Checkout", "Add 3 items and complete full checkout");

        inventoryPage.addItemsToCart(3);
        Assert.assertEquals(inventoryPage.getCartCount(), 3, "Cart should show 3");

        CartPage cartPage = inventoryPage.goToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 3, "Cart page should list 3 items");

        CheckoutPage checkoutPage = cartPage.proceedToCheckout();
        checkoutPage.fillShippingInfo("Einstein", "Ellandala", "500001").clickContinue();
        checkoutPage.clickFinish();

        Assert.assertTrue(checkoutPage.isOrderConfirmed(), "Order should be confirmed");
    }
}
