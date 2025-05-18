package com.example.hango.ui.cart;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.hango.MainActivity;
import com.example.hango.R;
import com.example.hango.api.ApiService;
import com.example.hango.api.ProductsResponse;
import com.example.hango.api.RetrofitClient;
import com.example.hango.products.Product;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private ImageView addProductButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        // Call the API to fetch products
        fetchProducts(view);

        // Gắn sự kiện cho nút thêm sản phẩm
        addProductButton = view.findViewById(R.id.addProductButton);
        addProductButton.setOnClickListener(v -> showAddProductDialog());

        return view;
    }

    private void fetchProducts(View rootView) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ProductsResponse> call = apiService.getProducts();

        call.enqueue(new Callback<ProductsResponse>() {
            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body().getProducts();
                    if (products != null && !products.isEmpty()) {
                        // Display the products on the UI
                        showProductList(rootView, products, "");
                    } else {
                        Toast.makeText(requireContext(), "No products found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProductList(View rootView, List<Product> products, String predictedCategory) {
        LinearLayout contentContainer = rootView.findViewById(R.id.product_container);

        // Kiểm tra null để tránh NullPointerException
        if (contentContainer == null) {
            Log.e("CartFragment", "product_container là null!");
            Toast.makeText(requireContext(), "Không tìm thấy container để hiển thị sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xóa các view cũ trước khi thêm mới để tránh trùng lặp
        contentContainer.removeAllViews();

        // Kiểm tra danh sách sản phẩm rỗng
        if (products == null || products.isEmpty()) {
            Toast.makeText(requireContext(), "Không có sản phẩm để hiển thị", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        String baseImageUrl = RetrofitClient.getBaseUrl() + "/static/";

        // Tạo một bản sao của danh sách để tránh ConcurrentModificationException
        List<Product> productsCopy = new ArrayList<>(products);

        for (Product product : productsCopy) {
            View productView = inflater.inflate(R.layout.product_item_manager, contentContainer, false);

            ImageView imageView = productView.findViewById(R.id.headphonesImage);
            TextView nameView = productView.findViewById(R.id.headphonesName);
            TextView priceView = productView.findViewById(R.id.headphonesPrice);
            TextView categoryView = productView.findViewById(R.id.headphonesCategory);
            ImageView deleteButton = productView.findViewById(R.id.deleteHeadphonesButton);

            // Đổ dữ liệu
            nameView.setText(product.getProductName() != null ? product.getProductName() : "Không rõ");
            priceView.setText(product.getPrice() != null ? product.getPrice() + "đ" : "Không rõ");
            categoryView.setText(product.getCategoryName() != null ? product.getCategoryName() : "Không rõ");

            // Load ảnh
            String label = product.getLabel();
            String imagePath = product.getImagePath();
            if (imagePath != null && !imagePath.isEmpty() && label != null && !label.isEmpty()) {
                String fullImageUrl = baseImageUrl + label + "/" + imagePath;
                Glide.with(requireContext())
                        .load(fullImageUrl)
                        .error(R.drawable.hango_logo)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                                if (e != null) {
                                    Log.e("GlideError", "Failed to load image: " + fullImageUrl, e);
                                    e.logRootCauses("GlideError");
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.hango_logo);
            }

            // Thêm chức năng xóa
            final int position = productsCopy.indexOf(product); // Lấy vị trí trong danh sách sao chép
            deleteButton.setOnClickListener(v -> {
                contentContainer.removeView(productView);
                if (position >= 0 && position < products.size()) {
                    products.remove(position); // Xóa khỏi danh sách gốc
                }
                Toast.makeText(requireContext(), "Sản phẩm đã được xóa", Toast.LENGTH_SHORT).show();
            });

            contentContainer.addView(productView);
        }
    }

    private void showAddProductDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_product, null);

        EditText etName = dialogView.findViewById(R.id.etProductName);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        Button btnChooseImage = dialogView.findViewById(R.id.btnChooseImage);
        EditText etFeature = dialogView.findViewById(R.id.etFeature);
        ImageButton btnAddFeat = dialogView.findViewById(R.id.btnAddFeature);
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