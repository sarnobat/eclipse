import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.google.common.collect.FluentIterable;

public class Jsoup2 {

	public static void main(String[] args) throws IOException {
		String domainRoot = "http://www.teamtalk.com/";
		writeToStdOut(domainRoot);
	}

	private static void writeToStdOut(String domainRoot) throws IOException {
		// No disk I/O takes place
		File input = new File("nonexistent");
		if (input.exists()) {
			throw new RuntimeException("Disk IO is slowing us down");
		}
		Document doc = Jsoup.parse(input, "UTF-8", domainRoot);
		Elements tags = doc.getElementsByTag("img");
		Set out = FluentIterable.from(tags).toSet();
		System.out.println(out);
	}

}
