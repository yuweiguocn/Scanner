package io.github.yuweiguo.classscaner;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;

public class L {

    public static void d(String msg){
        Log.d("ywg", "ywg " + msg);
    }

    public static int getId(String num){
        try {
            String name = "weather_detail_icon_" + num;
            Field field = R.mipmap.class.getField(name);
            return field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    public static int getTest(Context context, String name){
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }

}
