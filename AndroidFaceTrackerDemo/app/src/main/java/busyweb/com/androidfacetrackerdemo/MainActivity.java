package busyweb.com.androidfacetrackerdemo;

/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import busyweb.com.androidfacetrackerdemo.app.Shared;
import busyweb.com.androidfacetrackerdemo.interfaces.CameraPreviewEvents;
import busyweb.com.androidfacetrackerdemo.ui.camera.CameraSource;
import busyweb.com.androidfacetrackerdemo.ui.camera.CameraSourcePreview;
import busyweb.com.androidfacetrackerdemo.ui.camera.GraphicOverlay;
import busyweb.com.androidfacetrackerdemo.util.UtilGeneralHelper;
import busyweb.com.androidfacetrackerdemo.util.UtilGraphic;
import busyweb.com.androidfacetrackerdemo.vision.MyFaceDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */

public final class MainActivity extends AppCompatActivity {
    private static final String TAG = "FaceTracker";

    private CameraSource mCameraSource = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private ImageView mImageViewFrame;
    private ImageButton mImageButtonSave;
    private ImageButton mImageButtonSwitch;
    private Switch mSwitchLandmark;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // this is for the older api
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        Shared.gActivity = this;
        Shared.gContext = this;

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        mImageViewFrame = (ImageView) findViewById(R.id.imageViewFrame);
        mImageViewFrame.setOnClickListener(TrackerSaveListener);

        mImageButtonSave = (ImageButton) findViewById(R.id.imageButtonSaveTracker);
        mImageButtonSave.setOnClickListener(TrackerSaveListener);

        mImageButtonSwitch = (ImageButton) findViewById(R.id.imageButtonSwitchCamera);
        mImageButtonSwitch.setOnClickListener(SwitchCameraListener);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA) + ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
            prepareApp();
        } else {
            requestCameraPermission();
        }

        mSwitchLandmark = (Switch) findViewById(R.id.switchLandmark);
        mSwitchLandmark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Shared.DrawLandmark = b;
            }
        });
    }

    private void prepareApp() {
        try {
            UtilGeneralHelper.CreateAppFolder();

            prepareCameraPreviewEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener TrackerSaveListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                boolean success = UtilGeneralHelper.SaveOverlayBitmap(mGraphicOverlay, Shared.gFrameBitmap);

                if (success) {
                    //Toast.makeText(Shared.gContext, "Tracker images saved.", Toast.LENGTH_LONG).show();
                    Snackbar.make(mGraphicOverlay, "Tracker image saved.", Snackbar.LENGTH_LONG)
                            .setAction("VIEW", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    File file = new File(Shared.LastSavedFilePath);
                                    Uri uri = Uri.fromFile(file);
                                    String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
                                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(uri, mimeType);
                                    startActivity(Intent.createChooser(intent, "Select viewer"));
                                }
                            }).show();
                } else {
                    Toast.makeText(Shared.gContext, "Failed to save tracker image.", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @SuppressWarnings("deprecation")
    private View.OnClickListener SwitchCameraListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {

//
//                if (mPreview != null) {
//                    mPreview.stop();
//                    //mPreview.release();
//                    mPreview.RefreshTextureView(Shared.gContext);
//                }
                if (mCameraSource != null) {
                    mCameraSource.release();
                    mCameraSource = null;
                }
                if (Shared.gFaceDetector != null) {
                    Shared.gFaceDetector.release();
                }
                if (Shared.gMyFaceDetector != null) {
                    Shared.gMyFaceDetector.release();
                }
                if (Shared.CameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    Shared.CameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                } else {
                    Shared.CameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
                }

//                if (mPreview != null) {
//                    mPreview.RefreshTextureView(getApplicationContext());
//                }
                createCameraSource();
                startCameraSource(true);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };


        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, listener)
                        .show();

        } else {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
        }


    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        Shared.gFaceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        Shared.gMyFaceDetector = new MyFaceDetector(Shared.gFaceDetector);

//        Shared.gFaceDetector.setProcessor(
//                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
//                        .build());
        Shared.gMyFaceDetector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());


        if (!Shared.gMyFaceDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

//        mCameraSource = new CameraSource.Builder(context, Shared.gMyFaceDetector)
//                .setRequestedPreviewSize(640, 480)
//                .setFacing(CameraSource.CAMERA_FACING_BACK)
//                .setRequestedFps(30.0f)
//                .build();

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), Shared.gMyFaceDetector)
                .setFacing(Shared.CameraFacing)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f);

        // make sure that auto focus is an available option
        boolean autoFocus = true;
        boolean useFlash = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();


    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource(false);
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Shared.gFaceDetector != null) {
            Shared.gFaceDetector.release();
        }

        if (mPreview != null) {
            mPreview.release();
        }
        if (mCameraSource != null) {
            mCameraSource = null;
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

//        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "Camera permission granted - initialize the camera source");
//            // we have permission, so create the camerasource
//            createCameraSource();
//            return;
//        }

        if (grantResults.length != 0) {
            boolean camera = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean storage = grantResults[1] == PackageManager.PERMISSION_GRANTED;

            if (camera && storage) {
                Log.d(TAG, "Camera and storage write permission granted - initialize the camera source");
                createCameraSource();

                prepareApp();

                startCameraSource(false);
                return;
            }
        }


        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker Demo")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource(boolean refresh) throws SecurityException {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {

                mPreview.start(mCameraSource, mGraphicOverlay, refresh);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
            Log.i("DBG", "onNewItem");
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            Log.i("DBG", "onUpdate");
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }


    private void prepareCameraPreviewEvents() {
        try {
            if (Shared.gPreviewEvents == null) {
            }
            Shared.gPreviewEvents = new CameraPreviewEvents() {
                @Override
                public void PreviewBitmapReady() {
                    Shared.gActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mImageViewFrame.setImageBitmap(Shared.gFrameBitmap);
                        }
                    });
                }

                @Override
                public void PreviewFrameReady(byte[] data) {
                    try {
                        // using SurfaceView preview data conversion to bitmap
                        // does not work well on some unknown reason
                        // byte buffer size might be different than expected???

//                            UtilGraphic.decodeYUV(
//                                    Shared.gPreviewDataInt,
//                                    data,
//                                    Shared.gPreviewWidth,
//                                    Shared.gPreviewHeight);
//                            Shared.gFrameBitmap = Bitmap.createBitmap(
//                                    Shared.gPreviewDataInt,
//                                    Shared.gPreviewWidth,
//                                    Shared.gPreviewHeight,
//                                    Bitmap.Config.RGB_565
//                            );

//                            Shared.gPreviewFrameData = data;
//
//                            YuvImage yuvImage = new YuvImage(
//                                    Shared.gPreviewFrameData,
//                                    ImageFormat.NV21,
//                                    Shared.gPreviewWidth,
//                                    Shared.gPreviewHeight,
//                                    null
//                            );
//                            ByteArrayOutputStream out = new ByteArrayOutputStream();
//                            yuvImage.compressToJpeg(new Rect(0, 0, Shared.gPreviewWidth, Shared.gPreviewHeight), 95, out);
//                            byte[] bitmapData = out.toByteArray();
//                            Shared.gFrameBitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
//
//                            Shared.gActivity.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    mImageViewFrame.setImageBitmap(Shared.gFrameBitmap);
//                                }
//                            });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
