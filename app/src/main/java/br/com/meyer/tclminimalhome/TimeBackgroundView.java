package br.com.meyer.tclminimalhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.view.View;
import java.util.Calendar;

/** A local, zero-network wallpaper that follows the time of day. */
final class TimeBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap onlineImage;

    TimeBackgroundView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setOnlineImage(Bitmap image) {
        onlineImage = image;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int top;
        int bottom;
        if (hour >= 6 && hour < 11) { top = Color.rgb(20, 29, 44); bottom = Color.rgb(47, 69, 91); }
        else if (hour >= 11 && hour < 17) { top = Color.rgb(16, 25, 38); bottom = Color.rgb(34, 57, 78); }
        else if (hour >= 17 && hour < 21) { top = Color.rgb(21, 25, 42); bottom = Color.rgb(42, 43, 73); }
        else { top = Color.rgb(8, 12, 20); bottom = Color.rgb(17, 25, 39); }
        paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(), top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
        if (onlineImage != null) {
            int imageWidth = onlineImage.getWidth();
            int imageHeight = onlineImage.getHeight();
            float scale = Math.max((float) getWidth() / imageWidth, (float) getHeight() / imageHeight);
            int drawnWidth = (int) (getWidth() / scale);
            int drawnHeight = (int) (getHeight() / scale);
            int left = Math.max(0, (imageWidth - drawnWidth) / 2);
            int topCrop = Math.max(0, (imageHeight - drawnHeight) / 2);
            paint.setAlpha(190);
            canvas.drawBitmap(onlineImage, new Rect(left, topCrop, left + drawnWidth, topCrop + drawnHeight),
                    new Rect(0, 0, getWidth(), getHeight()), paint);
            paint.setAlpha(255);
            paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                    Color.argb(165, 5, 10, 20), Color.argb(95, 9, 16, 28), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
        }
        paint.setColor(Color.argb(34, 141, 180, 255));
        canvas.drawCircle(getWidth() * .83f, getHeight() * .14f, getHeight() * .34f, paint);
        paint.setColor(Color.argb(16, 255, 255, 255));
        canvas.drawCircle(getWidth() * .75f, getHeight() * .07f, getHeight() * .25f, paint);
    }
}
