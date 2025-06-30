package testngsuite;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.testng.annotations.*;
import pages.HobbyClubPage;
import pages.LoginPage;
import utils.ExcelReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;

public class HobbyClubTest {

	WebDriver driver;
	WebDriverWait wait;
	LoginPage loginPage;
	HobbyClubPage hobbyClubPage;
	String email, password;
	boolean isSuiteMode;

	@BeforeClass
	@Parameters("suiteMode")
	public void setUp(@Optional("false") String suiteMode) {
	    WebDriverManager.chromedriver().setup();

	    ChromeOptions options = new ChromeOptions();

	    // Check if headless is enabled (true by default in GitHub Actions)
	    boolean isHeadless = Boolean.parseBoolean(System.getProperty("headless", "false"));
	    if (isHeadless) {
	        options.addArguments("--headless=new");
	        options.addArguments("--window-size=1920,1080");
	    }

	    options.addArguments("--disable-dev-shm-usage");
	    options.addArguments("--no-sandbox");
	    options.addArguments("--disable-gpu");

	    driver = new ChromeDriver(options);
	    driver.manage().window().maximize();
	    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
	    wait = new WebDriverWait(driver, Duration.ofSeconds(15));

	    // Your login + setup code
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

	@Test(priority = 3, dependsOnMethods = "testPostBuzzInFirstClub")
	public void testPostAudioBuzz() {
		String audioCaption = "AudioBuzz_" + System.currentTimeMillis();
		String audioPath = "src/test/resources/sample_audio.mp3";
		hobbyClubPage.postAudioOnly(audioPath, audioCaption);
		hobbyClubPage.verifyLastAudioPost(audioCaption);
	}

	@Test(priority = 4, dependsOnMethods = "testPostAudioBuzz")
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
//	
//	@Test
//	public void testDeleteAllBuzzPosts() throws InterruptedException {
//	    hobbyClubPage.openFirstClub();
//	    hobbyClubPage.deleteAllBuzzPosts();
//	}
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
	    hobbyClubPage.verifyJoinAndLeaveFunctionality();
	}



	@AfterClass
	public void tearDown() {
		if (driver != null) {
			driver.quit();
		}
	}
}
