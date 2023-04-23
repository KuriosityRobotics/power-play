package com.kuriosityrobotics.powerplay.pubsub.dynamic;

import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.SerialisationConfig;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Hidden
public class DynamicClassResolver extends Node {
   private final Set<ClassRequest> requested = ConcurrentHashMap.newKeySet();

   public DynamicClassResolver(Orchestrator orchestrator) {
		super(orchestrator);
	}

	private byte[] getBytes(String className) {
		return wrapException(
				() -> {
					try (var stream =
							Orchestrator.class
									.getClassLoader()
									.getResourceAsStream(className.replace('.', '/') + ".class")) {
						if (stream == null) return null;

						return stream.readAllBytes();
					}
				});
	}

   @SubscribedTo(topic = "dynamic/class/request")
	public void resolve(ClassRequest request, String topicName, RobotDetails origin) {
	   if (origin.equals(orchestrator.robotDetails())) { // don't try to load classes if they were requested locally
		  requested.add(request);
		  return;
	   }

	   info("Resolving class " + request.className());
	   var bytes = getBytes(request.className());
		if (bytes == null) {
			warn("Class " + request.className() + " not found locally");
			return;
		}

		orchestrator.dispatch("dynamic/class/resource", new ClassResource(request, bytes));
	}


	@SubscribedTo(topic = "dynamic/class/resource")
	public synchronized void onClassResource(ClassResource resource) {
		if (requested.remove(resource.request())) {
		   ByteClassLoader.INSTANCE.defineClass(resource.className(), resource.bytes());

		   info("Resolved class " + resource.className());
		}
	}

	private static class ByteClassLoader extends ClassLoader {
		static final ByteClassLoader INSTANCE = new ByteClassLoader();
		private final ReentrantLock classDefineLock = new ReentrantLock();

		private ByteClassLoader() {
			super(DynamicClassResolver.class.getClassLoader());
			SerialisationConfig.conf.setClassLoader(this);
		}

		void defineClass(String name, byte[] bytes) {
		   classDefineLock.lock();
			defineClass(name, bytes, 0, bytes.length);
			classDefineLock.unlock();
		}
	}
}
