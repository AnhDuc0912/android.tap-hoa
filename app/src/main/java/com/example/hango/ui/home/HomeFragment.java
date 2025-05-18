package com.example.hango.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.hango.R;
import com.example.hango.api.RetrofitClient;
import com.example.hango.entitys.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private String predictedCategory;
    private List<Product> productList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        parseArguments(); // Tách xử lý Bundle riêng
        if (productList != null && !productList.isEmpty()) {
            showProductList(rootView);
        } else {
            // TODO: Hiển thị thông báo khi không có sản phẩm
        }

        return rootView;
    }

    /**
     * Tách xử lý lấy dữ liệu từ Bundle ra riêng
     */
    private void parseArguments() {
        Bundle args = getArguments();
        if (args != null) {
            predictedCategory = args.getString("predictedCategory", "");
            String productListJson = args.getString("productList", "[]");

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Product>>() {}.getType();
            productList = gson.fromJson(productListJson, listType);
        } else {
            predictedCategory = "";
            productList = Collections.emptyList();
        }
    }

    /**
     * Hiển thị danh sách sản phẩm ra giao diện
     */
    private void showProductList(View rootView) {
        LinearLayout container = rootView.findViewById(R.id.content_container);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String baseImageUrl = RetrofitClient.getBaseUrl() + "/static/";

        for (Product product : productList) {
            View itemView = inflater.inflate(R.layout.product_item, container, false);

            TextView categoryView = itemView.findViewById(R.id.categoryName);
            TextView nameView = itemView.findViewById(R.id.productName);
            TextView priceView = itemView.findViewById(R.id.productPrice);
            TextView similarityView = itemView.findViewById(R.id.productSimilarity);
            ImageView imageView = itemView.findViewById(R.id.productImage);

            categoryView.setText("Danh mục: " + getOrDefault(product.getCategoryName()));
            nameView.setText("Tên: " + getOrDefault(product.getProductName()));
            priceView.setText("Giá: " + getOrDefault(product.getPrice()) + "đ");
            similarityView.setText(String.format("Độ tương đồng: %.2f%%", product.getSimilarity() * 100));

            // Tải ảnh nếu có
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

            container.addView(itemView);
        }
    }

    /**
     * Trả về giá trị hoặc "Không rõ" nếu null
     */
    private String getOrDefault(String value) {
        return (value != null && !value.trim().isEmpty()) ? value : "Không rõ";
    }
}
