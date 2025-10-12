package com.example.hango;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.hango.api.ApiService;
import com.example.hango.api.ResponseWrapper;
import com.example.hango.api.RetrofitClient;
import com.example.hango.api.SearchImageResponse;
import com.example.hango.ui.cart.CartFragment;
import com.example.hango.ui.dashboard.DashboardFragment;
import com.example.hango.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    // Launcher xử lý cấp quyền camera
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    // Launcher xử lý kết quả chụp ảnh
    private ActivityResultLauncher<Intent> cameraActivityLauncher;

    // Callback trả về ảnh chụp
    private CameraCallback cameraCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        loadFragment(new DashboardFragment());

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_dashboard) {
                loadFragment(new DashboardFragment());
                Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show();

                return true;
            }
//            Trang quản lý
            else if (id == R.id.navigation_cart) {
                Toast.makeText(MainActivity.this, "Trang quản lý", Toast.LENGTH_SHORT).show();
                loadFragment(new CartFragment());
                return true;
            }
            return false;
        });

        // Đăng ký launcher cấp quyền camera
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Yêu cầu quyền camera", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Đăng ký launcher nhận kết quả chụp ảnh
        cameraActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = (Bitmap) extras.get("data");

                        if (imageBitmap != null && cameraCallback != null) {
                            cameraCallback.onImageCaptured(imageBitmap);
                        } else {
                            Toast.makeText(this, "Không thể chụp ảnh", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    // Giao diện callback trả về ảnh chụp
    public interface CameraCallback {
        void onImageCaptured(Bitmap image);
    }

    // Hàm mở camera với callback để trả ảnh về
    public void openCameraWithCallback(CameraCallback callback) {
        this.cameraCallback = callback;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    // Thực hiện intent mở camera
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraActivityLauncher.launch(intent);
    }

    public void sendImageToApi(Bitmap imageBitmap) {
        try {
            // Lưu tạm bitmap thành file JPEG
            File imageFile = new File(getCacheDir(), "captured_image.jpg");
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            }

            // Build multipart: field name PHẢI là "file"
            RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", imageFile.getName(), fileBody);

            // Tham số k (tuỳ bạn, vd 20)
            RequestBody kPart = RequestBody.create("20", MediaType.parse("text/plain"));

            ApiService apiService = RetrofitClient.getApiService();
            Call<SearchImageResponse> call = apiService.searchByImage(filePart, kPart);

            call.enqueue(new retrofit2.Callback<SearchImageResponse>() {
                @Override
                public void onResponse(Call<SearchImageResponse> call, retrofit2.Response<SearchImageResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(MainActivity.this, "Lỗi gửi ảnh: " + response.code(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SearchImageResponse body = response.body();
                    if (body.error != null) {
                        Toast.makeText(MainActivity.this, "Server error: " + body.error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // TODO: chuyển sang màn hình hiển thị kết quả
                    // Ví dụ: truyền JSON qua Bundle cho HomeFragment
                    Bundle bundle = new Bundle();
                    //bundle.putString("search_filename", body.filename);
                    bundle.putString("search_results_json", new Gson().toJson(body.results));
                    bundle.putInt("search_total", body.total != null ? body.total : 0);

                    HomeFragment homeFragment = new HomeFragment();
                    homeFragment.setArguments(bundle);

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, homeFragment)
                            .addToBackStack(null)
                            .commit();
                }

                @Override
                public void onFailure(Call<SearchImageResponse> call, Throwable t) {
                    Toast.makeText(MainActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi gửi ảnh", Toast.LENGTH_SHORT).show();
        }
    }
}