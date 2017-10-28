package com.bluetoothcontact;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 蓝牙服务器
 */
public class BluetoothClient {
    private static final String TAG = "BluetoothClient";
    /**
     * 消息集合
     */
    private List<String> listMsg = new ArrayList<String>();
    /**
     * 是否工作中
     */
    private boolean isWorking = false;
    /**
     * spp well-known UUID
     */
    public final UUID uuid = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    /**
     * 客户端socket
     */
    private BluetoothSocket mClientSocket;

    public BluetoothClient() {

    }

    /**
     * 开启服务器
     */
    public void start() {
        startDiscovery();
    }

    /**
     * 开始检查设备
     */
    private void startDiscovery() {
        if (!BluetoothUtils.checkBluetoothExists()) {
            throw new RuntimeException("bluetooth module not exists.");
        }
        // 打开设备
        if (!BluetoothUtils.openBluetoothDevice()) { //如果没有打开蓝牙，return
            return;
        }
        // 开始扫描设备
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        defaultAdapter.startDiscovery();
    }

    OutputStream outputStream;
    private InputStream inputStream;

    /**
     * 停止
     */
    public void stop() {
        isWorking = false;
        if (mClientSocket != null) {
            try {
                mClientSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                mClientSocket = null;
            }
        }
    }

    /**
     * 客户端socket工作类
     *
     * @author weizh
     */
    private class ClientWorkingThread extends Thread {

        public ClientWorkingThread() {
        }

        @SuppressLint("NewApi")
        @Override
        public void run() {
            try {
                // 从输入流中取出数据，插入消息条中
                byte[] buffer = new byte[1024];
                while (isWorking) {

//                    StringBuffer sb = new StringBuffer();
//                    int len = 0;
//                    while ((len = inputStream.read(buffer)) != -1) {
//                        sb.append(new String(buffer, 0, len, "utf-8"));
//                    }
//
//                    Log.i(TAG, "客户端收到：" + sb.toString());
//                    synchronized (listMsg) {
//                        listMsg.add("服务器发送：" + sb.toString());
//                    }

                    int read = inputStream.read(buffer);
                    if (read != -1) { //返回从inputstream读取的字节数，如果返回来了-1，代表数据已经读取完，
                        // 有内容
                        // 判断是否取得的消息填充满了buffer，未到字符串结尾符；如果不是，证明读取到了一条信息，并且信息是完整的，这个完整的前提是不能粘包，不粘包可以使用flush进行处理。
                        StringBuilder sb = new StringBuilder();
                        if (read < buffer.length) {
                            String msg = new String(buffer, 0, read);
                            sb.append(msg);
                        } else {
                            byte[] tempBytes = new byte[1024 * 4];
                            while (read == buffer.length
                                    && buffer[read - 1] != 0x7f) {
                                read = inputStream.read(buffer);
                            }
                            String msg = new String(buffer, 0, read);
                            sb.append(msg);
                        }
                        Log.i(TAG, "客户端收到：" + sb.toString());
                        synchronized (listMsg) {
                            listMsg.add("服务器发送：" + sb.toString());
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // 工作完毕，关闭socket
            try {
                mClientSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    /**
     * 返回listMsg
     *
     * @return
     */
    public List<String> getMsgs() {
        synchronized (listMsg) {
            return listMsg;
        }
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    public void send(final String msg) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (mClientSocket != null) {
                    try {
                        if (outputStream != null) {
                            byte[] bytes = msg.getBytes(); //以字节流的形式发送
                            outputStream.write(bytes);
                            outputStream.flush();
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    /**
     * 进行连接
     *
     * @param device
     */
    @SuppressLint("NewApi")
    public void connect(final BluetoothDevice device) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    mClientSocket = device
                            .createRfcommSocketToServiceRecord(BluetoothServer.MY_UUID);
                    mClientSocket.connect();
                    isWorking = true; //代表正在工作
                    outputStream = mClientSocket.getOutputStream();
                    inputStream = mClientSocket.getInputStream();
                    new ClientWorkingThread().start(); //

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Log.i(TAG, "连接失败");
                    try {
                        Log.e("", "trying fallback...");
                        mClientSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                        mClientSocket.connect();
                        Log.e("", "Connected");
                        outputStream = mClientSocket.getOutputStream();
                        inputStream = mClientSocket.getInputStream();
                        new ClientWorkingThread().start();

                    } catch (Exception e2) {
                        Log.e("", "Couldn't establish Bluetooth connection!");
                    }
                }
            }
        }).start();
    }

}
