package com.kuriosityrobotics.powerplay.cameras;

import static org.opencv.imgproc.Imgproc.COLOR_HLS2BGR;
import static org.opencv.imgproc.Imgproc.LINE_8;

import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openftc.apriltag.AprilTagDetectorJNI;

import java.util.Comparator;

public class VisionTask extends Node {
	private static final Rect subRect = new Rect(new Point(410,560),new Point(480,580));


	private long nativeApriltagPtr;

	public VisionTask(Orchestrator orch) {
		super(orch);
		 nativeApriltagPtr = AprilTagDetectorJNI.createApriltagDetector(AprilTagDetectorJNI.TagFamily.TAG_standard41h12.string, 3, 3);
	}

	@SubscribedTo(topic = "webcamFrame")
	public void processFrame(Mat input) {
		if (input.empty())
			return;

		Mat grey = new Mat();
		Imgproc.cvtColor(input, grey, Imgproc.COLOR_RGB2GRAY);
		var result = AprilTagDetectorJNI.runAprilTagDetectorSimple(
			nativeApriltagPtr,
			grey,
			.042,
			1385.92,1385.92,951.982,534.084
		);
		if (result.size() > 0) {
			var tag = result.stream().max(Comparator.comparing(c -> c.decisionMargin)).get();
			// vision/parkingspot
			switch (tag.id) {
				case 202:
					orchestrator.dispatch("vision/parkingspot", 3);
					break;
				case 201:
					orchestrator.dispatch("vision/parkingspot", 2);
					break;
				case 200:
				default:
					orchestrator.dispatch("vision/parkingspot", 1);
			}
		}
	}

	@Override
	public void close() {
		super.close();
		AprilTagDetectorJNI.releaseApriltagDetector(nativeApriltagPtr);
	}
}