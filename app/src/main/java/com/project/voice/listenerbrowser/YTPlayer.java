package com.project.voice.listenerbrowser;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

/**
 * Created by Devdeep on 05-Feb-17.
 */
public class YTPlayer implements YouTubePlayer.OnInitializedListener {

    public static final String YOUTUBE_API_KEY = "AIzaSyA-LiNNe4UK4I0SwWHSKTbCz_FooatSL04";
    private PlayerStateChangeListener mPlayerStateChangeListener;
    private MainActivity mActivity;
    private String mCurrentVideoId;
    private YouTubePlayerFragment mYoutubePlayerFragment;

    private final class PlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {

        @Override
        public void onLoading() {

        }

        @Override
        public void onLoaded(String s) {

        }

        @Override
        public void onAdStarted() {

        }

        @Override
        public void onVideoStarted() {
        }

        @Override
        public void onVideoEnded() {
            mActivity.removeYTFragment();
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            mActivity.startSpeechService();
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {
        }
    }

    public YTPlayer(MainActivity main_activity, YouTubePlayerFragment yt_fragment) {
        mActivity = main_activity;
        mYoutubePlayerFragment = yt_fragment;
        mPlayerStateChangeListener = new PlayerStateChangeListener();
    }

    public void playVideo(String video_id) {
        mCurrentVideoId = video_id;
        mYoutubePlayerFragment.initialize(YOUTUBE_API_KEY, this);
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        if (b) return;
        youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
        youTubePlayer.setPlayerStateChangeListener(mPlayerStateChangeListener);
        if (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            youTubePlayer.setFullscreen(true);
        else youTubePlayer.setFullscreen(false);
        youTubePlayer.setFullscreenControlFlags(YouTubePlayer.FULLSCREEN_FLAG_CONTROL_ORIENTATION | YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE);
        youTubePlayer.loadVideo(mCurrentVideoId);
        youTubePlayer.play();
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
    }
}
