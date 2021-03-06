import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

public class MwkSort {

	public static void main(String[] args) throws IOException {
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine();
		List<String> lines = new LinkedList<String>();
		while (line != null) {
			System.err.println(line);
			lines.add(line);
			line = br.readLine();
		}
		// Prints to stdout
		Defragmenter.defragmentFile(lines);
	}

	private static class Defragmenter {
		public static final String PUBLISHING = "publishing";

		/**
		 * This only writes it to stdout, it doesn't modify the file. Hmmmm,
		 * this comment looks wrong.
		 */
		public static void defragmentFile(List<String> lines) {
			System.err.println("TextSorter.Defragmenter.defragmentFile() - begin: ");
			MyTreeNode treeRootNode = TreeCreator.createTreeFromLines(lines);
			MyTreeNode.validateTotalNodeCount(treeRootNode);
			MyTreeNode.dumpTreeToFileAndVerify(treeRootNode, Utils.countNonHeadingLines(lines),
					"-sorted.mwk", "coagulate");
			System.err.println("TextSorter.Defragmenter.defragmentFile() - end");
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
						throw new RuntimeException("Developer Error - can't be parent of self: "
								+ s);
					}
				}
				this.snippet = currentSnippet;
				this.parentNode = parentNode;
				if (parentNode != null) {
					parentNode.addChild(this);
				}
				++totalNodeCount;
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
					System.err.println("####################################");
					System.err.println(childNodes.get(currentNode.getSnippetHeadingLine())
							.iterator().next().getSnippetText());
					System.err.println("##################");
					System.err.println(currentNode.getSnippetText());
					throw new RuntimeException("Developer Error");
				}
			}

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

			// TODO: Bad. A method that has side effects AND returns something.
			// This is why this algorithm is difficult to understand
			private static MyTreeNode printNonPublishedSectionsOfSubtree(MyTreeNode iTreeRoot) {
				if (iTreeRoot.getSnippetHeadingLine().contains(Defragmenter.PUBLISHING)) {
					return iTreeRoot;
				}
				System.out.print(iTreeRoot.getSnippetText());
				MyTreeNode forPublishing = null;
				for (MyTreeNode child : iTreeRoot.getChildNodes()) {
					MyTreeNode forPublishingChild = printNonPublishedSectionsOfSubtree(child);
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

			static void dumpTreeToFileAndVerify(MyTreeNode iRootTreeNode,
					int nonHeadinglinesInOriginalFile, String string, String string2) {
				System.err.println("TextSorter.MyTreeNode.dumpTreeToFileAndVerify() - begin");

				dumpTree(iRootTreeNode);
				// verifyFileWasWritten(outputMwkFilePath,
				// nonHeadinglinesInOriginalFile);
				// TODO: Move this to another script
				if (false) {
					removeRedundantHeadings(iRootTreeNode, nonHeadinglinesInOriginalFile, string2);
				}
			}

			static void dumpTree(MyTreeNode root) {

				// Step 1 - print the non-published sections
				MyTreeNode forPublishing = MyTreeNode.printNonPublishedSectionsOfSubtree(root);

				// Step 2 - print the published section
				if (forPublishing != null) {
					printPublishedSubtree(forPublishing);
				}
			}

			private static void removeRedundantHeadings(MyTreeNode inputTreeRootNode,
					int nonHeadinglinesInOriginalFile, String string) {

				MyTreeNode outputTreeRootNode = coagulateChildrenOfRootNode(inputTreeRootNode);

				validateNodeAfterCoagulationOfChildren(inputTreeRootNode, outputTreeRootNode);

				writeTreeToFile(outputTreeRootNode);
			}

			private static void writeTreeToFile(MyTreeNode iTreeRootNode) {
				dumpTree(iTreeRootNode);
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

			/**
			 * Creates a mirror of the passed subtree, but with children
			 * combined into a single node
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
				for (MyTreeNode n : nodes) {
					// Hmmm we're assuming that a string containing newlines
					// will have the same effect as newline-deliminted line. Hopefully
					// this won't be a problem.
					// TODO: Ideally use this, but since note making has not
					// been disciplined, this will lose separations.
					StringBuffer textToAdd = n.getSnippetText();
					String textToAdd2 = textToAdd.substring(0, textToAdd.length() - 1);
					superSnippetLines.add(textToAdd2.toString());
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

			private static void printPublishedSubtree(MyTreeNode forPublishing) {
				System.out.print(forPublishing.getSnippetText());
				for (MyTreeNode child : forPublishing.getChildNodes()) {
					printPublishedSubtree(child);
				}
			}

			public static void validateCount(MyTreeNode parentNode) {
				Preconditions.checkNotNull(parentNode);
				int countAllNodesInTree = MyTreeNode.countAllNodesInTree(parentNode);
				int totalNodeCount2 = MyTreeNode.totalNodeCount;
				if (countAllNodesInTree < totalNodeCount2) {
					throw new RuntimeException("Disconnected from parent: " + countAllNodesInTree
							+ " vs " + totalNodeCount2 + "\n\n" + parentNode.getSnippetText());
				}
			}

			public static void validateTotalNodeCount(MyTreeNode root) {
				System.err.println("TextSorter.MyTreeNode.validateTotalNodeCount() - begin");
				int subtreeNodeCount = root.countNodesInSubtree();
				if (subtreeNodeCount < MyTreeNode.totalNodeCount) {
					throw new RuntimeException("Nodes lost: [" + MyTreeNode.totalNodeCount + ", "
							+ subtreeNodeCount + "]");
				}
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
						allFileLines), Utils.determineHeadingLevel(getHeadingLine(start,
						allFileLines)));
			}

			public static String getHeadingLine(int start, List<String> allFileLines) {
				validateIsHeadingLine(allFileLines.get(start));
				return allFileLines.get(start);
			}

			private static void validateIsHeadingLine(String headingLine) {
				if (!headingLine.matches("^=+.*=+")) {
					throw new RuntimeException("Developer error: Not a heading line: "
							+ headingLine);
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

			private static void validateSnippetStartAndNextAreNotSame(int start,
					int nextSnippetStart) {
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

			public StringBuffer getText() {
				StringBuffer sb = new StringBuffer();
				for (String line : snippetLines) {
					sb.append(line);
					sb.append("\n");
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
		}

		private static class TreeCreator {
			private static class VirtualNodeCreator {

				public static MyTreeNode createVirtualNode(int iHeadingLevel,
						Stack<MyTreeNode> snippetTreePath, MyTreeNode highestNodeInExistingTree) {
					boolean attachExistingTreeToNewNode = false;
					MyTreeNode parentNode = null;
					// attach to existing tree
					if (highestNodeInExistingTree != null) {
						if (highestNodeInExistingTree.level() == iHeadingLevel) {
							throw new RuntimeException(
									"Not sure how to handle this case. Recursive?");
						} else if (highestNodeInExistingTree.level() < iHeadingLevel) {
							parentNode = findFosterParent(iHeadingLevel, snippetTreePath);
						} else if (highestNodeInExistingTree.level() > iHeadingLevel) {
							// attach the highest node as a child to the new
							// virtual node (is it necessary to create a virtual
							// node then?)
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
					String string = new StringBuffer().append(equalsLeg).append(" ")
							.append(equalsLeg).toString();
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

			public static MyTreeNode createTreeFromLines(List<String> lines) {
				System.err.println("TextSorter.TreeCreator.createTreeFromLines() - begin");
				List<Snippet> theSnippetList = getSnippetList(lines);
				Stack<MyTreeNode> snippetTreePath = new Stack<MyTreeNode>();
				MyTreeNode.totalNodeCount = 0;
				System.err.println("TextSorter.TreeCreator.createTreeFromLines() - before loop");
				for (Snippet aSnippet : theSnippetList) {
					System.err.print(".");
					// Find the parent node
					MyTreeNode aParentNode = getParentNode(snippetTreePath, aSnippet);
					// Add this snippet as a child of parentNode
					snippetTreePath.push(new MyTreeNode(aSnippet, aParentNode));
					// MyTreeNode.dumpTree(aParentNode);
					MyTreeNode.validateCount(aParentNode);
				}
				System.err.println("");
				System.err.println("TextSorter.TreeCreator.createTreeFromLines() - after loop");
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

			@SuppressWarnings("unchecked")
			private static MyTreeNode getParentNode(Stack<MyTreeNode> snippetTreePath,
					Snippet aSnippet) {
				MyTreeNode aParentNode = null;
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
							(Stack<MyTreeNode>) snippetTreePath.clone(), highestNodeInExistingTree);
					snippetTreePath.push(aParentNode);
				}
				return aParentNode;
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
				System.err.println("TextSorter.TreeCreator.getSnippetList() - begin");
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
					throw new RuntimeException("Developer error: [" + start + "] = "
							+ nextHeadingLine);
				}
				if (nextSnippetStart <= start) {
					throw new RuntimeException("Developer error: [" + start + ", "
							+ nextSnippetStart + "]");
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
	}
}
