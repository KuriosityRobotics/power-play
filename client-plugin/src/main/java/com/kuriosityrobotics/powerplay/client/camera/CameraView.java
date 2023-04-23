package com.kuriosityrobotics.powerplay.client.camera;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.panels.HorizontalBox;
import com.intellij.uiDesigner.core.GridConstraints;
import com.kuriosityrobotics.powerplay.images.ImageUtils;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.Subscription;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.swing.*;

import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CameraView extends JPanel {
   private final Orchestrator orchestrator;
   private final Map<RobotDetails, JPanel> robotLayouts = new ConcurrentHashMap<>();
   private final Map<RobotDetails, Map<String, JLabel>> robotLabels = new ConcurrentHashMap<>();

   public CameraView(Orchestrator orchestrator) {
	  super(new BorderLayout());
	  this.orchestrator = orchestrator;
	  setVisible(true);
	  orchestrator.startNode("camera/helper", new CameraViewHelper());
   }

   @Hidden
   public class CameraViewHelper extends Node {
	  protected CameraViewHelper() {
		 super(CameraView.this.orchestrator);
	  }

	  @SubscribedTo(topic = ".*", isPattern = true)
	  public void onMat(Object mat_, String topicName, RobotDetails details) {
		  if (!(mat_ instanceof Mat))
			  return;
		  var mat = (Mat) mat_;
		 var converted = wrapException(() -> ImageUtils.matToBufferedImage(mat));
		 if (converted == null)
			return;

		 var layout = robotLayouts.computeIfAbsent(details, k -> {
			var panel = new JPanel(new GridLayout(0, 1));
			CameraView.this.add(panel, BorderLayout.NORTH);

			return panel;
		 });
		 var labels = robotLabels.computeIfAbsent(details, k -> new ConcurrentHashMap<>());
		 var label = labels.computeIfAbsent(topicName, k -> {
			info("Creating image display for topic " + topicName + " on robot " + details);
			var label_ = new JLabel();
			label_.setHorizontalTextPosition(JLabel.CENTER);
			label_.setVerticalTextPosition(JLabel.TOP);
			label_.setHorizontalAlignment(JLabel.CENTER);
			label_.setVerticalAlignment(JLabel.TOP);
			label_.setForeground(JBColor.green);
			label_.setFont(label_.getFont().deriveFont(Font.BOLD).deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)));
			label_.setText(topicName + " on " + details);

			var currentLayout = (GridLayout)layout.getLayout();
			var newSize = currentLayout.getRows() + 1;
			layout.setLayout(new GridLayout(newSize, 1));
			layout.add(label_);
			for (var comp : layout.getComponents())
			   comp.setPreferredSize(new Dimension(CameraView.this.getWidth() / CameraView.this.getComponentCount(), CameraView.this.getHeight() / newSize));

			return label_;
		 });

		 label.setIcon(new StretchIcon(converted, true));
		 label.repaint();
	  }
   }
}
