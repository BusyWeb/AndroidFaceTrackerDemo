package busyweb.com.androidfacetrackerdemo.interfaces;

/**
 * Created by BusyWeb on 1/29/2017.
 */

public interface CameraPreviewEvents {

    public void PreviewBitmapReady();
    public void PreviewFrameReady(byte[] data);

}
