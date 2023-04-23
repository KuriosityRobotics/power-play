package com.kuriosityrobotics.powerplay.cameras;

import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.Publisher;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class LocalCamera extends Node {
	private final VideoCapture camera;
	private final Mat buffer;
	private final Publisher<Mat> imageFrame;

	public LocalCamera(Orchestrator orchestrator) {
		super(orchestrator);
		this.buffer = new Mat();
		this.imageFrame = orchestrator.publisher("cameraFrame", Mat.class);

		VideoCapture camera = null;
		int i = 0;
		while (i < 5 && !(camera = new VideoCapture(i++)).isOpened())
			; // try all cameras until one works

		if (!camera.isOpened()) err("Could not find a camera");
		else info("Opened camera " + i);

		this.camera = camera;
	}

	@RunPeriodically(maxFrequency = 30)
	public void grabFrame() {
		if (camera.read(buffer)) {
			orchestrator.dispatch("cameraFrame", buffer);
		}
	}
}
