package com.mobilkod.barcode;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import com.getcapacitor.*;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;

@CapacitorPlugin(
    name = "Barcode",
    permissions = {
        @Permission(alias = "camera", strings = { android.Manifest.permission.CAMERA })
    }
)
public class BarcodePlugin extends Plugin implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private BarcodeScanner barcodeScanner;
    private PluginCall savedCall;
    private boolean isProcessing = false;

    @PluginMethod
    public void scan(PluginCall call) {
        savedCall = call;
        isProcessing = false;

        if (getPermissionState("camera") != PermissionState.GRANTED) {
            requestPermissionForAlias("camera", call, "permissionCallback");
            return;
        }

        startCamera();
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            startCamera();
        } else {
            call.reject("Camera permission denied");
        }
    }

    private void startCamera() {
        Activity activity = getActivity();

        surfaceView = new SurfaceView(activity);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        activity.runOnUiThread(() -> {
            FrameLayout layout = new FrameLayout(activity);
            layout.addView(surfaceView);
            activity.addContentView(layout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));
        });

        // Tüm barcode formatlarını destekleyen scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build();

        barcodeScanner = BarcodeScanning.getClient(options);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            camera = Camera.open();
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);

            Camera.Parameters params = camera.getParameters();

            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            camera.setParameters(params);
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (IOException e) {
            if (savedCall != null) {
                savedCall.reject("Camera initialization failed: " + e.getMessage());
            }
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isProcessing) {
            return; // Önceki tarama bitmeden yeni tarama başlamasın
        }
        isProcessing = true;

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();

        InputImage image = InputImage.fromByteArray(
                data,
                size.width,
                size.height,
                90,
                ImageFormat.NV21
        );

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.size() > 0) {
                        Barcode barcode = barcodes.get(0);
                        String rawValue = barcode.getRawValue();

                        JSObject ret = new JSObject();
                        ret.put("value", rawValue);
                        ret.put("format", barcode.getFormat());

                        stopCamera();

                        if (savedCall != null && !savedCall.isReleased()) {
                            savedCall.resolve(ret);
                            savedCall = null;
                        }

                        isProcessing = false; // Tarama tamamlandı, yeni tarama için aç
                    } else {
                        isProcessing = false; // Hiç barkod yok, yeniden tarama için aç
                    }
                })
                .addOnFailureListener(e -> {
                    stopCamera();

                    if (savedCall != null && !savedCall.isReleased()) {
                        savedCall.reject("Barcode scanning failed: " + e.getMessage());
                        savedCall = null;
                    }
                    isProcessing = false; // Hata olsa da flag'i sıfırla
                });
    }
    private void stopCamera() {
        Activity activity = getActivity();
        activity.runOnUiThread(() -> {
            try {
                if (camera != null) {
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

                if (barcodeScanner != null) {
                    barcodeScanner.close();
                    barcodeScanner = null;
                }

                if (surfaceView != null) {
                    if (surfaceView.getParent() != null) {
                        ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
                    }
                    surfaceView = null;
                    surfaceHolder = null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        stopCamera();
    }

    @Override
    public void load() {
        super.load();

        getActivity().getOnBackPressedDispatcher().addCallback(getActivity(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (camera != null) {
                    stopCamera();

                    if (savedCall != null && !savedCall.isReleased()) {
                        savedCall.reject("Scan cancelled by user");
                        savedCall = null;
                    }
                } else {
                    surfaceView.setEnabled(false);
                    getActivity().onBackPressed();
                    surfaceView.setEnabled(true);
                }
            }
        });
    }


}
