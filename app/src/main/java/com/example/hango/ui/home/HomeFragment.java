package com.example.hango.ui.home;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.hango.MainActivity;
import com.example.hango.R;
import com.example.hango.api.ApiService;
import com.example.hango.api.ResponseWrapper;
import com.example.hango.api.RetrofitClient;
import com.example.hango.products.Product;
import com.google.gson.Gson;
import com.bumptech.glide.Glide;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class HomeFragment extends Fragment {
    private ImageView openCameraButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        openCameraButton = view.findViewById(R.id.openCameraButton);

        openCameraButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCameraWithCallback(imageBitmap -> {
                    // Xử lý ảnh chụp trả về ở đây
                    sendImageToApi(imageBitmap);
                });
            } else {
                Toast.makeText(getContext(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void sendImageToApi(Bitmap imageBitmap) {
        new Thread(() -> {
            try {
                // Lưu bitmap thành file JPEG tạm thời
                File imageFile = new File(requireContext().getCacheDir(), "captured_image.jpg");
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                }

                // Chuẩn bị MultipartBody.Part cho Retrofit
                RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
                MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

                // Gọi API
                ApiService apiService = RetrofitClient.getApiService();
                Call<ResponseBody> call = apiService.uploadImage(body);

                call.enqueue(new retrofit2.Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        requireActivity().runOnUiThread(() -> {
                            if (response.isSuccessful() && response.body() != null) {
                                try {
                                    String jsonResponse = response.body().string();

                                    Gson gson = new Gson();
                                    ResponseWrapper responseWrapper = gson.fromJson(jsonResponse, ResponseWrapper.class);

                                    List<Product> productList = responseWrapper.getSimilarProducts();
                                    showProductList(productList, responseWrapper.getPredictedCategory());

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getContext(), "Lỗi đọc dữ liệu trả về", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getContext(), "Lỗi gửi ảnh: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                });

            } catch (Exception e) {
                Log.e("SendImageError", "Lỗi khi gửi ảnh", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Lỗi khi gửi ảnh", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
    // Thêm biến predictedCategory làm tham số đầu vào của hàm
    private void showProductList(List<Product> products, String predictedCategory) {
        View view = getView();
        if (view == null) return;

        LinearLayout content_container = view.findViewById(R.id.content_container);
        content_container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        String baseImageUrl = RetrofitClient.getBaseUrl() + "/static/";

        for (Product product : products) {
            View productView = inflater.inflate(R.layout.product_item, content_container, false);

            TextView nameView = productView.findViewById(R.id.productName);
            TextView priceView = productView.findViewById(R.id.productPrice);
            ImageView imageView = productView.findViewById(R.id.productImage);
            TextView similarityView = productView.findViewById(R.id.productSimilarity);

            String name = product.getName() != null ? product.getName() : "Không rõ";
            String price = product.getPrice() != null ? product.getPrice() + "đ" : "Không rõ";
            double similarity = product.getSimilarity();

            nameView.setText("Tên: " + name);
            priceView.setText("Giá: " + price);
            similarityView.setText(String.format("Độ tương đồng: %.2f%%", similarity * 100));

            String imagePath = product.getImagePath();

            if (predictedCategory != null && !predictedCategory.isEmpty()
                    && imagePath != null && !imagePath.isEmpty()) {

                // Nối predictedCategory + imagePath tạo thành URL ảnh
                String fullImageUrl = baseImageUrl + predictedCategory + "/" + imagePath;
                Glide.with(requireContext())
                        .load(fullImageUrl)
                        .error(R.drawable.hango_logo) // ảnh mặc định khi lỗi tải
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.hango_logo); // ảnh mặc định
            }

            content_container.addView(productView);
        }
    }
}