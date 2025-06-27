package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.util.*;
import java.util.regex.*;

public class VendorPage {
    private WebDriver driver;
    private WebDriverWait wait;

    public VendorPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
    }

    private By countrySelect = By.cssSelector("select.p3.h-16.koreanNoTranslate");
    private By gurgaonOption = By.xpath("//div[contains(@class,'LocationPopUp_zoneName') and text()='Gurgaon']");
    private By vendorContainer = By.cssSelector(".Vendor_vendorComponentContainer__X65Vl");

    public void navigateToVendorSection() {
        driver.get("https://ac-react.advantageclub.co/pages/sections?section_id=3");
        wait.until(ExpectedConditions.elementToBeClickable(countrySelect));
        new Select(driver.findElement(countrySelect)).selectByVisibleText("India");
        wait.until(ExpectedConditions.elementToBeClickable(gurgaonOption)).click();
    }

    public List<String[]> extractVendors() throws InterruptedException {
        List<String[]> vendorData = new ArrayList<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(1500);
            long newHeight = (long) js.executeScript("return document.body.scrollHeight");
            if (newHeight == lastHeight) break;
            lastHeight = newHeight;
        }

        List<WebElement> containers = driver.findElements(vendorContainer);
        for (WebElement container : containers) {
            try {
                String href = container.findElement(By.cssSelector("a[href*='/deals/']")).getAttribute("href");
                String name = container.findElement(By.xpath(".//h2[contains(@class, 'Vendor_vendorName')]")).getText();
                Matcher matcher = Pattern.compile("/deals/(\\d+)").matcher(href);
                if (matcher.find()) {
                    vendorData.add(new String[]{matcher.group(1), name});
                }
            } catch (Exception ignored) {}
        }

        return vendorData;
    }
}
