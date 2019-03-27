package by.umdom.bluetoothservice.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import by.umdom.bluetoothservice.MainActivity;


public class BluetoothService extends Service {
    final String LOG_TAG = "myLogs";

    //Notification2
    private NotificationManagerCompat notificationManager;

    //TCP/IP Server
    final Handler handler = new Handler();
    private boolean end = false;

    //---Bluetooth---
    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter bluetoothAdapter;
    private UUID myUUID;
    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;
    private StringBuilder sb = new StringBuilder();
    private BluetoothSocket btSocket = null;
    final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
    //private static String address = "00:21:13:04:96:D0";
    //private static String address = "98:D3:32:31:59:C6";
    //public static String address = "00:21:13:04:97:D8";
    public static String address;
    String sbprint;

    String[] sbprintArrayStr; //получение данных на планшет по bluetooth
    //---End Bluetooth---

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");

        //---Bluetooth---
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, "BLUETOOTH NOT support", Toast.LENGTH_LONG).show();
            return;
        }
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();
            return;
        }
        //---End Bluetooth---

    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "MyServiceBluetooth onStartCommand, name = " + intent.getStringExtra("name"));

        //---Bluetooth---
        //address = "00:21:13:04:97:D8";
        address = MainActivity.dataBluetooth();
        Log.d(LOG_TAG, "------------------------------------------------ " + address);
        setup();
        //---End Bluetooth---
        return Service.START_STICKY;
    }

    public static void start(Context context) {

    }

    private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth
        private BluetoothSocket bluetoothSocket = null;

        private ThreadConnectBTdevice(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() { // Коннект
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {

                    e1.printStackTrace();
                }
            }
            if (success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных
                myThreadConnected = new ThreadConnected(bluetoothSocket);
                myThreadConnected.start(); // запуск потока приёма и отправки данных
            }
        }

        public void cancel() {
            Toast.makeText(getApplicationContext(), "Close - BluetoothSocket", Toast.LENGTH_LONG).show();
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Создание списка сопряжённых Bluetooth-устройств
                BluetoothDevice device2 = bluetoothAdapter.getRemoteDevice(address);
                myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
                myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
            }
        }).start();
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    private class ThreadConnected extends Thread {    // Поток - приём и отправка данных
        private final BluetoothSocket copyBtSocket;
        private final InputStream connectedInputStream; //приём
        private final OutputStream connectedOutputStream; //отправка

        public ThreadConnected(BluetoothSocket socket) {
            copyBtSocket = socket;
            InputStream in = null;
            OutputStream out = null;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() { // Приём данных
            while (true) {
                try {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, bytes);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки
                    if (endOfLineIndex > 0) {
                        sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());
                        Log.d(LOG_TAG, "***MyServiceBluetooth: " + sbprint + "***");

                        sbprintArrayStr = sbprint.split(",");

                        // принимаем данные с условием
//                      if(sbprintArrayStr[17].equals("F")) {
//
//                      }





                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
        Log.e(LOG_TAG, "***myThreadConnectBTdevice ЗАКРЫТ ");
    }

//        отправляем данные в Bluetooth
//        if (myThreadConnected != null) {
//        byte[] bytesToSend = "F".getBytes();
//        myThreadConnected.write(bytesToSend);}

}
