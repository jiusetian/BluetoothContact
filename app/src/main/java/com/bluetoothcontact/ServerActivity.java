package com.bluetoothcontact;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 服务器
 *
 */
public class ServerActivity extends Activity {
    private EditText et_msg;
    private BluetoothServer server;
    private List<String> msgs = new ArrayList<String>();
    private TimerTask task = new TimerTask() {

        @Override
        public void run() {
            synchronized (msgs) {
                msgs = server.getMsgs(); //获取从客户端发送过来的消息
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    msgAdapter.notifyDataSetChanged();
                }
            });
        }

    };
    private MyAdapter msgAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        et_msg = (EditText) findViewById(R.id.et_msg);
        server = new BluetoothServer(this);
        server.start(); //开启服务器

        ListView lv_msg = (ListView) findViewById(R.id.lv_msg);
        msgAdapter = new MyAdapter();
        lv_msg.setAdapter(msgAdapter);

        Timer timer = new Timer();
        timer.schedule(task, 0, 1000);
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
                tv = new TextView(ServerActivity.this);
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
        server.send(msg);
        synchronized (msgs) {
            msgs.add("服务器发送：" + msg);
        }
    }
}
