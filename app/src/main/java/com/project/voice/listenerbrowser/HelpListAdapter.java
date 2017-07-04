package com.project.voice.listenerbrowser;

import android.app.Activity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Devdeep on 05-Mar-17.
 */
public class HelpListAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final String[] helpText;
    private final Integer[] helpImageId;

    public HelpListAdapter(Activity context, String[] txt, Integer[] img) {
        super(context, R.layout.help_item, txt);
        this.context = context;
        this.helpText = txt;
        this.helpImageId = img;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View v = inflater.inflate(R.layout.help_item, null, true);
        TextView txtTitle = (TextView) v.findViewById(R.id.help_text);

        ImageView imageView = (ImageView) v.findViewById(R.id.help_image);
        txtTitle.setText(Html.fromHtml(helpText[position]));

        imageView.setImageResource(helpImageId[position]);
        return v;
    }
}