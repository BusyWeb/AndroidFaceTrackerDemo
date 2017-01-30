package busyweb.com.androidfacetrackerdemo.vision;

import android.graphics.Bitmap;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;


/**
 * Created by BusyWeb on 1/29/2017.
 */

public class MyFaceDetector extends Detector<Face> {

    private Detector<Face> mDetector;
    private Frame mFrame;
    private static Bitmap mBitmap;


    public MyFaceDetector(Detector<Face> detector) {
        mDetector = detector;
    }

    public Bitmap GetFrameBitmap() {
        if (mFrame != null) {
            return mFrame.getBitmap();
        }
        return null;
    }

    @Override
    public SparseArray<Face> detect(Frame frame) {
        mFrame = frame;
        // after return frame, the bitmap data will be reset
        return mDetector.detect(frame);
    }

    @Override
    public boolean isOperational() {
        return mDetector.isOperational();
    }

    @Override
    public boolean setFocus(int id) {
        return mDetector.setFocus(id);
    }

}
