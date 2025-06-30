package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class HobbyClubPage {
	private WebDriver driver;
	private WebDriverWait wait;

	public HobbyClubPage(WebDriver driver, WebDriverWait wait) {
		this.driver = driver;
		this.wait = wait;
	}

	private By clubCardsLocator = By.xpath("//div[contains(@class,'HobbyCardComponent_hobbyCard')]");

	public void openHobbyClubsPage() {
		driver.get("https://ac-react.advantageclub.co/pages/hobby_clubs");
	}

	public void selectCountryAndCityIfVisible(String country, String city) {
		try {
			WebElement dropdown = wait.until(
					ExpectedConditions.presenceOfElementLocated(By.cssSelector("select.p3.h-16.koreanNoTranslate")));
			new Select(dropdown).selectByVisibleText(country);

			WebElement cityElement = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//div[contains(@class,'LocationPopUp_zoneName') and text()='" + city + "']")));
			cityElement.click();
			System.out.println("City '" + city + "' selected.");
		} catch (TimeoutException e) {
			System.out.println("Popup not visible — skipping location selection.");
		}
	}

	public void verifyAllClubCardsAndDetails() {
		List<WebElement> clubCards = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(clubCardsLocator));
		System.out.println("🔍 Total clubs found: " + clubCards.size());

		for (int i = 0; i < clubCards.size(); i++) {
			// Refetch list each time to avoid StaleElementReferenceException
			clubCards = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(clubCardsLocator));
			WebElement card = clubCards.get(i);

			// Extract data from listing
			String name = card.findElement(By.cssSelector(".inner_text_container p.p1")).getText().trim();
			String memberText = card.findElement(By.cssSelector(".inner_text_container p:nth-of-type(2)")).getText()
					.trim();
			int memberCount = extractNumber(memberText);
			String quote = extractQuote(card);

			System.out.println("📋 [Listing] → " + name + " | Members: " + memberCount + " | Quote: " + quote);

			// Basic validations
			Assert.assertFalse(name.isEmpty(), "❌ Club name is empty!");
			Assert.assertTrue(memberCount > 0, "❌ Member count invalid for: " + name);
			Assert.assertFalse(quote.isEmpty(), "❌ Quote missing for: " + name);

			// Click card
			scrollAndClick1(card);
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.flex.flex-col")));

			// Banner check
			List<WebElement> banners = driver.findElements(By.cssSelector("img.w-full"));
			Assert.assertFalse(banners.isEmpty(), "❌ Banner not found for: " + name);
			Assert.assertTrue(banners.get(0).isDisplayed(), "❌ Banner not visible for: " + name);

			// Extract detail page info
			String detailName = getTextOrEmpty1(
					By.xpath("//div[@class='font-semibold' and not(contains(text(),'Points'))]"));
			String detailQuote = getDetailQuote();
			WebElement memberDiv = driver.findElement(By.xpath("//div[p[text()='Members']]"));
			int detailMemberCount = Integer.parseInt(memberDiv.findElement(By.xpath("./p[1]")).getText().trim());

			// Compare with listing
			Assert.assertEquals(detailName, name, "❌ Club name mismatch!");
			Assert.assertEquals(detailQuote, quote, "❌ Quote mismatch!");
			Assert.assertEquals(detailMemberCount, memberCount, "❌ Member count mismatch!");

			System.out.println("✅ [Detail Match] → " + detailName + " | Members: " + detailMemberCount + " | Quote: "
					+ detailQuote);
			System.out.println("------------------------------------------------");

			// Go back and wait for cards again
			driver.navigate().back();
			wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(clubCardsLocator));
		}
	}

	private void scrollAndClick1(WebElement element) {
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", element);
		try {
			element.click();
		} catch (ElementClickInterceptedException e) {
			((JavascriptExecutor) driver).executeScript("arguments[0].click()", element);
		}
	}

	private String getTextOrEmpty1(By locator) {
		try {
			return driver.findElement(locator).getText().trim();
		} catch (NoSuchElementException e) {
			return "";
		}
	}

	public void openFirstClub() {
		List<WebElement> cards = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(clubCardsLocator));
		WebElement first = cards.get(0);
		scrollAndClick1(first);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.flex.flex-col")));
	}

	private String extractQuote(WebElement card) {
		try {
			return card.findElement(By.cssSelector(".HobbyCardComponent_descriptionText__DCzmn p b")).getText().trim();
		} catch (NoSuchElementException e) {
			try {
				return card.findElement(By.cssSelector(".HobbyCardComponent_descriptionText__DCzmn p strong")).getText()
						.trim();
			} catch (NoSuchElementException ignore) {
				return "";
			}
		}
	}

	private int extractNumber(String text) {
		try {
			return Integer.parseInt(text.replaceAll("[^0-9]", ""));
		} catch (Exception e) {
			return 0;
		}
	}

	private String getDetailQuote() {
		String quote = getTextOrEmpty1(By.xpath("//*[@id='maincomponent-layout-body-wrapper']//p/b"));
		if (!quote.isEmpty())
			return quote;

		quote = getTextOrEmpty1(By.xpath("//*[@id='maincomponent-layout-body-wrapper']//p/strong"));
		if (!quote.isEmpty())
			return quote;

		List<WebElement> paragraphs = driver.findElements(By.xpath("//*[@id='maincomponent-layout-body-wrapper']//p"));
		for (WebElement para : paragraphs) {
			String text = para.getText().trim();
			if (text.length() > 20 && text.startsWith("\""))
				return text;
		}
		return "";
	}

	public void postTextOnly(String text) throws InterruptedException {
		WebElement ta = wait.until(
				ExpectedConditions.visibilityOfElementLocated(By.cssSelector("textarea.PostStatus_textArea__ySn55")));
		ta.clear();
		ta.sendKeys(text);
		clickPostButton();
		System.out.println("✅ Text-only posted: " + text);
	}

	private void clickPostButton() throws InterruptedException {
		By btnLoc = By.xpath("//button[contains(text(),'Post')]");
		WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(btnLoc));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'})", btn);

		for (int i = 0; i < 10; i++) {
			if (btn.isEnabled()) {
				try {
					btn.click();
					break;
				} catch (ElementClickInterceptedException e) {
					((JavascriptExecutor) driver).executeScript("arguments[0].click()", btn);
					break;
				}
			}
			Thread.sleep(500);
			btn = driver.findElement(btnLoc);
		}
		Thread.sleep(2000); // wait for buzz to post
	}

	public void postMediaOnly(String path) throws InterruptedException {
		File file = new File(path);
		if (!file.exists()) {
			System.out.println("⚠️ File not found: " + path);
			return;
		}

		WebElement fileInput = wait
				.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")));
		fileInput.sendKeys(file.getAbsolutePath());

		System.out.println("📎 Uploaded: " + file.getName());

		Thread.sleep(2000); // Give some time for upload preview to render
		clickPostButton();

		System.out.println("✅ Media-only posted: " + file.getName());
	}

	public void verifyLastTextPost(String expectedText) {
		try {
			// ✅ Step 1: Wait and read toast message
			WebElement toast = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.Toastify__toast")));
			String toastMsg = toast.getText().trim();
			System.out.println("🔔 Toast message: " + toastMsg);

			// ✅ Step 2: Handle expected error toast (character limit)
			if (toastMsg.toLowerCase().contains("minimum") || toastMsg.toLowerCase().contains("character")) {
				Assert.assertTrue(
						toastMsg.toLowerCase().contains("minimum character limit is 5")
								|| toastMsg.toLowerCase().contains("please enter 5 characters"),
						"❌ Unexpected toast error message: " + toastMsg);
				System.out.println("✅ Toast error message verified.");
				return; // ✅ Skip post UI checks if post failed
			}

			// ✅ Step 3: Handle success toast
			Assert.assertTrue(toastMsg.toLowerCase().contains("buzz added successfully"),
					"❌ Unexpected success toast message: " + toastMsg);
			System.out.println("✅ Toast success message verified.");

			// ✅ Step 4: Verify poster name
			WebElement nameElement = wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//span[@class='font-semibold' and text()='You']")));
			Assert.assertTrue(nameElement.isDisplayed(), "❌ Name 'You' not visible.");
			System.out.println("✅ Name verified as 'You'.");

			// ✅ Step 5: Verify post text
			WebElement postTextElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
					"//div[@class='koreanNoTranslate break-words p1 w-full' and text()='" + expectedText + "']")));
			Assert.assertTrue(postTextElement.isDisplayed(), "❌ Post text not visible.");
			System.out.println("✅ Post text verified.");

			// ✅ Step 6: Validate timestamp within 5 minutes
			WebElement dateElement = wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//div[@class='p5 text-subtitle' and contains(text(),',')]")));
			String postDate = dateElement.getText().replace("\u00A0", " ").trim();
			System.out.println("✅ Post date found: " + postDate);

			String fullPostDate = postDate + " " + Year.now().getValue();
			LocalDateTime postDateTime = LocalDateTime.parse(fullPostDate,
					DateTimeFormatter.ofPattern("MMM dd, hh:mm a yyyy", Locale.ENGLISH));
			LocalTime uiTime = postDateTime.toLocalTime();
			LocalTime systemTime = LocalTime.now();

			long diff = Math.abs(systemTime.toSecondOfDay() - uiTime.toSecondOfDay());
			Assert.assertTrue(diff <= 300, "❌ Post time is not within 5 minutes.");
			System.out.println("✅ Post time is within 5 minutes.");

			// ✅ Step 7: Verify 3-dot menu presence
			WebElement threeDot = wait.until(ExpectedConditions.visibilityOfElementLocated(
					By.xpath("//div[contains(@class,'cursor-pointer')]/div/*[name()='svg']")));
			Assert.assertTrue(threeDot.isDisplayed(), "❌ Three-dot menu not visible.");
			System.out.println("✅ Three-dot menu is present.");

		} catch (TimeoutException e) {
			Assert.fail("❌ Toast message did not appear in time.");
		} catch (Exception e) {
			Assert.fail("❌ Post verification failed: " + e.getMessage());
		}
	}

	public void postAudioOnly(String filePath, String caption) {
		try {
			// Upload audio file
			WebElement uploadInput = wait
					.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[type='file']")));
			uploadInput.sendKeys(new File(filePath).getAbsolutePath());

			// Optional caption
			WebElement captionBox = wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.cssSelector("textarea.PostStatus_textArea__ySn55")));
			captionBox.clear();
			captionBox.sendKeys(caption);

			clickPostButton(); // Existing method to click the Post button
			System.out.println("✅ Audio posted with caption: " + caption);
		} catch (Exception e) {
			Assert.fail("❌ Failed to post audio: " + e.getMessage());
		}
	}

	public void verifyLastAudioPost(String expectedCaption) {
		try {
			WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
			WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));

			// ✅ Step 1: Wait for and verify toast
			WebElement toast = longWait.until(driver -> {
				List<WebElement> toasts = driver.findElements(By.cssSelector("div.Toastify__toast"));
				return toasts.stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
			});

			String toastMsg = (String) ((JavascriptExecutor) driver).executeScript(
					"return arguments[0].childNodes.length > 1 ? arguments[0].childNodes[1].nodeValue.trim() : arguments[0].textContent.trim();",
					toast);

			System.out.println("🔔 Toast message: '" + toastMsg + "'");

			if (toastMsg.toLowerCase().contains("minimum") || toastMsg.toLowerCase().contains("character")) {
				Assert.assertTrue(
						toastMsg.toLowerCase().contains("minimum character limit is 5")
								|| toastMsg.toLowerCase().contains("please enter 5 characters"),
						"❌ Unexpected toast error message: " + toastMsg);
				System.out.println("✅ Toast error message verified.");
				return;
			}

			Assert.assertTrue(toastMsg.toLowerCase().contains("buzz added successfully"),
					"❌ Unexpected success toast message: " + toastMsg);
			System.out.println("✅ Toast success message verified.");

			// ✅ Step 2: Verify poster name
			WebElement nameElement = shortWait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//span[@class='font-semibold' and text()='You']")));
			Assert.assertTrue(nameElement.isDisplayed(), "❌ Poster name 'You' not visible.");
			System.out.println("✅ Poster name verified.");

			// ✅ Step 3: Caption check
			WebElement caption = shortWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
					"//div[@class='koreanNoTranslate break-words p1 w-full' and text()='" + expectedCaption + "']")));
			Assert.assertTrue(caption.isDisplayed(), "❌ Caption not visible.");
			System.out.println("✅ Caption verified.");

			// ✅ Step 4: Timestamp check
			WebElement dateElement = shortWait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//div[@class='p5 text-subtitle' and contains(text(),',')]")));
			String postDate = dateElement.getText().replace("\u00A0", " ").trim();
			String fullPostDate = postDate + " " + Year.now().getValue();
			LocalDateTime postTime = LocalDateTime.parse(fullPostDate,
					DateTimeFormatter.ofPattern("MMM dd, hh:mm a yyyy", Locale.ENGLISH));
			long diffSec = Math.abs(LocalTime.now().toSecondOfDay() - postTime.toLocalTime().toSecondOfDay());
			Assert.assertTrue(diffSec <= 300, "❌ Post time is not within 5 minutes.");
			System.out.println("✅ Timestamp verified.");

			// ✅ Step 6: Audio player container check (instead of raw <audio> tag)
			WebElement audioWrapper = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[text()='"
					+ expectedCaption
					+ "']/ancestor::div[contains(@class,'Feed_centerContainer')]//div[contains(@class,'CustomAudioPlayer_audioPlayerComponentWrapper')]")));
			Assert.assertTrue(audioWrapper.isDisplayed(), "❌ Audio player wrapper not visible.");
			System.out.println("✅ Audio player container verified.");

		} catch (TimeoutException e) {
			Assert.fail("❌ Timeout while verifying post: " + e.getMessage());
		} catch (Exception e) {
			Assert.fail("❌ Unexpected error during post verification: " + e.getMessage());
		}
	}

	public void verifyLastVideoPost(String expectedCaption) {
		try {
			WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(30));
			WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(10));

			// ✅ Step 1: Wait for and verify toast
			WebElement toast = longWait.until(driver -> {
				List<WebElement> toasts = driver.findElements(By.cssSelector("div.Toastify__toast"));
				return toasts.stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
			});

			String toastMsg = (String) ((JavascriptExecutor) driver).executeScript(
					"return arguments[0].childNodes.length > 1 ? arguments[0].childNodes[1].nodeValue.trim() : arguments[0].textContent.trim();",
					toast);

			System.out.println("🔔 Toast message: '" + toastMsg + "'");

			if (toastMsg.toLowerCase().contains("minimum") || toastMsg.toLowerCase().contains("character")) {
				Assert.assertTrue(
						toastMsg.toLowerCase().contains("minimum character limit is 5")
								|| toastMsg.toLowerCase().contains("please enter 5 characters"),
						"❌ Unexpected toast error message: " + toastMsg);
				System.out.println("✅ Toast error message verified.");
				return;
			}

			Assert.assertTrue(toastMsg.toLowerCase().contains("buzz added successfully"),
					"❌ Unexpected success toast message: " + toastMsg);
			System.out.println("✅ Toast success message verified.");

			// ✅ Step 2: Poster name
			WebElement nameElement = shortWait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//span[@class='font-semibold' and text()='You']")));
			Assert.assertTrue(nameElement.isDisplayed(), "❌ Poster name 'You' not visible.");
			System.out.println("✅ Poster name verified.");

			// ✅ Step 3: Caption
			WebElement caption = shortWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
					"//div[@class='koreanNoTranslate break-words p1 w-full' and text()='" + expectedCaption + "']")));
			Assert.assertTrue(caption.isDisplayed(), "❌ Caption not visible.");
			System.out.println("✅ Caption verified.");

			// ✅ Step 4: Timestamp
			WebElement dateElement = shortWait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//div[@class='p5 text-subtitle' and contains(text(),',')]")));
			String postDate = dateElement.getText().replace("\u00A0", " ").trim();
			String fullPostDate = postDate + " " + Year.now().getValue();
			LocalDateTime postTime = LocalDateTime.parse(fullPostDate,
					DateTimeFormatter.ofPattern("MMM dd, hh:mm a yyyy", Locale.ENGLISH));
			long diffSec = Math.abs(LocalTime.now().toSecondOfDay() - postTime.toLocalTime().toSecondOfDay());
			Assert.assertTrue(diffSec <= 300, "❌ Post time is not within 5 minutes.");
			System.out.println("✅ Timestamp verified.");

			WebElement videoElement = shortWait
					.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[text()='" + expectedCaption
							+ "']/ancestor::div[contains(@class,'Feed_centerContainer')]//video")));
			Assert.assertTrue(videoElement.isDisplayed(), "❌ Video element not visible.");
			System.out.println("✅ Video element verified.");

			// ✅ Optional: Print video src
			String videoSrc = videoElement.getAttribute("src");
			Assert.assertNotNull(videoSrc, "❌ Video source (src) is null.");
			System.out.println("🎥 Video source: " + videoSrc);

		} catch (TimeoutException e) {
			Assert.fail("❌ Timeout while verifying post: " + e.getMessage());
		} catch (Exception e) {
			Assert.fail("❌ Unexpected error during post verification: " + e.getMessage());
		}
	}

	public void postVideoOnly(String videoPath, String caption) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

			// Step 1: Enter text
			WebElement textArea = wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.cssSelector("textarea.PostStatus_textArea__ySn55")));
			textArea.sendKeys(caption);
			System.out.println("✅ Caption entered: " + caption);

			// Step 2: Upload video
			WebElement fileInput = driver.findElement(By.cssSelector("input[type='file'][accept*='video']"));
			fileInput.sendKeys(new File(videoPath).getAbsolutePath());
			System.out.println("✅ Video uploaded: " + videoPath);

			// Step 3: Wait for video to attach (small buffer)
			Thread.sleep(2000); // optional, can be improved by checking media preview element

			// Step 4: Click post button safely
			WebElement postBtn = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[text()='Post']")));

			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", postBtn);
			wait.until(ExpectedConditions.elementToBeClickable(postBtn));

			try {
				postBtn.click();
				System.out.println("✅ Post button clicked (normal click).");
			} catch (ElementClickInterceptedException e) {
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", postBtn);
				System.out.println("✅ Post button clicked (via JS fallback).");
			}

		} catch (Exception e) {
			Assert.fail("❌ Failed to post video: " + e.getMessage());
		}
	}

	public void clickNewLikeIconForBuzz(String buzzText) throws InterruptedException {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		// 1. Locate the post container by buzz text
		WebElement postContainer = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//div[contains(@class,'Feed_newsFeedContainer__')][.//div[text()='" + buzzText + "']]")));

		// 2. Scroll to the post
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", postContainer);
		Thread.sleep(500);

		// 3. Locate the like <img> by its src
		WebElement likeIcon = postContainer.findElement(By.xpath(".//img[contains(@src,'fb2a529a83.svg')]"));

		// 4. Scroll the like icon into center view
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", likeIcon);
		Thread.sleep(300);

		// 5. Click via JavaScript to bypass overlays/intercepts
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", likeIcon);
		System.out.println("✅ Like icon clicked (via JS).");

		// 6. Optional: Wait to visually confirm and get count
		Thread.sleep(1000);

		WebElement countEl = likeIcon.findElement(By.xpath("./following-sibling::div"));
		int count = Integer.parseInt(countEl.getText().trim());
		System.out.println("🧮 New Like count: " + count);

		Assert.assertTrue(count > 0, "❌ Like count did not increase.");
	}

	public void postBuzzAndComment(String buzzText, String commentText) throws InterruptedException {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		// ✅ Step 1: Post a new buzz
		postTextOnly(buzzText);
		verifyLastTextPost(buzzText);

		// ✅ Step 2: Locate the new post by its text
		WebElement post = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//div[contains(@class,'Feed_newsFeedContainer__')][.//div[text()='" + buzzText + "']]")));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", post);
		Thread.sleep(500);

		// ✅ Step 3: Click on comment icon
		WebElement commentIcon = post.findElement(By.xpath(".//img[contains(@src,'5959793ac8.svg')]"));
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", commentIcon);

		// ✅ Step 4: Read comment count before
		WebElement countElement = post.findElement(By.xpath(".//span[@class='p1']"));
		int beforeCount = Integer.parseInt(countElement.getText().trim());
		System.out.println("💬 Comment count before: " + beforeCount);

		// ✅ Step 5: Type comment
		WebElement input = wait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@placeholder='Comment here..']")));
		input.sendKeys(commentText);
		Thread.sleep(300);

		// ✅ Step 6: Click Comment button
		WebElement commentBtn = driver
				.findElement(By.xpath("//span[contains(@class,'FeedCommentSection_commentBtn')]"));
		commentBtn.click();
		System.out.println("✅ Comment posted.");

		// ✅ Step 7: Wait and verify count increased
		Thread.sleep(1500);
		int afterCount = Integer.parseInt(countElement.getText().trim());
		System.out.println("🔄 Comment count after: " + afterCount);

		Assert.assertTrue(afterCount > beforeCount, "❌ Comment count did not increase");
	}

	public void commentAndDeleteOnExistingPost(String postText, String commentText) throws InterruptedException {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		// 1. Scroll to the post
		WebElement post = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//div[contains(@class,'Feed_newsFeedContainer__')][.//div[text()='" + postText + "']]")));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", post);
		Thread.sleep(500);

		// 2. Click on comment icon
		WebElement commentIcon = post.findElement(By.xpath(".//img[contains(@src,'5959793ac8.svg')]"));
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", commentIcon);
		Thread.sleep(500);

		// 3. Type comment and post
		WebElement input = wait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@placeholder='Comment here..']")));
		input.sendKeys(commentText);

		WebElement commentBtn = wait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[contains(text(),'Comment')]")));
		commentBtn.click();
		System.out.println("✅ Comment posted: " + commentText);
		Thread.sleep(2000);

		// 4. Locate the comment span
		WebElement commentSpan = wait.until(ExpectedConditions
				.presenceOfElementLocated(By.xpath("//span[@class='p3' and text()='" + commentText + "']")));

		// 5. Go up and find delete icon <i> within the same comment block
		WebElement container = commentSpan
				.findElement(By.xpath("./ancestor::div[contains(@class,'CommentFeed_userContainer__')]"));
		WebElement deleteIcon = container.findElement(By.xpath(".//i[contains(@class,'fa-trash-can')]"));

		// 6. Scroll and click delete icon
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", deleteIcon);
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteIcon);
		System.out.println("🗑️ Clicked delete icon for comment.");
		Thread.sleep(1500);

		// 6. Confirm popup click "Confirm"
		WebElement confirmButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
				"//div[contains(@class,'ConfirmationPopup_confirmationPopupWrapper')]//button[@value='confirm']")));
		confirmButton.click();
		System.out.println("☑️ Confirmed deletion in popup.");
		Thread.sleep(1500);

		// 7. Confirm it is deleted
		List<WebElement> deletedCheck = driver
				.findElements(By.xpath("//span[@class='p3' and text()='" + commentText + "']"));
		Assert.assertTrue(deletedCheck.isEmpty(), "❌ Comment was not deleted");
		System.out.println("✅ Comment deleted successfully.");
	}

	public void deleteBuzzPost(String postText) throws InterruptedException {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

		// 1. Locate the post by its exact text
		WebElement postTextElement = wait.until(ExpectedConditions.presenceOfElementLocated(By
				.xpath("//div[contains(@class,'koreanNoTranslate') and normalize-space(text())='" + postText + "']")));

		// 2. Scroll to the post
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", postTextElement);
		Thread.sleep(1000);

		// 3. Find the 3-dot SVG menu relative to this post
		WebElement svg = postTextElement
				.findElement(By.xpath("./ancestor::div[contains(@class,'Feed_newsFeedContainer__')]"
						+ "//div[@class='relative cursor-pointer']//*[name()='svg' and not(name()='path')]"));

		// 4. Click the 3-dot menu
		((JavascriptExecutor) driver)
				.executeScript("arguments[0].dispatchEvent(new MouseEvent('click', { bubbles: true }))", svg);
		System.out.println("☰ Clicked 3-dot menu.");
		Thread.sleep(1000);

		// 5. Click "Delete"
		WebElement deleteBtn = wait.until(ExpectedConditions
				.elementToBeClickable(By.xpath("//div[contains(@class,'px-8') and normalize-space(text())='Delete']")));
		deleteBtn.click();
		System.out.println("🗑️ Clicked Delete option.");
		Thread.sleep(1000);

		// 6. Confirm deletion in popup
		WebElement confirmBtn = wait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@value='confirm']")));
		confirmBtn.click();
		System.out.println("☑️ Confirmed deletion.");
		Thread.sleep(1500);

		// 7. Ensure post no longer exists

		boolean isDeleted = wait.until(ExpectedConditions.invisibilityOfElementLocated(By
				.xpath("//div[contains(@class,'koreanNoTranslate') and normalize-space(text())='" + postText + "']")));
		Assert.assertTrue(isDeleted, "❌ Post was not deleted.");
		System.out.println("✅ Post deleted successfully.");

	}
	
	public void verifyMemberCountsMatch() {
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

	    // 1. From <div><p>13</p><p>Members</p></div>
	    WebElement numberOnly = wait.until(ExpectedConditions.presenceOfElementLocated(
	        By.xpath("//div[p[text()='Members']]/p[1]")));
	    int countFromNumberOnly = Integer.parseInt(numberOnly.getText().trim());

	    // 2. From <div class="font-semibold text-center">Members (13)</div>
	    WebElement countInText = driver.findElement(
	        By.xpath("//div[contains(@class,'font-semibold') and contains(text(),'Members (')]"));
	    int countFromTextInBracket = extractNumberFromText(countInText.getText());

	    // 3. Click the "Members" tab to reveal the third count location
	    WebElement membersTab = wait.until(ExpectedConditions.elementToBeClickable(By.id("Members")));
	    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", membersTab);
	    membersTab.click();
	    System.out.println("🟢 Clicked Members tab");
	    
	    WebElement thirdCountElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
	    	    By.xpath("//div[contains(text(),'Members (')]")));
	    	int countFromTabSection = extractNumberFromText(thirdCountElement.getText());


	    // ✅ Assertions
	    Assert.assertEquals(countFromTextInBracket, countFromNumberOnly, "❌ Mismatch: <p> vs 'Members (...)' (top)");
	    Assert.assertEquals(countFromTabSection, countFromNumberOnly, "❌ Mismatch: <p> vs 'Members (...)' in tab");

	    System.out.println("✅ All member counts match: " + countFromNumberOnly);
	}

	// Helper method
	private int extractNumberFromText(String text) {
	    String digits = text.replaceAll("[^0-9]", "");
	    return Integer.parseInt(digits);
	}
	
	
	
	public void verifyMemberCardVisible(String memberName) throws InterruptedException {
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

	    // 1. Click Members tab
	    WebElement membersTab = wait.until(ExpectedConditions.elementToBeClickable(By.id("Members")));
	    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", membersTab);
	    membersTab.click();
	    System.out.println("🟢 Clicked Members tab");

	    // 2. Wait and type in the search input
	    WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
	        By.xpath("//input[@placeholder='Search']")));
	    searchInput.clear();
	    searchInput.sendKeys(memberName);
	    System.out.println("🔍 Searched for member: " + memberName);

	    // 3. Wait for member card with matching name (case-insensitive)
	    String lowered = memberName.toLowerCase();
	    WebElement memberCard = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
	        "//div[contains(@class,'flex') and .//div[translate(normalize-space(text()), " +
	        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + lowered + "']]")));

	    // 4. Click the member card (anchor tag inside)
	    WebElement memberLink = memberCard.findElement(By.xpath(".//a[contains(@href,'/pages/lookup')]"));
	    String expectedHref = memberLink.getAttribute("href");
	    memberLink.click();
	    System.out.println("🔗 Clicked member card, navigating to profile...");

	    // 5. Wait for URL to update and verify
	    wait.until(ExpectedConditions.urlContains("/pages/lookup"));
	    String actualUrl = driver.getCurrentUrl();
	    Assert.assertTrue(actualUrl.equals(expectedHref) || actualUrl.contains(expectedHref),
	        "❌ Incorrect profile URL. Expected: " + expectedHref + " | Actual: " + actualUrl);
	    System.out.println("✅ Correct profile page opened for: " + memberName);
	    driver.navigate().back();
	}

	public void verifyJoinAndLeaveFunctionality() {
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));

	    // 1. Get current button state: Join or Leave
	    WebElement joinOrLeaveBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
	        By.xpath("//div[contains(@class,'cursor-pointer') and (normalize-space()='Join' or normalize-space()='Leave')]")
	    ));
	    String initialText = joinOrLeaveBtn.getText().trim();
	    System.out.println("🔍 Initial Button: " + initialText);

	    if (initialText.equalsIgnoreCase("Join")) {
	        // ➕ Join the club
	        joinOrLeaveBtn.click();
	        System.out.println("✅ Clicked 'Join'");
	        verifyToastContains("You have joined the hobby club");
	        waitForToastToDisappear();

	        // 🔁 Join → Leave
	        WebElement leaveBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
	            By.xpath("//div[normalize-space()='Leave' and contains(@class,'cursor-pointer')]")));
	        Assert.assertTrue(leaveBtn.isDisplayed(), "❌ Leave button not visible after joining.");
	        System.out.println("🔁 Join → Leave verified.");

	        // ➖ Leave again
	        leaveBtn.click();
	        System.out.println("✅ Clicked 'Leave'");
	        verifyToastContains("You have left the hobby club");
	        waitForToastToDisappear();

	        // 🔁 Leave → Join
	        WebElement joinBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
	            By.xpath("//div[normalize-space()='Join' and contains(@class,'cursor-pointer')]")));
	        Assert.assertTrue(joinBtn.isDisplayed(), "❌ Join button not visible after leaving.");
	        System.out.println("🔁 Leave → Join verified.");

	    } else if (initialText.equalsIgnoreCase("Leave")) {
	        // ➖ Leave first
	        joinOrLeaveBtn.click();
	        System.out.println("✅ Clicked 'Leave'");
	        verifyToastContains("You have left the hobby club");
	        waitForToastToDisappear();

	        // 🔁 Leave → Join
	        WebElement joinBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
	            By.xpath("//div[normalize-space()='Join' and contains(@class,'cursor-pointer')]")));
	        Assert.assertTrue(joinBtn.isDisplayed(), "❌ Join button not visible after leaving.");
	        System.out.println("🔁 Leave → Join verified.");

	        // ➕ Rejoin
	        joinBtn.click();
	        System.out.println("✅ Rejoined the club.");
	        verifyToastContains("You have joined the hobby club successfully");
	        waitForToastToDisappear();

	        // Final check: Leave visible again
	        WebElement leaveBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
	            By.xpath("//div[normalize-space()='Leave' and contains(@class,'cursor-pointer')]")));
	        Assert.assertTrue(leaveBtn.isDisplayed(), "❌ Leave button not visible after rejoining.");
	    } else {
	        Assert.fail("❌ Neither Join nor Leave button found.");
	    }
	}
	
	private void verifyToastContains(String expectedText) {
	    WebDriverWait toastWait = new WebDriverWait(driver, Duration.ofSeconds(10));
	    try {
	        WebElement toast = toastWait.until(ExpectedConditions.visibilityOfElementLocated(
	            By.xpath("//*[contains(@class,'Toastify__toast') and contains(text(),'" + expectedText + "')]")));
	        Assert.assertTrue(toast.isDisplayed(), "❌ Toast not shown: " + expectedText);
	        System.out.println("🔔 Toast verified: " + toast.getText());
	    } catch (TimeoutException e) {
	        Assert.fail("❌ Toast message not found: " + expectedText);
	    }
	}

	private void waitForToastToDisappear() {
	    try {
	        WebDriverWait toastGoneWait = new WebDriverWait(driver, Duration.ofSeconds(7));
	        toastGoneWait.until(ExpectedConditions.invisibilityOfElementLocated(
	            By.xpath("//div[contains(@class,'Toastify__toast') and contains(@class,'Toastify__toast--success')]")));
	        System.out.println("✅ Toast message disappeared.");
	    } catch (TimeoutException e) {
	        System.out.println("⚠️ Toast message did not disappear in time.");
	    }
	}



//	public void deleteAllBuzzPosts() throws InterruptedException {
//		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
//
//		while (true) {
//			List<WebElement> posts = driver.findElements(By.xpath("//div[contains(@class,'koreanNoTranslate')]"));
//
//			if (posts.isEmpty()) {
//				System.out.println("✅ No more posts to delete.");
//				break;
//			}
//
//			boolean deletedAny = false;
//
//			for (WebElement post : posts) {
//				String postText = post.getText().trim();
//				if (postText.isEmpty()) {
//					System.out.println("⚠️ Skipping empty post.");
//					continue;
//				}
//
//				System.out.println("🧹 Deleting post: " + postText);
//				deleteBuzzPost(postText);
//				deletedAny = true;
//				Thread.sleep(1000);
//				break; // Break so the DOM refreshes — avoid stale elements
//			}
//
//			if (!deletedAny) {
//				System.out.println("🚫 No deletable posts found.");
//				break;
//			}
//		}
//	}

}
