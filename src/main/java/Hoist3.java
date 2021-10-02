import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Sets.SetView;
import com.sun.net.httpserver.HttpServer;

public class Hoist3 {
	@Path("hoist3")
	public static class HelloWorldResource { // Must be public

		@GET
		@Path("list2")
		@Produces("application/json")
		public Response json(@QueryParam("dirs") String iDirs) throws JSONException {
			System.out.println("1 - " + iDirs);
			JsonObject json = getFiles(iDirs);
			System.out.println("3");
			return Response.ok().header("Access-Control-Allow-Origin", "*").entity(json.toString())
					.type("application/json").build();
		}

		public static JsonObject getFiles(String iDirs) {
			String[] theDirs = iDirs.split("\n");
			JsonObjectBuilder json = Json.createObjectBuilder();
			System.out.println("2 - " + ImmutableList.copyOf(theDirs));
			List<Entry<String, JsonObject>> l = FluentIterable.from(ImmutableList.copyOf(theDirs))
					.transform(DIR_TO_JSON).toList();
			// json.add("foo", "bar");
			for (Entry<String, JsonObject> e : l) {
				json.add(e.getKey(), e.getValue());
			}
			return json.build();
		}

		private static final Function<String, Entry<String, JsonObject>> DIR_TO_JSON = new Function<String, Entry<String, JsonObject>>() {
			@Override
			public @Nullable
			Entry<String, JsonObject> apply(String iDir2) {
				String iDir = iDir2.endsWith("/") ? iDir2 : iDir2 + "/";
				JsonObjectBuilder theDirJson = Json.createObjectBuilder();

				File theDir = Paths.get(iDir).toFile();
				if (!theDir.isDirectory()) {
					System.err.println("DIR_TO_JSON..apply() - skipping non-dir " + iDir);
					return null;
				}

				File metadataDir = new File(theDir, ".metadata");
				if (!metadataDir.exists()) {
					// create metadata directory
					metadataDir.mkdir();
				}

				File fileListTxt = new File(metadataDir, "files.txt");

				ensureFileExists(fileListTxt);

				// Determine what files are new
				String before = readFileToString(fileListTxt);

				// Add the new files to file.txt
				SetView<String> difference = Sets.difference(ImmutableSet.copyOf(getFiles(iDir)),
						ImmutableSet.copyOf(before.split("\\n")));
				List<String> s = new LinkedList<String>(difference);
				s.remove(iDir + ".metadata");
				System.out.println("Difference size = " + s.size());
				// s.add("NEW_FILES");
				String join = Joiner.on("\n").join(difference);
				System.out.println("Before substitution:\t" + join);
				String fullList = before.replace("NEW_FILES", join + "\nNEW_FILES");
				System.out.println("After substitution:\t" + fullList);
				System.out
						.println("Hoist3.HelloWorldResource.DIR_TO_JSON.new Function() {...}.apply()");

				String[] split = fullList.split("\n");
				System.out.println("Split size = " + split.length);
				List<String> allFiles = ImmutableList.copyOf(split);
				System.out.println("All files size = " + allFiles.size());

//				allFiles.remove(iDir + ".metadata");
				// Write the complete listing back out to files.txt
				try {
					FileUtils.writeLines(fileListTxt, allFiles);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				System.out.println("DIR_TO_JSON.apply() - 1");
				if (!allFiles.contains("NEW_FILES")) {
					System.out.println("DIR_TO_JSON.apply() - 1.1");
					throw new RuntimeException("Developer error - no NEW_FILES line");

				}
				System.out.println("DIR_TO_JSON.apply() - 1.2");
				List<String> allFiles2 = new LinkedList<String>(allFiles);
				allFiles2.remove("NEW_FILES");
				String o = iDir + ".metadata";
				if (!allFiles2.contains(o)) {
					throw new RuntimeException("no .metadata");
				}
				allFiles2.remove(o);
				System.out.println("DIR_TO_JSON.apply() - 2");
				JsonArrayBuilder a3 = Json.createArrayBuilder();
				for (String a : allFiles2) {
					System.out.println(a);
					if (a.trim().equals("")) {
						System.err.println("Don't let empty strings in");
					} else if (a.trim().equals("")) {
						a.endsWith(".metadata");
					} else {
						a3.add(a);
					}
				}
				System.out.println("DIR_TO_JSON.apply() - 3");
				theDirJson.add("files", a3.build());
				System.out.println("DIR_TO_JSON.apply() - 4");
				theDirJson.add("dirs", Json.createObjectBuilder().build());
				return new AbstractMap.SimpleEntry<String, JsonObject>(iDir, theDirJson.build());
			}

			private void ensureFileExists(File fileListTxt) {
				if (!fileListTxt.exists()) {
					// create files.txt
					try {
						FileUtils.write(fileListTxt, "NEW_FILES");
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}

			private String readFileToString(File fileListTxt) {
				String filesInTxt;
				if (fileListTxt.exists()) {
					try {
						filesInTxt = FileUtils.readFileToString(fileListTxt);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else {
					throw new RuntimeException("Please ensure the file exists before reading.");
				}
				return filesInTxt;
			}

			private List<String> getFiles(String iDir) {
				List<String> filesInDirList;
				{
					DirectoryStream<java.nio.file.Path> newDirectoryStream;
					try {
						newDirectoryStream = Files.newDirectoryStream(Paths.get(iDir));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					ImmutableList.Builder<String> filesInDir = ImmutableList.builder();
					for (Iterator<java.nio.file.Path> it = newDirectoryStream.iterator(); it
							.hasNext();) {
						filesInDir.add(it.next().toAbsolutePath().toString());
					}
					filesInDirList = filesInDir.build();
				}
				return filesInDirList;
			}
		};
	}

	public static void main(String[] args) throws URISyntaxException {

		// JsonObject o =
		// HelloWorldResource.getFiles("/sarnobat.garagebandbroken/Desktop/new_do_not_sort/photos/2015-11_india/Ketki_Vallabh/");
		// System.out.println("Output:");
		// System.out.println(o);
		HttpServer server = JdkHttpServerFactory.createHttpServer(
				new URI("http://localhost:4464/"), new ResourceConfig(HelloWorldResource.class));
	}
}
