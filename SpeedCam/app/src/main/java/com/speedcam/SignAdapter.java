package com.speedcam;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;


public class SignAdapter extends ArrayAdapter<Integer> {
    ArrayList<Integer> signs;
    Context mContext;

    public SignAdapter(Context context, ArrayList<Integer> signs) {
        super(context, R.layout.signview_item);
        this.signs = signs;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return signs.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder = new ViewHolder();
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.signview_item, parent, false);
            viewHolder.signVIew = convertView.findViewById(R.id.imageView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.signVIew.setImageResource(signs.get(position));

        return convertView;
    }

    static class ViewHolder {
        ImageView signVIew;
    }
}
