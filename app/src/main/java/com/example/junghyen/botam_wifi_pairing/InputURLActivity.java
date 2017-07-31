package com.example.junghyen.botam_wifi_pairing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class InputURLActivity extends AppCompatActivity {

    // 이전 액티비티에서 가져온 정보
    String select_AP; // 장비 AP
    String select_Wifi; // 연결할 공유기 AP
    String select_Wifi_password; // 연결할 공유기 AP 비밀번호

    // UI
    EditText input_url_editText;
    TextView input_url_select_ap;
    TextView input_url_select_wifi;
    TextView input_url_select_wifi_password;
    Button input_url_previous;
    Button input_url_next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_url);

        // 이전 액티비티에서 정보 가져오기
        Intent i = getIntent();
        select_AP = i.getCharSequenceExtra("select_AP").toString();
        select_Wifi = i.getCharSequenceExtra("select_Wifi").toString();
        select_Wifi_password = i.getCharSequenceExtra("select_Wifi_password").toString();

        // UI 셋팅
        input_url_editText = (EditText)findViewById(R.id.input_url_editText);
        input_url_select_ap = (TextView)findViewById(R.id.input_url_select_ap);
        input_url_select_wifi = (TextView)findViewById(R.id.input_url_select_wifi);
        input_url_select_wifi_password = (TextView)findViewById(R.id.input_url_select_wifi_password);
        input_url_previous = (Button)findViewById(R.id.input_url_previous);
        input_url_next = (Button)findViewById(R.id.input_url_next);

        // 정보 셋팅
        input_url_select_ap.setText(select_AP);
        input_url_select_wifi.setText(select_Wifi);
        input_url_select_wifi_password.setText(select_Wifi_password);

        // 이전 버튼 동작
        input_url_previous.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        // 다음 버튼 동작 ( 입력된 URL 가지고 넘어감)
        input_url_next.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InputURLActivity.this, DebugActivity.class);
                intent.putExtra("server_url", input_url_editText.getText().toString());
                intent.putExtra("select_AP", select_AP); // 장비 AP
                intent.putExtra("select_Wifi", select_Wifi); // 연결할 공유기 AP
                intent.putExtra("select_Wifi_password",select_Wifi_password); // 연결할 공유기 AP 비밀번호
                if(input_url_editText.getText().length() == 0) {
                    Toast.makeText(getApplicationContext(),"서버 URL을 입력하세요.",Toast.LENGTH_SHORT).show();
                }
                else{
                    startActivity(intent);
                }
            }
        });
    }
}
