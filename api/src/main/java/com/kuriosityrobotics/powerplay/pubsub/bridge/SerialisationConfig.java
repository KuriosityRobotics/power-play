package com.kuriosityrobotics.powerplay.pubsub.bridge;

import com.nqzero.permit.Permit;

import org.nustaq.serialization.FSTConfiguration;
import org.ojalgo.matrix.Primitive64Matrix;
import org.opencv.core.Mat;

public class SerialisationConfig {
	public static FSTConfiguration conf;

	private static void openPackage(Module module, String packageName) {
		try {
			var export = Module.class.getDeclaredMethod("implAddOpens", String.class);

			Permit.setAccessible(export);
			export.invoke(module, packageName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static {
		if ("Oracle Corporation".equals(System.getProperty("java.specification.vendor"))) {
			var javaBase = Object.class.getModule();

			javaBase.getPackages().forEach(p -> openPackage(javaBase, p));
		}

		SerialisationConfig.conf = FSTConfiguration.createAndroidDefaultConfiguration();
		SerialisationConfig.conf.registerSerializer(
				Primitive64Matrix.class, new Primitive64MatrixCodec(), true);

		try {
			SerialisationConfig.conf.registerSerializer(Mat.class, new MatCodec(), true);
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
	}
}
