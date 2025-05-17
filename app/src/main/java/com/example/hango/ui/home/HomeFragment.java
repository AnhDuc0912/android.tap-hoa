package com.example.hango.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

        openCameraButton.setOnClickListener(v -> openCamera());
        return view;
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraActivityLauncher.launch(intent);
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), "Yêu cầu quyền camera", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");

                    if (imageBitmap != null) {
                        sendImageToApi(imageBitmap); // Gửi ảnh sau khi chụp
                    } else {
                        Toast.makeText(getContext(), "Không thể chụp ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void sendImageToApi(Bitmap imageBitmap) {
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
                                Log.d("API_RESPONSE", jsonResponse);

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
    }

    private String buildImageUrl(String baseUrl, String category, String imagePath) {
        if (category == null || category.isEmpty() || imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        String encodedCategory = Uri.encode(category);
        return baseUrl + encodedCategory + "/" + imagePath;
    }

    private void showProductList(List<Product> products, String predictedCategory) {
        View view = getView();
        if (view == null) return;

        LinearLayout content_container = view.findViewById(R.id.content_container);
        content_container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        String baseImageUrl = "http://192.168.1.249:5000/static/";

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

            String fullImageUrl = buildImageUrl(baseImageUrl, predictedCategory, imagePath);
            if (fullImageUrl != null) {
                Glide.with(requireContext())
                        .load(fullImageUrl)
                        .error(R.drawable.hango_logo)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.hango_logo);
            }

            content_container.addView(productView);
        }
    }
}
