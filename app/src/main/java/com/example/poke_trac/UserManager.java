package com.example.poke_trac;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserManager {

    public static boolean isLoggedIn(Context ctx) {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    /** Returns the user's unique Firebase id (used as the storage key). */
    public static String getCurrentUser(Context ctx) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    public static String getEmail(Context ctx) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u == null ? "" : u.getEmail();
    }

    public static void logout(Context ctx) {
        FirebaseAuth.getInstance().signOut();
    }
}