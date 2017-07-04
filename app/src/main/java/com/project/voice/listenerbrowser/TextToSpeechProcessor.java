package com.project.voice.listenerbrowser;

import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Devdeep on 04-Feb-17.
 */
public class TextToSpeechProcessor {
    private MainActivity mActivity;
    private final Handler mHandler;
    private TextToSpeech mTts = null;
    private HashMap<String, String> mTtsmap = new HashMap<String, String>();
    private int mTtsRegularId = 1;
    private int mTtsContinuousId = 1;
    private boolean mIsReadingLargeText = false;
    private boolean mIsReadingPaused = false;
    private boolean mIsReadingNonStop = false;
    private String mSentencesToSpeak[];
    private int mSentenceCount = 0;
    private CountDownTimer mPauseCountDownTimer = null;
    private static String TAG = "TextToSpeechProcessor";
    private static int PAUSE_COUNT = 10;

    public TextToSpeechProcessor(MainActivity main_activity) {
        mActivity = main_activity;
        mHandler = new Handler(main_activity.getMainLooper());
        mSentencesToSpeak = new String[0];
        mTts = new TextToSpeech(mActivity.getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    mTts.setLanguage(Locale.US);
                    mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {

                        }

                        @Override
                        public void onDone(String utteranceId) {
                            if (utteranceId.startsWith("continuous") && mIsReadingLargeText) {
                                speakNextSentence();
                                return;
                            }
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    new CountDownTimer(2000, 2000) {
                                        @Override
                                        public void onTick(long millisUntilFinished) {
                                            // TODO Auto-generated method stub
                                        }

                                        @Override
                                        public void onFinish() {
                                            if (mActivity.isActivityVisible())
                                                mActivity.startSpeechService();
                                            mIsReadingLargeText = false;
                                        }
                                    }.start();
                                }
                            });
                        }

                        @Override
                        public void onError(String utteranceId) {

                        }
                    });
                }
            }
        });
    }

    private void runOnUiThread(Runnable r) {
        mHandler.post(r);
    }

    public void speakLargeText(String toSpeak) {
        mSentencesToSpeak = toSpeak.split("(?<=\\.|!|\\?|\\n)");
        mSentenceCount = 0;
        mIsReadingLargeText = true;
        speakNextSentence();
    }

    public boolean speakNextSentence() {
        if (mSentencesToSpeak.length == 0) {
            speakText("Nothing to read", TextToSpeech.QUEUE_FLUSH, "regular");
            resetFlags();
            return false;
        }
        if (mSentenceCount >= mSentencesToSpeak.length || isReadingPaused()) {
            return false;
        }
        final String sp = mSentencesToSpeak[mSentenceCount];
        if (sp.length() > TextToSpeech.getMaxSpeechInputLength()) {
            Toast.makeText(mActivity.getApplicationContext(), "longer sentence:" + sp.length(), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mSentenceCount < mSentencesToSpeak.length - 1) {
            if (mSentenceCount > 0 && mSentenceCount % PAUSE_COUNT == 0 && !mActivity.isSpeechServiceStarted() && !isReadingNonStop()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mActivity.isActivityVisible())
                            mActivity.startSpeechService();
                    }
                });
            }
            else {
                speakText(sp, TextToSpeech.QUEUE_ADD, "continuous");
                mSentenceCount++;
            }
        }
        else {
            speakText(sp, TextToSpeech.QUEUE_ADD, "regular");
            mSentenceCount++;
        }
        //if (!isReadingPaused())
            //mSentenceCount++;

        return true;
    }

    public void speakText(String text, int queue_option, String idtype) {
        final String sptext = text, type= idtype;
        final int option = queue_option;
        if (!mActivity.isActivityVisible()) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mActivity.isSpeechServiceStarted())
                    mActivity.stopSpeechService();
                String id = "";
                if (type.equals("regular")) {
                    id = type + mTtsRegularId;
                    //mIsReading = true;
                    mTtsRegularId++;
                }
                else if (type.equals("continuous")) {
                    id = type + mTtsContinuousId;
                    mTtsContinuousId++;
                }

                mTtsmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
                mTts.speak(sptext, option, mTtsmap);
            }
        });
    }

    public void stop() {
        mTts.stop();
    }

    public void shutdown() {
        mTts.shutdown();
        resetFlags();
    }

    public void resetFlags() {
        setIsReadingPaused(false);
        setIsReadingLargeText(false);
        setIsReadingNonStop(false);
        if (mPauseCountDownTimer != null)
            mPauseCountDownTimer.cancel();
    }

    public void setIsReadingLargeText (boolean isReadingLargeText) {mIsReadingLargeText = isReadingLargeText;}

    public boolean isReadingLargeText() { return  mIsReadingLargeText;}

    public void setIsReadingPaused (boolean isReadingPaused) {mIsReadingPaused = isReadingPaused;}

    public boolean isReadingPaused() { return  mIsReadingPaused;}

    public void setIsReadingNonStop(boolean isReadingNonStop) { mIsReadingNonStop = isReadingNonStop;}

    public boolean isReadingNonStop() { return  mIsReadingNonStop;}

}

