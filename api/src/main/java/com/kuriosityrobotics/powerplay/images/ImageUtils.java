package com.kuriosityrobotics.powerplay.images;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A class that can encode and decode an unserializeable mat to a byte[] that can be sent over the
 * Network Bridge.
 *
 * @see com.kuriosityrobotics.powerplay.pubsub.bridge.BidirectionalBridge
 *     <p>The SerialImage class's decode(byte[] bytes) depends on the size of the mat specified.
 *     Here is a full list of sizes: Key: [index number, which can be changed in the RobotCamera
 *     class] = width x height 0 = 640 x 480 1 = 160 x 90 2 = 160 x 120 3 = 176 x 144 4 = 320 x 180
 *     5 = 320 x 240 6 = 352 x 288 7 = 432 x 240 8 = 640 x 360 9 = 800 x 448 10 = 800 x 600 11 = 864
 *     x 480 12 = 960 x 720 13 = 1024 x 576 14 = 1280 x 720 15 = 1600 x 896 16 = 1920 x 1080 17 =
 *     2304 x 1296 18 = 2304 x 1536
 *     <p>We currently use the format 1920 x 1080, which is reflected in the decode method.
 *     <p>THIS MAY BE CHANGED IF WE NEED A HIGHER FPS.
 */
public class ImageUtils {

	/**
	 * Converts the given {@link Mat} to a {@link BufferedImage}
	 *
	 * @param m the {@link Mat} to convert
	 * @return the {@link BufferedImage} representation of the {@link Mat}
	 */
	public static BufferedImage matToBufferedImage(Mat m) throws IOException {
		var mob = new MatOfByte();
		Imgcodecs.imencode(".jpg", m, mob, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 40));
		var bytes = mob.toArray();
		return ImageIO.read(new ByteArrayInputStream(bytes));
	}
}
