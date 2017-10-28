package com.bluetoothcontact;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 客户端
 */
public class ClientActivity extends Activity {
    /**
     * 被发现的设备
     */
    private List<BluetoothDevice> discoverDevices = new ArrayList<BluetoothDevice>();
    /**
     * 蓝牙客户端
     */
    private BluetoothClient bluetoothClient;
    /**
     * tag
     */
    public final String TAG = "ClientActivity";
    /**
     * 搜索对话框
     */
    private AlertDialog dlgSearch;
    /**
     * adapter
     */
    private BaseAdapter adapter;
    private EditText et_msg;
    private List<String> msgs = new ArrayList<String>();
    private TimerTask task = new TimerTask() {

        @Override
        public void run() {
            synchronized (msgs) {
                msgs = bluetoothClient.getMsgs();
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    msgAdapter.notifyDataSetChanged();
                }
            });
        }

    };
    private MyAdapter msgAdapter;
    /**
     * 设备搜索广播
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    Log.d("tag", "发现设备");
                    // 发现设备，添加到列表，刷新列表
                    discoverDevices.add((BluetoothDevice) intent
                            .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    // 开始搜索
                    Log.i(TAG, "开始搜索设备");
                    discoverDevices.clear();
                    // 弹出对话框
                    if (dlgSearch == null) {
                        dlgSearch = new AlertDialog.Builder(ClientActivity.this)
                                .create();
                        // 自定义对话框
                        View view = LayoutInflater.from(ClientActivity.this)
                                .inflate(R.layout.dialog_search, null);
                        ListView lv_devices = (ListView) view
                                .findViewById(R.id.lv_devices);
                        adapter = new DevicesAdapter(ClientActivity.this);
                        lv_devices.setAdapter(adapter);
                        lv_devices
                                .setOnItemClickListener(new OnItemClickListener() {

                                    @Override
                                    public void onItemClick(AdapterView<?> parent,
                                                            View view, int position, long id) {
                                        // 项点击时，进行连接
                                        BluetoothDevice device = (BluetoothDevice) view
                                                .getTag();
                                        bluetoothClient.connect(device);
                                        dlgSearch.dismiss();
                                        dlgSearch = null;

                                    }
                                });
                        dlgSearch.setView(view);
                        dlgSearch.setCancelable(true);// 可以按back键取消
                        dlgSearch.setCanceledOnTouchOutside(false);// 不可以按空白地方取消
                    }
                    dlgSearch.show();
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    // 结束搜索
                    Log.i(TAG, "结束搜索设备");
                    break;

                default:
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        bluetoothClient = new BluetoothClient();

        et_msg = (EditText) findViewById(R.id.et_msg);

        ListView lv_msg = (ListView) findViewById(R.id.lv_msg);
        msgAdapter = new MyAdapter();
        lv_msg.setAdapter(msgAdapter);

        Timer timer = new Timer();
        timer.schedule(task, 0, 1000);

        // 搜索蓝牙设备
        bluetoothClient.start();
    }

    public class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return msgs.size();
        }

        @Override
        public Object getItem(int position) {
            return msgs.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv = null;
            if (convertView != null) {
                tv = (TextView) convertView;
            } else {
                tv = new TextView(ClientActivity.this);
                AbsListView.LayoutParams params = new AbsListView.LayoutParams(
                        AbsListView.LayoutParams.MATCH_PARENT,
                        AbsListView.LayoutParams.WRAP_CONTENT);
                tv.setLayoutParams(params);
            }
            tv.setTag(msgs.get(position));
            tv.setText(msgs.get(position));
            return tv;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    //注册广播监听蓝牙搜索情况
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //开始搜索
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //结束搜索
        filter.addAction(BluetoothDevice.ACTION_FOUND); //找到设备
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * 设备adapter
     *
     * @author c
     */
    private class DevicesAdapter extends BaseAdapter {

        private Context context;

        public DevicesAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return discoverDevices.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return discoverDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent) {
            TextView tv = null;
            if (convertView != null) {
                tv = (TextView) convertView;
            } else {
                tv = new TextView(context);
                AbsListView.LayoutParams params = new AbsListView.LayoutParams(
                        AbsListView.LayoutParams.MATCH_PARENT,
                        AbsListView.LayoutParams.WRAP_CONTENT);
                tv.setLayoutParams(params);
            }
            tv.setTag(discoverDevices.get(position)); //进行添加附属
            tv.setText(discoverDevices.get(position).getName());
            tv.setFocusable(false);
            tv.setFocusableInTouchMode(false);
            return tv;
        }

    }

    /**
     * 发送
     *
     * @param view
     */
    public void btn_send(View view) {
        String msg = et_msg.getText().toString().trim();
        et_msg.setText("");
        send(msg);
    }

    /**
     * 发送消息到客户端
     *
     * @param msg
     */
    private void send(String msg) {
        bluetoothClient.send(msg);
        synchronized (msgs) {
            msgs.add("客户端发送：" + msg);
        }
    }
}
