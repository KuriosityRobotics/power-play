package com.kuriosityrobotics.powerplay.client;

import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CheckBoxListListener;
import com.intellij.ui.JBColor;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import javax.swing.*;
import java.util.stream.Collectors;

public class RobotSelectionPane {
   private CheckBoxList<RobotDetails> checkBoxList1;
   public JPanel panel;

   private void createUIComponents() {
	  // A column containing the robot name, and a checkbox to enable/disable it
	  var model = new DefaultListModel<JCheckBox>();
	  checkBoxList1 = new CheckBoxList<>(model);

	  Orchestrators.addRobotAddedListener(robot -> {
		 for (int i = 0; i < model.size(); i++)
			if (model.get(i).getText().equals(robot.toString())) {
			   checkBoxList1.repaint();
			   return;
			}

		 checkBoxList1.addItem(robot, robot.toString(), true);
	  });
	  Orchestrators.addRobotAddedListener(robot -> checkBoxList1.repaint());
	  Orchestrators.addRobotRemovedListener(ignored -> checkBoxList1.repaint());

	  var oldRender = checkBoxList1.getCellRenderer();
	  checkBoxList1.setCellRenderer(
			  (list, value, index, isSelected, cellHasFocus) -> {
				 var rendered =
						 oldRender.getListCellRendererComponent(
								 list, value, index, isSelected, cellHasFocus);
				 if (Orchestrators.CONNECTED.stream()
						 .map(RobotDetails::toString)
						 .filter(n -> n.equals(value.getText()))
						 .findAny().isEmpty())
					rendered.setForeground(JBColor.RED);
				 else
					rendered.setForeground(JBColor.BLACK);

				 return rendered;
			  });

	  Orchestrators.CONNECTED.forEach(
			  n -> {
				 checkBoxList1.addItem(n, n.toString(), true);
			  });
   }

   public boolean isRobotSelected(RobotDetails robot) {
	  return checkBoxList1.isItemSelected(robot);
   }

   public void setCheckboxListListener(CheckBoxListListener listener) {
	  checkBoxList1.setCheckBoxListListener(listener);
   }
}
