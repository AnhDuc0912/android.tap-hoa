package com.example.hango.ui.cart;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.hango.MainActivity;
import com.example.hango.R;
import com.example.hango.api.ApiService;
import com.example.hango.api.CatResponse;
import com.example.hango.api.ProductsResponse;
import com.example.hango.api.ResponseWrapper;
import com.example.hango.api.RetrofitClient;
import com.example.hango.products.Category;
import com.example.hango.products.Product;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private ImageView addProductButton;
    private NestedScrollView nestedScrollView;
    private List<Product> productList = new ArrayList<>();
    private int offset = 0;
    private final int PAGE_SIZE = 5;
    private boolean isLoading = false;
    private Handler handler; // Handler để trì hoãn tải ảnh
    private List<Call<ProductsResponse>> activeCalls = new ArrayList<>(); // Theo dõi các Retrofit Call

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper()); // Khởi tạo Handler
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        // Tìm NestedScrollView
        nestedScrollView = view.findViewById(R.id.main_scroll);
        if (nestedScrollView == null) {
            Log.e("CartFragment", "main_scroll là null!");
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
                        loadMoreProducts(view);
                    }
                }
            });
        }

        // Gắn sự kiện cho nút thêm sản phẩm
        addProductButton = view.findViewById(R.id.addProductButton);
        addProductButton.setOnClickListener(v -> showAddProductDialog());

        // Gọi API để lấy danh sách sản phẩm ban đầu
        fetchProducts(view);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy tất cả các tác vụ bất đồng bộ
        if (handler != null) {
            handler.removeCallbacksAndMessages(null); // Hủy tất cả các tác vụ trì hoãn
        }
        // Hủy các Retrofit Call
        for (Call<ProductsResponse> call : activeCalls) {
            call.cancel();
        }
        activeCalls.clear();
    }

    private void fetchProducts(View rootView) {
        ApiService apiService = RetrofitClient.getApiService();
        Call<ProductsResponse> call = apiService.getProducts();
        activeCalls.add(call); // Thêm vào danh sách để theo dõi

        call.enqueue(new Callback<ProductsResponse>() {
            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {
                activeCalls.remove(call); // Xóa khỏi danh sách khi hoàn thành
                if (!isAdded()) return; // Kiểm tra Fragment có còn gắn không

                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body().getProducts();
                    if (products != null && !products.isEmpty()) {
                        productList.clear();
                        productList.addAll(products);
                        offset = products.size();
                        showProductList(rootView, productList, "");
                    } else {
                        Toast.makeText(requireContext(), "No products found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {
                activeCalls.remove(call);
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMoreProducts(View rootView) {
        if (isLoading) return;
        isLoading = true;

        ApiService apiService = RetrofitClient.getApiService();
        Call<ProductsResponse> call = apiService.loadMoreProducts(offset);
        activeCalls.add(call);

        call.enqueue(new Callback<ProductsResponse>() {
            @Override
            public void onResponse(Call<ProductsResponse> call, Response<ProductsResponse> response) {
                activeCalls.remove(call);
                isLoading = false;
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Product> newProducts = response.body().getProducts();
                    if (newProducts != null && !newProducts.isEmpty()) {
                        productList.addAll(newProducts);
                        offset += newProducts.size();
                        showProductList(rootView, productList, "");
                    } else {
                        Toast.makeText(requireContext(), "Không còn sản phẩm để tải", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải thêm sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductsResponse> call, Throwable t) {
                activeCalls.remove(call);
                isLoading = false;
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProductList(View rootView, List<Product> products, String predictedCategory) {
        LinearLayout contentContainer = rootView.findViewById(R.id.product_container);

        if (contentContainer == null) {
            Log.e("CartFragment", "product_container là null!");
            if (isAdded()) {
                Toast.makeText(requireContext(), "Không tìm thấy container để hiển thị sản phẩm", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        contentContainer.removeAllViews();

        if (products == null || products.isEmpty()) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Không có sản phẩm để hiển thị", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        String baseImageUrl = RetrofitClient.getBaseUrl() + "/static/";
        final int delayMs = 500;

        List<Product> productsCopy = new ArrayList<>(products);

        for (int i = 0; i < productsCopy.size(); i++) {
            final Product product = productsCopy.get(i);
            final int index = i;
            final View productView = inflater.inflate(R.layout.product_item_manager, contentContainer, false);

            ImageView imageView = productView.findViewById(R.id.headphonesImage);
            TextView nameView = productView.findViewById(R.id.headphonesName);
            TextView priceView = productView.findViewById(R.id.headphonesPrice);
            TextView categoryView = productView.findViewById(R.id.headphonesCategory);
            ImageView deleteButton = productView.findViewById(R.id.deleteHeadphonesButton);

            nameView.setText(product.getProductName() != null ? product.getProductName() : "Không rõ");
            priceView.setText(product.getPrice() != null ? product.getPrice() + "đ" : "Không rõ");
            categoryView.setText(product.getCategoryName() != null ? product.getCategoryName() : "Không rõ");

            String label = product.getLabel();
            String imagePath = product.getImagePath();
            if (imagePath != null && !imagePath.isEmpty() && label != null && !label.isEmpty()) {
                String fullImageUrl = baseImageUrl + label + "/" + imagePath;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) return; // Kiểm tra Fragment có còn gắn không
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
                    }
                }, index * delayMs);
            } else {
                imageView.setImageResource(R.drawable.hango_logo);
            }

            final int position = productsCopy.indexOf(product);
            deleteButton.setOnClickListener(v -> {
                contentContainer.removeView(productView);
                if (position >= 0 && position < productList.size()) {
                    productList.remove(position);
                }
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Sản phẩm đã được xóa", Toast.LENGTH_SHORT).show();
                }
            });

            contentContainer.addView(productView);
        }
    }

    private void showAddProductDialog() {
        if (!isAdded()) return;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_product, null);

        EditText etName = dialogView.findViewById(R.id.etProductName);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        Button btnChooseImage = dialogView.findViewById(R.id.btnChooseImage);
        LinearLayout featuresList = dialogView.findViewById(R.id.llFeaturesList);
        ImageView productImage = dialogView.findViewById(R.id.productImage);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);

        List<String> features = new ArrayList<>();

        // Gọi API để lấy danh sách danh mục
        ApiService apiService = RetrofitClient.getApiService();
        Call<CatResponse> call = apiService.getCategories();
        call.enqueue(new Callback<CatResponse>() {
            @Override
            public void onResponse(Call<CatResponse> call, Response<CatResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Category> categories = response.body().getCategories();
                    if (categories != null && !categories.isEmpty()) {
                        // Thiết lập Spinner với danh sách danh mục
                        ArrayAdapter<Category> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategory.setAdapter(adapter);

                        // Thiết lập giá trị mặc định
                        spinnerCategory.setSelection(0);
                    } else {
                        Toast.makeText(requireContext(), "Không có danh mục để hiển thị", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Không thể tải danh mục", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CatResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnChooseImage.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraWithCallback(imageBitmap -> {
                    if (productImage != null && imageBitmap != null) {
                        productImage.setImageBitmap(imageBitmap);
                    } else {
                        Log.e("AddProductDialog", "ImageView hoặc ảnh chụp bị null");
                    }
                });
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Thêm sản phẩm mới")
                .setView(dialogView)
                .setPositiveButton("Thêm", (d, which) -> {
                    String name = etName.getText().toString().trim();
                    String price = etPrice.getText().toString().trim();
                    Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
                    if (isAdded()) {
                        if (selectedCategory != null) {
                            Toast.makeText(requireContext(), "Đã thêm sản phẩm: " + name + " (Danh mục: " + selectedCategory.getName() + ")", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Đã thêm sản phẩm: " + name, Toast.LENGTH_SHORT).show();
                        }
                    }
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