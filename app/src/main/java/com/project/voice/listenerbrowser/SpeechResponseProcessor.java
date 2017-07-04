package com.project.voice.listenerbrowser;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Devdeep on 04-Feb-17.
 */
public class SpeechResponseProcessor {
    private String[] loadOptions = {"load", "open", "go to", "lord", "show", "find"};
    private String[] backOptions = {"back", "go back"};
    private String[] readOptions = {"read", "lead", "start", "stark"};
    private String[] stopOptions = {"stop", "pause", "stock"};
    private String[] playOptions = {"play"};
    private String QUERY_URL = "https://www.google.com/search?q=";
    private String VIDEO_QUERY_OPTION = "&tbm=vid";
    private static String TAG = "SpeechResponseProcessor";
    private VoiceCommandService mVoiceCommandService;

    public SpeechResponseProcessor(VoiceCommandService voiceCommandService) {
        mVoiceCommandService = voiceCommandService;
    }

    public String processMatches(ArrayList<String> matches) {
        String topMatch = matches.get(0);
        return processCommand(topMatch);
    }

    private String processCommand(String cmd) {
        String processed = "";
        cmd = cmd.toLowerCase();
        for(String option: readOptions) {
            if (cmd.startsWith(option)) {
                if (cmd.endsWith("non-stop") || cmd.endsWith("non stop"))
                    processed = "readns:readns";
                else processed = "read:read";
                return processed;
            }
        }

        for(String option: backOptions) {
            if (cmd.startsWith(option)) {
                processed = "back:back";
                return processed;
            }
        }

        for(String option: stopOptions) {
            if (cmd.startsWith(option)) {
                processed = "stop:stop";
                return processed;
            }
        }

        for(String option: playOptions) {
            if (cmd.startsWith(option) && cmd.length() > option.length()) {
                String query = cmd.substring(option.length() + 1);
                new VideoSearchTask().execute(QUERY_URL+query+VIDEO_QUERY_OPTION);
                return "PLAY";
            }
        }

        for(String option: loadOptions) {
            if (cmd.startsWith(option) && cmd.length() > option.length()) {
                String query = cmd.substring(option.length() + 1);
                if (query.equals("google"))
                    query = "google.com";
                if (!query.contains(".")) {
                    new SearchTask().execute(QUERY_URL+query);
                    return "QUERY";
                }
                query = query.replaceAll("\\s","");
                processed = "load:" + query;
                return processed;
            }
        }

        return processed;
    }

    private class SearchTask extends AsyncTask<String, Void, Integer> {
        Document document;
        protected Integer doInBackground(String... urls) {
            try
            {
                document = Jsoup.connect(urls[0]).get();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return -1;
            }
            return 0;
        }

        protected void onPostExecute(Integer result) {
            if (result == 0) {
                Elements links = document.select("a[href]");
                if (links.size() >= 19) {
                    for (int i = 0; i < links.size(); i++) {
                        String url = links.get(i).attr("href");
                        if (!url.startsWith("javascript") && !url.contains("google") && !url.contains("/search?") && (url.startsWith("http://") || url.startsWith("https://"))) {
                            mVoiceCommandService.setLastResult("load:"+url);
                            break;
                        }
                    }
                }
            }
            mVoiceCommandService.startSpeechCountDown();
        }
    }

    private class VideoSearchTask extends AsyncTask<String, Void, Integer> {
        Document document;
        protected Integer doInBackground(String... urls) {
            try
            {
                document = Jsoup.connect(urls[0]).get();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return -1;
            }
            return 0;
        }

        protected void onPostExecute(Integer result) {
            if (result == 0) {
                Elements links = document.select("a[href]");
                if (links.size() >= 19) {
                    for (int i = 0; i < links.size(); i++) {
                        String url = links.get(i).attr("href");
                        if (!url.contains("/search?") && url.contains("youtube") && !url.contains("/channel/") && !url.contains("/user/")) {
                            String vid = url.substring(url.lastIndexOf("v=")+2);
                            mVoiceCommandService.setLastResult("play:"+vid);
                            break;
                        }
                    }
                }
            }
            mVoiceCommandService.startSpeechCountDown();
        }

    }


}
