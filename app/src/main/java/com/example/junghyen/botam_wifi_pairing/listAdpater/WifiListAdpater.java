package com.example.junghyen.botam_wifi_pairing.listAdpater;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.junghyen.botam_wifi_pairing.R;
import com.example.junghyen.botam_wifi_pairing.listmodel.WifiList;

import java.util.ArrayList;

/**
 * Created by jungh on 2017-05-09.
 */

public class WifiListAdpater extends BaseAdapter{
    Context context;
    ArrayList<WifiList> list_wifi;
    ViewHolder viewHolder;

    class ViewHolder{
        TextView wifi_list_ssid_textView;
    }

    public WifiListAdpater(Context context, ArrayList<WifiList> list_wifi){
        this.context = context;
        this.list_wifi = list_wifi;
    }

    @Override
    public int getCount() {
        return this.list_wifi.size();
    }

    @Override
    public Object getItem(int i) {
        return this.list_wifi.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null){
            view = LayoutInflater.from(context).inflate(R.layout.activity_wifi_list,null);
            viewHolder = new ViewHolder();
            viewHolder.wifi_list_ssid_textView = (TextView)view.findViewById(R.id.wifi_list_ssid_textView);

            view.setTag(viewHolder);
        } else{
            viewHolder = (ViewHolder)view.getTag();
        }

        viewHolder.wifi_list_ssid_textView.setText(list_wifi.get(i).getWifi_ssid());

        return view;
    }
}
