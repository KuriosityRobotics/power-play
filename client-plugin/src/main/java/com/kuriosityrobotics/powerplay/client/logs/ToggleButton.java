package com.kuriosityrobotics.powerplay.client.logs;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.NlsActions;
import com.intellij.ui.TextIcon;
import com.intellij.ui.ToggleActionButton;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToggleButton extends ToggleActionButton {
	private final String text;
	private final Consumer<Boolean> listener;
	private boolean enabled;

	public ToggleButton(
			@NlsActions.ActionText String text, Consumer<Boolean> listener, boolean defaultValue) {
		super(text, null);
		var icon = new TextIcon(text, UIUtil.getLabelForeground(), null, 1);
		icon.setFont(UIUtil.getLabelFont());

		getTemplatePresentation().setIcon(icon);

		var disabledIcon = new TextIcon(text, UIUtil.getLabelDisabledForeground(), null, 1);
		disabledIcon.setFont(UIUtil.getLabelFont());
		getTemplatePresentation().setDisabledIcon(disabledIcon);

		this.text = text;
		this.listener = listener;
		setVisible(true);
		this.enabled = defaultValue;
		listener.consume(defaultValue);
	}

	public ToggleButton(@NlsActions.ActionText String text, Consumer<Boolean> listener) {
		this(text, listener, true);
	}

	private static TextIcon getIcon(String text) {
		return new TextIcon(
				text,
				UIUtil.getLabelForeground(),
				JBUI.CurrentTheme.ActionButton.pressedBackground(),
				1);
	}

	public static MutableActionGroup createActionGroup(ToggleButton... buttons) {
		return new MutableActionGroup() {
			{
				for (var button : buttons) {
					add(button);
				}
			}
		};
	}

	private boolean isTempTriggered = false;
	// below is a horrifically sketchy hack to get around the fact that the button is not actually
	// triggered when the mouse is hovered
	// abusing icons to do this is a bad idea, but it works for now
	public void setupTempTriggerAndHoverColour(Color color) {
		var transparent = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
		var delegate = new TextIcon(text, UIUtil.getLabelForeground(), transparent, 1);
		var icon =
				new Icon() {
					@Override
					public void paintIcon(Component component, Graphics graphics, int i, int i1) {
						delegate.paintIcon(component, graphics, i, i1);
						if (!enabled && !isTempTriggered) {
							isTempTriggered = true;
							listener.consume(true);
						}
					}

					@Override
					public int getIconWidth() {
						return delegate.getIconWidth();
					}

					@Override
					public int getIconHeight() {
						return delegate.getIconHeight();
					}
				};
		delegate.setFont(UIUtil.getLabelFont());
		getTemplatePresentation().setHoveredIcon(icon);

		var originalIcon = getTemplatePresentation().getIcon();
		var newDefaultIcon =
				new Icon() {
					@Override
					public void paintIcon(Component component, Graphics graphics, int i, int i1) {
						originalIcon.paintIcon(component, graphics, i, i1);
						if (!enabled && isTempTriggered) {
							isTempTriggered = false;
							listener.consume(false);
						}
					}

					@Override
					public int getIconWidth() {
						return originalIcon.getIconWidth();
					}

					@Override
					public int getIconHeight() {
						return originalIcon.getIconHeight();
					}
				};

		getTemplatePresentation().setIcon(newDefaultIcon);
	}

	@Override
	public boolean isSelected(AnActionEvent e) {
		return enabled;
	}

	@Override
	public void setSelected(AnActionEvent e, boolean state) {
		enabled = state;
		listener.consume(state);
	}

	public static class MutableActionGroup extends ActionGroup {
		private final List<AnAction> actions = new ArrayList<>();

		public void add(AnAction action) {
			actions.add(action);
		}

		@Override
		@NotNull
		public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
			return actions.toArray(new AnAction[0]);
		}
	}
}
