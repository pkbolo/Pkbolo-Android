package com.pkbolo.securesms.ringrtc;


import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

import org.signal.ringrtc.CallConnection;
import org.signal.ringrtc.CallConnectionFactory;
import org.signal.ringrtc.CallException;
import org.signal.ringrtc.SignalMessageRecipient;

import com.pkbolo.securesms.logging.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Capturer;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.UnregisteredUserException;

public class CallConnectionWrapper {
  private static final String TAG = Log.tag(CallConnectionWrapper.class);

  @NonNull  private final CallConnection callConnection;
  @NonNull  private final AudioTrack     audioTrack;
  @NonNull  private final AudioSource    audioSource;
  @NonNull  private final Camera         camera;
  @Nullable private final VideoSource    videoSource;
  @Nullable private final VideoTrack     videoTrack;

  public CallConnectionWrapper(@NonNull Context                     context,
                               @NonNull CallConnectionFactory       factory,
                               @NonNull CallConnection.Observer     observer,
                               @NonNull VideoSink                   localRenderer,
                               @NonNull CameraEventListener         cameraEventListener,
                               @NonNull EglBase                     eglBase,
                               boolean                              hideIp,
                               long                                 callId,
                               boolean                              outBound,
                               @NonNull SignalMessageRecipient      recipient,
                               @NonNull SignalServiceAccountManager accountManager)
    throws UnregisteredUserException, IOException, CallException
  {

    CallConnection.Configuration configuration = new CallConnection.Configuration(callId,
                                                                                  outBound,
                                                                                  recipient,
                                                                                  accountManager,
                                                                                  hideIp);

    this.callConnection = factory.createCallConnection(configuration, observer);
    this.callConnection.setAudioPlayout(false);
    this.callConnection.setAudioRecording(false);

    MediaStream      mediaStream      = factory.createLocalMediaStream("ARDAMS");
    MediaConstraints audioConstraints = new MediaConstraints();

    audioConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
    this.audioSource = factory.createAudioSource(audioConstraints);
    this.audioTrack  = factory.createAudioTrack("ARDAMSa0", audioSource);
    this.audioTrack.setEnabled(false);
    mediaStream.addTrack(audioTrack);

    this.camera = new Camera(context, cameraEventListener);

    if (camera.capturer != null) {
      this.videoSource = factory.createVideoSource(false);
      this.videoTrack  = factory.createVideoTrack("ARDAMSv0", videoSource);

      camera.capturer.initialize(SurfaceTextureHelper.create("WebRTC-SurfaceTextureHelper", eglBase.getEglBaseContext()), context, videoSource.getCapturerObserver());

      this.videoTrack.addSink(localRenderer);
      this.videoTrack.setEnabled(false);
      mediaStream.addTrack(videoTrack);
    } else {
      this.videoSource = null;
      this.videoTrack  = null;
    }

    this.callConnection.addStream(mediaStream);
  }

  public boolean addIceCandidate(IceCandidate candidate) {
    return callConnection.addIceCandidate(candidate);
  }

  public void sendOffer() throws CallException {
    callConnection.sendOffer();
  }

  public boolean validateResponse(SignalMessageRecipient recipient, Long inCallId)
    throws CallException
  {
    return callConnection.validateResponse(recipient, inCallId);
  }

  public void handleOfferAnswer(String sessionDescription) throws CallException {
    callConnection.handleOfferAnswer(sessionDescription);
  }

  public void acceptOffer(String offer) throws CallException {
    callConnection.acceptOffer(offer);
  }

  public void hangUp() throws CallException {
    callConnection.hangUp();
  }

  public void answerCall() throws CallException {
    callConnection.answerCall();
  }

  public void setVideoEnabled(boolean enabled) throws CallException {
    if (videoTrack != null) {
      videoTrack.setEnabled(enabled);
    }
    camera.setEnabled(enabled);
    callConnection.sendVideoStatus(enabled);
  }

  public void flipCamera() {
    camera.flip();
  }

  public CameraState getCameraState() {
    return new CameraState(camera.getActiveDirection(), camera.getCount());
  }

  public void setCommunicationMode() {
    callConnection.setAudioPlayout(true);
    callConnection.setAudioRecording(true);
  }

  public void setAudioEnabled(boolean enabled) {
    audioTrack.setEnabled(enabled);
  }

  public void dispose() {
    camera.dispose();

    if (videoSource != null) {
      videoSource.dispose();
    }

    audioSource.dispose();
    callConnection.dispose();
  }

  private static class Camera implements CameraVideoCapturer.CameraSwitchHandler {

    @Nullable
    private final CameraVideoCapturer   capturer;
    private final CameraEventListener   cameraEventListener;
    private final int                   cameraCount;

    private CameraState.Direction activeDirection;
    private boolean               enabled;

    Camera(@NonNull Context context, @NonNull CameraEventListener cameraEventListener)
    {
      this.cameraEventListener    = cameraEventListener;
      CameraEnumerator enumerator = getCameraEnumerator(context);
      cameraCount                 = enumerator.getDeviceNames().length;

      CameraVideoCapturer capturerCandidate = createVideoCapturer(enumerator, CameraState.Direction.FRONT);
      if (capturerCandidate != null) {
        activeDirection = CameraState.Direction.FRONT;
      } else {
        capturerCandidate = createVideoCapturer(enumerator, CameraState.Direction.BACK);
        if (capturerCandidate != null) {
          activeDirection = CameraState.Direction.BACK;
        } else {
          activeDirection = CameraState.Direction.NONE;
        }
      }
      capturer = capturerCandidate;
    }

    void flip() {
      if (capturer == null || cameraCount < 2) {
        throw new AssertionError("Tried to flip the camera, but we only have " + cameraCount +
                                 " of them.");
      }
      activeDirection = CameraState.Direction.PENDING;
      capturer.switchCamera(this);
    }

    void setEnabled(boolean enabled) {
      this.enabled = enabled;

      if (capturer == null) {
        return;
      }

      try {
        if (enabled) {
          capturer.startCapture(1280, 720, 30);
        } else {
          capturer.stopCapture();
        }
      } catch (InterruptedException e) {
        Log.w(TAG, "Got interrupted while trying to stop video capture", e);
      }
    }

    void dispose() {
      if (capturer != null) {
        capturer.dispose();
      }
    }

    int getCount() {
      return cameraCount;
    }

    @NonNull CameraState.Direction getActiveDirection() {
      return enabled ? activeDirection : CameraState.Direction.NONE;
    }

    @Nullable CameraVideoCapturer getCapturer() {
      return capturer;
    }

    private @Nullable CameraVideoCapturer createVideoCapturer(@NonNull CameraEnumerator enumerator,
                                                              @NonNull CameraState.Direction direction)
    {
      String[] deviceNames = enumerator.getDeviceNames();
      for (String deviceName : deviceNames) {
        if ((direction == CameraState.Direction.FRONT && enumerator.isFrontFacing(deviceName)) ||
            (direction == CameraState.Direction.BACK  && enumerator.isBackFacing(deviceName)))
        {
          return enumerator.createCapturer(deviceName, null);
        }
      }

      return null;
    }

    private @NonNull CameraEnumerator getCameraEnumerator(@NonNull Context context) {
      boolean camera2EnumeratorIsSupported = false;
      try {
        camera2EnumeratorIsSupported = Camera2Enumerator.isSupported(context);
      } catch (final Throwable throwable) {
        Log.w(TAG, "Camera2Enumator.isSupport() threw.", throwable);
      }

      Log.i(TAG, "Camera2 enumerator supported: " + camera2EnumeratorIsSupported);

      return camera2EnumeratorIsSupported ? new FilteredCamera2Enumerator(context)
                                          : new Camera1Enumerator(true);
    }

    @Override
    public void onCameraSwitchDone(boolean isFrontFacing) {
      activeDirection = isFrontFacing ? CameraState.Direction.FRONT : CameraState.Direction.BACK;
      cameraEventListener.onCameraSwitchCompleted(new CameraState(getActiveDirection(), getCount()));
    }

    @Override
    public void onCameraSwitchError(String errorMessage) {
      Log.e(TAG, "onCameraSwitchError: " + errorMessage);
      cameraEventListener.onCameraSwitchCompleted(new CameraState(getActiveDirection(), getCount()));
    }
  }

  public interface CameraEventListener {
    void onCameraSwitchCompleted(@NonNull CameraState newCameraState);
  }

  @TargetApi(21)
  private static class FilteredCamera2Enumerator extends Camera2Enumerator {

    @NonNull  private final Context       context;
    @Nullable private final CameraManager cameraManager;
    @Nullable private       String[]      deviceNames;

    FilteredCamera2Enumerator(@NonNull Context context) {
      super(context);

      this.context       = context;
      this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      this.deviceNames   = null;
    }

    private boolean isMonochrome(String deviceName, CameraManager cameraManager) {

      try {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(deviceName);
        int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

        if (capabilities != null) {
          for (int cap : capabilities) {
            if (cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME) {
              return true;
            }
          }
        }
      } catch (CameraAccessException e) {
        return false;
      }

      return false;
    }

    private boolean isLensFacing(String deviceName, CameraManager cameraManager, Integer facing) {

      try {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(deviceName);
        Integer               lensFacing      = characteristics.get(CameraCharacteristics.LENS_FACING);

        return facing.equals(lensFacing);
      } catch (CameraAccessException e) {
        return false;
      }

    }

    @Override
    public @NonNull String[] getDeviceNames() {

      if (deviceNames != null) {
        return deviceNames;
      }

      try {
        List<String> cameraList = new LinkedList<>();

        if (cameraManager != null) {
          // While skipping cameras that are monochrome, gather cameras
          // until we have at most 1 front facing camera and 1 back
          // facing camera.

          List<String> devices = Stream.of(cameraManager.getCameraIdList())
                                       .filterNot(id -> isMonochrome(id, cameraManager))
                                       .toList();

          String frontCamera = Stream.of(devices)
                                     .filter(id -> isLensFacing(id, cameraManager, CameraMetadata.LENS_FACING_FRONT))
                                     .findFirst()
                                     .orElse(null);

          if (frontCamera != null) {
            cameraList.add(frontCamera);
          }

          String backCamera = Stream.of(devices)
                                    .filter(id -> isLensFacing(id, cameraManager, CameraMetadata.LENS_FACING_BACK))
                                    .findFirst()
                                    .orElse(null);

          if (backCamera != null) {
            cameraList.add(backCamera);
          }
        }

        this.deviceNames = cameraList.toArray(new String[0]);
      } catch (CameraAccessException e) {
        Log.e(TAG, "Camera access exception: " + e);
        this.deviceNames = new String[] {};
      }

      return deviceNames;
    }

    @Override
    public @NonNull CameraVideoCapturer createCapturer(@Nullable String deviceName,
                                                       @Nullable CameraVideoCapturer.CameraEventsHandler eventsHandler) {
      return new Camera2Capturer(context, deviceName, eventsHandler, new FilteredCamera2Enumerator(context));
    }
  }
}
