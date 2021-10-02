import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;

import com.google.api.client.repackaged.com.google.common.base.Strings;

public class BulletsToHtml {


	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String lineOrig = null;
		int previousLineDepth = 0;
		while ((lineOrig = reader.readLine()) != null) {
			// do something with every line, one at a time
			final int currentLineLevel = getBulletIndentationLevel(lineOrig);
			final int previousLineLevel = previousLineDepth;
			String line = lineOrig.substring(currentLineLevel);
			System.err.println("[" + previousLineLevel + " --> " + currentLineLevel + "]");

			// Close the sublist if the previous line was the last in the
			// sublist
			String listClosingPrefix = getListClosingPrefix(previousLineLevel, currentLineLevel);
			if (listClosingPrefix.trim().length() > 0) {
				System.out.println(listClosingPrefix);
			}

			String padding = getPadding(currentLineLevel); 

			String openList = getSublistOpen(currentLineLevel, previousLineLevel, padding);
			if (openList.trim().length() > 0) {
				System.out.println(openList);
			}

			String currentLine = getCurrentLine(currentLineLevel, previousLineLevel, line, padding);
			if (currentLine.trim().length() > 0) {
				System.out.println(currentLine);
			}
			previousLineDepth = currentLineLevel;
		}
		System.out.println(getListClosingPrefix(previousLineDepth, 0));
	}

	private static String getCurrentLine(final int currentLineLevel, final int previousLineLevel,
			String line, String padding) {
		String currentLine;
//		if (previousLineLevel < currentLineLevel && previousLineLevel == 0) {
//			currentLine = padding + "<li>" + line + "</li>";
//		} else
		padding = padding + "  ";
		if (previousLineLevel < currentLineLevel && previousLineLevel >= 0) {
			currentLine = padding + "<li>" + line + "</li>";
		} else {
		if (previousLineLevel >= currentLineLevel && currentLineLevel > 0) {
			currentLine = padding + "<li>" + line + "</li>";
		} else 
//		if (previousLineLevel < currentLineLevel && previousLineLevel > 0) {
//			currentLine = padding + "<li>" + line + "</li>";
//		} else {
			currentLine = line;
		}
		return currentLine;
	}

	private static String getSublistOpen(final int currentLineLevel, final int previousLineLevel,
			String padding) {
		String openList = "";
		if (previousLineLevel < currentLineLevel) {
			openList = padding + "<ul>";
		}
		return openList;
	}

	private static String getPadding(final int currentLineLevel) {
		String padding;
		if (currentLineLevel > 0) {
			padding = Strings.repeat("  ", currentLineLevel - 1);
		} else {
			padding = "";
		}
		return padding;
	}

	private static String getListClosingPrefix(int previousLineLevel, int currentLineLevel) {
		String ret = "";
		while(previousLineLevel > currentLineLevel) {
			ret += StringUtils.repeat("  ", previousLineLevel - 1) + "</ul>";
			--previousLineLevel;
			if (previousLineLevel > currentLineLevel) {
				ret += "\n";
			}
		}
		return ret;
	}

	private static int getBulletIndentationLevel(String line) {
		int count = 0;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '*') {
				count++;
			} else {
				break;
			}
		}
		return count;
	}
}
