package tan.examlple.com.javacoban.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

import tan.examlple.com.javacoban.R;
import tan.examlple.com.javacoban.activity.MainActivity;

public class ImageHorizontalListAdapter extends RecyclerView.Adapter<ImageHorizontalListAdapter.ImageViewHolder> {

    private Context context;
    private ArrayList<Bitmap> bitmapArrayList;
    private int layoutResourceId;

    //image size for display
    private int imageHeight;
    private int imageWidth;

    class ImageViewHolder extends RecyclerView.ViewHolder{
        ImageView imvMainImage;
        ImageView imvIconRemove;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
             imvIconRemove = itemView.findViewById(R.id.imvIconRemove);
             imvMainImage = itemView.findViewById(R.id.imvMainImage);
        }
    }

    public ImageHorizontalListAdapter(Context context, ArrayList<Bitmap> bitmapArrayList, int layoutResourceId) {
        this.context = context;
        this.bitmapArrayList = bitmapArrayList;
        this.layoutResourceId = layoutResourceId;
        imageHeight = 400;
        imageHeight = 600;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(layoutResourceId,viewGroup,false);
        ImageViewHolder viewHolder = new ImageViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder imageViewHolder, int i) {
        Bitmap bitmap = bitmapArrayList.get(i);

        //create a scaled bitmap just for display
        int displayWidth = (int)(bitmap.getWidth()*0.7);
        int displayHeight = (int)(bitmap.getHeight()*0.7);
        Bitmap miniBitmap = Bitmap.createScaledBitmap(bitmap,displayWidth,displayHeight,false);
        imageViewHolder.imvMainImage.setImageBitmap(miniBitmap);


        final int position = i;

        //hide remove icon inside the last item
        if(i==bitmapArrayList.size()-1){
            imageViewHolder.imvIconRemove.setVisibility(View.GONE);
        }
        else{
            imageViewHolder.imvIconRemove.setVisibility(View.VISIBLE);
            imageViewHolder.imvIconRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bitmapArrayList.remove(position);
                    ImageHorizontalListAdapter.this.notifyDataSetChanged();
                }
            });
        }

        imageViewHolder.imvMainImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(context instanceof MainActivity){
                    ((MainActivity)context).chooseMutipleImageFromGallery();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return bitmapArrayList.size();
    }

}
