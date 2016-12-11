import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

public class SeleniumWebdriver {

	public static void main(String[] args) throws IOException {
		String biggestImage = getBiggestImage("http://www.teamtalk.com/liverpool/9922711/Roberto-Firmino-settling-in-quickly-at-Liverpool");
//		String biggestImage = getBiggestImage("http://www.denimblog.com/2015/07/stella-maxwell-in-rag-bone/");
		System.out.println(biggestImage);
	}

	static String getBiggestImage(String url) throws MalformedURLException, IOException {
		List<String> imagesDescendingSize = getImagesDescendingSize(url);
		String biggestImage = imagesDescendingSize.get(0);
		return biggestImage;
	}

	private static List<String> getImagesDescendingSize(String url) throws MalformedURLException,
			IOException {
		String base = getBaseUrl(url);
		// Don't use the chrome binaries that you browse the web with.
		System.setProperty("webdriver.chrome.driver",
//				"/home/sarnobat/trash/chromedriver");
	"/sarnobat.garagebandbroken/trash/chromedriver");

		WebDriver driver = new ChromeDriver();
		List<String> ret = ImmutableList.of();
		try {
			driver.get(url);
			// TODO: shame there isn't an input stream, then we wouldn't have to
			// store the whole page in memory
			String source = driver.getPageSource();
			// System.out.println(source);
			List<String> out = getAllTags(base, source);
			Map<Integer, String> imageSizes = getImageSizes(out);
			// System.out.println(Joiner.on("\n").join(sortedImages));
			ret = sortByKey(imageSizes);
		} finally {
			driver.quit();
		}
		return ret;
	}

	private static List<String> sortByKey(Map<Integer, String> imageSizes) {
		ImmutableList.Builder<String> builder = ImmutableList.builder();
		for (Integer size : FluentIterable.from(imageSizes.keySet()).toSortedList(
				Ordering.natural().reverse())) {
			String url = imageSizes.get(size);
			builder.add(url);
			System.out.println(size + "\t" + url);
		}
		return builder.build();
	}

	private static Map<Integer, String> getImageSizes(List<String> out) {
		ImmutableMap.Builder<Integer, String> builder = ImmutableMap.builder();
		Set<Integer> taken = new HashSet<Integer>();
		for (String imgSrc : out) {
			int size = getByteSize(imgSrc);
			if (!taken.contains(size)) {
				builder.put(size, imgSrc);
				taken.add(size);
			}
		}
		return builder.build();
	}

	private static int getByteSize(String absUrl) {
		URL url;
		try {
			url = new URL(absUrl);
			int contentLength = url.openConnection().getContentLength();
			// System.out.println(contentLength + "\t"+ absUrl);
			return contentLength;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private static String getBaseUrl(String url1) throws MalformedURLException {
		URL url = new URL(url1);
		String file = url.getFile();
		String path;
		if (file.length() == 0) {
			path = url1;
		} else {
			path = url.getFile().substring(0, file.lastIndexOf('/'));
		}
		return url.getProtocol() + "://" + url.getHost() + path;
	}

	private static List<String> getAllTags(String domainRoot, String source) throws IOException {
		Document doc = Jsoup.parse(IOUtils.toInputStream(source), "UTF-8", domainRoot);
		System.out.println(doc.outerHtml());
		Elements tagsa = doc.getElementsByTag("a");
		Elements tagsp = doc.getElementsByTag("p");
		Elements tags = doc.getElementsByTag("img");
		return FluentIterable.<Element> from(tags).transform(IMG_TO_SOURCE).toList();
	}

	private static final Function<Element, String> IMG_TO_SOURCE = new Function<Element, String>() {
		@Override
		public String apply(Element e) {
			return e.absUrl("src");
		}
	};
}
