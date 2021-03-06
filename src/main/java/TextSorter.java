import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CopyOnWriteArrayList;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

// Insertion-sort into "sorted" is not implemented
public class TextSorter {
	public static void main(String[] args) throws URISyntaxException {
		try {
			JdkHttpServerFactory.createHttpServer(new URI("http://localhost:4455/"),
					new ResourceConfig(HelloWorldResource.class));
		} catch (Exception e) {
			System.out.println("Already running");
		}
	}

	@Path("helloworld")
	public static class HelloWorldResource { // Must be public

		@GET
		@Path("json")
		@Produces("application/json")
		public Response read(@QueryParam("filePath") String iFilePath) throws JSONException,
				IOException {

			System.out.println("TextSorter.HelloWorldResource.read() - begin");
			createSortedCopyOfFile(iFilePath);

			try {
				JSONObject mwkFileAsJson = new JSONObject();
				File mwkFile = new File(iFilePath);
				if (!mwkFile.exists()) {
					throw new RuntimeException();
				}
				JSONArray o = toJson(iFilePath);
				mwkFileAsJson.put("tree", o);
				System.out.println("TextSorter.HelloWorldResource.read() - end");
				return Response.ok().header("Access-Control-Allow-Origin", "*")
						.entity(mwkFileAsJson.toString()).type("application/json").build();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		private void createSortedCopyOfFile(String iFilePath) {
			System.out.println("TextSorter.HelloWorldResource.createSortedCopyOfFile() - begin");
			try {
				Defragmenter.defragmentFile(iFilePath);
				System.out.println("TextSorter.HelloWorldResource.createSortedCopyOfFile() - sort successful");
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
		}

		@SuppressWarnings("unused")
		@POST
		@Path("persist")
		@Deprecated
		// I don't think we're using this
		public Response persist(final String body) throws JSONException, IOException,
				URISyntaxException {
			System.out.println("persist() - begin");
			System.out.println(body);
			// Save the changes to the file
			save: {
				List<NameValuePair> params = URLEncodedUtils.parse(new URI("http://www.fake.com/?"
						+ body), "UTF-8");
				Map<String, String> m = new HashMap<String, String>();
				for (NameValuePair param : params) {
					m.put(param.getName(), URLDecoder.decode(param.getValue(), "UTF-8"));
				}
				FileUtils.write(new File(m.get("filePath")), m.get("newFileContents"));
				System.out.println("persist() - write successful");
			}
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(new JSONObject().toString()).type("application/json").build();
		}

		@POST
		@Path("move")
		public Response move(@QueryParam("filePath") String iFilePath,
				@QueryParam("id") final String iIdOfObjectToMove,
				@QueryParam("destId") final String iIdOfLocationToMoveTo) throws JSONException,
				IOException, URISyntaxException {

			try {
				JSONArray topLevelArray = toJson(iFilePath);

				JSONObject destination = findSnippetById(iIdOfLocationToMoveTo, topLevelArray);
				if (destination == null) {
					throw new RuntimeException("couldn't find " + iIdOfLocationToMoveTo);
				}
				JSONObject snippetOriginalParent = findParentOfSnippetById(iIdOfObjectToMove,
						topLevelArray);
				if (snippetOriginalParent == null) {
					throw new RuntimeException("couldn't find " + iIdOfObjectToMove);
				}
				JSONObject snippetToMove = removeObject(iIdOfObjectToMove, topLevelArray,
						snippetOriginalParent);
				if (snippetToMove == null) {
					throw new RuntimeException("Couldn't find snippet " + iIdOfObjectToMove);
				}
				if (!destination.getString("id").equals(iIdOfLocationToMoveTo)) {
					System.out.println("Wrong location");
					throw new RuntimeException("Wrong destination");
				}
				System.out.println("move() - Snippet to move: " + snippetToMove);
				destination.getJSONArray("subsections").put(snippetToMove);

				try {
					String string = asString(topLevelArray).toString();
					FileUtils.writeStringToFile(new File(iFilePath), string);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("TextSorter.HelloWorldResource.move() - success");
				return Response.ok().header("Access-Control-Allow-Origin", "*")
						.entity(topLevelArray.toString()).type("application/json").build();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;

		}

		// TODO: why do we need the top level array?
		private JSONObject removeObject(final String iIdOfObjectToRemove, JSONArray topLevelArray,
				JSONObject snippetOriginalParent) {
			JSONObject rDesiredSnippet = null;
			if (snippetOriginalParent == null) {
				throw new IllegalArgumentException();
			}
			JSONArray subsectionsOfParent = snippetOriginalParent.getJSONArray("subsections");
			for (int i = 0; i < subsectionsOfParent.length(); i++) {
				JSONObject aSubsection = subsectionsOfParent.getJSONObject(i);
				if (aSubsection.getString("id").equals(iIdOfObjectToRemove)) {
					// System.out.println("removeObject(): aSubsection: " +
					// aSubsection);
					rDesiredSnippet = (JSONObject) subsectionsOfParent.remove(i);
					// System.out.println("removeObject(): rDesiredSnippet: " +
					// rDesiredSnippet);
					rDesiredSnippet = aSubsection;
					// String r = rDesiredSnippet.toString();
					// System.out.println("removeObject(): subsectionsOfParent: "
					// + subsectionsOfParent);Too long
					// System.out.println("removeObject(): r: " + r);
					break;
				}
			}
			if (rDesiredSnippet == null) {
				throw new RuntimeException("Did not find snippet in parent");
			}
			return rDesiredSnippet;
		}

		@Deprecated
		private JSONObject notWorking(final String iIdOfObjectToRemove, JSONArray topLevelArray,
				JSONObject snippetOriginalParent) {
			System.out.println("snippetOriginalParent - " + snippetOriginalParent.getString("id"));
			System.out.println("IdOfObjectToRemove - " + iIdOfObjectToRemove);
			System.out.println();
			JSONObject rDesiredSnippet = null;
			JSONArray subsectionsArray = (JSONArray) snippetOriginalParent.get("subsections");
			for (int i = 0; i < topLevelArray.length(); i++) {
				if (rDesiredSnippet != null) {
					throw new RuntimeException(
							"Bug. We already found the snippet. We don't want to keep searching");
				}
				if (topLevelArray.get(i) != null) {
					for (int j = 0; j < subsectionsArray.length(); j++) {
						if (rDesiredSnippet != null) {
							throw new RuntimeException(
									"Bug. We already found the snippet. We don't want to keep searching");
						}
						JSONObject subtreeRootJsonObject = subsectionsArray.getJSONObject(j);
						JSONObject aParentSnippet = findSnippetById(iIdOfObjectToRemove,
								subtreeRootJsonObject);
						if (aParentSnippet != null) {

							rDesiredSnippet = aParentSnippet;
							JSONArray allParentSubsections = snippetOriginalParent
									.getJSONArray("subsections");
							boolean foundParent = false;
							for (int k = 0; i < allParentSubsections.length(); i++) {
								System.out.println(allParentSubsections.getJSONObject(k).getString(
										"id"));
								if (iIdOfObjectToRemove.equals(allParentSubsections
										.getJSONObject(k).getString("id"))) {
									allParentSubsections.remove(k);
									foundParent = true;
									break;
								}
							}
							if (foundParent) {
								break;
							} else {
								throw new RuntimeException(
										"Found parent but did not remove snippet.");
							}
						} else {
							throw new RuntimeException("Parent Snippet not found");
						}
					}
					break;
				}
			}
			return rDesiredSnippet;
		}

		private JSONObject findParentOfSnippetById(String iIdOfObjectToMove, JSONArray a) {
			for (int i = 0; i < a.length(); i++) {
				JSONObject jsonObject = a.getJSONObject(i);
				JSONObject p = findParentOfSnippetById(iIdOfObjectToMove, jsonObject);
				if (p != null) {
					return p;
				}
			}
			throw new RuntimeException("Couldn't find parent of " + iIdOfObjectToMove);
		}

		private JSONObject findParentOfSnippetById(String iIdOfObjectToMove, JSONObject jsonObject) {
			JSONArray a = jsonObject.getJSONArray("subsections");
			for (int i = 0; i < a.length(); i++) {
				if (iIdOfObjectToMove.equals(a.getJSONObject(i).getString("id"))) {
					return jsonObject;
				} else {
					JSONObject parentCandidate = findParentOfSnippetById(iIdOfObjectToMove,
							a.getJSONObject(i));
					if (parentCandidate != null) {
						return parentCandidate;
					}
				}
				// JSONObject jsonObject2 = a.getJSONObject(i);
				// JSONObject o = findSnippetById(iIdOfObjectToMove,
				// jsonObject2);
				// if (o != null) {
				// return jsonObject2;
				// }
			}
			return null;
		}

		private JSONObject findSnippetById(String iIdOfObjectToMove, JSONArray a) {
			for (int i = 0; i < a.length(); i++) {
				JSONObject o = findSnippetById(iIdOfObjectToMove, a.getJSONObject(i));
				if (o != null) {
					return o;
				}
			}
			return null;
		}

		private JSONObject findSnippetById(String iIdOfObjectToMove, JSONObject jsonObject) {
			String string2 = jsonObject.getString("id");
			if (iIdOfObjectToMove.equals(string2)) {
				return jsonObject;
			}
			JSONArray arr = (JSONArray) jsonObject.getJSONArray("subsections");

			for (int j = 0; j < arr.length(); j++) {
				JSONObject jsonObject2 = arr.getJSONObject(j);
				JSONObject o = findSnippetById(iIdOfObjectToMove, jsonObject2);
				if (o != null) {
					return o;
				}
			}
			return null;
		}
	}

	public static JSONArray toJson(String iFilePath) throws JSONException, IOException {
		System.out.println("TextSorter.toJson() - begin");
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

	private static void addSectionsAtLevel(int level, JSONArray oSubObjectToFill, int startLineIdx,
			int endLineIdx, List<String> allLines) throws JSONException {
		ArrayList<String> al = new ArrayList<String>(allLines);
		List<JSONObject> objectsAtLevel = getObjectsAtLevel(level,
				al.subList(startLineIdx, endLineIdx));
		for (JSONObject o : objectsAtLevel) {
			oSubObjectToFill.put(o);
		}

	}

	private static List<JSONObject> getObjectsAtLevel(int level, List<String> subList)
			throws JSONException {
		String startingPattern = "^" + StringUtils.repeat('=', level) + "\\s.*";
		List<JSONObject> ret = new LinkedList<JSONObject>();
		for (int start = 0; start < subList.size(); start++) {
			String line = subList.get(start);
			if (!"= =".matches(startingPattern)) {
				throw new RuntimeException("wrong logic");
			}
			if (line.matches(startingPattern)) {
				int j = start + 1;
				while (j < subList.size() && !subList.get(j).matches(startingPattern)) {
					++j;
				}
				// find ending line
				JSONObject js = convertStringRangeToJSONObject(subList.subList(start, j), level + 1);
				ret.add(js);
				start = j - 1;
			}

		}
		return ret;
	}

	private static JSONObject convertStringRangeToJSONObject(List<String> subList, int levelBelow)
			throws JSONException {
		JSONObject ret = new JSONObject();
		int start = 0;
		String heading = subList.get(start++) + "\n";
		if (heading.equals("")) {
			throw new RuntimeException("Incorrect assumption.");
		}
		ret.put("heading", heading);
		// first get free text
		String equals = StringUtils.repeat('=', levelBelow);
		String startingPattern = "^" + equals + "\\s.*";
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
			while (end < subList.size() && !subList.get(end).matches(startingPattern)) {
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
		// It's difficult if you make this recursive because the destination's
		// ID will change
		// if you've added content to it since loading the file for display on
		// the client
		// You'd have to refresh each time
		// ret.put("id", DigestUtils.md5Hex(heading + string +
		// subsections.toString()));
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

	private static class Defragmenter {

		public static final String PUBLISHING = "publishing";

		/**
		 * This only writes it to stdout, it doesn't modify the file.
		 * Hmmmm, this comment looks wrong.
		 */
		public static void defragmentFile(String fileToOrganizePath) {
			System.out.println("TextSorter.Defragmenter.defragmentFile() - begin: " + fileToOrganizePath);
			List<String> lines = TextSorterControllerUtils.readFile(fileToOrganizePath);
			MyTreeNode treeRootNode = TreeCreator.createTreeFromLines(lines);
			MyTreeNode.validateTotalNodeCount(treeRootNode);
			MyTreeNode.dumpTreeToFileAndVerify(treeRootNode, fileToOrganizePath,
					Utils.countNonHeadingLines(lines), "-sorted.mwk", "coagulate");
			MyTreeNode.resetValidationStats();
			System.out.println("TextSorter.Defragmenter.defragmentFile() - end");
		}

	}

	private static class Utils {
		public static int determineHeadingLevel(String headingLine) {
			int headingLevel = 0;
			for (int i = 0; i < headingLine.length(); i++) {
				char c = headingLine.charAt(i);
				if (c == '=') {
					++headingLevel;
				} else {
					break;
				}
			}
			return headingLevel;
		}

		public static int countNonHeadingLines(List<String> readFile) {
			int count = 0;
			for (String line : readFile) {
				if (line.trim().length() > 0 && !line.startsWith("=")) {
					++count;
				}
			}
			return count;
		}

		public static String getDeragmentedFilePath(String fragmentedFilePath, String string) {
			return fragmentedFilePath.replace(".mwk", string);
		}
	}

	private static class TextSorterControllerUtils {
		public static List<String> readFile(String inputFilePath) {
			System.out
					.println("TextSorter.TextSorterControllerUtils.readFile() - " + inputFilePath);
			List<String> theLines = null;
			try {
				theLines = FileUtils.readLines(new File(inputFilePath));

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
//			System.out.println("TextSorter.TextSorterControllerUtils.readFile() - " + theLines);
			// To ensure we don't lose text before the first heading
			if (!theLines.get(0).startsWith("=")) {
				theLines.add(0, "= =");
			}
			return new CopyOnWriteArrayList<String>(theLines);
		}
	}

	private static class TreeCreator {
		@SuppressWarnings("unchecked")
		public static MyTreeNode createTreeFromLines(List<String> lines) {
			System.out.println("TextSorter.TreeCreator.createTreeFromLines() - begin");
			List<Snippet> theSnippetList = getSnippetList(lines);
			Stack<MyTreeNode> snippetTreePath = new Stack<MyTreeNode>();
			MyTreeNode.totalNodeCount = 0;
			System.out.println("TextSorter.TreeCreator.createTreeFromLines() - before loop");
			for (Snippet aSnippet : theSnippetList) {
				System.out.print(".");
				// Find the parent node
				MyTreeNode aParentNode = null;
				findParentForCurrentSnippet: {
					// rewind up the path until we find a snippet 1 higher
					// than the current snippet
					MyTreeNode highestNodeInExistingTree = popStackToHighest((Stack<MyTreeNode>) snippetTreePath
							.clone());
					popStackToParent(aSnippet, snippetTreePath);

					if (!snippetTreePath.isEmpty()) {
						aParentNode = snippetTreePath.peek();
					}
					if (aParentNode == null) {
						// push a virtual node
						int parentHeadingLevel = aSnippet.getLevelNumber() - 1;
						aParentNode = VirtualNodeCreator.createVirtualNode(parentHeadingLevel,
								(Stack<MyTreeNode>) snippetTreePath.clone(),
								highestNodeInExistingTree);
						snippetTreePath.push(aParentNode);
					}
				}
				// Add this snippet as a child of parentNode
				snippetTreePath.push(new MyTreeNode(aSnippet, aParentNode));
				// MyTreeNode.dumpTree(aParentNode);
				MyTreeNode.validateCount(aParentNode);
			}
			System.out.println("");
			System.out.println("TextSorter.TreeCreator.createTreeFromLines() - after loop");
			// Pop the stack and return the root
			MyTreeNode root = null;
			while (!snippetTreePath.isEmpty()) {
				root = snippetTreePath.pop();
			}
			if (root == null) {
				throw new RuntimeException("Developer Error");
			}
			return root;
		}

		private static void popStackToParent(Snippet snippet, Stack<MyTreeNode> snippetTreePath) {
			if (!snippetTreePath.isEmpty()) {
				while (!snippetTreePath.isEmpty()
						&& snippetTreePath.peek().level() >= snippet.getLevelNumber()) {
					snippetTreePath.pop();
				}
			}
		}

		private static List<Snippet> getSnippetList(List<String> lines) {
			System.out.println("TextSorter.TreeCreator.getSnippetList() - begin");
			List<Snippet> snippets = new LinkedList<Snippet>();
			int firstHeadingLine = 0;
			while (!isHeadingLine(lines.get(firstHeadingLine))) {
				++firstHeadingLine;
			}
			int nextSnippetStart = 0;
			for (int start = firstHeadingLine; nextSnippetStart < lines.size() - 1; start = Math
					.max(start + 1, nextSnippetStart)) {
				nextSnippetStart = findNextHeadingLineAfter(start, lines);
				if (nextSnippetStart < lines.size() - 1) {
					validate3(lines, nextSnippetStart, start);
				} else {
					++nextSnippetStart;
				}
				validate2(lines, start);
				snippets.add(new Snippet(start, nextSnippetStart, lines));
			}
			return snippets;
		}

		private static void validate3(List<String> lines, int nextSnippetStart, int start) {
			if (!lines.get(nextSnippetStart).matches("^=+.*=+")) {
				throw new RuntimeException("Developer error: [" + start + "] = "
						+ lines.get(nextSnippetStart));
			}
		}

		private static void validate2(List<String> lines, int start) {
			if (!lines.get(start).matches("^=+.*=+")) {
				throw new RuntimeException("Developer error: start is [" + start + "] = "
						+ lines.get(start));
			}
		}

		private static int findNextHeadingLineAfter(final int start, List<String> lines) {
			int nextSnippetStart = start + 1;
			String endLine = lines.get(nextSnippetStart);
			while (!isHeadingLine(endLine)) {
				if (isEndOfFile(nextSnippetStart, lines)) {
					break;
				}
				lines.get(nextSnippetStart);
				endLine = lines.get(++nextSnippetStart);
			}
			String nextHeadingLine = endLine;
			validate(start, lines, nextSnippetStart, nextHeadingLine);
			return nextSnippetStart;
		}

		private static MyTreeNode popStackToHighest(Stack<MyTreeNode> snippetTreePathClone) {
			MyTreeNode highest = null;
			while (!snippetTreePathClone.isEmpty() && snippetTreePathClone.peek() != null) {
				highest = snippetTreePathClone.pop();
			}
			return highest;
		}

		private static void validate(final int start, List<String> lines, int nextSnippetStart,
				String nextHeadingLine) {
			if (!isHeadingLine(nextHeadingLine) && !isEndOfFile(nextSnippetStart, lines)) {
				throw new RuntimeException("Developer error: [" + start + "] = " + nextHeadingLine);
			}
			if (nextSnippetStart <= start) {
				throw new RuntimeException("Developer error: [" + start + ", " + nextSnippetStart
						+ "]");
			}
			if (nextSnippetStart < lines.size() - 1) {
				if (!lines.get(nextSnippetStart).matches("^=+.*=+")) {
					throw new RuntimeException("Developer error: [" + nextSnippetStart + "] = "
							+ lines.get(nextSnippetStart) + ". Last line is " + lines.size());
				}
			}
		}

		private static boolean isEndOfFile(int end, List<String> lines) {
			return end >= lines.size() - 1;

		}

		@Deprecated
		// Use Utils
		private static boolean isHeadingLine(String line) {
			return line.matches("^=+.*=+");
		}

	}

	private static class VirtualNodeCreator {

		public static MyTreeNode createVirtualNode(int iHeadingLevel,
				Stack<MyTreeNode> snippetTreePath, MyTreeNode highestNodeInExistingTree) {
			boolean attachExistingTreeToNewNode = false;
			MyTreeNode parentNode = null;
			// attach to existing tree
			if (highestNodeInExistingTree != null) {
				if (highestNodeInExistingTree.level() == iHeadingLevel) {
					throw new RuntimeException("Not sure how to handle this case. Recursive?");
				} else if (highestNodeInExistingTree.level() < iHeadingLevel) {
					parentNode = findFosterParent(iHeadingLevel, snippetTreePath);
				} else if (highestNodeInExistingTree.level() > iHeadingLevel) {
					// attach the highest node as a child to the new virtual
					// node
					// (is it necessary to create a virtual node then?)
					attachExistingTreeToNewNode = true;
				}
			}
			MyTreeNode n = new MyTreeNode(createVirtualSnippet(iHeadingLevel), parentNode);
			if (attachExistingTreeToNewNode) {
				attachExistingTreeToNode(highestNodeInExistingTree, n);
			}
			return n;
		}

		private static void attachExistingTreeToNode(MyTreeNode highestNodeInExistingTree,
				MyTreeNode n) {
			if (highestNodeInExistingTree.level() + 1 != n.level()) {
				System.err.println("Ideally this should never happen");
			}
			n.addChild(highestNodeInExistingTree);
		}

		private static Snippet createVirtualSnippet(int parentHeadingLevel) {
			StringBuffer equalsLeg = new StringBuffer();
			for (int i = 0; i < parentHeadingLevel; i++) {
				equalsLeg.append("=");
			}
			String string = new StringBuffer().append(equalsLeg).append(" ").append(equalsLeg)
					.toString();
			return new Snippet(ImmutableList.of(string), string, parentHeadingLevel);
		}

		private static MyTreeNode findFosterParent(int parentHeadingLevel,
				Stack<MyTreeNode> snippetTreePathClone) {
			while (snippetTreePathClone.peek().level() >= parentHeadingLevel) {
				snippetTreePathClone.pop();
			}
			return snippetTreePathClone.peek();
		}
	}

	private static class Snippet implements Comparable<Object> {
		final int levelNumber;
		final List<String> snippetLines;
		private final String headingLine;

		public Snippet(List<String> singleSnippetLines, String headingLine, int headingLevel) {
			this.snippetLines = Preconditions.checkNotNull(singleSnippetLines);
			this.levelNumber = headingLevel;
			this.headingLine = Preconditions.checkNotNull(headingLine);
		}

		public Snippet(int start, int nextSnippetStart, List<String> allFileLines) {
			this(getSnippetLines(start, nextSnippetStart, allFileLines), getHeadingLine(start,
					allFileLines), Utils.determineHeadingLevel(getHeadingLine(start, allFileLines)));
		}

		public static String getHeadingLine(int start, List<String> allFileLines) {
			validateIsHeadingLine(allFileLines.get(start));
			return allFileLines.get(start);
		}

		private static void validateIsHeadingLine(String headingLine) {
			if (!headingLine.matches("^=+.*=+")) {
				throw new RuntimeException("Developer error: Not a heading line: " + headingLine);
			}
		}

		public static ImmutableList<String> getSnippetLines(int start, int nextSnippetStart,
				List<String> allFileLines) {
			if (allFileLines.size() <= nextSnippetStart) {
				// make sure the final line gets included in this snippet
				nextSnippetStart = allFileLines.size();
			}
			validateSnippetStartAndNextAreNotSame(start, nextSnippetStart);
			return ImmutableList.copyOf(allFileLines.subList(start,
					Math.max(start + 1, nextSnippetStart)));
		}

		private static void validateSnippetStartAndNextAreNotSame(int start, int nextSnippetStart) {
			if (start == nextSnippetStart) {
				throw new RuntimeException("end should be the start of the next");
			}
		}

		public int getLevelNumber() {
			return levelNumber;
		}

		public String getHeadingLine() {
			return headingLine;
		}

		@Override
		public String toString() {
			return getText().toString();
		}

		public StringBuffer getTextNoHeading() {
			StringBuffer sb = new StringBuffer();

			for (int i = 1; i < snippetLines.size(); i++) {
				sb.append(snippetLines.get(i));
				sb.append("\n");
			}
			return sb;
		}

		public StringBuffer getText() {
			StringBuffer sb = new StringBuffer();
			int i = 0;
			for (String line : snippetLines) {
				sb.append(line);
				// if (i == snippetLines.size()-1) {
				// if (line.equals("\n")) {
				//
				// }
				// sb.append("\n");
				// } else {
				sb.append("\n");
				// }
				i++;
			}
			return sb;
		}

		// @Override
		public int compareTo(Object o) {
			if (o == null) {
				return 1;
			}
			Snippet that = (Snippet) o;
			String thisHeading = this.headingLine.toLowerCase();
			String thatHeading = that.headingLine.toLowerCase();
			return thisHeading.compareTo(thatHeading);
		}

		@Override
		public boolean equals(Object o) {
			String thisText = this.getText().toString();
			String thatText = ((Snippet) o).getText().toString();
			if (thisText.equals(thatText)) {
				if (thisText != thatText) {
					throw new RuntimeException("This case is not considered");
				}
			}
			return thisText.equals(thatText);
		}
	}

	private static class MyTreeNode implements Comparable<Object> {
		final ListMultimap<String, MyTreeNode> childNodes = LinkedListMultimap.create();
		final MyTreeNode parentNode;
		final Snippet snippet;
		public static int totalNodeCount = 0;

		// parentNode could be null
		public MyTreeNode(Snippet currentSnippet, MyTreeNode parentNode) {
			if (parentNode == this) {
				throw new RuntimeException("Developer Error - can't be parent of self");
			}
			if (parentNode != null) {
				if (parentNode.getSnippetHeadingLine().equals(currentSnippet.getHeadingLine())) {
					String s = parentNode.getSnippetHeadingLine() + "::"
							+ currentSnippet.getHeadingLine();
					throw new RuntimeException("Developer Error - can't be parent of self: " + s);
				}
			}
			this.snippet = currentSnippet;
			this.parentNode = parentNode;
			if (parentNode != null) {
				parentNode.addChild(this);
			}
			++totalNodeCount;
			// System.out.println(currentSnippet.getHeadingLine());
		}

		// Only necessary for virtual nodes. Otherwise there's no need to
		// call this.
		// Child-parent relationships should be established in the
		// constructor
		// itself
		void addChild(MyTreeNode currentNode) {
			validateIsNotParentOf(currentNode);
			int sizeBefore = childNodes.get(currentNode.getSnippetHeadingLine()).size();
			if (sizeBefore > 0) {
				if (currentNode.getSnippetText().equals(
						childNodes.get(currentNode.getSnippetHeadingLine()).iterator().next())) {
					throw new RuntimeException("Developer Error");
				}
			}
			childNodes.put(currentNode.getSnippetHeadingLine(), currentNode);
			validateSizeBeforeAndAfterNotSame(currentNode, sizeBefore);

		}

		private void validateIsNotParentOf(MyTreeNode currentNode) {
			if (currentNode.isParentOf(this)) {
				throw new RuntimeException("Cycle detected");
			}
		}

		private void validateSizeBeforeAndAfterNotSame(MyTreeNode currentNode, int sizeBefore) {
			int sizeAfter = childNodes.get(currentNode.getSnippetHeadingLine()).size();
			if (sizeBefore == sizeAfter) {
				System.out.println("####################################");
				System.out.println(childNodes.get(currentNode.getSnippetHeadingLine()).iterator()
						.next().getSnippetText());
				System.out.println("##################");
				System.out.println(currentNode.getSnippetText());
				throw new RuntimeException("Developer Error");
			}
		}

		// public Snippet getSnippet() {
		// return snippet;
		// }
		String getSnippetHeadingLine() {
			return snippet.getHeadingLine();
		}

		int level() {
			return snippet.getLevelNumber();
		}

		@Deprecated
		// Demeter
		Snippet getSnippet() {
			return snippet;
		}

		StringBuffer getSnippetText() {
			return snippet.getText();
		}

		StringBuffer getSnippetTextNoHeading() {
			return snippet.getTextNoHeading();
		}

		private ImmutableList<MyTreeNode> getChildNodes() {
			List<MyTreeNode> l = new java.util.LinkedList<MyTreeNode>();
			for (String key : childNodes.keySet()) {
				List<MyTreeNode> nodesWithSameHeading = childNodes.get(key);
				l.addAll(nodesWithSameHeading);
			}
			if (!preserveOriginalOrder(this)) {
				Collections.sort(l);
			}
			ImmutableList<MyTreeNode> ret = ImmutableList.copyOf(l);
			return ret;
		}

		private Boolean preserveOriginalOrder(MyTreeNode myTreeNode1) {
			boolean doNotSort = myTreeNode1.getSnippetHeadingLine().contains("do not sort");
			boolean publishing = myTreeNode1.getSnippetHeadingLine().contains(
					Defragmenter.PUBLISHING);
			boolean preserveOriginalOrder = doNotSort || publishing;
			if (preserveOriginalOrder) {
				return true;
			}
			if (myTreeNode1.parentNode == null) {
				return false;
			}
			return this.preserveOriginalOrder(myTreeNode1.getParentNode());

		}

		@Override
		public String toString() {
			return print("");
		}

		private String print(String indent) {
			String children = "";
			for (MyTreeNode child : childNodes.values()) {
				children += child.print(indent + "\t");
			}
			return indent + getHeadingText() + (childNodes.size() > 0 ? "" : "") + children;
		}

		MyTreeNode getParentNode() {
			return parentNode;
		}

		// @Override
		public int compareTo(Object other) {
			MyTreeNode that = (MyTreeNode) other;
			int ret = this.snippet.compareTo(that.snippet);
			if (ret == 0) {
				// throw new
				// RuntimeException("snippets should never be equal");
			}
			return ret;
		}

		String getHeadingText() {
			return snippet.getHeadingLine();
		}

		boolean isParentOf(MyTreeNode iNode) {

			if (iNode.parentNode == this) {
				return true;
			}

			if (iNode.parentNode == null) {
				return false;
			}
			return this.isParentOf(iNode.parentNode);

		}

		int countNodesInSubtree() {
			int count = 1;
			for (MyTreeNode child : this.getChildNodes()) {
				count += child.countNodesInSubtree();
			}
			return count;
		}

		void addChildren(List<MyTreeNode> roots) {
			for (MyTreeNode aRoot : roots) {
				this.addChild(aRoot);
			}
		}

		static int countAllNodesInTree(MyTreeNode anyNodeInTree) {
			// Find the highest parent
			MyTreeNode root = getHighestNode(anyNodeInTree);
			return root.countNodesInSubtree();
		}

		private static MyTreeNode getHighestNode(MyTreeNode anyNodeInTree) {
			if (anyNodeInTree.parentNode == null) {
				return anyNodeInTree;
			} else {
				return getHighestNode(anyNodeInTree.parentNode);
			}
		}

		// TODO: Bad. A method that has side effects AND returns something. This
		// is
		// why this algorithm is difficult to understand
		private static MyTreeNode printNonPublishedSectionsOfSubtree(MyTreeNode iTreeRoot,
				StringBuffer outputString) {
			if (iTreeRoot.getSnippetHeadingLine().contains(Defragmenter.PUBLISHING)) {
				return iTreeRoot;
			}
			outputString.append(iTreeRoot.getSnippetText());
			MyTreeNode forPublishing = null;
			for (MyTreeNode child : iTreeRoot.getChildNodes()) {
				MyTreeNode forPublishingChild = printNonPublishedSectionsOfSubtree(child,
						outputString);
				if (forPublishingChild != null) {
					// sanity check
					if (forPublishing != null) {
						throw new RuntimeException(
								"Developer error - we cannot support more than one tree for publishing");
					}
					forPublishing = forPublishingChild;
				}
			}
			return forPublishing;
		}

		static void dumpTreeToFileAndVerify(MyTreeNode iRootTreeNode, String iFileToWritePath,
				int nonHeadinglinesInOriginalFile, String string, String string2) {
			System.out.println("TextSorter.MyTreeNode.dumpTreeToFileAndVerify() - begin");
			String outputMwkFilePath = Utils.getDeragmentedFilePath(iFileToWritePath, string);

			writeTreeToFile2(iRootTreeNode, outputMwkFilePath);
			verifyFileWasWritten(outputMwkFilePath, nonHeadinglinesInOriginalFile);

			removeRedundantHeadings(outputMwkFilePath, nonHeadinglinesInOriginalFile, string2);
		}

		private static void verifyFileWasWritten(String outputPath,
				int nonHeadinglinesInOriginalFile) {
			System.out.println("TextSorter.MyTreeNode.verifyFileWasWritten() - begin");
			int after = Utils.countNonHeadingLines(TextSorterControllerUtils.readFile(outputPath));
			if (nonHeadinglinesInOriginalFile > after) {
				throw new RuntimeException("Lines lost");
			} else {
				System.out.println("TextSorter.MyTreeNode.verifyFileWasWritten() - Before, after: ["
						+ nonHeadinglinesInOriginalFile + ", " + after + "]");
			}

		}

		static StringBuffer dumpTree(MyTreeNode root) {
			StringBuffer rStringBuffer = new StringBuffer();

			// Step 1 - print the non-published sections
			MyTreeNode forPublishing = MyTreeNode.printNonPublishedSectionsOfSubtree(root,
					rStringBuffer);

			// Step 2 - print the published section
			if (forPublishing != null) {
				printPublishedSubtree(forPublishing, rStringBuffer);
			}

			return rStringBuffer;
		}

		private static void removeRedundantHeadings(String uncoagulatedFilePath,
				int nonHeadinglinesInOriginalFile, String string) {
			String coagulatedFilePath = getCoagulatedFilePath(uncoagulatedFilePath, string);

			MyTreeNode inputTreeRootNode = createTreeFromMwkFile(uncoagulatedFilePath);

			MyTreeNode outputTreeRootNode = coagulateChildrenOfRootNode(inputTreeRootNode);

			validateNodeAfterCoagulationOfChildren(inputTreeRootNode, outputTreeRootNode);

			writeTreeToFile(outputTreeRootNode, coagulatedFilePath);

			validateSizeBeforeAndAfterCoagulation(coagulatedFilePath, nonHeadinglinesInOriginalFile);
		}

		@Deprecated
		// Use TextSorterWebServer#writeTreeToFile()
		private static void writeTreeToFile2(MyTreeNode iTreeRootNode, String iMwkFilePath) {
			StringBuffer theEntireFileString = dumpTree(iTreeRootNode);
			writeStringToFile(theEntireFileString, iMwkFilePath);
		}

		private static void writeTreeToFile(MyTreeNode iTreeRootNode, String iMwkFilePath) {
			StringBuffer theEntireFileString = dumpTree(iTreeRootNode);
			writeStringToFile(theEntireFileString, iMwkFilePath);
		}

		private static void writeStringToFile(StringBuffer sb, String iFilePath) {
			try {
				FileUtils.write(new File(iFilePath), sb.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private static MyTreeNode createTreeFromMwkFile(String iMwkFilePath) {
			try {
				List<String> lines = FileUtils.readLines(new File(iMwkFilePath));

				MyTreeNode inputTreeRootNode = TreeCreator.createTreeFromLines(lines);
				return inputTreeRootNode;
			} catch (IOException e) {
				throw new RuntimeException();
			}
		}

		private static void validateSizeBeforeAndAfterCoagulation(String coagulatedFilePath,
				int nonHeadinglinesInOriginalFile) {

			int after = Utils.countNonHeadingLines(TextSorterControllerUtils
					.readFile(coagulatedFilePath));
			System.out.println("TextSorter.MyTreeNode.validateSizeBeforeAndAfterCoagulation() - Before, after: ["
					+ nonHeadinglinesInOriginalFile + ", " + after + "]");
			if (nonHeadinglinesInOriginalFile != after) {
				throw new RuntimeException("Lines lost or spurious added");
			} else {
			}
		}

		private static void validateNodeAfterCoagulationOfChildren(MyTreeNode inputTreeNode,
				MyTreeNode outputTreeNode) {
			if (inputTreeNode.getParentNode() == null && outputTreeNode.getParentNode() == null) {
				return;
			}
			if (inputTreeNode.getParentNode().getHeadingText()
					.equals(outputTreeNode.getParentNode().getHeadingText())) {
				return;
			}
			if (inputTreeNode.getChildNodes().size() > 0) {
				if (!(outputTreeNode.getChildNodes().size() > 0)) {
					return;
				}
			}
			throw new RuntimeException("Something got lost during coagulation");
		}

		public static String getCoagulatedFilePath(String fragmentedFilePath, String string) {
			return fragmentedFilePath.replace("sorted", string);
		}

		/**
		 * Creates a mirror of the passed subtree, but with children combined
		 * into a single node
		 */
		private static MyTreeNode coagulateChildrenOfRootNode(MyTreeNode iInputTreeRootNode) {

			MyTreeNode rOutputTreeNode = cloneRootNodeHeader(iInputTreeRootNode);
			Map<String, List<MyTreeNode>> childNodesGroupedByHeading = getRootChildNodesGrouped(iInputTreeRootNode
					.getChildNodes());
			Map<String, MyTreeNode> superChildNodes = createSuperNodesForRootChildren(childNodesGroupedByHeading);
			// Ideally we should sort these
			addChildNodesToOutputRootNode(rOutputTreeNode, superChildNodes.values());
			return rOutputTreeNode;
		}

		private static MyTreeNode cloneRootNodeHeader(MyTreeNode iInputTreeRootNode) {
			// TODO: override clone method instead?
			MyTreeNode clone = new MyTreeNode(iInputTreeRootNode.getSnippet(),
					iInputTreeRootNode.getParentNode());
			return clone;
		}

		private static Map<String, List<MyTreeNode>> getRootChildNodesGrouped(
				List<MyTreeNode> childNodes2) {
			return groupByHeading(childNodes2);
		}

		private static void addChildNodesToOutputRootNode(MyTreeNode rOutputTreeNode,
				Collection<MyTreeNode> values) {
			rOutputTreeNode.addChildren(new LinkedList<MyTreeNode>(values));

		}

		private static Map<String, MyTreeNode> createSuperNodesForRootChildren(
				Map<String, List<MyTreeNode>> nodesGroupedByHeading) {
			Map<String, MyTreeNode> ret = new HashMap<String, MyTreeNode>();
			if (nodesGroupedByHeading == null) {
				return ret;
			}
			for (String aHeading : nodesGroupedByHeading.keySet()) {
				List<MyTreeNode> allNodesForHeading = nodesGroupedByHeading.get(aHeading);
				MyTreeNode singleNode = squashNodes(allNodesForHeading);
				ret.put(aHeading, singleNode);
			}
			return ret;
		}

		private static MyTreeNode squashNodes(List<MyTreeNode> allNodesForHeading) {

			MyTreeNode squashedNode = createSuperNodeHeader(allNodesForHeading);

			List<MyTreeNode> roots = getSuperNodes(allNodesForHeading);
			squashedNode.addChildren(roots);

			return squashedNode;
		}

		private static List<MyTreeNode> getSuperNodes(List<MyTreeNode> allNodesForHeading) {
			List<MyTreeNode> children = getAllChildren(allNodesForHeading);

			Map<String, List<MyTreeNode>> childrenByHeading = groupByHeading(children);
			Map<String, MyTreeNode> superChildrenByHeading = squashNodesByHeading(childrenByHeading);
			// Ideally we should add these children in sorted order
			List<MyTreeNode> roots = new LinkedList<MyTreeNode>(superChildrenByHeading.values());
			return roots;
		}

		private static Map<String, MyTreeNode> squashNodesByHeading(
				Map<String, List<MyTreeNode>> nodesByHeading) {
			Map<String, MyTreeNode> ret = new HashMap<String, MyTreeNode>();
			for (String aHeading : nodesByHeading.keySet()) {
				List<MyTreeNode> nodes = nodesByHeading.get(aHeading);
				MyTreeNode superNode = createSuperNodeHeader(nodes);
				List<MyTreeNode> superChildNodes = getSuperNodes(nodes);
				superNode.addChildren(superChildNodes);
				ret.put(aHeading, superNode);
			}
			return ret;
		}

		// @Nullable
		private static MyTreeNode createSuperNodeHeader(List<MyTreeNode> nodes) {
			if (nodes.size() == 0) {
				return null;
			}
			checkAllNodesHaveSameHeading(nodes);
			MyTreeNode first = nodes.get(0);
			MyTreeNode parent = first.getParentNode();
			List<String> superSnippetLines = new LinkedList<String>();
			int i = 0;
			for (MyTreeNode n : nodes) {
				// Hmmm we're assuming that a string containing newlines will
				// have
				// the same effect as newline-deliminted line. Hopefully this
				// won't
				// be a problem.
				StringBuffer textToAdd;
				// TODO: Ideally use this, but since note making has not been
				// disciplined, this will lose separations.
				// if (i == 0) {
				textToAdd = n.getSnippetText();
				String textToAdd2 = textToAdd.substring(0, textToAdd.length() - 1);
				// } else {

				// textToAdd = n.getSnippetTextNoHeading();
				// }
				superSnippetLines.add(textToAdd2.toString());
				i++;
			}
			Snippet superSnippet = new Snippet(superSnippetLines, first.getHeadingText(), first
					.getSnippet().getLevelNumber());
			MyTreeNode ret = new MyTreeNode(superSnippet, parent);
			return ret;
		}

		private static void checkAllNodesHaveSameHeading(List<MyTreeNode> nodes) {
			if (nodes.size() == 0) {
				return;
			}
			String heading = nodes.get(0).getHeadingText();
			for (MyTreeNode n : nodes) {
				if (!heading.equals(n.getHeadingText())) {
					throw new RuntimeException(heading + "::" + n.getHeadingText());
				}
			}

		}

		private static List<MyTreeNode> getAllChildren(List<MyTreeNode> iNodesForHeading) {
			List<MyTreeNode> superList = new LinkedList<MyTreeNode>();
			for (MyTreeNode aChildNode : iNodesForHeading) {
				superList.addAll(aChildNode.getChildNodes());
			}
			return superList;
		}

		private static Map<String, List<MyTreeNode>> groupByHeading(List<MyTreeNode> iNodes) {
			Map<String, List<MyTreeNode>> rHeadingToAllChildrenOfHeading = new HashMap<String, List<MyTreeNode>>();

			for (MyTreeNode childNode : iNodes) {
				List<MyTreeNode> childNodesForHeading = rHeadingToAllChildrenOfHeading
						.get(childNode.getHeadingText());
				if (childNodesForHeading == null) {
					childNodesForHeading = new LinkedList<MyTreeNode>();
					rHeadingToAllChildrenOfHeading.put(childNode.getHeadingText(),
							childNodesForHeading);
				}
				childNodesForHeading.add(childNode);
			}
			return rHeadingToAllChildrenOfHeading;
		}

		private static void printPublishedSubtree(MyTreeNode forPublishing, StringBuffer sb) {
			sb.append(forPublishing.getSnippetText());
			for (MyTreeNode child : forPublishing.getChildNodes()) {
				printPublishedSubtree(child, sb);
			}
		}

		public static void validateCount(MyTreeNode parentNode) {
			Preconditions.checkNotNull(parentNode);
			int countAllNodesInTree = MyTreeNode.countAllNodesInTree(parentNode);
			int totalNodeCount2 = MyTreeNode.totalNodeCount;
			if (countAllNodesInTree < totalNodeCount2) {
				throw new RuntimeException("Disconnected from parent: " + countAllNodesInTree
						+ " vs " + totalNodeCount2 + "\n\n" + parentNode.getSnippetText());
			} else {
				// System.out.println(countAllNodesInTree + " vs " +
				// totalNodeCount2
				// + "\n\n" + parentNode.getSnippetText());
			}
		}

		public static void validateTotalNodeCount(MyTreeNode root) {
			System.out.println("TextSorter.MyTreeNode.validateTotalNodeCount() - begin");
			int subtreeNodeCount = root.countNodesInSubtree();
			if (subtreeNodeCount < MyTreeNode.totalNodeCount) {
				throw new RuntimeException("Nodes lost: [" + MyTreeNode.totalNodeCount + ", "
						+ subtreeNodeCount + "]");
			}
		}

		public static void resetValidationStats() {
			totalNodeCount = 0;
		}

	}

}
