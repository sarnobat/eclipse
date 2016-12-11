import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MwkReader {

	private MwkReader() {

	}

	public static JSONArray toJson(String iFilePath) throws JSONException,
			IOException {
		List<String> _lines;
		File f = new File(iFilePath);
		if (!f.exists()) {
			throw new RuntimeException();
		}
		_lines = FileUtils.readLines(f);
		int level = 1;
		JSONArray o = new JSONArray();
		addSectionsAtLevel(level, o, 0, _lines.size(), _lines);
		return o;
	}

	private static void addSectionsAtLevel(int level,
			JSONArray oSubObjectToFill, int startLineIdx, int endLineIdx,
			List<String> allLines) throws JSONException {
		ArrayList<String> al = new ArrayList<String>(allLines);
		List<JSONObject> objectsAtLevel = getObjectsAtLevel(level,
				al.subList(startLineIdx, endLineIdx));
		for (JSONObject o : objectsAtLevel) {
			oSubObjectToFill.put(o);
		}

	}

	private static List<JSONObject> getObjectsAtLevel(int level,
			List<String> subList) throws JSONException {
		String startingPattern = "^" + StringUtils.repeat('=', level) + "\\s.*";
		List<JSONObject> ret = new LinkedList<JSONObject>();
		for (int start = 0; start < subList.size(); start++) {
			String line = subList.get(start);
			if (!"= =".matches(startingPattern)) {
				throw new RuntimeException("wrong logic");
			}
			if (line.matches(startingPattern)) {
				int j = start + 1;
				while (j < subList.size()
						&& !subList.get(j).matches(startingPattern)) {
					++j;
				}
				// find ending line
				JSONObject js = convertStringRangeToJSONObject(
						subList.subList(start, j), level + 1);
				ret.add(js);
				start = j - 1;
			}

		}
		return ret;
	}

	private static JSONObject convertStringRangeToJSONObject(
			List<String> subList, int levelBelow) throws JSONException {
		JSONObject ret = new JSONObject();
		int start = 0;
		String heading = subList.get(start++) + "\n";
		if (heading.equals("")) {
			throw new RuntimeException("Incorrect assumption.");
		}
		ret.put("heading", heading);
		// first get free text
		String startingPattern = "^" + StringUtils.repeat('=', levelBelow)
				+ "\\s.*";
		StringBuffer freeTextSb = new StringBuffer();
		JSONArray subsections = new JSONArray();
		for (; start < subList.size(); start++) {
			String str = subList.get(start);
			if (str.matches(startingPattern)) {
				break;
			}
			freeTextSb.append(str);
			freeTextSb.append("\n");

		}
		for (; start < subList.size(); start++) {

			if (!subList.get(start).startsWith("=")) {
				throw new RuntimeException("The first line should be a heading");
			}
			int end = start + 1;
			while (end < subList.size()
					&& !subList.get(end).matches(startingPattern)) {
				++end;
			}
			int nextStartOrEnd = end;
			JSONObject innerObj = convertStringRangeToJSONObject(
					subList.subList(start, nextStartOrEnd), levelBelow + 1);
			subsections.put(innerObj);
			start = end - 1;
			if (end == subList.size()) {
				break;
			}
			String endingLine = subList.get(end);
			if (endingLine != null) {
				if (!endingLine.matches(startingPattern)) {
					throw new RuntimeException(
							"You can't have free text after the subsections have begun");
				}
			}

		}
		String string = freeTextSb.toString();
		ret.put("freetext", string);
		ret.put("subsections", subsections);
		ret.put("id", DigestUtils.md5Hex(heading + string));

		return ret;
	}

	public static StringBuffer asString(JSONArray topLevelArray) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < topLevelArray.length(); i++) {
			JSONObject subtree = topLevelArray.getJSONObject(i);
			sb.append(subtree.getString("heading"));
			sb.append(subtree.getString("freetext"));
			JSONArray subsections = subtree.getJSONArray("subsections");
			StringBuffer sb1 = asString(subsections);
			sb.append(sb1);
		}
		return sb;
	}
}
