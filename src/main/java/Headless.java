import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import org.json.JSONException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.collect.ImmutableList;

public class Headless {

	private static final String CHROMEDRIVER_PATH = "/Users/sarnobat/github/chrome_headless/chromedriver_mac32";

//	 private static final String CHROMEDRIVER_PATH = "/home/sarnobat/github/chrome_headless/chromedriver_linux64";

	private static List<String> getGeneratedHtml(String binary, String url1) throws MalformedURLException, IOException {
		String url = url1.startsWith("http") ? url1 : "http://" + url1;
		// Don't use the chrome binaries that you browse the web with.
		System.setProperty("webdriver.chrome.driver", binary);
		System.setProperty("webdriver.chrome.logfile", "/dev/null");
		System.setProperty("webdriver.chrome.args", "disable-logging");
		System.setProperty("webdriver.chrome.silentOutput", "true");

		WebDriver driver = new FirefoxDriver();
		//Wait<WebDriver> wait = new WebDriverWait(driver, 30);

		// HtmlUnitDriver and FirefoxDriver didn't work. Thankfully
		// ChromeDriver does
		//WebDriver driver = new ChromeDriver();
		List<String> ret = ImmutableList.of();
		try {
			driver.get(url);
//			driver.get(url);
			// TODO: shame there isn't an input stream, then we wouldn't
			// have to store the whole page in memory
			try {
				// We need to let the dynamic content load.
				Thread.sleep(5000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String source = driver.getPageSource();
			System.out.println(source);
		} finally {
			driver.quit();
		}
		return ret;
	}

	public static void main(String[] args) throws URISyntaxException, JSONException, IOException {
		getGeneratedHtml(CHROMEDRIVER_PATH, "www.bbc.co.uk");
	}
}
