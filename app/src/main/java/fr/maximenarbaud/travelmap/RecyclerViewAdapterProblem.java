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

public class RecyclerViewAdapterProblem extends RecyclerView.Adapter<RecyclerViewAdapterProblem.ViewHolder> {
    private final List<ProblemObj> problemObjList;
    private final LayoutInflater mInflater;
    private RecyclerViewAdapter.ItemClickListener mClickListener;

    // Data is passed into the constructor
    RecyclerViewAdapterProblem(Context context, List<ProblemObj> problemObjList) {
        this.mInflater = LayoutInflater.from(context);
        this.problemObjList = problemObjList;
    }

    // Inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recycler_item_problem, parent, false);
        return new ViewHolder(view);
    }

    // Binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ProblemObj problem = problemObjList.get(position);

        holder.firstLineTextView.setText(problem.getProblemText());
        holder.secondLineTextView.setText(String.format("Le %s", problem.getFormatTimestamp()));
    }

    // Total number of rows
    @Override
    public int getItemCount() {
        return problemObjList.size();
    }


    // Stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView firstLineTextView;
        TextView secondLineTextView;
        ImageView deleteIcon;

        ViewHolder(View itemView) {
            super(itemView);

            firstLineTextView = itemView.findViewById(R.id.recycler_item_first_text);
            secondLineTextView = itemView.findViewById(R.id.recycler_item_second_text);
            deleteIcon = itemView.findViewById(R.id.recycler_item_problem_delete_imageView);

            deleteIcon.setOnClickListener(this);
            //itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // Allows clicks events to be caught
    void setClickListener(RecyclerViewAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
        // TODO Define setClickListener
    }


    public void deleteItem(final int position) {
        problemObjList.remove(position);
        notifyItemRemoved(position);
    }
}
