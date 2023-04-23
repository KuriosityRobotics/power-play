package com.kuriosityrobotics.powerplay.client.field;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.kuriosityrobotics.powerplay.client.Orchestrators;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.jetbrains.annotations.NotNull;

public class FieldFactory implements ToolWindowFactory {
	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		toolWindow.setTitle("Field");

		var toolWindowContent = new Field(Orchestrators.CLIENT);

		var content =
				ContentFactory.SERVICE.getInstance().createContent(toolWindowContent, "", true);
		content.setPreferredFocusableComponent(toolWindowContent);
		toolWindow.getContentManager().addContent(content);
		content.getPreferredFocusableComponent()
				.addMouseListener(
						new MouseAdapter() {
							@Override
							public void mousePressed(MouseEvent e) {
								super.mousePressed(e);
								toolWindowContent.requestFocusInWindow();
							}
						});
		content.getPreferredFocusableComponent()
				.addKeyListener(new ArrowKeyController(Orchestrators.CLIENT));
	}
}
