import java.io.IOException;

import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.SftpClient;
import org.apache.sshd.client.SftpClient.DirEntry;

public class SshdClient {

	private static final String host = "netgear.rohidekar.com";
	private static final String login = "sarnobat";
	private static final String password = "aize2F";

	public static void main(String[] args) throws InterruptedException,
			IOException {
		SftpClient sftp = getClient();
		for (DirEntry d : sftp.readDir("/home/sarnobat/")) {
			System.out.println(d.longFilename);
		}

	}

	private static SftpClient getClient() throws InterruptedException,
			IOException {
		SshClient client = SshClient.setUpDefaultClient();
		client.start();
		ClientSession session = client.connect(login, host, 22).await()
				.getSession();
		// TODO: Use key authentication instead
		session.addPasswordIdentity(password);
		session.auth().await();
		SftpClient sftp = session.createSftpClient();
		return sftp;
	}
}