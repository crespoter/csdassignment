package com.iiits.multiroutecommunicator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Boolean end = false;
    private Button buttonStartReceiving;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonStartReceiving = (Button) findViewById(R.id.btn_start_receiving);
        Button buttonSend = (Button) findViewById(R.id.SendMessage);
        Button buttonBluetoothListen = (Button) findViewById(R.id.bluetoothListen);
        buttonSend.setOnClickListener(this);
        buttonStartReceiving.setOnClickListener(this);
        buttonBluetoothListen.setOnClickListener(this);
        try {
            initBluetooth();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void startServerSocket() {
        Thread thread = new Thread(new Runnable() {
            private String stringData = null;
            @Override
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(9002);
                    while (!end) {
                        Socket s = ss.accept();
                        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        PrintWriter output = new PrintWriter(s.getOutputStream());
                        stringData = input.readLine();
                        bluetoothSend(stringData);
                        output.flush();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("CRESPOTER", "run: " + stringData);
                        if (stringData.equalsIgnoreCase("STOP")) {
                            end = true;
                            output.close();
                            s.close();
                            break;
                        }
                        output.close();
                        s.close();
                    }
                    ss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_receiving:
                startServerSocket();
                buttonStartReceiving.setEnabled(false);
                break;
            case R.id.SendMessage:
                sendMessage(((EditText)findViewById(R.id.message)).getText().toString());
                break;
            case R.id.bluetoothListen:
                bluetoothRun();
                break;
        }
    }

    private void sendMessage(final String msg) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Socket s = new Socket(((EditText)findViewById(R.id.serverIp)).getText().toString(), 9002);
                    OutputStream out = s.getOutputStream();
                    PrintWriter output = new PrintWriter(out);
                    output.println(msg);
                    output.flush();
                    BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    final String st = input.readLine();
                    Log.d("CRESPOTER", "run: " + st);
                    output.close();
                    out.close();
                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
    private OutputStream outputStream;
    private InputStream inStream;
    private void initBluetooth() throws IOException {

        Toast.makeText(getApplicationContext(),"HERE",Toast.LENGTH_LONG);
        BluetoothAdapter blueAdapter = BluetoothAdapter.getDefaultAdapter();
        if (blueAdapter != null) {
            if (blueAdapter.isEnabled()) {
                Set<BluetoothDevice> bondedDevices = blueAdapter.getBondedDevices();

                if(bondedDevices.size() > 0) {
                    Object[] devices = (Object []) bondedDevices.toArray();
                    BluetoothDevice device = (BluetoothDevice) devices[0];
                    ParcelUuid[] uuids = device.getUuids();
                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                    socket.connect();
                    outputStream = socket.getOutputStream();
                    inStream = socket.getInputStream();
                }
                Log.e("error", "No appropriate paired devices.");
            } else {
                Log.e("error", "Bluetooth is disabled.");
            }
        }
    }
    public void bluetoothSend(String s) throws IOException {
        outputStream.write(s.getBytes());
    }

    public void bluetoothRun() {
        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytes = 0;
        int b = BUFFER_SIZE;

        while (true) {
            try {
                bytes = inStream.read(buffer, bytes, BUFFER_SIZE - bytes);
                TextView message = (TextView) findViewById(R.id.bluetoothMessage);
                message.setText(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

