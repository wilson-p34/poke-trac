package com.example.poke_trac;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CollectionActivity extends AppCompatActivity {

    private static final int SPAN = 3;

    private CollectionAdapter adapter;
    private TextView textSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
        setTitle("My collection");

        textSummary = findViewById(R.id.textSummary);
        RecyclerView recycler = findViewById(R.id.recyclerCollection);

        adapter = new CollectionAdapter();
        GridLayoutManager lm = new GridLayoutManager(this, SPAN);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? SPAN : 1;   // headers use full width
            }
        });
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);
        adapter.setOnCardClickListener(this::confirmRemove);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    // ---------- Group: Type > Set > Cards ----------
    private void refresh() {
        List<Card> all = Collection_Manager.getAll(this);

        // TreeMaps keep types and sets in alphabetical order
        Map<String, Map<String, List<Card>>> byType = new TreeMap<>();
        int total = 0;

        for (Card c : all) {
            String type = typeOf(c);
            String set = setOf(c);

            Map<String, List<Card>> bySet = byType.get(type);
            if (bySet == null) {
                bySet = new TreeMap<>();
                byType.put(type, bySet);
            }
            List<Card> list = bySet.get(set);
            if (list == null) {
                list = new ArrayList<>();
                bySet.put(set, list);
            }
            list.add(c);
            total += c.quantity;
        }

        List<CollectionAdapter.Row> rows = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Card>>> typeEntry : byType.entrySet()) {
            int typeCopies = 0;
            for (List<Card> l : typeEntry.getValue().values()) typeCopies += countCopies(l);
            rows.add(CollectionAdapter.Row.header(0,
                    typeEntry.getKey() + "  (" + typeCopies + ")"));

            for (Map.Entry<String, List<Card>> setEntry : typeEntry.getValue().entrySet()) {
                List<Card> cards = setEntry.getValue();
                Collections.sort(cards, (a, b) -> a.name.compareToIgnoreCase(b.name));
                rows.add(CollectionAdapter.Row.header(1,
                        setEntry.getKey() + "  (" + countCopies(cards) + ")"));
                for (Card c : cards) rows.add(CollectionAdapter.Row.card(c));
            }
        }
        adapter.setRows(rows);

        textSummary.setText(all.isEmpty()
                ? "Your collection is empty. Go back and tap cards to add them."
                : all.size() + " unique cards, " + total + " total");
    }

    private int countCopies(List<Card> cards) {
        int n = 0;
        for (Card c : cards) n += c.quantity;
        return n;
    }

    private String setOf(Card c) {
        return (c.setName == null || c.setName.isEmpty()) ? "Unknown set" : c.setName;
    }

    private String typeOf(Card c) {
        return (c.cardType == null || c.cardType.isEmpty()) ? "Other" : c.cardType;
    }

    // ---------- Remove cards ----------
    private void confirmRemove(Card c) {
        new AlertDialog.Builder(this)
                .setTitle(c.name)
                .setMessage(setOf(c) + "\nYou own " + c.quantity)
                .setPositiveButton("Remove one", (d, w) -> {
                    Collection_Manager.removeOne(this, c.id);
                    refresh();
                })
                .setNegativeButton("Remove all", (d, w) -> {
                    Collection_Manager.removeAll(this, c.id);
                    refresh();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }
}