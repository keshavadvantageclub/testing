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
			WebElement audioWrapper = wait.until(ExpectedConditions.visibilityOfElementLocated(
			    By.xpath("//div[text()='" + expectedCaption + "']/ancestor::div[contains(@class,'Feed_centerContainer')]//div[contains(@class,'CustomAudioPlayer_audioPlayerComponentWrapper')]")
			));
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

	        WebElement videoElement = shortWait.until(ExpectedConditions.visibilityOfElementLocated(
	        	    By.xpath("//div[text()='" + expectedCaption + "']/ancestor::div[contains(@class,'Feed_centerContainer')]//video")
	        	));
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
	        WebElement textArea = wait.until(ExpectedConditions.visibilityOfElementLocated(
	            By.cssSelector("textarea.PostStatus_textArea__ySn55")));
	        textArea.sendKeys(caption);
	        System.out.println("✅ Caption entered: " + caption);

	        // Step 2: Upload video
	        WebElement fileInput = driver.findElement(By.cssSelector("input[type='file'][accept*='video']"));
	        fileInput.sendKeys(new File(videoPath).getAbsolutePath());
	        System.out.println("✅ Video uploaded: " + videoPath);

	        // Step 3: Wait for video to attach (small buffer)
	        Thread.sleep(2000); // optional, can be improved by checking media preview element

	        // Step 4: Click post button safely
	        WebElement postBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(
	            By.xpath("//button[text()='Post']")));

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






}
