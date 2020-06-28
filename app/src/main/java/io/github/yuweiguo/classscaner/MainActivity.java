package io.github.yuweiguo.classscaner;

import android.os.Bundle;

import java.lang.reflect.Field;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Field field = R.mipmap.class.getField("ic_launcher");
            int res = field.getInt(null);
            L.d("resid =" + res);

            getResources().getIdentifier("testident", "id", getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        test();
        test2();
    }

    public void test2(){
        L.getId("testcall2");
    }

    public void test(){
        L.getId("testcall");
    }

    public void test3(){
        L.getTest(this, "testgetid");
    }


    public void test4(){
        try {
            Field field = MainActivity.class.getField("mConfigChangeFlags");
            int mConfigChangeFlags = field.getInt(this);
            L.d("mConfigChangeFlags" + mConfigChangeFlags);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
