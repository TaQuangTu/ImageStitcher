package tan.examlple.com.javacoban.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import tan.examlple.com.javacoban.MainActivity;
import tan.examlple.com.javacoban.R;

public class DialogWaiting extends Dialog {
    Button btnStop;
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
}
