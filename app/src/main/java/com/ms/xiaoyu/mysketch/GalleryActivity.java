package com.ms.xiaoyu.mysketch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.ms.xiaoyu.adapter.ImageAdapter;
import com.ms.xiaoyu.bean.Coordinate;
import com.ms.xiaoyu.bean.Sketch;
import com.ms.xiaoyu.util.JsonHelper;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class GalleryActivity extends Activity {
    private ImageAdapter adapter;
    private List<Bitmap> list;
    private Socket socket;
    private ProgressDialog dialog;
    private SwipeFlingAdapterView gallery;
    private LocationClient locationClient;

    private Coordinate coordinate;
    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

//    private double longtitude;
//    private double latitude;
    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == 0x11){
                locationClient.stop();
                adapter.refresh(list);
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), "加载成功,,T_T,,",
                        Toast.LENGTH_LONG).show();
                System.out.println("数量为" + adapter.getCount());

            }
            else{
                locationClient.stop();
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), "加载失败,,T_T,,",
                        Toast.LENGTH_LONG).show();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);


        dialog = ProgressDialog.show(GalleryActivity.this,(CharSequence)"稍等<(￣︶￣)>", (CharSequence)"正在加载..");
        init();
    }

    private void init() {
        // TODO Auto-generated method stub
        coordinate = new Coordinate();
        locationClient = new LocationClient(this);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);
        option.setCoorType("bd09ll");       //set type
        locationClient.setLocOption(option);
        locationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (bdLocation == null) return;

                coordinate.longtitude = bdLocation.getLongitude();
                coordinate.latitude = bdLocation.getLatitude();


            }
        });
        locationClient.start();
        initGalleryRes();
        gallery = (SwipeFlingAdapterView) findViewById(R.id.frame);
        System.out.println("初始化完毕");
        adapter = new ImageAdapter(GalleryActivity.this,R.layout.item,list);
        gallery.setAdapter(adapter);
        gallery.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                list.remove(0);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object o) {

            }

            @Override
            public void onRightCardExit(Object o) {

            }

            @Override
            public void onAdapterAboutToEmpty(int i) {

            }

            @Override
            public void onScroll(float v) {

            }
        });

    }

    private void initGalleryRes() {
        // TODO Auto-generated method stub
        list = new ArrayList<Bitmap>();
        new Thread(new Runnable(){

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Thread.sleep(1000);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                Message msg = new Message();
                try {
                    socket = new Socket("192.168.1.101",2021);

                    //write the json first
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    Sketch sketch = new Sketch();

                    sketch.setLongtitude(String.valueOf(coordinate.longtitude));
                    sketch.setLatitude(String.valueOf(coordinate.latitude));

                    JSONObject jsonObject = JsonHelper.toJSON(sketch);
                    dos.writeUTF(jsonObject.toString());
                    dos.flush();


                    //then img
                    BufferedInputStream bi = new BufferedInputStream(socket.getInputStream());
                    while(true){
                        byte[] l = new byte[4];
                        int tmp = 0;
                        if((tmp = bi.read(l)) == -1) break;
                        int len = deserialize(l);
//						byte[] buffer = getByteArray(bi,len);

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        for(int i = 0;i < len;i ++){
                            outputStream.write(bi.read());
                        }
                        outputStream.flush();
                        byte[] buffer = outputStream.toByteArray();

                        Bitmap bm = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                        list.add(bm);
                    }
                    msg.what = 0x11;
                    handler.sendMessage(msg);

                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    msg.what = 0x12;
                    handler.sendMessage(msg);
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    msg.what = 0x12;
                    handler.sendMessage(msg);
                    e.printStackTrace();
                } finally{
                    if(socket != null){
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }}).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery, menu);
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
    public int deserialize(byte[] b){
        int ans = 0;
        ans += (b[0] >= 0 ? b[0] : b[0] + 256);
        ans += (b[1] >= 0 ? b[1] << 8 : (b[1]+ 256) << 8);
        ans += (b[2] >= 0 ? b[2] << 16 : (b[2]+ 256) << 16);
        ans += (b[3] >= 0 ? b[3] << 24 : (b[3] + 256) << 24);
        return ans;
    }
    public byte[] getByteArray(InputStream bi,int len){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for(int i = 0;i < len;i ++){
            try {
                outputStream.write(bi.read());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            outputStream.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] buffer = outputStream.toByteArray();

        return buffer;
    }
}
