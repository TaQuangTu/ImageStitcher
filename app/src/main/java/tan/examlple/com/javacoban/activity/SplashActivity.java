package tan.examlple.com.javacoban.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import tan.examlple.com.javacoban.R;

public class SplashActivity extends AppCompatActivity {

    private boolean mIsFirstTimeRun = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }
    private void goToMainActivity(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            }
        },1300);  //delay 1,3 s
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mIsFirstTimeRun==true){
            mIsFirstTimeRun = false;
            goToMainActivity();
        }
        else{
            finish();
        }
    }
}
