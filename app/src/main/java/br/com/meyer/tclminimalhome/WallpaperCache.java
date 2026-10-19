package br.com.meyer.tclminimalhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;

/** Stores the last successful wallpaper so Home can render before the network responds. */
final class WallpaperCache {
    private static final String FILE_NAME = "casa-wallpaper.jpg";

    static Bitmap read(Context context) {
        try {
            File file = new File(context.getCacheDir(), FILE_NAME);
            return file.exists() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
        } catch (Exception ignored) { return null; }
    }

    static void write(Context context, Bitmap image) {
        try {
            File target = new File(context.getCacheDir(), FILE_NAME);
            File temporary = new File(context.getCacheDir(), FILE_NAME + ".new");
            FileOutputStream stream = new FileOutputStream(temporary);
            image.compress(Bitmap.CompressFormat.JPEG, 84, stream);
            stream.close();
            if (!temporary.renameTo(target)) temporary.delete();
        } catch (Exception ignored) { }
    }

    private WallpaperCache() { }
}
