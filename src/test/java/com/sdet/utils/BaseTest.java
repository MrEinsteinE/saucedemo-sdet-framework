package com.sdet.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BaseTest — parent class for all test classes.
 *
 * Responsibilities:
 *  - Spin up / tear down ChromeDriver (headless in CI, headed locally)
 *  - Wire ExtentReports for HTML reporting
 *  - Auto-capture screenshots on test failure
 *  - Expose shared utilities (driver, logger, config)
 */
public class BaseTest {

    // ThreadLocal ensures parallel test runs each get their own driver
    protected static ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    protected static ThreadLocal<ExtentTest> extentTestThreadLocal = new ThreadLocal<>();

    private static ExtentReports extent;
    private static final String REPORT_DIR = "test-output/reports/";
    private static final String SCREENSHOT_DIR = "test-output/screenshots/";
    private static final String BASE_URL = "https://www.saucedemo.com";

    // ─────────────────────────────────────────────────────────────
    //  Suite-level setup/teardown (runs once per suite)
    // ─────────────────────────────────────────────────────────────

    @BeforeSuite(alwaysRun = true)
    public void initReport() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportPath = REPORT_DIR + "TestReport_" + timestamp + ".html";

        new File(REPORT_DIR).mkdirs();
        new File(SCREENSHOT_DIR).mkdirs();

        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setTheme(Theme.DARK);
        spark.config().setDocumentTitle("SauceDemo SDET Framework — Test Report");
        spark.config().setReportName("Automation Test Results");
        spark.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Author", "Einstein Ellandala");
        extent.setSystemInfo("Framework", "Java + Selenium 4 + TestNG");
        extent.setSystemInfo("App Under Test", "SauceDemo (https://www.saucedemo.com)");
        extent.setSystemInfo("Pattern", "Page Object Model (POM)");

        System.out.println("[BaseTest] ExtentReports initialized → " + reportPath);
    }

    @AfterSuite(alwaysRun = true)
    public void flushReport() {
        if (extent != null) {
            extent.flush();
            System.out.println("[BaseTest] Report flushed to disk.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Method-level setup/teardown (runs per test method)
    // ─────────────────────────────────────────────────────────────

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser"})
    public void setUp(@Optional("chrome") String browser) {
        WebDriver driver = createDriver(browser);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().window().maximize();
        driver.get(BASE_URL);
        driverThreadLocal.set(driver);
        System.out.println("[BaseTest] Browser launched → " + BASE_URL);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        WebDriver driver = getDriver();
        ExtentTest test = extentTestThreadLocal.get();

        if (result.getStatus() == ITestResult.FAILURE) {
            // Capture screenshot on failure
            String screenshotPath = captureScreenshot(result.getName());
            if (test != null && screenshotPath != null) {
                test.fail("Test FAILED: " + result.getThrowable().getMessage());
                test.addScreenCaptureFromPath(screenshotPath, "Failure Screenshot");
            }
        } else if (result.getStatus() == ITestResult.SUCCESS) {
            if (test != null) test.log(Status.PASS, "Test PASSED");
        } else if (result.getStatus() == ITestResult.SKIP) {
            if (test != null) test.log(Status.SKIP, "Test SKIPPED: " + result.getThrowable());
        }

        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    public static ExtentReports getExtent() {
        return extent;
    }

    protected ExtentTest createTest(String testName, String description) {
        ExtentTest test = extent.createTest(testName, description);
        extentTestThreadLocal.set(test);
        return test;
    }

    private WebDriver createDriver(String browser) {
        // Detect headless mode: set HEADLESS=true in CI environment
        boolean headless = Boolean.parseBoolean(System.getenv().getOrDefault("HEADLESS", "false"));

        switch (browser.toLowerCase()) {
            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-dev-shm-usage");
                    options.addArguments("--window-size=1920,1080");
                    options.addArguments("--ignore-certificate-errors"); 
                    System.out.println("[BaseTest] Running in HEADLESS mode (CI)");
                }
                options.addArguments("--disable-notifications");
                options.addArguments("--disable-popup-blocking");
                return new ChromeDriver(options);
        }
    }

    private String captureScreenshot(String testName) {
        try {
            WebDriver driver = getDriver();
            if (driver == null) return null;
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            String fileName = SCREENSHOT_DIR + testName + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + ".png";
            Path path = Paths.get(fileName);
            Files.write(path, bytes);
            System.out.println("[BaseTest] Screenshot saved → " + fileName);
            return fileName;
        } catch (IOException e) {
            System.err.println("[BaseTest] Screenshot capture failed: " + e.getMessage());
            return null;
        }
    }
}
