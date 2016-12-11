import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.jcraft.jsch.JSchException;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.command.CommandRunner.ChannelExecWrapper;
import com.pastdev.jsch.nio.file.UnixSshFileSystem;
import com.pastdev.jsch.nio.file.UnixSshFileSystemProvider;
import com.pastdev.jsch.nio.file.UnixSshPath;

/**
 * SSHD uses slf4j. So add the api + binding jars, and point to a properties
 * file
 */
// @Grab(group='com.pastdev', module='jsch-nio', version='0.1.5')
public class Test {
	public static void main(String[] args) throws URISyntaxException, IOException {
//		System.err.println(StringEscapeUtils.escapeHtml4("$"));
//		System.out.println("\u0024"); 
		//&#36;
		System.out.println(escapeDollarSign("Hello $")
//				.replace("\u0024", "____"))
				);
	}
	
	private static String escapeDollarSign(String input) {
	    StringBuilder b = new StringBuilder();

	    for (char c : input.toCharArray()) {
	        if (c == '\u0024'){
	            b.append("__0024__");//"\\u").append(String.format("%04X", (int) c));
	        }
	        else {
	            b.append(c);
	        }
	    }

	    return b.toString();
	}

}
