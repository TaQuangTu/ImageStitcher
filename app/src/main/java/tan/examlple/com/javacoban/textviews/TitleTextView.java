package tan.examlple.com.javacoban.textviews;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.TextView;

import tan.examlple.com.javacoban.R;

public class TitleTextView extends TextView {
    private float mTextSize = 18;
    public TitleTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setDefaultProperties();
    }
    public void setDefaultProperties(){
        setTextSize(mTextSize);
        setTextColor(ContextCompat.getColor(getContext(),R.color.color_red));
    }
}
