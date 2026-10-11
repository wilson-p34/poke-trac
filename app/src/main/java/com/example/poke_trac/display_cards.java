package com.example.poke_trac;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import android.content.Intent;

public class display_cards extends AppCompatActivity {

    private static final int PAGE_SIZE = 200;          // API max for /sets/:id
    private static final String TAG_CARDS = "cards";
    private static final boolean ENGLISH_ONLY = true;  // set false to include Japanese etc.

    private Spinner spinnerSets, spinnerTypes;
    private TextView textStatus;
    private CardAdapter adapter;
    private RequestQueue queue;

    private final List<Card> allCards = new ArrayList<>();
    private List<String> currentTypes = new ArrayList<>();
    private int loadId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_cards);

        spinnerSets = findViewById(R.id.spinnerSets);
        spinnerTypes = findViewById(R.id.spinnerTypes);
        textStatus = findViewById(R.id.textStatus);
        RecyclerView recycler = findViewById(R.id.recyclerCards);

        adapter = new CardAdapter();
        recycler.setLayoutManager(new GridLayoutManager(this, 3));
        recycler.setAdapter(adapter);
        adapter.setOnCardClickListener(this::addToCollection);       // tap = add
        adapter.setOnCardLongClickListener(this::showCardDialog);    // hold = big preview

        findViewById(R.id.buttonCollection).setOnClickListener(v ->
                startActivity(new Intent(this, CollectionActivity.class)));
        queue = Volley.newRequestQueue(this);

        setTypeList(new ArrayList<>(Collections.singletonList("All")));
        spinnerTypes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                applyFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

        spinnerSets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                PokemonSet set = (PokemonSet) p.getItemAtPosition(pos);
                loadCards(set.id);
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

        loadSets();
    }

    private void addToCollection(Card c) {
        int qty = Collection_Manager.add(this, c);
        Toast.makeText(this, c.name + " added (you own " + qty + ")",
                Toast.LENGTH_SHORT).show();
    }
    private void showCardDialog(Card c) {
        View view = getLayoutInflater().inflate(R.layout.dialog_card, null);
        ImageView image = view.findViewById(R.id.imageLarge);
        TextView info = view.findViewById(R.id.textInfo);

        info.setText(c.name + "\nType: " + c.cardType);

        GlideUrl url = new GlideUrl(api.BASE + "images/" + c.id + "?size=high",
                new LazyHeaders.Builder().addHeader("X-API-Key", api.KEY).build());
        Glide.with(this).load(url).into(image);

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Close", null)
                .show();
    }
    // ---------- Request helper (adds the API key header) ----------
    private JsonObjectRequest makeRequest(String url,
                                          Response.Listener<JSONObject> ok,
                                          Response.ErrorListener err) {
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null, ok, err) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("X-API-Key", api.KEY);
                return h;
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(30000, 1, 1f));
        return req;
    }

    // ---------- Load all sets ----------
    private void loadSets() {
        String url = api.BASE + "sets";
        queue.add(makeRequest(url,
                response -> {
                    try {
                        JSONArray data = response.getJSONArray("data");
                        List<PokemonSet> sets = new ArrayList<>();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject o = data.getJSONObject(i);
                            if (ENGLISH_ONLY && !"eng".equals(o.optString("language"))) continue;
                            PokemonSet s = new PokemonSet();
                            s.id = o.getString("set_id");
                            s.name = o.getString("name");
                            sets.add(s);
                        }
                        spinnerSets.setAdapter(new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_dropdown_item, sets));
                        textStatus.setText(sets.size() + " sets loaded");
                    } catch (Exception e) {
                        showError(e.getMessage());
                    }
                },
                error -> showError(describe(error, "Could not load sets"))));
    }

    // ---------- Load all cards in a set (paged) ----------
    private void loadCards(String setId) {
        queue.cancelAll(TAG_CARDS);
        allCards.clear();
        adapter.setCards(new ArrayList<>());
        setTypeList(new ArrayList<>(Collections.singletonList("All")));
        textStatus.setText("Loading cards...");
        loadCardPage(setId, 1, ++loadId);
    }

    private void loadCardPage(String setId, int page, int myLoadId) {
        String url = api.BASE + "sets/" + setId + "?page=" + page + "&limit=" + PAGE_SIZE;

        JsonObjectRequest req = makeRequest(url,
                response -> {
                    if (myLoadId != loadId) return;   // user picked another set
                    try {
                        JSONArray data = response.optJSONArray("cards");
                        if (data == null) {
                            showError("No cards returned for this set");
                            return;
                        }
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject o = data.getJSONObject(i);
                            JSONObject info = o.getJSONObject("card_info");

                            if ("sealed".equals(info.optString("product_type"))) continue;

                            Card c = new Card();
                            c.id = o.getString("id");
                            c.name = info.optString("name", "Unknown");
                            c.setName = info.optString("set_name", "");
                            c.cardType = info.isNull("card_type")
                                    ? "Other" : info.getString("card_type");
                            c.imageUrl = api.BASE + "images/" + c.id + "?size=low";
                            allCards.add(c);
                        }
                        rebuildTypeList();
                        applyFilter();

                        if (data.length() == PAGE_SIZE) {      // probably more pages
                            loadCardPage(setId, page + 1, myLoadId);
                        }
                    } catch (Exception e) {
                        showError(e.getMessage());
                    }
                },
                error -> showError(describe(error, "Could not load cards")));
        req.setTag(TAG_CARDS);
        queue.add(req);
    }

    // ---------- Type spinner built from the cards actually in the set ----------
    private void rebuildTypeList() {
        TreeSet<String> found = new TreeSet<>();
        for (Card c : allCards) found.add(c.cardType);

        List<String> types = new ArrayList<>();
        types.add("All");
        types.addAll(found);

        if (!types.equals(currentTypes)) {
            String selected = (String) spinnerTypes.getSelectedItem();
            setTypeList(types);
            int idx = types.indexOf(selected);
            if (idx >= 0) spinnerTypes.setSelection(idx);
        }
    }

    private void setTypeList(List<String> types) {
        currentTypes = types;
        spinnerTypes.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, types));
    }

    // ---------- Filter ----------
    private void applyFilter() {
        String filter = (String) spinnerTypes.getSelectedItem();
        List<Card> shown = new ArrayList<>();

        for (Card c : allCards) {
            if (filter == null || filter.equals("All") || c.cardType.equals(filter)) {
                shown.add(c);
            }
        }
        adapter.setCards(shown);
        textStatus.setText(shown.size() + " cards shown (" + allCards.size() + " in set)");
    }

    // ---------- Errors ----------
    private String describe(VolleyError error, String fallback) {
        if (error.networkResponse != null) {
            int code = error.networkResponse.statusCode;
            if (code == 401) return "Invalid API key (401)";
            if (code == 429) return "Rate limit reached (429), try later";
            return fallback + " (HTTP " + code + ")";
        }
        return fallback + ": " + error.getClass().getSimpleName();
    }

    private void showError(String msg) {
        textStatus.setText("Error: " + msg);
        Toast.makeText(this, "Error: " + msg, Toast.LENGTH_SHORT).show();
    }
}