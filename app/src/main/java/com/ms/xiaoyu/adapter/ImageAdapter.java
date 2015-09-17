package com.ms.xiaoyu.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.ms.xiaoyu.mysketch.R;

import java.util.List;

/**
 * Created by xiaoyu on 7/5/15.
 */
public class ImageAdapter extends ArrayAdapter<Bitmap> {
    private Context context;
    private List<Bitmap> list;
    public ImageAdapter(Context context, int resourceID, List<Bitmap> objects){
        super(context,resourceID,objects);
        this.context = context;
        this.list = objects;
    }
    /* private view holder class */
    private class ViewHolder {
        ImageView img;
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Bitmap getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Bitmap bm = getItem(position);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item, parent,false);
            holder = new ViewHolder();
            holder.img = (ImageView) convertView
                    .findViewById(R.id.img);

            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.img.setImageBitmap(bm);
        return convertView;
    }
    public void refresh(List<Bitmap> list){
        this.list = list;
        notifyDataSetChanged();
    }

}
