package com.kuriosityrobotics.powerplay.client;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.kuriosityrobotics.powerplay.pubsub.bridge.MatCodec;
import com.kuriosityrobotics.powerplay.pubsub.bridge.Primitive64MatrixCodec;
import com.kuriosityrobotics.powerplay.pubsub.bridge.SerialisationConfig;
import com.nqzero.permit.Permit;
import org.jetbrains.annotations.NotNull;
import org.nustaq.serialization.FSTConfiguration;
import org.ojalgo.matrix.Primitive64Matrix;
import org.opencv.core.Mat;

public class DynamicReloadListener implements DynamicPluginListener {
	private static void openPackage(Module module, String packageName) {
		try {
			var export = Module.class.getDeclaredMethod("implAddOpens", String.class);

			Permit.setAccessible(export);
			export.invoke(module, packageName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void beforePluginLoaded(@NotNull IdeaPluginDescriptor pluginDescriptor) {
		DynamicPluginListener.super.beforePluginLoaded(pluginDescriptor);

		if ("Oracle Corporation".equals(System.getProperty("java.specification.vendor"))) {
			var javaBase = Object.class.getModule();

			javaBase.getPackages().forEach(p -> openPackage(javaBase, p));
		}

		SerialisationConfig.conf = FSTConfiguration.createDefaultConfiguration();
		SerialisationConfig.conf.registerSerializer(
				Primitive64Matrix.class, new Primitive64MatrixCodec(), true);
		SerialisationConfig.conf.registerSerializer(Mat.class, new MatCodec(), true);
	}

	@Override
	public void beforePluginUnload(
			@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
		DynamicPluginListener.super.pluginUnloaded(pluginDescriptor, isUpdate);

		SerialisationConfig.conf.clearCaches();
		SerialisationConfig.conf = null;

		try {
			var field = FSTConfiguration.class.getDeclaredField("singleton");
			field.setAccessible(true);
			field.set(null, null);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed to nullify FSTConfiguration singleton");
		}
	}
}
