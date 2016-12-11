import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;

@Path("helloworld")
public class Selecast {

	@Path("cornette")
	@GET
	@Produces("text/plain")
	public Response jimCornette() {
		return Response.ok(
				getFeed("http://mlwradio.libsyn.com/rss", "Jim Cornette Experience",
						"foobar")).build();
	}

	@Path("wkh")
	@GET
	@Produces("text/plain")
	public Response wkh() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml", "otline",
						"lashback")).build();
	}

	@Path("int")
	@GET
	@Produces("text/plain")
	public Response interview() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml",
						"Livecast.*nterview", "foobar")).build();
	}

	@Path("jc")
	@GET
	@Produces("text/plain")
	public Response jamesCaldwell() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml",
						"Caldwell", "lashback")).build();
	}

	@Path("bm")
	@GET
	@Produces("text/plain")
	public Response bruceMitchell() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml",
						"Bruce Mitchell Audio", "lashback")).build();
	}

	@Path("ppv")
	@GET
	@Produces("text/plain")
	public Response ppv() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml",
						"(Special|ound)", "")).build();
	}
	
	@Path("tm")
	@GET
	@Produces("text/plain")
	public Response toddMartin() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml",
						"Todd Martin", "foobar")).build();
	}

	@Path("greg")
	@GET
	@Produces("text/plain")
	public Response greg() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml", "arks",
						"aldwell")).build();
	}

	@Path("flashback")
	@GET
	@Produces("text/plain")
	public Response flashback() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml",
						"lashback", "foobar")).build();
	}

        @Path("playback")
        @GET
        @Produces("text/plain")
        public Response playback() {
                return Response.ok(
                                getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml",
                                                "layback", "foobar")).build();
        }

	@Path("livecast")
	@GET
	@Produces("text/plain")
	public Response livecast() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml",
						"ivecast", "(Neill|Caldwell)")).build();
	}

	@Path("pat")
	@GET
	@Produces("text/plain")
	public Response patMcneill() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml", "ivecast.*eill",
						"(lashback|layback)")).build();
	}

	@Path("powell")
	@GET
	@Produces("text/plain")
	public Response jamesPowell() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml", "owell",
						"foobar")).build();
	}

	@Path("test")
	@GET
	@Produces("text/plain")
	public Response test() {
		return Response.ok(
				getFeed("http://www.pwtorch.com/pwtorchvipaudio.xml", "otline",
						"lashback")).build();
	}

	private static String getFeed(String url, String include, String exclude) {
		SyndFeed feed;
		try {
			feed = new SyndFeedInput().build(new XmlReader(new URL(url)));

			SyndFeed wkHotlineFeed = (SyndFeed) feed.clone();
			for (Object o : feed.getEntries()) {
				SyndEntry entry = (SyndEntry) o;
				if (entry.getTitle().matches(".*" + include + ".*")) {
					if (!entry.getTitle().matches(".*" + exclude + ".*")) {
						continue;
					}
				}
				wkHotlineFeed.getEntries().remove(o);
			}
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(bao);
			new SyndFeedOutput().output(wkHotlineFeed, outputStreamWriter);
			outputStreamWriter.close();
			return new String(bao.toByteArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws URISyntaxException {

		if ("VIP - 02/14 PWTorch Livecast w/Jonny Fairplay: Never-told stories on what went wrong in TNA ten years ago, Punk stories, Heenans big compliment"
				.matches(".*Livecast( . VIP Aftershow)? w/.*")) {
			System.out.println("regex is correct");
		} else {
			System.out.println("regex is not correct");
		}

		String BASE_URI = "http://localhost:4444/";

		ResourceConfig rc = new ResourceConfig(Selecast.class);
		URI endpoint = new URI(BASE_URI);

		JdkHttpServerFactory.createHttpServer(endpoint, rc);
		System.out.println("console v2.0 : Press Enter to stop the server. ");

	}
}
