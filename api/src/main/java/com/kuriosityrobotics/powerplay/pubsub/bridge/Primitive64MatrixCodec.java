package com.kuriosityrobotics.powerplay.pubsub.bridge;

import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.ojalgo.matrix.Primitive64Matrix;

import java.io.IOException;

public class Primitive64MatrixCodec extends FSTBasicObjectSerializer {
	@Override
	public void writeObject(
			FSTObjectOutput out,
			Object toWrite,
			FSTClazzInfo clzInfo,
			FSTClazzInfo.FSTFieldInfo referencedBy,
			int streamPosition)
			throws IOException {
		out.writeObject(((Primitive64Matrix) toWrite).toRawCopy2D());
	}

	@Override
	public Object instantiate(
			Class objectClass,
			FSTObjectInput in,
			FSTClazzInfo serializationInfo,
			FSTClazzInfo.FSTFieldInfo referencee,
			int streamPosition)
			throws Exception {
		return Primitive64Matrix.FACTORY.rows((double[][]) in.readObject());
	}
}
