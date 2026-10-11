package com.example.poke_trac;

import android.util.TypedValue;
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

public class CollectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_CARD = 1;

    /** One row in the list: either a header (level 0 or 1) or a card. */
    public static class Row {
        final int level;      // 0 = main header, 1 = sub-header, -1 = card
        final String title;
        final Card card;

        private Row(int level, String title, Card card) {
            this.level = level;
            this.title = title;
            this.card = card;
        }

        public static Row header(int level, String title) {
            return new Row(level, title, null);
        }

        public static Row card(Card c) {
            return new Row(-1, null, c);
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private CardAdapter.OnCardClickListener clickListener;

    public void setOnCardClickListener(CardAdapter.OnCardClickListener l) {
        this.clickListener = l;
    }

    public void setRows(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    public boolean isHeader(int position) {
        return rows.get(position).card == null;
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ? VIEW_HEADER : VIEW_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_HEADER) {
            return new HeaderHolder(inf.inflate(R.layout.item_header, parent, false));
        }
        return new CardHolder(inf.inflate(R.layout.activity_item_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);

        if (holder instanceof HeaderHolder) {
            TextView t = ((HeaderHolder) holder).text;
            t.setText(row.title);
            if (row.level == 0) {
                // type header: navy pill with yellow text
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                t.setTextColor(0xFFFFD23F);
                t.setBackgroundResource(R.drawable.bg_type_header);
                t.setPadding(dp(t, 16), dp(t, 12), dp(t, 16), dp(t, 12));
            } else {
                // set sub-header: blue text, no background
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                t.setTextColor(0xFF2B4FA3);
                t.setBackground(null);
                t.setPadding(dp(t, 8), dp(t, 10), dp(t, 8), dp(t, 2));
            }
            return;
        }

        CardHolder h = (CardHolder) holder;
        Card c = row.card;
        h.name.setText(c.quantity > 1 ? c.name + " x" + c.quantity : c.name);

        GlideUrl url = new GlideUrl(c.imageUrl,
                new LazyHeaders.Builder().addHeader("X-API-Key", api.KEY).build());
        Glide.with(h.image.getContext()).load(url).into(h.image);

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCardClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView text;
        HeaderHolder(View v) {
            super(v);
            text = v.findViewById(R.id.textHeader);
        }
    }
    private int dp(View v, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                v.getResources().getDisplayMetrics());
    }

    static class CardHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        CardHolder(View v) {
            super(v);
            image = v.findViewById(R.id.imageCard);
            name = v.findViewById(R.id.textCardName);
        }
    }
}