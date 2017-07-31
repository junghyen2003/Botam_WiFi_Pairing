package com.example.junghyen.botam_wifi_pairing.Wifi;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.util.List;

import static android.content.ContentValues.TAG;

public class ConnectWifi {

    // 암호 필요 없을경우
    public static WifiConfiguration ConnectOpenCapabilites(String ssid ) {
        WifiConfiguration wfc = new WifiConfiguration();
        wfc.SSID = "\"".concat(ssid).concat("\"");
        wfc.priority = 40;

        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wfc.allowedAuthAlgorithms.clear();
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        //connect(wfc);
        return wfc;
    }
    /**
     * WEP  방식 일 때 설정

     */
    public static WifiConfiguration ConnectWEP( String ssid ) {
        WifiConfiguration wfc = new WifiConfiguration();
        wfc.SSID = "\"".concat(ssid).concat("\"");
        wfc.priority = 40;

        String password = "123456789";

        wfc.status = WifiConfiguration.Status.DISABLED;
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);

        int length = password.length();
        if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
            wfc.wepKeys[0] = password;
        } else {
            wfc.wepKeys[0] = '"' + password + '"';
        }
        //connect(wfc);
        return wfc;
    }
    /**
     * WPA, WPA2 방식 일 때 설정

     */
    public static WifiConfiguration ConnectWPA( String ssid ) {
        // 공통 부분
        WifiConfiguration wfc = new WifiConfiguration();
        wfc.SSID = "\"".concat(ssid).concat("\"");
        wfc.priority = 40;

        wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);

        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        String password = "123456789";
        wfc.preSharedKey = "\"".concat(password).concat("\"");

        //connect(wfc);
        return wfc;
    }
    /**
     * 원하는 네트워크 아이디에 AP 에 연결
     */
    public static void connect(WifiConfiguration wfc, WifiManager wifi, String ssid){
        boolean isId = false;
        int networkID = 0;
        int tempID = 0;
        String tempSSID;

        List<WifiConfiguration> wifiConfigurationList;
        wifiConfigurationList = wifi.getConfiguredNetworks();
        for(WifiConfiguration w : wifiConfigurationList){
            if(w.SSID.equals("\""+ssid+"\"")){
                isId = true;
                tempID = w.networkId;
                tempSSID = w.SSID;

                break;
            } else {
                //Log.e("check", "else : id = "+w.SSID);
            }
        }
        if (isId == true) {
            networkID = tempID;
        } else {
            networkID = wifi.addNetwork(wfc);
        }
        boolean bEnableNetwork = wifi.enableNetwork(networkID, true);
        if (bEnableNetwork) {
            Log.d(TAG, "Connected!");
        } else {
            Log.d(TAG, "Disconnected!");
        }
    }
}
