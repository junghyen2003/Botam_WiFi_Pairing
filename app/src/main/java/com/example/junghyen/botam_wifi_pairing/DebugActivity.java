package com.example.junghyen.botam_wifi_pairing;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.junghyen.botam_wifi_pairing.Wifi.ConnectWifi;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;

public class DebugActivity extends AppCompatActivity {

    // 장비 ip & port
    String ipAddress;
    int port = 47000;

    // 이전 액티비티에서 가져온 정보
    String select_AP; // 장비 AP
    String select_Wifi; // 연결할 공유기 AP
    String select_Wifi_password; // 연결할 공유기 AP 비밀번호
    String server_url; // 연결될 서버 URL

    // UI
    TextView debug_textView;
    Button debug_next;

    // 버튼 조정 변수
    int debug_switch = 0;

    // header 및 dp 명령어들
    byte[] header = new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // length 필드 ( 0 )
            (byte)0x49, (byte)0x4E, (byte)0x43, (byte)0x54, // Magi Value 필드
            (byte)0x00, (byte)0x00, // Data Type 필드 ( 0000 )
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Reserved
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00};
    byte[] total_length = new byte[4]; // 명령의 길이를 변경하기 위한 total_length
    byte[] dp0100;
    byte[] dp0200;
    byte[] dp0300;

    // wifi 매니저
    WifiManager wm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        // 이전 액티비티에서 정보 가져오기
        Intent i = getIntent();
        select_AP = i.getCharSequenceExtra("select_AP").toString();
        select_Wifi = i.getCharSequenceExtra("select_Wifi").toString();
        select_Wifi_password = i.getCharSequenceExtra("select_Wifi_password").toString();
        server_url = i.getCharSequenceExtra("server_url").toString();

        // UI 셋팅
        debug_textView = (TextView) findViewById(R.id.debug_textView);
        debug_next = (Button)findViewById(R.id.debug_next);

        // 장비 AP에 연결(ex. KYOWON 1E1084)
        ConnectWifi connectWifi = new ConnectWifi();
        wm = (WifiManager) this.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiConfiguration wc = connectWifi.ConnectOpenCapabilites(select_AP);
        ConnectivityManager manager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiInfo wifiInfo = wm.getConnectionInfo();

        // 연결
        while (!wifiInfo.getSSID().substring(1,wifiInfo.getSSID().length()-1).equals(select_AP)){
            connectWifi.connect(wc, wm, select_AP);
            wifiInfo = wm.getConnectionInfo();
        }

        // 명령어 셋팅(dp0100, dp0200, dp0300)
        dp0100 = DP0100();
        dp0200 = DP0200();
        dp0300 = DP0300();

        // debug_next 버튼 동작
        debug_next.setOnClickListener(new Button.OnClickListener(){
                @Override
                public void onClick(View view) {
                   try {
                        switch (debug_switch){
                            case 0 : // dp 0100 동작 및 결과
                                DP0100_connect();
                                debug_next.setText("Next");
                                debug_switch++;
                                break;
                            case 1 : // dp 0200 동작 및 결과
                                DP0200_connect();
                                debug_switch++;
                                break;
                            case 2 : // dp 0300 동작 및 결과
                                DP0300_connect();
                                debug_next.setText("Stop");
                                debug_switch++;
                                break;
                            default :
                                break;
                        } // switch 문 종료
                                } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } // onClick 종료
            }
        ); // 버튼 동작 정의 종료
    }


    // ====================== 바이트 배열 & 스트링 관련 ===============================

    // int 값을 받아 LittelEndian 2byte 배열로 변환
    public static byte[] int_getLittleEndiaby2byte(int v){
        byte[] buf = new byte[2];
        buf[1] = (byte)((v >>> 8) & 0xFF);
        buf[0] = (byte)((v >>> 0) & 0xFF);
        return buf;
    }

    // int 값을 받아 LittelEndian 4byte 배열로 변환
    public static byte[] int_getLittleEndiaby4byte(int v){
        byte[] buf = new byte[4];
        buf[3] = (byte)((v >>> 24) & 0xFF);
        buf[2] = (byte)((v >>> 16) & 0xFF);
        buf[1] = (byte)((v >>> 8) & 0xFF);
        buf[0] = (byte)((v >>> 0) & 0xFF);
        return buf;
    }

    // String 변수를 받아 hex_byte 배열로 변환
    public byte[] convert_string_hex(String s){
        String plainText = s;
        byte[] byteArrayForPlain = plainText.getBytes();
        String hexString = "";
        for (byte b : byteArrayForPlain) {
            hexString += Integer.toString((b & 0xF0) >> 4, 16);
            hexString += Integer.toString(b & 0x0F, 16);
        }
        byte[] bytes = new BigInteger(hexString, 16).toByteArray();
        return bytes;
    }

    // 바이트를 헥사 스트링으로 출력하는 함수
    public String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for(final byte b: a)
            sb.append(String.format("0x%02x ", b&0xff));
        return sb.toString();
    }

    // 바이트 이어붙이기
    byte[] concat(byte[]...arrays)
    {
        // Determine the length of the result array
        int totalLength = 0;
        for (int i = 0; i < arrays.length; i++)
        {
            totalLength += arrays[i].length;
        }

        // create the result array
        byte[] result = new byte[totalLength];

        // copy the source arrays into the result array
        int currentIndex = 0;
        for (int i = 0; i < arrays.length; i++)
        {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
            currentIndex += arrays[i].length;
        }

        return result;
    }

    // inputstream 을 받아 바이트 배열로 출력
    public static byte[] inputStreamToByteArray(DataInputStream is) {

        byte[] resBytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int read = -1;
        try {
            while ( (read = is.read(buffer)) != -1 ) {
                bos.write(buffer, 0, read);
            }

            resBytes = bos.toByteArray();
            bos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return resBytes;
    }

    // =============================== DP 명령어 ==============================================

    // DP0100 명령어 생성
    public byte[] DP0100(){
        // dp0100 명령을 지정
        header[9] = (byte)0x01;

        // serial 넘버 지정 ( 추후 가변(?))
        byte[] serial_num = new byte[]{(byte)0x31,(byte)0x32,(byte)0x33,(byte)0x34,(byte)0x2D,(byte)0x61,(byte)0x62,(byte)0x63,(byte)0x64,(byte)0x00,
                                        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                                        (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                                        (byte)0x00,(byte)0x00}; // 1234-abcd
        // 최종적인 db0100 명령
        byte[] dp0100 = concat(header, serial_num);

        // dp0100 명령의 길이를 header에 업데이트
        total_length = int_getLittleEndiaby4byte(dp0100.length);
        dp0100[0] = total_length[0];
        dp0100[1] = total_length[1];
        dp0100[2] = total_length[2];
        dp0100[3] = total_length[3];

        return dp0100;
    }

    //DP0100 명령어 수행
    public void DP0100_connect() throws InterruptedException{
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                        try {
                            // 장비 address 가져오기(gateway : 192.168.43.1)
                            DhcpInfo dhcpInfo = wm.getDhcpInfo();
                            int serverIp = dhcpInfo.gateway;
                            ipAddress = String.format(
                                    "%d.%d.%d.%d",
                                    (serverIp & 0xff),
                                    (serverIp >> 8 & 0xff),
                                    (serverIp >> 16 & 0xff),
                                    (serverIp >> 24 & 0xff));

                            Socket sock = new Socket(ipAddress, port);
                            DataOutputStream dOut = new DataOutputStream(sock.getOutputStream());
                            DataInputStream in = new DataInputStream(sock.getInputStream());

                            dOut.write(dp0100);
                            dOut.flush();

                            // bp0100 명령의 출력
                            final byte[] in_byte = new byte[72];
                            in.read(in_byte);

                            runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    debug_textView.append(DP0100_string_delivery(dp0100));
                                    debug_textView.append(DP0101_string_request(in_byte));
                                }
                            });

                            in.close();
                            sock.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
            }});
        t.start();
        t.join();
    }

    // DP0100 명령어 분해 -> String으로 반환
    public String DP0100_string_delivery(byte[] dp0100){
        byte[] two_bytes = new byte[2];
        byte[] four_bytes = new byte[4];
        byte[] ten_bytes = new byte[10];
        byte[] copy = new byte[32];

        String result = "DP0100 : Device Information Delivery (APP -> WF5000)\n";
        // DP0100 전체길이 -> 4bytes
        System.arraycopy(dp0100, 0, four_bytes, 0, 4);
        result = result + "Length : " + byteArrayToHex(four_bytes) + "\n";
        // DP0100 Magic Value -> 4bytes
        System.arraycopy(dp0100, 4, four_bytes, 0, 4);
        result = result + "Magic Value : " + byteArrayToHex(four_bytes) + "\n";
        // DP0100 명령어 -> 2bytes
        System.arraycopy(dp0100, 8, two_bytes, 0, 2);
        result = result + "Data Type : "+ byteArrayToHex(two_bytes) + "\n";
        // DP0100 Reserved -> 10bytes
        System.arraycopy(dp0100, 10, ten_bytes, 0, 10);
        result = result + "Reserved : " + byteArrayToHex(ten_bytes) + "\n";
        // Serial num -> 32bytes
        System.arraycopy(dp0100, 20, copy, 0, 32);
        result = result + "Serial number : "+ byteArrayToHex(copy) + "\n\n";

        return result;
    }

    // DP0101 명령어 분해 -> String으로 반환
    public String DP0101_string_request(byte[] in_byte){
        byte[] two_bytes = new byte[2];
        byte[] four_bytes = new byte[4];
        byte[] sixty_bytes = new byte[16];
        byte[] copy = new byte[32];

        String result = "DP0101 : Device Information Response (WF5000 -> APP)\n";
        // DP0101 전체 길이 -> 4bytes
        System.arraycopy(in_byte, 0, four_bytes, 0, 4);
        result = result + "Length : " + byteArrayToHex(four_bytes) + "\n";
        // DP0101 Magic Value -> 4bytes
        System.arraycopy(in_byte, 4, four_bytes, 0, 4);
        result = result + "Magic Value : " + byteArrayToHex(four_bytes) + "\n";
        // DP0101 명령어 -> 2bytes
        System.arraycopy(in_byte, 8, two_bytes, 0, 2);
        result = result + "Data Type : "+ byteArrayToHex(two_bytes) + "\n";
        // DP0100 Reserved -> 10bytes
        System.arraycopy(in_byte, 10, copy, 0, 10);
        result = result + "Reserved : " + byteArrayToHex(copy) + "\n";
        // DP0101 Device Code -> 2bytes
        System.arraycopy(in_byte, 20, two_bytes, 0, 2);
        result = result + "Device Code : " + byteArrayToHex(two_bytes) + "\n";
        // DP0101 Device Type -> 2bytes
        System.arraycopy(in_byte, 22, two_bytes, 0, 2);
        result = result + "Device Type : " + byteArrayToHex(two_bytes) + "\n";
        // DP0101 Device Name -> 16bytes
        System.arraycopy(in_byte, 24, sixty_bytes, 0, 16);
        result = result + "Device Name : " + byteArrayToHex(sixty_bytes) + "\n";
        // DP0101 Model Name -> 32bytes
        System.arraycopy(in_byte, 40, copy, 0, 32);
        result = result + "Model Name : " + byteArrayToHex(copy) + "\n\n";

        return result;
    }

    // DP0200 명령어 생성
    public byte[] DP0200(){
        // dp0200 명령을 지정
        header[9] = (byte)0x02;

        // uri 갯수 = 현재는 1개
        byte[] uri_count = new byte[]{(byte)0x01, (byte)0x00};
        // uri의 길이
        byte[] uri_length = new byte[2];
        uri_length = int_getLittleEndiaby2byte(server_url.length());
        // uri
        byte [] uri = convert_string_hex(server_url);

        // 최종적인 dp0200 명령
        byte[] dp0200 = concat(header, uri_count, uri_length, uri);

        // dp0200 명령의 길이를 header에 업데이트
        total_length = int_getLittleEndiaby4byte(dp0200.length);
        dp0200[0] = total_length[0];
        dp0200[1] = total_length[1];
        dp0200[2] = total_length[2];
        dp0200[3] = total_length[3];

        return dp0200;
    }

    // DP0200 명령어 수행
    public void DP0200_connect() throws InterruptedException{
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 장비 address 가져오기(gateway : 192.168.43.1)
                    DhcpInfo dhcpInfo = wm.getDhcpInfo();
                    int serverIp = dhcpInfo.gateway;
                    ipAddress = String.format(
                            "%d.%d.%d.%d",
                            (serverIp & 0xff),
                            (serverIp >> 8 & 0xff),
                            (serverIp >> 16 & 0xff),
                            (serverIp >> 24 & 0xff));

                    Socket sock = new Socket(ipAddress, port);
                    DataOutputStream dOut = new DataOutputStream(sock.getOutputStream());
                    DataInputStream in = new DataInputStream(sock.getInputStream());

                    dOut.write(dp0200);
                    dOut.flush();

                    // bp0200 명령의 출력
                    final byte[] in_byte = new byte[21];
                    in.read(in_byte);

                    runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            debug_textView.append(DP0200_string_delivery(dp0200));
                            debug_textView.append(DP0201_string_request(in_byte));
                        }
                    });

                    in.close();
                    sock.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        t.join();
    }

    // DP0200 명령어 분해 -> String으로 반환
    public String DP0200_string_delivery(byte[] dp0200){
        byte[] two_bytes = new byte[2];
        byte[] four_bytes = new byte[4];
        byte[] ten_bytes = new byte[10];
        byte[] copy = new byte[server_url.length()];

        String result = "DP0200 : Device Information Delivery (APP -> WF5000)\n";
        // DP0200 전체길이 -> 4bytes
        System.arraycopy(dp0200, 0, four_bytes, 0, 4);
        result = result + "Length : " + byteArrayToHex(four_bytes) + "\n";
        // DP0200 Magic Value -> 4bytes
        System.arraycopy(dp0200, 4, four_bytes, 0, 4);
        result = result + "Magic Value : " + byteArrayToHex(four_bytes) + "\n";
        // DP0200 명령어 -> 2bytes
        System.arraycopy(dp0200, 8, two_bytes, 0, 2);
        result = result + "Data Type : "+ byteArrayToHex(two_bytes) + "\n";
        // DP0200 Reserved -> 10bytes
        System.arraycopy(dp0200, 10, ten_bytes, 0, 10);
        result = result + "Reserved : " + byteArrayToHex(ten_bytes) + "\n";
        // DP0200 URL count -> 2bytes
        System.arraycopy(dp0200, 20, two_bytes, 0, 2);
        result = result + "URL count : "+ byteArrayToHex(two_bytes) + "\n";
        // DP0200 URL length -> 2bytes
        System.arraycopy(dp0200, 22, two_bytes, 0, 2);
        result = result + "URL length : "+ byteArrayToHex(two_bytes) + "\n";
        // DP0200 URL
        System.arraycopy(dp0200, 24, copy, 0, server_url.length());
        result = result + "URL : " + byteArrayToHex(copy) + "\n\n";

        return result;
    }

    // DP0201 명령어 분해 -> String으로 반환
    public String DP0201_string_request(byte[] in_byte){
        byte[] two_bytes = new byte[2];
        byte[] four_bytes = new byte[4];
        byte[] ten_bytes = new byte[10];
        byte[] copy = new byte[1];

        String result = "DP0201 : Device Information Response (WF5000 -> APP)\n";
        // DP0201 전체 길이 -> 4bytes
        System.arraycopy(in_byte, 0, four_bytes, 0, 4);
        result = result + "Length : " + byteArrayToHex(four_bytes) + "\n";
        // DP0201 Magic Value -> 4bytes
        System.arraycopy(in_byte, 4, four_bytes, 0, 4);
        result = result + "Magic Value : " + byteArrayToHex(four_bytes) + "\n";
        // DP0201 명령어 -> 2bytes
        System.arraycopy(in_byte, 8, two_bytes, 0, 2);
        result = result + "Data Type : "+ byteArrayToHex(two_bytes) + "\n";
        // DP0201 Reserved -> 10bytes
        System.arraycopy(in_byte, 10, ten_bytes, 0, 10);
        result = result + "Reserved : " + byteArrayToHex(ten_bytes) + "\n";
        // DP0201 Result Code Success -> 1bytes
        System.arraycopy(in_byte, 20, copy, 0, 1);
        result = result + "Result Code Success : " + byteArrayToHex(copy) + "\n\n";

        return result;
    }

    // DP0300 명령어 생성
    public byte[] DP0300(){
        // dp0300 명령을 지정
        header[9] = (byte)0x03;
        // SSID 길이
        byte[] ssid_length = new byte[2];
        ssid_length = int_getLittleEndiaby2byte(select_Wifi.length());

        // SSID 이름
        byte[] ssid_name = convert_string_hex(select_Wifi);

        // 공유기 비밀번호 길이
        byte[] passwd_length = new byte[2];
        passwd_length = int_getLittleEndiaby2byte(select_Wifi_password.length());

        // 공유기 비밀번호
        byte[] passwd = convert_string_hex(select_Wifi_password);

        // WEP Key index - 2byte - 0으로 설정
        byte[] wep_key_index = new byte[]{(byte)0x00, (byte)0x00};
        // 공유기 mac-address - 6byte - 0으로 설정
        byte[] mac = new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};

        // 최종적인 dp0300 명령
        byte[] dp0300 = concat(header, ssid_length, ssid_name, passwd_length, passwd, wep_key_index, mac);

        // dp0200 명령의 길이를 header에 업데이트
        total_length = int_getLittleEndiaby4byte(dp0300.length);
        dp0300[0] = total_length[0];
        dp0300[1] = total_length[1];
        dp0300[2] = total_length[2];
        dp0300[3] = total_length[3];

        return dp0300;
    }

    // DP0300 명령어 수행
    public void DP0300_connect() throws InterruptedException{
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 장비 address 가져오기(gateway : 192.168.43.1)
                    DhcpInfo dhcpInfo = wm.getDhcpInfo();
                    int serverIp = dhcpInfo.gateway;
                    ipAddress = String.format(
                            "%d.%d.%d.%d",
                            (serverIp & 0xff),
                            (serverIp >> 8 & 0xff),
                            (serverIp >> 16 & 0xff),
                            (serverIp >> 24 & 0xff));

                    Socket sock = new Socket(ipAddress, port);
                    DataOutputStream dOut = new DataOutputStream(sock.getOutputStream());
                    DataInputStream in = new DataInputStream(sock.getInputStream());

                    dOut.write(dp0300);
                    dOut.flush();

                    // bp0300 명령의 출력
                    final byte[] in_byte = new byte[21];
                    in.read(in_byte);

                    runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            debug_textView.append(DP0300_string_delivery(dp0300));
                            debug_textView.append(DP0301_string_request(in_byte));
                        }
                    });

                    in.close();
                    sock.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        t.join();
    }

    // DP0300 명령어 분해 -> String으로 반환
    public String DP0300_string_delivery(byte[] dp0300){
        byte[] two_bytes = new byte[2];
        byte[] four_bytes = new byte[4];
        byte[] ten_bytes = new byte[10];
        byte[] SSID_bytes = new byte[select_Wifi.length()];
        byte[] SSID_password_bytes = new byte[select_Wifi_password.length()];
        byte[] six_bytes = new byte[6];

        String result = "DP0300 : Device Information Delivery (APP -> WF5000)\n";
        // DP0300 전체길이 -> 4bytes
        System.arraycopy(dp0300, 0, four_bytes, 0, 4);
        result = result + "Length : " + byteArrayToHex(four_bytes) + "\n";
        // DP0300 Magic Value -> 4bytes
        System.arraycopy(dp0300, 4, four_bytes, 0, 4);
        result = result + "Magic Value : " + byteArrayToHex(four_bytes) + "\n";
        // DP0300 명령어 -> 2bytes
        System.arraycopy(dp0300, 8, two_bytes, 0, 2);
        result = result + "Data Type : "+ byteArrayToHex(two_bytes) + "\n";
        // DP0300 Reserved -> 10bytes
        System.arraycopy(dp0300, 10, ten_bytes, 0, 10);
        result = result + "Reserved : " + byteArrayToHex(ten_bytes) + "\n";
        // DP0300 SSID Length -> 2bytes
        System.arraycopy(dp0300, 20, two_bytes, 0, 2);
        result = result + "SSID Length : " + byteArrayToHex(two_bytes) + "\n";
        // DP0300 SSID -> SSID.length()bytes
        System.arraycopy(dp0300, 22, SSID_bytes, 0, select_Wifi.length());
        result = result + "SSID : " + byteArrayToHex(SSID_bytes) + "\n";
        // DP0300 Password Length -> 2bytes
        System.arraycopy(dp0300, 22 + select_Wifi.length(), two_bytes, 0, 2);
        result = result + "SSID Password length : " + byteArrayToHex(two_bytes) + "\n";
        // DP0300 SSID Password -> SSID Password.length()bytes
        System.arraycopy(dp0300, 24 + select_Wifi.length(), SSID_password_bytes, 0, select_Wifi_password.length());
        result = result + "SSID Password : " + byteArrayToHex(SSID_password_bytes) + "\n";
        // DP0300 WEP key index -> 2bytes
        System.arraycopy(dp0300, 24 + select_Wifi.length() + select_Wifi_password.length(), two_bytes, 0, 2);
        result = result + "WEP key index : " + byteArrayToHex(two_bytes) + "\n";
        // DP0300 BSSID -> 6bytes
        System.arraycopy(dp0300, 26 + select_Wifi.length() + select_Wifi_password.length(), six_bytes, 0, 6);
        result = result + "BSSID : " + byteArrayToHex(six_bytes) + "\n\n";

        return result;
    }

    // DP0301 명령어 분해 -> String으로 반환
    public String DP0301_string_request(byte[] in_byte){
        byte[] two_bytes = new byte[2];
        byte[] four_bytes = new byte[4];
        byte[] ten_bytes = new byte[10];
        byte[] copy = new byte[1];

        String result = "DP0301 : Device Information Response (WF5000 -> APP)\n";
        // DP0301 전체 길이 -> 4bytes
        System.arraycopy(in_byte, 0, four_bytes, 0, 4);
        result = result + "Length : " + byteArrayToHex(four_bytes) + "\n";
        // DP0301 Magic Value -> 4bytes
        System.arraycopy(in_byte, 4, four_bytes, 0, 4);
        result = result + "Magic Value : " + byteArrayToHex(four_bytes) + "\n";
        // DP0301 명령어 -> 2bytes
        System.arraycopy(in_byte, 8, two_bytes, 0, 2);
        result = result + "Data Type : "+ byteArrayToHex(two_bytes) + "\n";
        // DP0301 Reserved -> 10bytes
        System.arraycopy(in_byte, 10, ten_bytes, 0, 10);
        result = result + "Reserved : " + byteArrayToHex(ten_bytes) + "\n";
        // DP0301 Result Code Success -> 1bytesZ
        System.arraycopy(in_byte, 20, copy, 0, 1);
        result = result + "Result Code Success : " + byteArrayToHex(copy) + "\n\n";

        return result;
    }
}
