import static com.ui4j.api.browser.BrowserFactory.getWebKit;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.ui4j.api.browser.Page;
import com.ui4j.api.dom.Element;

public class UI4J {

	public static void main(String[] args) {
		String url = "http://www.teamtalk.com";
		getImages(url);
	}

	private static void getImages(String url) {
		List c = FluentIterable.from(getImageElements(url)).filter(Predicates.notNull())
				.toSortedList(IMG_SIZE);
		List<String> imageHtml = FluentIterable.from(c).transform(GET_SRC)
				.transform(new Absolutify(url))
				// .transform(IMG_TAG)
				.toList();
	}

	private static Comparator<Element> IMG_SIZE = new Comparator<Element>() {
		@Override
		public int compare(Element e1, Element e2) {
			checkElem(e1);
			checkElem(e2);
			Optional<String> height1 = e1.getAttribute("height");
			Optional<String> width1 = e1.getAttribute("width");
			Optional<String> height2 = e2.getAttribute("height");
			Optional<String> width2 = e2.getAttribute("width");
			System.out.println(height1.get());
			System.out.println(width1.get());
			System.out.println(height2.get());
			System.out.println(width2.get());
			System.out.println();
			// PICKUP
			return 0;
		}

		private void checkElem(Element e1) {
			if (!isType(e1, "img")) {
//				throw new RuntimeException("Not an image: " + e1);
			}
		}

		private boolean isType(Element e, String tagName) {
			System.out.println(e);
			if (e == null) {
				System.out.println();
			}
			if (e.getTagName() == null) {
				System.out.println(e);
				return false;
			}
			return e.getTagName().equalsIgnoreCase(tagName);
		}
	};

	private static List<Element> getImageElements(String url) {
		Page page = getWebKit().navigate(url);
		List<Element> queryAll = page.getDocument().queryAll("img");
		page.close();
		getWebKit().shutdown();
		for (Iterator iterator = queryAll.iterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			System.out.println(element.getTagName() + " " + element.getAttribute("src"));
		}
		System.out.println("Done with network I/O");
		return ImmutableList.copyOf(queryAll);
	}

	private static final Function<String, String> IMG_TAG = new Function<String, String>() {

		@Override
		public String apply(String input) {
			return "<img src='" + input + "'>";
		}

	};

	private static final Function<Element, String> GET_SRC = new Function<Element, String>() {
		@Override
		@Nullable
		public String apply(Element elem) {
			String src = elem.getAttribute("src").get();
			return src;
		}
	};

	private static class Absolutify implements Function<String, String> {
		private final String url;

		Absolutify(String url) {
			this.url = url;
		}

		@Override
		public String apply(String input) {
			String x;
			if (input.startsWith("http")) {
				x = input;
			} else {
				x = url + "/" + input;
			}
			return x;
		}
	}
}