package com.example.myapplication.UI;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.Data.AiRepository;
import com.example.myapplication.Data.NoteRepository;
import com.example.myapplication.Model.Note;
import com.example.myapplication.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class NoteDetailActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "NoteDetailActivity";
    TextView tvContent, tvSummary, tvKeywords;
    Button btnCopy;
    ImageButton btnFavorite;
    private TextToSpeech tts;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; 
    
    private NoteRepository repository;
    private long currentNoteId = -1;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_note_detail);

        repository = new NoteRepository(this);

        tvContent = findViewById(R.id.tvContent);
        tvSummary = findViewById(R.id.tvSummary);
        tvKeywords = findViewById(R.id.tvKeywords);
        btnCopy = findViewById(R.id.btnCopySummary);
        btnFavorite = findViewById(R.id.btnFavorite);

        tts = new TextToSpeech(this, this);

        setupDoubleTap(tvSummary);
        setupDoubleTap(tvContent);

        btnCopy.setOnClickListener(v -> copyToClipboard(tvSummary.getText().toString()));
        
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        String ocr = getIntent().getStringExtra("OCR");
        if (ocr != null) {
            tvContent.setText(ocr);
            callGrok(ocr);
        } else {
            currentNoteId = getIntent().getLongExtra("NOTE_ID", -1);
            if (currentNoteId != -1) {
                Note n = repository.getById(currentNoteId);
                if (n != null) {
                    tvContent.setText(n.content);
                    tvSummary.setText(n.summary);
                    tvKeywords.setText(n.keywords);
                    isFavorite = n.isFavorite;
                    updateFavoriteUI();
                }
            }
        }
    }

    private void toggleFavorite() {
        if (currentNoteId == -1) {
            Toast.makeText(this, "Sauvegardez d'abord la note", Toast.LENGTH_SHORT).show();
            return;
        }
        isFavorite = !isFavorite;
        repository.toggleFavorite(currentNoteId, isFavorite);
        updateFavoriteUI();
        Toast.makeText(this, isFavorite ? "Ajouté aux favoris ⭐" : "Retiré des favoris", Toast.LENGTH_SHORT).show();
    }

    private void updateFavoriteUI() {
        if (isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    private void setupDoubleTap(View view) {
        view.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                if (tts != null && tts.isSpeaking()) {
                    tts.stop();
                    Toast.makeText(this, "Lecture arrêtée", Toast.LENGTH_SHORT).show();
                } else {
                    String text = ((TextView)v).getText().toString();
                    if (!text.isEmpty() && !text.equals("Pas de résumé")) {
                        speak(text);
                    }
                }
            }
            lastClickTime = clickTime;
        });
    }

    private void speak(String text) {
        if (tts != null) {
            int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ID1");
            if (result == TextToSpeech.ERROR) {
                Toast.makeText(this, "Erreur de lecture vocale", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lecture en cours...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.FRENCH);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault());
            }
        } else {
            Toast.makeText(this, "Le moteur vocal n'est pas prêt", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Résumé Note", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Résumé copié", Toast.LENGTH_SHORT).show();
    }

    private void callGrok(String text) {
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
            tvSummary.setText("Erreur : Clé API non configurée.");
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
                                    
                                    JsonObject jsonObject = JsonParser.parseString(jsonResult).getAsJsonObject();
                                    
                                    String summary = jsonObject.has("summary") ? jsonObject.get("summary").getAsString() : "Pas de résumé";
                                    
                                    String keywords = "";
                                    if (jsonObject.has("keywords")) {
                                        JsonElement kwElement = jsonObject.get("keywords");
                                        if (kwElement.isJsonArray()) {
                                            JsonArray kwArray = kwElement.getAsJsonArray();
                                            List<String> kwList = new ArrayList<>();
                                            for (JsonElement e : kwArray) {
                                                kwList.add(e.getAsString());
                                            }
                                            keywords = String.join(", ", kwList);
                                        } else {
                                            keywords = kwElement.getAsString();
                                        }
                                    }

                                    tvSummary.setText(summary);
                                    tvKeywords.setText(keywords);

                                    Note n = new Note();
                                    n.content = text;
                                    n.summary = summary;
                                    n.keywords = keywords;
                                    n.createdAt = System.currentTimeMillis();
                                    currentNoteId = repository.insert(n);
                                } catch (Exception e) {
                                    Log.e(TAG, "Parsing error", e);
                                    tvSummary.setText("Erreur parsing.");
                                }
                            }
                        } else {
                            tvSummary.setText("Erreur API.");
                        }
                    }

                    @Override
                    public void onFailure(Call<AiRepository.GrokResponse> call, Throwable t) {
                        tvSummary.setText("Échec réseau.");
                    }
                });
    }
}
