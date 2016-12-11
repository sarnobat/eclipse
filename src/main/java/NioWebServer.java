import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

/** Simple Java non-blocking NIO webserver. */
public class NioWebServer implements Runnable {

	private static final String RESPONSE_STRING = "I liek cates";
	private static final CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
	private final Selector selector = Selector.open();
	private final ServerSocketChannel server;// = ServerSocketChannel.open();

	// Start listening on port
	NioWebServer(InetSocketAddress address) throws IOException {
		server = listenOnPortAsynchronously(address);
		server.register(selector, SelectionKey.OP_ACCEPT);
	}

	private static ServerSocketChannel listenOnPortAsynchronously(InetSocketAddress address)
			throws IOException {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.socket().bind(address);
		server.configureBlocking(false);
		return server;
	}

	private static void startServer(NioWebServer server) throws InterruptedException {
		while (true) {
			server.run();
			Thread.sleep(100);
		}
	}

	@Override
	public final void run() {
		try {
			respondToQueuedRequests(selector, server);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void respondToQueuedRequests(Selector selector, ServerSocketChannel serverSocket)
			throws IOException {
		selector.selectNow();
		Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
		while (keys.hasNext()) {
			SelectionKey key = keys.next();
			// TODO: Our server endpoint must add a key to the selector whenever
			// we get a request to load an image
			keys.remove();
			if (!key.isValid()) {
				continue;
			}
			try {
				if (key.isAcceptable()) {
					// New Client encountered
					serverSocket.accept().configureBlocking(false)
							.register(selector, SelectionKey.OP_READ);

				} else if (key.isReadable()) {
					// Additional data for existing client encountered
					SocketChannel selectedClient = (SocketChannel) key.channel();
					ByteBuffer buffer = ByteBuffer.allocate(548);
					String requestedFile2 = getRequstedFile(key, selectedClient, buffer);
					buffer.clear();
					buffer.flip();
					try {
						System.out.println("NioWebServer.respondToQueuedRequests() - "
								+ requestedFile2);
						FileChannel fc = FileChannel.open(Paths.get(requestedFile2));
						String string = "HTTP/1.1 200 Ok\nContent-Type: image/jpeg\nContent-Length: "
								+ (Files.size(Paths.get(requestedFile2)) + "\n\n");
						selectedClient.write(ByteBuffer.wrap(string.getBytes()));
						System.out.println(string);
						System.out.println();
						int loops = 0;
						while (fc.read(buffer) > 0 || buffer.position() > 0) {
							buffer.flip(); // read from the buffer
++loops;
							try {
								System.out.println(buffer.remaining());
								selectedClient.write(buffer);
							} catch (IOException e) {
								System.out.println("NioWebServer.respondToQueuedRequests() - " + e);
								break;
							}
							buffer.compact(); // write into the buffer. Does this
							// flip the buffer back into read
							// mode?
						}
						int bytesRead = loops * 548;
						System.out.println("NioWebServer.respondToQueuedRequests() - bytes read: " + bytesRead);
						System.out.println("NioWebServer.respondToQueuedRequests() - closing");
					} catch (NoSuchFileException e) {
						// sendResponseAndCloseChannel2(selectedClient,
						// requestedFile2);
						System.err.println("error");
					}
					selectedClient.close();

				}
			} catch (Exception ex) {
				ex.printStackTrace();
				if (key.attachment() instanceof HTTPSession) {
					close((SocketChannel) key.channel());
				}
			}
		}
	}

	private String getRequstedFile(SelectionKey key, SocketChannel clientSocket, ByteBuffer buffer)
			throws IOException {
		HTTPSession httpSession = getOrCreateSession(key, buffer);
		httpSession.collectClientDataInBuffer(clientSocket, buffer);
		return getLocationFromHeader(getRequestedFile(buffer, httpSession));
	}

	private void sendResponseAndCloseChannel2(SocketChannel clientSocket, String filePathRequested) {
		// Finished reading client's http request, now parse it
		byte[] responseContent = (RESPONSE_STRING + ": " + filePathRequested).getBytes();
		try {
			writeHeaders(addDefaultHeaders(responseContent), clientSocket);
			clientSocket.write(ByteBuffer.wrap(responseContent));
		} catch (IOException ex) {
			// slow silently
			System.err.println("NioWebServer.HTTPSession.sendResponse() - " + ex);
		} finally {
			close(clientSocket);
		}
	}

	void close(SocketChannel clientSocket) {
		try {
			clientSocket.close();
		} catch (IOException ex) {
		}
	}

	void sendResponseAndCloseChannel(Map<String, String> headers, byte[] responseContent,
			SocketChannel clientSocket) {
		try {
			writeHeaders(headers, clientSocket);
			clientSocket.write(ByteBuffer.wrap(responseContent));
		} catch (IOException ex) {
			// slow silently
			System.err.println("NioWebServer.HTTPSession.sendResponse() - " + ex);
		} finally {
			close(clientSocket);
		}
	}

	private void writeHeaders(Map<String, String> headers, SocketChannel clientSocket)
			throws IOException {
		writeLineTo("HTTP/1.1" + " " + 200 + " " + "OK", clientSocket);
		for (Map.Entry<String, String> header : headers.entrySet()) {
			writeLineTo(header.getKey() + ": " + header.getValue(), clientSocket);
		}
		writeLineTo("", clientSocket);
	}

	private static void writeLineTo(String line, SocketChannel clientChannel) throws IOException {
		clientChannel.write(encoder.encode(CharBuffer.wrap(line + "\r\n")));
	}

	private static Map<String, String> addDefaultHeaders(byte[] responseContent) {
		String responseSie = Integer.toString(responseContent.length);
		Map<String, String> headers = new LinkedHashMap<String, String>();
		headers.put("Date", new Date().toString());
		headers.put("Server", "Java NIO Webserver by md_5");
		headers.put("Connection", "close");
		headers.put("Content-Length", responseSie);
		return headers;
	}

	private static String getRequestedFile(ByteBuffer buffer, HTTPSession httpSession)
			throws IOException {
		String aLine;
		StringBuffer requestString = new StringBuffer();
		while ((aLine = httpSession.readLineFrom(buffer)) != null) {
			requestString.append(aLine);
			if (aLine.isEmpty()) {
				break;
			}
		}
		return requestString.toString();
	}

	private static String getLocationFromHeader(String headersFromClient) {
		StringTokenizer tokenizer = new StringTokenizer(headersFromClient);
		// String method =
		tokenizer.nextToken().toUpperCase();
		String location = tokenizer.nextToken();
		// String version =
		tokenizer.nextToken();
		return location;
	}

	private HTTPSession getOrCreateSession(SelectionKey key, ByteBuffer buffer) {
		HTTPSession httpSession = (HTTPSession) key.attachment();
		// create it if it doesnt exist
		if (httpSession == null) {
			httpSession = new HTTPSession();
			key.attach(httpSession);
		}
		return httpSession;
	}

	public final class HTTPSession {

		private int mark = 0;

		String readLineFrom(ByteBuffer buffer) throws IOException {
			StringBuilder line = new StringBuilder();
			int lastChar = -1;
			while (buffer.hasRemaining()) {
				char currentChar = (char) buffer.get();
				line.append(currentChar);
				if (currentChar == '\n' && lastChar == '\r') {
					// mark our position
					mark = buffer.position();
					// append to the total
					// return with no line separators
					return line.substring(0, line.length() - 2);
				}
				lastChar = currentChar;
			}
			return null;
		}

		/**
		 * Get more data from the stream.
		 */
		ByteBuffer collectClientDataInBuffer(SocketChannel socketChannel, ByteBuffer buffer)
				throws IOException {
			buffer.limit(buffer.capacity());
			int read = socketChannel.read(buffer);
			if (read == -1) {
				throw new IOException("End of stream");
			}
			buffer.flip();
			buffer.position(mark);
			return buffer;
		}
	}

	public static void main(String[] args) throws Exception {
		NioWebServer server = new NioWebServer(new InetSocketAddress(5555));
		startServer(server);
	}
}