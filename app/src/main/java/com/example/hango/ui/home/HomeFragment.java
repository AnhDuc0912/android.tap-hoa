package com.example.hango.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.hango.R;
import com.example.hango.api.ApiService;
import com.example.hango.api.ResponseWrapper;
import com.example.hango.api.RetrofitClient;
import com.example.hango.entitys.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private String predictedCategory;
    private List<Product> productList = new ArrayList<>();
    private NestedScrollView nestedScrollView;
    private int offset = 0; // Theo dõi offset
    private final int PAGE_SIZE = 5; // Số lượng sản phẩm mỗi lần tải
    private boolean isLoading = false; // Trạng thái đang tải
    private List<Call<ResponseWrapper>> activeCalls = new ArrayList<>(); // Theo dõi các Retrofit Call

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Tìm NestedScrollView
        nestedScrollView = rootView.findViewById(R.id.main_scroll);
        if (nestedScrollView == null) {
            Log.e("HomeFragment", "main_scroll là null!");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Không tìm thấy NestedScrollView", Toast.LENGTH_SHORT).show();
            }
        }

        // Lắng nghe sự kiện cuộn
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isLoading) {
                    View viewChild = v.getChildAt(v.getChildCount() - 1);
                    int diff = (viewChild.getBottom() - (v.getHeight() + v.getScrollY()));
                    if (diff <= 100) { // Ngưỡng 100px để phát hiện gần cuối
                        loadMoreSimilarProducts(rootView);
                    }
                }
            });
        }

        // Lấy dữ liệu ban đầu từ Bundle và hiển thị
        parseArguments();
        if (productList != null && !productList.isEmpty()) {
            offset = productList.size(); // Cập nhật offset ban đầu
            showProductList(rootView);
        } else if (isAdded()) {
            Toast.makeText(requireContext(), "Không có sản phẩm để hiển thị", Toast.LENGTH_SHORT).show();
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy tất cả các Retrofit Call khi Fragment bị hủy
        for (Call<ResponseWrapper> call : activeCalls) {
            call.cancel();
        }
        activeCalls.clear();
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
     * Gọi API để tải thêm sản phẩm tương tự
     */
    private void loadMoreSimilarProducts(View rootView) {
        if (isLoading) return; // Tránh gọi API nhiều lần
        isLoading = true;

        ApiService apiService = RetrofitClient.getApiService();
        Call<ResponseWrapper> call = apiService.loadMoreSimilarProducts(offset);
        activeCalls.add(call);

        call.enqueue(new Callback<ResponseWrapper>() {
            @Override
            public void onResponse(@NonNull Call<ResponseWrapper> call, @NonNull Response<ResponseWrapper> response) {
                activeCalls.remove(call);
                isLoading = false;
                if (!isAdded()) return; // Kiểm tra Fragment có còn gắn không

                if (response.isSuccessful() && response.body() != null) {
                    List<Product> newProducts = response.body().getSimilarProducts(); // Giả định ResponseWrapper có phương thức getProducts()
                    if (newProducts != null && !newProducts.isEmpty()) {
                        productList.addAll(newProducts); // Thêm sản phẩm mới vào danh sách
                        offset += newProducts.size(); // Cập nhật offset
                        showProductList(rootView); // Cập nhật giao diện
                    } else {
                        Toast.makeText(requireContext(), "Không còn sản phẩm để tải", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải thêm sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseWrapper> call, @NonNull Throwable t) {
                activeCalls.remove(call);
                isLoading = false;
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Hiển thị danh sách sản phẩm ra giao diện
     */
    private void showProductList(View rootView) {
        LinearLayout container = rootView.findViewById(R.id.content_container);
        if (container == null) {
            Log.e("HomeFragment", "content_container là null!");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Không tìm thấy container để hiển thị sản phẩm", Toast.LENGTH_SHORT).show();
            }
            return;
        }
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