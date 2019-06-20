package com.novoda.noplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.novoda.noplayer.drm.DrmType;
import com.novoda.noplayer.drm.KeyRequestExecutor;
import com.novoda.noplayer.drm.ModularDrmKeyRequest;
import com.novoda.noplayer.internal.drm.provision.ProvisionExecutorCreator;
import com.novoda.noplayer.internal.exoplayer.NoPlayerExoPlayerCreator;
import com.novoda.noplayer.internal.exoplayer.drm.DrmSessionCreatorFactory;
import com.novoda.noplayer.internal.mediaplayer.NoPlayerMediaPlayerCreator;
import com.novoda.noplayer.internal.utils.AndroidDeviceVersion;
import com.novoda.noplayer.model.KeySetId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Builds instances of {@link NoPlayer} for given configurations.
 */
public class PlayerBuilder {

    private DrmType drmType = DrmType.NONE;
    private KeyRequestExecutor keyRequestExecutor = KeyRequestExecutor.NOT_REQUIRED;
    @Nullable
    private KeySetId keySetId;
    private List<PlayerType> prioritizedPlayerTypes = Arrays.asList(PlayerType.EXO_PLAYER, PlayerType.MEDIA_PLAYER);
    private boolean allowFallbackDecoder; /* initialised to false by default */
    private boolean allowCrossProtocolRedirects; /* initialised to false by default */
    private String userAgent = "user-agent";
    private AdvertsLoader advertsLoader;

    /**
     * Sets {@link PlayerBuilder} to build a {@link NoPlayer} which will play adverts provided by the passed in loader
     *
     * @param advertsLoader The loader used by NoPlayer to fetch what adverts to play.
     * @return {@link PlayerBuilder}
     * @see NoPlayer
     */
    public PlayerBuilder withAdverts(AdvertsLoader advertsLoader) {
        this.advertsLoader = advertsLoader;
        return this;
    }

    /**
     * Sets {@link PlayerBuilder} to build a {@link NoPlayer} which supports Widevine classic DRM.
     *
     * @return {@link PlayerBuilder}
     * @see NoPlayer
     */
    public PlayerBuilder withWidevineClassicDrm() {
        return withDrm(DrmType.WIDEVINE_CLASSIC, KeyRequestExecutor.NOT_REQUIRED, null);
    }

    /**
     * Sets {@link PlayerBuilder} to build a {@link NoPlayer} which supports Widevine modular streaming DRM.
     *
     * @param keyRequestExecutor Implementation of {@link KeyRequestExecutor}.
     * @return {@link PlayerBuilder}
     * @see NoPlayer
     */
    public PlayerBuilder withWidevineModularStreamingDrm(KeyRequestExecutor keyRequestExecutor) {
        return withDrm(DrmType.WIDEVINE_MODULAR_STREAM, keyRequestExecutor, null);
    }

    /**
     * Sets {@link PlayerBuilder} to build a {@link NoPlayer} which supports Widevine modular download DRM.
     *
     * @param keySetId The KeySetId to restore.
     * @return {@link PlayerBuilder}
     * @see NoPlayer
     */
    public PlayerBuilder withWidevineModularDownloadDrm(final KeySetId keySetId) {
        KeyRequestExecutor keyRequestExecutor = new KeyRequestExecutor() {
            @Override
            public byte[] executeKeyRequest(ModularDrmKeyRequest request) throws DrmRequestException {
                return keySetId.asBytes();
            }
        };

        return withDrm(DrmType.WIDEVINE_MODULAR_DOWNLOAD, keyRequestExecutor, keySetId);
    }

    /**
     * Sets {@link PlayerBuilder} to build a {@link NoPlayer} which supports the specified parameters.
     *
     * @param drmType            {@link DrmType}
     * @param keyRequestExecutor {@link KeyRequestExecutor}
     * @return {@link PlayerBuilder}
     * @see NoPlayer
     */
    public PlayerBuilder withDrm(DrmType drmType, KeyRequestExecutor keyRequestExecutor, KeySetId keySetId) {
        this.drmType = drmType;
        this.keyRequestExecutor = keyRequestExecutor;
        this.keySetId = keySetId;
        return this;
    }

    /**
     * Sets {@link PlayerBuilder} to build a {@link NoPlayer} which will prioritise the underlying player when
     * multiple underlying players share the same features.
     *
     * @param playerType  First {@link PlayerType} with the highest priority.
     * @param playerTypes Remaining {@link PlayerType} in order of priority.
     * @return {@link PlayerBuilder}
     * @see NoPlayer
     */
    public PlayerBuilder withPriority(PlayerType playerType, PlayerType... playerTypes) {
        List<PlayerType> types = new ArrayList<>();
        types.add(playerType);
        types.addAll(Arrays.asList(playerTypes));
        prioritizedPlayerTypes = types;
        return this;
    }

    /**
     * Will fallback to using a non-secure decoder when the device does not support a secure decoder.
     *
     * @return {@link PlayerBuilder}
     */
    public PlayerBuilder allowFallbackDecoders() {
        allowFallbackDecoder = true;
        return this;
    }

    /**
     * @param userAgent The application's user-agent value
     * @return {@link PlayerBuilder}
     */
    public PlayerBuilder withUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * Network connections will be allowed to perform redirects between HTTP and HTTPS protocols
     *
     * @return {@link PlayerBuilder}
     */
    public PlayerBuilder allowCrossProtocolRedirects() {
        allowCrossProtocolRedirects = true;
        return this;
    }

    /**
     * Builds a new {@link NoPlayer} instance.
     *
     * @param context The {@link Context} associated with the player.
     * @return a {@link NoPlayer} instance.
     * @throws UnableToCreatePlayerException thrown when the configuration is not supported and there is no way to recover.
     * @see NoPlayer
     */
    public NoPlayer build(Context context) throws UnableToCreatePlayerException {
        Context applicationContext = context.getApplicationContext();
        Handler handler = new Handler(Looper.getMainLooper());
        ProvisionExecutorCreator provisionExecutorCreator = new ProvisionExecutorCreator();
        DrmSessionCreatorFactory drmSessionCreatorFactory = new DrmSessionCreatorFactory(
                AndroidDeviceVersion.newInstance(),
                provisionExecutorCreator,
                handler
        );

        NoPlayerExoPlayerCreator noPlayerExoPlayerCreator = createExoPlayerCreator(handler);

        NoPlayerCreator noPlayerCreator = new NoPlayerCreator(
                applicationContext,
                prioritizedPlayerTypes,
                noPlayerExoPlayerCreator,
                NoPlayerMediaPlayerCreator.newInstance(handler),
                drmSessionCreatorFactory
        );
        return noPlayerCreator.create(drmType, keyRequestExecutor, keySetId, allowFallbackDecoder, allowCrossProtocolRedirects);
    }

    private NoPlayerExoPlayerCreator createExoPlayerCreator(Handler handler) {
        if (advertsLoader == null) {
            return NoPlayerExoPlayerCreator.newInstance(userAgent, handler);
        } else {
            return NoPlayerExoPlayerCreator.newInstance(userAgent, handler, advertsLoader);
        }
    }

}
