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

import com.example.hango.ui.cart.CartFragment;
import com.example.hango.ui.dashboard.DashboardFragment;
import com.example.hango.ui.home.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
}