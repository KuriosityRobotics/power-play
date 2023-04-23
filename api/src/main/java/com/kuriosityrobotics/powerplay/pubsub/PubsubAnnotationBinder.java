package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunnableAction;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

final class PubsubAnnotationBinder {
	private final Orchestrator orchestrator;

	public PubsubAnnotationBinder(Orchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	public void bindFields(Node node) {
		node.wrapException(
			() -> {
				for (Field field : node.getClass().getDeclaredFields()) {
					bindLastMessagePublishedIfPresent(node, field);
				}
				return null;
			});
	}

	public void bindMethods(Node node) {
		node.wrapException(
			() -> {
				for (Method method : node.getClass().getDeclaredMethods()) {
					bindSubscribedIfPresent(node, method);
					bindPeriodicallyIfPresent(node, method);
					bindActionIfPresent(node, method);
				}
				return null;
			});
	}

	private void bindActionIfPresent(Node node, Method method) throws InvocationTargetException, IllegalAccessException {
		var runnableActions = method.getAnnotationsByType(RunnableAction.class);

		if (runnableActions.length == 0) return;
		if (runnableActions.length > 1)
			orchestrator.warn("Multiple @RunnableAction annotations on method" + method);

		method.setAccessible(true);
		for (var runnableAction : runnableActions) {
			orchestrator.assertThat(
				runnableAction.actionName().length() > 0,
				"Action name length must be greater than 0"
			);
			method.setAccessible(true);
			Node.NodeTimer supplier = () -> {
				try {
					method.invoke(node);
				} catch (IllegalAccessException | InvocationTargetException e) {
					if (e.getCause() instanceof InterruptedException)
						return;

					throw new RuntimeException(e);
				}
			};
			orchestrator.addAction(node, runnableAction.actionName(), supplier);
		}
	}

	private void bindLastMessagePublishedIfPresent(Node node, Field field)
		throws IllegalAccessException {
		var lastMessagePublished = field.getAnnotation(LastMessagePublished.class);
		if (lastMessagePublished == null) return;
		field.setAccessible(true);

		if ((field.getModifiers() & Modifier.FINAL) != 0) {
			orchestrator.err(
				"LastMessagePublished fields must not be final:  " + field.getName() + " in " + node.getClass().getSimpleName() + ".  If the IDE is whinging, just slape a @SuppressWarnings(\"FieldMayBeFinal\") before the class declaration.");
			return;
		}
		orchestrator
			.getOrAddTopic(lastMessagePublished.topic(), field.getType())
			.addLastValueHandle(node, field);

		if (field.get(node) == null) {
			field.set(node, orchestrator.getTopic(lastMessagePublished.topic()).map(Topic::lastValue).orElse(null));
		}
	}

	private void bindPeriodicallyIfPresent(Node node, Method method) {
		var runPeriodicallyList = method.getAnnotationsByType(RunPeriodically.class);
		if (runPeriodicallyList.length == 0) return;

		if (runPeriodicallyList.length > 1)
			orchestrator.warn("Multiple @RunPeriodically annotations on method " + method);

		for (var runPeriodically : runPeriodicallyList) {
			method.setAccessible(true);
			orchestrator.assertThat(
				runPeriodically.maxFrequency() > 0, "frequency must be positive");

			method.setAccessible(true);
			node.startPeriodicTask(
				() -> node.wrapException(() -> method.invoke(node)),
				runPeriodically.maxFrequency());
			orchestrator.info("Started period:  " + method.getName());
		}
	}

	private void bindSubscribedIfPresent(Node node, Method method) {
		var subscribedList = method.getAnnotationsByType(SubscribedTo.class);
		if (subscribedList.length == 0) return;

		for (var subscribed : subscribedList) {
			method.setAccessible(true);
			MessageConsumer handle = getSubscriptionHandle(node, method);
			var messageType = method.getParameterTypes().length > 0 ? method.getParameterTypes()[0] : Object.class;

			Subscription sub;
			if (subscribed.isPattern()) {
				sub =
					orchestrator.subscribeToPattern(
						Pattern.compile(subscribed.topic()),
						(message, topicName, origin) -> {
							if (subscribed.onlyLocal()
								&& !origin.equals(orchestrator.robotDetails())) return;
							if (!messageType.isAssignableFrom(message.getClass())) {
								orchestrator.err("Method " + method.getName() + "'s parameter of type " + messageType.getSimpleName() + " is not applicable to topic message type " + message.getClass().getSimpleName());
								return; // should we warn here, or is it ok to just implicitly match on the type?
							}

							handle.accept(message, topicName, origin);
						});
			} else {
				sub =
					orchestrator.subscribe(
						subscribed.topic(),
						messageType,
						(message, topicName, origin) -> {
							if (subscribed.onlyLocal()
								&& !origin.equals(orchestrator.robotDetails())) return;
							handle.accept(message, topicName, origin);
						});
			}

			node.boundSubscriptions.add(sub);
		}
	}

	private static MessageConsumer<?, String, RobotDetails> getSubscriptionHandle(Node node, Method method) {
		var parameterCount = method.getParameterTypes().length;

		MessageConsumer<?, String, RobotDetails> consumer;
		if (parameterCount == 3)
			consumer = ((o, s, robotDetails) -> node.wrapException(() -> method.invoke(node, o, s, robotDetails)));
		else if (parameterCount == 2)
			consumer = ((o, s, robotDetails) -> node.wrapException(() -> method.invoke(node, o, s)));
		else if (parameterCount == 1)
			consumer = ((o, s, robotDetails) -> node.wrapException(() -> method.invoke(node, o)));
		else if (parameterCount == 0)
			consumer = ((o, s, robotDetails) -> node.wrapException(() -> method.invoke(node)));
		else
			throw new IllegalArgumentException("subscriptions must have 0-3 parameters " + method);


		return consumer;
	}

	/*
	var callSite = LambdaMetafactory.metafactory(
	// method handle lookup
	lookup,
	// name of the method defined in the target functional interface
	"accept",
	// type to be implemented and captured objects
	// in this case the Node instance is captured
	MethodType.methodType(Consumer.class, Node.class),
	MethodType.methodType(void.class, type.parameterType(0)),
	// method handle to transform
	handle,
	// Supplier method real signature (reified)
	// trim accepts no parameters and returns String
	MethodType.methodType(void.class, type.parameterType(0)));
	*/
}
