package testngsuite;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;
import org.testng.annotations.Optional;
import io.github.bonigarcia.wdm.WebDriverManager;
import utils.ExcelReader;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

public class VendorTests {
    WebDriver driver;
    WebDriverWait wait;
    String fileName;
    List<String[]> vendorData = new ArrayList<>();

    @Parameters("browser")
    @BeforeClass
    public void setUpAndLogin(@Optional("chrome") String browser) {
        if (browser.equalsIgnoreCase("chrome")) {
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver();
        } else if (browser.equalsIgnoreCase("firefox")) {
            WebDriverManager.firefoxdriver().setup();
            driver = new FirefoxDriver();
        } else if (browser.equalsIgnoreCase("edge")) {
            WebDriverManager.edgedriver().setup();
            driver = new EdgeDriver();
        } else {
            throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Read login credentials from Excel (first row only)
        Object[][] data = ExcelReader.getData("src/test/resources/testdata.xlsx", "LoginData");
        String email = data[0][0].toString();
        String password = data[0][1].toString();

        driver.get("https://ac-react.advantageclub.co/signin");
        driver.findElement(By.name("email")).sendKeys(email);
        driver.findElement(By.name("password")).sendKeys(password);
        driver.findElement(By.cssSelector("button[class*='Login_login']")).click();

        try {
            WebElement dropdown = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("select")));
            Assert.assertTrue(dropdown.isDisplayed(), "Login might have failed.");
        } catch (TimeoutException e) {
            Assert.fail("Login failed or dropdown not found.");
        }
    }

    @Test(priority = 1)
    public void testCountrySelection() {
        try {
            WebElement selectElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("select")));
            Select select = new Select(selectElement);
            select.selectByVisibleText("India");
            Assert.assertEquals(select.getFirstSelectedOption().getText(), "India");
        } catch (Exception e) {
            Assert.fail("Country selection failed: " + e.getMessage());
        }
    }

    @Test(priority = 2)
    public void testVendorSectionNavigation() {
        driver.get("https://ac-react.advantageclub.co/pages/sections?section_id=3");

        try {
            WebElement countryDropdown = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("select.p3.h-16.koreanNoTranslate")));
            Select select = new Select(countryDropdown);
            select.selectByVisibleText("India");

            WebElement gurgaon = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'LocationPopUp_zoneName') and text()='Gurgaon']")));
            gurgaon.click();

            List<WebElement> vendorList = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector(".Vendor_vendorComponentContainer__X65Vl")));

            Assert.assertTrue(vendorList.size() > 0, "Vendor list not loaded.");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Vendor navigation failed: " + e.getMessage());
        }
    }

    @Test(priority = 3, dependsOnMethods = {"testVendorSectionNavigation"})
    public void testVendorExtraction() throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(1500);
            long newHeight = (long) js.executeScript("return document.body.scrollHeight");
            if (newHeight == lastHeight) break;
            lastHeight = newHeight;
        }

        List<WebElement> vendorContainers = driver.findElements(By.cssSelector(".Vendor_vendorComponentContainer__X65Vl"));
        Assert.assertTrue(vendorContainers.size() > 0, "No vendors found.");

        for (WebElement container : vendorContainers) {
            try {
                String href = container.findElement(By.cssSelector("a[href*='/deals/']")).getAttribute("href");
                String name = container.findElement(By.xpath(".//h2[contains(@class, 'Vendor_vendorName')]")).getText();

                Matcher matcher = Pattern.compile("/deals/(\\d+)").matcher(href);
                if (matcher.find()) {
                    System.out.println("Found Vendor: " + matcher.group(1) + " - " + name);
                    vendorData.add(new String[]{matcher.group(1), name});
                }
            } catch (Exception ignored) {}
        }

        Assert.assertTrue(vendorData.size() > 0, "No valid vendor data extracted.");
    }

    @Test(priority = 4, dependsOnMethods = {"testVendorExtraction"})
    public void testCsvFileGenerated() throws IOException {
        fileName = "vendors_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
        FileWriter writer = new FileWriter(fileName);
        writer.append("Vendor ID,Vendor Name\n");

        for (String[] entry : vendorData) {
            writer.append(entry[0]).append(",\"").append(entry[1].replace("\"", "\"\"")).append("\"\n");
        }

        writer.flush();
        writer.close();

        File csvFile = new File(fileName);
        Assert.assertTrue(csvFile.exists(), "CSV file not created.");
        Assert.assertTrue(csvFile.length() > 30, "CSV file may be empty.");
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) driver.quit();
    }
}
