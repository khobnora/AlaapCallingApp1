package com.example.call

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.data.IceCandidateRecord
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class WebRtcCallManager(
  private val context: Context,
  private val onLocalOfferCreated: (String) -> Unit,
  private val onLocalAnswerCreated: (String) -> Unit,
  private val onLocalIceCandidate: (sdpMid: String, sdpMLineIndex: Int, candidate: String) -> Unit,
) {
  companion object {
    private const val TAG = "WebRtcCallManager"
  }

  val eglBase: EglBase = EglBase.create()
  private var factory: PeerConnectionFactory? = null
  private var peerConnection: PeerConnection? = null
  private var videoCapturer: CameraVideoCapturer? = null
  private var surfaceTextureHelper: SurfaceTextureHelper? = null
  private var localAudioSource: AudioSource? = null
  private var localVideoSource: VideoSource? = null
  private var localAudioTrack: AudioTrack? = null
  private var localVideoTrack: VideoTrack? = null
  private var remoteVideoTrack: VideoTrack? = null

  private var localRenderer: SurfaceViewRenderer? = null
  private var remoteRenderer: SurfaceViewRenderer? = null

  private val addedCandidateKeys = mutableSetOf<String>()
  private var remoteDescriptionSet = false
  private val pendingRemoteCandidates = mutableListOf<IceCandidate>()

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private var isStarted = false

  fun start(enableVideo: Boolean, createOffer: Boolean) {
    if (isStarted) return
    isStarted = true

    try {
      audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
      audioManager?.isSpeakerphoneOn = enableVideo
      audioManager?.isMicrophoneMute = false

      PeerConnectionFactory.initialize(
        PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
          .setEnableInternalTracer(false)
          .createInitializationOptions()
      )

      val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
      val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

      factory = PeerConnectionFactory.builder()
        .setVideoEncoderFactory(encoderFactory)
        .setVideoDecoderFactory(decoderFactory)
        .createPeerConnectionFactory()

      val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
      )

      val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
        sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
      }

      peerConnection = factory?.createPeerConnection(
        rtcConfig,
        object : PeerConnection.Observer {
          override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
          override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
          override fun onIceConnectionReceivingChange(receiving: Boolean) {}
          override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
          override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let {
              onLocalIceCandidate(it.sdpMid ?: "0", it.sdpMLineIndex, it.sdp)
            }
          }
          override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
          override fun onAddStream(stream: MediaStream?) {
            val video = stream?.videoTracks?.firstOrNull()
            if (video != null) {
              remoteVideoTrack = video
              remoteRenderer?.let { video.addSink(it) }
            }
          }
          override fun onRemoveStream(stream: MediaStream?) {}
          override fun onDataChannel(channel: DataChannel?) {}
          override fun onRenegotiationNeeded() {}
          override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
            val track = receiver?.track()
            if (track is VideoTrack) {
              remoteVideoTrack = track
              remoteRenderer?.let { track.addSink(it) }
            }
          }
        }
      )

      // Audio track
      localAudioSource = factory?.createAudioSource(MediaConstraints())
      localAudioTrack = factory?.createAudioTrack("ARDAMSa0", localAudioSource)
      localAudioTrack?.setEnabled(true)
      localAudioTrack?.let { peerConnection?.addTrack(it, listOf("ARDAMS")) }

      // Video track
      if (enableVideo) {
        startCameraCapture()
      }

      if (createOffer) {
        val constraints = MediaConstraints().apply {
          mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
          mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", enableVideo.toString()))
        }
        peerConnection?.createOffer(
          object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
              sdp ?: return
              peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
              onLocalOfferCreated(sdp.description)
            }
          },
          constraints,
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize WebRTC: ${e.message}", e)
    }
  }

  private fun startCameraCapture() {
    try {
      val enumerator = Camera2Enumerator(context)
      val frontCam = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        ?: enumerator.deviceNames.firstOrNull()
        ?: return
      videoCapturer = enumerator.createCapturer(frontCam, null) ?: return
      surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
      localVideoSource = factory?.createVideoSource(videoCapturer!!.isScreencast)
      videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
      videoCapturer?.startCapture(640, 480, 24)

      localVideoTrack = factory?.createVideoTrack("ARDAMSv0", localVideoSource)
      localVideoTrack?.setEnabled(true)
      localVideoTrack?.let { track ->
        localRenderer?.let { track.addSink(it) }
        peerConnection?.addTrack(track, listOf("ARDAMS"))
      }
    } catch (e: Exception) {
      Log.w(TAG, "Camera capture unavailable: ${e.message}")
    }
  }

  fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
    localRenderer = renderer
    localVideoTrack?.addSink(renderer)
  }

  fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
    remoteRenderer = renderer
    remoteVideoTrack?.addSink(renderer)
  }

  fun handleRemoteOfferAndCreateAnswer(offerSdp: String, enableVideo: Boolean) {
    val pc = peerConnection ?: return
    if (offerSdp.isBlank() || remoteDescriptionSet) return
    val sdp = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
    pc.setRemoteDescription(
      object : SimpleSdpObserver() {
        override fun onSetSuccess() {
          remoteDescriptionSet = true
          flushPendingCandidates()
          val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", enableVideo.toString()))
          }
          pc.createAnswer(
            object : SimpleSdpObserver() {
              override fun onCreateSuccess(answer: SessionDescription?) {
                answer ?: return
                pc.setLocalDescription(SimpleSdpObserver(), answer)
                onLocalAnswerCreated(answer.description)
              }
            },
            constraints,
          )
        }
      },
      sdp,
    )
  }

  fun handleRemoteAnswer(answerSdp: String) {
    val pc = peerConnection ?: return
    if (answerSdp.isBlank() || remoteDescriptionSet) return
    val sdp = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
    pc.setRemoteDescription(
      object : SimpleSdpObserver() {
        override fun onSetSuccess() {
          remoteDescriptionSet = true
          flushPendingCandidates()
        }
      },
      sdp,
    )
  }

  fun addRemoteIceCandidates(records: List<IceCandidateRecord>, myUid: String) {
    for (rec in records) {
      if (rec.senderId == myUid || rec.candidate.isBlank()) continue
      val key = "${rec.sdpMid}_${rec.sdpMLineIndex}_${rec.candidate}"
      if (!addedCandidateKeys.add(key)) continue
      val ice = IceCandidate(rec.sdpMid, rec.sdpMLineIndex, rec.candidate)
      if (remoteDescriptionSet) {
        peerConnection?.addIceCandidate(ice)
      } else {
        pendingRemoteCandidates.add(ice)
      }
    }
  }

  private fun flushPendingCandidates() {
    val pc = peerConnection ?: return
    for (ice in pendingRemoteCandidates) {
      pc.addIceCandidate(ice)
    }
    pendingRemoteCandidates.clear()
  }

  fun setMuted(muted: Boolean) {
    localAudioTrack?.setEnabled(!muted)
    audioManager?.isMicrophoneMute = muted
  }

  fun setSpeakerEnabled(enabled: Boolean) {
    audioManager?.isSpeakerphoneOn = enabled
  }

  fun setCameraEnabled(enabled: Boolean) {
    if (enabled && localVideoTrack == null) {
      startCameraCapture()
    }
    localVideoTrack?.setEnabled(enabled)
  }

  fun switchCamera() {
    try {
      videoCapturer?.switchCamera(null)
    } catch (e: Exception) {
      Log.w(TAG, "Switch camera failed: ${e.message}")
    }
  }

  fun release() {
    if (!isStarted) return
    isStarted = false
    try {
      localRenderer?.let { localVideoTrack?.removeSink(it) }
      remoteRenderer?.let { remoteVideoTrack?.removeSink(it) }
      videoCapturer?.stopCapture()
      videoCapturer?.dispose()
      surfaceTextureHelper?.dispose()
      localVideoSource?.dispose()
      localAudioSource?.dispose()
      peerConnection?.close()
      peerConnection?.dispose()
      factory?.dispose()
      eglBase.release()
      audioManager?.mode = AudioManager.MODE_NORMAL
      audioManager?.isMicrophoneMute = false
      audioManager?.isSpeakerphoneOn = false
    } catch (e: Exception) {
      Log.w(TAG, "WebRTC release warning: ${e.message}")
    }
  }

  private open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
  }
}
