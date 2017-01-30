package busyweb.com.androidfacetrackerdemo.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

/**
 * Created by BusyWeb on 8/30/2014.
 */
public class UtilGraphic {


    public static void decodeYUV(int[] out, byte[] fg, int width, int height)
            throws NullPointerException, IllegalArgumentException {
        try {
            final int sz = width * height;
            if (out == null)
                throw new NullPointerException("buffer 'out' is null");
            if (out.length < sz)
                throw new IllegalArgumentException("buffer 'out' size "
                        + out.length + " < minimum " + sz);
            if (fg == null)
                throw new NullPointerException("buffer 'fg' is null");
            if (fg.length < sz)
                throw new IllegalArgumentException("buffer 'fg' size "
                        + fg.length + " < minimum " + sz * 3 / 2);
            int i, j;
            int Y, Cr = 0, Cb = 0;
            for (j = 0; j < height; j++) {
                int pixPtr = j * width;
                final int jDiv2 = j >> 1;
                for (i = 0; i < width; i++) {
                    Y = fg[pixPtr];
                    if (Y < 0)
                        Y += 255;
                    if ((i & 0x1) != 1) {
                        final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
                        Cb = fg[cOff];
                        if (Cb < 0)
                            Cb += 127;
                        else
                            Cb -= 128;
                        Cr = fg[cOff + 1];
                        if (Cr < 0)
                            Cr += 127;
                        else
                            Cr -= 128;
                    }
                    int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                    if (R < 0)
                        R = 0;
                    else if (R > 255)
                        R = 255;
                    int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1)
                            + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
                    if (G < 0)
                        G = 0;
                    else if (G > 255)
                        G = 255;
                    int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
                    if (B < 0)
                        B = 0;
                    else if (B > 255)
                        B = 255;
                    out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts semi-planar YUV420 as generated for camera preview into RGB565
     * format for use as an OpenGL ES texture. It assumes that both the input
     * and output data are contiguous and start at zero.
     *
     * @param yuvs
     *            the array of YUV420 semi-planar data
     * @param rgbs
     *            an array into which the RGB565 data will be written
     * @param width
     *            the number of pixels horizontally
     * @param height
     *            the number of pixels vertically
     */

    // we tackle the conversion two pixels at a time for greater speed
    public static void toRGB565(byte[] yuvs, int width, int height, byte[] rgbs) {
        // the end of the luminance data
        final int lumEnd = width * height;
        // points to the next luminance value pair
        int lumPtr = 0;
        // points to the next chromiance value pair
        int chrPtr = lumEnd;
        // points to the next byte output pair of RGB565 value
        int outPtr = 0;
        // the end of the current luminance scanline
        int lineEnd = width;

        while (true) {

            // skip back to the start of the chromiance values when necessary
            if (lumPtr == lineEnd) {
                if (lumPtr == lumEnd)
                    break; // we've reached the end
                // division here is a bit expensive, but's only done once per
                // scanline
                chrPtr = lumEnd + ((lumPtr >> 1) / width) * width;
                lineEnd += width;
            }

            // read the luminance and chromiance values
            final int Y1 = yuvs[lumPtr++] & 0xff;
            final int Y2 = yuvs[lumPtr++] & 0xff;
            final int Cr = (yuvs[chrPtr++] & 0xff) - 128;
            final int Cb = (yuvs[chrPtr++] & 0xff) - 128;
            int R, G, B;

            // generate first RGB components
            B = Y1 + ((454 * Cb) >> 8);
            if (B < 0)
                B = 0;
            else if (B > 255)
                B = 255;
            G = Y1 - ((88 * Cb + 183 * Cr) >> 8);
            if (G < 0)
                G = 0;
            else if (G > 255)
                G = 255;
            R = Y1 + ((359 * Cr) >> 8);
            if (R < 0)
                R = 0;
            else if (R > 255)
                R = 255;
            // NOTE: this assume little-endian encoding
            rgbs[outPtr++] = (byte) (((G & 0x3c) << 3) | (B >> 3));
            rgbs[outPtr++] = (byte) ((R & 0xf8) | (G >> 5));

            // generate second RGB components
            B = Y2 + ((454 * Cb) >> 8);
            if (B < 0)
                B = 0;
            else if (B > 255)
                B = 255;
            G = Y2 - ((88 * Cb + 183 * Cr) >> 8);
            if (G < 0)
                G = 0;
            else if (G > 255)
                G = 255;
            R = Y2 + ((359 * Cr) >> 8);
            if (R < 0)
                R = 0;
            else if (R > 255)
                R = 255;
            // NOTE: this assume little-endian encoding
            rgbs[outPtr++] = (byte) (((G & 0x3c) << 3) | (B >> 3));
            rgbs[outPtr++] = (byte) ((R & 0xf8) | (G >> 5));
        }
    }

    public static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width,
                                      int height) {
        final int frameSize = width * height;

        try {
            for (int j = 0, yp = 0; j < height; j++) {
                int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & ((int) yuv420sp[yp])) - 16;
                    if (y < 0)
                        y = 0;
                    if ((i & 1) == 0) {
                        v = (0xff & yuv420sp[uvp++]) - 128;
                        u = (0xff & yuv420sp[uvp++]) - 128;
                    }
                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);

                    if (r < 0)
                        r = 0;
                    else if (r > 262143)
                        r = 262143;
                    if (g < 0)
                        g = 0;
                    else if (g > 262143)
                        g = 262143;
                    if (b < 0)
                        b = 0;
                    else if (b > 262143)
                        b = 262143;

                    rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                            | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static void decodeYUV420SP_nv12(int[] rgb, byte[] yuv420sp, int width,
                                      int height) {
        final int frameSize = width * height;
        try {
            for (int j = 0, yp = 0; j < height; j++) {
                int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & ((int) yuv420sp[yp])) - 16;
                    if (y < 0)
                        y = 0;
                    if ((i & 1) == 0) {
                        v = (0xff & yuv420sp[uvp++]) - 128;
                        u = (0xff & yuv420sp[uvp++]) - 128;
                    }
                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);

                    if (r < 0)
                        r = 0;
                    else if (r > 262143)
                        r = 262143;
                    if (g < 0)
                        g = 0;
                    else if (g > 262143)
                        g = 262143;
                    if (b < 0)
                        b = 0;
                    else if (b > 262143)
                        b = 262143;

//                    rgb[yp] = ((r << 6) & 0xff0000)
//                            | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff) | 0xff000000;

                    rgb[yp] = ((r << 14) & 0xff000000) | ((g << 6) & 0xff0000)
                            | ((b >> 2) | 0xff00);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static int[] decodeYuvToRgb(byte[] data, int width, int height) {
        int size = width * height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        for (int i = 0, k = 0; i < size; i+=2, k+=2) {
            y1 = data[i]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i]&0xff;
            y4 = data[width+i+1]&0xff;

            u = data[offset+k]&0xff;
            v = data[offset+k+1]&0xff;
            u = u - 128;
            v = v - 128;

            pixels[i] = YuvToRgb(y1, u, v);
            pixels[i+1] = YuvToRgb(y2,u, v);
            pixels[width+i] = YuvToRgb(y3, u, v);
            pixels[width+i+1] = YuvToRgb(y4, u, v);

            if (i!=0 && (i+2)%width== 0) {
                i+=width;
            }
        }

        return pixels;
    }
    private static int YuvToRgb(int y, int u, int v) {
        int r, g, b;
        r = y + (int)1.402f*v;
        g = y - (int)(0.344f*u + 0.714f*v);
        b = y + (int)1.772f*u;
        r = r > 255 ? 255 : (r < 0 ? 0 : r);
        g = g > 255 ? 255 : (g < 0 ? 0 : g);
        b = b > 255 ? 255 : (b < 0 ? 0 : b);

        return 0xff000000 | (b  <<16) | (g << 8) | r;
    }

    public static byte[] intToByteArray(int value) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (b.length - 1 - i) * 8;
            b[i] = (byte) ((value >>> offset) & 0xFF);
        }
        return b;
    }

    public static final byte[] intToByteArrayB(int value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
                (byte) (value >>> 8), (byte) value };
    }

    public static final int byteArrayToInt(byte[] b) {
        return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8)
                + (b[3] & 0xFF);
    }

    public static boolean checkJpgeData(byte[] data, int dataLength) {
        boolean ret = false;

        if (data.length != dataLength) {
            return ret;
        }
        byte s1 = data[0];
        byte s2 = data[1];
        byte e1 = data[data.length - 2];
        byte e2 = data[data.length -1];

        ret = ((char)s1 == (char)0xffff
                && (char)s2 == (char)0xffd8
                && (char)e1 == (char)0xffff
                && (char)e2 == (char)0xffd9);
        return ret;
    }
    public static boolean checkDataLength(byte[] data, int dataLength) {
        boolean ret = false;

        if (data.length != dataLength) {
            return ret;
        }
        return true;
    }

    /** * Converts YUV420 NV21 to RGB8888 * *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a RGB8888 pixels int array. Where each int is a pixels ARGB. */
    public static int[] convertYUV420_NV21toRGB8888(byte[] data, int width, int height) {
        int size = width * height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;
        // i percorre os Y and the final pixels
        // k percorre os pixles U e V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i ]&0xff;
            y4 = data[width+i+1]&0xff;
            u = data[offset+k ]&0xff;
            v = data[offset+k+1]&0xff;
            u = u-128;
            v = v-128;
            pixels[i ] = convertYUVtoRGB(y1, u, v);
            pixels[i+1] = convertYUVtoRGB(y2, u, v);
            pixels[width+i ] = convertYUVtoRGB(y3, u, v);
            pixels[width+i+1] = convertYUVtoRGB(y4, u, v);
            if (i!=0 && (i+2)%width==0) i+=width;
        }
        return pixels;
    }
    private static int convertYUVtoRGB(int y, int u, int v) {
        int r,g,b;
        r = y + (int)1.402f*v;
        g = y - (int)(0.344f*u +0.714f*v);
        b = y + (int)1.772f*u;
        r = r>255? 255 : r<0 ? 0 : r;
        g = g>255? 255 : g<0 ? 0 : g;
        b = b>255? 255 : b<0 ? 0 : b;
        return 0xff000000 | (b<<16) | (g<<8) | r;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Bitmap YuvToRGBRenderScript(Context context, byte[] data, int width , int height) {
        RenderScript rs;
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(data);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        out.copyTo(bitmap);

        return bitmap;
    }
}
