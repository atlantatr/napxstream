package com.napxstream.ui.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Rational
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.napxstream.R
import com.napxstream.XtreamApp
import com.napxstream.data.local.WatchProgressEntity
import com.napxstream.databinding.ActivityPlayerBinding
import com.napxstream.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    private lateinit var contentId: String
    private lateinit var contentType: String
    private var title: String = ""
    private var streamUrl: String = ""
    private var parentId: String? = null
    private var posterUrl: String? = null

    // Dizilerde "sıradaki bölüm" otomatik oynatma
    private var nextTitle: String? = null
    private var nextStreamUrl: String? = null
    private var nextContentId: String? = null
    private var autoplayCountdown: CountDownTimer? = null
    private var hasShownEndCard = false

    private var castContext: CastContext? = null
    private var castSession: CastSession? = null
    private var isCasting = false

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = onCastConnected(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = onCastConnected(session)
        override fun onSessionEnded(session: CastSession, error: Int) = onCastDisconnected()
        override fun onSessionSuspended(session: CastSession, reason: Int) = onCastDisconnected()
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        streamUrl = intent.getStringExtra(Constants.EXTRA_STREAM_URL) ?: run { finish(); return }
        title = intent.getStringExtra(Constants.EXTRA_TITLE) ?: ""
        contentType = intent.getStringExtra(Constants.EXTRA_CONTENT_TYPE) ?: Constants.CONTENT_TYPE_LIVE
        val streamId = intent.getIntExtra(Constants.EXTRA_STREAM_ID, -1)
        contentId = intent.getStringExtra(Constants.EXTRA_CONTENT_ID) ?: streamId.toString()
        parentId = intent.getStringExtra(Constants.EXTRA_PARENT_ID)
        posterUrl = intent.getStringExtra(Constants.EXTRA_POSTER_URL)
        nextTitle = intent.getStringExtra(Constants.EXTRA_NEXT_TITLE)
        nextStreamUrl = intent.getStringExtra(Constants.EXTRA_NEXT_STREAM_URL)
        nextContentId = intent.getStringExtra(Constants.EXTRA_NEXT_CONTENT_ID)

        initPlayer(streamUrl)
        setupCast()
    }

    // ==================== Picture-in-Picture ====================

    /** Kullanıcı Ana Ekran'a dönme/uygulamayı arkaya alma hareketi yaptığında (Home tuşu vb.) çağrılır. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && shouldOfferPip()) {
            enterPipMode()
        }
    }

    private fun shouldOfferPip(): Boolean {
        val exoPlayer = player ?: return false
        // Cast oturumu aktifken veya video oynamıyorken PIP'e gerek yok
        return !isCasting && exoPlayer.isPlaying
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val aspectRatio = Rational(16, 9)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        try {
            enterPictureInPictureMode(params)
        } catch (e: Exception) {
            Log.w("PlayerActivity", "PIP moduna geçilemedi: ${e.message}")
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // PIP'teyken oynatıcı kontrollerini/overlay'leri gizle, sadece video görünsün
        binding.playerView.useController = !isInPictureInPictureMode
        binding.mediaRouteButton.visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        if (isInPictureInPictureMode) {
            binding.nextEpisodeCard.root.visibility = View.GONE
        }
    }

    // ==================== Chromecast ====================

    /**
     * Chromecast kurulumu. Play Services yüklü olmayan cihazlarda (bazı özel Android TV
     * kutuları, Amazon Fire TV vb.) CastContext alınamayabilir; bu durumda sessizce
     * Cast düğmesi devre dışı kalır, uygulama normal (yerel) oynatmaya devam eder.
     */
    private fun setupCast() {
        try {
            castContext = CastContext.getSharedInstance(this)
            CastButtonFactory.setUpMediaRouteButton(applicationContext, binding.mediaRouteButton)
            castContext?.sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
            castContext?.sessionManager?.currentCastSession?.let { onCastConnected(it) }
        } catch (e: Exception) {
            Log.w("PlayerActivity", "Cast kullanılamıyor: ${e.message}")
            binding.mediaRouteButton.visibility = View.GONE
        }
    }

    private fun onCastConnected(session: CastSession) {
        castSession = session
        isCasting = true
        player?.pause()

        val mimeType = if (streamUrl.contains(".m3u8")) "application/x-mpegurl" else "video/mp4"

        val movieMetadata = CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(CastMediaMetadata.KEY_TITLE, title)
        }

        val mediaInfo = MediaInfo.Builder(streamUrl)
            .setStreamType(
                if (contentType == Constants.CONTENT_TYPE_LIVE) MediaInfo.STREAM_TYPE_LIVE
                else MediaInfo.STREAM_TYPE_BUFFERED
            )
            .setContentType(mimeType)
            .setMetadata(movieMetadata)
            .build()

        val loadRequest = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .setCurrentTime(player?.currentPosition ?: 0)
            .build()

        session.remoteMediaClient?.load(loadRequest)
    }

    private fun onCastDisconnected() {
        isCasting = false
        castSession = null
        player?.play()
    }

    // ==================== ExoPlayer ====================

    @OptIn(UnstableApi::class)
    private fun initPlayer(streamUrl: String) {
        val okHttpClient = OkHttpClient.Builder().build()
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player = exoPlayer
        binding.playerView.player = exoPlayer

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.text = "Oynatma hatası: ${error.errorCodeName}\nYayın adresi veya hesap bilgilerini kontrol edin."
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                binding.bufferingProgress.visibility =
                    if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE

                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded()
                }
            }
        })

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()
            )
            .build()

        exoPlayer.setMediaItem(mediaItem)

        // Film/dizi için kaldığı yerden devam et (arşiv/canlı için geçerli değil)
        if (contentType == Constants.CONTENT_TYPE_VOD || contentType == Constants.CONTENT_TYPE_SERIES) {
            CoroutineScope(Dispatchers.Main).launch {
                val app = application as XtreamApp
                val progress = app.repository.getProgress(contentId)
                exoPlayer.prepare()
                if (progress != null && progress.positionMs > 5000) {
                    exoPlayer.seekTo(progress.positionMs)
                }
                exoPlayer.playWhenReady = true
            }
        } else {
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // ==================== Sıradaki bölüm ====================

    private fun onPlaybackEnded() {
        if (hasShownEndCard) return
        if (contentType != Constants.CONTENT_TYPE_SERIES) return
        val nUrl = nextStreamUrl ?: return
        val nTitle = nextTitle ?: return
        hasShownEndCard = true

        binding.nextEpisodeCard.root.visibility = View.VISIBLE
        binding.nextEpisodeCard.nextEpisodeTitle.text = nTitle
        binding.nextEpisodeCard.playNowButton.setOnClickListener { playNextEpisode(nUrl, nTitle) }
        binding.nextEpisodeCard.cancelAutoplayButton.setOnClickListener {
            autoplayCountdown?.cancel()
            binding.nextEpisodeCard.root.visibility = View.GONE
        }

        binding.nextEpisodeCard.countdownText.text = getString(R.string.play_next_in, 8)
        autoplayCountdown = object : CountDownTimer(8000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                binding.nextEpisodeCard.countdownText.text = getString(R.string.play_next_in, secondsLeft)
            }

            override fun onFinish() {
                playNextEpisode(nUrl, nTitle)
            }
        }.start()
    }

    private fun playNextEpisode(url: String, epTitle: String) {
        autoplayCountdown?.cancel()
        saveProgressIfNeeded()
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(Constants.EXTRA_STREAM_URL, url)
            putExtra(Constants.EXTRA_TITLE, epTitle)
            putExtra(Constants.EXTRA_CONTENT_TYPE, Constants.CONTENT_TYPE_SERIES)
            putExtra(Constants.EXTRA_CONTENT_ID, nextContentId)
            putExtra(Constants.EXTRA_PARENT_ID, parentId)
            putExtra(Constants.EXTRA_POSTER_URL, posterUrl)
        }
        startActivity(intent)
        finish()
    }

    // ==================== Yaşam döngüsü ====================

    override fun onPause() {
        super.onPause()
        // PIP modundaysa oynatmaya devam etsin (kullanıcı küçük pencerede izliyor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) return
        saveProgressIfNeeded()
        player?.pause()
    }

    private fun saveProgressIfNeeded() {
        if (contentType != Constants.CONTENT_TYPE_VOD && contentType != Constants.CONTENT_TYPE_SERIES) return
        val exoPlayer = player ?: return
        val position = exoPlayer.currentPosition
        val duration = exoPlayer.duration.coerceAtLeast(0)
        if (position <= 0) return

        val app = application as XtreamApp
        CoroutineScope(Dispatchers.IO).launch {
            app.repository.saveProgress(
                WatchProgressEntity(
                    contentId = contentId,
                    type = contentType,
                    title = title,
                    positionMs = position,
                    durationMs = duration,
                    parentId = parentId,
                    posterUrl = posterUrl
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoplayCountdown?.cancel()
        saveProgressIfNeeded()
        castContext?.sessionManager?.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        player?.release()
        player = null
    }
}
