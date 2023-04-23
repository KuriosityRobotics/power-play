package com.kuriosityrobotics.powerplay.images;

import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.Subscription;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;

/**
 * This is a Basic Class that subscribes to RobotCamera and saves those images for later analysis.
 * This runs about 3-5 times a second, but can be further ratelimited.
 */
public class ImageDumper extends Node {
	private final Subscription<Mat> imageSubscription;
	private int frameNum = 0;

	/**
	 * Registers the node with the orchestrator using {@link Orchestrator#}. This will NOT start
	 * calling the update function, and will give this node instance a placeholder name
	 *
	 * @param orchestrator the orchestrator on which to start the node
	 */
	public ImageDumper(Orchestrator orchestrator) {
		super(orchestrator);

		this.imageSubscription =
				orchestrator.subscribe(
						"webcamFrame",
						Mat.class,
						mat -> {
							var mob = new MatOfByte();
							Imgcodecs.imencode(".jpg", mat, mob, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 40));

						   boolean android = "The Android Project".equals(System.getProperty("java.specification.vendor"));
						   var storageDirectory = android ? "/storage/self/primary/FIRST/data" : "./data";
						   new File(storageDirectory).mkdirs();
						   try (var stream =
									new FileOutputStream(
											storageDirectory + "/webcam-frame-"
													+ frameNum++
													+ ".jpg")) {
								stream.write(mob.toArray());
							} catch (Exception e) {
								err(e.getLocalizedMessage());
							}

							mob.release();
						});
	}
}
