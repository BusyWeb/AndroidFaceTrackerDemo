package busyweb.com.androidfacetrackerdemo.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;

import busyweb.com.androidfacetrackerdemo.app.Shared;

/**
 * Created by BusyWeb on 1/29/2017.
 */

public class UtilGeneralHelper {

    public static void CreateAppFolder() {
        try {
            File appFolder = new File(Shared.RootFolder);
            if (!appFolder.exists()) {
                appFolder.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean SaveOverlayBitmap(View overlay, Bitmap preview) {
        boolean success = false;
        try {

            Bitmap bitmapOverlay = GetViewBitmap(overlay);

            Bitmap bitmap = Bitmap.createBitmap(preview.getWidth(), preview.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            Rect rect = new Rect();
            rect.left = 0;
            rect.top = 0;
            rect.right = bitmap.getWidth();
            rect.bottom = bitmap.getHeight();
            canvas.drawBitmap(preview, null, rect, null);
            canvas.drawBitmap(bitmapOverlay, null, rect, null);

            String fileName = String.valueOf(System.currentTimeMillis()) + ".jpg";
            Shared.LastSavedFilePath = Shared.RootFolder + fileName;
            File file = new File(Shared.LastSavedFilePath);
            FileOutputStream out = new FileOutputStream(file);
            success = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public static boolean SaveBitmapToFile(Bitmap bitmap, File file) {
        boolean success = false;
        try {


            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public static Bitmap GetViewBitmap(View v) {
        Bitmap viewBitmap = null;
        try {
            //v.invalidate();
            boolean cacheEnabled = false;
            if (!v.isDrawingCacheEnabled()){
                v.setDrawingCacheEnabled(true);
                cacheEnabled = false;
            }
            v.buildDrawingCache();

            viewBitmap = v.getDrawingCache().copy(Bitmap.Config.ARGB_8888, false);

            if (!cacheEnabled) {
                v.setDrawingCacheEnabled(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return viewBitmap;
    }

    public static Bitmap ScaledBitmap = null;
    public static Bitmap GetScaledBitmap(Bitmap bitmap, int widthOutput, int heightOutput) {
        //Bitmap ret = null;
        try {
            ScaledBitmap = Bitmap.createScaledBitmap(bitmap, widthOutput, heightOutput, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ScaledBitmap;
    }

    public static Bitmap GetBitmapMutable(Bitmap source) {
        Bitmap b = null;
        try {
            b = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            Rect rect = new Rect();
            rect.left = 0;
            rect.top = 0;
            rect.right = b.getWidth();
            rect.bottom = b.getHeight();
            canvas.drawBitmap(source, null, rect, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    public static int GetDisplayPixel(Context context, int dp){
        // Use mGestureThreshold as a distance in pixels
        //return (int)(dp * (this.getResources().getDisplayMetrics().density / 160) * dp);
        return (int)(dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

}
