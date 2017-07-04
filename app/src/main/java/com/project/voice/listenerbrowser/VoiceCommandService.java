package com.project.voice.listenerbrowser;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Devdeep on 04-Feb-17.
 */
public class VoiceCommandService extends Service
{
    protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));
    protected Messenger mResultMessenger;

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private SpeechResponseProcessor mResponseProcessor;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_RESULT = 2;
    static final int MSG_RECOGNIZER_SETUP_RESULT = 3;
    private static String TAG = "VoiceCommandService";
    private String mLastResult = "";
    private Handler mNoSpeechHandler;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mResponseProcessor = new SpeechResponseProcessor(VoiceCommandService.this);
        mNoSpeechHandler = new Handler();
        createSpeechRecognizer();
    }

    public void createSpeechRecognizer() {
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
    }

    protected static class IncomingHandler extends Handler
    {
        private WeakReference<VoiceCommandService> mtarget;

        IncomingHandler(VoiceCommandService target)
        {
            mtarget = new WeakReference<VoiceCommandService>(target);
        }


        @Override
        public void handleMessage(Message msg)
        {
            final VoiceCommandService target = mtarget.get();

            switch (msg.what)
            {
                case MSG_RECOGNIZER_START_LISTENING:
                    /*if (target.mResultMessenger != null /*&& !target.mLastResult.isEmpty()) {
                        try {
                            Message message = new Message();
                            message.obj = target.mLastResult;
                            target.mResultMessenger.send(message);//replying / sending msg to activity
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }*/
                    //sendMessageToActivity(target.mLastResult);
                    sendMessageToActivity("LISTENING");
                    if (!target.mIsListening)
                    {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                    }
                    break;

                case MSG_RECOGNIZER_RESULT:
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    sendMessageToActivity(target.mLastResult);
                    target.mNoSpeechHandler.removeCallbacksAndMessages(null);
                    break;

                case MSG_RECOGNIZER_SETUP_RESULT:
                    target.mResultMessenger = msg.replyTo;
                    break;
            }
        }

        public void sendMessageToActivity(String msg) {
            final VoiceCommandService target = mtarget.get();
            if (target.mResultMessenger != null /*&& !msg.isEmpty()*/) {
                try {
                    Message message = new Message();
                    message.obj = msg;
                    target.mResultMessenger.send(message);//replying / sending msg to activity
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(2000, 2000)
    {

        @Override
        public void onTick(long millisUntilFinished)
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void onFinish()
        {
            mIsCountDownOn = false;
            //Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                //mServerMessenger.send(message);
                Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                //message.obj = mLastResult;
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (mIsCountDownOn)
        {
            mNoSpeechCountDown.cancel();
        }
        mNoSpeechHandler.removeCallbacksAndMessages(null);
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }
    }

    protected class SpeechRecognitionListener implements RecognitionListener
    {

        private static final String TAG = "SpeechRecognitionListener";

        @Override
        public void onBeginningOfSpeech()
        {
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
        }

        @Override
        public void onError(int error)
        {
            /*if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            mLastResult = "";
            startSpeechCountDown();*/
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {

        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {

        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {

            final Runnable r = new Runnable() {
                public void run() {
                    if (mIsCountDownOn)
                    {
                        mIsCountDownOn = false;
                        mNoSpeechCountDown.cancel();
                    }
                    mIsListening = false;
                    mLastResult = "";
                    startSpeechCountDown();
                }
            };

            mNoSpeechHandler.postDelayed(r, 5000);
        }

        @Override
        public void onResults(Bundle results)
        {
            mNoSpeechHandler.removeCallbacksAndMessages(null);
            ArrayList<String> matches = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String text = "";
            for (String result : matches)
                text += result + "\n";
            //mLastResult = text;
            mLastResult = mResponseProcessor.processMatches(matches);
            if (!mLastResult.isEmpty()) {
                Toast.makeText(getApplicationContext(), "You said: "+matches.get(0), Toast.LENGTH_SHORT).show();
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            if (mLastResult.equals("QUERY") || mLastResult.equals("PLAY")) {
                mLastResult = "";
                return;
            }
            //mIsCountDownOn = true;
            //mNoSpeechCountDown.start();
            startSpeechCountDown();

        }

        @Override
        public void onRmsChanged(float rmsdB)
        {

        }

        public String getErrorText(int errorCode) {
            String message;
            switch (errorCode) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "Audio recording error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    message = "Client side error";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "Insufficient permissions";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "Network error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "Network timeout";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    message = "No match";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RecognitionService busy";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "error from server";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    message = "No speech input";
                    break;
                default:
                    message = "Didn't understand, please try again.";
                    break;
            }
            return message;
        }

    }

    public void startSpeechCountDown() {
        Message message = Message.obtain(null, MSG_RECOGNIZER_RESULT);
        try
        {
            mServerMessenger.send(message);
        }
        catch (RemoteException e)
        {
        }
        mIsCountDownOn = true;
        mNoSpeechCountDown.start();
    }

    public void setLastResult(String result) {
        mLastResult = result;
    }

    @Override
    public IBinder onBind(Intent arg0) {

        return mServerMessenger.getBinder();
    }
}

