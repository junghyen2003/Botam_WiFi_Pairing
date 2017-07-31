package com.example.junghyen.botam_wifi_pairing;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import static android.os.Build.VERSION_CODES.M;

public class InfoActivity extends AppCompatActivity {
    // 현재 GPS 사용 여부
    boolean isGPSEnabled = false;

    Button info_next_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        permission_check();
        info_next_button = (Button)findViewById(R.id.info_next_button);
        info_next_button.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(turnGPSOn()) {
                    Intent i = new Intent(InfoActivity.this, SelectAPActivity.class);
                    startActivity(i);
                }
            }
        });
    }

    private boolean turnGPSOn() {
        String gps = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!(gps.matches(".*gps.*") && gps.matches(".*network.*"))) {
            // GPS OFF 일때 Dialog 표시
            AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
            gsDialog.setTitle("위치 서비스 설정");
            gsDialog.setMessage("위치 서비스 기능을 설정하셔야 정확한 위치 서비스가 가능합니다.\n위치 서비스 기능을 설정하시겠습니까?");
            // Dialog 뒤로가기 막기
            gsDialog.setCancelable(false);
            gsDialog.setPositiveButton("네", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // GPS설정 화면으로 이동
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
                    isGPSEnabled   = true;
                }
            })
                    .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(),"GPS를 켜고 다시 시도해 주시기 바랍니다.",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }).create().show();
            return false;
        } else {
            return true;
        }
    }

    //권한 사용 체크
    public void permission_check() {
        if (Build.VERSION.SDK_INT >= M) {
            // 권한이 없을 경우
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // 사용자가 임의로 권한을 취소시킨 경우
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                } else {
                    // 최초로 권한을 요청하는 경우 (첫 실행)
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
            } else {
                // 사용 권한이 모두 있을 경우
            }
        }
    }
}
