package com.example.hango.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.hango.MainActivity;
import com.example.hango.R;
import com.example.hango.api.ApiPagedResponse;
import com.example.hango.api.ApiService;
import com.example.hango.api.Item;
import com.example.hango.api.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    // ---- Args
    private static final String ARG_RESULTS_JSON = "search_results_json";
    private static final String ARG_SEARCH_QUERY = "search_query";

    // ---- UI
    private ImageView openCameraButton;
    private EditText  searchEditText;
    private NestedScrollView nestedScrollView;
    private ProgressBar loadingMore;
    private long lastLoadMoreTs = 0L;

    // ---- Data
    private final List<Item> results = new ArrayList<>();
    private final Set<String> seenKeys = new HashSet<>(); // tránh trùng (sku_id + image_path)

    // ---- Paging state cho /search/similar-skus
    private boolean isLoading = false;
    private int currentPage = 0;              // bắt đầu từ 0 để lần gọi đầu = 1
    private static final int PAGE_SIZE = 10;  // nên >=10 để cuộn mượt
    private boolean hasNext = true;           // server báo còn trang không
    private static final double SIMILAR_THRESHOLD = 0.15;

    private long currentQueryId = -1L; // NEW

    private String currentQuery = "";         // query hiện tại để gọi similar-skus

    // ---- API
    private ApiService api;

    /** Factory method */
    public static HomeFragment newInstance(String resultsJson, @Nullable String query) {
        HomeFragment f = new HomeFragment();
        Bundle b = new Bundle();
        b.putString(ARG_RESULTS_JSON, resultsJson);
        if (query != null) b.putString(ARG_SEARCH_QUERY, query);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        api = RetrofitClient.getApiService();

        // --- Tìm kiếm & Camera ---
        searchEditText   = rootView.findViewById(R.id.searchEditText);
        openCameraButton = rootView.findViewById(R.id.openCameraButton);
        loadingMore      = rootView.findViewById(R.id.loading_more);

        if (searchEditText != null) {
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                boolean ime = actionId == EditorInfo.IME_ACTION_SEARCH;
                boolean enter = event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (ime || enter) {
                    String q = searchEditText.getText().toString().trim();
                    if (q.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập từ khóa", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).sendTextToApi(q, 20);
                    }
                    return true;
                }
                return false;
            });
        }

        if (openCameraButton != null) {
            openCameraButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openCameraWithCallback(bitmap ->
                            ((MainActivity) getActivity()).sendImageToApi(bitmap)
                    );
                } else {
                    Toast.makeText(getContext(), "Không thể mở camera", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // --- Scroll để tải thêm similar ---
        nestedScrollView = rootView.findViewById(R.id.main_scroll);
        if (nestedScrollView == null) {
            Log.e(TAG, "main_scroll là null!");
        } else {
            nestedScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        // chỉ quan tâm khi kéo xuống
                        if (scrollY <= oldScrollY) return;
                        if (isLoading || !hasNext) return;

                        // CHỈ kích hoạt khi tới đáy thật sự: không thể cuộn xuống thêm
                        if (!v.canScrollVertically(1)) {
                            long now = android.os.SystemClock.elapsedRealtime();
                            if (now - lastLoadMoreTs < 500) return; // throttle 0.5s
                            lastLoadMoreTs = now;

                            loadMoreSimilarProducts(rootView);
                        }
                    }
            );
        }

        // Nhận dữ liệu ban đầu
        parseArguments();

        // Nếu có query, set vào ô search cho tiện sửa/chạy lại
        if (searchEditText != null && currentQuery != null && !currentQuery.isEmpty()) {
            searchEditText.setText(currentQuery);
            searchEditText.setSelection(searchEditText.getText().length());
        }

        // Hiển thị ngay
        showProductList(rootView);

        if (currentQuery != null && !currentQuery.trim().isEmpty() && hasNext && !isLoading) {
            rootView.postDelayed(() -> loadMoreSimilarProducts(rootView), 120);
        }

        return rootView;
    }

    /** Lấy dữ liệu từ Bundle (kết quả + query) */
    private void parseArguments() {
        results.clear();
        seenKeys.clear();

        // reset paging cho similar-skus
        currentPage = 0;  // lần loadMore đầu sẽ gọi page=1
        hasNext = true;
        isLoading = false;

        Bundle args = getArguments();
        String json = "[]";
        if (args != null) {
            json = args.getString(ARG_RESULTS_JSON, "[]");
            currentQuery = args.getString(ARG_SEARCH_QUERY, "");
            currentQueryId = args.getLong("arg_query_id", -1L); // NEW
        }
        Log.d(TAG, "queryId=" + currentQueryId + ", query='" + currentQuery + "'");

        Type listType = new TypeToken<List<Item>>(){}.getType();
        List<Item> init = new Gson().fromJson(json, listType);
        if (init != null) {
            for (Item it : init) addIfNotExists(it);
        }
        // KHÔNG đoán hasNext dựa vào kích thước init — similar-skus là endpoint khác.
        if ((currentQuery == null || currentQuery.trim().isEmpty()) && !results.isEmpty()) {
            String guessed = inferQueryFrom(results.get(0)); // lấy từ item top-1
            if (guessed != null) {
                currentQuery = guessed;
                Log.d(TAG, "🔎 inferred query from image result: " + currentQuery);
            }
        }

    }

    /** Thêm item nếu chưa có (tránh trùng khi load more) */
    private boolean addIfNotExists(Item item) {
        String key = (item.skuId != null ? item.skuId : -1L) + "|" + (item.imagePath != null ? item.imagePath : "");
        if (seenKeys.contains(key)) return false;
        seenKeys.add(key);
        results.add(item);
        return true;
    }

    /** Gọi /search/similar-skus (paging=page, page_size) để tải thêm */
    private void loadMoreSimilarProducts(View rootView) {
        if (currentQuery == null || currentQuery.trim().isEmpty()) {
            Log.d(TAG, "❌ Không có query nên không gọi similar");
            return;
        }
        if (isLoading || !hasNext) {
            Log.d(TAG, "⏳ isLoading=" + isLoading + ", hasNext=" + hasNext);
            return;
        }

        isLoading = true;
        if (loadingMore != null) loadingMore.setVisibility(View.VISIBLE);

        final int nextPage = currentPage + 1;
        Log.d(TAG, "⬇️ /search/similar-skus q=" + currentQuery + " page=" + nextPage + " page_size=" + PAGE_SIZE);

        api.getSimilarSkus(currentQuery, nextPage, PAGE_SIZE, SIMILAR_THRESHOLD)
                .enqueue(new Callback<ApiPagedResponse<Item>>() {
                    @Override
                    public void onResponse(Call<ApiPagedResponse<Item>> call, Response<ApiPagedResponse<Item>> response) {
                        isLoading = false;
                        if (loadingMore != null) loadingMore.setVisibility(View.GONE);
                        if (!isAdded()) return;

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e(TAG, "❌ similar-skus HTTP " + response.code());
                            Toast.makeText(requireContext(), "Không thể tải thêm sản phẩm", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ApiPagedResponse<Item> body = response.body();
                        if (!body.isOk()) {
                            Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int added = 0;
                        List<Item> list = (body.results != null) ? body.results : new ArrayList<>();
                        for (Item it : list) if (addIfNotExists(it)) added++;

                        // cập nhật paging từ server
                        if (body.paging != null) {
                            hasNext = body.paging.hasNext;
                            if (added > 0) currentPage = body.paging.page; // chỉ tăng trang khi có dữ liệu mới
                        } else {
                            // fallback nếu server không trả paging (không nên xảy ra)
                            hasNext = list.size() >= PAGE_SIZE;
                            if (added > 0) currentPage = nextPage;
                        }

                        Log.d(TAG, "✅ Added=" + added + "/" + list.size() +
                                " | page=" + currentPage + " | hasNext=" + hasNext);

                        if (added > 0) {
                            showProductList(rootView);
                        } else if (!hasNext) {
                            Toast.makeText(requireContext(), "Không còn sản phẩm tương tự", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiPagedResponse<Item>> call, Throwable t) {
                        isLoading = false;
                        if (loadingMore != null) loadingMore.setVisibility(View.GONE);
                        if (!isAdded()) return;

                        Log.e(TAG, "❌ Lỗi tải thêm: " + (t.getMessage() != null ? t.getMessage() : "unknown"));
                        Toast.makeText(requireContext(), "Lỗi tải thêm: " + (t.getMessage() != null ? t.getMessage() : "unknown"), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Render danh sách */
    private void showProductList(View rootView) {
        Log.d(TAG, "showProductList, results=" + results.size() + ", hasNext=" + hasNext);
        LinearLayout container = rootView.findViewById(R.id.content_container);
        if (container == null || getContext() == null) return;
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String base = RetrofitClient.getBaseUrl();

        if (results.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Không có kết quả.");
            empty.setTextColor(0xFF5F6368);
            empty.setPadding(24, 24, 24, 24);
            container.addView(empty);
            return;
        }

        for (Item item : results) {
            Log.d(TAG, "add item sku=" + item.skuId + " name=" + item.skuName + " img=" + item.imagePath);
            View itemView = inflater.inflate(R.layout.product_item, container, false);

            TextView similarityView = itemView.findViewById(R.id.productSimilarity);
            ImageView imageView     = itemView.findViewById(R.id.productImage);
            TextView nameView       = itemView.findViewById(R.id.productName);
            TextView descView       = itemView.findViewById(R.id.categoryName);
            TextView priceView      = itemView.findViewById(R.id.productPrice);
            ImageView btnAdd        = itemView.findViewById(R.id.btnAddProduct); // nút thêm sản phẩm

            // ===== Độ tương đồng =====
            Double scorePct = computeScorePct(item);
            if (similarityView != null && scorePct != null) {
                double pct = Math.max(0.0, Math.min(100.0, scorePct));
                similarityView.setText(String.format("Độ tương đồng: %.2f%%", pct));
                similarityView.setVisibility(View.VISIBLE);
                if (pct >= 85)      similarityView.setTextColor(0xFF1A73E8);
                else if (pct >= 70) similarityView.setTextColor(0xFF5F6368);
                else                similarityView.setTextColor(0xFF9AA0A6);
            } else if (similarityView != null) {
                similarityView.setVisibility(View.GONE);
            }

            // ===== Ảnh =====
            String fullUrl = buildImageUrl(base, item.imagePath);
            if (fullUrl != null && !fullUrl.trim().isEmpty()) {
                Glide.with(requireContext())
                        .load(fullUrl)
                        .placeholder(R.drawable.hango_logo)
                        .error(R.drawable.hango_logo)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.hango_logo);
            }

            // ===== Tên =====
            if (nameView != null) nameView.setText(getOrDefault(item.skuName));

            // ===== Mô tả =====
            String desc = firstNonEmpty(item.description, item.text, item.ocrText, item.brandName);
            if (descView != null) descView.setText(getOrDefault(desc));

            // ===== Giá =====
            if (priceView != null) {
                String priceText = buildPriceText(item);
                priceView.setText(priceText);
            }


            // ===== Click vào card -> log click =====
            final Long skuId = item.skuId;
            itemView.setOnClickListener(v -> {
                if (currentQueryId > 0 && skuId != null) {
                    sendCandidateAction("click", skuId, null);
                } else {
                    Log.w(TAG, "Bỏ qua click event: queryId/skuId không hợp lệ. q=" + currentQueryId + " sku=" + skuId);
                }
                // TODO: mở màn hình chi tiết nếu cần
            });

            // ===== Long-click -> dwell demo =====
            itemView.setOnLongClickListener(v -> {
                if (currentQueryId > 0 && skuId != null) {
                    sendCandidateAction("dwell", skuId, 1.5);
                    Toast.makeText(requireContext(), "Đã ghi nhận xem lâu", Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            // ===== NÚT THÊM SẢN PHẨM VÀO GIỎ =====
            if (btnAdd != null) {
                btnAdd.setOnClickListener(v -> {
                    // lưu local + persist (CartManager đã dùng SharedPreferences)
                    com.example.hango.data.CartManager
                            .getInstance()
                            .addItem(requireContext(), item);

                    Toast.makeText(
                            requireContext(),
                            "Đã thêm vào giỏ: " + getOrDefault(item.skuName),
                            Toast.LENGTH_SHORT
                    ).show();

                    // optional: log event để training
                    if (currentQueryId > 0 && item.skuId != null) {
                        sendCandidateAction("add_to_cart", item.skuId, null);
                    }
                });
            }

            container.addView(itemView);
        }
    }


    /** Tính % độ tương đồng từ score/similarity (0..100). Trả null nếu không có. */
    private Double computeScorePct(Item item) {
        try {
            if (item == null) return null;
            if (item.score != null) return item.score * 100.0;
            if (item.similarity != null) return item.similarity * 100.0;
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }

    /** Lấy phần tử đầu tiên khác null/không rỗng. */
    private String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null) {
                String t = v.trim();
                if (!t.isEmpty()) return t;
            }
        }
        return null;
    }

    private String buildPriceText(Item item) {
        if (item == null) return "Giá: Đang cập nhật";

        // Ưu tiên lần lượt các field có giá trị
        Number price = null;

        if (item.price != null) {
            price = item.price;
        } else if (item.sellingPrice != null) {
            price = item.sellingPrice;
        } else if (item.unitPrice != null) {
            price = item.unitPrice;
        }

        if (price != null) {
            return "Giá: " + formatPriceVnd(price);
        }
        return "Giá: Đang cập nhật";
    }


    /** Format tiền VND (khi có giá thật). */
    private String formatPriceVnd(Number price) {
        if (price == null) return "Đang cập nhật";
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return nf.format(price.longValue()) + "₫";
    }

    private @Nullable String inferQueryFrom(@Nullable Item it) {
        if (it == null) return null;
        // Ưu tiên: brand -> skuName -> description
        String raw = firstNonEmpty(it.brandName, it.skuName, it.description);
        if (raw == null) return null;
        raw = raw.trim();
        // Cắt ngắn để tránh query quá dài
        if (raw.length() > 40) raw = raw.substring(0, 40);
        // Loại bỏ ký tự lạ cơ bản
        raw = raw.replaceAll("\\s+", " ");
        return raw.isEmpty() ? null : raw;
    }


    private void sendCandidateAction(String action, Long skuId, @Nullable Double dwellSeconds) {
        if (!isAdded() || getContext() == null) return;
        if (currentQueryId <= 0 || skuId == null) {
            Log.w(TAG, "Bỏ qua event: thiếu queryId/skuId. q=" + currentQueryId + " sku=" + skuId);
            return;
        }
        ApiService api = RetrofitClient.getApiService();
        com.example.hango.api.CandidateActionRequest body =
                new com.example.hango.api.CandidateActionRequest(currentQueryId, skuId, action, dwellSeconds);

        api.sendCandidateAction(body).enqueue(new retrofit2.Callback<com.example.hango.api.ApiOkResponse>() {
            @Override public void onResponse(
                    retrofit2.Call<com.example.hango.api.ApiOkResponse> call,
                    retrofit2.Response<com.example.hango.api.ApiOkResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null && Boolean.TRUE.equals(resp.body().ok)) {
                    Log.d(TAG, "✅ event sent: " + action + " q=" + currentQueryId + " sku=" + skuId + " dwell=" + dwellSeconds);
                } else {
                    String msg = (resp.body()!=null && resp.body().error!=null)? resp.body().error : ("HTTP " + resp.code());
                    Log.w(TAG, "⚠️ event failed: " + msg);
                }
            }
            @Override public void onFailure(retrofit2.Call<com.example.hango.api.ApiOkResponse> call, Throwable t) {
                Log.e(TAG, "❌ event error: " + (t.getMessage()!=null?t.getMessage():"unknown"));
            }
        });
    }

    private @Nullable String buildImageUrl(@Nullable String base, @Nullable String imagePath) {
        if (imagePath == null) return null;
        String p = imagePath.trim();
        if (p.isEmpty()) return null;
        if (p.startsWith("http://") || p.startsWith("https://")) return p;

        if (base == null || base.trim().isEmpty()) return null;
        String b = base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        while (p.startsWith("/")) p = p.substring(1);
        if (!(p.startsWith("uploads/") || p.startsWith("static/uploads/"))) {
            p = "uploads/" + p;
        }
        return b + "/" + p;
    }

    private String getOrDefault(String v) {
        return (v != null && !v.trim().isEmpty()) ? v : "Không rõ";
    }
}