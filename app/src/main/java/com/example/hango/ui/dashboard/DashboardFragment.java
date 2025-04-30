package com.example.hango.ui.dashboard;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.hango.R;
import com.example.hango.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private ImageView openCameraButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout cho fragment_home
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Lấy reference của nút mở camera
        openCameraButton = view.findViewById(R.id.openCameraButton);

        // Đặt sự kiện click cho nút
        openCameraButton.setOnClickListener(v -> openCamera());

        return view;
    }

    private void openCamera() {
        // Kiểm tra quyền truy cập camera
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Nếu đã cấp quyền, mở camera
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraActivityLauncher.launch(intent);
        } else {
            // Nếu chưa cấp quyền, yêu cầu cấp quyền
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }
    // Khai báo launcher cho yêu cầu quyền truy cập camera
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Nếu quyền được cấp, mở camera
                    openCamera();
                } else {
                    // Nếu quyền không được cấp, hiển thị thông báo
                    Toast.makeText(getContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Khai báo launcher để mở camera
    private final ActivityResultLauncher<Intent> cameraActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    // Xử lý kết quả từ camera (nếu cần)
                    Toast.makeText(getContext(), "Camera photo captured!", Toast.LENGTH_SHORT).show();
                }
            }
    );
}