# SauceDemo SDET Automation Framework

[![CI](https://github.com/MrEinsteinE/saucedemo-sdet-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/MrEinsteinE/saucedemo-sdet-framework/actions)
![Java](https://img.shields.io/badge/Java-8+-blue)
![Selenium](https://img.shields.io/badge/Selenium-4.18-green)
![TestNG](https://img.shields.io/badge/TestNG-7.9-orange)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

A production-grade test automation framework for the [SauceDemo](https://www.saucedemo.com) e-commerce web application, built to demonstrate core **SDET best practices**.

---

## 📐 Architecture

```
saucedemo-sdet-framework/
├── src/test/java/com/sdet/
│   ├── pages/              # Page Object Model (POM) layer
│   │   ├── BasePage.java       ← shared page interactions
│   │   ├── LoginPage.java
│   │   ├── InventoryPage.java
│   │   ├── CartPage.java
│   │   └── CheckoutPage.java
│   ├── tests/              # Test classes (TestNG)
│   │   ├── LoginTest.java      ← 7 tests (incl. data-driven)
│   │   ├── InventoryTest.java  ← 8 tests
│   │   └── CheckoutTest.java   ← 6 tests (E2E)
│   └── utils/              # Reusable utilities
│       ├── BaseTest.java       ← driver lifecycle, ExtentReports, screenshots
│       ├── ExcelDataReader.java← data-driven testing via Apache POI
│       └── WaitUtils.java      ← explicit wait helpers
├── src/test/resources/
│   ├── testng.xml              ← suite config (smoke + regression)
│   └── testdata/
│       └── login_data.xlsx     ← Excel data for data-driven tests
└── .github/workflows/
    └── ci.yml                  ← GitHub Actions CI/CD pipeline
```

---

## ✅ Test Coverage (21 Test Cases)

| Module          | Test ID      | Description                                  | Group            |
|-----------------|-------------|----------------------------------------------|-----------------|
| **Login**       | TC-001       | Valid login → lands on inventory              | smoke, regression |
|                 | TC-002       | Invalid password → error message              | regression       |
|                 | TC-003       | Locked-out user → lock error                  | regression       |
|                 | TC-004       | Empty form submission → required field error  | regression       |
|                 | TC-005       | Missing password → password error             | regression       |
|                 | TC-006       | Browser title validation                      | smoke            |
|                 | TC-007       | **Data-driven login** (10 rows, Excel)        | regression, data-driven |
| **Inventory**   | TC-101       | 6 products displayed                          | smoke, regression |
|                 | TC-102       | Add 1 item → badge = 1                        | smoke, regression |
|                 | TC-103       | Add 3 items → badge = 3                       | regression       |
|                 | TC-104       | Sort A→Z verification                         | regression       |
|                 | TC-105       | Sort Z→A verification                         | regression       |
|                 | TC-106       | Sort price low→high                           | regression       |
|                 | TC-107       | Sort price high→low                           | regression       |
|                 | TC-108       | Cart icon navigates to /cart.html             | smoke, regression |
| **Checkout**    | TC-201       | **Full E2E checkout** (happy path)            | smoke, e2e       |
|                 | TC-202       | Missing first name → error                    | regression       |
|                 | TC-203       | Empty cart checkout                           | regression       |
|                 | TC-204       | Cancel on step 2 → back to inventory          | regression       |
|                 | TC-205       | Remove item from cart                         | regression       |
|                 | TC-206       | Multi-item (3 items) E2E checkout             | regression, e2e  |

---

## 🛠️ Tech Stack

| Component            | Technology                        |
|----------------------|-----------------------------------|
| Language             | Java 8+                           |
| Test Framework       | TestNG 7.9                        |
| Browser Automation   | Selenium WebDriver 4.18           |
| Driver Management    | WebDriverManager 5.7 (auto-setup) |
| Test Reporting       | ExtentReports 5 (HTML, dark theme)|
| Data-Driven Testing  | Apache POI (Excel .xlsx)          |
| Build Tool           | Maven                             |
| CI/CD                | GitHub Actions                    |
| Design Pattern       | Page Object Model (POM)           |

---

## 🚀 Prerequisites

- Java 8+
- Maven 3.8+
- Google Chrome (latest)

> **No manual ChromeDriver setup required** — WebDriverManager handles it automatically.

---

## ▶️ Running Tests

```bash
# Clone
git clone https://github.com/MrEinsteinE/saucedemo-sdet-framework.git
cd saucedemo-sdet-framework

# Run full suite
mvn clean test

# Run only smoke tests
mvn clean test -Dgroups=smoke

# Run only regression
mvn clean test -Dgroups=regression

# Run only E2E tests
mvn clean test -Dgroups=e2e

# Run headless (CI mode)
HEADLESS=true mvn clean test
```

---

## 📊 Test Reports

After running, open the HTML report:
```
test-output/reports/TestReport_<timestamp>.html
```

The report includes:
- Pass/Fail/Skip counts per test
- Detailed step logs
- Failure screenshots embedded directly in the report
- System info (author, framework, app under test)

---

## 🔄 CI/CD Pipeline

Every push to `main` or `develop` triggers the GitHub Actions workflow:

1. Spins up Ubuntu runner
2. Installs JDK 11 + Chrome
3. Runs tests in **headless mode**
4. Uploads HTML report as a downloadable artifact
5. Captures and uploads screenshots on failure
6. Publishes TestNG XML results as a PR check

Nightly regression run is also scheduled at 02:00 UTC daily.

---

## 🧪 Data-Driven Testing

`TC-007` reads from `src/test/resources/testdata/login_data.xlsx` (Sheet: `LoginTests`).

| username | password | expected_result | error_hint | notes |
|---|---|---|---|---|
| standard_user | secret_sauce | success | | Happy path |
| locked_out_user | secret_sauce | failure | locked out | Should be blocked |
| … | … | … | … | … |

To add more test scenarios, just add rows to the Excel file — no code changes needed.

---

## 📌 Key SDET Concepts Demonstrated

| Concept | Where |
|---|---|
| Page Object Model (POM) | `pages/` package |
| Explicit Waits (no Thread.sleep) | `WaitUtils.java` |
| TestNG groups (smoke/regression/e2e) | `testng.xml` + `@Test(groups=...)` |
| Data-driven testing (Excel) | `ExcelDataReader.java` + TC-007 |
| Screenshot on failure | `BaseTest.java` → `@AfterMethod` |
| HTML reporting | `ExtentReports` in `BaseTest.java` |
| ThreadLocal for parallel safety | `BaseTest.driverThreadLocal` |
| CI/CD integration | `.github/workflows/ci.yml` |
| Headless execution | `HEADLESS=true` env var |

---

## 👤 Author

**Einstein Ellandala**  
[github.com/MrEinsteinE](https://github.com/MrEinsteinE) · [linkedin.com/in/einstein-ellandala](https://www.linkedin.com/in/einstein-ellandala)
