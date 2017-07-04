package com.project.voice.listenerbrowser;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Created by Devdeep on 05-Mar-17.
 */
public class HelpFragment extends Fragment {
    ListView list;
    String[] helpTxt = {
            "<b>Start Listening</b><br>Hit this button to make the browser listen. When &#x1f442; is visible, use one of the following commands. The browser does not listen when it is reading or playing music.",
            "<b>Record Audio Permissions</b><br> This app requires 'record audio permissions' to hear your commands. The browser does not store any recordings. View our <a href=\"http://www.yahoo.com\">Privacy Policy</a>.",
            "Say <b>'open/go to/find/show <i>any query'</i></b><br><b>Ex: 'show good restaurants'</b> opens most relevant page matching the query 'good restaurants.'<br>" +
                    "<b>Ex: 'open example.com'</b> opens example.com",
            "<b>Ex: 'play jazz music'</b> would play most relevant 'jazz music' video from Youtube.<br>" +
                    "<b>Ex: 'play <i>any song or video name</i></b>' would play the video on Youtube.</b><br>" +
                    "<i>You need to have the Youtube app on your phone to use this feature.</i>",
            "Say <b>'start'</b> when a page is opened to read the page with pauses. The browser is listening during the pauses.<br>Say <b>'stop'</b> during the pauses to stop reading or give another command.",
            "Say <b>'start non stop'</b> to read a page without pauses.",
            "Makes the browser go back a page.",
            "<b>Send Feedback</b><br>Hit this button to send feedback to listenerbrowser@gmail.com"
    } ;
    Integer[] helpImageId = {
            R.drawable.help_1,
            R.drawable.help_0,
            R.drawable.help_2,
            R.drawable.help_3,
            R.drawable.help_4,
            R.drawable.help_5,
            R.drawable.help_6,
            R.drawable.help_7
    };

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.help_fragment, container, false);
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        HelpListAdapter adapter = new
                HelpListAdapter(getActivity(),helpTxt, helpImageId);
        list=(ListView)getView().findViewById(R.id.help_list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    ((MainActivity)getActivity()).loadPrivacyPolicyInWebview();
                }
            }
        });
        list.setAdapter(adapter);
    }
}
