package com.example.cuisine_finder.utils;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageStorageUtils {
    /**
     * Copy a selected image from a content URI (e.g., content://) to the app's internal files directory,
     * so that it has persistent local read permissions, yielding a file:// URI.
     *
     * @param context    The android context
     * @param uri        The source image Uri
     * @param folderName The subfolder name inside files directory (e.g. "stories", "avatars")
     * @return The local file URI string, or the original URI string on error
     */
    public static String saveImageToInternalStorage(Context context, Uri uri, String folderName) {
        if (uri == null || context == null) return null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return uri.toString();
            
            File directory = new File(context.getFilesDir(), folderName);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            File file = new File(directory, System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            return Uri.fromFile(file).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return uri.toString();
        }
    }
}
