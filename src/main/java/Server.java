import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VideoInfo.VideoQuality;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class Server {
	@Path("yurl")
	public static class HelloWorldResource { // Must be public

		private static final String CYPHER_URI = "http://netgear.rohidekar.com:7474/db/data/cypher";
		private static final String TARGET_DIR_PATH = "/media/sarnobat/Unsorted/Videos/";
		private static final String TARGET_DIR_PATH_IMAGES = "/media/sarnobat/Unsorted/images/";

		@GET
		@Path("parent")
		@Produces("application/json")
		public Response parent(@QueryParam("nodeId") Integer iNodeId)
				throws JSONException, IOException {
			Map<String, Object> theParams = new HashMap<String, Object>();
			theParams.put("nodeId", iNodeId);
			// TODO: order these by most recent-first (so that they appear this
			// way in the UI)
			JSONObject theParentNodeJson = execute(
					"start n=node({nodeId}) MATCH p-[r:CONTAINS]->n RETURN id(p)",
					theParams);
			JSONArray theData = (JSONArray) theParentNodeJson.get("data");
			JSONArray ret = new JSONArray();
			for (int i = 0; i < theData.length(); i++) {
				JSONArray a = theData.getJSONArray(i);
				JSONObject o = new JSONObject();
				String id = (String) a.get(0);
				o.put("id", id);
				ret.put(o);
			}
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(ret.toString()).type("application/json").build();
		}

		//
		// Batch
		//

		@GET
		@Path("batchInsert")
		@Produces("application/json")
		public Response batchInsert(@QueryParam("rootId") Integer iRootId,
				@QueryParam("urls") String iUrls) throws Exception {
			Preconditions.checkArgument(iRootId != null);
			System.out.println("batchInsert - " + iRootId);
			// System.out.println("batchInsert - " + URLDecoder.decode(iUrls,
			// "UTF-8"));
			StringBuffer unsuccessfulLines = new StringBuffer();

			String[] lines = iUrls.trim().split("\\n");
			int i = 0;
			while (i < lines.length) {

				System.out.println("1");
				// System.out.println("lines 1");
				// System.out.println("lines 2: " + lines);
				String first = lines[i];
				System.out.println("first: " + first);
				String theHttpUrl;
				try {
					// Fails if it sees the string "%)"
					theHttpUrl = URLDecoder.decode(first, "UTF-8");
				} catch (Exception e) {
					System.out.println("Could not decode");
					System.out.println(e);
					System.out.println(e.getStackTrace());
					// i += lines.length();
					addToUnsuccessful(unsuccessfulLines, first, "");
					++i;
					// for (int j = 0; j < lines.length(); j++) {
					// addToUnsuccessful(unsuccessfulLines, lines[j], "");
					// ++i;
					// }
					continue;
				}
				System.out.println("decoded: " + theHttpUrl);
				if (first.startsWith("=")) {
					++i;
					addToUnsuccessful(unsuccessfulLines, first, "");
					System.out.println("1.1");
					continue;
				}
				System.out.println("2");
				if (first.startsWith("http")) {
					++i;
					addToUnsuccessful(unsuccessfulLines, first, "");
					System.out.println("1.2");
					continue;
				}
				System.out.println("3");
				if (first.matches("^\\s*" + '$')) {

					System.out.println("whitespace: " + first);
					++i;
					continue;
				}
				System.out.println("4");

				String second = lines[i + 1];
				if (first.startsWith("\"") && second.startsWith("http")) {

					System.out.println("to be processed: " + first);
					System.out.println("to be processed: " + second);

					System.out.println("5");
					System.out.println("5.25");

				} else {
					System.out.println("10");
					addToUnsuccessful(unsuccessfulLines, first, second);
				}
				i += 2;

			}
			JSONObject entity = new JSONObject();
			entity.put("unsuccessful", unsuccessfulLines.toString());
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(entity.toString()).type("application/json").build();
		}

		public void addToUnsuccessful(StringBuffer unsuccessfulLines,
				String first, String second) {
			unsuccessfulLines.append(first);
			unsuccessfulLines.append("\n");
			unsuccessfulLines.append(second);
			unsuccessfulLines.append("\n");
			unsuccessfulLines.append("\n");
		}

		//
		// ------------------------------------------------------------------------------------
		// Backup
		// ------------------------------------------------------------------------------------

		@GET
		@Path("dumpurls")
		@Produces("application/text")
		public Response dumpUrls(@QueryParam("rootId") Integer iRootId)
				throws IOException, JSONException {
			Set<String> visitedInternalNodes = new HashSet<String>();
			Integer startId = iRootId;
			if (startId == null) {
				startId = 0;
			}
			StringBuffer sb = new StringBuffer();
			StringBuffer plainText = new StringBuffer();
			sb.append("foo\n");
			printNode(startId, sb, plainText, visitedInternalNodes);
			System.out.println("dumpUrls");
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(plainText.toString()).type("text/plain").build();
		}

		private void printNode(Integer iRootId, StringBuffer json,
				StringBuffer plainText, Set<String> visitedInternalNodes)
				throws IOException, JSONException {
			json.append("bar");
			Map<String, Object> theParams = new HashMap<String, Object>();
			theParams.put("nodeId", iRootId);
			JSONObject theResponse = execute(
					"start root=node({nodeId}) MATCH root--n RETURN distinct n, id(n)",
					theParams);
			JSONArray jsonArray = (JSONArray) theResponse.get("data");
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray aNode = (JSONArray) jsonArray.get(i);
				JSONObject object = (JSONObject) ((JSONObject) aNode.get(0))
						.get("data");

				String id = (String) checkNotNull(aNode.get(1));
				if (visitedInternalNodes.contains(id)) {
					continue;
				}
				System.out.println("1.5");

				if (object.has("type") && object.get("type") != null
						&& object.get("type").equals("categoryNode")) {
					plainText.append("=== ");
					plainText.append(object.get("name"));
					plainText.append(" ===\n");

				}

				if (object.has("title")) {
					plainText.append("\"");
					plainText.append(object.get("title"));
					plainText.append("\",\"");
					if (object.has("url")) {
						plainText.append(object.get("url"));
					}
					plainText.append("\"");
					plainText.append("\n");
				}
				if (object.has("url")) {
					plainText.append(object.get("url"));
					plainText.append("\n");
					plainText.append("\n");
				}

				json.append(object);
				System.out.println("2");
				visitedInternalNodes.add(id);

				printNode(Integer.parseInt(id), json, plainText,
						visitedInternalNodes);
			}
		}

		// ------------------------------------------------------------------------------------
		// Page operations
		// ------------------------------------------------------------------------------------

		@GET
		@Path("uncategorized")
		@Produces("application/json")
		public Response uncategorized(@QueryParam("rootId") Integer iRootId)
				throws JSONException, IOException {
			checkNotNull(iRootId);
			// TODO: the source is null clause should be obsoleted
			Map<String, Object> theParams = new HashMap<String, Object>();
			theParams.put("rootId", iRootId);
			// TODO: order these by most recent-first (so that they appear this
			// way in the UI)
			JSONObject theQueryResultJson = execute(
					"start n=node(*) MATCH n<-[r?:CONTAINS]-source where (source is null or ID(source) = {rootId}) and not(has(n.type)) AND id(n) > 0 return distinct ID(n),n.title?,n.url?,n.created?,n.ordinal? ORDER BY n.ordinal? DESC",
					theParams);
			JSONArray theDataJson = (JSONArray) theQueryResultJson.get("data");
			JSONArray theUncategorizedNodesJson = new JSONArray();
			for (int i = 0; i < theDataJson.length(); i++) {
				JSONObject anUncategorizedNodeJsonObject = new JSONObject();
				_1: {
					JSONArray a = theDataJson.getJSONArray(i);
					_11: {
						String anId = (String) a.get(0);
						anUncategorizedNodeJsonObject.put("id", anId);
					}
					_12: {
						String aTitle = (String) a.get(1);
						anUncategorizedNodeJsonObject.put("title", aTitle);
					}
					_13: {
						String aUrl = (String) a.get(2);
						anUncategorizedNodeJsonObject.put("url", aUrl);
					}
				}
				theUncategorizedNodesJson.put(anUncategorizedNodeJsonObject);
			}
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(theUncategorizedNodesJson.toString())
					.type("application/json").build();
		}

		// -----------------------------------------------------------------------------
		// Key bindings
		// -----------------------------------------------------------------------------

		@GET
		@Path("keysUpdate")
		@Produces("application/json")
		// Ideally we should wrap this method inside a transaction but over REST
		// I don't know how to do that.
		// TODO: we will have to start supporting disassociation of key bindings
		// with child categories
		public Response keysUpdate(@QueryParam("parentId") Integer iParentId,
				@QueryParam("newKeyBindings") String iNewKeyBindings,
				@QueryParam("oldKeyBindings") String iOldKeyBindings)
				throws JSONException, IOException {
			System.out.println("keysUpdate");

			Set<String> theOldKeyBindingsSet = new HashSet<String>();
			Collections.addAll(theOldKeyBindingsSet, iOldKeyBindings.trim()
					.split("\n"));
			Set<String> theNewKeyBindingsSet = new HashSet<String>();
			Collections.addAll(theNewKeyBindingsSet, iNewKeyBindings.trim()
					.split("\n"));

			// NOTE: This is not symmetric (commutative?). If you want to
			// support removal do that in a separate loop
			Set<String> theNewKeyBindingLines = Sets.difference(
					theNewKeyBindingsSet, theOldKeyBindingsSet);
			_1: {
				System.out.println("Old: " + theOldKeyBindingsSet);
				System.out.println("New: " + theNewKeyBindingsSet);
				System.out.println("Difference: " + theNewKeyBindingLines);
			}

			// Remove duplicates by putting the bindings in a map
			Map<String, String> theKeyBindingsNoDuplicates = new HashMap<String, String>();
			for (String aNewKeyBinding : theNewKeyBindingLines) {
				if (aNewKeyBinding.trim().startsWith("#")
						&& !aNewKeyBinding.trim().startsWith("#=")) {
					continue;// A commented out keybinding
				}
				// TODO: do not allow key binding that is "_". This is reserved
				// for hiding until refresh

				_1: {
					// Ignore trailing comments
					String[] aLineElements = aNewKeyBinding.split("=");
					String aKeyCode = aLineElements[0].trim();
					String[] aRightHandSideElements = aLineElements[1].trim()
							.split("#");
					_2: {
						String aName = aRightHandSideElements[0].trim();
						System.out
								.println("Accepting proposal to create key binding for "
										+ aName + "(" + aKeyCode + ")");
						theKeyBindingsNoDuplicates.put(aKeyCode, aName);
					}
				}

				// TODO: if it fails, recover and create the remaining ones?
			}

			for (String aKeyCode : theKeyBindingsNoDuplicates.keySet()) {
				String aName = theKeyBindingsNoDuplicates.get(aKeyCode);
				Map<String, Object> aParamValues = new HashMap<String, Object>();
				_1: {
					aParamValues.put("parentId", iParentId);
					aParamValues.put("key", aKeyCode);
				}
				System.out
						.println("About to remove keybinding for " + aKeyCode);
				JSONObject json = execute(
						"START parent=node( {parentId} ) MATCH parent-[r:CONTAINS]->category WHERE has(category.key) and category.type = 'categoryNode' and category.key = {key} DELETE category.key RETURN category",
						aParamValues);
				theKeyBindingsNoDuplicates.remove(aKeyCode);
				System.out.println("Removed keybinding for " + aName);

				createNewKeyBinding(aName, aKeyCode, iParentId);
			}
			JSONArray ret = getKeys(iParentId);
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(ret.toString()).type("application/json").build();
		}

		private void createNewKeyBinding(String iCategoryName, String iKeyCode,
				Integer iParentId) throws IOException, JSONException {

			// TODO: Also create a trash category for each new category key node

			boolean shouldCreateNewCategoryNode = false;
			_1: {
				Map<String, Object> theParamValues = new HashMap<String, Object>();
				theParamValues.put("parentId", iParentId);
				// TODO: change this back
				theParamValues.put("aCategoryName", iCategoryName);
				theParamValues.put("aKeyCode", iKeyCode);
				String iCypherQuery = "START parent=node({parentId}) MATCH parent -[r:CONTAINS]-> existingCategory WHERE has(existingCategory.type) and existingCategory.type = 'categoryNode' and existingCategory.name = {aCategoryName} SET existingCategory.key = {aKeyCode} RETURN id(existingCategory)";
				JSONObject theJson = execute(iCypherQuery, theParamValues);
				System.out.println("restoring unassociated category: "
						+ iCypherQuery + "\t" + theParamValues);

				System.out.println(theJson.get("data"));
				JSONArray theCategoryNodes = (JSONArray) theJson.get("data");
				if (theCategoryNodes.length() > 0) {
					if (theCategoryNodes.length() > 1) {
						throw new RuntimeException(
								"There should never be 2 child categories of the same node with the same name");
					}
					String theNewCategoryNodeIdString = "-1";

					theNewCategoryNodeIdString = (String) theCategoryNodes
							.get(0);
					System.out.println("Category ID to reattach: "
							+ theNewCategoryNodeIdString);
				} else {
					shouldCreateNewCategoryNode = true;
				}

			}
			if (shouldCreateNewCategoryNode) {

				Map<String, Object> theParamValues = new HashMap<String, Object>();
				_1: {
					theParamValues.put("name", iCategoryName);
					theParamValues.put("key", iKeyCode);
					theParamValues.put("type", "categoryNode");
					theParamValues.put("created", System.currentTimeMillis());
					System.out.println("cypher params: " + theParamValues);
				}
				// TODO: first check if there is already a node with this name,
				// which is for re-associating the keycode with the category
				JSONObject theNewKeyBindingResponseJson = execute(
						"CREATE (n { name : {name} , key : {key}, created: {created} , type :{type}}) RETURN id(n)",
						theParamValues);
				System.out.println(theNewKeyBindingResponseJson.toString());
				String theNewCategoryNodeIdString = (String) ((JSONArray) ((JSONArray) theNewKeyBindingResponseJson
						.get("data")).get(0)).get(0);
				Integer theNewCategoryNodeId = Integer
						.parseInt(theNewCategoryNodeIdString);
				relateHelper(iParentId, theNewCategoryNodeId);
			}
		}

		@GET
		@Path("keys")
		@Produces("application/json")
		public Response keys(@QueryParam("parentId") Integer iParentId)
				throws JSONException, IOException {
			JSONArray ret = getKeys(iParentId);
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(ret.toString()).type("application/json").build();
		}

		public JSONArray getKeys(Integer iParentId) throws IOException,
				JSONException {
			Map<String, Object> theParams = new HashMap<String, Object>();
			_1: {
				theParams.put("parentId", iParentId);
			}
			// Unfortunately, we cannot insist on counting only the URL nodes -
			// it will prevent category nodes from being returned. See if
			// there's
			// a way to do this in Cypher. If there isn't, this is not a huge
			// compromise.
			JSONObject theQueryJsonResult = execute(
					"START n=node(*) MATCH parent-[c:CONTAINS]->n -[c2?:CONTAINS*]->n2 WHERE has(n.name)  and n.type = 'categoryNode'  and id(parent) = {parentId}  RETURN ID(n),n.name,n.key?,count(n2) as c order by c desc",
					theParams);
			JSONArray theData = (JSONArray) theQueryJsonResult.get("data");
			JSONArray oKeys = new JSONArray();
			for (int i = 0; i < theData.length(); i++) {
				JSONObject aBindingObject = new JSONObject();
				_1: {
					JSONArray aBindingArray = theData.getJSONArray(i);
					String id = (String) aBindingArray.get(0);
					aBindingObject.put("id", id);
					String title = (String) aBindingArray.get(1);
					String aUrl = (String) aBindingArray.get(2);
					String aCount = ((Integer) aBindingArray.get(3)).toString();
					_11: {
						// remove this. Just checking that this is a valid
						// number
						Integer.parseInt(aCount);
					}
					aBindingObject.put("name", title);
					aBindingObject.put("key", aUrl);// TODO: this could be null
					aBindingObject.put("count", aCount);
					oKeys.put(aBindingObject);
				}
			}
			return oKeys;
		}

		// --------------------------------------------------------------------------------------
		// Write operations
		// --------------------------------------------------------------------------------------

		@GET
		@Path("stash")
		@Produces("application/json")
		public Response stash(@QueryParam("param1") String iUrl,
				@QueryParam("rootId") Integer iRoodId) throws JSONException,
				IOException {
			System.out.println("stash() - begin");
			String theHttpUrl = URLDecoder.decode(iUrl, "UTF-8");
			System.out.println("stash() - url decoded: " + theHttpUrl);
			String theTitle = getTitle(new URL(theHttpUrl));
			try {
				JSONObject newNodeJsonObject = createNode(theHttpUrl, theTitle,
						new Integer(iRoodId));
				System.out.println("stash() - node created");
				System.out.println("About to get id");
				JSONArray theNewNodeId = (JSONArray) ((JSONArray) newNodeJsonObject
						.get("data")).get(0);
				System.out.println("Got array: " + theNewNodeId);
				System.out.println(theNewNodeId.get(0));
				String id = (String) theNewNodeId.get(0);
				System.out.println(id);

				launchAsynchronousTasks(iUrl, id);
				// TODO: check that it returned successfully (redundant?)
				System.out.println(newNodeJsonObject.toString());
				return Response.ok().header("Access-Control-Allow-Origin", "*")
						.entity(newNodeJsonObject.get("data").toString())
						.type("application/json").build();
			} catch (Exception e) {
				System.out.println("error");
				System.out.println(e);
				e.printStackTrace();
				return null;
			}
		}

		private static void launchAsynchronousTasks(String iUrl, String id) {
			downloadImageInSeparateThread(iUrl, TARGET_DIR_PATH_IMAGES,
					CYPHER_URI, id);
			downloadVideoInSeparateThread(iUrl, TARGET_DIR_PATH, CYPHER_URI, id);
		}

		private static void downloadImageInSeparateThread(final String iUrl,
				final String targetDirPath, final String cypherUri,
				final String id) {

			Runnable r = new Runnable() {
				// @Override
				public void run() {
					System.out.println("downloadImageInSeparateThread() - "
							+ iUrl + " :: " + targetDirPath);
					if (iUrl.toLowerCase().contains(".jpg")) {

					} else if (iUrl.toLowerCase().contains(".png")) {

					} else if (iUrl.toLowerCase().contains(".gif")) {

					} else if (iUrl.toLowerCase().contains("gstatic")) {

					} else {
						System.out
								.println("downloadImageInSeparateThread() - not an image");
						return;// not an image
					}
					try {
						System.out
								.println("downloadImageInSeparateThread() - about to save");
						saveImage(iUrl, targetDirPath);
						System.out
								.println("downloadImageInSeparateThread() - success, updating DB");
						execute("start n=node({id}) WHERE n.url = {url} SET n.downloaded_image = {date}",
								ImmutableMap.<String, Object> of("id",
										Long.valueOf(id), "url", iUrl, "date",
										System.currentTimeMillis()));
						System.out
								.println("downloadImageInSeparateThread() - DB updated");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			new Thread(r).start();
		}

		private static void saveImage(String urlString, String targetDirPath)
				throws IllegalAccessError, IOException {

			URL url = new URL(urlString);
			BufferedImage image = ImageIO.read(url);
			String extension = FilenameUtils.getExtension(urlString);
			String baseName = FilenameUtils.getBaseName(urlString);
			System.out.println(baseName);
			String outputFilename = determineDestinationPathAvoidingExisting(
					targetDirPath + "/" + baseName + "." + extension)
					.toString();
			ImageIO.write(image, extension, new File(outputFilename));

		}

		private static java.nio.file.Path determineDestinationPathAvoidingExisting(
				String destinationFilePath) throws IllegalAccessError {
			String destinationFilePathWithoutExtension = destinationFilePath
					.substring(0, destinationFilePath.lastIndexOf('.'));
			String extension = FilenameUtils.getExtension(destinationFilePath);
			java.nio.file.Path destinationFile = Paths.get(destinationFilePath);
			while (Files.exists(destinationFile)) {
				destinationFilePathWithoutExtension += "1";
				destinationFilePath = destinationFilePathWithoutExtension + "."
						+ extension;
				destinationFile = Paths.get(destinationFilePath);
			}
			if (Files.exists(destinationFile)) {
				throw new IllegalAccessError(
						"an existing file will get overwritten");
			}
			return destinationFile;
		}

		private JSONObject createNode(String theHttpUrl, String theTitle,
				Integer rootId) throws IOException, JSONException {
			Map<String, Object> theParamValues = new HashMap<String, Object>();
			_1: {
				theParamValues.put("url", theHttpUrl);
				theParamValues.put("title", theTitle);
				long currentTimeMillis = System.currentTimeMillis();
				theParamValues.put("created", currentTimeMillis);
				theParamValues.put("ordinal", currentTimeMillis);
			}
			JSONObject json = execute(
					"CREATE (n { title : {title} , url : {url}, created: {created}, ordinal: {ordinal} }) RETURN id(n)",
					theParamValues);
			_2: {
				JSONArray theNewNodeId = (JSONArray) ((JSONArray) json
						.get("data")).get(0);
				System.out.println("New node: " + theNewNodeId.get(0));
				relateHelper(rootId, (Integer) theNewNodeId.get(0));
			}
			return json;
		}

		private String getTitle(final URL iUrl) {
			String title = "";
			ExecutorService theExecutorService = Executors
					.newFixedThreadPool(2);
			Collection<Callable<String>> tasks = new ArrayList<Callable<String>>();
			Callable<String> callable = new Callable<String>() {
				public String call() throws Exception {
					Document doc = Jsoup.connect(iUrl.toString()).get();
					return doc.title();
				}
			};
			tasks.add(callable);
			try {
				List<Future<String>> taskFutures = theExecutorService
						.invokeAll(tasks, 3000L, TimeUnit.SECONDS);
				title = taskFutures.get(0).get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			return title;
		}

		private static void downloadVideoInSeparateThread(
				final String iVideoUrl, final String TARGET_DIR_PATH,
				final String cypherUri, final String id) {
			Runnable r = new Runnable() {

				// @Override
				public void run() {
					try {

						System.out
								.println("downloadVideoInSeparateThread() - Begin");

						File theTargetDir = new File(TARGET_DIR_PATH);
						if (!theTargetDir.exists()) {
							throw new RuntimeException(
									"Target directory doesn't exist");
						}
						VGet v = new VGet(new URL(iVideoUrl), theTargetDir);
						System.out
								.println("downloadVideoInSeparateThread() - About to start downloading");
						v.getVideo().setVideoQuality(VideoQuality.p1080);

						System.out.println(v.getVideo().getWeb().toString());
						System.out.println(v.getVideo().getVideoQuality());
						v.download();// If this hangs, make sure you are using
										// vget 1.15. If you use 1.13 I know it
										// hangs
						System.out
								.println("downloadVideoInSeparateThread() - Download successful. Updating database");

						execute("start n=node({id}) WHERE n.url = {url} SET n.downloaded_video = {date}",
								ImmutableMap.<String, Object> of("id",
										Long.valueOf(id), "url", iVideoUrl,
										"date", System.currentTimeMillis()));
						System.out
								.println("downloadVideoInSeparateThread() - Download recorded in database");

					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					} catch (JSONException e) {
						System.out
								.println("downloadVideoInSeparateThread() - ERROR recording download in database");
						// Why won't the compiler let me throw this?
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			new Thread(r).start();
			System.out.println("End");
		}

		@GET
		@Path("surpassOrdinal")
		@Produces("application/json")
		public Response surpassOrdinal(
				@QueryParam("nodeIdToChange") Integer nodeIdToChange,
				@QueryParam("nodeIdToSurpass") Integer nodeIdToSurpass)
				throws IOException, JSONException {

			System.out.println("surpassOrdinals");

			Map<String, Object> theParams = new HashMap<String, Object>();
			theParams.put("nodeIdToChange", nodeIdToChange);
			theParams.put("nodeIdToSurpass", nodeIdToSurpass);

			JSONObject jsonObject = execute(
					" start n=node({nodeIdToChange}),n2=node({nodeIdToSurpass}) set n.ordinal=n2.ordinal + 100 return n.ordinal,n2.ordinal",
					theParams);

			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(jsonObject.toString()).type("application/json")
					.build();
		}

		@GET
		@Path("undercutOrdinal")
		@Produces("application/json")
		public Response undercutOrdinal(
				@QueryParam("nodeIdToChange") Integer nodeIdToChange,
				@QueryParam("nodeIdToUndercut") Integer nodeIdToUndercut)
				throws IOException, JSONException {

			System.out.println("undercutOrdinal");

			Map<String, Object> theParams = new HashMap<String, Object>();
			theParams.put("nodeIdToChange", nodeIdToChange);
			theParams.put("nodeIdToUndercut", nodeIdToUndercut);

			JSONObject jsonObject = execute(
					" start n=node({nodeIdToChange}),n2=node({nodeIdToUndercut}) set n.ordinal=n2.ordinal - 100 return n.ordinal,n2.ordinal",
					theParams);

			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(jsonObject.toString()).type("application/json")
					.build();

		}

		@GET
		@Path("swapOrdinals")
		@Produces("application/json")
		public Response swapOrdinals(@QueryParam("firstId") Integer iFirstId,
				@QueryParam("secondId") Integer iSecondId) throws IOException,
				JSONException {
			System.out.println("swapOrdinals");

			Map<String, Object> theParams = new HashMap<String, Object>();
			theParams.put("id1", iFirstId);
			theParams.put("id2", iSecondId);

			JSONObject jsonObject = execute(
					" start n=node({id1}),n2=node({id2}) set n.temp=n2.ordinal, n2.ordinal=n.ordinal,n.ordinal=n.temp  return n.ordinal,n2.ordinal",
					theParams);

			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(jsonObject.toString()).type("application/json")
					.build();
		}

		/**
		 * No existing relationships get deleted
		 */
		@GET
		@Path("relateCategoriesToItem")
		@Produces("application/json")
		public Response relateCategoriesToItem(
				@QueryParam("nodeId") Integer iNodeToBeTagged,
				@QueryParam("newCategoryIds") String iCategoriesToBeAddedTo)
				throws JSONException, IOException {
			System.out.println("relateCategoriesToItem(): begin");
			String decode = URLDecoder.decode(iCategoriesToBeAddedTo, "UTF-8");
			System.out.println(decode);
			JSONArray theCategoryIdsToBeAddedTo = new JSONArray(decode);
			for (int i = 0; i < theCategoryIdsToBeAddedTo.length(); i++) {
				Integer aCategoryIdToBeAddedTo = theCategoryIdsToBeAddedTo
						.getInt(i);
				System.out.println("relateCategoriesToItem(): "
						+ aCategoryIdToBeAddedTo + " --> " + iNodeToBeTagged);
				relateHelper(aCategoryIdToBeAddedTo, iNodeToBeTagged);
			}
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(new JSONObject().toString())
					.type("application/json").build();
		}

		/**
		 * This MOVES a node to a new subcategory. It deletes the relationship
		 * with the existing parent
		 */
		@GET
		@Path("relate")
		@Produces("application/json")
		public Response relate(@QueryParam("parentId") Integer iNewParentId,
				@QueryParam("childId") Integer iChildId,
				@QueryParam("currentParentId") Integer iCurrentParentId)
				throws JSONException, IOException {
			// first delete any existing contains relationship with the
			// specified existing parent (but not with all parents since we
			// could have a many-to-one contains)
			_1: {
				Map<String, Object> theParams = new HashMap<String, Object>();
				theParams.put("currentParentId", iCurrentParentId);
				theParams.put("childId", iChildId);

				execute("START oldParent = node({currentParentId}), child = node({childId}) MATCH oldParent-[r:CONTAINS]-child DELETE r",
						theParams);
				System.out
						.println("Finished trying to delete relationship between "
								+ iCurrentParentId + " and " + iChildId);
			}
			JSONObject theRelateOperationResponseJson;
			_2: {
				Map<String, Object> theParamValues = new HashMap<String, Object>();
				theParamValues.put("childId", iChildId);

				theRelateOperationResponseJson = relateHelper(iNewParentId,
						iChildId);
				System.out.println("Finished relating to new category");
			}

			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(theRelateOperationResponseJson.toString())
					.type("application/json").build();
		}

		/**
		 * No deletion of existing relationships occurs here.
		 * 
		 * @throws RuntimeException
		 *             - If the command fails. This could legitimately happen if
		 *             we try to relate to a newly created category if the
		 *             system becomes non-deterministic.
		 */
		private JSONObject relateHelper(Integer iParentId, Integer iChildId)
				throws IOException, JSONException {
			Map<String, Object> theParamValues = new HashMap<String, Object>();
			_1: {
				theParamValues.put("parentId", iParentId);
				theParamValues.put("childId", iChildId);
				theParamValues.put("currentTime", System.currentTimeMillis());
			}
			JSONObject theJson = execute(
					"start a=node({parentId}),b=node({childId}) CREATE a-[r:CONTAINS]->b SET b.accessed = {currentTime} return a,r,b;",
					theParamValues);
			return theJson;
		}

		// TODO: make this map immutable
		private static JSONObject execute(String iCypherQuery,
				Map<String, Object> iParams) throws IOException, JSONException {
			WebResource theWebResource = Client.create().resource(CYPHER_URI);
			Map<String, Object> thePostBody = new HashMap<String, Object>();
			thePostBody.put("query", iCypherQuery);
			thePostBody.put("params", iParams);
			// POST {} to the node entry point URI
			ClientResponse theResponse = theWebResource
					.accept(MediaType.APPLICATION_JSON)
					.type(MediaType.APPLICATION_JSON).entity("{ }")
					.post(ClientResponse.class, thePostBody);
			if (theResponse.getStatus() != 200) {
				System.out.println("failed: " + iCypherQuery + "\tparams: "
						+ iParams);
				throw new RuntimeException();
			}
			String theNeo4jResponse = IOUtils.toString(theResponse
					.getEntityInputStream());
			_1: {
				System.out.println(theNeo4jResponse);
				theResponse.getEntityInputStream().close();
				theResponse.close();
			}
			JSONObject oJson = new JSONObject(theNeo4jResponse);
			return oJson;
		}

		// ----------------------------------------------------------------------------
		// Read operations
		// ----------------------------------------------------------------------------

		@GET
		@Path("categoriesRecursive")
		@Produces("application/json")
		public Response categoriesRecursive(
				@QueryParam("parentId") Integer iParentId)
				throws JSONException, IOException {
			System.out.println("categoriesRecurisve() - begin");
			Map<String, Object> theParams = new HashMap<String, Object>();
			_1: {
				theParams.put("parentId", iParentId);
			}
			JSONObject theQueryJsonResult = execute(
					"START n=node(*) MATCH parent-[c:CONTAINS*]->n WHERE has(n.name) and n.type = 'categoryNode' and id(parent) = {parentId} RETURN distinct ID(n),n.name",
					theParams);
			JSONArray theData = (JSONArray) theQueryJsonResult.get("data");
			JSONArray oKeys = new JSONArray();
			for (int i = 0; i < theData.length(); i++) {
				System.out.println("categoriesRecurisve() - " + i);
				JSONObject aBindingObject = new JSONObject();
				_1: {
					JSONArray aBindingArray = theData.getJSONArray(i);
					String id = (String) aBindingArray.get(0);
					aBindingObject.put("id", id);
					String title = (String) aBindingArray.get(1);
					aBindingObject.put("name", title);
					oKeys.put(aBindingObject);
					System.out.println("categoriesRecurisve() - " + id
							+ "\t::\t" + title);
				}
			}
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(oKeys.toString()).type("application/json").build();
		}
	}

	public static void main(String[] args) throws URISyntaxException {
		JdkHttpServerFactory.createHttpServer(
				new URI("http://localhost:4447/"), new ResourceConfig(
						HelloWorldResource.class));
	}
}
