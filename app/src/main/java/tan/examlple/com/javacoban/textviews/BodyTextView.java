package tan.examlple.com.javacoban.textviews;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.TextView;

import tan.examlple.com.javacoban.R;

public class BodyTextView extends TextView {
    private float mTextSize = 15;
    public BodyTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setDefaultProperties();
    }
    public void setDefaultProperties(){
        setTextSize(mTextSize);
        setTextColor(ContextCompat.getColor(getContext(),R.color.color_black));
    }
}
