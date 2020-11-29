package com.example.chat.CallUtil

import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.chat.CallDialogFragment
import com.example.chat.MainActivity
import com.example.chat.MainActivity.Companion.activity
import com.example.chat.MainActivity.Companion.ring
import com.example.chat.MainActivity.Companion.runOnUiThread
import com.example.chat.MyApplication
import com.example.chat.R
import com.example.chat.UserUtil.MyUser
import com.example.chat.ui.BaseFragment
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer

class CallFragment(var username:String, var offer:String?=null, var random: Boolean = false) : BaseFragment() {

    val sdpConstraints = MediaConstraints()

    var localPeer:PeerConnection?=null
    var videoView_their: SurfaceViewRenderer?=null
    var videoView_my: SurfaceViewRenderer?=null
    var peerConnectionFactory :PeerConnectionFactory?=null
    var rootEglBase: EglBase?=null
    var videoCapturerAndroid: VideoCapturer?=null
    var stream: MediaStream?=null
    var localVideoTrack: VideoTrack?=null
    var videoTrack_their: VideoTrack?=null
    var localAudioTrack: AudioTrack?=null
    lateinit var rot_View: View

    var video_item: MenuItem?=null

    override fun onBackPressed() {
        super.onBackPressed()
        hangup()
        MyUser.send_webSocket_(
            JSONObject().put(
                "call_close", JSONObject().put("username", username)
            ).toString()
        )
        MainActivity.activity?.lockScreen()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        callFragment =this
        setHasOptionsMenu(true)
        rot_View = inflater.inflate(R.layout.fragment_call, container, false)
        start()
        return rot_View
    }

    private fun start()
    {
        rootEglBase = EglBase.create()
        videoView_their = SurfaceViewRenderer(requireContext())
        var lpView =
            ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        videoView_their?.layoutParams = lpView
        (rot_View as ConstraintLayout).addView(videoView_their)
        videoView_their?.init(rootEglBase!!.eglBaseContext, null)
        videoView_their?.visibility=View.GONE

        videoView_my = SurfaceViewRenderer(requireContext())
        videoView_my?.setZOrderMediaOverlay(true)
        videoView_my?.setMirror(true)
        videoView_my?.visibility=View.GONE
        lpView =
            ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        videoView_my?.layoutParams = lpView
        rot_View.findViewById<ConstraintLayout>(R.id.cl_sv_my).addView(videoView_my)
        videoView_my?.init(rootEglBase!!.eglBaseContext, null)

        Thread {
            //create sdpConstraints
            sdpConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(
                    "offerToReceiveAudio",
                    "true"
                )
            )
            sdpConstraints.mandatory.add(
                MediaConstraints.KeyValuePair(
                    "offerToReceiveVideo",
                    "true"
                )
            )
            create_peerConnectionFactory()
            create_audio()
            call()
        }.start()
    }

    private fun create_peerConnectionFactory() {
        val initializationOptions =
            PeerConnectionFactory.InitializationOptions.builder(activity)
                .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        val options: PeerConnectionFactory.Options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory =
            DefaultVideoEncoderFactory(
                rootEglBase!!.eglBaseContext,  /* enableIntelVp8Encoder */
                true,  /* enableH264HighProfile */
                true
            )
        val defaultVideoDecoderFactory =
            DefaultVideoDecoderFactory(rootEglBase!!.eglBaseContext)
        peerConnectionFactory?.dispose()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }


    private fun call() {
        create_local_peer()
        if(localPeer!=null) {
            if (offer != null) {
                set_offer_create_answer(offer)
            } else {
                //creating Offer
                create_offer()
            }
        }
    }

    private fun set_offer_create_answer(offer: String?) {
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offer)
        localPeer!!.setRemoteDescription(
            CustomSdpObserver("remoteSetRemoteDesc"),
            sessionDescription
        )

        localPeer!!.createAnswer(object : CustomSdpObserver("remoteCreateOffer") {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                //remote answer generated. Now set it as local desc for remote peer and remote desc for local peer.
                super.onCreateSuccess(sessionDescription)
                localPeer!!.setLocalDescription(
                    CustomSdpObserver("remoteSetLocalDesc"),
                    sessionDescription
                )
                MyUser.send_webSocket_(
                    JSONObject().put(
                        "answer",
                        JSONObject()
                            .put("answer", sessionDescription.description)
                            .put("username", username)
                    ).toString()
                )
            }
        }, sdpConstraints)
    }

    fun gotRemoteStream(stream: MediaStream) {
        if(stream.audioTracks.isNotEmpty()) {
            val audioTrack: AudioTrack = stream.audioTracks[0]
            audioTrack.setEnabled(true)
        }
        //we have remote video stream. add to the renderer.
        if(stream.videoTracks.isNotEmpty()) {
            videoTrack_their = stream.videoTracks[0]
            runOnUiThread(Runnable {
                try {
                    videoView_their?.visibility = View.VISIBLE
                    videoTrack_their?.addSink(videoView_their)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        } else {
            runOnUiThread(Runnable {
                try {
                    videoView_their?.visibility = View.GONE
                    videoTrack_their?.removeSink(videoView_their)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        return createCameraCapturer(Camera1Enumerator(false))
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // Trying to find a front facing camera!
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? =
                    enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // We were not able to find a front cam. Look for other cameras
        for (deviceName in deviceNames) {
            if (!enumerator.isBackFacing(deviceName)) {
                val videoCapturer: VideoCapturer? =
                    enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    override fun onCreateOptionsMenu (menu: Menu,
                                      inflater: MenuInflater
    ) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_call, menu)
        video_item = menu.findItem(R.id.video_item)
    }

    var is_video=false

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.close_item -> {
                callFragment!!.onBackPressed()
                requireActivity().supportFragmentManager.popBackStack()
                false
            }
            R.id.video_item -> {
                //Now create a VideoCapturer instance. Callback methods are there if you want to do something! Duh!
                if(!is_video) {
                    video_item?.setIcon(R.drawable.ic_videocam_red_24dp)
                    create_video()
                    videoView_my?.visibility=View.VISIBLE
                } else {
                    video_item?.setIcon(R.drawable.ic_videocam_black_24dp)
                    localVideoTrack?.removeSink(videoView_my)
                    videoView_my?.visibility=View.GONE
                    videoCapturerAndroid?.stopCapture()
                    stream?.removeTrack(localVideoTrack)
                }
                create_local_peer()
                create_offer()
                is_video=!is_video
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun create_video() {
        if (videoCapturerAndroid == null) {
            videoCapturerAndroid = createVideoCapturer()
            if (videoCapturerAndroid != null) {
                set_localVideoTrack(videoCapturerAndroid!!)
            }
        }

        if (videoCapturerAndroid != null) {
            //we will start capturing the video from the camera
            //params are width,height and fps
            videoCapturerAndroid?.startCapture(256, 256, 10)
            localVideoTrack?.addSink(videoView_my)
            stream?.addTrack(localVideoTrack)
        }
    }

    private fun create_audio() {
        if (peerConnectionFactory != null) {
            val constraints = MediaConstraints()
            //create an AudioSource instance
            val audioSource: AudioSource =
                peerConnectionFactory!!.createAudioSource(constraints)
            localAudioTrack =
                peerConnectionFactory!!.createAudioTrack("101", audioSource)
            stream = peerConnectionFactory!!.createLocalMediaStream("101")
            stream?.addTrack(localAudioTrack)
        }
    }

    private fun set_localVideoTrack(videoCapturerAndroid: VideoCapturer) {
        //Create a VideoSource instance
        val surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)
        val videoSource =
            peerConnectionFactory?.createVideoSource(videoCapturerAndroid.isScreencast)
        videoCapturerAndroid.initialize(
            surfaceTextureHelper,
            activity,
            videoSource?.capturerObserver
        )
        localVideoTrack = peerConnectionFactory!!.createVideoTrack("100", videoSource)
    }

    private fun create_local_peer() {
        localPeer?.close()
        localPeer = peerConnectionFactory?.createPeerConnection(
            object : ArrayList<IceServer>(){
                init {
                    add(IceServer("turn:dimanys222.ddns.net:3478", "dima", "84962907"))
                }
            },
            object : CustomPeerConnectionObserver("remotePeerCreation") {
                override fun onIceCandidate(iceCandidate: IceCandidate) {
                    super.onIceCandidate(iceCandidate)
                    MyUser.send_webSocket_(
                        JSONObject().put(
                            "iceCandidate",
                            JSONObject()
                                .put(
                                    "iceCandidate",
                                    JSONObject()
                                        .put("sdp", iceCandidate.sdp)
                                        .put("sdpMid", iceCandidate.sdpMid)
                                        .put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
                                )
                                .put("username", username)
                        ).toString()
                    )
                }

                override fun onAddStream(mediaStream: MediaStream) {
                    super.onAddStream(mediaStream)
                    gotRemoteStream(mediaStream)
                }

                override fun onAddTrack(
                    rtpReceiver: RtpReceiver,
                    mediaStreams: Array<MediaStream>
                ) {
                    super.onAddTrack(rtpReceiver,mediaStreams)
                }
            })
        localPeer?.addStream(stream)
    }

    private fun create_offer() {
        localPeer!!.createOffer(object : CustomSdpObserver("localCreateOffer") {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                //we have localOffer. Set it as local desc for localpeer and remote desc for remote peer.
                //try to create answer from the remote peer.
                super.onCreateSuccess(sessionDescription)
                localPeer!!.setLocalDescription(
                    CustomSdpObserver("localSetLocalDesc"),
                    sessionDescription
                )
                MyUser.send_webSocket_(
                    JSONObject().put(
                        "offer",
                        JSONObject()
                            .put("offer", sessionDescription.description)
                            .put("username", username)
                            .put("random", random)
                    ).toString()
                )
            }
        }, sdpConstraints)
    }

    fun hangup() {
        localVideoTrack?.removeSink(videoView_my)
        videoTrack_their?.removeSink(videoView_their)
        videoView_my?.release()
        videoView_their?.release()

        videoCapturerAndroid?.stopCapture()
        videoCapturerAndroid?.dispose()
        videoCapturerAndroid=null

        localPeer?.dispose()
        localPeer = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory=null
    }

    companion object {
        var callFragment: CallFragment? = null
        var dialog: CallDialogFragment? = null

        fun newInstance(username:String,random: Boolean = false, offert:String?=null) =
            CallFragment(username, offert, random)

        fun offer_send(it: JSONObject) {
            val offer = it.getString("offer")
            val username = it.getString("username")
            val random = it.getBoolean("random")
            if(callFragment?.localPeer!=null){
                callFragment?.removeSink()
                callFragment?.create_local_peer()
                callFragment?.set_offer_create_answer(offer)
            } else {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                ring = RingtoneManager.getRingtone(MyApplication.appContext, notification);
                ring?.play()
                if (activity != null && activity!!.is_visible) {
                    dialog =
                        CallDialogFragment(
                            username,
                            offer,
                            random
                        )
                    dialog?.show(activity!!.supportFragmentManager, "123")
                } else {
                    MainActivity.wake_lock_create()

                    val intent = Intent(MyApplication.appContext, MainActivity::class.java)
                    intent.putExtra("id", "call")
                    intent.putExtra("username", username)
                    intent.putExtra("offer", offer)
                    intent.putExtra("random", random)
                    MyApplication.appContext.startActivity(intent)
                }
            }
        }

        fun answer_send(it: JSONObject) {
            val answer = it.getString("answer")
            callFragment?.set_answer(answer)
        }

        fun iceCandidate_send(it: JSONObject) {
            val sdpMid = it.getString("sdpMid")
            val sdpMLineIndex = it.getInt("sdpMLineIndex")
            val sdp = it.getString("sdp")
            val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
            callFragment?.set_iceCandidate(iceCandidate)
        }

        fun call_close_send(it: JSONObject? = null) {
            runOnUiThread(Runnable {
                if(callFragment !=null && callFragment!!.isVisible) {
                    callFragment!!.onBackPressed()
                    activity?.supportFragmentManager?.popBackStack()
                } else {
                    ring?.stop()
                    dialog?.dismiss()
                    activity?.lockScreen()
                    MainActivity.wake_lock_destroy()
                }
            })
        }
    }

    private fun removeSink() {
        runOnUiThread(Runnable{
            videoTrack_their?.removeSink(videoView_their)
        })
    }

    private fun set_iceCandidate(iceCandidate: IceCandidate) {
        localPeer?.addIceCandidate(iceCandidate)
    }

    private fun set_answer(answer: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, answer)
        localPeer!!.setRemoteDescription(
            CustomSdpObserver("localSetRemoteDesc"),
            sessionDescription
        )
    }
}
