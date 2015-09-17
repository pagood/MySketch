package com.ms.xiaoyu.mysketch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.ms.xiaoyu.bean.Sketch;
import com.ms.xiaoyu.util.JsonHelper;
import com.ms.xiaoyu.view.Pad;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class PadActivity extends Activity {
    private Pad pad;
    private Button clear,eraser,upload;
    private ProgressDialog dialog;
    private double longtitude;
    private double latitude;
    private LocationClient locationClient;
    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == 0x11){
				locationClient.stop();
				Toast.makeText(getApplicationContext(), "上传成功(*´∇｀*)",
					     Toast.LENGTH_LONG).show();

            }
            else{
				locationClient.stop();
				Toast.makeText(getApplicationContext(), "上传失败,,Ծ‸Ծ,,",
					     Toast.LENGTH_LONG).show();


            }

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pad);
        init();
    }

    private void init() {
        // TODO Auto-generated method stub
        locationClient = new LocationClient(this);


        ///
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");       //set type
        locationClient.setLocOption(option);
        locationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (bdLocation == null) return;
                longtitude = bdLocation.getLongitude();
                latitude = bdLocation.getLatitude();
            }
        });
        locationClient.start();


        pad = (Pad)findViewById(R.id.pad);
        clear = (Button)findViewById(R.id.clear);
        eraser = (Button)findViewById(R.id.eraser);
        upload = (Button)findViewById(R.id.upload);
        clear.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                pad.clearPad();
            }
        });
        eraser.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (pad.getColor() == getResources().getColor(R.color.black)) {
                    int temp = R.color.white;
                    pad.setColor(temp);
                    eraser.setText("铅笔");
                } else {
                    int temp = R.color.black;
                    pad.setColor(temp);
                    eraser.setText("橡皮");
                }

            }

        });
        upload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                dialog = ProgressDialog.show(PadActivity.this, (CharSequence) "稍等<(￣︶￣)>", (CharSequence) "正在上传");
                new Thread(new Runnable() {
                    Message msg = new Message();
                    Socket socket = null;

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            Thread.sleep(1000);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }


                        //
                        try {
                            socket = new Socket("192.168.1.101", 2020);
                            DataInputStream dis = new DataInputStream(new BufferedInputStream(pad.getInputStream()));
                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                            //write the json first
                            Sketch sketch = new Sketch();
                            sketch.setLongtitude(String.valueOf(longtitude));
                            sketch.setLatitude(String.valueOf(latitude));
                            JSONObject jsonObject = JsonHelper.toJSON(sketch);
                            dos.writeUTF(jsonObject.toString());


                            //then the image data
                            byte[] temp = new byte[1024];
                            int a = 0;
                            while ((a = dis.read(temp)) != -1) {
                                dos.write(temp, 0, a);
                            }
                            socket.close();
                            dis.close();
                            dos.close();
                            msg.what = 0x11;
                        } catch (UnknownHostException e) {
                            // TODO Auto-generated catch block
                            msg.what = 0x12;
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            msg.what = 0x13;
                            e.printStackTrace();
                        } finally {
                            if (socket != null)
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            handler.sendMessage(msg);
                            dialog.dismiss();

                        }
                    }
                }).start();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pad, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
