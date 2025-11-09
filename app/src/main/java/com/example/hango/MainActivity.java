package com.example.hango;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.hango.api.ApiListResponse;
import com.example.hango.api.ApiService;
import com.example.hango.api.Item;
import com.example.hango.api.RetrofitClient;
import com.example.hango.ui.cart.CartFragment;
import com.example.hango.ui.dashboard.DashboardFragment;
import com.example.hango.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> cameraActivityLauncher;
    private CameraCallback cameraCallback;

    private ProgressDialog progressDialog; // dùng tạm cho nhanh
    private final Gson gson = new Gson();
    private ApiService api;

    // cấu hình search ảnh
    private static final String DEF_K = "20";
    private static final String DEF_THRESHOLD = "0.15";
    private static final String DEF_APPLY_RERANK = "1"; // "0" để tắt re-rank phía server

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        api = RetrofitClient.getApiService();

        loadFragment(new DashboardFragment());

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_dashboard) {
                loadFragment(new DashboardFragment());
                Toast.makeText(this, "Trang chủ", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.navigation_cart) {
                loadFragment(new CartFragment());
                Toast.makeText(this, "Trang quản lý", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // Quyền camera
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openCamera();
                    else Toast.makeText(this, "Yêu cầu quyền camera", Toast.LENGTH_SHORT).show();
                }
        );

        // Nhận ảnh chụp (thumbnail)
        cameraActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        Bitmap imageBitmap = extras != null ? (Bitmap) extras.get("data") : null;
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

    public interface CameraCallback {
        void onImageCaptured(Bitmap image);
    }

    public void openCameraWithCallback(CameraCallback callback) {
        this.cameraCallback = callback;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraActivityLauncher.launch(intent);
    }

    // ==========================
    // GỬI ẢNH LÊN SERVER
    // ==========================
    public void sendImageToApi(Bitmap imageBitmap) {
        try {
            showLoadingDialog("Đang xử lý ảnh...");

            // Lưu tạm ảnh
            File imageFile = new File(getCacheDir(), "captured_image.jpg");
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            }

            // Multipart
            RequestBody fileBody  = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
            MultipartBody.Part pFile = MultipartBody.Part.createFormData("file", imageFile.getName(), fileBody);
            RequestBody pK       = RequestBody.create(MediaType.parse("text/plain"), DEF_K);
            RequestBody pThr     = RequestBody.create(MediaType.parse("text/plain"), DEF_THRESHOLD);
            RequestBody pApply   = RequestBody.create(MediaType.parse("text/plain"), DEF_APPLY_RERANK);

            Call<ApiListResponse<Item>> call = api.searchImage(pFile, pK, pThr, pApply);

            call.enqueue(new retrofit2.Callback<ApiListResponse<Item>>() {
                @Override
                public void onResponse(Call<ApiListResponse<Item>> call, Response<ApiListResponse<Item>> res) {
                    hideLoadingDialog();

                    if (!res.isSuccessful()) {
                        int code = res.code();
                        if (code == 404)       toast("Không tìm thấy kết quả ảnh");
                        else if (code == 503)  toast("Máy chủ dữ liệu đang bận (503)");
                        else                   toast("Lỗi gửi ảnh: HTTP " + code);
                        return;
                    }

                    ApiListResponse<Item> body = res.body();
                    if (body == null) { toast("Phản hồi rỗng"); return; }
                    if (!body.isOk())  { toast("Server error: " + body.error); return; }

                    List<Item> items = (body.results != null) ? body.results : java.util.Collections.emptyList();
                    String inferredQuery = inferQueryFromTop(items);

                    // NEW: lấy query_id từ response
                    long queryId = (body.queryId != null) ? body.queryId : -1L;

                    // NEW: truyền queryId sang HomeFragment
                    openHomeWithResults(gson.toJson(items), inferredQuery, queryId);
                }

                @Override
                public void onFailure(Call<ApiListResponse<Item>> call, Throwable t) {
                    hideLoadingDialog();
                    toast("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "unknown"));
                }
            });

        } catch (Exception e) {
            hideLoadingDialog();
            e.printStackTrace();
            toast("Lỗi khi gửi ảnh");
        }
    }

    // ==========================
    // GỬI TEXT LÊN SERVER
    // ==========================
    public void sendTextToApi(String query, int k) {
        try {
            showLoadingDialog("Đang tìm kiếm...");

            Map<String, Object> body = new HashMap<>();
            body.put("q", query);
            body.put("k", k);

            Call<ApiListResponse<Item>> call = api.searchTextWithImage(body);

            call.enqueue(new retrofit2.Callback<ApiListResponse<Item>>() {
                @Override
                public void onResponse(Call<ApiListResponse<Item>> call,
                                       Response<ApiListResponse<Item>> res) {
                    hideLoadingDialog();

                    if (!res.isSuccessful()) {
                        int code = res.code();
                        if (code == 400) toast("Thiếu từ khóa tìm kiếm (400)");
                        else if (code == 503) toast("Máy chủ dữ liệu đang bận (503)");
                        else toast("Lỗi tìm kiếm: HTTP " + code);
                        return;
                    }

                    ApiListResponse<Item> body = res.body();
                    if (body == null) { toast("Phản hồi rỗng"); return; }
                    if (!body.isOk())  { toast("Server error: " + body.error); return; }

                    // NEW
                    long queryId = (body.queryId != null) ? body.queryId : -1L;

                    // NEW
                    openHomeWithResults(gson.toJson(body.results), query, queryId);
                }

                @Override
                public void onFailure(Call<ApiListResponse<Item>> call, Throwable t) {
                    hideLoadingDialog();
                    toast("Lỗi mạng: " + (t.getMessage() != null ? t.getMessage() : "unknown"));
                }
            });

        } catch (Exception e) {
            hideLoadingDialog();
            e.printStackTrace();
            toast("Lỗi khi gửi text");
        }
    }

    // ==========================
    // Điều hướng tiện ích
    // ==========================
    private void openHomeWithResults(String resultsJson, @Nullable String query, long queryId) {
        HomeFragment homeFragment = HomeFragment.newInstance(resultsJson, query);
        Bundle args = homeFragment.getArguments();
        if (args != null) args.putLong("arg_query_id", queryId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    // giữ bản cũ, forward queryId = -1
    private void openHomeWithResults(String resultsJson, @Nullable String query) {
        openHomeWithResults(resultsJson, query, -1L);
    }
    // ==========================
    // Dialog tiện ích
    // ==========================
    private void showLoadingDialog(String message) {
        if (isFinishing() || isDestroyed()) return;
        if (progressDialog == null) progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        if (!progressDialog.isShowing()) progressDialog.show();
    }

    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try { progressDialog.dismiss(); } catch (Exception ignore) {}
        }
    }

    @Override
    protected void onDestroy() {
        hideLoadingDialog();
        progressDialog = null;
        super.onDestroy();
    }

    // ==========================
    // Helpers
    // ==========================
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private @Nullable String inferQueryFromTop(@Nullable List<Item> items) {
        if (items == null || items.isEmpty()) return null;
        Item it0 = items.get(0);
        String raw = firstNonEmpty(it0.brandName, it0.skuName, it0.description);
        if (raw == null) return null;
        raw = raw.trim().replaceAll("\\s+", " ");
        if (raw.length() > 40) raw = raw.substring(0, 40);  // tránh query quá dài
        return raw.isEmpty() ? null : raw;
    }

    private @Nullable String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null) {
                String t = v.trim();
                if (!t.isEmpty()) return t;
            }
        }
        return null;
    }
}
