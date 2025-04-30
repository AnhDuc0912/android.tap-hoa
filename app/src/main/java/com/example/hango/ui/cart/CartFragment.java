package com.example.hango.ui.cart;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.hango.R;

import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment {

    private ImageView openCameraButton;
    private ImageView addProductButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout cho fragment_cart
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        // Lấy reference cho các nút
        openCameraButton = view.findViewById(R.id.openCameraButton);
        addProductButton = view.findViewById(R.id.addProductButton);

        // Đặt sự kiện click cho nút mở camera
        openCameraButton.setOnClickListener(v -> openCamera());

        // Đặt sự kiện click cho nút thêm sản phẩm
        addProductButton.setOnClickListener(v -> showAddProductDialog());

        return view;
    }

    private void showAddProductDialog() {
        // 1. Inflate layout custom
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_product, null, false);

        // 2. Lấy các view con
        EditText etName        = dialogView.findViewById(R.id.etProductName);
        EditText etPrice       = dialogView.findViewById(R.id.etPrice);
        Button btnChooseImage  = dialogView.findViewById(R.id.btnChooseImage);
        EditText etFeature = dialogView.findViewById(R.id.etFeature);
        ImageButton btnAddFeat = dialogView.findViewById(R.id.btnAddFeature);
        LinearLayout featuresList = dialogView.findViewById(R.id.llFeaturesList);

        // Danh sách lưu features tạm
        List<String> features = new ArrayList<>();

        btnAddFeat.setOnClickListener(v -> {
            String feature = etFeature.getText().toString().trim();
            if (!feature.isEmpty()) {
                features.add(feature);
                // Ví dụ thêm một chip đơn giản
                TextView chip = new TextView(requireContext());
                chip.setText(feature);
                chip.setPadding(16,8,16,8);
                chip.setBackgroundResource(R.drawable.bg_chip);
                featuresList.addView(chip);
                etFeature.setText("");
            }
        });

        btnChooseImage.setOnClickListener(v -> {
            // TODO: mở file picker hoặc camera
        });

        // 3. Tạo AlertDialog với custom view
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add New Product")
                .setView(dialogView)
                .setPositiveButton("Add Product", (d, which) -> {
                    // Thu thập dữ liệu và xử lý
                    String name = etName.getText().toString();
                    String price= etPrice.getText().toString();
                    // features list đã cập nhật
                    // TODO: lưu hoặc gửi lên server
                    Toast.makeText(requireContext(), "Product added!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.show();
    }


    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Nếu quyền được cấp, mở camera
                    openCamera();
                } else {
                    // Nếu quyền không được cấp, hiển thị thông báo
                    Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    // Khai báo launcher để mở camera
    private final ActivityResultLauncher<Intent> cameraActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    // Xử lý kết quả từ camera (nếu cần)
                    Toast.makeText(requireContext(), "Camera photo captured!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void openCamera() {
        // Kiểm tra quyền truy cập camera
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Nếu đã cấp quyền, mở camera
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraActivityLauncher.launch(intent);
        } else {
            // Nếu chưa cấp quyền, yêu cầu cấp quyền
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }
}
