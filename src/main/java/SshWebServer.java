import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SshWebServer {
	private static final String SFTPHOST = "192.168.1.2";
	private static final String privateKeyOnClient = "/Users/sarnobat/.ssh/id_rsa";
	private static final int SFTPPORT = 22;
	private static final String SFTPUSER = "sarnobat";

	public static void main(String[] args) {
		getFileFromSsh();
	}

	private static void getFileFromSsh() {
		String fileSimpleName = ".dmrc";
		String sourceFile = "/home/sarnobat/" + fileSimpleName;
		try {
			InputStream sshStream = getRemoteFileStream(SFTPHOST, SFTPPORT,
					SFTPUSER, sourceFile, privateKeyOnClient);
			// Mostly not relevant since we're writing to a stream, not a file.
			{
				String destinationFilePath = "/sarnobat.garagebandbroken/trash/"
						+ fileSimpleName;
				File newFile = Paths.get(destinationFilePath).toFile();
				OutputStream os = new FileOutputStream(newFile);
				BufferedOutputStream bos = new BufferedOutputStream(os);
				{
					int readCount;
					byte[] buffer = new byte[1024];
					while ((readCount = sshStream.read(buffer)) > 0) {
						bos.write(buffer, 0, readCount);
					}
				}
				bos.close();
			}
			sshStream.close();
		} catch (RuntimeException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// channelSftp.exit();
		// session.disconnect();
	}

	/** Close the input stream after reading */
	private static InputStream getRemoteFileStream(String host, int port,
			String username, String sourceFile, String clientIdRSAPath)
			throws JSchException, SftpException {
		ChannelSftp channelSftp = geChannelSftp(host, port, username,
				clientIdRSAPath);
		return getRegularFileStreamFromSsh(sourceFile, channelSftp);
	}

	private static InputStream getRegularFileStreamFromSsh(String sourceFile,
			ChannelSftp channelSftp) throws SftpException {
		channelSftp.cd(Paths.get(sourceFile).getParent().toAbsolutePath()
				.toString());
		return new BufferedInputStream(channelSftp.get(Paths.get(sourceFile)
				.getFileName().toString()));
	}

	private static ChannelSftp geChannelSftp(String host, int port,
			String username, String privateKey) throws JSchException {
		ChannelSftp channelSftp;
		Session session = null;
		Channel channel = null;
		JSch jsch = new JSch();
		session = jsch.getSession(username, host, port);
		// session.setPassword(SFTPPASS);
		jsch.addIdentity(privateKey);

		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		if (!session.isConnected()) {
			session.connect();
		}
		channel = session.openChannel("sftp");
		if (!channel.isConnected()) {
			channel.connect();
		}
		channelSftp = (ChannelSftp) channel;
		return channelSftp;
	}

}
