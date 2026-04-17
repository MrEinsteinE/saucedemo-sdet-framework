package com.sdet.tests;

import com.sdet.pages.InventoryPage;
import com.sdet.pages.LoginPage;
import com.sdet.utils.BaseTest;
import com.sdet.utils.ExcelDataReader;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

/**
 * LoginTest — validates the authentication flows of SauceDemo.
 *
 * Test categories:
 *  - Positive login (valid credentials)
 *  - Negative login (invalid, locked, empty)
 *  - Data-driven login via Excel (multiple credential sets)
 */
public class LoginTest extends BaseTest {

    private static final String STANDARD_USER = "standard_user";
    private static final String PASSWORD       = "secret_sauce";
    private static final String LOCKED_USER    = "locked_out_user";
    private static final String WRONG_PASSWORD = "wrong_password";

    // ── TC-001: Valid login ───────────────────────────────────────

    @Test(priority = 1, groups = {"smoke", "regression"},
          description = "TC-001: Valid credentials should navigate to inventory page")
    public void testValidLogin() {
        createTest("TC-001: Valid Login", "Verify standard_user can log in successfully");

        LoginPage loginPage = new LoginPage(getDriver());
        InventoryPage inventoryPage = loginPage.login(STANDARD_USER, PASSWORD);

        Assert.assertTrue(inventoryPage.isOnInventoryPage(),
                "Expected to land on /inventory.html after valid login");
        Assert.assertEquals(inventoryPage.getPageHeaderText(), "Products",
                "Page header should read 'Products'");
    }

    // ── TC-002: Invalid password ──────────────────────────────────

    @Test(priority = 2, groups = {"regression"},
          description = "TC-002: Invalid password should show error message")
    public void testInvalidPassword() {
        createTest("TC-002: Invalid Password", "Verify error is shown for wrong password");

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterUsername(STANDARD_USER).enterPassword(WRONG_PASSWORD).clickLoginButton();

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error message should be visible");
        Assert.assertTrue(loginPage.getErrorMessage().contains("Username and password do not match"),
                "Error text mismatch. Actual: " + loginPage.getErrorMessage());
    }

    // ── TC-003: Locked-out user ───────────────────────────────────

    @Test(priority = 3, groups = {"regression"},
          description = "TC-003: Locked-out user should see appropriate error")
    public void testLockedOutUser() {
        createTest("TC-003: Locked-Out User", "Verify locked_out_user sees lock message");

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(LOCKED_USER, PASSWORD);

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error should be shown for locked user");
        Assert.assertTrue(loginPage.getErrorMessage().contains("locked out"),
                "Error should mention 'locked out'. Actual: " + loginPage.getErrorMessage());
    }

    // ── TC-004: Empty credentials ─────────────────────────────────

    @Test(priority = 4, groups = {"regression"},
          description = "TC-004: Submitting empty form should show required field error")
    public void testEmptyCredentials() {
        createTest("TC-004: Empty Credentials", "Verify blank submission shows error");

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.submitEmptyLogin();

        Assert.assertTrue(loginPage.isErrorDisplayed(), "Error should appear for empty submission");
        Assert.assertTrue(loginPage.getErrorMessage().contains("Username is required"),
                "Expected 'Username is required'. Actual: " + loginPage.getErrorMessage());
    }

    // ── TC-005: Empty password only ───────────────────────────────

    @Test(priority = 5, groups = {"regression"},
          description = "TC-005: Username entered but password blank should show password error")
    public void testEmptyPassword() {
        createTest("TC-005: Empty Password", "Verify missing password shows password error");

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterUsername(STANDARD_USER).submitEmptyLogin();

        Assert.assertTrue(loginPage.isErrorDisplayed());
        Assert.assertTrue(loginPage.getErrorMessage().contains("Password is required"),
                "Expected 'Password is required'. Actual: " + loginPage.getErrorMessage());
    }

    // ── TC-006: Page title check ──────────────────────────────────

    @Test(priority = 6, groups = {"smoke"},
          description = "TC-006: Login page should have correct browser title")
    public void testLoginPageTitle() {
        createTest("TC-006: Login Page Title", "Verify browser tab title");

        LoginPage loginPage = new LoginPage(getDriver());
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login button should be visible");
        Assert.assertEquals(getDriver().getTitle(), "Swag Labs",
                "Browser title should be 'Swag Labs'");
    }

    // ── TC-007: Data-driven login (Excel) ─────────────────────────

    @Test(priority = 7, groups = {"regression", "data-driven"},
          dataProvider = "loginData",
          description = "TC-007: Data-driven login test using Excel sheet")
    @SuppressWarnings("unchecked")
    public void testDataDrivenLogin(Map<String, String> row) {
        String username    = row.get("username");
        String password    = row.get("password");
        String expectation = row.get("expected_result"); // "success" or "failure"
        String errorHint   = row.getOrDefault("error_hint", "");

        createTest("TC-007 [" + username + "]", "Data-driven login: " + expectation);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterUsername(username).enterPassword(password).clickLoginButton();

        if ("success".equalsIgnoreCase(expectation)) {
            InventoryPage inventoryPage = new InventoryPage(getDriver());
            Assert.assertTrue(inventoryPage.isOnInventoryPage(),
                    "Expected successful login for user: " + username);
        } else {
            Assert.assertTrue(loginPage.isErrorDisplayed(),
                    "Expected error for user: " + username);
            if (!errorHint.isBlank()) {
                Assert.assertTrue(loginPage.getErrorMessage().contains(errorHint),
                        "Error hint mismatch. Expected to contain: " + errorHint
                        + " | Actual: " + loginPage.getErrorMessage());
            }
        }
    }

    @DataProvider(name = "loginData")
    public Object[][] loginDataProvider() throws IOException {
        return ExcelDataReader.getTableArray(
                "src/test/resources/testdata/login_data.xlsx", "LoginTests");
    }
}
