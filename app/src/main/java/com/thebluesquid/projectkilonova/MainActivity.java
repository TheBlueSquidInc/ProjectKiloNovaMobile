package com.thebluesquid.projectkilonova;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static volatile TextView wlanAdrTextView;
    public static volatile TextView listeningPortData;
    public static volatile EditText destIpData;
    public static volatile EditText destPortData;
    public static volatile String destIp = null;
    public static volatile int destPort = 0;

    public static volatile int minBuffSize = 15000;

    public static int wifiAdrInt = 0;
    public static String wifiAdrString;
    public static WifiManager wifiMgr;
    public static WifiInfo wifiInfo;

    public static DataInputStream dIs;
    public static DataOutputStream dOs;


    Thread audioPlaybackThread;
    Socket tcpSocket;
    ServerSocket tcpServerSocket;
    public static volatile  DatagramSocket dgSkt;
    public static volatile DatagramPacket dgPkt;
    public static volatile AudioTrack aTrack;
    AudioRecord aRecord;
    byte[] byteSpeakerBuffer = new byte[3100];
    byte[] byteMicBuffer;
    public static volatile int numBytesRead = 0;

    public static volatile boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWlanAdr();
        getSockets();
        setListenPortInfo();

    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void playAudioStream(View view){

          destIpData = (EditText) findViewById(R.id.editTextDestIp);
        destPortData = (EditText) findViewById(R.id.editTextDestPort);
        destIp = destIpData.getText().toString();
        destPort =  Integer.parseInt(destPortData.getText().toString());
          initSoundAudio();

        buildInputAudioPacket();
        audioPlaybackThread = new Thread(){



            @Override
            public void run() {

          try{
            tcpSocket = new Socket(destIp, destPort);
              dIs = new DataInputStream(tcpSocket.getInputStream());
              dOs = new DataOutputStream(tcpSocket.getOutputStream());
              dOs.writeUTF("Start" + " " + dgSkt.getLocalPort());

        } catch (IOException e){
            e.printStackTrace();
        }

                isPlaying = true;
                while(isPlaying){
                    try {
                         dgSkt.receive(dgPkt);
                        aTrack.write(dgPkt.getData(), 0, dgPkt.getLength());
                    } catch(IOException e){
                        e.printStackTrace();
                    }

                }
            }
        };

        audioPlaybackThread.start();


    }



    public void getWlanAdr(){
        wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiInfo = wifiMgr.getConnectionInfo();
        wifiAdrInt = wifiInfo.getIpAddress();
        wifiAdrString = Formatter.formatIpAddress(wifiAdrInt);
        wlanAdrTextView = (TextView) findViewById(R.id.textView6);
        wlanAdrTextView.setText(wifiAdrString);
    }

    public void setListenPortInfo(){
        int testInt = 0;
        listeningPortData = (TextView) findViewById(R.id.textView5);
        listeningPortData.setText(Integer.toString(dgSkt.getLocalPort()));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void initMicAudio(){
        aRecord = new AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                        .setSampleRate(8000)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                .setBufferSizeInBytes(minBuffSize)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void initSoundAudio(){
    aTrack = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build())
            .setAudioFormat(new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                    .setSampleRate(8000)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build())
            .setBufferSizeInBytes(minBuffSize)
            .build();
        initAudioPorts();
    }

    public void initAudioPorts(){
        aTrack.play();
    }


    public void getSockets(){
        getTcpServerSocket();
        getUdpSocket();
    }


    public void getTcpServerSocket(){
        try{
            tcpServerSocket = new ServerSocket();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void getUdpSocket(){

        try{
            dgSkt = new DatagramSocket();
        } catch(SocketException e){
            e.printStackTrace();
        }

    }

    public void buildInputAudioPacket(){
        dgPkt = new DatagramPacket(byteSpeakerBuffer, 0, byteSpeakerBuffer.length);
    }


}