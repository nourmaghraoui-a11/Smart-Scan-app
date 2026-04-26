package com.example.myapplication.UI;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.util.concurrent.Executors;

public class CuptueActivity extends AppCompatActivity {

    PreviewView previewView;
    ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_capture);

        previewView = findViewById(R.id.previewView);
        Button btn = findViewById(R.id.btnCapture);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            startCamera();
        }

        btn.setOnClickListener(v -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> f =
                ProcessCameraProvider.getInstance(this);

        f.addListener(() -> {
            try {
                ProcessCameraProvider p = f.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                p.unbindAll();
                p.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (Exception ignored) {}
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        File file = new File(getCacheDir(), "img.jpg");
        ImageCapture.OutputFileOptions opt =
                new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(opt, Executors.newSingleThreadExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                        runOcr(Uri.fromFile(file));
                    }
                    @Override public void onError(@NonNull ImageCaptureException e) {}
                });
    }

    private void runOcr(Uri uri) {
        try {
            InputImage img = InputImage.fromFilePath(this, uri);
            TextRecognizer r = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            r.process(img).addOnSuccessListener(text -> {
                Intent i = new Intent(this, NoteDetailActivity.class);
                i.putExtra("OCR", text.getText());
                startActivity(i);
            });
        } catch (Exception ignored) {}
    }
}
