package com.novoda.noplayer.internal.exoplayer.forwarder;

import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.drm.DefaultDrmSessionEventListener;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.video.VideoListener;
import com.novoda.noplayer.NoPlayer;
import com.novoda.noplayer.PlayerState;
import com.novoda.noplayer.internal.utils.Optional;

public class ExoPlayerForwarder {

    private final EventListener exoPlayerEventListener;
    private final NoPlayerMediaSourceEventListener mediaSourceEventListener;
    private final NoPlayerAnalyticsListener analyticsListener;
    private final ExoPlayerVideoListener videoListener;
    private final ExoPlayerDrmSessionEventListener drmSessionEventListener;
    private Optional<NoPlayer.AdvertListener> advertListeners = Optional.absent();

    public ExoPlayerForwarder() {
        exoPlayerEventListener = new EventListener();
        mediaSourceEventListener = new NoPlayerMediaSourceEventListener();
        videoListener = new ExoPlayerVideoListener();
        analyticsListener = new NoPlayerAnalyticsListener();
        drmSessionEventListener = new ExoPlayerDrmSessionEventListener();
    }

    public EventListener exoPlayerEventListener() {
        return exoPlayerEventListener;
    }

    public MediaSourceEventListener mediaSourceEventListener() {
        return mediaSourceEventListener;
    }

    public VideoListener videoListener() {
        return videoListener;
    }

    public DefaultDrmSessionEventListener drmSessionEventListener() {
        return drmSessionEventListener;
    }

    public AnalyticsListener analyticsListener() {
        return analyticsListener;
    }

    public Optional<NoPlayer.AdvertListener> advertListener() {
        return advertListeners;
    }

    public void bind(NoPlayer.PreparedListener preparedListener, PlayerState playerState) {
        exoPlayerEventListener.add(new OnPrepareForwarder(preparedListener, playerState));
    }

    public void bind(NoPlayer.CompletionListener completionListener, NoPlayer.StateChangedListener stateChangedListener) {
        exoPlayerEventListener.add(new OnCompletionForwarder(completionListener));
        exoPlayerEventListener.add(new OnCompletionStateChangedForwarder(stateChangedListener));
    }

    public void bind(NoPlayer.ErrorListener errorListener, NoPlayer.ErrorListener internalErrorListener) {
        exoPlayerEventListener.add(new PlayerOnErrorForwarder(errorListener));
        exoPlayerEventListener.add(new PlayerOnErrorForwarder(internalErrorListener));
    }

    public void bind(NoPlayer.BufferStateListener bufferStateListener) {
        exoPlayerEventListener.add(new BufferStateForwarder(bufferStateListener));
    }

    public void bind(NoPlayer.VideoSizeChangedListener videoSizeChangedListener) {
        videoListener.add(new VideoSizeChangedForwarder(videoSizeChangedListener));
    }

    public void bind(NoPlayer.BitrateChangedListener bitrateChangedListener) {
        mediaSourceEventListener.add(new BitrateForwarder(bitrateChangedListener));
    }

    public void bind(NoPlayer.InfoListener infoListeners) {
        exoPlayerEventListener.add(new EventInfoForwarder(infoListeners));
        mediaSourceEventListener.add(new MediaSourceEventForwarder(infoListeners));
        drmSessionEventListener.add(new DrmSessionInfoForwarder(infoListeners));
        analyticsListener.add(new AnalyticsListenerForwarder(infoListeners));
    }

    public void bind(NoPlayer.DroppedVideoFramesListener droppedVideoFramesListeners) {
        analyticsListener.add(droppedVideoFramesListeners);
    }

    public void bind(NoPlayer.AdvertListener advertListeners) {
        this.advertListeners = Optional.of(advertListeners);
    }

    public void bind(NoPlayer.TracksChangedListener tracksChangedListeners) {
        exoPlayerEventListener.add(new TracksChangedForwarder(tracksChangedListeners));
    }
}
