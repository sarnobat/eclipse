import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FuseException;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterFull;

/**
 * This reproduces issue:
 * 
 * https://github.com/EtiennePerot/fuse-jna/issues/58
 *
 */
public class FuseTruncationProblem extends FuseFilesystemAdapterFull
{
	public static void main(final String... args) throws FuseException, IOException {
		Files.createDirectories(Paths.get("/tmp/fusejnaproblem"));
		new FuseTruncationProblem().log(false).mount("/tmp/fusejnaproblem");
		System.out.println("Now please execute from the command line: 'find -L /tmp/fusejnaproblem'");
		System.out.println("You'll see that not all 6000 files appear. Only the first 3500 do.");
	}

	final String filename = "/hello1.txt";
	final String contents = "Hello World\n";

	@Override
	public int getattr(final String path, final StatWrapper stat)
	{
		if (path.equals(File.separator)) { // Root directory
			stat.setMode(NodeType.DIRECTORY);
			return 0;
		}
		if (path.equals("/hello1.txt")) { // hello.txt
			stat.setMode(NodeType.FILE).size(contents.length());
			return 0;
		}
		if (path.endsWith("dir")) {
			stat.setMode(NodeType.DIRECTORY);
			return 0;
		}
		else if (path.endsWith("txt")) {
			stat.setMode(NodeType.FILE).size(contents.length());
			return 0;
		}
		return -ErrorCodes.ENOENT();
	}

	@Override
	public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info)
	{
		// Compute substring that we are being asked to read
		final String s = contents.substring((int) offset,
				(int) Math.max(offset, Math.min(contents.length() - offset, offset + size)));
		buffer.put(s.getBytes());
		return s.getBytes().length;
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler)
	{
		filler.add(filename);
		if (path.length() == 1) {
			filler.add("1dir");
		}
		if (path.endsWith("1dir")) {
			for (String file : getList()) {
				final boolean added = filler.add(file);
				if (!added) {
					System.out.println("HelloWorldFuse.readdir() Failed to add");
					return ErrorCodes.ENOMEM();
				}
			}
		}
		return 0;
	}

	private List<String> getList() {
		List<String> files = new LinkedList<String>();
//		files.add("3503::Mainstream American pop culture seem completely worthless! Why aren't people interested in more worthwhile activies? - Politics and Other Controversies -Democrats, Republicans, Libertarians, Conservatives, Liberals, Third Parties, Left-Wing, Right-Wing, Congress, President - Page 5 - City-Data Forum");
		//String e = "3503::A! Why aren't people interested in more worthwhile activies? - Politics and Other Controversies -Democrats, Republicans, Libertarians, Conservatives, Liberals, Third Parties, Left-Wing, Right-Wi";
		StringBuffer s = new StringBuffer();
		for(int i = 0; i < 256; i++) {
			s.append("a");
		}
		files.add(s.toString());
		// 200 works
		// 256 does not
//		System.out.println("FuseTruncationProblem.getList() length = " + e.length());
		return files;
	}
}
