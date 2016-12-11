import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.ExceptionLogger;
import org.apache.http.HttpConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContexts;

/**
 * Embedded HTTP/1.1 file server based on a non-blocking I/O model and capable
 * of direct channel (zero copy) data transfer.
 */
public class NHttpFileServer {

	public static void main(String[] args) throws Exception {
		NHttpFileServer1.startServer(4452);
	}

	private static class NHttpFileServer1 {
		static void startServer(int port) throws NoSuchAlgorithmException,
				KeyManagementException, KeyStoreException, UnrecoverableKeyException,
				CertificateException, IOException, InterruptedException {
			SSLContext sslcontext = null;
			if (port == 8443) {
				// Initialize SSL context
				URL url = NHttpFileServer.class.getResource("/my.keystore");
				if (url == null) {
					System.out.println("Keystore not found");
					System.exit(1);
				}
				sslcontext = SSLContexts.custom()
						.loadKeyMaterial(url, "secret".toCharArray(), "secret".toCharArray())
						.build();
			}

			IOReactorConfig config = IOReactorConfig.custom().setSoTimeout(15000)
					.setTcpNoDelay(true).build();

			final HttpServer server = ServerBootstrap.bootstrap().setListenerPort(port)
					.setServerInfo("Test/1.1").setIOReactorConfig(config).setSslContext(sslcontext)
					.setExceptionLogger(ExceptionLogger.STD_ERR)
					.registerHandler("*", new HttpFileHandler()).create();

			server.start();
		}

		private static class HttpFileHandler implements HttpAsyncRequestHandler<HttpRequest> {

			@Override
			public HttpAsyncRequestConsumer<HttpRequest> processRequest(final HttpRequest request,
					final HttpContext context) {
				// Buffer request content in memory for simplicity
				return new BasicAsyncRequestConsumer();
			}

			@Override
			public void handle(final HttpRequest request, final HttpAsyncExchange httpexchange,
					final HttpContext context) throws HttpException, IOException {
				HttpResponse response = httpexchange.getResponse();
				handleInternal(request, response, context);
				httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
			}

			private static void handleInternal(final HttpRequest request,
					final HttpResponse response, final HttpContext context) throws HttpException,
					IOException {

				String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
				if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
					throw new MethodNotSupportedException(method + " method not supported");
				}

				String target = request.getRequestLine().getUri();
				final File file = Paths.get(target).toFile();// ,
																// URLDecoder.decode(target,
																// "UTF-8"));
				System.out.println("NHttpFileServer.HttpFileHandler.handleInternal() - serving "
						+ file.getAbsolutePath());
				if (!file.canRead()) {
					throw new RuntimeException("cannot read");
				}
				if (!file.exists()) {

					response.setStatusCode(HttpStatus.SC_NOT_FOUND);
					NStringEntity entity = new NStringEntity("<html><body><h1>File"
							+ file.getPath() + " not found</h1></body></html>", ContentType.create(
							"text/html", "UTF-8"));
					response.setEntity(entity);
					System.out.println("File " + file.getPath() + " not found");

				} else if (!file.canRead() || file.isDirectory()) {

					response.setStatusCode(HttpStatus.SC_FORBIDDEN);
					NStringEntity entity = new NStringEntity(
							"<html><body><h1>Access denied</h1></body></html>", ContentType.create(
									"text/html", "UTF-8"));
					response.setEntity(entity);
					System.out.println("Cannot read file " + file.getPath());

				} else {

					HttpCoreContext coreContext = HttpCoreContext.adapt(context);
					HttpConnection conn = coreContext.getConnection(HttpConnection.class);
					response.setStatusCode(HttpStatus.SC_OK);
					NFileEntity body = new NFileEntity(file, ContentType.create("image/jpeg"));
					response.setEntity(body);
					System.out.println(conn + ": serving file " + file.getPath());
				}
			}
		}
	}

}