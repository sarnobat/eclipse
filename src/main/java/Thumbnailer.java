import java.io.File;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;

public class Thumbnailer {

	public static void main(String[] args) throws Exception {
		System.out.println("Begin");
		FFmpegFrameGrabber g = new FFmpegFrameGrabber(
				"/Applications/eclipse3.6-corrupted by groovy/plugins/com.google.gwt.eclipse.sdkbundle_2.4.0.v201203300216-rel-r36/gwt-2.4.0/samples/MobileWebApp/src/main/webapp/video/tutorial.mp4");

		g.start();
g.setFrameNumber(4);
		for (int i = 0; i < 3; i++) {
			ImageIO.write(g.grab().getBufferedImage(), "jpg", new File(
					"/sarnobat.garagebandbroken/trash/video-frame-" + System.currentTimeMillis()
							+ ".jpg"));
		}

		g.stop();

	}

}