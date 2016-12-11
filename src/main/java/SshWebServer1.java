import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONException;

import com.sun.net.httpserver.HttpServer;

public class SshWebServer1 {

	public static void main(String[] args) throws URISyntaxException {
		HttpServer server = JdkHttpServerFactory.createHttpServer(new URI(
				"http://localhost:9099/"), new ResourceConfig(
				HelloWorldResource.class));
	}

	@Path("sshws")
	public static class HelloWorldResource { // Must be public
	// final OutputStream outputStream = new ByteArrayOutputStream();
		Writer writer;
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException,
					WebApplicationException {
				writer = new BufferedWriter(new OutputStreamWriter(os));
				writer.write("test");
				writer.flush();
			}
		};

		public HelloWorldResource() throws IOException {
			new Thread() {
				@Override
				public void run() {
					int i = 1;
					while (true) {
						try {
							i += 1;
							this.sleep(10);
							if (writer != null) {
								// outputStream.write(i);
								// outputStream.flush();
								writer.write("foobar");
//								writer.flush();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (WebApplicationException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
		}

		@GET
		@Path("logfile3")
		@Produces(MediaType.TEXT_PLAIN)
		public Response logfile3() throws JSONException, IOException {

			return Response.ok(stream).build();
		}
	}

	private static void getFileJcsh() {

	}

}
