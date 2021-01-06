package fr.maximenarbaud.travelmap;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private final List<RecyclerElement> recyclerElementList;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // Data is passed into the constructor
    RecyclerViewAdapter(Context context, List<RecyclerElement> recyclerElementList) {
        this.mInflater = LayoutInflater.from(context);
        this.recyclerElementList = recyclerElementList;
    }

    // Inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(view);
    }

    // Binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecyclerElement item = recyclerElementList.get(position);

        holder.firstLineTextView.setText(item.getFirstLine());
        holder.secondLineTextView.setText(item.getSecondLine());
        holder.iconImageView.setImageResource(item.getResourceId());
        holder.metaTextView.setText(item.getMeta());
        holder.cardView.setBackgroundTintList(ColorStateList.valueOf(item.getBackgroundTint()));
    }

    // Total number of rows
    @Override
    public int getItemCount() {
        return recyclerElementList.size();
    }


    // Stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView firstLineTextView;
        TextView secondLineTextView;
        TextView metaTextView;
        ImageView iconImageView;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);

            firstLineTextView = itemView.findViewById(R.id.recycler_item_first_text);
            secondLineTextView = itemView.findViewById(R.id.recycler_item_second_text);
            iconImageView = itemView.findViewById(R.id.recycler_item_icon_image_view);
            metaTextView = itemView.findViewById(R.id.recycler_item_meta_text);
            cardView = itemView.findViewById(R.id.recycler_item_card_view);
            // TODO Change picture

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
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

    public void deleteItem(final int position) {
        recyclerElementList.remove(position);
        notifyItemRemoved(position);
    }
}
