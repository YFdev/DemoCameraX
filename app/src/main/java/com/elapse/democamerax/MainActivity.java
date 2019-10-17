package com.elapse.democamerax;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.elapse.democamerax.fragments.CameraFragment;
import com.elapse.democamerax.fragments.PermissionFragment;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final String KEY_EVENT_ACTION = "key_event_action";
    public static final String KEY_EVENT_EXTRA = "key_event_extra";
    private static final long IMMERSIVE_FLAG_TIMEOUT = 500L;
    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.fragment_container);

    }

    @Override
    protected void onResume(){
        super.onResume();
        //设置全屏
        container.postDelayed(new Runnable() {
            @Override
            public void run() {
                container.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                replaceFragment(new PermissionFragment());
            }
        }, IMMERSIVE_FLAG_TIMEOUT);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container,fragment);
//        transaction.addToBackStack(null);
        transaction.commit();
    }

    //音量键控制拍照
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Intent intent = new Intent(KEY_EVENT_ACTION);
            intent.putExtra(KEY_EVENT_EXTRA,keyCode);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            return true;
        } else
            return super.onKeyDown(keyCode, event);
    }

    //图片输出路径
    public static File getOutputDirectory(Context context){
        Context applicationContext = context.getApplicationContext();
        File mediaDir = context.getExternalMediaDirs()[0];
        File output = null;
        if (mediaDir != null){
            output = new File(mediaDir,context.getString(R.string.app_name));
            if (!output.exists()){
                output.mkdirs();
            }
        }

        if (output != null && output.exists()){
            return output;
        }else {
            return applicationContext.getFilesDir();
        }
    }

}
