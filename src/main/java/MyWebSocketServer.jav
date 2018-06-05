import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;
import org.eclipse.jetty.websocket.WebSocket.Connection;

public class MyWebSocketServer {

	public static void main(String[] args) {
		try {
			Server server = new Server(8081);
			WebSocketHandler chatWebSocketHandler = new WebSocketHandler() {
				Connection connection;
				public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
					return new WebSocket.OnTextMessage() {
						@Override public void onOpen(Connection conn) {
							connection = conn;
						}

						@Override public void onClose(int closeCode, String message) {
							connection = null;
						}

						@Override public void onMessage(String data) {
							System.out.println("onMessage() - data: " + data);
							try {
								connection.sendMessage("I think I heard you say \"" + data+"\". Well, my name is ChatWebSocketServer.java");
							} catch (IOException x) {
								connection.close();
							}
						}
					};
				}
			};
			chatWebSocketHandler.setHandler(new DefaultHandler());
			server.setHandler(chatWebSocketHandler);
			server.start();
			server.join();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}