package com.example.poke_trac;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class Collection_Manager {

    private static final String PREFS = "collection_prefs";
    private static String key(Context ctx) {
        return "collection_" + UserManager.getCurrentUser(ctx);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Card> getAll(Context ctx) {
        List<Card> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(ctx).getString(key(ctx), "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Card c = new Card();
                c.id = o.getString("id");
                c.name = o.optString("name");
                c.cardType = o.optString("cardType");
                c.setName = o.optString("setName");
                c.imageUrl = o.optString("imageUrl");
                c.quantity = o.optInt("quantity", 1);
                list.add(c);
            }
        } catch (JSONException ignored) { }
        return list;
    }

    private static void save(Context ctx, List<Card> cards) {
        JSONArray arr = new JSONArray();
        try {
            for (Card c : cards) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("name", c.name);
                o.put("cardType", c.cardType);
                o.put("setName", c.setName);
                o.put("imageUrl", c.imageUrl);
                o.put("quantity", c.quantity);
                arr.put(o);
            }
        } catch (JSONException ignored) {
        }
        String json = arr.toString();
        prefs(ctx).edit().putString(key(ctx), json).apply();
        push(ctx, json);
    }
    /** Adds one copy and returns how many copies the user now owns. */
    public static int add(Context ctx, Card card) {
        List<Card> cards = getAll(ctx);
        for (Card c : cards) {
            if (c.id.equals(card.id)) {
                c.quantity++;
                save(ctx, cards);
                return c.quantity;
            }
        }
        Card copy = new Card();
        copy.id = card.id;
        copy.name = card.name;
        copy.cardType = card.cardType;
        copy.setName = card.setName;
        copy.imageUrl = card.imageUrl;
        copy.quantity = 1;
        cards.add(copy);
        save(ctx, cards);
        return 1;
    }

    public static void removeOne(Context ctx, String id) {
        List<Card> cards = getAll(ctx);
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (c.id.equals(id)) {
                if (c.quantity > 1) c.quantity--;
                else cards.remove(i);
                break;
            }
        }
        save(ctx, cards);
    }

    public static void removeAll(Context ctx, String id) {
        List<Card> cards = getAll(ctx);
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).id.equals(id)) {
                cards.remove(i);
                break;
            }
        }
        save(ctx, cards);
    }

    private static void push(Context ctx, String json) {
        String uid = UserManager.getCurrentUser(ctx);
        if (uid == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("collection", json);
        FirebaseFirestore.getInstance().collection("users").document(uid).set(data);
    }

    /** Downloads the user's collection from the server into the local cache. */
    public static void pull(Context ctx, Runnable onDone) {
        String uid = UserManager.getCurrentUser(ctx);
        if (uid == null) {
            if (onDone != null) onDone.run();
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String json = doc.getString("collection");
                    if (json != null) {
                        prefs(ctx).edit().putString(key(ctx), json).apply();
                    }
                    if (onDone != null) onDone.run();
                })
                .addOnFailureListener(e -> {
                    if (onDone != null) onDone.run();   // offline: keep using the local copy
                });
    }
}