package it.paolo.allenamentoportieri;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(16, 42, 67);
    private static final int GREEN = Color.rgb(18, 184, 134);
    private static final int PAGE = Color.rgb(242, 246, 248);
    private static final int MUTED = Color.rgb(91, 110, 127);
    private static final String[] GOALS = {"Tecnica di base", "Presa e tuffo", "Reattività", "Uscite alte", "Uno contro uno", "Gioco con i piedi", "Forza e mobilità", "Seduta completa"};
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY);
    private final SimpleDateFormat pretty = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALY);
    private final List<Session> sessions = new ArrayList<>();
    private SharedPreferences prefs;
    private LinearLayout content;
    private boolean homeVisible;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("goalkeeper_training", MODE_PRIVATE);
        load();
        showHome();
    }

    private void base(String title, String subtitle) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(20), dp(18), dp(20), dp(16));
        head.setBackgroundColor(NAVY);
        TextView t = text(title, 25, Color.WHITE, true);
        head.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView st = text(subtitle, 14, Color.rgb(202, 219, 231), false);
            st.setPadding(0, dp(4), 0, 0);
            head.addView(st);
        }
        root.addView(head);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showHome() {
        homeVisible = true;
        base("Allenamento Portieri", "Diario e programmazione delle sedute");
        LinearLayout stats = row();
        stats.addView(stat("ALLENAMENTI", String.valueOf(sessions.size())), weight());
        stats.addView(space(10));
        stats.addView(stat("MINUTI TOTALI", String.valueOf(totalMinutes())), weight());
        content.addView(stats);

        Button add = primary("＋  REGISTRA ALLENAMENTO");
        add.setOnClickListener(v -> showEditor(null));
        marginTop(add, 18);
        content.addView(add);

        TextView recent = section("Ultime giornate");
        content.addView(recent);
        if (sessions.isEmpty()) {
            content.addView(empty("Non hai ancora registrato allenamenti.\nPremi il pulsante verde per iniziare."));
        } else {
            List<Session> sorted = sorted();
            for (int i = 0; i < Math.min(3, sorted.size()); i++) content.addView(sessionCard(sorted.get(i)));
            Button all = secondary("VEDI TUTTO LO STORICO");
            all.setOnClickListener(v -> showHistory());
            content.addView(all);
        }

        TextView plan = section("Prepara la prossima seduta");
        content.addView(plan);
        TextView copy = text("Scegli obiettivo e durata: l’app prepara una proposta completa che puoi adattare e salvare.", 15, MUTED, false);
        copy.setPadding(dp(2), 0, dp(2), dp(12));
        content.addView(copy);
        Button ideas = secondary("⚽  SUGGERISCI ALLENAMENTO");
        ideas.setOnClickListener(v -> showPlanner());
        content.addView(ideas);
    }

    private void showHistory() {
        homeVisible = false;
        base("Storico", sessions.size() + (sessions.size() == 1 ? " giornata registrata" : " giornate registrate"));
        Button back = link("‹  Torna alla home");
        back.setOnClickListener(v -> showHome());
        content.addView(back);
        if (sessions.isEmpty()) content.addView(empty("Nessun allenamento registrato."));
        for (Session s : sorted()) content.addView(sessionCard(s));
        Button add = primary("＋  NUOVO ALLENAMENTO");
        add.setOnClickListener(v -> showEditor(null));
        marginTop(add, 12);
        content.addView(add);
    }

    private View sessionCard(Session s) {
        LinearLayout card = card();
        TextView date = text(formatDate(s.date), 16, NAVY, true);
        card.addView(date);
        LinearLayout badgeRow = row();
        TextView goal = pill(s.goal, GREEN, Color.WHITE);
        badgeRow.addView(goal);
        TextView duration = pill(s.minutes + " min", Color.rgb(225, 233, 238), NAVY);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-2, -2);
        dp.setMargins(dp(8), 0, 0, 0);
        badgeRow.addView(duration, dp);
        marginTop(badgeRow, 9);
        card.addView(badgeRow);
        if (!s.work.trim().isEmpty()) {
            TextView work = text(s.work, 15, Color.rgb(35, 55, 70), false);
            work.setPadding(0, dp(11), 0, 0);
            work.setMaxLines(4);
            card.addView(work);
        }
        if (!s.notes.trim().isEmpty()) {
            TextView notes = text("Note: " + s.notes, 14, MUTED, false);
            notes.setPadding(0, dp(8), 0, 0);
            card.addView(notes);
        }
        LinearLayout actions = row();
        Button edit = smallButton("MODIFICA");
        edit.setOnClickListener(v -> showEditor(s));
        actions.addView(edit, weight());
        actions.addView(space(8));
        Button delete = smallButton("ELIMINA");
        delete.setTextColor(Color.rgb(190, 50, 50));
        delete.setOnClickListener(v -> confirmDelete(s));
        actions.addView(delete, weight());
        marginTop(actions, 10);
        card.addView(actions);
        return card;
    }

    private void showEditor(Session existing) {
        homeVisible = false;
        boolean editing = existing != null;
        base(editing ? "Modifica allenamento" : "Nuovo allenamento", "Registra quello che avete svolto");
        Button back = link("‹  Annulla");
        back.setOnClickListener(v -> editing ? showHistory() : showHome());
        content.addView(back);

        Calendar selected = Calendar.getInstance();
        if (editing) try { selected.setTime(iso.parse(existing.date)); } catch (Exception ignored) {}
        final String[] dateValue = {iso.format(selected.getTime())};
        TextView dateLabel = label("Data");
        content.addView(dateLabel);
        Button date = inputButton(formatDate(dateValue[0]));
        date.setOnClickListener(v -> new DatePickerDialog(this, (view, y, m, d) -> {
            selected.set(y, m, d);
            dateValue[0] = iso.format(selected.getTime());
            date.setText(formatDate(dateValue[0]));
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show());
        content.addView(date);

        content.addView(label("Obiettivo principale"));
        Spinner goal = spinner(GOALS);
        if (editing) goal.setSelection(indexOf(GOALS, existing.goal));
        content.addView(goal);

        content.addView(label("Durata in minuti"));
        EditText minutes = input("Es. 75", InputType.TYPE_CLASS_NUMBER);
        minutes.setText(editing ? String.valueOf(existing.minutes) : "75");
        content.addView(minutes);

        content.addView(label("Cosa si è fatto"));
        EditText work = input("Es. riscaldamento, prese basse, tuffi, uscite alte…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        work.setMinLines(5);
        work.setGravity(Gravity.TOP);
        if (editing) work.setText(existing.work);
        content.addView(work);

        content.addView(label("Note e cose da migliorare (facoltative)"));
        EditText notes = input("Osservazioni sulla seduta", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notes.setMinLines(3);
        notes.setGravity(Gravity.TOP);
        if (editing) notes.setText(existing.notes);
        content.addView(notes);

        Button save = primary(editing ? "SALVA MODIFICHE" : "SALVA ALLENAMENTO");
        marginTop(save, 20);
        save.setOnClickListener(v -> {
            int mins;
            try { mins = Integer.parseInt(minutes.getText().toString().trim()); }
            catch (Exception e) { mins = 0; }
            if (mins < 1 || mins > 300) {
                minutes.setError("Inserisci una durata da 1 a 300 minuti");
                return;
            }
            if (work.getText().toString().trim().isEmpty()) {
                work.setError("Scrivi cosa si è fatto");
                return;
            }
            if (editing) sessions.remove(existing);
            sessions.add(new Session(editing ? existing.id : System.currentTimeMillis(), dateValue[0], String.valueOf(goal.getSelectedItem()), mins, work.getText().toString().trim(), notes.getText().toString().trim()));
            save();
            Toast.makeText(this, editing ? "Allenamento aggiornato" : "Allenamento salvato", Toast.LENGTH_SHORT).show();
            showHome();
        });
        content.addView(save);
    }

    private void showPlanner() {
        homeVisible = false;
        base("Allenamento consigliato", "Una traccia pratica da adattare ai tuoi portieri");
        Button back = link("‹  Torna alla home");
        back.setOnClickListener(v -> showHome());
        content.addView(back);
        content.addView(label("Obiettivo della seduta"));
        Spinner goal = spinner(GOALS);
        content.addView(goal);
        content.addView(label("Tempo disponibile"));
        String[] times = {"45 minuti", "60 minuti", "75 minuti", "90 minuti"};
        Spinner duration = spinner(times);
        duration.setSelection(2);
        content.addView(duration);

        LinearLayout result = card();
        marginTop(result, 18);
        content.addView(result);
        Runnable generate = () -> renderPlan(result, String.valueOf(goal.getSelectedItem()), Integer.parseInt(String.valueOf(duration.getSelectedItem()).split(" ")[0]));
        goal.setOnItemSelectedListener(listener(generate));
        duration.setOnItemSelectedListener(listener(generate));
        generate.run();

        Button use = primary("USA E REGISTRA QUESTA SEDUTA");
        marginTop(use, 14);
        use.setOnClickListener(v -> {
            String plan = String.valueOf(result.getTag());
            showEditorWithPlan(String.valueOf(goal.getSelectedItem()), Integer.parseInt(String.valueOf(duration.getSelectedItem()).split(" ")[0]), plan);
        });
        content.addView(use);

        TextView warning = text("Suggerimento generale: adatta sempre intensità, distanze e numero di ripetizioni a età, livello, condizioni fisiche e spazio disponibile. Interrompi in caso di dolore.", 13, MUTED, false);
        warning.setPadding(dp(4), dp(16), dp(4), 0);
        content.addView(warning);
    }

    private void showEditorWithPlan(String goalValue, int mins, String plan) {
        homeVisible = false;
        base("Registra la seduta", "La proposta è già inserita: puoi modificarla");
        Button back = link("‹  Torna ai suggerimenti");
        back.setOnClickListener(v -> showPlanner());
        content.addView(back);
        final String today = iso.format(new Date());
        content.addView(label("Data"));
        TextView date = inputDisplay(formatDate(today));
        content.addView(date);
        content.addView(label("Obiettivo"));
        content.addView(inputDisplay(goalValue));
        content.addView(label("Durata"));
        content.addView(inputDisplay(mins + " minuti"));
        content.addView(label("Programma della seduta"));
        EditText work = input("", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        work.setMinLines(9);
        work.setGravity(Gravity.TOP);
        work.setText(plan);
        content.addView(work);
        content.addView(label("Note finali (facoltative)"));
        EditText notes = input("Cosa ha funzionato? Cosa migliorare?", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        notes.setMinLines(3);
        notes.setGravity(Gravity.TOP);
        content.addView(notes);
        Button save = primary("SALVA ALLENAMENTO");
        marginTop(save, 20);
        save.setOnClickListener(v -> {
            sessions.add(new Session(System.currentTimeMillis(), today, goalValue, mins, work.getText().toString().trim(), notes.getText().toString().trim()));
            save();
            Toast.makeText(this, "Allenamento salvato", Toast.LENGTH_SHORT).show();
            showHome();
        });
        content.addView(save);
    }

    private void renderPlan(LinearLayout target, String goal, int total) {
        target.removeAllViews();
        int warm = Math.max(8, total / 7);
        int cool = Math.max(5, total / 12);
        int main = total - warm - cool;
        String[] blocks = exercises(goal);
        int first = main / 2;
        int second = main - first;
        String plan = warm + " min · Attivazione\n" + "Mobilità dinamica, appoggi rapidi e tecnica di presa semplice.\n\n" +
                first + " min · " + blocks[0] + "\n" + blocks[1] + "\n\n" +
                second + " min · " + blocks[2] + "\n" + blocks[3] + "\n\n" +
                cool + " min · Ritorno alla calma\nMobilità, respirazione e breve confronto sulla seduta.";
        target.setTag(plan);
        target.addView(text(goal + " · " + total + " minuti", 19, NAVY, true));
        String[] parts = plan.split("\n\n");
        for (String part : parts) {
            TextView p = text(part, 15, Color.rgb(38, 58, 73), false);
            p.setLineSpacing(0, 1.15f);
            p.setPadding(0, dp(13), 0, 0);
            target.addView(p);
        }
    }

    private String[] exercises(String goal) {
        switch (goal) {
            case "Tecnica di base": return new String[]{"Prese e postura", "Palla frontale rasoterra, mezza altezza e alta: cura posizione delle mani e corpo dietro la palla.", "Spostamenti e tuffo", "Passi di assestamento, presa laterale e tuffi controllati sui due lati."};
            case "Presa e tuffo": return new String[]{"Tecnica di presa", "Sequenze progressive da fermo e dopo spostamento, alternando traiettorie basse e medie.", "Tuffi e seconde palle", "Tuffo, rialzata rapida e intervento su una seconda conclusione."};
            case "Reattività": return new String[]{"Stimolo e risposta", "Partenze su segnale visivo, palline o conclusioni ravvicinate con recupero completo.", "Doppio intervento", "Prima parata vincolata e seconda conclusione casuale; qualità prima della velocità."};
            case "Uscite alte": return new String[]{"Lettura della traiettoria", "Passi d’incrocio, chiamata della palla e presa nel punto più alto senza opposizione.", "Uscita in situazione", "Cross da posizioni diverse con ostacolo passivo, poi pressione controllata."};
            case "Uno contro uno": return new String[]{"Avvicinamento", "Scelta del tempo, frenata e postura di attesa contro conduzione controllata.", "Situazioni reali", "Uno contro uno da angoli e distanze diverse, alternando uscita e difesa della porta."};
            case "Gioco con i piedi": return new String[]{"Controllo orientato", "Ricezione su entrambi i piedi e passaggio corto dopo spostamento rispetto alla porta.", "Costruzione sotto pressione", "Scelte a due o tre soluzioni, palla lunga e rilancio con le mani."};
            case "Forza e mobilità": return new String[]{"Forza funzionale", "Affondi, squat monopodalico assistito, core e atterraggi stabili con carico adeguato.", "Esplosività controllata", "Balzi laterali brevi e partenze, recuperando abbastanza per mantenere tecnica pulita."};
            default: return new String[]{"Tecnica e spostamenti", "Prese, tuffi e lavoro piedi con progressione dal semplice al situazionale.", "Situazioni di gara", "Uscite, uno contro uno e costruzione dal basso con scelte variabili."};
        }
    }

    private AdapterView.OnItemSelectedListener listener(Runnable action) {
        return new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { action.run(); }
            public void onNothingSelected(AdapterView<?> p) {}
        };
    }

    private void confirmDelete(Session s) {
        new AlertDialog.Builder(this).setTitle("Eliminare l’allenamento?")
                .setMessage(formatDate(s.date) + "\n" + s.goal)
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Elimina", (d, w) -> { sessions.remove(s); save(); showHistory(); })
                .show();
    }

    private void load() {
        sessions.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString("sessions", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                sessions.add(new Session(o.optLong("id", i), o.optString("date"), o.optString("goal", GOALS[0]), o.optInt("minutes", 60), o.optString("work"), o.optString("notes")));
            }
        } catch (JSONException ignored) {}
    }

    private void save() {
        JSONArray a = new JSONArray();
        for (Session s : sessions) {
            JSONObject o = new JSONObject();
            try { o.put("id", s.id); o.put("date", s.date); o.put("goal", s.goal); o.put("minutes", s.minutes); o.put("work", s.work); o.put("notes", s.notes); a.put(o); }
            catch (JSONException ignored) {}
        }
        prefs.edit().putString("sessions", a.toString()).apply();
    }

    private List<Session> sorted() {
        List<Session> result = new ArrayList<>(sessions);
        Collections.sort(result, (a, b) -> b.date.compareTo(a.date));
        return result;
    }
    private int totalMinutes() { int n = 0; for (Session s : sessions) n += s.minutes; return n; }
    private String formatDate(String value) { try { return capitalize(pretty.format(iso.parse(value))); } catch (ParseException e) { return value; } }
    private String capitalize(String s) { return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ITALY) + s.substring(1); }
    private int indexOf(String[] items, String value) { for (int i = 0; i < items.length; i++) if (items[i].equals(value)) return i; return 0; }

    private LinearLayout card() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); v.setPadding(dp(16), dp(15), dp(16), dp(14)); v.setBackground(round(Color.WHITE, 14, Color.rgb(222, 230, 235), 1)); marginBottom(v, 12); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private View stat(String title, String value) { LinearLayout v = card(); TextView n = text(value, 27, NAVY, true); TextView l = text(title, 11, MUTED, true); l.setPadding(0, dp(3), 0, 0); v.addView(n); v.addView(l); return v; }
    private TextView section(String s) { TextView v = text(s, 19, NAVY, true); v.setPadding(dp(2), dp(22), 0, dp(11)); return v; }
    private TextView label(String s) { TextView v = text(s, 14, NAVY, true); v.setPadding(dp(2), dp(14), 0, dp(6)); return v; }
    private TextView empty(String s) { TextView v = text(s, 15, MUTED, false); v.setGravity(Gravity.CENTER); v.setPadding(dp(18), dp(28), dp(18), dp(28)); v.setBackground(round(Color.WHITE, 14, Color.rgb(222, 230, 235), 1)); return v; }
    private TextView text(String s, int sp, int color, boolean bold) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v; }
    private TextView pill(String s, int bg, int fg) { TextView v = text(s, 12, fg, true); v.setPadding(dp(10), dp(5), dp(10), dp(5)); v.setBackground(round(bg, 30, Color.TRANSPARENT, 0)); return v; }
    private TextView inputDisplay(String s) { TextView v = text(s, 16, Color.rgb(35, 55, 70), false); v.setPadding(dp(13), dp(13), dp(13), dp(13)); v.setBackground(round(Color.WHITE, 10, Color.rgb(201, 213, 221), 1)); return v; }
    private EditText input(String hint, int type) { EditText v = new EditText(this); v.setHint(hint); v.setTextSize(16); v.setTextColor(Color.rgb(30, 48, 62)); v.setHintTextColor(Color.rgb(135, 149, 159)); v.setPadding(dp(13), dp(11), dp(13), dp(11)); v.setInputType(type); v.setBackground(round(Color.WHITE, 10, Color.rgb(201, 213, 221), 1)); return v; }
    private Spinner spinner(String[] values) { Spinner v = new Spinner(this); ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) { @Override public View getView(int p, View c, ViewGroup parent) { TextView t = (TextView) super.getView(p, c, parent); t.setTextSize(16); t.setTextColor(Color.rgb(35, 55, 70)); t.setPadding(dp(13), dp(13), dp(13), dp(13)); return t; }}; v.setAdapter(adapter); v.setBackground(round(Color.WHITE, 10, Color.rgb(201, 213, 221), 1)); return v; }
    private Button primary(String s) { Button b = button(s); b.setTextColor(Color.WHITE); b.setBackground(round(GREEN, 12, Color.TRANSPARENT, 0)); b.setMinHeight(dp(52)); return b; }
    private Button secondary(String s) { Button b = button(s); b.setTextColor(NAVY); b.setBackground(round(Color.WHITE, 12, Color.rgb(188, 203, 213), 1)); b.setMinHeight(dp(50)); return b; }
    private Button smallButton(String s) { Button b = button(s); b.setTextColor(NAVY); b.setTextSize(12); b.setBackground(round(Color.rgb(241, 245, 247), 9, Color.rgb(218, 227, 232), 1)); return b; }
    private Button link(String s) { Button b = button(s); b.setTextColor(NAVY); b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); b.setPadding(0, 0, 0, 0); b.setBackgroundColor(Color.TRANSPARENT); return b; }
    private Button inputButton(String s) { Button b = button(s); b.setTextColor(Color.rgb(35, 55, 70)); b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); b.setPadding(dp(13), 0, dp(13), 0); b.setBackground(round(Color.WHITE, 10, Color.rgb(201, 213, 221), 1)); return b; }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false); return b; }
    private GradientDrawable round(int fill, int radius, int stroke, int width) { GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(radius)); if (width > 0) g.setStroke(dp(width), stroke); return g; }
    private View space(int width) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(width), 1)); return v; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private void marginTop(View v, int n) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(n), 0, 0); v.setLayoutParams(p); }
    private void marginBottom(View v, int n) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 0, 0, dp(n)); v.setLayoutParams(p); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    @Override public void onBackPressed() {
        if (homeVisible) super.onBackPressed(); else showHome();
    }

    static class Session {
        final long id; final String date, goal, work, notes; final int minutes;
        Session(long id, String date, String goal, int minutes, String work, String notes) { this.id = id; this.date = date; this.goal = goal; this.minutes = minutes; this.work = work; this.notes = notes; }
    }
}
