package com.sdet.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * LoginPage — Page Object for https://www.saucedemo.com (login screen).
 *
 * All locators live here. Tests never touch By/CSS/XPath directly.
 */
public class LoginPage extends BasePage {

    // ── Locators ──────────────────────────────────────────────────
    @FindBy(id = "user-name")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(id = "login-button")
    private WebElement loginButton;

    @FindBy(css = "[data-test='error']")
    private WebElement errorMessage;

    private static final By ERROR_LOCATOR = By.cssSelector("[data-test='error']");

    // ── Constructor ───────────────────────────────────────────────
    public LoginPage(WebDriver driver) {
        super(driver);
    }

    // ── Actions ───────────────────────────────────────────────────

    public LoginPage enterUsername(String username) {
        waitAndFind(By.id("user-name")).clear();
        usernameField.sendKeys(username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        passwordField.clear();
        passwordField.sendKeys(password);
        return this;
    }

    public InventoryPage clickLoginButton() {
        loginButton.click();
        return new InventoryPage(driver);
    }

    /** Convenience method: login and return InventoryPage. */
    public InventoryPage login(String username, String password) {
        return enterUsername(username)
                .enterPassword(password)
                .clickLoginButton();
    }

    /** Click login without entering credentials — for negative tests. */
    public LoginPage submitEmptyLogin() {
        loginButton.click();
        return this;
    }

    // ── Assertions / Getters ──────────────────────────────────────

    public boolean isErrorDisplayed() {
        try {
            return driver.findElement(ERROR_LOCATOR).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getErrorMessage() {
        waitAndFind(ERROR_LOCATOR);
        return errorMessage.getText();
    }

    public boolean isLoginPageDisplayed() {
        return loginButton.isDisplayed();
    }
}
