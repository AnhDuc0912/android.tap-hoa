package com.example.hango.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.hango.api.Item;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CartManager {
    private static final String PREF_NAME = "hango_cart";
    private static final String KEY_CART = "cart_items";

    private static CartManager instance;
    private final List<Item> cartItems = new ArrayList<>();
    private final Gson gson = new Gson();

    private CartManager() {}

    public static synchronized CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        return instance;
    }

    /** Gọi 1 lần ở MainActivity.onCreate */
    public void loadFromPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CART, null);
        if (json != null) {
            try {
                Type listType = new TypeToken<List<Item>>() {}.getType();
                List<Item> saved = gson.fromJson(json, listType);
                cartItems.clear();
                if (saved != null) {
                    cartItems.addAll(saved);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** Lưu lại SharedPreferences */
    public void saveToPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CART, gson.toJson(cartItems)).apply();
    }

    /** Thêm 1 sản phẩm vào giỏ: nếu đã có thì +1 quantity */
    public void addItem(Context context, Item item) {
        if (item == null || item.skuId == null) return;

        for (Item i : cartItems) {
            if (Objects.equals(i.skuId, item.skuId)) {
                if (i.quantity == null) i.quantity = 1;
                i.quantity += 1;
                saveToPrefs(context);
                return;
            }
        }

        if (item.quantity == null) item.quantity = 1;
        cartItems.add(item);
        saveToPrefs(context);
    }

    /** Xóa theo skuId */
    public void removeItem(Context context, Long skuId) {
        if (skuId == null) return;
        cartItems.removeIf(i -> i.skuId != null && Objects.equals(i.skuId, skuId));
        saveToPrefs(context);
    }

    /** Cập nhật số lượng mới (dùng cho nút + -) */
    public void updateItemQuantity(Context context, Long skuId, int newQty) {
        if (skuId == null || newQty < 1) return;

        for (Item i : cartItems) {
            if (i.skuId != null && Objects.equals(i.skuId, skuId)) {
                i.quantity = newQty;
                break;
            }
        }
        saveToPrefs(context);
    }

    public void clear(Context context) {
        cartItems.clear();
        saveToPrefs(context);
    }

    public List<Item> getItems() {
        return new ArrayList<>(cartItems);
    }

    public int getCount() {
        return cartItems.size();
    }
}
