package com.example.myapplication.UI;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.Data.AiRepository;
import com.example.myapplication.R;
import com.example.myapplication.Data.NoteRepository;
import com.example.myapplication.Model.Note;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Uploadactivity extends AppCompatActivity {

    private static final String TAG = "UPLOAD_ACTIVITY";

    private ImageView ivPreview;
    private TextView tvOcrResult, tvStatus;
    private Button btnPick, btnAnalyze;
    private ProgressBar progressBar;
    private LinearLayout layoutResult;

    private NoteRepository repository;
    private Bitmap selectedBitmap;
    private String ocrText = "";

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) handlePickedFile(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        repository  = new NoteRepository(this);

        ivPreview   = findViewById(R.id.ivPreview);
        tvOcrResult = findViewById(R.id.tvOcrResult);
        tvStatus    = findViewById(R.id.tvStatus);
        btnPick     = findViewById(R.id.btnPickFile);
        btnAnalyze  = findViewById(R.id.btnAnalyze);
        progressBar = findViewById(R.id.progressBar);
        layoutResult = findViewById(R.id.layoutResult);

        btnAnalyze.setVisibility(View.GONE);
        layoutResult.setVisibility(View.GONE);

        btnPick.setOnClickListener(v ->
                filePicker.launch(new String[]{"image/*", "application/pdf"}));

        btnAnalyze.setOnClickListener(v -> {
            if (!ocrText.isEmpty()) callGrokApi(ocrText);
        });
    }

    private void handlePickedFile(Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (mime != null && mime.equals("application/pdf")) {
            loadPdfFirstPage(uri);
        } else {
            loadImage(uri);
        }
    }

    private void loadImage(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            selectedBitmap = BitmapFactory.decodeStream(stream);
            ivPreview.setImageBitmap(selectedBitmap);
            ivPreview.setVisibility(View.VISIBLE);
            runOcr(selectedBitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPdfFirstPage(Uri uri) {
        try {
            ParcelFileDescriptor pfd = getContentResolver()
                    .openFileDescriptor(uri, "r");
            if (pfd == null) return;

            PdfRenderer renderer = new PdfRenderer(pfd);
            PdfRenderer.Page page = renderer.openPage(0);

            selectedBitmap = Bitmap.createBitmap(
                    page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(selectedBitmap, null, null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
            renderer.close();

            ivPreview.setImageBitmap(selectedBitmap);
            ivPreview.setVisibility(View.VISIBLE);
            runOcr(selectedBitmap);

        } catch (Exception e) {
            Toast.makeText(this, "Cannot render PDF: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void runOcr(Bitmap bitmap) {
        tvStatus.setText("Running OCR...");
        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setVisibility(View.GONE);

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(result -> {
                    ocrText = result.getText().trim();
                    progressBar.setVisibility(View.GONE);

                    if (ocrText.isEmpty()) {
                        tvStatus.setText("No text detected in this file.");
                        return;
                    }

                    tvStatus.setText("Text extracted. Ready to analyze.");
                    layoutResult.setVisibility(View.VISIBLE);
                    tvOcrResult.setText(ocrText.length() > 500
                            ? ocrText.substring(0, 500) + "..." : ocrText);
                    btnAnalyze.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("OCR failed: " + e.getMessage());
                });
    }

    private void callGrokApi(String text) {
        tvStatus.setText("Analyzing with Grok AI...");
        progressBar.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.groq.com/openai/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        GrokApi api = retrofit.create(GrokApi.class);

        String prompt = "Analyse le texte suivant et génère un résumé et des mots-clés. " +
                "Réponds uniquement au format JSON avec les clés 'summary' et 'keywords'. " +
                "Texte : " + text;

        List<AiRepository.GrokRequest.Message> messages = new ArrayList<>();
        messages.add(new AiRepository.GrokRequest.Message("system", "Tu es un assistant qui analyse des textes et répond uniquement en JSON."));
        messages.add(new AiRepository.GrokRequest.Message("user", prompt));

        AiRepository.GrokRequest req = new AiRepository.GrokRequest(
                "llama-3.3-70b-versatile",
                messages,
                0.3
        );

        String apiKey = BuildConfig.GROK_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            showError("Erreur : Clé API Groq non configurée.");
            return;
        }

        api.generate("Bearer " + apiKey, req)
                .enqueue(new Callback<AiRepository.GrokResponse>() {
                    @Override
                    public void onResponse(Call<AiRepository.GrokResponse> call, Response<AiRepository.GrokResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().choices != null && !response.body().choices.isEmpty()) {
                                try {
                                    String jsonResult = response.body().choices.get(0).message.content;
                                    
                                    // Nettoyage Markdown si présent
                                    if (jsonResult.contains("```json")) {
                                        jsonResult = jsonResult.substring(jsonResult.indexOf("```json") + 7);
                                        if (jsonResult.contains("```")) {
                                            jsonResult = jsonResult.substring(0, jsonResult.indexOf("```"));
                                        }
                                    } else if (jsonResult.contains("```")) {
                                        jsonResult = jsonResult.substring(jsonResult.indexOf("```") + 3);
                                        if (jsonResult.contains("```")) {
                                            jsonResult = jsonResult.substring(0, jsonResult.indexOf("```"));
                                        }
                                    }
                                    jsonResult = jsonResult.trim();
                                    
                                    JSONObject jsonObject = new JSONObject(jsonResult);
                                    String summary = jsonObject.optString("summary", "Pas de résumé");
                                    String keywords = jsonObject.optString("keywords", "pas-de-tags");

                                    runOnUiThread(() -> saveNote(text, summary, keywords));
                                } catch (Exception e) {
                                    Log.e(TAG, "Parsing error", e);
                                    runOnUiThread(() -> showError("Erreur parsing : " + e.getMessage()));
                                }
                            } else {
                                runOnUiThread(() -> showError("Groq n'a pas renvoyé de réponse."));
                            }
                        } else {
                            runOnUiThread(() -> showError("Erreur API Groq : " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<AiRepository.GrokResponse> call, Throwable t) {
                        runOnUiThread(() -> showError("Échec réseau : " + t.getMessage()));
                    }
                });
    }

    private void saveNote(String originalText, String summary, String keywords) {
        progressBar.setVisibility(View.GONE);
        btnAnalyze.setEnabled(true);

        try {
            Note note = new Note();
            note.content = originalText;
            note.summary = summary;
            note.keywords = keywords;
            note.createdAt = System.currentTimeMillis();

            repository.insert(note);

            tvStatus.setText("Note saved successfully!");
            Toast.makeText(this, "Note saved!", Toast.LENGTH_LONG).show();

            startActivity(new Intent(this, NotesListActivity.class));
            finish();

        } catch (Exception e) {
            showError("Save error: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        progressBar.setVisibility(View.GONE);
        btnAnalyze.setEnabled(true);
        tvStatus.setText(msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
