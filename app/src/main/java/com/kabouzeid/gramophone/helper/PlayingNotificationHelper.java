package com.kabouzeid.gramophone.helper;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.glide.palette.BitmapPaletteTranscoder;
import com.kabouzeid.gramophone.glide.palette.BitmapPaletteWrapper;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.service.MusicService;
import com.kabouzeid.gramophone.ui.activities.MainActivity;
import com.kabouzeid.gramophone.util.ColorUtil;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.PreferenceUtil;

public class PlayingNotificationHelper {

    public static final String TAG = PlayingNotificationHelper.class.getSimpleName();

    private final MusicService service;

    private final NotificationManager notificationManager;
    private Notification notification;
    private int notificationId = 1;

    private RemoteViews notificationLayout;
    private RemoteViews notificationLayoutBig;

    private Song currentSong;
    private boolean isPlaying;

    private boolean isDark;
    private boolean isColored;

    private Target<BitmapPaletteWrapper> target;

    public PlayingNotificationHelper(@NonNull final MusicService service) {
        this.service = service;
        notificationManager = (NotificationManager) service
                .getSystemService(Context.NOTIFICATION_SERVICE);

        int bigNotificationImageSize = service.getResources().getDimensionPixelSize(R.dimen.notification_big_image_size);
        target = new SimpleTarget<BitmapPaletteWrapper>(bigNotificationImageSize, bigNotificationImageSize) {
            @Override
            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                super.onLoadFailed(e, errorDrawable);
                setAlbumCover(null, Color.TRANSPARENT);
            }

            @Override
            public void onResourceReady(BitmapPaletteWrapper resource, GlideAnimation<? super BitmapPaletteWrapper> glideAnimation) {
                setAlbumCover(resource.getBitmap(), ColorUtil.getColor(resource.getPalette(), Color.TRANSPARENT));
            }
        };
    }

    public void updateNotification() {
        updateNotification(PreferenceUtil.getInstance(service).coloredNotification());
    }

    public void updateNotification(final boolean isColored) {
        Song song = service.getCurrentSong();
        if (song.id == -1) {
            service.stopForeground(true);
            return;
        }
        currentSong = song;
        this.isColored = isColored;
        this.isPlaying = service.isPlaying();

        notificationLayout = new RemoteViews(service.getPackageName(), R.layout.notification);
        notificationLayoutBig = new RemoteViews(service.getPackageName(), R.layout.notification_big);

        notification = new NotificationCompat.Builder(service)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(getOpenMusicControllerPendingIntent())
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContent(notificationLayout)
                .build();

        notification.bigContentView = notificationLayoutBig;

        setUpCollapsedLayout();
        setUpExpandedLayout();
        loadAlbumArt();
        setUpPlaybackActions();
        setUpExpandedPlaybackActions();

        service.startForeground(notificationId, notification);
    }

    private PendingIntent getOpenMusicControllerPendingIntent() {
        Intent intent = new Intent(service, MainActivity.class);
        return PendingIntent.getActivity(service, 0, intent, 0);
    }

    private void setUpExpandedPlaybackActions() {
        notificationLayoutBig.setOnClickPendingIntent(R.id.action_play_pause,
                retrievePlaybackActions(1));

        notificationLayoutBig.setOnClickPendingIntent(R.id.action_next,
                retrievePlaybackActions(2));

        notificationLayoutBig.setOnClickPendingIntent(R.id.action_prev,
                retrievePlaybackActions(3));

        notificationLayoutBig.setOnClickPendingIntent(R.id.action_quit,
                retrievePlaybackActions(4));

        notificationLayoutBig.setImageViewResource(R.id.action_play_pause, getPlayPauseRes(isDark));
    }

    private void setUpPlaybackActions() {
        notificationLayout.setOnClickPendingIntent(R.id.action_play_pause,
                retrievePlaybackActions(1));

        notificationLayout.setOnClickPendingIntent(R.id.action_next,
                retrievePlaybackActions(2));

        notificationLayout.setOnClickPendingIntent(R.id.action_prev,
                retrievePlaybackActions(3));

        notificationLayout.setImageViewResource(R.id.action_play_pause, getPlayPauseRes(isDark));
    }

    private PendingIntent retrievePlaybackActions(final int which) {
        Intent action;
        PendingIntent pendingIntent;
        final ComponentName serviceName = new ComponentName(service, MusicService.class);
        switch (which) {
            case 1:
                action = new Intent(MusicService.ACTION_TOGGLE_PAUSE);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(service, 1, action, 0);
                return pendingIntent;
            case 2:
                action = new Intent(MusicService.ACTION_SKIP);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(service, 2, action, 0);
                return pendingIntent;
            case 3:
                action = new Intent(MusicService.ACTION_REWIND);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(service, 3, action, 0);
                return pendingIntent;
            case 4:
                action = new Intent(MusicService.ACTION_QUIT);
                action.setComponent(serviceName);
                pendingIntent = PendingIntent.getService(service, 4, action, 0);
                return pendingIntent;
            default:
                break;
        }
        return null;
    }

    private void setUpCollapsedLayout() {
        notificationLayout.setTextViewText(R.id.title, currentSong.title);
        notificationLayout.setTextViewText(R.id.text, currentSong.artistName);
        notificationLayout.setTextViewText(R.id.text2, currentSong.albumName);
    }

    private void setUpExpandedLayout() {
        notificationLayoutBig.setTextViewText(R.id.title, currentSong.title);
        notificationLayoutBig.setTextViewText(R.id.text, currentSong.artistName);
        notificationLayoutBig.setTextViewText(R.id.text2, currentSong.albumName);
    }

    private void loadAlbumArt() {
        service.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(service)
                        .loadFromMediaStore(MusicUtil.getAlbumArtUri(currentSong.albumId))
                        .asBitmap()
                        .transcode(new BitmapPaletteTranscoder(service), BitmapPaletteWrapper.class)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .signature(new MediaStoreSignature("", currentSong.dateModified, 0))
                        .into(target);
            }
        });
    }

    private void setAlbumCover(@Nullable Bitmap cover, int bgColor) {
        if (cover != null) {
            notificationLayout.setImageViewBitmap(R.id.icon, cover);
            notificationLayoutBig.setImageViewBitmap(R.id.icon, cover);
        } else {
            notificationLayout.setImageViewResource(R.id.icon, R.drawable.default_album_art);
            notificationLayoutBig.setImageViewResource(R.id.icon, R.drawable.default_album_art);
        }

        if (!isColored) {
            bgColor = Color.TRANSPARENT;
        }
        setBackgroundColor(bgColor);
        setDarkNotificationContent(bgColor == Color.TRANSPARENT ? Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP : ColorUtil.useDarkTextColorOnBackground(bgColor));

        if (notification != null) {
            notificationManager.notify(notificationId, notification);
        }
    }

    private void setBackgroundColor(int color) {
        notificationLayout.setInt(R.id.root, "setBackgroundColor", color);
        notificationLayoutBig.setInt(R.id.root, "setBackgroundColor", color);
    }

    public void killNotification() {
        service.stopForeground(true);
        notification = null;
    }

    public void updatePlayState(final boolean setPlaying) {
        isPlaying = setPlaying;

        if (notification == null) {
            updateNotification();
        }
        int playPauseRes = getPlayPauseRes(isDark);
        if (notificationLayout != null) {
            notificationLayout.setImageViewResource(R.id.action_play_pause, playPauseRes);
        }
        if (notificationLayoutBig != null) {
            notificationLayoutBig.setImageViewResource(R.id.action_play_pause, playPauseRes);
        }
        if (notification != null) {
            notificationManager.notify(notificationId, notification);
        }
    }

    private void setDarkNotificationContent(boolean dark) {
        isDark = dark;

        if (notificationLayout != null && notificationLayoutBig != null) {
            int darkContentColor = ContextCompat.getColor(service, R.color.primary_text_default_material_light);
            int darkContentSecondaryColor = ContextCompat.getColor(service, R.color.secondary_text_default_material_light);
            int contentColor = ContextCompat.getColor(service, R.color.primary_text_default_material_dark);
            int contentSecondaryColor = ContextCompat.getColor(service, R.color.secondary_text_default_material_dark);

            notificationLayout.setTextColor(R.id.title, dark ? darkContentColor : contentColor);
            notificationLayout.setTextColor(R.id.text, dark ? darkContentSecondaryColor : contentSecondaryColor);
            notificationLayout.setImageViewResource(R.id.action_prev, dark ? R.drawable.ic_skip_previous_dark_36dp : R.drawable.ic_skip_previous_white_36dp);
            notificationLayout.setImageViewResource(R.id.action_play_pause, getPlayPauseRes(dark));
            notificationLayout.setImageViewResource(R.id.action_next, dark ? R.drawable.ic_skip_next_dark_36dp : R.drawable.ic_skip_next_white_36dp);

            notificationLayoutBig.setTextColor(R.id.title, dark ? darkContentColor : contentColor);
            notificationLayoutBig.setTextColor(R.id.text, dark ? darkContentSecondaryColor : contentSecondaryColor);
            notificationLayoutBig.setTextColor(R.id.text2, dark ? darkContentSecondaryColor : contentSecondaryColor);
            notificationLayoutBig.setImageViewResource(R.id.action_prev, dark ? R.drawable.ic_skip_previous_dark_36dp : R.drawable.ic_skip_previous_white_36dp);
            notificationLayoutBig.setImageViewResource(R.id.action_play_pause, getPlayPauseRes(dark));
            notificationLayoutBig.setImageViewResource(R.id.action_next, dark ? R.drawable.ic_skip_next_dark_36dp : R.drawable.ic_skip_next_white_36dp);
            notificationLayoutBig.setImageViewResource(R.id.action_quit, dark ? R.drawable.ic_close_dark_24dp : R.drawable.ic_close_white_24dp);
        }
    }

    private int getPlayPauseRes(boolean dark) {
        return isPlaying ? (dark ? R.drawable.ic_pause_dark_36dp : R.drawable.ic_pause_white_36dp) : (dark ? R.drawable.ic_play_arrow_dark_36dp : R.drawable.ic_play_arrow_white_36dp);
    }
}
