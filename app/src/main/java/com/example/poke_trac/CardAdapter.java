package com.example.poke_trac;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import java.util.ArrayList;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.Holder> {

    public interface OnCardClickListener {
        void onCardClick(Card card);
    }

    private final List<Card> cards = new ArrayList<>();
    private OnCardClickListener clickListener;
    private OnCardClickListener longClickListener;

    public void setOnCardClickListener(OnCardClickListener l) {
        this.clickListener = l;
    }

    public void setOnCardLongClickListener(OnCardClickListener l) {
        this.longClickListener = l;
    }

    public void setCards(List<Card> newCards) {
        cards.clear();
        cards.addAll(newCards);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_card, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Card c = cards.get(position);
        h.name.setText(c.quantity > 1 ? c.name + " x" + c.quantity : c.name);

        GlideUrl url = new GlideUrl(c.imageUrl,
                new LazyHeaders.Builder().addHeader("X-API-Key", api.KEY).build());
        Glide.with(h.image.getContext()).load(url).into(h.image);

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCardClick(c);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCardClick(c);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;

        Holder(View v) {
            super(v);
            image = v.findViewById(R.id.imageCard);
            name = v.findViewById(R.id.textCardName);
        }
    }
}