import java.io.IOException;
import java.net.URISyntaxException;

import org.json.JSONException;

public class Test1 {
	public static void main(String[] args) throws URISyntaxException, JSONException, IOException {
		DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory("sarnobat",
				"192.168.1.2", 22);
		try {
			defaultSessionFactory.setKnownHosts("/Users/sarnobat.reincarnated/.ssh/known_hosts");
			defaultSessionFactory
					.setIdentityFromPrivateKey("/Users/sarnobat.reincarnated/.ssh/id_rsa");
			// defaultSessionFactory.setConfig( "StrictHostKeyChecking", "no" );
		} catch (JSchException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		Map<String, Object> environment = new HashMap<String, Object>();
		environment.put("defaultSessionFactory", defaultSessionFactory);
		URI uri = new URI("ssh.unix://sarnobat@192.168.1.2:22/home/sarnobat");
		String scheme = "ssh.unix";
		for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
			System.out.println("-------- " + provider.getScheme());
			if (scheme.equalsIgnoreCase(provider.getScheme())) {
				System.out.println("++++++++++++");
			}
		}
//		printClassPath this.class.classLoader
		
		FileSystem sshfs = FileSystems.newFileSystem(uri, environment, new ClassLoader() {});
		Path path = sshfs.getPath(".zshrc");
		FileSystemProvider provider = path.getFileSystem().provider();
		InputStream inputStream = provider.newInputStream(path);
		String fileContents = IOUtils.copyToString(inputStream);
		System.out.println(fileContents);
		inputStream.close();
		sshfs.close();
	}
	def printClassPath(classLoader) {
		println "$classLoader"
		classLoader.getURLs().each {url->
		   println "- ${url.toString()}"
		}
		if (classLoader.parent) {
		   printClassPath(classLoader.parent)
		}
	  }
}

