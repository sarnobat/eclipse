import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SshWebServer2 {
	public static void main(String[] args) {
		getFileFromSsh();

	}

	private static void getFileFromSsh() {
		String SFTPHOST = "192.168.1.2";
		int SFTPPORT = 22;
		String SFTPUSER = "sarnobat";
		String SFTPWORKINGDIR = "/home/sarnobat";

		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;

		try {
			JSch jsch = new JSch();
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			// session.setPassword(SFTPPASS);
			String privateKey = "/Users/sarnobat/.ssh/id_rsa";
			jsch.addIdentity(privateKey);

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			String fileNameSimple = ".dmrc";
			String destinationDirPath = "/sarnobat.garagebandbroken/trash/";
			{
				byte[] buffer = new byte[1024];
				BufferedInputStream bis = new BufferedInputStream(
						channelSftp.get(fileNameSimple));
				File newFile = new File(
						destinationDirPath + fileNameSimple);
				OutputStream os = new FileOutputStream(newFile);
				BufferedOutputStream bos = new BufferedOutputStream(os);
				int readCount;
				while ((readCount = bis.read(buffer)) > 0) {
					bos.write(buffer, 0, readCount);
				}
				bis.close();
				bos.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		channelSftp.exit();
		session.disconnect();
	}

}
