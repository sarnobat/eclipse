import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import no.uis.nio.sftp.SFTPFileSystemProvider;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class NioSftp {

	public static void main(String[] args) throws URISyntaxException, IOException {
		URI uri = new URI("sftp", "sarnobat" + ':' + "aize2F", "192.168.1.2", 2222,
				"/", null, null);

		FileSystem fs = FileSystems.newFileSystem(uri, null);
//		Path p = fs.getPath("/media/sarnobat/3TB/trash/test.txt");
//		SeekableByteChannel i = Files.newByteChannel(p, StandardOpenOption.READ);
//		checkNotNull(i);
		SFTPFileSystemProvider pr = (SFTPFileSystemProvider) fs.provider();
		pr.getPath(uri);
		InputStream is = pr.newInputStream(pr.getPath(uri), StandardOpenOption.READ);
//		InputStream is = fs.provider().newInputStream(p, StandardOpenOption.READ);
		String s = IOUtils.toString(is);
		System.out.println(s);
	}

}
