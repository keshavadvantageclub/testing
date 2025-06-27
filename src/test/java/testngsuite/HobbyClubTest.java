package testngsuite;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import org.testng.Assert;
import org.testng.annotations.*;

import pages.HobbyClubPage;
import pages.LoginPage;
import utils.ExcelReader;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.List;

public class HobbyClubTest {

    WebDriver driver;
    WebDriverWait wait;
    LoginPage loginPage;
    HobbyClubPage hobbyClubPage;
    String email, password;
    boolean isSuiteMode;

    @Parameters("suiteMode")
    @BeforeClass
    public void setUp(@Optional("false") String suiteMode) {
    	WebDriverManager.chromedriver().setup();
    	ChromeOptions options = new ChromeOptions();
    	options.addArguments("--remote-allow-origins=*");
    	options.addArguments("--disable-dev-shm-usage", "--disable-gpu", "--no-sandbox", "--disable-extensions");

    	driver = new ChromeDriver(options);
    	driver.manage().window().maximize();
    	driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    	wait = new WebDriverWait(driver, Duration.ofSeconds(15));

    	Object[][] data = ExcelReader.getData("src/test/resources/testdata.xlsx", "LoginData");
    	email = data[0][0].toString();
    	password = data[0][1].toString();

    	loginPage = new LoginPage(driver, wait);
    	hobbyClubPage = new HobbyClubPage(driver, wait);

    	// Always login and open first club
    	loginPage.openLoginPage();
    	loginPage.login(email, password);
    	hobbyClubPage.openHobbyClubsPage();
    	hobbyClubPage.selectCountryAndCityIfVisible("India", "Gurgaon");
    }


    @Test(priority = 1)
    public void testVerifyClubListingAndDetailMatch() {
        hobbyClubPage.verifyAllClubCardsAndDetails();
    }

    @Test(priority = 2)
    public void testPostBuzzInFirstClub() throws InterruptedException {
        hobbyClubPage.openFirstClub();
        String buzz = "Testing";
        hobbyClubPage.postTextOnly(buzz);
        hobbyClubPage.verifyLastTextPost(buzz);
    }

    @Test(priority = 3, dependsOnMethods = "testPostBuzzInFirstClub")
    public void testPostAudioBuzz() {
        String audioCaption = "This is an audio buzz!";
        String audioPath = "src/test/resources/sample_audio.mp3";
        hobbyClubPage.postAudioOnly(audioPath, audioCaption);
        hobbyClubPage.verifyLastAudioPost(audioCaption);
    }

    @Test(priority = 4, dependsOnMethods = "testPostAudioBuzz")
    public void testPostVideoBuzz() {
        String videoCaption = "This is a video buzz!";
        String videoPath = "src/test/resources/sample_video.mp4";
        hobbyClubPage.postVideoOnly(videoPath, videoCaption);
        hobbyClubPage.verifyLastVideoPost(videoCaption);
    }

    @Test(priority = 5, dependsOnMethods = "testPostVideoBuzz")
    public void verifyAllPostsAfterReload() {


        driver.navigate().refresh(); // 🔁 Reload the club page

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@class,'Feed_centerContainer')]")));

        hobbyClubPage.verifyLastTextPost("Testing");
        hobbyClubPage.verifyLastAudioPost("This is an audio buzz!");
        hobbyClubPage.verifyLastVideoPost("This is a video buzz!");
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
