package com.project.voice.listenerbrowser;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubePlayerFragment;

public class MainActivity extends AppCompatActivity {
    private int mBindFlag;
    private Messenger mServiceMessenger;
    private TextView mListenerText;
    private WebView mSpeechWebView;
    private Button mSpeechServiceButton;
    private Button mHelpButton;
    private Button mFeedbackButton;
    private static String TAG = "MainActivity";
    private Intent mSpeechService;
    private boolean mSpeechServiceStarted = false;
    private boolean mIsActivityVisible = false;
    private Context mContext = this;
    private Activity mActivity = this;
    private TextToSpeechProcessor mTTSProcessor;
    private AudioManager mAudioManager;
    private int mOrigNotificationVol;
    private int mOrigMusicVol;
    private YouTubePlayerFragment mYouTubePlayerFragment;
    private HelpFragment mHelpFragment;
    private YTPlayer mYTPlayer;
    private boolean mShowSplash = true;

    static final String PRIVACY_POLICY_URL = "https://www.iubenda.com/privacy-policy/8076942";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListenerText = (TextView) findViewById(R.id.listening_tv);
        mListenerText.setTextColor(Color.RED);
        mListenerText.setVisibility(View.INVISIBLE);
        mSpeechServiceButton = (Button) findViewById(R.id.speech_button);
        mHelpButton = (Button) findViewById(R.id.help_button);
        mFeedbackButton = (Button) findViewById(R.id.feedback_button);
        mSpeechService = new Intent(this, VoiceCommandService.class);
        mTTSProcessor = new TextToSpeechProcessor(MainActivity.this);
        mYouTubePlayerFragment = YouTubePlayerFragment.newInstance();
        mHelpFragment = new HelpFragment();
        mYTPlayer = new YTPlayer(MainActivity.this, mYouTubePlayerFragment);

        mSpeechServiceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!mSpeechServiceStarted) {
                    if(PermissionHandler.checkPermission(mActivity,PermissionHandler.RECORD_AUDIO)) {
                        //mTTSProcessor.setIsReadingLargeText(false);
                        if (mTTSProcessor.isReadingLargeText())
                            mTTSProcessor.setIsReadingPaused(true);
                        if (mYouTubePlayerFragment.isVisible()) {
                            removeYTFragment();
                        }
                        startSpeechService();
                    }
                    else
                    {
                        PermissionHandler.askForPermission(PermissionHandler.RECORD_AUDIO, mActivity);
                    }
                }
                else {
                    if (mTTSProcessor.isReadingLargeText()) {
                        mTTSProcessor.setIsReadingPaused(true);
                        mTTSProcessor.stop();
                    }
                    stopSpeechService();
                }
            }
        });

        mFeedbackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                String uriText = "mailto:" + Uri.encode("listenerbrowser@gmail.com");
                Uri uri = Uri.parse(uriText);
                intent.setData(uri);
                startActivity(Intent.createChooser(intent, "Send Feedback"));
            }
        });

        mHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mHelpFragment.isVisible())
                    addHelpFragment();
                else removeHelpFragment();
            }
        });

        mBindFlag = Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ? 0 : Context.BIND_ABOVE_CLIENT;

        mSpeechWebView = (WebView) findViewById(R.id.webView);
        setupSpeechWebView();
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }

    Messenger replyMessenger = new Messenger(new HandlerReplyMsg());

    class HandlerReplyMsg extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String recdMessage = msg.obj.toString(); //msg received from service
            if (recdMessage.isEmpty()) {
                mListenerText.setVisibility(View.INVISIBLE);
                if (mTTSProcessor.isReadingLargeText() && !mTTSProcessor.isReadingPaused()) {
                    mTTSProcessor.speakNextSentence();
                }
            }
            else if (recdMessage.equals("LISTENING")){
                mListenerText.setVisibility(View.VISIBLE);
                return;
            }

            if (!recdMessage.isEmpty()) {
                //String[] cmdpair = recdMessage.split(":");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                if (mHelpFragment.isVisible())
                    removeHelpFragment();
                int pos = recdMessage.indexOf(':');
                String cmd = recdMessage.substring(0, pos);
                final String action = recdMessage.substring(pos + 1);
                if (!cmd.isEmpty()) {
                    if(cmd.equals("load")) {
                        if (mShowSplash)
                            mShowSplash = false;
                        if(mTTSProcessor.isReadingLargeText()) {
                           mTTSProcessor.stop();
                           mTTSProcessor.setIsReadingPaused(true);
                        }
                        String url = action;
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            mTTSProcessor.speakText("loading most relevant page", TextToSpeech.QUEUE_FLUSH, "regular");
                            mSpeechWebView.loadUrl(url);
                        }
                        else {
                            mTTSProcessor.speakText("loading " + action, TextToSpeech.QUEUE_FLUSH, "regular");
                            mSpeechWebView.loadUrl("http://www." + url + "/");
                        }
                    }
                    else if(cmd.equals("back")) {
                        if(mTTSProcessor.isReadingLargeText()) {
                            mTTSProcessor.stop();
                            mTTSProcessor.setIsReadingPaused(true);
                        }
                        if (mSpeechWebView.canGoBack()) {
                            mTTSProcessor.speakText("going back", TextToSpeech.QUEUE_FLUSH, "regular");
                            mSpeechWebView.goBack();
                        } else {
                            mTTSProcessor.speakText("can't go back", TextToSpeech.QUEUE_FLUSH, "regular");
                            finish();
                        }
                    }
                    else if (cmd.equals("play")) {
                        if(mTTSProcessor.isReadingLargeText()) {
                            mTTSProcessor.stop();
                            mTTSProcessor.setIsReadingPaused(true);
                        }

                        stopSpeechService();
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_MUSIC), 0);
                        final Handler handler = new Handler();

                        final Runnable r = new Runnable() {
                            public void run() {
                                addYTFragment(action);
                            }
                        };

                        handler.postDelayed(r, 1000);
                    }
                    else if(cmd.equals("stop")) {
                        if(mTTSProcessor.isReadingLargeText()) {
                            mTTSProcessor.setIsReadingPaused(true);
                        }
                    }
                    else if(cmd.equals("read") || cmd.equals("readns")) {
                        if (!mSpeechWebViewClient.hasNotified()) {
                            mTTSProcessor.speakText("page not yet loaded", TextToSpeech.QUEUE_FLUSH, "regular");
                            return;
                        }
                        if (cmd.equals("readns")) {
                            mTTSProcessor.setIsReadingNonStop(true);
                        }
                        else mTTSProcessor.setIsReadingNonStop(false);
                        if (mTTSProcessor.isReadingLargeText() && !mTTSProcessor.isReadingPaused()) return;
                        if (mTTSProcessor.isReadingPaused()) {
                            mTTSProcessor.setIsReadingPaused(false);
                            //mTTSProcessor.setIsReadingLargeText(true);
                            boolean result = mTTSProcessor.speakNextSentence();
                            if (!result)
                                mSpeechWebView.loadUrl("javascript:window.INTERFACE.processContent(document.getElementsByTagName('body')[0].innerText);");
                        }
                        else mSpeechWebView.loadUrl("javascript:window.INTERFACE.processContent(document.getElementsByTagName('body')[0].innerText);");
                    }
                }

            }
        }
    }

    class SpeechWebViewClient extends WebViewClient {
        private boolean mHasNotified = false;
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mHasNotified = false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(mActivity.getApplicationContext(), "load error:"+description, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (url.startsWith("file:///android_res/drawable/"))
                mShowSplash = true;
        }

        public void resetTTSAndNotify(String url) {
            if (!mHasNotified) {
                if (!url.equals("about:blank") && !url.startsWith("file:") && !url.equals(PRIVACY_POLICY_URL) && !mYouTubePlayerFragment.isVisible())
                    mTTSProcessor.speakText("page load finished", TextToSpeech.QUEUE_FLUSH, "regular");

                mTTSProcessor.resetFlags();

                mHasNotified = true;
            }
        }

        public boolean hasNotified() {
            return mHasNotified;
        }
    }

    class SpeechWebChromeClient extends WebChromeClient {
        private  SpeechWebViewClient mSpeechWebViewClientRef;

        public SpeechWebChromeClient(SpeechWebViewClient speechWebViewClient) {
            mSpeechWebViewClientRef = speechWebViewClient;
        }
        @Override
        public void onProgressChanged(WebView view, int progress) {
            if (progress >= 90 && !mSpeechWebViewClientRef.hasNotified()) {
                mSpeechWebViewClientRef.resetTTSAndNotify(view.getUrl());
            }
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            return Bitmap.createBitmap(1, 1,Bitmap.Config.ARGB_8888);
        }

    }

    private SpeechWebViewClient mSpeechWebViewClient = new SpeechWebViewClient();
    private SpeechWebChromeClient mSpeechWebChromeClient = new SpeechWebChromeClient(mSpeechWebViewClient);

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {

            mServiceMessenger = new Messenger(service);

            Message msg = new Message();
            msg.what = VoiceCommandService.MSG_RECOGNIZER_SETUP_RESULT;
            msg.replyTo = replyMessenger;

            try
            {
                mServiceMessenger.send(msg);
                msg = new Message();
                msg.what = VoiceCommandService.MSG_RECOGNIZER_START_LISTENING;
                mServiceMessenger.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mServiceMessenger = null;
        }

    }; // mServiceConnection

    public void setupSpeechWebView() {
        class WebViewJavaScriptInterface {
            @SuppressWarnings("unused")
            @JavascriptInterface
            public void processContent(final String extractedContent) {
                mTTSProcessor.speakLargeText(extractedContent);
            }
        }
        mSpeechWebView.getSettings().setJavaScriptEnabled(true);
        mSpeechWebView.addJavascriptInterface(new WebViewJavaScriptInterface(), "INTERFACE");
        //mSpeechWebView.getSettings().setLoadWithOverviewMode(true);
        //mSpeechWebView.getSettings().setUseWideViewPort(true);
        mSpeechWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mSpeechWebView.setWebViewClient(mSpeechWebViewClient);
        mSpeechWebView.setWebChromeClient(mSpeechWebChromeClient);
        loadSplashInWebview();
    }
    void loadSplashInWebview() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            mSpeechWebView.loadDataWithBaseURL("file:///android_res/drawable/", "<img src=\"splash.png\"  width=\"98%\"/>", "text/html", "utf-8", null);
        else
            mSpeechWebView.loadDataWithBaseURL("file:///android_res/drawable/", "<img src=\"splash_land.png\"  width=\"98%\"/>", "text/html", "utf-8", null);
    }

    void loadPrivacyPolicyInWebview() {
        removeHelpFragment();
        mSpeechWebView.loadUrl(PRIVACY_POLICY_URL);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!mShowSplash)
            return;
        loadSplashInWebview();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mIsActivityVisible = true;
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        stopSpeechService();
        mTTSProcessor.stop();
        mIsActivityVisible = false;
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mOrigNotificationVol, 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOrigMusicVol, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSpeechService();
        mTTSProcessor.stop();
        mIsActivityVisible = false;
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mOrigNotificationVol, 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOrigMusicVol, 0);
        if (mYouTubePlayerFragment.isVisible())
            removeYTFragment();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityVisible= true;
        mOrigNotificationVol = mAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        mOrigMusicVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_NOTIFICATION)/2, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTTSProcessor.shutdown();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode)
        {
            case PermissionHandler.RECORD_AUDIO:
                if(grantResults.length>0) {
                    if(grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                        mSpeechServiceButton.performClick();
                    }
                }
                break;

        }
    }

    public boolean isActivityVisible() {
        return mIsActivityVisible;
    }

    public boolean isSpeechServiceStarted() {
        return mSpeechServiceStarted;
    }

    public void startSpeechService() {
        if (isSpeechServiceStarted()) return;
        mTTSProcessor.stop();
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);

        mSpeechServiceButton.setText("Stop Listening");
        startService(mSpeechService);
        bindService(new Intent(mContext, VoiceCommandService.class), mServiceConnection, mBindFlag);
        mSpeechServiceStarted = true;
    }

    public void stopSpeechService() {
        if (!isSpeechServiceStarted()) return;
        if (mServiceMessenger != null)
        {
            unbindService(mServiceConnection);
            mServiceMessenger = null;
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(mAudioManager.STREAM_MUSIC)/2, 0);
        mSpeechServiceButton.setText("Start Listening");
        mListenerText.setVisibility(View.INVISIBLE);
        stopService(mSpeechService);
        mSpeechServiceStarted = false;
    }

    public void addYTFragment(String video_id) {
        FragmentManager fragManager=getFragmentManager();
        FragmentTransaction fragmentTransaction=fragManager.beginTransaction();
        mYTPlayer.playVideo(video_id);
        fragmentTransaction.add(R.id.youtube_fragment, mYouTubePlayerFragment);
        fragmentTransaction.commit();
    }

    public void removeYTFragment() {
        FragmentManager fragManager=getFragmentManager();
        FragmentTransaction fragmentTransaction=fragManager.beginTransaction();
        fragmentTransaction.remove(mYouTubePlayerFragment);
        fragmentTransaction.commit();
    }

    public void addHelpFragment() {
        FragmentManager fragManager=getFragmentManager();
        FragmentTransaction fragmentTransaction=fragManager.beginTransaction();
        fragmentTransaction.add(R.id.help_fragment, mHelpFragment);
        fragmentTransaction.commit();
    }

    public void removeHelpFragment() {
        FragmentManager fragManager=getFragmentManager();
        FragmentTransaction fragmentTransaction=fragManager.beginTransaction();
        fragmentTransaction.remove(mHelpFragment);
        fragmentTransaction.commit();
    }

}
