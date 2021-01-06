package fr.maximenarbaud.travelmap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapterCity extends RecyclerView.Adapter<RecyclerViewAdapterCity.ViewHolder> {
    private final List<CityObj> cityList;

    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // Data is passed into the constructor
    RecyclerViewAdapterCity(Context context, List<CityObj> cityList) {
        this.mInflater = LayoutInflater.from(context);
        this.cityList = cityList;
    }

    // Inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recycler_item_city, parent, false);
        return new ViewHolder(view);
    }

    // Binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.cityNameTextView.setText(cityList.get(position).getCityName());

        if(cityList.get(position).getSelected()) {
            holder.checkIconImageView.setVisibility(View.VISIBLE);
        }
    }

    // Total number of rows
    @Override
    public int getItemCount() {
        return cityList.size();
    }


    // Stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView cityNameTextView;
        ImageView checkIconImageView;

        ViewHolder(View itemView) {
            super(itemView);

            cityNameTextView = itemView.findViewById(R.id.recycler_item_city_text);
            checkIconImageView = itemView.findViewById(R.id.recycler_item_city_check_imageView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());

            if (checkIconImageView.getVisibility() == View.INVISIBLE) {
                checkIconImageView.setVisibility(View.VISIBLE);

            } else {
                checkIconImageView.setVisibility(View.INVISIBLE);
            }
        }
    }

    // Allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
        // TODO Define setClickListener
    }

    // Parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
