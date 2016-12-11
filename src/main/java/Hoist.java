import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.SizeFileComparator;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpServer;

public class Hoist {
	@javax.ws.rs.Path("helloworld")
	public static class HelloWorldResource { // Must be public

		private static final String HIGHER_RANK = "_+1";
		private static final String LOWER_RANK = "_-1";
		private static final Collection<String> IGNORED_FILES = new LinkedHashSet<String>() ;
//		{
//			{
//				add(".DS_Store");
//				add(".picasa.ini");
//			}
//		};

		//
		// Read-only operations
		//
		@GET
		@javax.ws.rs.Path("json")
		@Produces("application/json")
		public Response json(@QueryParam("dir") String iPath) throws JSONException,
				UnsupportedEncodingException {
			System.out.println("getImages() - " + iPath);
			String dir = URLDecoder.decode(iPath, "UTF-8");
			Collection<File> files = FileUtils.listFiles(new File(dir), new IOFileFilter() {

				public boolean accept(File file) {
					// TODO: This isn't working
					if (IGNORED_FILES.contains(file.getName())) {
						return false;
					}
					return true;
				}

				public boolean accept(File dir2, String name) {
					return false;
				}
			}, null);
			List<File> fileList = new LinkedList<File>(files);
			Collections.sort(fileList, SizeFileComparator.SIZE_REVERSE);
			System.out.println(files.size());
			JSONObject outerJson = new JSONObject();

			// Level zero
			JSONArray jsonLevel0 = new JSONArray();
			for (Object o : fileList) {
				File f = (File) o;
try {
	JSONObject fileJson = new JSONObject();
	fileJson.put("webLink", httpLinkFor(f.getAbsolutePath().toString()));
	fileJson.put("localPath", f.getAbsolutePath().toString());
				jsonLevel0.put(fileJson);
} catch (Exception e) {
System.err.println(e);
e.printStackTrace();
}
				System.out.println(f.getAbsolutePath());
			}
			outerJson.put("0", jsonLevel0);
			// Upper levels
			String inner = dir + "/_+1";
			File innerDir = new File(inner);
			if (innerDir.exists()) {
				getImagesAtLevel(innerDir, outerJson, 1, 1);
			}
			// Lower levels
			String innerMinus = dir + "/_-1";
			File innerMinusDir = new File(innerMinus);
			if (innerMinusDir.exists()) {
				getImagesAtLevel(innerMinusDir, outerJson, -1, -1);
			}
			System.out.println(outerJson.toString());
			return Response.ok().header("Access-Control-Allow-Origin", "*")
					.entity(outerJson.toString()).type("application/json").build();
		}

		private void getImagesAtLevel(File dir, JSONObject outerJson, int level, int increment)
				throws JSONException {

			Collection<File> files = FileUtils.listFiles(dir, new IOFileFilter() {

				public boolean accept(File file) {
					return true;
				}

				public boolean accept(File dir3, String name) {
					return false;
				}
			}, null);

			List<File> fileList = new LinkedList<File>(files);
			Collections.sort(fileList, SizeFileComparator.SIZE_REVERSE);
			JSONArray json = new JSONArray();
			for (Object o : fileList) {
				File f = (File) o;
try {
        JSONObject fileJson = new JSONObject();
        fileJson.put("webLink", httpLinkFor(f.getAbsolutePath().toString()));
        fileJson.put("localPath", f.getAbsolutePath().toString());
                                json.put(fileJson);
} catch (Exception e) {
System.err.println(e);
e.printStackTrace();
}
				System.out.println(f.getAbsolutePath());
			}
			outerJson.put(((Integer) level).toString(), json);

			// TODO: recurse
			String incrementStr = "-1";
			if (increment == 1) {
				incrementStr = "+1";
			}
			String incrementStrFull = "/_" + incrementStr;
			String inner = dir.getAbsolutePath() + incrementStrFull;
			File innerDir2 = new File(inner);
			if (innerDir2.exists()) {
				getImagesAtLevel(innerDir2, outerJson, level + increment, increment);
			}
		}

		//
		// Write operations
		//

		@GET
		@javax.ws.rs.Path("moveUp")
		@Produces("application/json")
		public Response moveUp(@QueryParam("path") String iPath) throws JSONException, IOException {
			System.out.println("moveUp() - " + iPath);

			String targetSubdirName = HIGHER_RANK;

			moveFileToSubfolder(iPath, targetSubdirName);
			JSONObject json = new JSONObject();
			System.out.println("moveUp() - end");
			return Response.ok().header("Access-Control-Allow-Origin", "*").entity(json.toString())
					.type("application/json").build();
		}

		@GET
		@javax.ws.rs.Path("moveDown")
		@Produces("application/json")
		public Response moveDown(@QueryParam("path") String iPath) throws JSONException {
			System.out.println("moveDown() - " + iPath);
			String targetSubdirName = LOWER_RANK;
			File fileToMove = new File(iPath);
			String higherRank = "_+";
try {
			if (fileToMove.getParentFile().getName().startsWith(higherRank)) {
				moveToParentDir(iPath);
			} else {
				moveFileToSubfolder(iPath, targetSubdirName);
			}
} catch (Exception e) {
	e.printStackTrace();
}
			JSONObject json = new JSONObject();
			System.out.println("moveUp() - end");
			return Response.ok().header("Access-Control-Allow-Origin", "*").entity(json.toString())
					.type("application/json").build();
		}

		private static void moveToParentDir(String sourceFilePathString) {
			System.out.println("moveToParentDir() - begin " + sourceFilePathString);
                        if (sourceFilePathString.endsWith("htm") || sourceFilePathString.endsWith(".html")) {
                                throw new RuntimeException("Need to move the _files folder too");
                        }
			try {
                        doMoveToParent(sourceFilePathString);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

   		private static void doMoveToParent(String sourceFilePathString)
                                throws IllegalAccessError {
                        java.nio.file.Path sourceFilePath = Paths.get(sourceFilePathString);
                        java.nio.file.Path destinationFile = getDestinationFilePathAvoidingExisting(sourceFilePath);
                        doMove(sourceFilePath, destinationFile);
                        System.out.println("File now resides at "
                                        + destinationFile.toAbsolutePath().toString());
                }

                private static java.nio.file.Path getDestinationFilePathAvoidingExisting(java.nio.file.Path sourceFile)
                                throws IllegalAccessError {
                        java.nio.file.Path destinationFile;
                        _1: {

                                String filename = sourceFile.getFileName().toString();
                                java.nio.file.Path parent = sourceFile.getParent().getParent().toAbsolutePath();
                                String parentPath = parent.toAbsolutePath().toString();
                                String destinationFilePath = parentPath + "/" + filename;
                                destinationFile = determineDestinationPathAvoidingExisting(destinationFilePath);
                        }
                        return destinationFile;
                }


                private static java.nio.file.Path determineDestinationPathAvoidingExisting(
                                String destinationFilePath) throws IllegalAccessError {
                        String destinationFilePathWithoutExtension = destinationFilePath
                                        .substring(0, destinationFilePath.lastIndexOf('.'));
                        String extension = FilenameUtils.getExtension(destinationFilePath);
                        java.nio.file.Path rDestinationFile = Paths.get(destinationFilePath);
                        while (Files.exists(rDestinationFile)) {
                                destinationFilePathWithoutExtension += "1";
                                destinationFilePath = destinationFilePathWithoutExtension + "." + extension;
                                rDestinationFile = Paths.get(destinationFilePath);
                        }
                        if (Files.exists(rDestinationFile)) {
                                throw new IllegalAccessError(
                                                "an existing file will get overwritten");
                        }
                        return rDestinationFile;
                }


      		private static void doMove(java.nio.file.Path path, java.nio.file.Path destinationFile)
                                throws IllegalAccessError {
                        try {
                                Files.move(path, destinationFile);// By default, it won't
                                                                                                        // overwrite existing
                                System.out.println("Success: file now at "
                                                + destinationFile.toAbsolutePath());
                        } catch (IOException e) {
                                e.printStackTrace();
                                throw new IllegalAccessError("Moving did not work");
                        }
                }

		@GET
		@javax.ws.rs.Path("duplicate")
		@Produces("application/json")
		public Response removeDuplicate(@QueryParam("path") String iPath) throws JSONException, IOException {
			String targetSubdirName = "duplicates";
			moveFileToSubfolder(iPath, targetSubdirName);
			JSONObject json = new JSONObject();
			System.out.println("removeDuplicate() - end");
			return Response.ok().header("Access-Control-Allow-Origin", "*").entity(json.toString())
					.type("application/json").build();
		}
		
		@GET
		@javax.ws.rs.Path("wrongCategory")
		@Produces("application/json")
		public Response wrongCategory(@QueryParam("path") String iPath) throws JSONException {
			moveToParentDir(iPath);
			JSONObject json = new JSONObject();
			System.out.println("moveToParentDir() - end");
			return Response.ok().header("Access-Control-Allow-Origin", "*").entity(json.toString())
					.type("application/json").build();
		}

		/**
		 * 
		 * @param iPath
		 * @param targetSubdirName
		 *            - just the name, not the path
		 */
		private static void moveFileToSubfolder(String iPath, String targetSubdirName) throws IOException {
			System.out.println("moveFileToSubfolder() - Move " + iPath + " to " + targetSubdirName);
			_1: {
				String theContainingDirPath = FilenameUtils.getFullPath(iPath);
				System.out.println();
				String theTargetDirPath = theContainingDirPath + "/" + targetSubdirName;
				File theTargetDir = new File(theTargetDirPath);
				boolean createTargetDir = true;
				File fileToMove = new File(iPath);
				String destinationFilePath;
				String fileShortName = FilenameUtils.getName(iPath);
				System.out.println("moveFileToSubfolder() - checking if exists");
				File destinationFile = null;
				if (fileToMove.exists()) {
					if (!theTargetDir.exists()) {
						Files.createDirectory(Paths.get(theTargetDirPath));
					}
				}
				if (theTargetDir.exists()) {
					System.out
							.println("moveFileToSubfolder() - dir already exists, need to make sure existing file is not overwritten");
					createTargetDir = false;
					destinationFilePath = theTargetDirPath + "/" + fileShortName;
					String extension = FilenameUtils.getExtension(destinationFilePath);
					String destinationFilePathWithoutExtension = destinationFilePath.substring(0,
							destinationFilePath.lastIndexOf('.'));
					destinationFile = new File(destinationFilePath);
					while (destinationFile.exists()) {
	                                        System.out.println("Already exists: " + destinationFilePath);
						destinationFilePathWithoutExtension += "1";
						destinationFilePath = destinationFilePathWithoutExtension + "." + extension;
						destinationFile = new File(destinationFilePath);
					}
					System.out.println("Does not exist, creating: " + destinationFilePath);
					fileShortName = FilenameUtils.getName(destinationFilePath);
					// Double check, for now. We don't want to lose any images
					if (destinationFile.exists()) {
						throw new RuntimeException("Developer error");
					}
				}

				try {
					if (destinationFile == null) {
						throw new RuntimeException("Destination file is null");
					}
					System.out.println("moveFileToSubfolder() - about to move");
					FileUtils.moveFile(fileToMove, destinationFile);
					System.out.println("moveFileToSubfolder() - success");
					check: {
						File newFile = new File(theTargetDirPath + "/" + fileShortName);
						// check that it exists
						if (!newFile.exists()) {
							throw new RuntimeException("Developer error");
						}
						System.out.println("moveFileToSubfolder() - " + newFile.getAbsolutePath());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
                private String httpLinkFor(String iAbsolutePath) {
                        // Unsorted
                        String rHttpUrl = iAbsolutePath.replaceFirst("/Volumes/Unsorted/",
                                        "http://netgear.rohidekar.com:8020/");
                        rHttpUrl = rHttpUrl.replaceFirst(".*/Unsorted/",
                                        "http://netgear.rohidekar.com:8020/");

                        // Record
                        rHttpUrl = rHttpUrl.replaceFirst("/media/sarnobat/Record/",
                                        "http://netgear.rohidekar.com:8024/");
                        rHttpUrl = rHttpUrl.replaceFirst("/Volumes/Record/",
                                        "http://netgear.rohidekar.com:8024/");
                        rHttpUrl = rHttpUrl.replaceFirst("/Record/",
                                        "http://netgear.rohidekar.com:8024/");

                        // Large
                        rHttpUrl = rHttpUrl.replaceFirst("/media/sarnobat/Large/",
                                        "http://netgear.rohidekar.com:8021/");
                        rHttpUrl = rHttpUrl.replaceFirst("/Volumes/Large/",
                                        "http://netgear.rohidekar.com:8021/");

                        rHttpUrl = rHttpUrl.replaceFirst(".*/e/Sridhar/Photos",
                                        "http://netgear.rohidekar.com:8022/");

                        rHttpUrl = rHttpUrl.replaceFirst(".*/e/Sridhar/Web",
                                        "http://netgear.rohidekar.com:8006/");

                        // Books
                        rHttpUrl = rHttpUrl.replaceFirst(".*/e/Sridhar/Books",
                                        "http://netgear.rohidekar.com:8023/");

                        rHttpUrl = rHttpUrl.replaceFirst(".*/e/new",
                                        "http://netgear.rohidekar.com:8025/");

                        rHttpUrl = rHttpUrl.replaceFirst(".*/e/Drive J",
                                        "http://netgear.rohidekar.com:8026/");


                        return rHttpUrl;
                }
	}

	public static void main(String[] args) throws URISyntaxException {
		HttpServer server = JdkHttpServerFactory.createHttpServer(
				new URI("http://localhost:4463/"), new ResourceConfig(HelloWorldResource.class));
	}
}
