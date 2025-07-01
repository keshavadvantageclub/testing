package testngsuite;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.testng.ITestResult;
import org.testng.annotations.*;
import pages.HobbyClubPage;
import pages.LoginPage;
import utils.ExcelReader;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.net.URI;
import java.net.URL;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.MutableCapabilities;

import java.io.File;

public class HobbyClubTest {

	WebDriver driver;
	WebDriverWait wait;
	LoginPage loginPage;
	HobbyClubPage hobbyClubPage;
	String email, password;
	boolean isSuiteMode;
	
	
	private String getMediaPath(String relativePath) {
		File file = new File(relativePath);

		if (isSuiteMode) {
			// LambdaTest expects cloud URLs
			String cloudUrlPrefix = "https://lt-upload.lambdatest.com/sample/";
			return cloudUrlPrefix + file.getName(); // example: https://lt-upload.lambdatest.com/sample/sample_audio.mp3
		} else {
			// Local absolute path
			return file.getAbsolutePath();
		}
	}


	@Parameters({ "suiteMode" })
	@BeforeClass
	public void setUp(@Optional("local") String suiteMode) throws Exception {
		isSuiteMode = suiteMode.equalsIgnoreCase("lambdatest");

		if (isSuiteMode) {
			String username = System.getenv("LT_USERNAME");
			String accessKey = System.getenv("LT_ACCESS_KEY");

			// Fallback credentials for local/dev
			if (username == null || accessKey == null) {
				username = "keshavs";
				accessKey = "w7VX6i299beNSNd3PHgaepmemLtMpEbYS0ePnvJEV69Bmog1cN";
			}

			MutableCapabilities capabilities = new MutableCapabilities();
			capabilities.setCapability("browserName", "Chrome");
			capabilities.setCapability("browserVersion", "latest");

			MutableCapabilities ltOptions = new MutableCapabilities();
			ltOptions.setCapability("platformName", "Windows 11");
			ltOptions.setCapability("project", "Hobby Club Automation");
			ltOptions.setCapability("build", "HobbyClubTest_" + System.currentTimeMillis());
			ltOptions.setCapability("name", "HobbyClubTest");
			ltOptions.setCapability("selenium_version", "4.14.0");

			// ✅ Turn on all logs
			ltOptions.setCapability("console", "true");
			ltOptions.setCapability("network", "true");
			ltOptions.setCapability("visual", "true");
			ltOptions.setCapability("seleniumLogs", "true");
			ltOptions.setCapability("driverLogs", "true");

			capabilities.setCapability("LT:Options", ltOptions);

			URI uri = new URI("https", username + ":" + accessKey, "hub.lambdatest.com", 443, "/wd/hub", null, null);
			driver = new RemoteWebDriver(uri.toURL(), capabilities);

		} else {
		    // Local setup
		    WebDriverManager.chromedriver().setup();

		    new File("logs").mkdirs();
		    System.setProperty("webdriver.chrome.verboseLogging", "true");
		    System.setProperty("webdriver.chrome.logfile", "logs/chromedriver.log");

		    ChromeOptions options = new ChromeOptions();
		    if (System.getenv("CI") != null) {
		        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
		                "--disable-gpu", "--window-size=1920,1080");
		    }

		    driver = new ChromeDriver(options);
		    driver.manage().window().maximize(); // ✅ Add this line to maximize window in local mode
		}

		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		// Login & Navigation
		Object[][] data = ExcelReader.getData("src/test/resources/testdata.xlsx", "LoginData");
		email = data[0][0].toString();
		password = data[0][1].toString();

		loginPage = new LoginPage(driver, wait);
		hobbyClubPage = new HobbyClubPage(driver, wait);

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
		String buzz = "Buzz_" + System.currentTimeMillis();
		hobbyClubPage.postTextOnly(buzz);
		hobbyClubPage.verifyLastTextPost(buzz);
	}

	@Test(priority = 3)
	public void testPostAudioBuzz() {
		hobbyClubPage.openFirstClub();
		String audioCaption = "AudioBuzz_" + System.currentTimeMillis();
		String audioPath = "src/test/resources/sample_audio.mp3";
		hobbyClubPage.postAudioOnly(audioPath, audioCaption);
		hobbyClubPage.verifyLastAudioPost(audioCaption);
	}

	@Test(priority = 4)
	public void testPostVideoBuzz() {
		String videoCaption = "VideoBuzz_" + System.currentTimeMillis();
		String videoPath = "src/test/resources/sample_video.mp4";
		hobbyClubPage.postVideoOnly(videoPath, videoCaption);
		hobbyClubPage.verifyLastVideoPost(videoCaption);
	}

	@Test(priority = 5)
	public void testPostBuzzAndClickLike() throws InterruptedException {
		String buzz = "LikeBuzz_" + System.currentTimeMillis();
		hobbyClubPage.postTextOnly(buzz);
		hobbyClubPage.clickNewLikeIconForBuzz(buzz);
	}

	@Test(priority = 6)
	public void testPostBuzzAndComment() throws InterruptedException {
		String buzz = "CommentBuzz_" + System.currentTimeMillis();
		String comment = "Comment_" + System.currentTimeMillis();
		hobbyClubPage.postBuzzAndComment(buzz, comment);
	}

	@Test(priority = 7)
	public void testCommentAndDeleteExistingPost() throws InterruptedException {
		String postText = "DeleteBuzz_" + System.currentTimeMillis();
		String commentText = "DeleteMe_" + System.currentTimeMillis();
		hobbyClubPage.postTextOnly(postText);
		hobbyClubPage.commentAndDeleteOnExistingPost(postText, commentText);
	}

	@Test(priority = 8)
	public void testDeleteBuzzPost() throws InterruptedException {
		String postText = "DeleteOnlyBuzz_" + System.currentTimeMillis();
		hobbyClubPage.postTextOnly(postText);
		hobbyClubPage.deleteBuzzPost(postText);
	}

	@Test(priority = 9)
	public void testVerifyMemberCounts() {
		hobbyClubPage.verifyMemberCountsMatch();
	}

	@Test(priority = 10)
	public void testSearchAndValidateLukeCooperCardVisible() throws InterruptedException {
		hobbyClubPage.verifyMemberCardVisible("luke cooper");
	}

	@Test(priority = 11)
	public void testJoinAndLeaveClub() {
		hobbyClubPage.openFirstClub();
		hobbyClubPage.verifyJoinAndLeaveFunctionality();
	}

	@AfterMethod
	public void updateLambdaStatus(ITestResult result) {
		if (isSuiteMode) {
			String status = result.isSuccess() ? "passed" : "failed";
			((JavascriptExecutor) driver).executeScript("lambda-status=" + "\"" + status + "\"");
		}
	}

	@AfterClass
	public void tearDown() {
		if (driver != null) {
			driver.quit();
		}
	}
}
