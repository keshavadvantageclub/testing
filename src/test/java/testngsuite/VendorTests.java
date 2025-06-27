package testngsuite;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.LoginPage;
import pages.VendorPage;
import utils.ExcelReader;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class VendorTests {
    WebDriver driver;
    WebDriverWait wait;
    VendorPage vendorPage;
    List<String[]> vendorData = new ArrayList<>();
    String fileName;

    @BeforeClass
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        driver = new ChromeDriver(options);

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Read credentials
        Object[][] data = ExcelReader.getData("src/test/resources/testdata.xlsx", "LoginData");
        String email = data[0][0].toString();
        String password = data[0][1].toString();

        LoginPage loginPage = new LoginPage(driver, wait);
        loginPage.openLoginPage();
        loginPage.login(email, password);

        vendorPage = new VendorPage(driver, wait);
    }

    @Test(priority = 1)
    public void testNavigateToVendors() {
        vendorPage.navigateToVendorSection();
    }

    @Test(priority = 2, dependsOnMethods = {"testNavigateToVendors"})
    public void testExtractVendors() throws InterruptedException {
        vendorData = vendorPage.extractVendors();
        Assert.assertTrue(vendorData.size() > 0, "No vendor data extracted.");
    }

    @Test(priority = 3, dependsOnMethods = {"testExtractVendors"})
    public void testSaveCsv() throws IOException {
        fileName = "vendors_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
        FileWriter writer = new FileWriter(fileName);
        writer.append("Vendor ID,Vendor Name\n");

        for (String[] entry : vendorData) {
            writer.append(entry[0]).append(",\"").append(entry[1].replace("\"", "\"\"")).append("\"\n");
        }

        writer.flush();
        writer.close();

        File file = new File(fileName);
        Assert.assertTrue(file.exists() && file.length() > 30, "CSV not created properly.");
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) driver.quit();
    }
}
