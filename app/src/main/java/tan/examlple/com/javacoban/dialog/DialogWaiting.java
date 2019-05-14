package tan.examlple.com.javacoban.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import tan.examlple.com.javacoban.R;
import tan.examlple.com.javacoban.activity.MainActivity;
import tan.examlple.com.javacoban.imageprocess.ImageStitcher.ProcessingListener;

public class DialogWaiting extends Dialog implements ProcessingListener {
    Button btnStop;
    TextView tvPercentage;
    public DialogWaiting(Context context) {
        super(context);
        this.setCancelable(false);
        setContentView(R.layout.dialog_waiting_processing);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        mapViews();
        setOnClick();
    }
    private void mapViews(){
        btnStop = findViewById(R.id.btnStop);
        tvPercentage = findViewById(R.id.tvPercentage);
    }
    private void setOnClick(){
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogWaiting.this.dismiss();
                if(getContext() instanceof MainActivity){((MainActivity)getContext()).stopMatching();}
            }
        });
    }

    @Override
    public int onProcess(int percentage) {
        tvPercentage.setText(""+percentage+"%");
        return percentage;
    }
}
