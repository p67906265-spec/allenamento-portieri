package it.paolo.allenamentoportieri;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Editor tattico locale: uno schema indipendente e salvabile per ogni esercizio. */
public class ExerciseEditorDialog extends Dialog {
    private static final int NAVY = Color.rgb(16, 42, 67);
    private static final int GREEN = Color.rgb(18, 184, 134);
    private static final int PAGE = Color.rgb(242, 246, 248);

    public interface OnSaveListener { void onSave(List<String> diagrams); }

    private final List<String> descriptions;
    private final List<DiagramState> states = new ArrayList<>();
    private final OnSaveListener listener;
    private final SharedPreferences templates;
    private TextView counter;
    private TextView description;
    private DiagramCanvas board;
    private int current;

    public ExerciseEditorDialog(Context context, List<String> descriptions, List<String> saved, OnSaveListener listener) {
        super(context, android.R.style.Theme_Material_Light_NoActionBar);
        this.descriptions = new ArrayList<>(descriptions);
        this.listener = listener;
        this.templates = context.getSharedPreferences("exercise_diagram_library", Context.MODE_PRIVATE);
        for (int i = 0; i < descriptions.size(); i++) {
            DiagramState state = i < saved.size() ? DiagramState.fromJson(saved.get(i)) : null;
            states.add(state == null ? learnedOrAutomatic(descriptions.get(i)) : state);
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE);

        LinearLayout header = row();
        header.setPadding(dp(10), dp(8), dp(10), dp(8));
        header.setBackgroundColor(NAVY);
        Button close = button("‹  Chiudi");
        close.setTextColor(Color.WHITE);
        close.setOnClickListener(v -> dismiss());
        header.addView(close, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView title = text("SCHEMI ESERCIZI", 18, Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 2));
        Button save = button("Salva");
        save.setTextColor(Color.rgb(92, 230, 183));
        save.setOnClickListener(v -> saveAndClose());
        header.addView(save, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(header);

        LinearLayout pager = row();
        pager.setPadding(dp(10), dp(7), dp(10), dp(4));
        Button previous = compact("‹");
        previous.setOnClickListener(v -> changeExercise(-1));
        pager.addView(previous, new LinearLayout.LayoutParams(dp(52), dp(44)));
        counter = text("", 16, NAVY, true);
        counter.setGravity(Gravity.CENTER);
        pager.addView(counter, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button next = compact("›");
        next.setOnClickListener(v -> changeExercise(1));
        pager.addView(next, new LinearLayout.LayoutParams(dp(52), dp(44)));
        root.addView(pager);

        description = text("", 14, Color.rgb(55, 72, 86), false);
        description.setMaxLines(3);
        description.setPadding(dp(16), dp(3), dp(16), dp(8));
        root.addView(description);

        TextView hint = text("Scegli uno strumento, poi tocca il campo. Trascina gli elementi per spostarli.", 12, Color.rgb(80, 96, 108), false);
        hint.setPadding(dp(16), 0, dp(16), dp(7));
        root.addView(hint);

        HorizontalScrollView toolsScroll = new HorizontalScrollView(getContext());
        toolsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tools = row();
        tools.setPadding(dp(10), 0, dp(10), dp(8));
        addTool(tools, "Portiere", "keeper");
        addTool(tools, "Palla", "ball");
        addTool(tools, "Cono", "cone");
        addTool(tools, "Paletto", "pole");
        addTool(tools, "Ostacolo", "hurdle");
        addTool(tools, "Sagoma", "dummy");
        addTool(tools, "Porta", "goal");
        addTool(tools, "Freccia", "arrow");
        toolsScroll.addView(tools);
        root.addView(toolsScroll, new LinearLayout.LayoutParams(-1, dp(58)));

        board = new DiagramCanvas(getContext());
        root.addView(board, new LinearLayout.LayoutParams(-1, 0, 1));

        HorizontalScrollView commandsScroll = new HorizontalScrollView(getContext());
        commandsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout commands = row();
        commands.setPadding(dp(10), dp(8), dp(10), dp(10));
        addCommand(commands, "Annulla", v -> { board.state.undo(); board.invalidate(); });
        addCommand(commands, "Elimina", v -> { board.deleteSelected(); });
        addCommand(commands, "Pulisci", v -> { board.state.snapshot(); board.state.items.clear(); board.invalidate(); });
        addCommand(commands, "Ricrea bozza", v -> { states.set(current, DiagramState.automatic(descriptions.get(current))); showCurrent(); });
        commandsScroll.addView(commands);
        root.addView(commandsScroll, new LinearLayout.LayoutParams(-1, dp(68)));

        setContentView(root);
        showCurrent();
    }

    @Override protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setStatusBarColor(NAVY);
        }
    }

    private void addTool(LinearLayout target, String label, String type) {
        Button b = compact(label);
        b.setOnClickListener(v -> {
            board.tool = type;
            Toast.makeText(getContext(), type.equals("arrow") ? "Trascina sul campo per disegnare la freccia" : "Tocca il campo per aggiungere: " + label, Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(46));
        lp.setMargins(0, 0, dp(7), 0);
        target.addView(b, lp);
    }

    private void addCommand(LinearLayout target, String label, View.OnClickListener action) {
        Button b = compact(label);
        b.setOnClickListener(action);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
        lp.setMargins(0, 0, dp(8), 0);
        target.addView(b, lp);
    }

    private void changeExercise(int delta) {
        int next = current + delta;
        if (next < 0 || next >= states.size()) return;
        current = next;
        showCurrent();
    }

    private void showCurrent() {
        if (board == null) return;
        counter.setText("Esercizio " + (current + 1) + " di " + descriptions.size());
        description.setText(descriptions.get(current));
        board.setState(states.get(current));
    }

    private void saveAndClose() {
        List<String> result = new ArrayList<>();
        JSONObject library = readLibrary();
        for (int i = 0; i < states.size(); i++) {
            String value = states.get(i).toJson();
            result.add(value);
            try { library.put(normalize(descriptions.get(i)), value); } catch (Exception ignored) {}
        }
        templates.edit().putString("templates", library.toString()).apply();
        if (listener != null) listener.onSave(result);
        dismiss();
    }

    private DiagramState learnedOrAutomatic(String exercise) {
        JSONObject library = readLibrary();
        String wanted = normalize(exercise);
        String best = null;
        double bestScore = 0;
        JSONArray names = library.names();
        if (names != null) for (int i = 0; i < names.length(); i++) {
            String candidate = names.optString(i);
            double score = similarity(wanted, candidate);
            if (score > bestScore) { bestScore = score; best = library.optString(candidate); }
        }
        DiagramState learned = bestScore >= .42 ? DiagramState.fromJson(best) : null;
        return learned == null ? DiagramState.automatic(exercise) : learned;
    }

    private JSONObject readLibrary() { try { return new JSONObject(templates.getString("templates", "{}")); } catch (Exception e) { return new JSONObject(); } }
    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ITALY).replaceAll("[^a-zà-ù0-9 ]", " ").replaceAll("\\s+", " ").trim(); }
    private double similarity(String a, String b) {
        if (a.equals(b)) return 1;
        String[] aa = a.split(" "), bb = b.split(" "); int common = 0;
        for (String x : aa) if (x.length() > 2) for (String y : bb) if (x.equals(y)) { common++; break; }
        int usefulA = 0, usefulB = 0; for (String x : aa) if (x.length() > 2) usefulA++; for (String x : bb) if (x.length() > 2) usefulB++;
        int union = usefulA + usefulB - common;
        return union == 0 ? 0 : (double) common / union;
    }

    private LinearLayout row() { LinearLayout v = new LinearLayout(getContext()); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private TextView text(String value, int size, int color, boolean bold) { TextView v = new TextView(getContext()); v.setText(value); v.setTextSize(size); v.setTextColor(color); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v; }
    private Button button(String value) { Button b = new Button(getContext()); b.setText(value); b.setTextSize(14); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackgroundColor(Color.TRANSPARENT); return b; }
    private Button compact(String value) { Button b = button(value); b.setTextColor(NAVY); b.setPadding(dp(14), 0, dp(14), 0); b.setBackground(rounded(Color.WHITE, Color.rgb(195, 208, 216))); return b; }
    private GradientDrawable rounded(int fill, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(10)); d.setStroke(dp(1), stroke); return d; }
    private int dp(int value) { return Math.round(value * getContext().getResources().getDisplayMetrics().density); }

    private static class DiagramItem {
        String type;
        float x, y, x2, y2;
        DiagramItem(String type, float x, float y) { this.type = type; this.x = x; this.y = y; this.x2 = x; this.y2 = y; }
        DiagramItem(String type, float x, float y, float x2, float y2) { this.type = type; this.x = x; this.y = y; this.x2 = x2; this.y2 = y2; }
        JSONObject json() throws Exception { JSONObject o = new JSONObject(); o.put("type", type); o.put("x", x); o.put("y", y); o.put("x2", x2); o.put("y2", y2); return o; }
        static DiagramItem from(JSONObject o) { return new DiagramItem(o.optString("type"), (float)o.optDouble("x", .5), (float)o.optDouble("y", .5), (float)o.optDouble("x2", .5), (float)o.optDouble("y2", .5)); }
    }

    private static class DiagramState {
        final List<DiagramItem> items = new ArrayList<>();
        final List<String> history = new ArrayList<>();

        void snapshot() { history.add(toJson()); if (history.size() > 20) history.remove(0); }
        void undo() { if (history.isEmpty()) return; DiagramState old = fromJson(history.remove(history.size() - 1)); if (old != null) { items.clear(); items.addAll(old.items); } }
        String toJson() { try { JSONArray a = new JSONArray(); for (DiagramItem item : items) a.put(item.json()); JSONObject root = new JSONObject(); root.put("items", a); return root.toString(); } catch (Exception e) { return "{\"items\":[]}"; } }
        static DiagramState fromJson(String value) { try { if (value == null || value.trim().isEmpty()) return null; JSONObject root = new JSONObject(value); JSONArray a = root.optJSONArray("items"); DiagramState state = new DiagramState(); if (a != null) for (int i = 0; i < a.length(); i++) state.items.add(DiagramItem.from(a.getJSONObject(i))); return state; } catch (Exception e) { return null; } }

        static DiagramState automatic(String raw) {
            DiagramState s = new DiagramState();
            String d = raw == null ? "" : raw.toLowerCase(Locale.ITALY);
            s.items.add(new DiagramItem("keeper", .50f, .82f));
            s.items.add(new DiagramItem("ball", .50f, .42f));
            if (d.contains("cross") || d.contains("uscit")) {
                s.items.add(new DiagramItem("ball", .16f, .35f));
                s.items.add(new DiagramItem("arrow", .16f, .35f, .50f, .72f));
                s.items.add(new DiagramItem("dummy", .42f, .68f));
                s.items.add(new DiagramItem("dummy", .60f, .68f));
            } else if (d.contains("slalom") || d.contains("palett") || d.contains("cono") || d.contains("cinesin")) {
                for (int i = 0; i < 5; i++) s.items.add(new DiagramItem(d.contains("palett") ? "pole" : "cone", .30f + i * .10f, .55f + (i % 2) * .06f));
                s.items.add(new DiagramItem("arrow", .50f, .80f, .50f, .35f));
            } else if (d.contains("ostacol") || d.contains("balz") || d.contains("forza")) {
                s.items.add(new DiagramItem("hurdle", .35f, .62f));
                s.items.add(new DiagramItem("hurdle", .50f, .55f));
                s.items.add(new DiagramItem("hurdle", .65f, .48f));
                s.items.add(new DiagramItem("arrow", .32f, .72f, .70f, .38f));
            } else if (d.contains("tuff") || d.contains("lateral")) {
                s.items.add(new DiagramItem("ball", .25f, .76f));
                s.items.add(new DiagramItem("ball", .75f, .76f));
                s.items.add(new DiagramItem("arrow", .50f, .82f, .25f, .76f));
                s.items.add(new DiagramItem("arrow", .50f, .82f, .75f, .76f));
            } else {
                s.items.add(new DiagramItem("keeper", .35f, .35f));
                s.items.add(new DiagramItem("arrow", .35f, .38f, .50f, .76f));
            }
            return s;
        }
    }

    private static class DiagramCanvas extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private DiagramState state = new DiagramState();
        private String tool;
        private DiagramItem selected;
        private float fieldLeft, fieldTop, fieldWidth, fieldHeight;
        private float downX, downY;

        DiagramCanvas(Context context) { super(context); setBackgroundColor(Color.rgb(229, 239, 233)); }
        void setState(DiagramState state) { this.state = state; selected = null; tool = null; invalidate(); }
        void deleteSelected() { if (selected == null) return; state.snapshot(); state.items.remove(selected); selected = null; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float margin = getWidth() * .055f;
            fieldLeft = margin; fieldTop = dp(5); fieldWidth = getWidth() - margin * 2; fieldHeight = getHeight() - dp(10);
            p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(48, 145, 80));
            c.drawRoundRect(fieldLeft, fieldTop, fieldLeft + fieldWidth, fieldTop + fieldHeight, dp(12), dp(12), p);
            drawField(c);
            for (DiagramItem item : state.items) drawItem(c, item);
        }

        private void drawField(Canvas c) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(Color.argb(220, 255, 255, 255));
            float l = fieldLeft, t = fieldTop, r = l + fieldWidth, b = t + fieldHeight;
            c.drawRoundRect(l, t, r, b, dp(12), dp(12), p);
            c.drawLine(l, t + fieldHeight / 2, r, t + fieldHeight / 2, p);
            c.drawCircle(l + fieldWidth / 2, t + fieldHeight / 2, fieldWidth * .13f, p);
            c.drawRect(l + fieldWidth * .27f, t, l + fieldWidth * .73f, t + fieldHeight * .18f, p);
            c.drawRect(l + fieldWidth * .27f, b - fieldHeight * .18f, l + fieldWidth * .73f, b, p);
        }

        private void drawItem(Canvas c, DiagramItem item) {
            float x = sx(item.x), y = sy(item.y), size = Math.max(dp(10), fieldWidth * .032f);
            if (item.type.equals("arrow")) { drawArrow(c, x, y, sx(item.x2), sy(item.y2)); return; }
            p.setStyle(Paint.Style.FILL);
            switch (item.type) {
                case "keeper":
                    p.setColor(Color.rgb(29, 79, 160)); c.drawCircle(x, y, size * 1.15f, p);
                    p.setColor(Color.WHITE); p.setTextSize(size * 1.35f); p.setTypeface(Typeface.DEFAULT_BOLD); p.setTextAlign(Paint.Align.CENTER); c.drawText("P", x, y + size * .48f, p); break;
                case "ball":
                    p.setColor(Color.WHITE); c.drawCircle(x, y, size * .72f, p); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(1)); p.setColor(Color.DKGRAY); c.drawCircle(x, y, size * .72f, p); break;
                case "cone":
                    p.setColor(Color.rgb(255, 145, 35)); Path cone = new Path(); cone.moveTo(x, y - size); cone.lineTo(x - size * .75f, y + size); cone.lineTo(x + size * .75f, y + size); cone.close(); c.drawPath(cone, p); break;
                case "pole":
                    p.setColor(Color.rgb(255, 220, 55)); c.drawRect(x - size * .18f, y - size * 1.5f, x + size * .18f, y + size * 1.5f, p); c.drawCircle(x, y + size * 1.5f, size * .55f, p); break;
                case "hurdle":
                    p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(size * .28f); p.setColor(Color.rgb(245, 190, 35)); c.drawLine(x - size, y + size * .7f, x - size, y - size * .55f, p); c.drawLine(x + size, y + size * .7f, x + size, y - size * .55f, p); c.drawLine(x - size, y - size * .55f, x + size, y - size * .55f, p); break;
                case "dummy":
                    p.setColor(Color.rgb(218, 72, 72)); c.drawCircle(x, y - size, size * .55f, p); c.drawRoundRect(x - size * .65f, y - size * .45f, x + size * .65f, y + size * 1.2f, size * .25f, size * .25f, p); break;
                case "goal":
                    p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(Color.WHITE); c.drawRect(x - size * 2.0f, y - size, x + size * 2.0f, y + size, p); break;
            }
            if (item == selected) { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(Color.rgb(255, 235, 59)); c.drawCircle(x, y, size * 2.1f, p); }
        }

        private void drawArrow(Canvas c, float x1, float y1, float x2, float y2) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(3)); p.setColor(Color.WHITE); c.drawLine(x1, y1, x2, y2, p);
            double angle = Math.atan2(y2 - y1, x2 - x1); float head = dp(13);
            Path path = new Path(); path.moveTo(x2, y2); path.lineTo((float)(x2 - head * Math.cos(angle - .55)), (float)(y2 - head * Math.sin(angle - .55))); path.moveTo(x2, y2); path.lineTo((float)(x2 - head * Math.cos(angle + .55)), (float)(y2 - head * Math.sin(angle + .55))); c.drawPath(path, p);
            if (selected != null && selected.type.equals("arrow") && selected.x == nx(x1) && selected.y == ny(y1)) { p.setColor(Color.YELLOW); c.drawCircle(x1, y1, dp(6), p); c.drawCircle(x2, y2, dp(6), p); }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float nx = nx(e.getX()), ny = ny(e.getY());
            if (nx < 0 || nx > 1 || ny < 0 || ny > 1) return true;
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downX = nx; downY = ny;
                if ("arrow".equals(tool)) return true;
                if (tool != null) { state.snapshot(); DiagramItem item = new DiagramItem(tool, nx, ny); state.items.add(item); selected = item; tool = null; invalidate(); return true; }
                selected = hit(nx, ny);
                if (selected != null) state.snapshot();
                invalidate(); return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE && selected != null && tool == null) {
                if (selected.type.equals("arrow")) { float dx = nx - selected.x, dy = ny - selected.y; selected.x = nx; selected.y = ny; selected.x2 += dx; selected.y2 += dy; }
                else { selected.x = nx; selected.y = ny; }
                invalidate(); return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP && "arrow".equals(tool)) {
                state.snapshot(); DiagramItem arrow = new DiagramItem("arrow", downX, downY, nx, ny); state.items.add(arrow); selected = arrow; tool = null; invalidate(); return true;
            }
            return true;
        }

        private DiagramItem hit(float x, float y) {
            for (int i = state.items.size() - 1; i >= 0; i--) { DiagramItem item = state.items.get(i); float dx = x - item.x, dy = y - item.y; if (dx * dx + dy * dy < .0035f) return item; }
            return null;
        }
        private float sx(float x) { return fieldLeft + x * fieldWidth; }
        private float sy(float y) { return fieldTop + y * fieldHeight; }
        private float nx(float x) { return fieldWidth == 0 ? 0 : (x - fieldLeft) / fieldWidth; }
        private float ny(float y) { return fieldHeight == 0 ? 0 : (y - fieldTop) / fieldHeight; }
        private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    }
}
