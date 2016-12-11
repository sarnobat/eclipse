//@Grab(group='com.pastdev', module='jsch-nio', version='0.1.5')

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.pastdev.jsch.DefaultSessionFactory;

public class NioClose {
	@javax.ws.rs.Path("cmsfs")
	public static class MyResource { // Must be public
	    private static final ExecutorService TASK_EXECUTOR = Executors.newCachedThreadPool();
	    private static final int SLEEP_TIME_IN_MILLIS = 1000;

	    

	    @GET
	    @javax.ws.rs.Path("static6/{echo}")
	    public String syncEcho(@PathParam("echo") final String echo) {
	        try {
	            Thread.sleep(SLEEP_TIME_IN_MILLIS);
	        } catch (final InterruptedException ex) {
	            throw new ServiceUnavailableException();
	        }
	        return echo;
	    }

	    
		@GET
		@javax.ws.rs.Path("static5/{echo}")
		public void asyncEcho(@PathParam("echo") final String echo,
				@Suspended final AsyncResponse ar) {
			TASK_EXECUTOR.submit(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(SLEEP_TIME_IN_MILLIS);
					} catch (final InterruptedException ex) {
						ar.cancel();
					}
					ar.resume("hello");
				}
			});
		}

		@GET
		@javax.ws.rs.Path("static3/{absolutePath : .+}")
		@Produces("application/json")
		public void getFileSshNio2(
				@PathParam("absolutePath") String absolutePathWithSlashMissing,
				@Context HttpHeaders header, @QueryParam("width") final Integer iWidth,
				@Suspended final AsyncResponse asyncResponse) {
//			final String absolutePath = "/media/sarnobat/Unsorted/Videos/_thumbnails/Atletico de Madrid VS Barcelona (3- 1) TEMPORADA 95 96.webm.jpg" ;
//			final String absolutePath = "/Users/sarnobat/.zshrc";
			final String absolutePath = "/home/sarnobat/temp2.txt";
			
			  asyncResponse.register(new CompletionCallback() {
		            @Override
		            public void onComplete(Throwable throwable) {
		            	System.out.println("onComplete()");
		                if (throwable == null) {
		                    // no throwable - the processing ended successfully
		                    // (response already written to the client)
		                } else {
		                }
		            }
		        });

				final FileSystem client = getAsyncClient();

				System.out.println("getFileSshNio() - 2");
				Path sshFilePath = client.getPath(absolutePath);
				System.out.println("getFileSshNio() - 2.1");
//				Files.copy(sshFilePath, os);
				asyncResponse.resume(sshFilePath);
				
			  
//			StreamingOutput s = new StreamingOutput() {
//				@Override
//				public void write(OutputStream os) throws IOException, WebApplicationException {
//					System.out.println("getFileSshNio() - 4");
//// TODO: don't do this every time
//					final FileSystem client = getAsyncClient();
//
//					System.out.println("getFileSshNio() - 2");
//					Path sshFilePath = client.getPath(absolutePath);
//					System.out.println("getFileSshNio() - 2.1");
//					Files.copy(sshFilePath, os);
//					System.out.println("getFileSshNio() - 2.2");
//					System.out.println("getFileSshNio() - 2.3");
//				}
//			};
		}

		public static String getMimeType(String fileFullPath) {
			String mime = null;
			// Get MIME type from file name extension, if possible
			int dot = fileFullPath.lastIndexOf('.');
			if (dot >= 0) {
				mime = (String) theMimeTypes.get(fileFullPath.substring(dot + 1).toLowerCase());
			}
			if (mime == null) {
				mime = MIME_DEFAULT_BINARY;
			}
			return mime;
		}

		public static final String MIME_DEFAULT_BINARY = "application/octet-stream";
		private static Hashtable<String, String> theMimeTypes = new Hashtable<String, String>();

		static {
			StringTokenizer st = new StringTokenizer("css		text/css " + "htm		text/html "
					+ "html		text/html " + "xml		text/xml " + "txt		text/plain "
					+ "asc		text/plain " + "gif		image/gif " + "jpg		image/jpeg "
					+ "jpeg		image/jpeg " + "png		image/png " + "mp3		audio/mpeg "
					+ "m3u		audio/mpeg-url " + "mp4		video/mp4 " + "ogv		video/ogg "
					+ "flv		video/x-flv " + "mov		video/quicktime "
					+ "swf		application/x-shockwave-flash " + "js			application/javascript "
					+ "pdf		application/pdf " + "doc		application/msword "
					+ "ogg		application/x-ogg " + "zip		application/octet-stream "
					+ "exe		application/octet-stream " + "class		application/octet-stream ");
			while (st.hasMoreTokens()) {
				theMimeTypes.put(st.nextToken(), st.nextToken());
			}
		}

		private FileSystem getAsyncClient() {
			DefaultSessionFactory defaultSessionFactory;
			try {
				defaultSessionFactory = new DefaultSessionFactory("sarnobat", "192.168.1.2", 22);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			try {
				defaultSessionFactory
						.setKnownHosts("/Users/sarnobat.reincarnated/.ssh/known_hosts");
				defaultSessionFactory
						.setIdentityFromPrivateKey("/Users/sarnobat.reincarnated/.ssh/id_rsa");
				// defaultSessionFactory.setKnownHosts("/home/sarnobat/.ssh/known_hosts");
				// defaultSessionFactory.setIdentityFromPrivateKey("/home/sarnobat/.ssh/id_rsa");
				defaultSessionFactory.setConfig("StrictHostKeyChecking", "no");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
			Map<String, Object> environment = new HashMap<String, Object>();
			environment.put("defaultSessionFactory", defaultSessionFactory);
			URI uri;
			try {
				uri = new URI("ssh.unix://sarnobat@192.168.1.2:22/home/sarnobat");
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			FileSystem sshfs;
			try {
				sshfs = FileSystems.newFileSystem(uri, environment, getClass().getClassLoader());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sshfs;
		}
	}

	public static void main(String[] args) throws URISyntaxException {
		System.out
				.println("Note this doesn't work with JVM 1.8 build 45 due to some issue with TLS");
		try {
			JdkHttpServerFactory.createHttpServer(new URI("http://localhost:4451/"),
					new ResourceConfig(MyResource.class));
		} catch (Exception e) {
			System.out.println("Port already listened on.");
		}
	}
}
