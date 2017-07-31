package com.example.junghyen.botam_wifi_pairing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.junghyen.botam_wifi_pairing.listAdpater.WifiListAdpater;
import com.example.junghyen.botam_wifi_pairing.listmodel.WifiList;

import java.util.ArrayList;
import java.util.List;

public class SelectAPActivity extends AppCompatActivity {

    // WiFi-Manager
    WifiManager wifiManager;

    // UI
    Button selectAP_search; // AP 검색 버튼
    Button selectAP_next; // 다음버튼
    Button selectAP_previous; // 이전버튼
    EditText selectAP_WiFi_name; // 사용자가 입력한 Wifi AP 이름

    // Scan result List
    private List<ScanResult> mScanResult;

    // SSID List
    ListView listView;
    ArrayList<WifiList> list_wifi;
    WifiListAdpater mWifiListAdapter;

    // 브로드캐스트 리시버
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                getWIFIScanResult(); // get WIFISCanResult
                //wifiManager.startScan(); // for refresh
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_ap);

        // UI 셋팅
        selectAP_search = (Button)findViewById(R.id.selectAP_search);
        selectAP_next = (Button)findViewById(R.id.selectAP_next);
        selectAP_previous = (Button)findViewById(R.id.selectAP_previous);
        selectAP_WiFi_name = (EditText)findViewById(R.id.selectAP_WiFi_name);

        listView = (ListView)findViewById(R.id.select_ap_wifi_list);
        list_wifi = new ArrayList<WifiList>();
        mWifiListAdapter = new WifiListAdpater(SelectAPActivity.this, list_wifi);
        listView.setAdapter(mWifiListAdapter);

        // 리스트뷰 아이템 클릭 시 이벤트
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectAP_WiFi_name.setText(list_wifi.get(i).getWifi_ssid());
            }
        });

        // WiFi 서비스 셋팅
        wifiManager = (WifiManager)this.getApplicationContext().getSystemService(WIFI_SERVICE);
        // WiFi 사용이 off 면 사용 가능하게 변경
        if(wifiManager.isWifiEnabled() == false){
            wifiManager.setWifiEnabled(true);
        }

        // AP 검색 버튼 동작
        selectAP_search.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                // 리스트 비움
                //list_wifi.clear();
                // wifi 리스트 검색
                // AP 검색 시 주변 wifi 이름을 리스트에 보여줌
                initWIFIScan();
            }
        });

        // 이전 버튼 동작
        selectAP_previous.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        // 다음 버튼 동작 ( 입력된 WiFi Name 가지고 넘어감)
        selectAP_next.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SelectAPActivity.this, ConnectAPActivity.class);
                intent.putExtra("select_AP", selectAP_WiFi_name.getText().toString());
                if(selectAP_WiFi_name.getText().length() == 0) {
                    Toast.makeText(getApplicationContext(),"장비 Wi-Fi를 선택하세요.",Toast.LENGTH_SHORT).show();
                }
                else{
                    startActivity(intent);
                }
            }
        });
    }

    // AP 검색 동작
    public void initWIFIScan() {
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
        wifiManager.startScan();
        Log.d("Action : ", "initWIFIScan()");
    }

    public void getWIFIScanResult() {
        mScanResult = wifiManager.getScanResults(); // ScanResult
        // Scan count
        // 리스트 클리어
        list_wifi.clear();
        for (int i = 0; i < mScanResult.size(); i++) {
            ScanResult result = mScanResult.get(i);
            if(result.level > -50) {
                // 리스트에 추가
                list_wifi.add(new WifiList(result.SSID.toString()));
            }
        }
        // 리스트 정비
        mWifiListAdapter.notifyDataSetChanged();
    }
}
