import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.repackaged.com.google.common.base.Joiner;

public class MwkUsage {

	public static void main(String[] args) throws IOException {
		if (!isHeading("== 1 ==")) {
			throw new RuntimeException("isHeading not working");
		}
		if (!getHeading("=== ===").equals("BLANK")) {
			throw new RuntimeException("isHeading not working");
		}
		if (!getHeading("===  ===").equals("BLANK")) {
			throw new RuntimeException("isHeading not working");
		}
		MeasurableStack currentWorkingCategory = new MeasurableStack();
		currentWorkingCategory.push("/");
		Map<String, Integer> counts = new HashMap<String, Integer>();
		counts.put("/", 0);
		read: {
			InputStreamReader isr = new InputStreamReader(System.in);
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			while (line != null) {
				if (isHeading(line)) {
					// System.out.println("MwkUsage.main() HEADING: " + line);
					String categoryName = getHeading(line);
					int headingLevel = getLevel(line);
					if (headingLevel == currentWorkingCategory.depth()) {
						// Remove the current
						currentWorkingCategory.pop();
						// Add this subcategory to the current working category
						// path
						currentWorkingCategory.push(categoryName);
					} else if (headingLevel > currentWorkingCategory.depth()) {
						// Add this subcategory to the current working category
						// path
						currentWorkingCategory.push(categoryName);
					} else if (headingLevel < currentWorkingCategory.depth()) {
						// could be multiple levels above, pop current working
						// category until it is above the current heading level
						while (currentWorkingCategory.depth() >= headingLevel) {
							currentWorkingCategory.pop();
						}
						// Add this subcategory to the current working category
						// path
						currentWorkingCategory.push(categoryName);
					}

					if (!counts.containsKey(currentWorkingCategory.getFullPath())) {
						counts.put(currentWorkingCategory.getFullPath(), 0);
					}
					// System.out.println("MwkUsage.main() " +
					// currentWorkingCategory.getFullPath());
				} else {
					// System.out.println("MwkUsage.main() : " + line);
					// Increment the current working category path's count, AND
					// each of its parents counts all the way up to the root
					String categoryName = currentWorkingCategory.getFullPath();
					Path p = Paths.get(categoryName);
					while (p.toString().length() > 1) {
						Integer integer = counts.get(categoryName);
						if (integer == null) {
							integer = 0;
						}
						counts.put(categoryName, ++integer);
						p = p.getParent();
						if (p == null) {
							break;
						} else {
							categoryName = p.toString();
						}
					}
				}
				line = br.readLine();
			}
		}
		// Now print the summary
		report: {
			usage(counts);
			//find(counts);
		}
	}

	@SuppressWarnings("unused")
	private static void usage(Map<String, Integer> counts) {
		for (String path : counts.keySet()) {
			int count = counts.get(path);
			System.out.println(count + "\t" + path);
		}
	}

	@SuppressWarnings("unused")
	private static void find(Map<String, Integer> counts) {
		for (String path : counts.keySet()) {
			System.out.println(path);
		}
	}

	private static String getHeading(String line) {
		String regex = "^=+([^=]*)=+";
		Pattern p = Pattern.compile(regex);
		Matcher i = p.matcher(line);
		i.find();
		String replaceAll = i.group(1).trim();
		if (replaceAll.length() > 0) {
			return replaceAll;
		} else {
			return "BLANK";
		}
	}

	private static int getLevel(String line) {
		String trimmed = line.trim();
		int ret = 0;
		int i = 0;
		while (trimmed.charAt(i) == '=') {
			++i;
			++ret;
		}
		return ret;
	}

	private static boolean isHeading(String line) {
		if (line == null) {
			return false;
		}
		return line.matches("^=+\\s+.*");
	}

	private static class MeasurableStack {
		List<String> elements = new LinkedList<String>();
		Stack<String> stack = new Stack<String>();

		public void push(String string) {
			stack.push(string);
			elements.add(string);
		}

		public String getFullPath() {
			// System.out.println("MwkUsage.MeasurableStack.getFullPath() " +
			// elements);
			return "/" + Joiner.on("/").join(elements);
		}

		public String pop() {
			elements.remove(elements.size() - 1);
			return stack.pop();
		}

		public int depth() {
			return elements.size();
		}

	}
}
