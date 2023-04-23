package com.kuriosityrobotics.powerplay.pubsub.bridge;

import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MatCodec extends FSTBasicObjectSerializer {
   ByteBuffer buffer;
   MatOfByte mob = new MatOfByte();


   @Override
   public synchronized void writeObject(
		   FSTObjectOutput out,
		   Object toWrite,
		   FSTClazzInfo clzInfo,
		   FSTClazzInfo.FSTFieldInfo referencedBy,
		   int streamPosition)
		   throws IOException {
	  var mat = (Mat) toWrite;

	  // Make width 150 pixels
	  var resized = new Mat();
	  Imgproc.resize(mat, resized, new Size(400, mat.rows() * 400. / mat.cols()));
	  Imgcodecs.imencode(".jpg", resized, mob, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 40));
	  resized.release();
	  var compressedSize = (int) mob.total();

	  if (buffer == null || buffer.capacity() < compressedSize)
		 buffer = ByteBuffer.allocate(compressedSize);
	  buffer.rewind();

	  mob.get(0, 0, buffer.array());

	  out.writeInt(compressedSize);
	  out.write(buffer.array(), 0, compressedSize);
   }

   @Override
   public synchronized Object instantiate(
		   Class objectClass,
		   FSTObjectInput in,
		   FSTClazzInfo serializationInfo,
		   FSTClazzInfo.FSTFieldInfo referencee,
		   int streamPosition)
		   throws Exception {
	  var compressedSize = in.readInt();
	  if (buffer == null || buffer.capacity() < compressedSize)
		 buffer = ByteBuffer.allocate(compressedSize);

	  in.read(buffer.array(), 0, compressedSize);

	  if (mob.total() < compressedSize) {
		 mob.release();
		 mob = new MatOfByte(buffer.array());
	  } else
		 mob.put(0, 0, buffer.array());

	  return Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
   }
}
