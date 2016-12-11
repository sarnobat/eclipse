import java.io.IOException;
import java.net.MalformedURLException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class HtmlUnit {
	public static void main(String[] args) throws FailingHttpStatusCodeException,
			MalformedURLException, IOException {
//		WebClient abc = new WebClient();
//		HtmlPage page = abc.getPage("http://www.raspberrypi.org/forums/viewtopic.php?f=35&t=15390");
//		String a = page.asXml();
//		String title = page.getTitleText();
//		System.out.print(a);
//		System.out.println(title);

	    final WebClient webClient = new WebClient();
	    final HtmlPage page = webClient.getPage("http://www.teamtalk.com");
	}
}
