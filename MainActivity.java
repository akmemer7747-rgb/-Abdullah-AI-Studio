package com.abdullah.aistudio;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import okhttp3.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private EditText openAiKey, pexelsKey, youtubeUrl, notes;
    private TextView status, output;
    private Button longBtn, shortBtn, voiceBtn, clipsBtn;
    private String currentScript = "";
    private TextToSpeech tts;
    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(90, TimeUnit.SECONDS).build();
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 36);
        root.setBackgroundColor(0xFF101827);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("ABDULLAH AI STUDIO");
        title.setTextSize(25); title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER); title.setPadding(0, 10, 0, 8);
        root.addView(title);
        TextView sub = new TextView(this);
        sub.setText("Mobile video creator • Original scripts • English voice");
        sub.setTextColor(0xFFB8C2D8); sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, 22); root.addView(sub);

        openAiKey = field(root, "OpenAI API Key", true);
        pexelsKey = field(root, "Pexels API Key", true);
        youtubeUrl = field(root, "YouTube link (topic reference)", false);
        notes = field(root, "Your instructions / niche / style (optional)", false);
        notes.setMinLines(3);

        longBtn = button(root, "Generate LONG video script");
        shortBtn = button(root, "Generate SHORTS script");
        voiceBtn = button(root, "Create English male voice audio");
        clipsBtn = button(root, "Find Pexels stock video clips");
        status = new TextView(this);
        status.setTextColor(0xFFFFD166); status.setPadding(0, 18, 0, 8);
        root.addView(status);
        output = new TextView(this);
        output.setTextColor(0xFFFFFFFF); output.setTextSize(15);
        output.setPadding(0, 12, 0, 20); root.addView(output);

        longBtn.setOnClickListener(v -> generate(true));
        shortBtn.setOnClickListener(v -> generate(false));
        voiceBtn.setOnClickListener(v -> makeVoice());
        clipsBtn.setOnClickListener(v -> findClips());
        setContentView(scroll);
    }

    private EditText field(LinearLayout root, String hint, boolean secret) {
        EditText e = new EditText(this);
        e.setSingleLine(!hint.startsWith("Your"));
        e.setHint(hint); e.setTextColor(0xFFFFFFFF); e.setHintTextColor(0xFF9AA7BD);
        e.setPadding(18, 12, 18, 12);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF7777AA));
        if (secret) e.setInputType(129);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 7, 0, 7); root.addView(e, lp); return e;
    }

    private Button button(LinearLayout root, String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF); b.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF5146C8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 7, 0, 7); root.addView(b, lp); return b;
    }

    private void busy(String msg) { runOnUiThread(() -> status.setText(msg)); }

    private void generate(boolean isLong) {
        String key = openAiKey.getText().toString().trim();
        String url = youtubeUrl.getText().toString().trim();
        String extra = notes.getText().toString().trim();
        if (key.isEmpty() || url.isEmpty()) { busy("OpenAI key اور YouTube link درج کریں۔"); return; }
        busy("اسکرپٹ تیار ہو رہی ہے...");
        new Thread(() -> {
            try {
                String topic = getYoutubeTitle(url);
                String prompt = "Create a genuinely original " + (isLong ? "8-10 minute YouTube video" : "30-60 second YouTube Short")
                    + " script in clear natural English. Topic/reference title: " + topic
                    + ". User notes: " + extra
                    + ". Do not copy source wording. Include a strong hook, narration, and scene suggestions. "
                    + "Return plain text with clear scene breaks.";
                JSONObject body = new JSONObject();
                body.put("model", "gpt-4.1-mini");
                JSONArray input = new JSONArray();
                input.put(new JSONObject().put("role","user").put("content",prompt));
                body.put("input", input);
                Request req = new Request.Builder().url("https://api.openai.com/v1/responses")
                    .addHeader("Authorization","Bearer " + key)
                    .post(RequestBody.create(body.toString(), JSON)).build();
                try (Response response = http.newCall(req).execute()) {
                    String raw = response.body().string();
                    if (!response.isSuccessful()) throw new Exception("OpenAI error: " + raw);
                    JSONObject obj = new JSONObject(raw);
                    String text = extractOutput(obj);
                    currentScript = text;
                    runOnUiThread(() -> { output.setText(text); status.setText("اسکرپٹ تیار ہے۔"); });
                }
            } catch (Exception ex) { busy("Error: " + ex.getMessage()); }
        }).start();
    }

    private String getYoutubeTitle(String url) throws Exception {
        HttpUrl parsed = HttpUrl.parse("https://www.youtube.com/oembed");
        if (parsed == null) return url;
        Request req = new Request.Builder().url(parsed.newBuilder().addQueryParameter("url",url)
            .addQueryParameter("format","json").build()).build();
        try (Response r = http.newCall(req).execute()) {
            if (!r.isSuccessful()) return url;
            return new JSONObject(r.body().string()).optString("title", url);
        }
    }

    private String extractOutput(JSONObject obj) throws Exception {
        JSONArray arr = obj.optJSONArray("output");
        if (arr != null) for (int i=0;i<arr.length();i++) {
            JSONObject item = arr.getJSONObject(i);
            JSONArray content = item.optJSONArray("content");
            if (content != null) for (int j=0;j<content.length();j++) {
                JSONObject c = content.getJSONObject(j);
                if ("output_text".equals(c.optString("type"))) return c.optString("text");
            }
        }
        return obj.optString("output_text", "No script text returned.");
    }

    private void makeVoice() {
        if (currentScript.isEmpty()) { busy("پہلے اسکرپٹ بنائیں۔"); return; }
        if (tts == null) { busy("Text-to-Speech تیار نہیں۔"); return; }
        String clean = currentScript.replaceAll("\\[.*?\\]", "").trim();
        String name = "Abdullah_AI_Voice_" + System.currentTimeMillis() + ".wav";
        File dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(dir, name);
        int result = tts.synthesizeToFile(clean, new Bundle(), file, "abdullah_voice");
        busy(result == TextToSpeech.SUCCESS ? "وائس فائل بنائی جا رہی ہے: " + file.getAbsolutePath()
                : "وائس بنانے میں مسئلہ آیا۔");
    }

    private void findClips() {
        String key = pexelsKey.getText().toString().trim();
        String query = youtubeUrl.getText().toString().trim();
        if (key.isEmpty()) { busy("Pexels API key درج کریں۔"); return; }
        busy("Pexels پر متعلقہ stock clips تلاش ہو رہے ہیں...");
        new Thread(() -> {
            try {
                String topic = query.isEmpty() ? "cinematic background" : getYoutubeTitle(query);
                HttpUrl u = HttpUrl.parse("https://api.pexels.com/videos/search").newBuilder()
                    .addQueryParameter("query", topic).addQueryParameter("per_page","5").build();
                Request req = new Request.Builder().url(u).addHeader("Authorization",key).build();
                try (Response r = http.newCall(req).execute()) {
                    String raw = r.body().string();
                    if (!r.isSuccessful()) throw new Exception("Pexels error: " + raw);
                    JSONObject data = new JSONObject(raw); JSONArray vids = data.optJSONArray("videos");
                    StringBuilder result = new StringBuilder("Pexels results (open/download links):\\n");
                    if (vids != null) for (int i=0;i<vids.length();i++) {
                        JSONObject video = vids.getJSONObject(i);
                        JSONArray fs = video.optJSONArray("video_files");
                        String link = "";
                        if (fs != null) for (int j=0;j<fs.length();j++) {
                            JSONObject f = fs.getJSONObject(j);
                            if (f.optString("file_type").contains("mp4")) { link=f.optString("link"); break; }
                        }
                        if (!link.isEmpty()) result.append(i+1).append(". ").append(link).append("\\n");
                    }
                    runOnUiThread(() -> { output.setText(result.toString()); status.setText("Stock clips مل گئے۔"); });
                }
            } catch (Exception ex) { busy("Error: " + ex.getMessage()); }
        }).start();
    }

    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
