package com.example.hango.ui.cart;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.hango.MainActivity;
import com.example.hango.R;
import com.example.hango.api.CategoryResponse;
import com.example.hango.api.RetrofitClient;
import com.example.hango.entitys.Category;
import com.example.hango.entitys.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartFragment extends Fragment {

    private ImageView addProductButton;
    private Bitmap capturedImageBitmap;
    private List<Product> products = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private String predictedCategory = "";  // Nếu có dự đoán danh mục, set sau này

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        addProductButton = view.findViewById(R.id.addProductButton);

        // Lấy danh sách sản phẩm truyền vào từ arguments (nếu có)
        Bundle args = getArguments();
        if (args != null) {
            String productListJson = args.getString("productList", "[]");
            products = parseProductList(productListJson);
        }

        addProductButton.setOnClickListener(v -> showAddProductDialog());

        // Không cần lấy danh mục thì trực tiếp show danh sách sản phẩm
        showProductList(view, products);

        return view;
    }

    private List<Product> parseProductList(String json) {
        Type productListType = new TypeToken<List<Product>>() {}.getType();
        return new Gson().fromJson(json, productListType);
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
                if (position >= 0 && position < productList.size()) {
                    productList.remove(position); // Xóa khỏi danh sách gốc
                }
                Toast.makeText(requireContext(), "Sản phẩm đã được xóa", Toast.LENGTH_SHORT).show();
            });

            contentContainer.addView(productView);
        }
    }

    private void showAddProductDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null);

        EditText etName = dialogView.findViewById(R.id.etProductName);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        Button btnChooseImage = dialogView.findViewById(R.id.btnChooseImage);
        ImageView productImage = dialogView.findViewById(R.id.productImage);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spCategory);

        // Tạo list tên danh mục để cho Spinner hiển thị
        List<String> categoryNames = new ArrayList<>();
        for (Category c : categories) {
            categoryNames.add(c.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnChooseImage.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraWithCallback(imageBitmap -> {
                    productImage.setImageBitmap(imageBitmap);
                    capturedImageBitmap = imageBitmap;
                });
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm sản phẩm mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String price = etPrice.getText().toString().trim();

                    if (name.isEmpty() || price.isEmpty()) {
                        Toast.makeText(requireContext(), "Vui lòng nhập tên và giá", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedPosition = spinnerCategory.getSelectedItemPosition();
                    String selectedCategoryName = "";
                    if (selectedPosition >= 0 && selectedPosition < categories.size()) {
                        selectedCategoryName = categories.get(selectedPosition).getName();
                    }

                    Product newProduct = new Product();
                    newProduct.setProductName(name);
                    newProduct.setPrice(price);
                    newProduct.setCategoryName(selectedCategoryName); // Gán danh mục theo chọn

                    // TODO: Xử lý ảnh nếu có capturedImageBitmap

                    addProductToServer(newProduct);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }


    private void addProductToServer(Product product) {
        RetrofitClient.getApiService().addProduct(product).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    products.add(response.body());
                    if (getView() != null) {
                        showProductList(getView(), products);
                    }
                    Toast.makeText(requireContext(), "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Không thể thêm sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCategories(View rootView) {
        RetrofitClient.getApiService().getCategories().enqueue(new Callback<CategoryResponse>() {
            @Override
            public void onResponse(Call<CategoryResponse> call, Response<CategoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body().getCategories();
                    showProductList(rootView, products);
                } else {
                    Toast.makeText(requireContext(), "Không tải được danh mục", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CategoryResponse> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
