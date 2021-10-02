import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FuseException;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterFull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Multimap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class FuseYurl extends FuseFilesystemAdapterFull {

	private static final Integer ROOT_ID = 45;
	private static JSONObject items;
	private static String root;
	private static JSONObject categoriesTreeCache;
	private static Map<String, JSONObject> categoryPathsToItemsInCategory;
	private static Map<String, List<String>> categoryPathsToSubcategories;
	private static Map<String, String> categoryIdToName;
	private static BiMap<String, String> categoryPathToId;
	private static final Object JSON_OBJECT_NULL = new Null();

	public static void main(String... args) throws FuseException {
		System.out.println("FuseYurl.main() args = " + args);
		// Strange - groovy ignores arg1's hardcoding. Maybe it's not an
		// acceptable array initialization in groovy?
		final String[] args1 = args;
		if (args.length != 1) {
			System.err.println("Usage: HelloFS <mountpoint>");
			System.exit(1);
		}
		try {
			if (FuseYurl.categoriesTreeCache == null) {
				FuseYurl.categoriesTreeCache = CategoryTree.getCategoriesTree(ROOT_ID);
				FuseYurl.categoryPathsToSubcategories = CategoryPathsToSubcategories.build(FuseYurl.categoriesTreeCache, "");
				FuseYurl.categoryIdToName = CategoryIdToName.build(FuseYurl.categoriesTreeCache, "");
				FuseYurl.categoryPathToId = CategoryPathToId.build(FuseYurl.categoriesTreeCache, "");
			}
			items = Yurl.getItemsAtLevelAndChildLevels(ROOT_ID);
			FuseYurl.categoryPathsToItemsInCategory = CategoryPathsToItems
					.build(items, "", FuseYurl.categoryIdToName);
//			System.out.println("FuseYurl.main() items = " + items);
//			System.out.println("FuseYurl.main() categoriesTreeCache = " + FuseYurl.categoriesTreeCache);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		root = args[0];
		new FuseYurl().log(false).mount(args[0]);
		try {
			Files.createDirectory(Paths.get(args[0]));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("FuseYurl.main() finished mounting, ready. " + args[0]);
	}

	private static class CategoryPathsToItems {
		static Map<String, JSONObject> build(JSONObject items, String path, Map<String, String> categoryIdToName) {
			Map<String, JSONObject> ret = new HashMap<String, JSONObject>();
			for (String categoryId : items.keySet()) {
				String categoryName = categoryIdToName.get(categoryId);
//				System.out.println("FuseYurl.CategoryPathsToItems.build() - categoryId = "
//						+ categoryId + " (" + categoryName + ")");
				String key = path + "/" + categoryName;
				if (!FuseYurl.categoryPathToId.keySet().contains(key)) {
					key = FuseYurl.categoryPathToId.inverse().get(categoryId);
				}
System.out.println("FuseYurl.CategoryPathsToItems.build() - path = " + key);
				if (key.startsWith("/root/products and services/DVD and video/Soccer")) {
					System.out.println("FuseYurl.CategoryPathsToItems.build()");
				}
				if (categoryId.equals(ROOT_ID)) {
					ret.put(key, items);
				} else {
//					System.out.println("FuseYurl.CategoryPathsToItems.build() - " + items.get(cate));
					JSONArray arr = items.getJSONArray(categoryId);
					JSONObject o = new JSONObject();
					o.put(categoryId, arr);
					ret.put(key, o);
				}
				
			}
			return ret;
		}
	}

	final String contents = "Hello World!\n";

	@Override
	public int getattr(final String path, final StatWrapper stat) {
//		System.out.println("FuseYurl.getattr()");
//		System.out.println("FuseYurl.getattr() path = " + path);
//		System.out.println("FuseYurl.getattr() Paths.get(path).getFileName() = " + Paths.get(path).getFileName());
//		System.out.println("FuseYurl.getattr() files = " + files(items));
		
		if ("._.".equals(path)) {
			return -ErrorCodes.ENOENT();
		}
		if ("/".equals(path)) {
			stat.setMode(NodeType.DIRECTORY);
			return 0;
		}
		
		String f = Paths.get(path).getFileName().toString();
		boolean contains = files(FuseYurl.items).contains(f);
		try {
			if (contains) {
				stat.setMode(NodeType.FILE).size(contents.length());
				return 0;
			}
			Path fileName2 = Paths.get(path).getFileName();
			Path fileName3 = Paths.get(root).getFileName();
			// Root directory
			if (path.equals(File.separator) || fileName2.equals(fileName3) || dirs(FuseYurl.categoriesTreeCache).contains(f) || 
					 isCategory(path)
					) { 
				stat.setMode(NodeType.DIRECTORY);
				return 0;
			} else {
				checkNotNull(fileName2);
				checkNotNull(fileName3);
//				if (contains) {
					stat.setMode(NodeType.FILE).size(contents.length());
					return 0;
//				}
			}
//			System.err.println("FuseYurl.getattr()" + path);
//			return -ErrorCodes.ENOENT();
		} catch (Exception e) {
			e.printStackTrace();
			return -ErrorCodes.ENOENT();
		}
	}

	private boolean isCategory(String path) {
		return FuseYurl.categoryPathsToSubcategories.keySet().contains(path);
	}

	@Override
	public int read(final String path, final ByteBuffer buffer, final long size, final long offset,
			final FileInfoWrapper info) {
		// Compute substring that we are being asked to read
		final String s = contents.substring((int) offset,
				(int) Math.max(offset, Math.min(contents.length() - offset, offset + size)));
		buffer.put(s.getBytes());
		return s.getBytes().length;
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler) {
		if ("/".equals(path)) {
			List<String> l = new LinkedList<String>();
			l.add("root");//dirs(FuseYurl.categoriesTreeCache));
			filler.add(l);
			System.out.println("FuseYurl.readdir() items = " + l);
			return 0;
		} else {
//			List<String> l = new LinkedList<String>();
			List<String> files = files(getItems(path));
			int max = Integer.MAX_VALUE;// 3155 too large (but later ok)
			// 3100 ok
//			if (files.size() > max) {
//				System.out.println("FuseYurl.readdir() truncating");
//				filler.add(files.subList(0, max));
//			} else {
//				System.err.println("FuseYurl.readdir() not truncating");
			System.out.println("FuseYurl.readdir() files.size = " + files.size());
//			StringBuffer all = new StringBuffer();
				int i = 1;
				for (String f : files) {
					if (f.trim().length() == 0) {
						continue;
					}
					String string = i + "  " + f.substring(0,Math.min(f.length(),230));
					++i;
					if (!filler.add(string)) {
						System.err.println("FuseYurl.readdir() - failed" + string);
						return ErrorCodes.ENOMEM();
					}
//					all.append(string).append("\n");
//					System.out.println("FuseYurl.readdir() " + string);
				}
//				l.addAll(files.subList(0, files.size()* 2/3));
//			}
			System.out.println("FuseYurl.readdir() files = " + files.size());
//			try {
//				FileUtils.write(Paths.get("/Users/sarnobat/trash/yurl_titles.txt").toFile(), all.toString());
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			System.out.println("FuseYurl.readdir() 1 - " + l.size());
			if (path.endsWith("root")) {
				List<String> l2 = new LinkedList<String>();
//				for (int i = 0; i < 3000; i++) {
//					l2.add(i + "sridhar.txt");
//				}
//				l.addAll(l2);
			}
//			System.out.println("FuseYurl.readdir() 2 - " + l.size());
			// Add subdirectories
			List<String> cs = categoryPathsToSubcategories.get(path);
			if (cs.size() == 0) {
				System.err.println("FuseYurl.readdir() No subcategories under " + path);
			}
			int k = 1;
			for (String c : cs) {
				String cf = k +"::"+c;
				++k;
//				System.out.println("FuseYurl.readdir() " + cf);
				if (!filler.add(c)) {
//					System.err.println("FuseYurl.readdir() - problem adding categories");
					return ErrorCodes.ENOMEM();
				}
				
			}
			return 0;
		}
	}

	private JSONObject getItems(final String path) {
		JSONObject items = FuseYurl.categoryPathsToItemsInCategory.get(path);
		if (items == null) {
			
			// TODO: Move this to a sooner point?
			// populate
			try {
				JSONObject ite = Yurl.getItemsAtLevelAndChildLevels(Integer
						.parseInt(FuseYurl.categoryPathToId.get(path)));
				Map<String, JSONObject> build = CategoryPathsToItems.build(ite, path,
						FuseYurl.categoryIdToName);
				FuseYurl.categoryPathsToItemsInCategory.putAll(build);
				items = FuseYurl.categoryPathsToItemsInCategory.get(path);
				if (items == null) {
					System.out.println("FuseYurl.readdir() no items for path " + path);
					items = new JSONObject();
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return items;
	}

	private static List<String> files(JSONObject items) {
		List<String> l = new LinkedList<String>();
		checkNotNull(items);
		if (items.keySet().size() > 0) {
			String next = (String) items.keySet().iterator().next();
			JSONArray a = items.getJSONArray(next);
			for (int i = 0; i < a.length(); i++) {
				JSONObject o = a.getJSONObject(i);
				if (o.has("title") && o.getString("title").length() > 0) {
					l.add(o.getString("title"));
//					System.out.println("FuseYurl.files() title = " + o.getString("title"));
				}
			}
		}
		return ImmutableList.copyOf(l);
	}

	private static List<String> dirs(JSONObject categoriesTreeCache) {
		JSONArray a = categoriesTreeCache.getJSONArray("children");
		List<String> l = new LinkedList<String>();
		for (int i = 0; i < a.length(); i++) {
			JSONObject o = a.getJSONObject(i);
			if (o.has("name") && o.getString("name").length() > 0) {
				l.add(o.getString("name"));
			}
		}
		return ImmutableList.copyOf(l);
	}

	private static class Yurl {

		private static final String CYPHER_URI = "http://netgear.rohidekar.com:7474/db/data/cypher";

		private static boolean isNotNull(Object val) {
			return val != null && !("null".equals(val)) && !(val.getClass().equals(JSON_OBJECT_NULL));
		}

		static JSONObject getItemsAtLevelAndChildLevels(Integer iRootId) throws JSONException,
				IOException {
			
			// TODO: the source is null clause should be obsoleted
			JSONObject theQueryResultJson = FuseYurl.Yurl.execute(
					"START source=node({rootId}) "
							+ "MATCH p = source-[r:CONTAINS*1..1]->u "
							+ "WHERE has(u.title) and (source is null or ID(source) = {rootId}) and not(has(u.type)) AND id(u) > 0  "
							+ "RETURN distinct ID(u),u.title,u.url, extract(n in nodes(p) | id(n)) as path,u.downloaded_video,u.downloaded_image,u.created,u.ordinal, u.biggest_image, u.user_image "
							// TODO : do not hardcode the limit to 500. Category
							// 38044 doesn't display more than 50 books since
							// there are so many child items.
							+ " LIMIT 90000", ImmutableMap
							.<String, Object> builder().put("rootId", iRootId).build(),
					"getItemsAtLevelAndChildLevels()");
			JSONArray theDataJson = (JSONArray) theQueryResultJson.get("data");
			JSONArray theUncategorizedNodesJson = new JSONArray();
			System.err.println("FuseYurl.Yurl.getItemsAtLevelAndChildLevels() - ("+iRootId+")\tresults = " + theDataJson.length());
			for (int i = 0; i < theDataJson.length(); i++) {
				JSONObject anUncategorizedNodeJsonObject = new JSONObject();
				_1: {
					JSONArray anItem = theDataJson.getJSONArray(i);
					_11: {
						Object object = anItem.get(0);
						String anId = toString(object);
						anUncategorizedNodeJsonObject.put("id", anId);
					}
					_12: {
						String aTitle = (String) anItem.get(1);
						anUncategorizedNodeJsonObject.put("title", aTitle);
					}
					_13: {
						String aUrl = (String) anItem.get(2);
						anUncategorizedNodeJsonObject.put("url", aUrl);
					}
					_14: {
						try {
							JSONArray path = (JSONArray) anItem.get(3);
							if (path.length() == 3) {
								anUncategorizedNodeJsonObject.put("parentId", path.get(1));
							} else if (path.length() == 2) {
								anUncategorizedNodeJsonObject.put("parentId", iRootId);
							} else if (path.length() == 1) {
								// This should never happen
								anUncategorizedNodeJsonObject.put("parentId", path.get(0));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					_15: {

						Object val = anItem.get(4);
						if (isNotNull(val)) {
							String aValue = toString(val);
							anUncategorizedNodeJsonObject.put("downloaded_video", aValue);
						}
					}
					_16: {
						Object val = anItem.get(6);
						if ("null".equals(toString(val))) {
							System.out.println("Is null value");
						} else if (val == null) {
							System.out.println("Is null string");
						} else if (isNotNull(val)) {
							Long aValue = (Long) val;
							anUncategorizedNodeJsonObject.put("created", aValue);
						}
					}
					_17: {
						Object val = anItem.get(8);
						if (isNotNull(val)) {
							String aValue = toString(val);
							anUncategorizedNodeJsonObject.put("biggest_image", aValue);
						}
					}
					_18: {
						Object val = anItem.get(9);
						if (isNotNull(val)) {
							String aValue = toString(val);
							anUncategorizedNodeJsonObject.put("user_image", aValue);
						}
					}
				}
				theUncategorizedNodesJson.put(anUncategorizedNodeJsonObject);
			}

			JSONObject ret = new JSONObject();
			transform: {
				for (int i = 0; i < theUncategorizedNodesJson.length(); i++) {
					JSONObject jsonObject = (JSONObject) theUncategorizedNodesJson.get(i);
					String parentId = toString(jsonObject.get("parentId"));
					if (!ret.has(parentId)) {
						ret.put(parentId, new JSONArray());
					}
					JSONArray target = (JSONArray) ret.get(parentId);
					target.put(jsonObject);
				}
			}
			return ret;
		}

		private static String toString(Object object) {
			String anId;// = (String) anItem.get(0);
			if (object instanceof Integer) {
				anId = ((Integer) object).toString();
			} else if (object instanceof Long) {
				anId = ((Long) object).toString();
			} else if (object instanceof String) {
				anId = (String) object;
			} else {
				anId = object.toString();
			}
			return anId;
		}

		private static JSONObject execute(String iCypherQuery, Map<String, Object> iParams,
				String... iCommentPrefix) throws IOException, JSONException {
			String commentPrefix = iCommentPrefix.length > 0 ? iCommentPrefix[0] + " " : "";
			// System.out.println(commentPrefix + "begin");
			System.out
					.println(commentPrefix + " - \n\t" + iCypherQuery + "\n\tparams - " + iParams);

			ClientConfig clientConfig = new DefaultClientConfig();
			clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

			// POST {} to the node entry point URI
			ClientResponse theResponse = Client
					.create(clientConfig)
					.resource(CYPHER_URI)
					.accept(MediaType.APPLICATION_JSON)
					.type(MediaType.APPLICATION_JSON)
					.entity("{ }")
					.post(ClientResponse.class,
							ImmutableMap.<String, Object> of("query", iCypherQuery, "params",
									Preconditions.checkNotNull(iParams)));
			if (theResponse.getStatus() != 200) {
				System.out.println(commentPrefix + "FAILED:\n\t" + iCypherQuery + "\n\tparams: "
						+ iParams);
				throw new RuntimeException(IOUtils.toString(theResponse.getEntityInputStream()));
			}
			String theNeo4jResponse;
			_1: {
				// Do not inline this. We need to close the stream after copying
				theNeo4jResponse = IOUtils.toString(theResponse.getEntityInputStream());
				theResponse.getEntityInputStream().close();
				theResponse.close();
			}
			System.out.println(commentPrefix + "end");
			return new JSONObject(theNeo4jResponse);
		}

	}

	private static class CategoryPathToId {
		public static BiMap<String, String> build(JSONObject categoriesTreeCache, String prefix) {
			String categoryName = categoriesTreeCache.getString("name");
			BiMap<String, String> ret = HashBiMap.create();
			String key = prefix + "/" + categoryName;
			String id =  Integer.toString(categoriesTreeCache.getInt("id"));
			// Immediate children
			ret.put(key, id);
			// recursive
			JSONArray a = categoriesTreeCache.getJSONArray("children");
			for (int i = 0; i < a.length(); i++) {
				JSONObject subdir = a.getJSONObject(i);
				ret.putAll(build(subdir, key));
			}
			return ret;
		}	
	}
	private static class CategoryIdToName {

		public static Map<String, String> build(JSONObject categoriesTreeCache, String prefix) {
			String categoryName = categoriesTreeCache.getString("name");
			Map<String, String> ret = new HashMap<String, String>();
			String key = prefix + "/" + categoryName;
			String id =  Integer.toString(categoriesTreeCache.getInt("id"));
			// Immediate children
			ret.put(id, categoryName);
			// recursive
			JSONArray a = categoriesTreeCache.getJSONArray("children");
			for (int i = 0; i < a.length(); i++) {
				JSONObject subdir = a.getJSONObject(i);
				ret.putAll(build(subdir, key));
			}
//			if ("".equals(prefix)) {
//				ret.put("", categoryName);
//			}
			return ret;
		}	
	}

	private static class CategoryPathsToSubcategories {
	
		public static Map<String, List<String>> build(JSONObject categoriesTreeCache, String prefix) {
			String categoryName = categoriesTreeCache.getString("name");
			JSONArray a = categoriesTreeCache.getJSONArray("children");
			List<String> immediateChildrenNames = getNames(a);
			Map<String, List<String>> ret = new HashMap<String, List<String>>();
			String key = prefix + "/" + categoryName;
			// Immediate children
			ret.put(key, immediateChildrenNames);
			// recursive
			for (int i = 0; i < a.length(); i++) {
				JSONObject subdir = a.getJSONObject(i);
				ret.putAll(build(subdir, key));
			}
			if ("".equals(prefix)) {
				ret.put("", ImmutableList.of(categoryName));
			}
			return ret;
		}

		private static List<String> getNames(JSONArray a) {
			List<String> immediateChildrenNames = new LinkedList<String>();
			for (int i = 0; i < a.length(); i++) {
				JSONObject subdir = a.getJSONObject(i);
				immediateChildrenNames.add(subdir.getString("name"));
			}
			return immediateChildrenNames;
		}
	}

	private static class CategoryTree {
		static JSONObject getCategoriesTree(Integer rootId) throws JSONException, IOException {
			return new AddSizes(
			// This is the expensive query, not the other one
					getCategorySizes(FuseYurl.Yurl.execute(
							"START n=node(*) MATCH n-->u WHERE has(n.name) "
									+ "RETURN id(n),count(u);",
							ImmutableMap.<String, Object> of(),
							"getCategoriesTree() [The expensive query]").getJSONArray("data")))
					.apply(
					// Path to JSON conversion done in Cypher
					createCategoryTreeFromCypherResultPaths(
							// TODO: I don't think we need each path do we?
							// We
							// just need each parent-child relationship.
							FuseYurl.Yurl.execute("START n=node({parentId}) "
									+ "MATCH path=n-[r:CONTAINS*]->c " + "WHERE has(c.name) "
									+ "RETURN extract(p in nodes(path)| " + "'{ "
									+ "id : ' + id(p) + ', " + "name : \"'+ p.name +'\" , "
									+ "key : \"' + coalesce(p.key, '') + '\"" + " }'" + ")",
									ImmutableMap.<String, Object> of("parentId", rootId),
									"getCategoriesTree() - [getting all paths 3]"), rootId));
		}
	
		private static Map<Integer, Integer> getCategorySizes(JSONArray counts) {
			Map<Integer, Integer> sizesMap = new HashMap<Integer, Integer>();
			for (int i = 0; i < counts.length(); i++) {
				JSONArray row = counts.getJSONArray(i);
				sizesMap.put(row.getInt(0), row.getInt(1));
			}
			return ImmutableMap.copyOf(sizesMap);
		}
	
		private static JSONObject addSizesToCypherJsonResultObjects(JSONObject categoriesTree,
				Map<Integer, Integer> categorySizes) {
			Integer id = categoriesTree.getInt("id");
			categoriesTree.put("size", categorySizes.get(id));
			if (categoriesTree.has("children")) {
				JSONArray children = categoriesTree.getJSONArray("children");
				for (int i = 0; i < children.length(); i++) {
					addSizesToCypherJsonResultObjects(children.getJSONObject(i), categorySizes);
				}
			}
			return categoriesTree;
		}
	
		private static class AddSizes implements Function<JSONObject, JSONObject> {
			private final Map<Integer, Integer> categorySizes;
	
			AddSizes(Map<Integer, Integer> categorySizes) {
				this.categorySizes = categorySizes;
			}
	
			@Override
			public JSONObject apply(JSONObject input) {
				return addSizesToCypherJsonResultObjects(input, categorySizes);
			}
		}
	
		private static JSONObject createCategoryTreeFromCypherResultPaths(
				JSONObject theQueryJsonResult, Integer rootId) {
			JSONArray cypherRawResults = theQueryJsonResult.getJSONArray("data");
			checkState(cypherRawResults.length() > 0);
			Multimap<Integer, Integer> parentToChildren = buildParentToChildMultimap2(cypherRawResults);
			Map<Integer, JSONObject> categoryNodesWithoutChildren = createId(cypherRawResults);
			JSONObject root = categoryNodesWithoutChildren.get(rootId);
			root.put(
					"children",
					toJsonArray(buildChildren(parentToChildren.get(rootId),
							categoryNodesWithoutChildren, parentToChildren)));
			return root;
		}
	
		/**
		 * @return - a pair for each category node
		 */
		private static Map<Integer, JSONObject> createId(JSONArray cypherRawResults) {
			ImmutableMap.Builder<Integer, JSONObject> idToJsonBuilder = ImmutableMap
					.<Integer, JSONObject> builder();
			Set<Integer> seen = new HashSet<Integer>();
			for (int i = 0; i < cypherRawResults.length(); i++) {
				JSONArray treePath = cypherRawResults.getJSONArray(i).getJSONArray(0);
				for (int j = 0; j < treePath.length(); j++) {
					if (treePath.get(j).getClass().equals(JSON_OBJECT_NULL)) {
						continue;
					}
					JSONObject pathHopNode = new JSONObject(treePath.getString(j));// treePath.getString(j));
					int categoryId = pathHopNode.getInt("id");
					if (!seen.contains(categoryId)) {
						seen.add(categoryId);
						idToJsonBuilder.put(categoryId, pathHopNode);
					}
				}
			}
			return idToJsonBuilder.build();
		}
	
		private static JSONArray removeNulls(JSONArray iJsonArray) {
			for (int i = 0; i < iJsonArray.length(); i++) {
				if (JSON_OBJECT_NULL.equals(iJsonArray.get(i))) {
					iJsonArray.remove(i);
					--i;
				}
			}
			return iJsonArray;
		}
	
		/**
		 * @return Integer to set of Integers
		 */
		private static Multimap<Integer, Integer> buildParentToChildMultimap2(
				JSONArray cypherRawResults) {
			Multimap<Integer, Integer> oParentToChildren = HashMultimap.create();
			getParentChildrenMap: {
				for (int pathNum = 0; pathNum < cypherRawResults.length(); pathNum++) {
					JSONArray categoryPath = removeNulls(cypherRawResults.getJSONArray(pathNum)
							.getJSONArray(0));
					for (int hopNum = 0; hopNum < categoryPath.length() - 1; hopNum++) {
						if (categoryPath.get(hopNum).getClass().equals(JSON_OBJECT_NULL)) {
							continue;
						}
						if (categoryPath.get(hopNum + 1).getClass().equals(JSON_OBJECT_NULL)) {
							continue;
						}
						if (!(categoryPath.get(hopNum + 1) instanceof String)) {
							continue;
						}
						int childId = new JSONObject(categoryPath.getString(hopNum + 1))
								.getInt("id");
						int parentId = checkNotNull(new JSONObject(
								categoryPath.getString(hopNum)).getInt("id"));
						Object childrenObj = oParentToChildren.get(parentId);
						if (childrenObj != null) {
							Set<?> children = (Set<?>) childrenObj;
							if (!children.contains(childId)) {
								oParentToChildren.put(parentId, childId);
							}
						} else {
							oParentToChildren.put(parentId, childId);
						}
					}
				}
			}
			return oParentToChildren;
		}
	
		private static JSONArray toJsonArray(Collection<JSONObject> children) {
			JSONArray arr = new JSONArray();
			for (JSONObject child : children) {
				arr.put(child);
			}
			return arr;
		}
	
		private static Set<JSONObject> buildChildren(Collection<Integer> childIds,
				Map<Integer, JSONObject> nodes, Multimap<Integer, Integer> parentToChildren) {
			Builder<JSONObject> set = ImmutableSet.builder();
			for (int childId : childIds) {
				JSONObject childJson = nodes.get(childId);
				Collection<Integer> grandchildIds = parentToChildren.get(childId);
				Collection<JSONObject> grandchildNodes = buildChildren(grandchildIds, nodes,
						parentToChildren);
				JSONArray grandchildrenArray = toJsonArray(grandchildNodes);
				childJson.put("children", grandchildrenArray);
				set.add(childJson);
			}
			return set.build();
		}
	}

	private static final class Null {
		@Override
		protected final Object clone() {
			return this;
		}
	
		@Override
		public boolean equals(Object object) {
			return object == null || object == this;
		}
	
		public String toString() {
			return "null";
		}
	}
}
