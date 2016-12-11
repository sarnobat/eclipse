import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.json.JSONObject;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/** At the moment, this web server is just for practice using the Akka library */
@SuppressWarnings("restriction")
public class AkkaWebServer {
	private static class MyListener extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			if (msg instanceof OutputStream) {
				OutputStream os = (OutputStream) msg;
				JSONObject json = new JSONObject();
				json.put("foo", "bar");
				os.write(json.toString().getBytes());
				os.close();
				System.out.println("onRecieve() - done");
//				context().actorSelection("*").
			} else {

			}
		}

	}

	private static class MyHandler implements HttpHandler {
		private final ActorSystem system;

		public MyHandler(ActorSystem system) {
			this.system = system;
		}

		@Override
		public void handle(HttpExchange http) throws IOException {
			final ActorRef listener = system.actorOf(
					Props.create(MyListener.class), Long.toString(System.currentTimeMillis()));
			System.out.println(http.getRequestURI());
			JSONObject json = new JSONObject();
			System.out.println("Request headers: " + http.getRequestHeaders());
			System.out.println("Request URI" + http.getRequestURI());
			json.put("foo", "bar");
			http.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			http.getResponseHeaders().add("Content-type", "application/json");
			http.sendResponseHeaders(200, json.toString().length());
			listener.tell(http.getResponseBody(), null);
			// OutputStream os = http.getResponseBody();
			// os.write(json.toString().getBytes());
			// os.close();
		}

	}

	public static void main(String[] args) throws IOException {
		ActorSystem system = ActorSystem.create("PiSystem");

		int port = 4457;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext("/", new MyHandler(system));
		server.setExecutor(null); // creates a default executor
		server.start();
	}
}
