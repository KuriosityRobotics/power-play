package com.kuriosityrobotics.powerplay.cameras;

import android.graphics.ImageFormat;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.kuriosityrobotics.powerplay.images.ImageUtils;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.NamedHardware;
import com.kuriosityrobotics.powerplay.pubsub.Node;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.function.Continuation;
import org.firstinspires.ftc.robotcore.external.hardware.camera.Camera;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureRequest;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCaptureSession.StateCallbackDefault;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraException;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraManager;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.internal.network.CallbackLooper;
import org.firstinspires.ftc.robotcore.internal.system.ContinuationSynchronizer;
import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nu.pattern.OpenCV;

/**
 * This Node takes in an Android camera, extracts native Android Bitmaps from it, and converts them
 * into OpenCV Mat messages on topic <code>webcamFrame</code>.
 */
public class RightCamera extends Node implements AutoCloseable {
  private Camera camera;
  private Handler callbackHandler;
  private CameraManager cameraManager;

  @NamedHardware("Webcam 2")
  private WebcamName cameraName;

  private CameraCaptureSession cameraCaptureSession;
  private boolean isOpen;

  private static final int MILLIS_PERMISSION_TIMEOUT = Integer.MAX_VALUE;
  private static final int IMAGE_FORMAT = ImageFormat.YUY2; // YUY2 is JPG when saved to the computer

  // load libraries
  static {
    OpenCV.loadShared();
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
  }

  /**
   * Constructs a robot camera that extracts frames from the webcam
   *
   * @param orchestrator the orchestrator to use
   * @throws RuntimeException if an error was occurred when starting the camera. An exception is
   *     only thrown when DEBUG mode is turned on in the Robot class.
   */
  public RightCamera(HardwareOrchestrator orchestrator) throws RuntimeException {
    super(orchestrator);

    if (cameraName.isAttached()) {
      initialize();
      openCamera();
      isOpen = true;
      try {
        startCamera();
      } catch (CameraException | RuntimeException e) {
        String msg = "An " + e.getClass().getName() + " error occurred. " + e.getLocalizedMessage();
        err(msg);
        close();
      }
    } else {
      String msg = "Camera is not attached! Please check the USB connection and try again.";
      err(msg);
    }

    isOpen = true;
  }

  /** Gets the CallbackHandler and CameraManager needed to run image collection. */
  private void initialize() {
    this.callbackHandler = CallbackLooper.getDefault().getHandler();
    this.cameraManager = ClassFactory.getInstance().getCameraManager();
  }

  /** Opens the camera and gets it ready to collect images. */
  private void openCamera() {
    if (camera != null) return;

    Deadline deadline = new Deadline(MILLIS_PERMISSION_TIMEOUT, TimeUnit.MILLISECONDS);
    camera = cameraManager.requestPermissionAndOpenCamera(deadline, cameraName, null);
    orchestrator.assertThat(
        camera != null,
        String.format(
            Locale.US, "camera not found or permission to use not granted: %s", cameraName));
  }

  /**
   * Starts collecting images for our camera!
   *
   * @see ImageUtils for details on the sizes for the logitech c920 webcam
   * @throws CameraException if a CameraCaptureSession could not be created
   * @throws RuntimeException if we could not start our capture
   */
  private void startCamera() throws CameraException, RuntimeException {
    if (cameraCaptureSession != null) return;

    var cameraCharacteristics = cameraName.getCameraCharacteristics();
    var sizes = cameraCharacteristics.getSizes(IMAGE_FORMAT);
    var size = sizes[16];
    var fps = 4;

    final ContinuationSynchronizer<CameraCaptureSession> synchronizer =
        new ContinuationSynchronizer<>();

    camera.createCaptureSession(
        Continuation.create(
            callbackHandler,
            new StateCallbackDefault() {
              @Override
              public void onConfigured(@NonNull CameraCaptureSession session) {
                final CameraCaptureRequest cameraCaptureRequest;
                try {
                  cameraCaptureRequest = camera.createCaptureRequest(IMAGE_FORMAT, size, fps);
                  session.startCapture(
                      cameraCaptureRequest,
                      (session1, request, cameraFrame) -> {
                        var bmp = cameraCaptureRequest.createEmptyBitmap();
                        cameraFrame.copyToBitmap(bmp);
                        var mat = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
                        Utils.bitmapToMat(bmp, mat);

						orchestrator.dispatch("webcamFrame", mat);

                        bmp.recycle();
                      },
                      Continuation.create(
                          callbackHandler,
                          ((session1, cameraCaptureSequenceId, lastFrameNumber) ->
                              debug(
                                  "<"
                                      + cameraName
                                      + "> finished capturing in session "
                                      + session1
                                      + " with id "
                                      + cameraCaptureSequenceId
                                      + ", capturing "
                                      + lastFrameNumber
                                      + " total frames."))));
                  synchronizer.finish(session);
                } catch (CameraException | RuntimeException e) {
                  session.close();
                  synchronizer.finish(null);
                  String msg =
                      "There was an exception starting the camera capture sequence. More info: "
                          + e;
                  err(msg);
                  throw new RuntimeException(msg);
                }
              }
            }));

    try {
      synchronizer.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    cameraCaptureSession = synchronizer.getValue();
    assertThat(
        cameraCaptureSession != null,
        "The Camera Session was null, please check the USB connection!");
  }

  /** Closes the camera to ensure the capture is not running when the OpMode is not active. */
  @Override
  public void close() { // certified android moment
	  super.close();
    if (cameraCaptureSession != null) {
      cameraCaptureSession.stopCapture();
      cameraCaptureSession.close();
      cameraCaptureSession = null;
    }
    if (camera != null) {
      camera.close();
      camera = null;
    }

    isOpen = false;
  }

  @Override
  protected void finalize() {
    if (orchestrator.assertThat(isOpen, "RobotCamera.close() needs to be called!")) {
      close();
    }
  }
}
