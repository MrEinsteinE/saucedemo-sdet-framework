package com.sdet.pages;

import com.sdet.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

/**
 * BasePage — parent class for all Page Objects.
 *
 * Encapsulates shared page interactions and forces all page objects
 * to receive a driver via constructor injection (POM best practice).
 */
public abstract class BasePage {

    protected WebDriver driver;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    protected WebElement waitAndFind(By locator) {
        return WaitUtils.waitForVisible(driver, locator);
    }

    protected WebElement waitAndClick(By locator) {
        WebElement el = WaitUtils.waitForClickable(driver, locator);
        el.click();
        return el;
    }

    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
