package com.nytuo.batremote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private ScheduledExecutorService scheduler;
    AtomicBoolean showInfo = new AtomicBoolean(false);

    private void onPowerOnButtonClicked() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String ip = sharedPreferences.getString("ip", "");
        String mac = sharedPreferences.getString("mac", "");
        if (ip.isEmpty() || mac.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No IP and/or MAC", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            byte[] macBytes = getMacBytes(mac);
            byte[] bytes = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xff;
            }
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }

            InetAddress address = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, 9);
            DatagramSocket socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
            Toast.makeText(getApplicationContext(), "Request send", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
            throw new RuntimeException(e);
        }
    }

    private void showInformation() {
        TextView textView = findViewById(R.id.connection_status);
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String ip = sharedPreferences.getString("ip", "");
        String mac = sharedPreferences.getString("mac", "");
        if (ip.isEmpty() || mac.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No IP and/or MAC", Toast.LENGTH_LONG).show();
            textView.setText("No IP and/or MAC");
        } else {
            textView.setText("IP: " + ip + "\nMAC: " + mac);
        }
    }

    private void onPowerOffButtonClicked() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String ip = sharedPreferences.getString("ip", "");
        String username = sharedPreferences.getString("username", "");
        String password = sharedPreferences.getString("password", "");
        String command = sharedPreferences.getString("command", "");
        if (ip.isEmpty() || username.isEmpty() || password.isEmpty() || command.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Insufficient information", Toast.LENGTH_LONG).show();
            return;
        }
        SSHManager sshManager = new SSHManager(ip, username, password);
        String result = sshManager.executeCommand(command);
        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
    }

    private void taskForce() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String ip = sharedPreferences.getString("ip", "");
        if (ip.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No IP", Toast.LENGTH_LONG).show();
            return;
        }
        scheduler = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            InetAddress address;
            try {
                address = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            ImageView imageView = findViewById(R.id.imageView);
            try {
                if (address.isReachable(5000)) {
                    imageView.setImageResource(R.drawable.baseline_circle_24);
                } else {
                    imageView.setImageResource(R.drawable.baseline_circle_24_red);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 10, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        taskForce();

        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> onPowerOnButtonClicked());

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(v -> onPowerOffButtonClicked());

        TextView textView = findViewById(R.id.connection_status);
        textView.setOnClickListener(v -> {
            if (showInfo.get()) {
                textView.setText("Connection status");
                showInfo.set(false);
            } else {
                showInformation();
                showInfo.set(true);
            }
        });

        ImageButton imageButton = findViewById(R.id.imageButton);
        imageButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        scheduler.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scheduler.isShutdown()) {
            taskForce();
        }
        if (showInfo.get()) {
            showInformation();
        }

    }

    private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            Toast.makeText(null, "Invalid MAC address.", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(null, "Invalid hex digit in MAC address.", Toast.LENGTH_LONG).show();
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scheduler.shutdown();
    }
}