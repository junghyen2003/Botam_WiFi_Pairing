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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.junghyen.botam_wifi_pairing.listAdpater.WifiListAdpater;
import com.example.junghyen.botam_wifi_pairing.listmodel.WifiList;

import java.util.ArrayList;
import java.util.List;

public class ConnectAPActivity extends AppCompatActivity {
    // WiFi-Manager
    WifiManager wifiManager;

    // Scan result List, value;

    // Select AP 이름
    String select_AP;

    // Connect 시킬 AP 이름 / Password
    String connect_ap;
    String connect_password;

    // UI
    TextView connectAP_selectedAP; // 이전 페이지에서 사용자가 선택한 장비 AP
    Button connectAP_search; // AP 검색 버튼
    EditText connectAP_selectWiFi; // 사용자가 입력한 WiFi AP 이름
    EditText connectAP_selectWiFi_password; // 사용자가 입력한 WiFi AP 비밀번호
    Button connectAP_previous; // 이전버튼
    Button connectAP_next; // 다음버튼

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
        setContentView(R.layout.activity_connect_ap);

        // 선택한 장비의 AP 가져옴
        Intent i = getIntent();
        select_AP = i.getCharSequenceExtra("select_AP").toString();

        // UI 셋팅
        connectAP_selectedAP = (TextView)findViewById(R.id.connectAP_selectedAP);
        connectAP_selectedAP.setText(select_AP);
        connectAP_search = (Button)findViewById(R.id.connectAP_search);
        connectAP_selectWiFi = (EditText)findViewById(R.id.connectAP_selectWiFi);
        connectAP_selectWiFi_password = (EditText)findViewById(R.id.connectAP_selectWiFi_password);
        connectAP_previous = (Button)findViewById(R.id.connectAP_previous);
        connectAP_next = (Button)findViewById(R.id.connectAP_next);

        listView = (ListView)findViewById(R.id.connect_ap_wifi_list);
        list_wifi = new ArrayList<WifiList>();
        mWifiListAdapter = new WifiListAdpater(ConnectAPActivity.this, list_wifi);
        listView.setAdapter(mWifiListAdapter);

        // 리스트뷰 아이템 클릭 시 이벤트
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                connectAP_selectWiFi.setText(list_wifi.get(i).getWifi_ssid());
            }
        });

        // WiFi 서비스 셋팅
        wifiManager = (WifiManager)this.getApplicationContext().getSystemService(WIFI_SERVICE);
        // WiFi 사용이 off 면 사용 가능하게 변경
        if(wifiManager.isWifiEnabled() == false){
            wifiManager.setWifiEnabled(true);
        }

        // AP 검색 버튼 동작
        connectAP_search.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                // wifi 리스트 검색
                // AP 검색 시 주변 wifi 이름을 리스트에 보여줌
                initWIFIScan();
            }
        });

        // 이전 버튼 동작
        connectAP_previous.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        // 다음 버튼 동작 ( 선택한 장비 AP, 연결할 공유기 SSID, Password 가지고 넘어감 ) -> InputURL
        connectAP_next.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ConnectAPActivity.this, InputURLActivity.class);
                intent.putExtra("select_AP", select_AP); // 장비 AP
                intent.putExtra("select_Wifi", connectAP_selectWiFi.getText()); // 연결할 공유기 AP
                intent.putExtra("select_Wifi_password",connectAP_selectWiFi_password.getText()); // 연결할 공유기 AP 비밀번호
                if(connectAP_selectWiFi.getText().length() == 0) {
                    Toast.makeText(getApplicationContext(),"장비를 연결 시킬 Wi-Fi를 선택하세요.",Toast.LENGTH_SHORT).show();
                }
                else if(connectAP_selectWiFi_password.getText().length() == 0){
                    Toast.makeText(getApplicationContext(),"Wi-Fi 비밀번호를 입력하세요.",Toast.LENGTH_SHORT).show();
                }
                else{
                    startActivity(intent);
                }
            }
        });

        // password 키보드 완료버튼 동작
        connectAP_selectWiFi_password.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_DONE:
                        // 완료 동작
                        Intent intent = new Intent(ConnectAPActivity.this, InputURLActivity.class);
                        intent.putExtra("select_AP", select_AP); // 장비 AP
                        intent.putExtra("select_Wifi", connectAP_selectWiFi.getText()); // 연결할 공유기 AP
                        intent.putExtra("select_Wifi_password",connectAP_selectWiFi_password.getText()); // 연결할 공유기 AP 비밀번호
                        if(connectAP_selectWiFi.getText().length() == 0) {
                            Toast.makeText(getApplicationContext(),"장비를 연결 시킬 Wi-Fi를 선택하세요.",Toast.LENGTH_SHORT).show();
                        }
                        else if(connectAP_selectWiFi_password.getText().length() == 0){
                            Toast.makeText(getApplicationContext(),"Wi-Fi 비밀번호를 입력하세요.",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            startActivity(intent);
                        }
                        break;
                    default:
                        // 기본 엔터키 동작
                        return false;
                }
                return true;
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
