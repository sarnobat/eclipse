import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

public class SelectorExample {
	private static final int PORT_NUMBER = 12345;
	private static final int BUFFER_SIZE = 1;
	private static final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

	public static void main(String[] args) throws IOException {
		Selector selector = Selector.open();
		ServerSocketChannel server = listenForConnections(PORT_NUMBER, selector);

		while (true) {
			if (selector.select() > 0) {
				// Check if any of the selectors have data
				Set<SelectionKey> keys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = keys.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();

					if (key.isAcceptable()) {
						System.out.println("SelectorExample.main() - client just connected");
						// Not sure what this is. Probably it's for clients connecting to us.
						SocketChannel client = server.accept();
						client.configureBlocking(false);
						int port = client.socket().getPort();
						System.out.println("SelectorExample.main() - session with client allocated to port " + port);
						client.register(selector, SelectionKey.OP_READ, port);
					} else if (key.isReadable()) {
						System.out.println("port: " + key.attachment());
						FileChannel fc = FileChannel.open(Paths
								.get("/Users/sarnobat/trash/DSC_0435.JPG"));
//								.get("/sarnobat.garagebandbroken/Windows/usb/web/Rohida.html"));
						SocketChannel selectedClient = (SocketChannel) key.channel();
						while (fc.read(buffer) > -1) {
							buffer.flip(); // read from the buffer
							selectedClient.write(buffer);
							buffer.clear(); // write into the buffer. Does this flip the buffer back into read mode?
						}
						selectedClient.close();
					}
				}
			}
		}
	}

	private static ServerSocketChannel listenForConnections(int portNumber, Selector selector) throws IOException,
			SocketException, ClosedChannelException {
		ServerSocketChannel server = listenForConnections(PORT_NUMBER);
		server.register(selector, SelectionKey.OP_ACCEPT);
		return server;
	}

	private static ServerSocketChannel listenForConnections(int portNumber) throws IOException, SocketException {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.socket().bind(new InetSocketAddress(portNumber));
		server.socket().setReuseAddress(true);
		server.configureBlocking(false);
		return server;
	}
}