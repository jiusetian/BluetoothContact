package com.bluetoothcontact;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 蓝牙服务器
 */
public class BluetoothServer {
    /**
     * 消息集合
     */
    private List<String> listMsg = new ArrayList<String>();
    /**
     * 是否工作中
     */
    private boolean isWorking = false;
    /**
     * bluetooth name
     */
    private String name = "FIUBluetoothServer";
    /**
     * spp well-known UUID
     */
    public static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB"); //00001101-0000-1000-8000-00805F9B34FB
    private static final String TAG = "BluetoothServer";
    /**
     * 蓝牙服务器socket
     */
    private BluetoothServerSocket bluetoothServerSocket;
    /**
     * 客户端socket
     */
    private BluetoothSocket mClientSocket;

    Context context;

    public BluetoothServer(Context context) {
        this.context = context;
    }

    /**
     * 开启服务器
     */
    public void start() {
        listen();
    }

    /**
     * 开始监听
     */
    private void listen() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                // 判断是否有蓝牙设备
                if (!BluetoothUtils.checkBluetoothExists()) {
                    throw new RuntimeException("bluetooth module not exists.");
                }
                // 打开设备
                if (!BluetoothUtils.openBluetoothDevice()) {
                    return;
                }
                try {
                    if (bluetoothServerSocket == null) {
                        bluetoothServerSocket = BluetoothAdapter
                                .getDefaultAdapter()
                                .listenUsingRfcommWithServiceRecord(name,
                                        MY_UUID);
                    }
                    isWorking = true;
                    while (isWorking) {
                        //获取一个客户端的蓝牙socket
                        mClientSocket = bluetoothServerSocket.accept();
                        Log.i(TAG, "客户端已连接："
                                + mClientSocket.getRemoteDevice().getName());
                        myHandler.sendEmptyMessage(0x01);
                        new ClientWorkingThread(mClientSocket).start(); //开启一个线程接收某个客户端的流
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Log.d("tag", "服务器蓝牙失败");
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Toast.makeText(context,
                    "客户端已连接：" + mClientSocket.getRemoteDevice().getName(), Toast.LENGTH_SHORT)
                    .show();
        }

    };

    /**
     * 停止
     */
    public void stop() {
        isWorking = false;
        if (bluetoothServerSocket != null) {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                bluetoothServerSocket = null;
            }
        }
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
        /**
         * 客户端socket
         */
        private BluetoothSocket mClientSocket;

        public ClientWorkingThread(BluetoothSocket clientSocket) {
            this.mClientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                InputStream inputStream = mClientSocket.getInputStream();// 输入流
                // 从输入流中取出数据，插入消息条中
                byte[] buffer = new byte[1024];
                while (isWorking) {

//                    StringBuffer sb = new StringBuffer();
//                    int len = 0;
//                    while ((len = inputStream.read(buffer)) != -1) {
//                        sb.append(new String(buffer, 0, len, "utf-8"));
//                    }
//
//                    Log.i(TAG, "服务器收到：" + sb.toString());
//                    synchronized (listMsg) {
//                        listMsg.add("客户端发送：" + sb.toString());
//                    }

                    int read = inputStream.read(buffer);
                    if (read != -1) {
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
                        Log.i(TAG, "服务器收到：" + sb.toString());
                        synchronized (listMsg) {
                            listMsg.add("客户端发送：" + sb.toString());
                        }
                    }

                    // try {
                    // Thread.sleep(300);
                    // } catch (InterruptedException e) {
                    // // TODO Auto-generated catch block
                    // e.printStackTrace();
                    // }
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
    public void send(String msg) {
        if (mClientSocket != null) {
            try {
                mClientSocket.getOutputStream().write(msg.getBytes());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                mClientSocket.getOutputStream().flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
