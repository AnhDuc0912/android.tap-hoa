package com.example.hango.ui.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.hango.MainActivity;
import com.example.hango.R;
import com.example.hango.api.RetrofitClient;
import com.example.hango.products.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private ImageView addProductButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        // Lấy danh sách sản phẩm từ arguments
        Bundle args = getArguments();
        if (args != null) {
            String productListJson = args.getString("productList", "[]");
            String predictedCategory = args.getString("predictedCategory", "");

            List<Product> products = parseProductList(productListJson);
            showProductList(view, products, predictedCategory);
        }

        // Gắn sự kiện cho nút thêm sản phẩm
        addProductButton = view.findViewById(R.id.addProductButton);
        addProductButton.setOnClickListener(v -> showAddProductDialog());

        return view;
    }

    private List<Product> parseProductList(String json) {
        Type productListType = new TypeToken<List<Product>>(){}.getType();
        return new Gson().fromJson(json, productListType);
    }

    private void showProductList(View rootView, List<Product> products, String predictedCategory) {
        LinearLayout contentContainer = rootView.findViewById(R.id.content_container);
        contentContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        String baseImageUrl = RetrofitClient.getBaseUrl() + "/static/";

        for (Product product : products) {
            View productView = inflater.inflate(R.layout.product_item, contentContainer, false);

            TextView categoryNameView = productView.findViewById(R.id.categoryName);
            TextView nameView         = productView.findViewById(R.id.productName);
            TextView priceView        = productView.findViewById(R.id.productPrice);
            ImageView imageView       = productView.findViewById(R.id.productImage);
            TextView similarityView   = productView.findViewById(R.id.productSimilarity);

            String categoryName = product.getCategoryName() != null ? product.getCategoryName() : "Không rõ";
            String name         = product.getProductName()  != null ? product.getProductName()  : "Không rõ";
            String price        = product.getPrice()        != null ? product.getPrice() + "đ"  : "Không rõ";
            double similarity   = product.getSimilarity();

            categoryNameView.setText("Danh mục: " + categoryName);
            nameView.setText("Tên: " + name);
            priceView.setText("Giá: " + price);
            similarityView.setText(String.format("Độ tương đồng: %.2f%%", similarity * 100));

            String imagePath = product.getImagePath();
            if (!predictedCategory.isEmpty() && imagePath != null && !imagePath.isEmpty()) {
                String fullImageUrl = baseImageUrl + predictedCategory + "/" + imagePath;
                Glide.with(requireContext())
                        .load(fullImageUrl)
                        .error(R.drawable.hango_logo)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.hango_logo);
            }

            contentContainer.addView(productView);
        }
    }

    private void showAddProductDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_product, null);

        EditText etName         = dialogView.findViewById(R.id.etProductName);
        EditText etPrice        = dialogView.findViewById(R.id.etPrice);
        Button btnChooseImage   = dialogView.findViewById(R.id.btnChooseImage);
        EditText etFeature      = dialogView.findViewById(R.id.etFeature);
        ImageButton btnAddFeat  = dialogView.findViewById(R.id.btnAddFeature);
        LinearLayout featuresList = dialogView.findViewById(R.id.llFeaturesList);

        List<String> features = new ArrayList<>();

        btnAddFeat.setOnClickListener(v -> {
            String feature = etFeature.getText().toString().trim();
            if (!feature.isEmpty()) {
                features.add(feature);
                addFeatureChip(feature, featuresList);
                etFeature.setText("");
            }
        });

        btnChooseImage.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraWithCallback(imageBitmap -> {
                    ImageView productImage = requireView().findViewById(R.id.productImage);
                    productImage.setImageBitmap(imageBitmap);
                });
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Thêm sản phẩm mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", (d, which) -> {
                    String name = etName.getText().toString().trim();
                    String price = etPrice.getText().toString().trim();
                    // TODO: xử lý gửi name, price và features
                    Toast.makeText(requireContext(), "Đã thêm sản phẩm!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", (d, which) -> d.dismiss())
                .create();

        dialog.show();
    }

    private void addFeatureChip(String feature, LinearLayout container) {
        TextView chip = new TextView(requireContext());
        chip.setText(feature);
        chip.setPadding(16, 8, 16, 8);
        chip.setBackgroundResource(R.drawable.bg_chip);
        container.addView(chip);
    }
}
