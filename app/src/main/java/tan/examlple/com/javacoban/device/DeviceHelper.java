package tan.examlple.com.javacoban.device;

import android.content.res.Resources;

public class DeviceHelper {
    private static DeviceHelper instance = new DeviceHelper();

    private DeviceHelper() {
    }

    public int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
    public static DeviceHelper getInstance(){
        if(instance==null)
            instance = new DeviceHelper();
        return instance;
    }
}
