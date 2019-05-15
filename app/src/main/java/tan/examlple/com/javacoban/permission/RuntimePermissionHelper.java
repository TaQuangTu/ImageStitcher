package tan.examlple.com.javacoban.permission;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public final class RuntimePermissionHelper {

    private Activity activity;
    public RuntimePermissionHelper(Activity activity)  {
        this.activity = activity;
    }
    public void requestPermisstion(String permission,int requestCode){
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            //do nothing
        }
        ActivityCompat.requestPermissions(activity,new String[]{permission},requestCode);
    }
    public boolean permissionAlreadyGranted(String permission){
        int result = ContextCompat.checkSelfPermission(activity,permission);
        if(result == PackageManager.PERMISSION_GRANTED) return true;
        return false;
    }
    public void toast(String message){
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}