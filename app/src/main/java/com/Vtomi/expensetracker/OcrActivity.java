package com.Vtomi.expensetracker;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcrActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextRecognizer recognizer;
    private TextView tvLiveAmount;
    private View scannerFrame;
    private double lastDetectedAmount = 0;

    //regex ai által generálva
    private final Pattern pattern = Pattern.compile("\\d+(?:[.,]\\d+)?");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        viewFinder = findViewById(R.id.viewFinder);
        tvLiveAmount = findViewById(R.id.tv_live_amount);
        scannerFrame = findViewById(R.id.scanner_frame);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        startCamera();

        findViewById(R.id.btn_capture).setOnClickListener(v -> {
            if (lastDetectedAmount > 0) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("EXTRACTED_AMOUNT", lastDetectedAmount);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Várj, amíg találok egy összeget!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new android.util.Size(1280, 720))
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    processImage(image);
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void processImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() != null) {
            //elforgatva is tudja olvasni
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        extractAmountFromFrame(visionText, imageProxy);
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> imageProxy.close());
        }
    }

    private void extractAmountFromFrame(Text visionText, ImageProxy imageProxy) {
        // 1. Kiszámoljuk a keret koordinátáit a képernyőn pixelben
        int[] location = new int[2];
        scannerFrame.getLocationOnScreen(location);
        int frameX = location[0];
        int frameY = location[1];
        int frameWidth = scannerFrame.getWidth();
        int frameHeight = scannerFrame.getHeight();
        Rect frameRect = new Rect(frameX, frameY, frameX + frameWidth, frameY + frameHeight);

        // 2. Kiszámoljuk a transzformációs arányt a kamera képe és a képernyő között
        // (A CameraX PreviewView segítségével)
        // Fontos: Ha a kép el van forgatva (pl. portré), a szélességet és magasságot fel kell cserélni.
        int rotation = imageProxy.getImageInfo().getRotationDegrees();
        int imageWidth, imageHeight;
        if (rotation == 90 || rotation == 270) {
            imageWidth = imageProxy.getHeight();
            imageHeight = imageProxy.getWidth();
        } else {
            imageWidth = imageProxy.getWidth();
            imageHeight = imageProxy.getHeight();
        }

        float scaleX = (float) viewFinder.getWidth() / imageWidth;
        float scaleY = (float) viewFinder.getHeight() / imageHeight;

        double currentMax = 0;

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {

                Rect textRect = line.getBoundingBox();
                if (textRect != null) {

                    Rect transformedRect = new Rect(
                            (int) (textRect.left * scaleX),
                            (int) (textRect.top * scaleY),
                            (int) (textRect.right * scaleX),
                            (int) (textRect.bottom * scaleY)
                    );

                    if (Rect.intersects(frameRect, transformedRect)) {

                        String text = line.getText().replace(" ", "").replace(",", ".");
                        text = text.replaceAll("[^0-9.]", "");

                        if (text.isEmpty()) continue;

                        try {
                            double found = Double.parseDouble(text);
                            //nagyjából reális szűrő
                            if (found > 100 && found < 1000000) {
                                if (found > currentMax) {
                                    currentMax = found;
                                }
                            }
                        } catch (Exception e) { }
                    }
                }
            }
        }

        if (currentMax > 0) {
            lastDetectedAmount = currentMax;
            double finalAmount = currentMax;
            runOnUiThread(() -> {
                tvLiveAmount.setText("Keretben talált összeg: " + (int)finalAmount + " Ft");
                tvLiveAmount.setTextColor(Color.parseColor("#4CAF50"));
            });
        } else {
            runOnUiThread(() -> {
                tvLiveAmount.setText("Tartsd a számot a keretbe!");
                tvLiveAmount.setTextColor(Color.WHITE);
            });
        }
    }
}