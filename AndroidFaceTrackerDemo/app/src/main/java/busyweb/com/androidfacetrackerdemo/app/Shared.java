package busyweb.com.androidfacetrackerdemo.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Environment;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.FaceDetector;

import java.nio.ByteBuffer;

import busyweb.com.androidfacetrackerdemo.interfaces.CameraPreviewEvents;
import busyweb.com.androidfacetrackerdemo.vision.MyFaceDetector;

/**
 * Created by BusyWeb on 1/29/2017.
 */
@SuppressWarnings("deprecation")
public class Shared {

    public static Context gContext;
    public static Activity gActivity;

    public static String RootFolder = Environment.getExternalStorageDirectory().toString() + "/facetrackerdemo/";
    public static String LastSavedFilePath = RootFolder;

    public static int CameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;

    public static FaceDetector gFaceDetector;
    public static MyFaceDetector gMyFaceDetector;
    public static Frame gFrame;
    public static Bitmap gFrameBitmap;
    public static int gPreviewWidth;
    public static int gPreviewHeight;
    public static int[] gPreviewDataInt;
    public static byte[] gPreviewFrameData;

    public static CameraPreviewEvents gPreviewEvents;

    public static boolean DrawLandmark = true;

}
