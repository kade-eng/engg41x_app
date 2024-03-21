package com.example.engg41x_nav_app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class OBDAdapter extends ArrayAdapter<OBD> {
    public OBDAdapter(Context context, ArrayList<OBD> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        OBD item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.obd_codes, parent, false);
        }

        TextView itemId = convertView.findViewById(R.id.item_id);
        TextView warningLevel = convertView.findViewById(R.id.warning_level);
        itemId.setText(item.getCode());
        warningLevel.setText(item.getLevel());

        int fadedRed = Color.parseColor("#FFCDD2");
        int fadedYellow = Color.parseColor("#FFF9C4");
        int fadedGreen = Color.parseColor("#C8E6C9");

        switch (item.getLevel().toLowerCase()) {
            case "high":
                convertView.setBackgroundColor(fadedRed);
                break;
            case "medium":
                convertView.setBackgroundColor(fadedYellow);
                break;
            case "low":
                convertView.setBackgroundColor(fadedGreen);
                break;
        }
        return convertView;
    }
}

