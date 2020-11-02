import java.io.IOException;
import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 2020-11-01 this is needed because Tomcat's webdav's move is slow for large
 * files (it does a copy and delete)
 */
public class CoagulateMoveLargeFast {

	@javax.ws.rs.Path("cmsfs/v2/")
	public static class MyResource { // Must be public

		@GET
		@javax.ws.rs.Path("moveBase64")
		@Produces("application/json")
		public Response moveBase64(@QueryParam("filePath") String iFilePath1,
				@QueryParam("destinationDirSimpleName") String iDestinationDirSimpleName)
				throws JSONException, IOException {

			System.err.println("moveBase64() " + iFilePath1);

			String iFilePath = "";
			try {
				iFilePath = StringUtils.newStringUtf8(Base64.decodeBase64(iFilePath1));// Base64.getDecoder().decode(iFilePath1);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("moveBase64() " + e.toString());
				throw e;
			}
			System.err.println("moveBase64() " + iFilePath);
			if (iFilePath.endsWith("htm") || iFilePath.endsWith(".html")) {
				throw new RuntimeException("Need to move the _files folder too");
			}
			if (iDestinationDirSimpleName.equals("_ 1")) {
				System.out.println("move() - dir name is wrong");
				throw new RuntimeException("dir name is wrong: " + iDestinationDirSimpleName);
			}

			return Response.ok().header("Access-Control-Allow-Origin", "*").entity(new JSONObject().toString(4))
					.type("application/json").build();
		}
	}

	public static void main(String[] args) {

		String port = System.getProperty("port", "1157");

		try {
			JdkHttpServerFactory.createHttpServer(new URI("http://localhost:" + port + "/"),
					new ResourceConfig(MyResource.class));
		} catch (Exception e) {
			System.out.println("Port already listened on.");
			System.exit(-1);
		}
	}
}
