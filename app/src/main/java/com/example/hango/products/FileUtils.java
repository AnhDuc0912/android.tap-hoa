package com.example.hango.products;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class FileUtils {
    public static File createTempImageFile(Context context, Bitmap bitmap) {
        try {
            File file = new File(context.getCacheDir(), "upload.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] readAllBytes(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream in = resolver.openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    public static MultipartBody.Part buildImagePart(byte[] bytes, String fieldName, String fileName, String mime) {
        MediaType mediaType = MediaType.parse(mime != null ? mime : "image/jpeg");
        RequestBody fileBody = RequestBody.create(mediaType, bytes);
        return MultipartBody.Part.createFormData(fieldName, fileName, fileBody);
    }

    public static RequestBody buildTextPart(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }
}
