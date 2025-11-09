package com.example.hango.ui.cart;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.hango.R;
import com.example.hango.api.Item;
import com.example.hango.api.RetrofitClient;
import com.example.hango.data.CartManager;
import android.app.AlertDialog;
import android.graphics.Bitmap;
// ZXing:
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;


import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private EditText searchEditText;
    private LinearLayout productContainer;
    private TextView totalPriceText;

    private final List<Item> allItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        searchEditText   = root.findViewById(R.id.searchEditText);
        productContainer = root.findViewById(R.id.product_container);
        totalPriceText   = root.findViewById(R.id.totalPriceText);

        // load từ CartManager (đã persist nếu mày cài SharedPreferences)
        allItems.clear();
        allItems.addAll(CartManager.getInstance().getItems());

        renderList(allItems);
        updateTotal(allItems);
        setupSearch();

        // Thanh toán
        View btnCheckout = root.findViewById(R.id.btnCheckout);
        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> {
                if (allItems.isEmpty()) {
                    Toast.makeText(requireContext(), "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                    return;
                }

                long total = calculateTotalAmount(allItems); // tính tổng tiền
                showQrDialog(total);
            });
        }


        return root;
    }

    // ============== SEARCH ==============
    private void setupSearch() {
        if (searchEditText == null) return;

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim().toLowerCase();
                if (q.isEmpty()) {
                    renderList(allItems);
                    updateTotal(allItems);
                    return;
                }

                List<Item> filtered = new ArrayList<>();
                for (Item it : allItems) {
                    String name = safe(it.skuName);
                    String desc = safe(firstNonEmpty(it.description, it.text, it.ocrText, it.brandName));
                    if (name.contains(q) || desc.contains(q)) {
                        filtered.add(it);
                    }
                }
                renderList(filtered);
                updateTotal(filtered);
            }
        });
    }

    // ============== RENDER LIST ==============
    private void renderList(List<Item> list) {
        if (productContainer == null || getContext() == null) return;
        productContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String base = RetrofitClient.getBaseUrl();

        if (list == null || list.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("🛒 Giỏ hàng trống");
            empty.setTextSize(16);
            empty.setTextColor(0xFF5F6368);
            empty.setPadding(24, 24, 24, 24);
            productContainer.addView(empty);
            return;
        }

        for (Item item : list) {
            View itemView = inflater.inflate(R.layout.cart_item, productContainer, false);

            ImageView imageView = itemView.findViewById(R.id.headphonesImage);
            TextView nameView   = itemView.findViewById(R.id.headphonesName);
            TextView priceView  = itemView.findViewById(R.id.headphonesPrice);
            TextView cateView   = itemView.findViewById(R.id.headphonesCategory);
            ImageView deleteBtn = itemView.findViewById(R.id.deleteHeadphonesButton);
            TextView qtyText = itemView.findViewById(R.id.headphonesQuantity);
            View btnPlus  = itemView.findViewById(R.id.btnIncrease);
            View btnMinus = itemView.findViewById(R.id.btnDecrease);

            // Gán giá trị hiện tại (mặc định là 1 nếu null)
            int currentQty = (item.quantity != null) ? item.quantity : 1;
            qtyText.setText(String.valueOf(currentQty));

            // Nút tăng
            btnPlus.setOnClickListener(v -> {
                int qty = Integer.parseInt(qtyText.getText().toString());
                qty++;
                qtyText.setText(String.valueOf(qty));
                item.quantity = qty;

                CartManager.getInstance().updateItemQuantity(requireContext(), item.skuId, qty);
                updateTotal(allItems);
            });

            // Nút giảm
            btnMinus.setOnClickListener(v -> {
                int qty = Integer.parseInt(qtyText.getText().toString());
                if (qty > 1) {
                    qty--;
                    qtyText.setText(String.valueOf(qty));
                    item.quantity = qty;

                    CartManager.getInstance().updateItemQuantity(requireContext(), item.skuId, qty);
                    updateTotal(allItems);
                } else {
                    Toast.makeText(requireContext(), "Số lượng tối thiểu là 1", Toast.LENGTH_SHORT).show();
                }
            });

            // Ảnh: reuse logic như HomeFragment.buildImageUrl
            String fullUrl = buildImageUrl(base, item.imagePath);
            if (fullUrl != null && !fullUrl.trim().isEmpty()) {
                Glide.with(requireContext())
                        .load(fullUrl)
                        .placeholder(R.drawable.hango_logo)
                        .error(R.drawable.hango_logo)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.hango_logo);
            }

            // Tên sản phẩm
            nameView.setText(getOrDefault(item.skuName));

            // Giá (nếu chưa có field thì để "Đang cập nhật" hoặc giá fake)
            priceView.setText(buildPriceText(item));

            // Category / mô tả ngắn: giống showProductList
            String shortDesc = firstNonEmpty(item.description, item.text, item.ocrText, item.brandName);
            cateView.setText(getOrDefault(shortDesc));

            // Xoá 1 item khỏi giỏ
            deleteBtn.setOnClickListener(v -> {
                if (item.skuId != null) {
                    CartManager.getInstance().removeItem(requireContext(), item.skuId);
                    allItems.clear();
                    allItems.addAll(CartManager.getInstance().getItems());
                    renderList(allItems);
                    updateTotal(allItems);
                    Toast.makeText(requireContext(), "Đã xoá " + getOrDefault(item.skuName), Toast.LENGTH_SHORT).show();
                }
            });

            productContainer.addView(itemView);
        }
    }

    // ============== TÍNH TỔNG ==============
    private void updateTotal(List<Item> list) {
        if (totalPriceText == null) return;

        long totalAmount = 0;
        int totalQty = 0;

        if (list != null) {
            for (Item it : list) {
                int qty = (it.quantity != null) ? it.quantity : 1;
                totalQty += qty;

                long price = 10_000L; // giá mặc định
                if (it.price != null) {
                    price = it.price;
                } else if (it.sellingPrice != null) {
                    price = it.sellingPrice.longValue();
                } else if (it.unitPrice != null) {
                    price = it.unitPrice.longValue();
                }
                totalAmount += price * qty;
            }
        }

        String formattedTotal = formatPriceVnd(totalAmount);
        totalPriceText.setText("Tổng (" + totalQty + " sản phẩm): " + formattedTotal);
    }


    // ============== HELPERS ==============
    private String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null) {
                String t = v.trim();
                if (!t.isEmpty()) return t;
            }
        }
        return null;
    }

    private String getOrDefault(String v) {
        return (v != null && !v.trim().isEmpty()) ? v : "Không rõ";
    }

    private String buildImageUrl(String base, String imagePath) {
        if (imagePath == null) return null;
        String p = imagePath.trim();
        if (p.isEmpty()) return null;
        if (p.startsWith("http://") || p.startsWith("https://")) return p;
        if (base == null || base.trim().isEmpty()) return null;

        String b = base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        while (p.startsWith("/")) p = p.substring(1);

        if (!(p.startsWith("uploads/") || p.startsWith("static/uploads/"))) {
            p = "uploads/" + p;
        }
        return b + "/" + p;
    }
    private long calculateTotalAmount(List<Item> list) {
        long total = 0;
        if (list != null) {
            for (Item it : list) {
                int qty = (it.quantity != null) ? it.quantity : 1;
                long price = 10_000L; // giá mặc định

                if (it.price != null) {
                    price = it.price;
                } else if (it.sellingPrice != null) {
                    price = it.sellingPrice.longValue();
                } else if (it.unitPrice != null) {
                    price = it.unitPrice.longValue();
                }

                total += price * qty;
            }
        }
        return total;
    }

    private void showQrDialog(long total) {
        if (!isAdded()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_qr_payment, null, false);

        TextView tvTotal = view.findViewById(R.id.tvTotal);
        ImageView imgQr = view.findViewById(R.id.imgQr);
        View btnClose = view.findViewById(R.id.btnClose);

        String totalStr = formatPriceVnd(total);
        tvTotal.setText("Tổng: " + totalStr);

        // Nội dung nhúng trong QR (tùy mày, sau này đổi theo chuẩn VietQR / Momo cũng được)
        String qrContent = "HANGO|AMOUNT=" + total + "|NOTE=Thanh toan don hang";

        Bitmap qrBitmap = generateQrBitmap(qrContent, 600, 600);
        if (qrBitmap != null) {
            imgQr.setImageBitmap(qrBitmap);
        } else {
            Toast.makeText(requireContext(), "Không tạo được QR", Toast.LENGTH_SHORT).show();
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String buildPriceText(Item item) {
        if (item == null) return "Giá: Đang cập nhật";

        Number price = null;

        if (item.price != null) {
            price = item.price;
        } else if (item.sellingPrice != null) {
            price = item.sellingPrice;
        } else if (item.unitPrice != null) {
            price = item.unitPrice;
        }

        if (price != null) {
            return "Giá: " + formatPriceVnd(price);
        }
        return "Giá: Đang cập nhật";
    }

    private String formatPriceVnd(Number price) {
        if (price == null) return "Đang cập nhật";
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return nf.format(price.longValue()) + "₫";
    }

    private Bitmap generateQrBitmap(String content, int width, int height) {
        try {
            com.google.zxing.MultiFormatWriter writer = new com.google.zxing.MultiFormatWriter();
            com.google.zxing.common.BitMatrix bitMatrix =
                    writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, width, height);

            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
