/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.incallui;

import android.telecom.CameraCapabilities;
import android.telecom.Connection;
import android.telecom.Connection.VideoProvider;
import android.telecom.InCallService.VideoCall;
import android.telecom.VideoProfile;

/**
 * Implements the InCallUI Video Call Listener.
 */
public class InCallVideoCallListener extends VideoCall.Listener {

    /**
     * The call associated with this {@link InCallVideoClient}.
     */
    private Call mCall;

    /**
     * Creates an instance of the call video client, specifying the call it is related to.
     *
     * @param call The call.
     */
    public InCallVideoCallListener(Call call) {
        mCall = call;
    }

    /**
     * Handles an incoming session modification request.
     *
     * @param videoProfile The requested video call profile.
     */
    @Override
    public void onSessionModifyRequestReceived(VideoProfile videoProfile) {
        Log.d(this, " onSessionModifyRequestReceived videoProfile=" + videoProfile);
        int previousVideoState = CallUtils.toUnPausedVideoState(mCall.getVideoState());
        int newVideoState = CallUtils.toUnPausedVideoState(videoProfile.getVideoState());

        boolean wasVideoCall = VideoProfile.VideoState.isVideo(previousVideoState);
        boolean isVideoCall = VideoProfile.VideoState.isVideo(newVideoState);

        // Check for upgrades to video and downgrades to audio.
        if (wasVideoCall && !isVideoCall) {
            InCallVideoCallListenerNotifier.getInstance().downgradeToAudio(mCall);
        } else if (previousVideoState != newVideoState) {
            InCallVideoCallListenerNotifier.getInstance().upgradeToVideoRequest(mCall);
        }
    }

    /**
     * Handles a session modification response.
     *
     * @param status Status of the session modify request. Valid values are
     *            {@link Connection.VideoProvider#SESSION_MODIFY_REQUEST_SUCCESS},
     *            {@link Connection.VideoProvider#SESSION_MODIFY_REQUEST_FAIL},
     *            {@link Connection.VideoProvider#SESSION_MODIFY_REQUEST_INVALID}
     * @param requestedProfile
     * @param responseProfile The actual profile changes made by the peer device.
     */
    @Override
    public void onSessionModifyResponseReceived(int status, VideoProfile requestedProfile,
            VideoProfile responseProfile) {
        Log.d(this, "onSessionModifyResponseReceived status=" + status + " requestedProfile="
                + requestedProfile + " responseProfile=" + responseProfile);
        if (status != VideoProvider.SESSION_MODIFY_REQUEST_SUCCESS) {
            InCallVideoCallListenerNotifier.getInstance().upgradeToVideoFail(status, mCall);
        } else if (requestedProfile != null && responseProfile != null) {
            boolean modifySucceeded = requestedProfile.getVideoState() ==
                    responseProfile.getVideoState();
            boolean isVideoCall = VideoProfile.VideoState.isVideo(responseProfile.getVideoState());
            if (modifySucceeded && isVideoCall) {
                InCallVideoCallListenerNotifier.getInstance().upgradeToVideoSuccess(mCall);
            } else if (!modifySucceeded) {
                InCallVideoCallListenerNotifier.getInstance().upgradeToVideoFail(status, mCall);
            }
        } else {
            Log.d(this, "onSessionModifyResponseReceived request and response Profiles are null");
        }
    }

    /**
     * Handles a call session event.
     *
     * @param event The event.
     */
    @Override
    public void onCallSessionEvent(int event) {
        InCallVideoCallListenerNotifier.getInstance().callSessionEvent(event);
    }

    /**
     * Handles a change to the peer video dimensions.
     *
     * @param width  The updated peer video width.
     * @param height The updated peer video height.
     */
    @Override
    public void onPeerDimensionsChanged(int width, int height) {
        InCallVideoCallListenerNotifier.getInstance().peerDimensionsChanged(mCall, width, height);
    }

    /**
     * Handles a change to the video quality of the call.
     *
     * @param videoQuality The updated video call quality.
     */
    @Override
    public void onVideoQualityChanged(int videoQuality) {
        InCallVideoCallListenerNotifier.getInstance().videoQualityChanged(mCall, videoQuality);
    }

    /**
     * Handles a change to the call data usage.  No implementation as the in-call UI does not
     * display data usage.
     *
     * @param dataUsage The updated data usage.
     */
    @Override
    public void onCallDataUsageChanged(long dataUsage) {
        Log.d(this, "onCallDataUsageChanged: dataUsage = " + dataUsage);
        InCallVideoCallListenerNotifier.getInstance().callDataUsageChanged(dataUsage);
    }

    /**
     * Handles changes to the camera capabilities.  No implementation as the in-call UI does not
     * make use of camera capabilities.
     *
     * @param cameraCapabilities The changed camera capabilities.
     */
    @Override
    public void onCameraCapabilitiesChanged(CameraCapabilities cameraCapabilities) {
        InCallVideoCallListenerNotifier.getInstance().cameraDimensionsChanged(
                mCall, cameraCapabilities.getWidth(), cameraCapabilities.getHeight());
    }
}
