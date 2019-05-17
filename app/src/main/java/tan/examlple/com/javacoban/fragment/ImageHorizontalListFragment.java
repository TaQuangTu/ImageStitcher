package tan.examlple.com.javacoban.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import tan.examlple.com.javacoban.R;
import tan.examlple.com.javacoban.device.DeviceHelper;

import static android.content.ContentValues.TAG;


public class ImageHorizontalListFragment extends Fragment {


    private RecyclerView recyclerViewImages;
    private ImageHorizontalListAdapter imageHorizontalListAdapter;
    private ArrayList<Bitmap> bitmapArrayList;

    private Bitmap defaultBitmap;
    public ImageHorizontalListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Drawable imageViewDrawable = getResources().getDrawable(R.drawable.ic_add_image);
        defaultBitmap = ((BitmapDrawable)imageViewDrawable).getBitmap();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image_horizontal_list, container, false);
        DeviceHelper deviceHelper= DeviceHelper.getInstance();
        int heightOfFragment = (int)(deviceHelper.getScreenHeight()/3.0);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,heightOfFragment);
        Log.d(TAG, "onCreateView: "+heightOfFragment);
        view.setLayoutParams(params);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }
    private void mapViews(){
        recyclerViewImages = getView().findViewById(R.id.recyclerViewImages);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //init recycler view, the recycler view has already have a default bitmap (insert image icon)
        mapViews();
        bitmapArrayList = new ArrayList<>();
        bitmapArrayList.add(defaultBitmap);

        imageHorizontalListAdapter = new ImageHorizontalListAdapter(getActivity(),bitmapArrayList,R.layout.view_row_image_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerViewImages.setLayoutManager(linearLayoutManager);

        recyclerViewImages.setAdapter(imageHorizontalListAdapter);
    }
    public void addBitmap(Bitmap bitmap, int position)
    {
        if(position==bitmapArrayList.size()-1){
            //remove last image, it's the empty image
            bitmapArrayList.remove(bitmapArrayList.size()-1);
            //then add bitmap add tail
            bitmapArrayList.add(bitmap);
            //and add a empty image for the next insertion
            bitmapArrayList.add(defaultBitmap);
        }
        else{
            bitmapArrayList.set(position,bitmap);
        }
        imageHorizontalListAdapter.notifyDataSetChanged();
    }

    public ArrayList<Bitmap> getBitmapArray() {
        return this.bitmapArrayList;
    }
    public void clearBitmapArrays(){
        this.bitmapArrayList.clear();
        this.bitmapArrayList.add(defaultBitmap);
        imageHorizontalListAdapter.notifyDataSetChanged();
    }
}
