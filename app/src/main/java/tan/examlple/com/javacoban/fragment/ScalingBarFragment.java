package tan.examlple.com.javacoban.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import tan.examlple.com.javacoban.R;

public class ScalingBarFragment extends Fragment {
    Button btnHelp;
    SeekBar seekBar;
    TextView tvScaleRatioValue;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scaling_bar, container, false);
        return view;
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mapViews();
        setViewListeners();
    }
    @Override
    public void onDetach() {
        super.onDetach();
    }


    private void mapViews(){
        View viewGroup = getView();
        btnHelp = viewGroup.findViewById(R.id.btnHelp);
        seekBar = viewGroup.findViewById(R.id.seekBarScaleRatio);
        tvScaleRatioValue = viewGroup.findViewById(R.id.tvScaleRatioValue);
    }
    private void setViewListeners(){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double ratio = ((double)seekBar.getProgress())/seekBar.getMax();
                Log.d("sds", "getScaleRatio: "+ratio);
                if(progress<9){
                    Toast.makeText(getActivity(), "Scale ratio must be greater than or equal 10%", Toast.LENGTH_SHORT).show();
                    tvScaleRatioValue.setText("10%");
                    seekBar.setProgress(10);
                }
                else{
                    tvScaleRatioValue.setText(""+progress+"%");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    public double getScaleRatio(){
        double ratio = ((double)seekBar.getProgress())/seekBar.getMax();
        Log.d("sds", "getScaleRatio: "+ratio);
        return ratio;
    }
}
