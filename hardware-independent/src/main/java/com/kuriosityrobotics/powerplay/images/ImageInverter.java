package com.kuriosityrobotics.powerplay.images;

import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ImageInverter extends Node {
   public ImageInverter(Orchestrator orchestrator) {
	  super(orchestrator);
   }

   @SubscribedTo(topic = "cameraFrame")
   public void onImage(Mat mat) {
	  var result = new Mat();
	  Imgproc.cvtColor(mat, result, Imgproc.COLOR_BGR2RGB);
	  orchestrator.dispatch("invertedImageFrame", result);
   }
}