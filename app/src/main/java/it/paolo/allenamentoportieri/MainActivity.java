package it.paolo.allenamentoportieri;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(16, 42, 67);
    private static final int GREEN = Color.rgb(38, 242, 173);
    private static final int CYAN = Color.rgb(31, 194, 255);
    private static final int PAGE = Color.rgb(3, 19, 32);
    private static final int SURFACE = Color.rgb(8, 40, 58);
    private static final int TEXT = Color.rgb(238, 250, 255);
    private static final int MUTED = Color.rgb(167, 195, 210);
    private static final String[] GOALS = {"Reattività", "Uscite alte", "Uno contro uno", "Gioco con i piedi", "Forza e mobilità", "Altro"};
    private static final int PICK_PHOTO = 40;
    private static final int EXPORT_BACKUP = 41;
    private static final int IMPORT_BACKUP = 42;
    private static final String REPORT_URL = "https://script.google.com/macros/s/AKfycbzj44wDBpIFxIbDipNGgdGzcAD7pRGnJ_G9VTN5jJ3Nl18yxAdQvCPS0N-qQZkukPnKwA/exec";
    private static final String REPORT_TOKEN = "8bfa288c5a57590e1335f4b7ebb5317360fb8aa092ff8ccb";
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ITALY);
    private final SimpleDateFormat pretty = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALY);
    private final List<Session> sessions = new ArrayList<>();
    private final List<String> goalkeepers = new ArrayList<>();
    private SharedPreferences prefs;
    private LinearLayout content;
    private boolean homeVisible;
    private Calendar historyMonth = Calendar.getInstance();
    private String selectedHistoryDate;
    private final List<String> editorPhotoUris = new ArrayList<>();
    private LinearLayout editorPhotoStrip;
    private String historyGoalFilter = "Tutte le tipologie";
    private String historySeasonFilter = "Tutte le stagioni";
    private String historyPersonFilter = "Tutti i portieri";
    private final ExecutorService backupExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private String language = "it";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("goalkeeper_training", MODE_PRIVATE);
        language = prefs.getString("language", "it");
        load();
        loadGoalkeepers();
        showSportTechIntro();
    }

    private void showSportTechIntro() {
        FrameLayout splash = new FrameLayout(this); splash.setBackgroundResource(R.drawable.bg_sport_tech);
        LinearLayout center = new LinearLayout(this); center.setOrientation(LinearLayout.VERTICAL); center.setGravity(Gravity.CENTER); center.setPadding(dp(20), 0, dp(20), 0);
        ImageView keeper = new ImageView(this); keeper.setImageResource(R.drawable.header_keeper_sport_tech); keeper.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        keeper.setAlpha(0f); keeper.setTranslationX(-dp(260)); keeper.setRotation(-8f); keeper.setScaleX(.72f); keeper.setScaleY(.72f);
        center.addView(keeper, new LinearLayout.LayoutParams(-1, dp(245)));
        TextView title = text("ALLENAMENTO PORTIERI", 27, TEXT, true); title.setGravity(Gravity.CENTER); title.setAlpha(0f); title.setLetterSpacing(.06f); center.addView(title);
        TextView motto = text("ALLENAMENTO  •  DISCIPLINA  •  RISULTATI", 11, GREEN, true); motto.setGravity(Gravity.CENTER); motto.setPadding(0, dp(9), 0, 0); motto.setAlpha(0f); center.addView(motto);
        splash.addView(center, new FrameLayout.LayoutParams(-1, -1)); setContentView(splash);
        keeper.post(() -> keeper.animate().alpha(1f).translationX(0).rotation(0).scaleX(1f).scaleY(1f).setDuration(950).withEndAction(() -> {
            title.animate().alpha(1f).setDuration(350).start(); motto.animate().alpha(1f).setDuration(500).withEndAction(() -> splash.postDelayed(() -> splash.animate().alpha(0f).setDuration(350).withEndAction(this::showHome).start(), 600)).start();
        }).start());
    }

    private void base(String title, String subtitle) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_sport_tech);

        FrameLayout headFrame = new FrameLayout(this);
        headFrame.setBackground(techGradient(Color.rgb(4, 27, 46), Color.rgb(5, 54, 65), CYAN, 1));
        ImageView athlete = new ImageView(this); athlete.setImageResource(R.drawable.header_keeper_sport_tech); athlete.setScaleType(ImageView.ScaleType.CENTER_CROP); athlete.setAlpha(.42f);
        FrameLayout.LayoutParams athleteLp = new FrameLayout.LayoutParams(dp(145), dp(82), Gravity.RIGHT | Gravity.CENTER_VERTICAL); headFrame.addView(athlete, athleteLp);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(20), dp(18), dp(20), dp(16));
        head.setBackgroundColor(Color.TRANSPARENT);
        Button menu = button("☰");
        menu.setTextSize(25);
        menu.setTextColor(Color.WHITE);
        menu.setBackgroundColor(Color.TRANSPARENT);
        menu.setPadding(0, 0, dp(14), 0);
        menu.setOnClickListener(v -> showMainMenu());
        head.addView(menu, new LinearLayout.LayoutParams(dp(48), dp(54)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        String shownTitle = tr(title); TextView t = text(title, 25, Color.WHITE, true);
        if ("Allenamento Portieri".equals(title)) { SpannableString styled = new SpannableString(shownTitle); int accentFrom = Math.max(0, shownTitle.lastIndexOf(' ') + 1); styled.setSpan(new ForegroundColorSpan(GREEN), accentFrom, shownTitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); t.setText(styled); }
        titles.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView st = text(subtitle, 14, Color.rgb(202, 219, 231), false);
            st.setPadding(0, dp(4), 0, 0);
            titles.addView(st);
        }
        head.addView(titles, weight());
        headFrame.addView(head, new FrameLayout.LayoutParams(-1, -2));
        root.addView(headFrame);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showMainMenu() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(8));
        panel.setBackground(techGradient(SURFACE, Color.rgb(4, 27, 43), CYAN, 1));
        LinearLayout menuHead = row(); TextView menuTitle = text("ALLENAMENTO PORTIERI", 21, TEXT, true); menuHead.addView(menuTitle, weight()); Button closeMenu = smallButton("✕"); menuHead.addView(closeMenu, new LinearLayout.LayoutParams(dp(48), dp(44))); panel.addView(menuHead);
        TextView motto = text("ALLENAMENTO  •  DISCIPLINA  •  RISULTATI", 10, GREEN, true); motto.setPadding(dp(2), 0, 0, dp(14)); panel.addView(motto);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).create(); closeMenu.setOnClickListener(v -> dialog.dismiss());
        addMenuItem(panel, "⌂  Home", dialog, this::showHome);
        addMenuItem(panel, "▣  Storico e calendario", dialog, this::showHistory);
        addMenuItem(panel, "＋  Nuovo allenamento", dialog, () -> showEditor(null));
        addMenuItem(panel, "♙  Gestione portieri", dialog, this::showGoalkeepers);
        addMenuItem(panel, "⚽  Suggerimenti", dialog, this::showPlanner);
        addMenuItem(panel, "↕  Backup e trasferimento", dialog, this::showBackupPanel);
        addMenuItem(panel, "?  Istruzioni di utilizzo", dialog, this::showInstructions);
        addMenuItem(panel, "✉  Segnalazioni", dialog, this::showReports);
        addMenuItem(panel, "🌐  Lingua", dialog, this::showLanguage);
        addMenuItem(panel, "ⓘ  Informazioni", dialog, this::showAbout);
        dialog.setOnShowListener(v -> { tint(dialog); if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(round(Color.TRANSPARENT, 18, Color.TRANSPARENT, 0)); });
        dialog.show();
    }

    private void addMenuItem(LinearLayout panel, String label, AlertDialog dialog, Runnable action) {
        LinearLayout item = row(); item.setPadding(dp(15), 0, dp(12), 0); item.setBackground(rippleRound(Color.rgb(7, 47, 65), 12, Color.rgb(26, 121, 151), 1));
        TextView name = text(label, 15, TEXT, true); item.addView(name, weight()); TextView arrow = text("›", 27, GREEN, false); item.addView(arrow);
        item.setOnClickListener(v -> { dialog.dismiss(); action.run(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, 0, 0, dp(7)); panel.addView(item, lp);
    }

    private void showInstructions() {
        String message = "1. Crea la lista dei portieri.\n\n2. Registra una seduta indicando data, durata, partecipanti ed esercizi.\n\n3. Scrivi un esercizio per riga oppure separa le fasi con il punto e virgola (;).\n\n4. Apri Schemi esercizi per disegnare o correggere ogni esercizio. Trascina gli estremi delle frecce per cambiarne direzione.\n\n5. Consulta lo storico dal calendario e usa i filtri.\n\n6. Esporta periodicamente un backup per trasferire o conservare i dati.";
        tint(new AlertDialog.Builder(this).setTitle(tr("Istruzioni di utilizzo")).setMessage(tr(message)).setPositiveButton(tr("Ho capito"), null).show());
    }

    private void showAbout() {
        homeVisible = false; base("Informazioni", "Allenamento Portieri"); LinearLayout box = card();
        box.addView(text("ALLENAMENTO PORTIERI", 22, GREEN, true)); TextView copy = text("Diario, archivio e schemi per le sedute dei portieri.", 16, TEXT, false); copy.setPadding(0, dp(12), 0, dp(18)); box.addView(copy);
        TextView signature = text("Paolo Free 1.0", 18, TEXT, true); signature.setGravity(Gravity.CENTER); signature.setPadding(dp(12), dp(14), dp(12), dp(14)); signature.setBackground(round(Color.rgb(7, 47, 65), 12, GREEN, 1)); box.addView(signature); content.addView(box);
    }

    private void showLanguage() {
        homeVisible = false; base("Lingua", "Scegli la lingua dell’app");
        String[][] languages = {{"Italiano","it"},{"English","en"},{"Español","es"},{"Français","fr"},{"Deutsch","de"}};
        for (String[] item : languages) { Button choice = secondary((language.equals(item[1]) ? "✓  " : "") + item[0]); choice.setOnClickListener(v -> { language = item[1]; prefs.edit().putString("language", language).apply(); showHome(); }); marginBottom(choice, 9); content.addView(choice); }
    }

    private void showReports() {
        homeVisible = false; base("Segnalazioni", "Invia un suggerimento o segnala un problema");
        EditText sender = input("La tua email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); content.addView(sender);
        final String[] selectedType = {"Suggerimento"}; LinearLayout types = row(); marginTop(types, 10);
        Button suggestion = smallButton("SUGGERIMENTO"); Button bug = smallButton("BUG"); types.addView(suggestion, weight()); types.addView(space(8)); types.addView(bug, weight()); content.addView(types);
        setReportTypeButtons(suggestion, bug, false);
        suggestion.setOnClickListener(v -> { selectedType[0] = "Suggerimento"; setReportTypeButtons(suggestion, bug, false); });
        bug.setOnClickListener(v -> { selectedType[0] = "Bug"; setReportTypeButtons(suggestion, bug, true); });
        EditText message = input("Descrivi il suggerimento o il problema", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); message.setMinLines(6); marginTop(message, 9); content.addView(message);
        Button send = primary("INVIA SEGNALAZIONE"); marginTop(send, 14); content.addView(send);
        send.setOnClickListener(v -> { String from = sender.getText().toString().trim(), body = message.getText().toString().trim(); if (from.isEmpty() || body.isEmpty()) { Toast.makeText(this, tr("Compila email e descrizione"), Toast.LENGTH_SHORT).show(); return; } sendReport(send, selectedType[0], from, body); });
    }

    private void setReportTypeButtons(Button suggestion, Button bug, boolean bugSelected) {
        suggestion.setTextColor(bugSelected ? TEXT : Color.rgb(2, 35, 35)); suggestion.setBackground(bugSelected ? rippleRound(SURFACE, 10, CYAN, 1) : rippleTech(GREEN, CYAN));
        bug.setTextColor(bugSelected ? Color.rgb(2, 35, 35) : TEXT); bug.setBackground(bugSelected ? rippleTech(GREEN, CYAN) : rippleRound(SURFACE, 10, CYAN, 1));
    }

    private void sendReport(Button send, String type, String email, String message) {
        send.setEnabled(false); send.setText(tr("INVIO IN CORSO…"));
        networkExecutor.execute(() -> {
            boolean ok = false; String result = tr("Invio non riuscito. Controlla la connessione."); HttpURLConnection connection = null;
            try {
                JSONObject data = new JSONObject(); data.put("token", REPORT_TOKEN); data.put("tipo", type); data.put("email", email); data.put("messaggio", message);
                byte[] bytes = data.toString().getBytes("UTF-8"); connection = (HttpURLConnection)new URL(REPORT_URL).openConnection(); connection.setConnectTimeout(15000); connection.setReadTimeout(20000); connection.setInstanceFollowRedirects(true); connection.setRequestMethod("POST"); connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
                int code = connection.getResponseCode(); ok = code >= 200 && code < 400; result = ok ? tr("Segnalazione inviata") : tr("Invio non riuscito. Riprova più tardi.");
            } catch (Exception ignored) { } finally { if (connection != null) connection.disconnect(); }
            boolean sent = ok; String finalResult = result; runOnUiThread(() -> { send.setEnabled(true); send.setText(tr("INVIA SEGNALAZIONE")); Toast.makeText(this, finalResult, Toast.LENGTH_LONG).show(); if (sent) showHome(); });
        });
    }

    private void showBackupPanel() {
        homeVisible = false; base("Backup e trasferimento", "Porta i dati su un altro cellulare");
        content.addView(text("Esporta un file completo con allenamenti, portieri, foto e schemi. Sul nuovo telefono usa Importa backup.", 15, MUTED, false));
        Button export = primary("ESPORTA BACKUP"); export.setOnClickListener(v -> exportBackup()); marginTop(export, 18); content.addView(export);
        Button importButton = secondary("IMPORTA BACKUP"); importButton.setOnClickListener(v -> importBackup()); marginTop(importButton, 10); content.addView(importButton);
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

        TextView recent = section("Ultimo allenamento");
        content.addView(recent);
        if (sessions.isEmpty()) {
            content.addView(empty("Non hai ancora registrato allenamenti.\nPremi il pulsante verde per iniziare."));
        } else {
            List<Session> sorted = sorted();
            content.addView(sessionCard(sorted.get(0)));
        }

        LinearLayout planCard = card(); marginTop(planCard, 8);
        TextView plan = text("⚽  Prepara la prossima seduta", 19, TEXT, true);
        planCard.addView(plan);
        TextView copy = text("Scegli obiettivo e durata: l’app prepara una proposta completa che puoi adattare e salvare.", 15, MUTED, false);
        copy.setPadding(dp(2), dp(10), dp(2), dp(12));
        planCard.addView(copy);
        Button ideas = secondary("⚽  SUGGERISCI ALLENAMENTO");
        ideas.setOnClickListener(v -> showPlanner());
        planCard.addView(ideas); content.addView(planCard);

    }

    private void showGoalkeepers() {
        homeVisible = false;
        base("Gestione portieri", "Crea la lista da usare negli allenamenti");
        Button back = link("‹  Torna alla home");
        back.setOnClickListener(v -> showHome());
        content.addView(back);
        if (goalkeepers.isEmpty()) content.addView(empty("La lista è vuota.\nAggiungi il primo portiere."));
        for (String name : new ArrayList<>(goalkeepers)) {
            LinearLayout item = card();
            LinearLayout line = row();
            TextView title = text(name, 18, NAVY, true);
            line.addView(title, weight());
            TextView count = pill(trainingCount(name) + " sedute", Color.rgb(10, 58, 75), TEXT);
            line.addView(count);
            item.addView(line);
            LinearLayout actions = row();
            Button rename = smallButton("MODIFICA");
            rename.setOnClickListener(v -> goalkeeperDialog(name));
            actions.addView(rename, weight()); actions.addView(space(8));
            Button delete = smallButton("ELIMINA DALLA LISTA");
            delete.setTextColor(Color.rgb(190, 50, 50));
            delete.setOnClickListener(v -> tint(new AlertDialog.Builder(this).setTitle("Eliminare " + name + " dalla lista?").setMessage("Gli allenamenti già registrati resteranno invariati.").setNegativeButton("Annulla", null).setPositiveButton("Elimina", (d, w) -> { goalkeepers.remove(name); saveGoalkeepers(); showGoalkeepers(); }).show()));
            actions.addView(delete, weight()); marginTop(actions, 10); item.addView(actions); content.addView(item);
        }
        Button add = primary("＋  AGGIUNGI PORTIERE");
        add.setOnClickListener(v -> goalkeeperDialog(null));
        marginTop(add, 10); content.addView(add);
    }

    private void goalkeeperDialog(String oldName) {
        EditText field = input("Nome e cognome", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        if (oldName != null) field.setText(oldName);
        int padding = dp(20); FrameLayout wrap = new FrameLayout(this); wrap.setPadding(padding, dp(8), padding, 0); wrap.addView(field);
        tint(new AlertDialog.Builder(this).setTitle(oldName == null ? "Nuovo portiere" : "Modifica portiere").setView(wrap).setNegativeButton("Annulla", null).setPositiveButton("Salva", (d, w) -> {
            String name = field.getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this, "Inserisci il nome", Toast.LENGTH_SHORT).show(); return; }
            if (oldName != null && !oldName.equals(name)) renameGoalkeeperInSessions(oldName, name);
            if (oldName != null) goalkeepers.remove(oldName);
            if (!goalkeepers.contains(name)) goalkeepers.add(name);
            Collections.sort(goalkeepers, String.CASE_INSENSITIVE_ORDER); saveGoalkeepers(); save(); showGoalkeepers();
        }).show());
    }

    private void renameGoalkeeperInSessions(String oldName, String newName) {
        for (int i = 0; i < sessions.size(); i++) {
            Session s = sessions.get(i); List<String> names = splitPeople(s.participants); boolean changed = false;
            for (int p = 0; p < names.size(); p++) if (names.get(p).equals(oldName)) { names.set(p, newName); changed = true; }
            if (changed) sessions.set(i, new Session(s.id, s.date, s.goal, s.minutes, s.work, s.notes, s.season, joinLines(names), new ArrayList<>(s.photoUris), new ArrayList<>(s.diagrams)));
        }
    }

    private void addGoalkeeperCounts(LinearLayout target) {
        LinearLayout counts = card();
        if (goalkeepers.isEmpty()) counts.addView(text("Nessun portiere nella lista.", 14, MUTED, false));
        else for (String name : goalkeepers) { int n = trainingCount(name); TextView line = text(name + "  ·  " + n + (n == 1 ? " allenamento" : " allenamenti"), 15, NAVY, true); line.setPadding(0, dp(6), 0, dp(6)); counts.addView(line); }
        target.addView(counts);
    }

    private int trainingCount(String name) { int count = 0; for (Session s : sessions) if (splitPeople(s.participants).contains(name)) count++; return count; }

    private void showHistory() {
        homeVisible = false;
        base("Calendario allenamenti", sessions.size() + (sessions.size() == 1 ? " giornata registrata" : " giornate registrate"));
        Button back = link("‹  Torna alla home");
        back.setOnClickListener(v -> showHome());
        content.addView(back);

        if (selectedHistoryDate == null) {
            selectedHistoryDate = sessions.isEmpty() ? iso.format(new Date()) : sorted().get(0).date;
            try { historyMonth.setTime(iso.parse(selectedHistoryDate)); } catch (Exception ignored) {}
        }
        renderCalendar();
    }

    private void renderCalendar() {
        content.removeViews(1, content.getChildCount() - 1);
        LinearLayout filters = card();
        filters.addView(text("Filtra gli allenamenti", 17, NAVY, true));
        String[] goalFilters = withFirst("Tutte le tipologie", GOALS);
        Spinner goalFilter = spinner(goalFilters);
        goalFilter.setSelection(indexOf(goalFilters, historyGoalFilter));
        filters.addView(goalFilter);
        String[] seasonFilters = withFirst("Tutte le stagioni", seasons());
        Spinner seasonFilter = spinner(seasonFilters);
        seasonFilter.setSelection(indexOf(seasonFilters, historySeasonFilter));
        filterMargin(seasonFilter); filters.addView(seasonFilter);
        String[] personFilters = withFirst("Tutti i portieri", people());
        Spinner personFilter = spinner(personFilters);
        personFilter.setSelection(indexOf(personFilters, historyPersonFilter));
        filterMargin(personFilter); filters.addView(personFilter);
        Button apply = secondary("APPLICA FILTRI");
        marginTop(apply, 10);
        apply.setOnClickListener(v -> {
            historyGoalFilter = String.valueOf(goalFilter.getSelectedItem());
            historySeasonFilter = String.valueOf(seasonFilter.getSelectedItem());
            historyPersonFilter = String.valueOf(personFilter.getSelectedItem());
            selectedHistoryDate = null;
            renderCalendar();
        });
        filters.addView(apply);
        content.addView(filters);

        LinearLayout calendarCard = card();
        LinearLayout monthBar = row();
        Button previous = smallButton("‹");
        previous.setTextSize(22);
        previous.setOnClickListener(v -> { historyMonth.add(Calendar.MONTH, -1); selectedHistoryDate = null; renderCalendar(); });
        monthBar.addView(previous, new LinearLayout.LayoutParams(dp(44), dp(40)));
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", appLocale());
        TextView month = text(capitalize(monthFormat.format(historyMonth.getTime())), 19, NAVY, true);
        month.setGravity(Gravity.CENTER);
        monthBar.addView(month, new LinearLayout.LayoutParams(0, dp(40), 1));
        Button next = smallButton("›");
        next.setTextSize(22);
        next.setOnClickListener(v -> { historyMonth.add(Calendar.MONTH, 1); selectedHistoryDate = null; renderCalendar(); });
        monthBar.addView(next, new LinearLayout.LayoutParams(dp(44), dp(40)));
        calendarCard.addView(monthBar);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        String[] weekdays = calendarWeekdays();
        for (String day : weekdays) {
            TextView h = text(day, 12, MUTED, true);
            h.setGravity(Gravity.CENTER);
            grid.addView(h, gridCell());
        }
        Calendar first = (Calendar) historyMonth.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int emptyDays = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        for (int i = 0; i < emptyDays; i++) grid.addView(new View(this), gridCell());
        int max = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= max; day++) {
            Calendar value = (Calendar) first.clone();
            value.set(Calendar.DAY_OF_MONTH, day);
            String key = iso.format(value.getTime());
            boolean trained = hasTraining(key);
            boolean selected = key.equals(selectedHistoryDate);
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(2), dp(2), dp(2), dp(2));
            cell.setBackground(selected ? techGradient(Color.rgb(8, 92, 82), Color.rgb(5, 48, 65), Color.rgb(118, 255, 213), 3) : trained ? round(Color.TRANSPARENT, 30, GREEN, 2) : round(Color.TRANSPARENT, 10, Color.TRANSPARENT, 0));
            TextView number = text(String.valueOf(day), 14, selected ? Color.WHITE : NAVY, selected || trained);
            number.setGravity(Gravity.CENTER);
            cell.addView(number, new LinearLayout.LayoutParams(-1, -1));
            cell.setOnClickListener(v -> { selectedHistoryDate = key; renderCalendar(); });
            grid.addView(cell, gridCell());
        }
        marginTop(grid, 10);
        calendarCard.addView(grid);
        content.addView(calendarCard);

        if (selectedHistoryDate != null) {
            content.addView(section(formatDate(selectedHistoryDate)));
            boolean found = false;
            for (Session s : sorted()) if (s.date.equals(selectedHistoryDate) && matchesFilters(s)) { content.addView(sessionCard(s)); found = true; }
            if (!found) content.addView(empty("Nessun allenamento in questa giornata."));
        } else {
            TextView hint = text("Tocca un giorno per vedere o aggiungere un allenamento.", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(dp(8), dp(8), dp(8), dp(14));
            content.addView(hint);
        }
        Button add = primary("＋  NUOVO ALLENAMENTO");
        add.setOnClickListener(v -> showEditor(null));
        marginTop(add, 12);
        content.addView(add);

    }

    private GridLayout.LayoutParams gridCell() {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = dp(38);
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(1), dp(1), dp(1), dp(1));
        return p;
    }

    private boolean hasTraining(String date) {
        for (Session s : sessions) if (s.date.equals(date) && matchesFilters(s)) return true;
        return false;
    }

    private boolean matchesFilters(Session s) {
        if (!historyGoalFilter.equals("Tutte le tipologie") && !historyGoalFilter.equals(s.goal)) return false;
        if (!historySeasonFilter.equals("Tutte le stagioni") && !historySeasonFilter.equals(s.season)) return false;
        if (!historyPersonFilter.equals("Tutti i portieri") && !splitPeople(s.participants).contains(historyPersonFilter)) return false;
        return true;
    }

    private View sessionCard(Session s) {
        LinearLayout card = card();
        LinearLayout summary = row();
        LinearLayout summaryText = new LinearLayout(this); summaryText.setOrientation(LinearLayout.VERTICAL);
        TextView date = text(formatDate(s.date), 16, NAVY, true); summaryText.addView(date);
        TextView shortInfo = text(s.goal + (!s.participants.trim().isEmpty() ? "  ·  " + s.participants.replace("\n", ", ") : ""), 13, MUTED, false);
        shortInfo.setMaxLines(1); summaryText.addView(shortInfo); summary.addView(summaryText, weight());
        Button toggle = smallButton("VEDI  ▾"); summary.addView(toggle, new LinearLayout.LayoutParams(dp(82), dp(44))); card.addView(summary);
        LinearLayout details = new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); details.setVisibility(View.GONE);
        LinearLayout badgeRow = row();
        TextView goal = pill(s.goal, GREEN, Color.rgb(2, 35, 35));
        badgeRow.addView(goal);
        TextView duration = pill(s.minutes + " min", Color.rgb(10, 58, 75), TEXT);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-2, -2);
        dp.setMargins(dp(8), 0, 0, 0);
        badgeRow.addView(duration, dp);
        marginTop(badgeRow, 9);
        details.addView(badgeRow);
        TextView season = text("Stagione " + s.season, 13, MUTED, true);
        season.setPadding(0, dp(8), 0, 0);
        details.addView(season);
        if (!s.participants.trim().isEmpty()) {
            TextView people = text("Portieri: " + s.participants.replace("\n", ", "), 14, NAVY, true);
            people.setPadding(0, dp(6), 0, 0);
            details.addView(people);
        }
        if (!s.work.trim().isEmpty()) {
            TextView work = text(numberedExercises(s.work), 15, TEXT, false);
            work.setPadding(0, dp(11), 0, 0);
            details.addView(work);
            Button diagram = smallButton("VEDI SCHEMA ESERCIZI");
            diagram.setOnClickListener(v -> showDiagram(s));
            marginTop(diagram, 10);
            details.addView(diagram);
        }
        if (!s.notes.trim().isEmpty()) {
            TextView notes = text("Note: " + s.notes, 14, MUTED, false);
            notes.setPadding(0, dp(8), 0, 0);
            details.addView(notes);
        }
        if (!s.photoUris.isEmpty()) details.addView(photoGallery(s.photoUris, 130));
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
        details.addView(actions);
        card.addView(details);
        toggle.setOnClickListener(v -> { boolean open = details.getVisibility() == View.VISIBLE; details.setVisibility(open ? View.GONE : View.VISIBLE); toggle.setText(tr(open ? "VEDI  ▾" : "CHIUDI  ▴")); });
        summary.setOnClickListener(v -> toggle.performClick());
        return card;
    }

    private void showEditor(Session existing) {
        homeVisible = false;
        boolean editing = existing != null;
        base(editing ? "Modifica allenamento" : "Nuovo allenamento", "Registra quello che avete svolto");
        Button back = link("‹  Annulla");
        back.setOnClickListener(v -> {
            if (editing) showHistory();
            else showHome();
        });
        content.addView(back);

        Calendar selected = Calendar.getInstance();
        if (editing) try { selected.setTime(iso.parse(existing.date)); } catch (Exception ignored) {}
        final String[] dateValue = {iso.format(selected.getTime())};
        TextView dateLabel = label("Data");
        content.addView(dateLabel);
        Button date = inputButton(formatDate(dateValue[0]));
        date.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
                selected.set(y, m, d);
                dateValue[0] = iso.format(selected.getTime());
                date.setText(formatDate(dateValue[0]));
            }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH));
            picker.show();
            tint(picker);
        });
        content.addView(date);

        content.addView(label("Stagione"));
        String[] seasonValues = seasonsAround(dateValue[0]);
        Spinner season = spinner(seasonValues);
        season.setSelection(indexOf(seasonValues, editing ? existing.season : seasonForDate(dateValue[0])));
        content.addView(season);

        content.addView(label("Chi ha fatto l’allenamento"));
        LinearLayout participantFields = goalkeeperSelector(editing ? existing.participants : "");
        content.addView(participantFields);
        Button managePeople = smallButton("GESTISCI LISTA PORTIERI");
        managePeople.setOnClickListener(v -> showGoalkeepers());
        marginTop(managePeople, 7); content.addView(managePeople);

        content.addView(label("Obiettivo principale"));
        Spinner goal = spinner(GOALS);
        if (editing) goal.setSelection(indexOf(GOALS, existing.goal));
        content.addView(goal);

        content.addView(label("Durata in minuti"));
        EditText minutes = input("Es. 75", InputType.TYPE_CLASS_NUMBER);
        minutes.setText(editing ? String.valueOf(existing.minutes) : "45");
        content.addView(minutes);

        content.addView(label("Sequenza degli esercizi"));
        EditText work = input("Scrivi un esercizio per riga\nEs. Riscaldamento con palla\nTuffi sui due lati\nUscite su cross", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        work.setMinLines(7);
        work.setGravity(Gravity.TOP);
        if (editing) work.setText(existing.work);
        content.addView(work);

        content.addView(label("Note e cose da migliorare (facoltative)"));
        EditText notes = input("Osservazioni sulla seduta", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notes.setMinLines(3);
        notes.setGravity(Gravity.TOP);
        if (editing) notes.setText(existing.notes);
        content.addView(notes);

        content.addView(label("Foto dell’allenamento (puoi sceglierne più di una)"));
        editorPhotoUris.clear();
        if (editing) editorPhotoUris.addAll(existing.photoUris);
        editorPhotoStrip = new LinearLayout(this);
        editorPhotoStrip.setOrientation(LinearLayout.HORIZONTAL);
        renderEditorPhotos();
        HorizontalScrollView photoScroll = new HorizontalScrollView(this);
        photoScroll.addView(editorPhotoStrip);
        content.addView(photoScroll, new LinearLayout.LayoutParams(-1, dp(130)));
        LinearLayout photoActions = row();
        Button choosePhoto = secondary("AGGIUNGI FOTO");
        choosePhoto.setOnClickListener(v -> pickPhoto());
        photoActions.addView(choosePhoto, weight());
        photoActions.addView(space(8));
        Button removePhoto = secondary("RIMUOVI");
        removePhoto.setOnClickListener(v -> { editorPhotoUris.clear(); renderEditorPhotos(); });
        photoActions.addView(removePhoto, weight());
        marginTop(photoActions, 7);
        content.addView(photoActions);

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
            String selectedPeople = collectSelectedGoalkeepers(participantFields);
            if (selectedPeople.isEmpty()) { Toast.makeText(this, "Seleziona almeno un portiere", Toast.LENGTH_SHORT).show(); return; }
            if (editing) sessions.remove(existing);
            sessions.add(new Session(editing ? existing.id : System.currentTimeMillis(), dateValue[0], String.valueOf(goal.getSelectedItem()), mins, work.getText().toString().trim(), notes.getText().toString().trim(), String.valueOf(season.getSelectedItem()), selectedPeople, new ArrayList<>(editorPhotoUris), editing ? new ArrayList<>(existing.diagrams) : new ArrayList<>()));
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

        content.addView(section("Libreria esercizi"));
        String[][] library = exerciseLibrary();
        String[] exerciseNames = new String[library.length];
        for (int i = 0; i < library.length; i++) exerciseNames[i] = library[i][0];
        Spinner exerciseChoice = spinner(exerciseNames);
        content.addView(exerciseChoice);
        LinearLayout exercisePreview = card(); marginTop(exercisePreview, 10); content.addView(exercisePreview);
        Button useExercise = secondary("USA QUESTO ESERCIZIO"); content.addView(useExercise);
        Runnable showExercise = () -> renderLibraryExercise(exercisePreview, library[exerciseChoice.getSelectedItemPosition()]);
        exerciseChoice.setOnItemSelectedListener(listener(showExercise)); showExercise.run();
        useExercise.setOnClickListener(v -> {
            String[] exercise = library[exerciseChoice.getSelectedItemPosition()];
            showEditorWithPlan(exercise[1], 45, exercise[0] + "\n\nObiettivo: " + exercise[2] + "\n\n" + exercise[3]);
        });

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
        content.addView(label("Stagione"));
        String currentSeason = seasonForDate(today);
        content.addView(inputDisplay(currentSeason));
        content.addView(label("Chi ha fatto l’allenamento"));
        LinearLayout participantFields = goalkeeperSelector("");
        content.addView(participantFields);
        Button managePeople = smallButton("GESTISCI LISTA PORTIERI");
        managePeople.setOnClickListener(v -> showGoalkeepers());
        marginTop(managePeople, 7); content.addView(managePeople);
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
        content.addView(label("Foto dell’allenamento (puoi sceglierne più di una)"));
        editorPhotoUris.clear();
        editorPhotoStrip = new LinearLayout(this);
        editorPhotoStrip.setOrientation(LinearLayout.HORIZONTAL);
        renderEditorPhotos();
        HorizontalScrollView photoScroll = new HorizontalScrollView(this);
        photoScroll.addView(editorPhotoStrip);
        content.addView(photoScroll, new LinearLayout.LayoutParams(-1, dp(130)));
        Button choosePhoto = secondary("AGGIUNGI FOTO");
        choosePhoto.setOnClickListener(v -> pickPhoto());
        content.addView(choosePhoto);
        Button save = primary("SALVA ALLENAMENTO");
        marginTop(save, 20);
        save.setOnClickListener(v -> {
            String selectedPeople = collectSelectedGoalkeepers(participantFields);
            if (selectedPeople.isEmpty()) { Toast.makeText(this, "Seleziona almeno un portiere", Toast.LENGTH_SHORT).show(); return; }
            sessions.add(new Session(System.currentTimeMillis(), today, goalValue, mins, work.getText().toString().trim(), notes.getText().toString().trim(), currentSeason, selectedPeople, new ArrayList<>(editorPhotoUris), new ArrayList<>()));
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

    private void renderLibraryExercise(LinearLayout target, String[] exercise) {
        target.removeAllViews();
        target.addView(text(exercise[0], 18, TEXT, true));
        TextView objective = text("OBIETTIVO\n" + exercise[2], 14, MUTED, false); objective.setPadding(0, dp(12), 0, dp(10)); target.addView(objective);
        TextView instructions = text(exercise[3], 15, TEXT, false); instructions.setLineSpacing(dp(2), 1.12f); target.addView(instructions);
    }

    private String[][] exerciseLibrary() {
        return new String[][]{
                {"01 · Tre coni", "Reattività", "Pensiero e movimenti veloci per raggiungere la zona da dove arriva il pallone.",
                        "1. Posiziona tre coni davanti alla porta, mezzo metro all’interno dell’area piccola.\n2. Il portiere parte al centro della porta; un compagno, sul dischetto del rigore, dispone dei palloni.\n3. Il compagno indica un cono a voce alta. Il portiere corre verso quel cono, torna al centro e si prepara a prendere il pallone."},
                {"02 · Un cono", "Reattività", "Cambiare rapidamente posizione e reagire a un tiro proveniente da un’angolazione differente.",
                        "1. Posiziona un cono al centro dell’area piccola.\n2. Il portiere parte al centro della porta; un compagno si trova sull’angolo dell’area di rigore.\n3. Il portiere corre verso il cono, lo aggira e fronteggia l’angolo.\n4. Il compagno calcia e il portiere blocca il pallone."},
                {"03 · Reazione al rimbalzo", "Reattività", "Reagire al rimbalzo imprevedibile di un pallone basso o alto.",
                        "1. Posiziona due coni come riferimento per la zona del rimbalzo.\n2. Un compagno si colloca a un metro dall’area piccola; il portiere parte di lato rispetto alla porta.\n3. Al «via» il portiere corre verso i coni e il compagno lancia il pallone a terra tra i coni, oppure in aria per variare.\n4. Il portiere legge il movimento del pallone e lo afferra."},
                {"04 · Presa bassa in tuffo", "Reattività", "Allenare la presa bassa in tuffo sui due lati.",
                        "1. Il compagno si posiziona sul dischetto del rigore.\n2. Calcia la palla bassa verso un lato della porta.\n3. Il portiere si tuffa nella direzione del pallone e lo blocca.\n4. Alterna regolarmente il lato destro e quello sinistro."},
                {"05 · Torsione di 180°", "Reattività", "Allenare riflessi e reazione a un pallone che arriva all’improvviso.",
                        "1. Il portiere si posiziona di lato rispetto alla porta.\n2. Il compagno si colloca vicino al dischetto del rigore.\n3. Il compagno calcia verso l’angolo destro o sinistro e grida «via».\n4. Il portiere si gira rapidamente, legge la traiettoria e afferra il pallone."}
        };
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
        tint(new AlertDialog.Builder(this).setTitle("Eliminare l’allenamento?")
                .setMessage(formatDate(s.date) + "\n" + s.goal)
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Elimina", (d, w) -> { sessions.remove(s); save(); showHistory(); })
                .show());
    }

    private void load() {
        sessions.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString("sessions", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                List<String> photos = new ArrayList<>();
                JSONArray savedPhotos = o.optJSONArray("photoUris");
                if (savedPhotos != null) for (int p = 0; p < savedPhotos.length(); p++) if (!savedPhotos.optString(p).isEmpty()) photos.add(savedPhotos.optString(p));
                else if (!o.optString("photoUri").isEmpty()) photos.add(o.optString("photoUri"));
                String savedDate = o.optString("date");
                List<String> diagrams = jsonStrings(o.optJSONArray("diagrams"));
                sessions.add(new Session(o.optLong("id", i), savedDate, o.optString("goal", GOALS[0]), o.optInt("minutes", 60), o.optString("work"), o.optString("notes"), o.optString("season", seasonForDate(savedDate)), o.optString("participants"), photos, diagrams));
            }
        } catch (JSONException ignored) {}
    }

    private void loadGoalkeepers() {
        goalkeepers.clear();
        try { JSONArray a = new JSONArray(prefs.getString("goalkeepers", "[]")); for (int i = 0; i < a.length(); i++) { String name = a.optString(i).trim(); if (!name.isEmpty() && !goalkeepers.contains(name)) goalkeepers.add(name); } } catch (Exception ignored) {}
        for (Session s : sessions) for (String name : splitPeople(s.participants)) if (!goalkeepers.contains(name)) goalkeepers.add(name);
        Collections.sort(goalkeepers, String.CASE_INSENSITIVE_ORDER); saveGoalkeepers();
    }

    private void saveGoalkeepers() { JSONArray a = new JSONArray(); for (String name : goalkeepers) a.put(name); prefs.edit().putString("goalkeepers", a.toString()).apply(); }

    private void save() {
        JSONArray a = new JSONArray();
        for (Session s : sessions) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", s.id); o.put("date", s.date); o.put("goal", s.goal); o.put("minutes", s.minutes); o.put("work", s.work); o.put("notes", s.notes); o.put("season", s.season); o.put("participants", s.participants);
                JSONArray photos = new JSONArray(); for (String uri : s.photoUris) photos.put(uri); o.put("photoUris", photos);
                JSONArray diagrams = new JSONArray(); for (String value : s.diagrams) diagrams.put(value); o.put("diagrams", diagrams); a.put(o);
            }
            catch (JSONException ignored) {}
        }
        prefs.edit().putString("sessions", a.toString()).apply();
    }

    private void exportBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "allenamento_portieri_backup_" + iso.format(new Date()) + ".apbackup");
        startActivityForResult(intent, EXPORT_BACKUP);
    }

    private void importBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, IMPORT_BACKUP);
    }

    private void writeBackup(Uri destination) {
        // Snapshot the data on the UI thread: sessions/goalkeepers must not be
        // touched from the background thread while it's exporting.
        List<Session> snapshot = new ArrayList<>(sessions);
        List<String> roster = new ArrayList<>(goalkeepers);
        AlertDialog progress = showProgress("Esportazione backup in corso…");
        backupExecutor.execute(() -> {
            String message;
            try (OutputStream raw = getContentResolver().openOutputStream(destination); ZipOutputStream zip = new ZipOutputStream(raw)) {
                JSONArray data = new JSONArray();
                for (Session s : snapshot) {
                    JSONObject o = sessionJson(s);
                    JSONArray photoNames = new JSONArray();
                    for (int i = 0; i < s.photoUris.size(); i++) {
                        String name = "photos/" + s.id + "_" + i + ".img";
                        try (InputStream in = getContentResolver().openInputStream(Uri.parse(s.photoUris.get(i)))) {
                            if (in == null) continue;
                            zip.putNextEntry(new ZipEntry(name)); copy(in, zip); zip.closeEntry(); photoNames.put(name);
                        } catch (Exception ignored) {}
                    }
                    o.put("backupPhotos", photoNames); data.put(o);
                }
                JSONObject backup = new JSONObject(); backup.put("version", 3); backup.put("sessions", data);
                JSONArray rosterJson = new JSONArray(); for (String name : roster) rosterJson.put(name); backup.put("goalkeepers", rosterJson);
                backup.put("diagramTemplates", getSharedPreferences("exercise_diagram_library", MODE_PRIVATE).getString("templates", "{}"));
                zip.putNextEntry(new ZipEntry("data.json"));
                zip.write(backup.toString().getBytes("UTF-8"));
                zip.closeEntry();
                message = "Backup esportato correttamente";
            } catch (Exception e) { message = "Errore durante il backup: " + e.getMessage(); }
            String finalMessage = message;
            runOnUiThread(() -> { progress.dismiss(); Toast.makeText(this, finalMessage, Toast.LENGTH_LONG).show(); });
        });
    }

    private void readBackup(Uri source) {
        AlertDialog progress = showProgress("Importazione backup in corso…");
        backupExecutor.execute(() -> {
            File photoDir = new File(getFilesDir(), "backup_photos");
            if (!photoDir.exists()) photoDir.mkdirs();
            String json = null;
            Map<String, String> importedPhotos = new HashMap<>();
            String error = null;
            List<Session> newSessions = new ArrayList<>();
            List<String> newRosterNames = new ArrayList<>();
            String importedDiagramTemplates = null;
            int imported = 0;
            try (InputStream raw = getContentResolver().openInputStream(source); ZipInputStream zip = new ZipInputStream(raw)) {
                ZipEntry entry;
                byte[] buffer = new byte[16384];
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.getName().equals("data.json")) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream(); copy(zip, out); json = out.toString("UTF-8");
                    } else if (entry.getName().startsWith("photos/") && !entry.isDirectory()) {
                        String safeName = new File(entry.getName()).getName();
                        File target = new File(photoDir, System.currentTimeMillis() + "_" + safeName);
                        try (FileOutputStream out = new FileOutputStream(target)) { int n; while ((n = zip.read(buffer)) > 0) out.write(buffer, 0, n); }
                        importedPhotos.put(entry.getName(), Uri.fromFile(target).toString());
                    }
                    zip.closeEntry();
                }
                if (json == null) throw new Exception("File dati non trovato");
                JSONArray a; JSONArray importedRoster = null;
                if (json.trim().startsWith("[")) a = new JSONArray(json); else { JSONObject root = new JSONObject(json); a = root.optJSONArray("sessions"); importedRoster = root.optJSONArray("goalkeepers"); importedDiagramTemplates = root.optString("diagramTemplates", null); }
                if (a == null) throw new Exception("Elenco allenamenti non trovato");
                if (importedRoster != null) for (int r = 0; r < importedRoster.length(); r++) { String name = importedRoster.optString(r).trim(); if (!name.isEmpty()) newRosterNames.add(name); }
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.getJSONObject(i); List<String> photos = new ArrayList<>(); JSONArray names = o.optJSONArray("backupPhotos");
                    if (names != null) for (int p = 0; p < names.length(); p++) { String uri = importedPhotos.get(names.optString(p)); if (uri != null) photos.add(uri); }
                    long id = o.optLong("id", System.currentTimeMillis() + i);
                    String date = o.optString("date", iso.format(new Date()));
                    newSessions.add(new Session(id, date, o.optString("goal", GOALS[0]), o.optInt("minutes", 60), o.optString("work"), o.optString("notes"), o.optString("season", seasonForDate(date)), o.optString("participants"), photos, jsonStrings(o.optJSONArray("diagrams"))));
                    imported++;
                }
            } catch (Exception e) { error = "Backup non valido: " + e.getMessage(); }
            String finalError = error;
            String finalDiagramTemplates = importedDiagramTemplates;
            int finalImported = imported;
            runOnUiThread(() -> {
                progress.dismiss();
                if (finalError != null) { Toast.makeText(this, finalError, Toast.LENGTH_LONG).show(); return; }
                // Applica i risultati alle liste vere solo qui, sul thread UI.
                for (String name : newRosterNames) if (!goalkeepers.contains(name)) goalkeepers.add(name);
                for (Session s : newSessions) { removeSessionById(s.id); sessions.add(s); }
                for (Session s : sessions) for (String name : splitPeople(s.participants)) if (!goalkeepers.contains(name)) goalkeepers.add(name);
                Collections.sort(goalkeepers, String.CASE_INSENSITIVE_ORDER); saveGoalkeepers();
                if (finalDiagramTemplates != null && !finalDiagramTemplates.isEmpty()) getSharedPreferences("exercise_diagram_library", MODE_PRIVATE).edit().putString("templates", finalDiagramTemplates).apply();
                save();
                Toast.makeText(this, finalImported + " allenamenti importati", Toast.LENGTH_LONG).show();
                showHome();
            });
        });
    }

    private AlertDialog tint(AlertDialog d) {
        Button pos = d.getButton(AlertDialog.BUTTON_POSITIVE);
        if (pos != null) pos.setTextColor(GREEN);
        Button neg = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (neg != null) neg.setTextColor(MUTED);
        return d;
    }

    private AlertDialog showProgress(String message) {
        LinearLayout box = row();
        box.setPadding(dp(24), dp(20), dp(24), dp(20));
        ProgressBar bar = new ProgressBar(this);
        box.addView(bar);
        TextView label = text(message, 15, NAVY, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(16), 0, 0, 0);
        box.addView(label, lp);
        return new AlertDialog.Builder(this).setView(box).setCancelable(false).show();
    }

    private JSONObject sessionJson(Session s) throws JSONException { JSONObject o = new JSONObject(); o.put("id", s.id); o.put("date", s.date); o.put("goal", s.goal); o.put("minutes", s.minutes); o.put("work", s.work); o.put("notes", s.notes); o.put("season", s.season); o.put("participants", s.participants); JSONArray diagrams = new JSONArray(); for (String value : s.diagrams) diagrams.put(value); o.put("diagrams", diagrams); return o; }
    private List<String> jsonStrings(JSONArray values) { List<String> result = new ArrayList<>(); if (values != null) for (int i = 0; i < values.length(); i++) result.add(values.optString(i)); return result; }
    private void removeSessionById(long id) { for (int i = sessions.size() - 1; i >= 0; i--) if (sessions.get(i).id == id) sessions.remove(i); }
    private void copy(InputStream in, OutputStream out) throws Exception { byte[] buffer = new byte[16384]; int n; while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n); }

    private List<Session> sorted() {
        List<Session> result = new ArrayList<>(sessions);
        Collections.sort(result, (a, b) -> b.date.compareTo(a.date));
        return result;
    }
    private int totalMinutes() { int n = 0; for (Session s : sessions) n += s.minutes; return n; }
    private String formatDate(String value) { try { return capitalize(new SimpleDateFormat("EEEE d MMMM yyyy", appLocale()).format(iso.parse(value))); } catch (ParseException e) { return value; } }
    private String capitalize(String s) { return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase(appLocale()) + s.substring(1); }
    private Locale appLocale() { if ("en".equals(language)) return Locale.ENGLISH; if ("es".equals(language)) return new Locale("es"); if ("fr".equals(language)) return Locale.FRENCH; if ("de".equals(language)) return Locale.GERMAN; return Locale.ITALIAN; }
    private String[] calendarWeekdays() { if ("en".equals(language)) return new String[]{"M","T","W","T","F","S","S"}; if ("es".equals(language)) return new String[]{"L","M","X","J","V","S","D"}; if ("fr".equals(language)) return new String[]{"L","M","M","J","V","S","D"}; if ("de".equals(language)) return new String[]{"M","D","M","D","F","S","S"}; return new String[]{"L","M","M","G","V","S","D"}; }
    private int indexOf(String[] items, String value) { for (int i = 0; i < items.length; i++) if (items[i].equals(value)) return i; return 0; }
    private String[] withFirst(String first, String[] values) { String[] result = new String[values.length + 1]; result[0] = first; System.arraycopy(values, 0, result, 1, values.length); return result; }
    private String seasonForDate(String date) { try { Calendar c = Calendar.getInstance(); c.setTime(iso.parse(date)); int y = c.get(Calendar.YEAR); if (c.get(Calendar.MONTH) < Calendar.JULY) y--; return y + "/" + String.valueOf(y + 1).substring(2); } catch (Exception e) { return "2026/27"; } }
    private String[] seasonsAround(String date) { String center = seasonForDate(date); int y; try { y = Integer.parseInt(center.substring(0, 4)); } catch (Exception e) { y = Calendar.getInstance().get(Calendar.YEAR); } return new String[]{(y - 1) + "/" + String.valueOf(y).substring(2), center, (y + 1) + "/" + String.valueOf(y + 2).substring(2)}; }
    private String[] seasons() { List<String> values = new ArrayList<>(); for (Session s : sessions) if (!s.season.isEmpty() && !values.contains(s.season)) values.add(s.season); Collections.sort(values, Collections.reverseOrder()); if (values.isEmpty()) values.add(seasonForDate(iso.format(new Date()))); return values.toArray(new String[0]); }
    private List<String> splitPeople(String value) { List<String> result = new ArrayList<>(); if (value == null) return result; for (String raw : value.split("[\\n,;]+")) { String name = raw.trim(); if (!name.isEmpty() && !result.contains(name)) result.add(name); } return result; }
    private String[] people() { List<String> values = new ArrayList<>(); for (Session s : sessions) for (String name : splitPeople(s.participants)) if (!values.contains(name)) values.add(name); Collections.sort(values, String.CASE_INSENSITIVE_ORDER); return values.toArray(new String[0]); }
    private String numberedExercises(String work) { StringBuilder out = new StringBuilder(); int n = 1; for (String raw : work.split("\\n+")) { String line = raw.trim(); if (!line.isEmpty()) { if (out.length() > 0) out.append("\n"); out.append(n++).append(". ").append(line); } } return out.toString(); }
    private LinearLayout goalkeeperSelector(String selectedNames) { LinearLayout box = card(); List<String> selected = splitPeople(selectedNames); if (goalkeepers.isEmpty()) box.addView(text("Prima aggiungi almeno un portiere alla lista.", 14, MUTED, false)); else for (String name : goalkeepers) { CheckBox check = new CheckBox(this); check.setText(name); check.setTextSize(16); check.setTextColor(TEXT); check.setButtonTintList(ColorStateList.valueOf(GREEN)); check.setPadding(dp(2), dp(5), dp(2), dp(5)); check.setChecked(selected.contains(name)); box.addView(check); } return box; }
    private String collectSelectedGoalkeepers(LinearLayout container) { List<String> names = new ArrayList<>(); for (int i = 0; i < container.getChildCount(); i++) if (container.getChildAt(i) instanceof CheckBox) { CheckBox check = (CheckBox) container.getChildAt(i); if (check.isChecked()) names.add(check.getText().toString()); } return joinLines(names); }
    private String joinLines(List<String> names) { StringBuilder out = new StringBuilder(); for (String name : names) { if (out.length() > 0) out.append("\n"); out.append(name); } return out.toString(); }

    private void showDiagram(Session session) {
        List<String> descriptions = new ArrayList<>();
        for (String raw : session.work.split("\\n+")) if (!raw.trim().isEmpty()) descriptions.add(raw.trim());
        if (descriptions.isEmpty()) return;
        new ExerciseEditorDialog(this, descriptions, session.diagrams, saved -> {
            session.diagrams.clear();
            session.diagrams.addAll(saved);
            save();
            Toast.makeText(this, descriptions.size() == 1 ? "Schema salvato" : descriptions.size() + " schemi salvati", Toast.LENGTH_SHORT).show();
        }).show();
    }

    private String tr(String it) {
        if (it == null || "it".equals(language)) return it;
        String[] v;
        switch (it) {
            case "Allenamento Portieri": v=a("Goalkeeper Training","Entrenamiento de Porteros","Entraînement des Gardiens","Torwarttraining"); break;
            case "ALLENAMENTO PORTIERI": v=a("GOALKEEPER TRAINING","ENTRENAMIENTO DE PORTEROS","ENTRAÎNEMENT DES GARDIENS","TORWARTTRAINING"); break;
            case "Diario e programmazione delle sedute": v=a("Training diary and planning","Diario y planificación de sesiones","Journal et planification des séances","Trainingstagebuch und Planung"); break;
            case "Non hai ancora registrato allenamenti.\nPremi il pulsante verde per iniziare.": v=a("No training recorded yet.\nPress the green button to begin.","Aún no hay entrenamientos.\nPulsa el botón verde para empezar.","Aucun entraînement enregistré.\nAppuyez sur le bouton vert pour commencer.","Noch kein Training gespeichert.\nDrücke zum Starten die grüne Taste."); break;
            case "Scegli obiettivo e durata: l’app prepara una proposta completa che puoi adattare e salvare.": v=a("Choose a goal and duration: the app creates a complete plan you can adapt and save.","Elige objetivo y duración: la aplicación prepara un plan que puedes adaptar y guardar.","Choisissez l’objectif et la durée : l’application prépare une séance adaptable.","Wähle Ziel und Dauer: Die App erstellt einen anpassbaren Trainingsplan."); break;
            case "ALLENAMENTI": v=a("TRAININGS","ENTRENAMIENTOS","ENTRAÎNEMENTS","TRAININGS"); break;
            case "MINUTI TOTALI": v=a("TOTAL MINUTES","MINUTOS TOTALES","MINUTES TOTALES","MINUTEN GESAMT"); break;
            case "＋  REGISTRA ALLENAMENTO": v=a("＋  RECORD TRAINING","＋  REGISTRAR ENTRENAMIENTO","＋  ENREGISTRER LA SÉANCE","＋  TRAINING SPEICHERN"); break;
            case "Ultimo allenamento": v=a("Latest training","Último entrenamiento","Dernier entraînement","Letztes Training"); break;
            case "⚽  Prepara la prossima seduta": v=a("⚽  Plan the next session","⚽  Prepara la próxima sesión","⚽  Préparer la prochaine séance","⚽  Nächstes Training planen"); break;
            case "⚽  SUGGERISCI ALLENAMENTO": v=a("⚽  SUGGEST TRAINING","⚽  SUGERIR ENTRENAMIENTO","⚽  SUGGÉRER UNE SÉANCE","⚽  TRAINING VORSCHLAGEN"); break;
            case "⌂  Home": v=a("⌂  Home","⌂  Inicio","⌂  Accueil","⌂  Start"); break;
            case "▣  Storico e calendario": v=a("▣  History and calendar","▣  Historial y calendario","▣  Historique et calendrier","▣  Verlauf und Kalender"); break;
            case "＋  Nuovo allenamento": v=a("＋  New training","＋  Nuevo entrenamiento","＋  Nouvel entraînement","＋  Neues Training"); break;
            case "♙  Gestione portieri": v=a("♙  Goalkeepers","♙  Gestión de porteros","♙  Gestion des gardiens","♙  Torwartverwaltung"); break;
            case "⚽  Suggerimenti": v=a("⚽  Suggestions","⚽  Sugerencias","⚽  Suggestions","⚽  Vorschläge"); break;
            case "↕  Backup e trasferimento": v=a("↕  Backup and transfer","↕  Copia y transferencia","↕  Sauvegarde et transfert","↕  Sicherung und Übertragung"); break;
            case "?  Istruzioni di utilizzo": v=a("?  Instructions","?  Instrucciones","?  Instructions","?  Anleitung"); break;
            case "✉  Segnalazioni": v=a("✉  Feedback","✉  Comentarios","✉  Signalements","✉  Rückmeldungen"); break;
            case "🌐  Lingua": v=a("🌐  Language","🌐  Idioma","🌐  Langue","🌐  Sprache"); break;
            case "ⓘ  Informazioni": v=a("ⓘ  About","ⓘ  Información","ⓘ  Informations","ⓘ  Informationen"); break;
            case "Calendario allenamenti": v=a("Training calendar","Calendario de entrenamientos","Calendrier des entraînements","Trainingskalender"); break;
            case "Filtra gli allenamenti": v=a("Filter trainings","Filtrar entrenamientos","Filtrer les entraînements","Trainings filtern"); break;
            case "Tutte le tipologie": v=a("All types","Todos los tipos","Tous les types","Alle Typen"); break;
            case "Tutte le stagioni": v=a("All seasons","Todas las temporadas","Toutes les saisons","Alle Saisons"); break;
            case "Tutti i portieri": v=a("All goalkeepers","Todos los porteros","Tous les gardiens","Alle Torhüter"); break;
            case "Reattività": v=a("Reactivity","Reactividad","Réactivité","Reaktion"); break;
            case "Uscite alte": v=a("High balls","Salidas aéreas","Sorties aériennes","Hohe Bälle"); break;
            case "Uno contro uno": v=a("One-on-one","Uno contra uno","Un contre un","Eins gegen eins"); break;
            case "Gioco con i piedi": v=a("Footwork","Juego con los pies","Jeu au pied","Fußspiel"); break;
            case "Forza e mobilità": v=a("Strength and mobility","Fuerza y movilidad","Force et mobilité","Kraft und Mobilität"); break;
            case "Altro": v=a("Other","Otro","Autre","Andere"); break;
            case "APPLICA FILTRI": v=a("APPLY FILTERS","APLICAR FILTROS","APPLIQUER LES FILTRES","FILTER ANWENDEN"); break;
            case "＋  NUOVO ALLENAMENTO": v=a("＋  NEW TRAINING","＋  NUEVO ENTRENAMIENTO","＋  NOUVEL ENTRAÎNEMENT","＋  NEUES TRAINING"); break;
            case "VEDI  ▾": v=a("OPEN  ▾","VER  ▾","VOIR  ▾","ÖFFNEN  ▾"); break;
            case "CHIUDI  ▴": v=a("CLOSE  ▴","CERRAR  ▴","FERMER  ▴","SCHLIESSEN  ▴"); break;
            case "VEDI SCHEMA ESERCIZI": v=a("VIEW EXERCISE DIAGRAM","VER ESQUEMA","VOIR LE SCHÉMA","ÜBUNGSPLAN ANZEIGEN"); break;
            case "MODIFICA": v=a("EDIT","EDITAR","MODIFIER","BEARBEITEN"); break;
            case "ELIMINA": v=a("DELETE","ELIMINAR","SUPPRIMER","LÖSCHEN"); break;
            case "Nuovo allenamento": v=a("New training","Nuevo entrenamiento","Nouvel entraînement","Neues Training"); break;
            case "Modifica allenamento": v=a("Edit training","Editar entrenamiento","Modifier l’entraînement","Training bearbeiten"); break;
            case "Registra quello che avete svolto": v=a("Record what you did","Registra lo realizado","Enregistrez le travail effectué","Durchgeführtes Training erfassen"); break;
            case "Data": v=a("Date","Fecha","Date","Datum"); break;
            case "Stagione": v=a("Season","Temporada","Saison","Saison"); break;
            case "Chi ha fatto l’allenamento": v=a("Participants","Participantes","Participants","Teilnehmer"); break;
            case "Obiettivo principale": v=a("Main goal","Objetivo principal","Objectif principal","Hauptziel"); break;
            case "Durata in minuti": v=a("Duration in minutes","Duración en minutos","Durée en minutes","Dauer in Minuten"); break;
            case "Sequenza degli esercizi": v=a("Exercise sequence","Secuencia de ejercicios","Séquence des exercices","Übungsablauf"); break;
            case "Note e cose da migliorare (facoltative)": v=a("Notes and improvements (optional)","Notas y mejoras (opcional)","Notes et améliorations (facultatif)","Notizen und Verbesserungen (optional)"); break;
            case "AGGIUNGI FOTO": v=a("ADD PHOTOS","AÑADIR FOTOS","AJOUTER DES PHOTOS","FOTOS HINZUFÜGEN"); break;
            case "SALVA ALLENAMENTO": v=a("SAVE TRAINING","GUARDAR ENTRENAMIENTO","ENREGISTRER","TRAINING SPEICHERN"); break;
            case "SALVA MODIFICHE": v=a("SAVE CHANGES","GUARDAR CAMBIOS","ENREGISTRER LES MODIFICATIONS","ÄNDERUNGEN SPEICHERN"); break;
            case "Gestione portieri": v=a("Goalkeepers","Gestión de porteros","Gestion des gardiens","Torwartverwaltung"); break;
            case "＋  AGGIUNGI PORTIERE": v=a("＋  ADD GOALKEEPER","＋  AÑADIR PORTERO","＋  AJOUTER UN GARDIEN","＋  TORWART HINZUFÜGEN"); break;
            case "Backup e trasferimento": v=a("Backup and transfer","Copia y transferencia","Sauvegarde et transfert","Sicherung und Übertragung"); break;
            case "Porta i dati su un altro cellulare": v=a("Move data to another phone","Transfiere los datos a otro teléfono","Transférez les données vers un autre téléphone","Daten auf ein anderes Handy übertragen"); break;
            case "Esporta un file completo con allenamenti, portieri, foto e schemi. Sul nuovo telefono usa Importa backup.": v=a("Export trainings, goalkeepers, photos and diagrams. Use Import backup on the new phone.","Exporta entrenamientos, porteros, fotos y esquemas. Usa Importar copia en el nuevo teléfono.","Exportez entraînements, gardiens, photos et schémas. Utilisez Importer sur le nouveau téléphone.","Exportiere Trainings, Torhüter, Fotos und Pläne. Nutze Backup importieren auf dem neuen Handy."); break;
            case "ESPORTA BACKUP": v=a("EXPORT BACKUP","EXPORTAR COPIA","EXPORTER LA SAUVEGARDE","BACKUP EXPORTIEREN"); break;
            case "IMPORTA BACKUP": v=a("IMPORT BACKUP","IMPORTAR COPIA","IMPORTER LA SAUVEGARDE","BACKUP IMPORTIEREN"); break;
            case "Informazioni": v=a("About","Información","Informations","Informationen"); break;
            case "Diario, archivio e schemi per le sedute dei portieri.": v=a("Diary, archive and diagrams for goalkeeper training.","Diario, archivo y esquemas para entrenamientos de porteros.","Journal, archives et schémas pour l’entraînement des gardiens.","Tagebuch, Archiv und Pläne für das Torwarttraining."); break;
            case "‹  Torna alla home": v=a("‹  Back to home","‹  Volver al inicio","‹  Retour à l’accueil","‹  Zurück zur Startseite"); break;
            case "Lingua": v=a("Language","Idioma","Langue","Sprache"); break;
            case "Scegli la lingua dell’app": v=a("Choose the app language","Elige el idioma de la aplicación","Choisissez la langue de l’application","App-Sprache auswählen"); break;
            case "Segnalazioni": v=a("Feedback","Comentarios","Signalements","Rückmeldungen"); break;
            case "Invia un suggerimento o segnala un problema": v=a("Send a suggestion or report a problem","Envía una sugerencia o informa de un problema","Envoyez une suggestion ou signalez un problème","Vorschlag senden oder Problem melden"); break;
            case "La tua email": v=a("Your email","Tu correo electrónico","Votre e-mail","Ihre E-Mail"); break;
            case "Suggerimento": v=a("Suggestion","Sugerencia","Suggestion","Vorschlag"); break;
            case "SUGGERIMENTO": v=a("SUGGESTION","SUGERENCIA","SUGGESTION","VORSCHLAG"); break;
            case "BUG": v=a("BUG","ERROR","BUG","FEHLER"); break;
            case "Descrivi il suggerimento o il problema": v=a("Describe the suggestion or problem","Describe la sugerencia o el problema","Décrivez la suggestion ou le problème","Beschreiben Sie den Vorschlag oder das Problem"); break;
            case "INVIA SEGNALAZIONE": v=a("SEND FEEDBACK","ENVIAR","ENVOYER","SENDEN"); break;
            case "INVIO IN CORSO…": v=a("SENDING…","ENVIANDO…","ENVOI…","WIRD GESENDET…"); break;
            case "Segnalazione inviata": v=a("Feedback sent","Mensaje enviado","Signalement envoyé","Rückmeldung gesendet"); break;
            case "Invio non riuscito. Controlla la connessione.": v=a("Could not send. Check your connection.","No se pudo enviar. Comprueba la conexión.","Échec de l’envoi. Vérifiez la connexion.","Senden fehlgeschlagen. Verbindung prüfen."); break;
            case "Invio non riuscito. Riprova più tardi.": v=a("Could not send. Try again later.","No se pudo enviar. Inténtalo más tarde.","Échec de l’envoi. Réessayez plus tard.","Senden fehlgeschlagen. Später erneut versuchen."); break;
            case "Compila email e descrizione": v=a("Enter email and description","Introduce el correo y la descripción","Saisissez l’e-mail et la description","E-Mail und Beschreibung eingeben"); break;
            case "Nessuna app email disponibile": v=a("No email app available","No hay aplicación de correo","Aucune application e-mail disponible","Keine E-Mail-App verfügbar"); break;
            case "Annulla": case "‹  Annulla": v=a("Cancel","Cancelar","Annuler","Abbrechen"); break;
            case "Salva": v=a("Save","Guardar","Enregistrer","Speichern"); break;
            case "Chiudi": v=a("Close","Cerrar","Fermer","Schließen"); break;
            case "Ho capito": v=a("Got it","Entendido","Compris","Verstanden"); break;
            default: return it;
        }
        return "en".equals(language) ? v[0] : "es".equals(language) ? v[1] : "fr".equals(language) ? v[2] : v[3];
    }
    private String[] a(String en, String es, String fr, String de) { return new String[]{en,es,fr,de}; }

    private LinearLayout card() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); v.setPadding(dp(16), dp(15), dp(16), dp(14)); v.setBackground(techGradient(SURFACE, Color.rgb(5, 31, 47), CYAN, 1)); v.setElevation(dp(3)); marginBottom(v, 12); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private View stat(String title, String value) { LinearLayout v = card(); LinearLayout top = row(); ImageView icon = new ImageView(this); if (title.startsWith("ALLENAMENTI")) icon.setImageResource(R.drawable.ic_allenamento_portieri); else { icon.setImageResource(android.R.drawable.ic_menu_recent_history); icon.setColorFilter(GREEN); } top.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28))); top.addView(space(8)); TextView n = text(value, 27, TEXT, true); top.addView(n); v.addView(top); TextView l = text(title, 11, MUTED, true); l.setPadding(0, dp(6), 0, 0); v.addView(l); return v; }
    private TextView section(String s) { TextView v = text(s, 19, NAVY, true); v.setPadding(dp(2), dp(22), 0, dp(11)); return v; }
    private TextView label(String s) { TextView v = text(s, 14, NAVY, true); v.setPadding(dp(2), dp(14), 0, dp(6)); return v; }
    private TextView empty(String s) { TextView v = text(s, 15, MUTED, false); v.setGravity(Gravity.CENTER); v.setPadding(dp(18), dp(28), dp(18), dp(28)); v.setBackground(techGradient(SURFACE, Color.rgb(5, 31, 47), CYAN, 1)); return v; }
    private TextView text(String s, int sp, int color, boolean bold) { TextView v = new TextView(this); v.setText(tr(s)); v.setTextSize(sp); v.setTextColor(color == NAVY ? TEXT : color); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v; }
    private TextView pill(String s, int bg, int fg) { TextView v = text(s, 12, fg, true); v.setPadding(dp(10), dp(5), dp(10), dp(5)); v.setBackground(round(bg, 30, Color.TRANSPARENT, 0)); return v; }
    private TextView inputDisplay(String s) { TextView v = text(s, 16, TEXT, false); v.setPadding(dp(13), dp(13), dp(13), dp(13)); v.setBackground(techGradient(SURFACE, Color.rgb(5, 31, 47), CYAN, 1)); return v; }
    private EditText input(String hint, int type) { EditText v = new EditText(this); v.setHint(tr(hint)); v.setTextSize(16); v.setTextColor(TEXT); v.setHintTextColor(MUTED); v.setPadding(dp(13), dp(11), dp(13), dp(11)); v.setInputType(type); v.setBackground(techGradient(SURFACE, Color.rgb(5, 31, 47), CYAN, 1)); return v; }
    private Spinner spinner(String[] values) { Spinner v = new Spinner(this); ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) { private TextView style(int p, View c, ViewGroup parent, boolean dropdown) { TextView t = (TextView)(dropdown ? super.getDropDownView(p, c, parent) : super.getView(p, c, parent)); t.setText(tr(getItem(p))); t.setTextSize(15); t.setTextColor(TEXT); t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(dp(13), 0, dp(13), 0); t.setMinHeight(dp(46)); if (dropdown) t.setBackgroundColor(Color.rgb(7, 47, 65)); return t; } @Override public View getView(int p, View c, ViewGroup parent) { return style(p,c,parent,false); } @Override public View getDropDownView(int p, View c, ViewGroup parent) { return style(p,c,parent,true); }}; v.setAdapter(adapter); v.setPopupBackgroundDrawable(round(Color.rgb(7, 47, 65), 8, CYAN, 1)); v.setMinimumHeight(0); v.setBackground(withCaret(techGradient(SURFACE, Color.rgb(5, 31, 47), CYAN, 1))); v.setPadding(dp(13), 0, dp(30), 0); v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(48))); return v; }
    private Button primary(String s) { Button b = button(s); b.setTextColor(Color.rgb(2, 35, 35)); b.setBackground(rippleTech(GREEN, Color.rgb(31, 218, 255))); b.setMinHeight(dp(54)); b.setElevation(dp(8)); return b; }
    private Button secondary(String s) { Button b = button(s); b.setTextColor(TEXT); b.setBackground(rippleRound(SURFACE, 12, CYAN, 1)); b.setMinHeight(dp(50)); return b; }
    private Button smallButton(String s) { Button b = button(s); b.setTextColor(TEXT); b.setTextSize(12); b.setBackground(rippleRound(Color.rgb(9, 49, 67), 9, Color.rgb(31, 143, 177), 1)); return b; }
    private Button link(String s) { Button b = button(s); b.setTextColor(GREEN); b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); b.setPadding(0, 0, 0, 0); b.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(55, 38, 242, 173)), null, null)); return b; }
    private Button inputButton(String s) { Button b = button(s); b.setTextColor(TEXT); b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); b.setPadding(dp(13), 0, dp(13), 0); b.setBackground(rippleRound(SURFACE, 10, CYAN, 1)); return b; }
    private Button button(String s) { Button b = new Button(this); b.setText(tr(s)); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false); return b; }
    private ImageView trainingPhoto(String uri, int height) {
        ImageView v = new ImageView(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(180), dp(height)));
        v.setBackground(round(SURFACE, 12, CYAN, 1));
        boolean loaded = false;
        if (uri != null && !uri.isEmpty()) {
            try (InputStream test = getContentResolver().openInputStream(Uri.parse(uri))) {
                if (test != null) { v.setImageURI(Uri.parse(uri)); loaded = true; }
            } catch (Exception ignored) {}
        }
        if (loaded) {
            v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            // La foto originale non è più raggiungibile (es. rimossa dalla galleria):
            // mostriamo un'icona invece di lasciare il riquadro vuoto.
            v.setScaleType(ImageView.ScaleType.CENTER);
            v.setImageResource(android.R.drawable.ic_menu_report_image);
            v.setColorFilter(MUTED);
        }
        return v;
    }
    private View photoGallery(List<String> uris, int height) { HorizontalScrollView scroll = new HorizontalScrollView(this); LinearLayout strip = new LinearLayout(this); strip.setOrientation(LinearLayout.HORIZONTAL); strip.setPadding(0, dp(11), 0, 0); for (String uri : uris) { ImageView photo = trainingPhoto(uri, height); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(180), dp(height)); p.setMargins(0, 0, dp(8), 0); strip.addView(photo, p); } scroll.addView(strip); return scroll; }
    private void renderEditorPhotos() { if (editorPhotoStrip == null) return; editorPhotoStrip.removeAllViews(); if (editorPhotoUris.isEmpty()) { TextView hint = text("Nessuna foto selezionata", 14, MUTED, false); hint.setGravity(Gravity.CENTER_VERTICAL); editorPhotoStrip.addView(hint, new LinearLayout.LayoutParams(dp(240), dp(120))); return; } for (String uri : editorPhotoUris) { ImageView photo = trainingPhoto(uri, 120); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(160), dp(120)); p.setMargins(0, 0, dp(8), 0); editorPhotoStrip.addView(photo, p); } }
    private GradientDrawable round(int fill, int radius, int stroke, int width) { GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(radius)); if (width > 0) g.setStroke(dp(width), stroke); return g; }
    private GradientDrawable techGradient(int start, int end, int stroke, int width) { GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end}); g.setCornerRadius(dp(14)); if (width > 0) g.setStroke(dp(width), stroke); return g; }
    private Drawable techPageBackground() { return new LayerDrawable(new Drawable[]{techGradient(PAGE, Color.rgb(4, 32, 45), 0, 0), new TechLinesDrawable(CYAN)}); }
    private Drawable rippleTech(int start, int end) { GradientDrawable base = techGradient(start, end, Color.rgb(92, 255, 214), 1); GradientDrawable mask = round(Color.WHITE, 14, Color.TRANSPARENT, 0); return new RippleDrawable(ColorStateList.valueOf(Color.argb(65, 255, 255, 255)), base, mask); }
    private Drawable rippleRound(int fill, int radius) { return rippleRound(fill, radius, Color.TRANSPARENT, 0); }
    private Drawable rippleRound(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable base = round(fill, radius, stroke, strokeWidth);
        GradientDrawable mask = round(Color.WHITE, radius, Color.TRANSPARENT, 0);
        int rippleColor = Color.argb(45, 0, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), base, mask);
    }
    private Drawable withCaret(GradientDrawable base) {
        LayerDrawable layered = new LayerDrawable(new Drawable[]{base, new CaretDrawable(MUTED)});
        layered.setLayerGravity(1, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layered.setLayerInset(1, 0, 0, dp(14), 0);
        layered.setLayerSize(1, dp(11), dp(7));
        return layered;
    }
    static class CaretDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        CaretDrawable(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            Path p = new Path();
            p.moveTo(b.left, b.top);
            p.lineTo(b.right, b.top);
            p.lineTo(b.centerX(), b.bottom);
            p.close();
            c.drawPath(p, paint);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
    static class TechLinesDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        TechLinesDrawable(int color) { paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setAlpha(22); }
        @Override public void draw(Canvas c) { Rect b = getBounds(); float step = Math.max(70, b.width() / 5f); for (float x = -b.height(); x < b.width(); x += step) c.drawLine(x, b.top, x + b.height(), b.bottom, paint); float y = b.bottom - b.height() * .18f; c.drawLine(b.left, y, b.right, y, paint); c.drawRect(b.width() * .23f, y, b.width() * .77f, b.bottom, paint); }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
    private View space(int width) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(width), 1)); return v; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private void marginTop(View v, int n) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(n), 0, 0); v.setLayoutParams(p); }
    private void marginBottom(View v, int n) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 0, 0, dp(n)); v.setLayoutParams(p); }
    private void filterMargin(View v) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48)); p.setMargins(0, dp(8), 0, 0); v.setLayoutParams(p); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_PHOTO);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null && requestCode == EXPORT_BACKUP) { writeBackup(data.getData()); return; }
        if (resultCode == RESULT_OK && data != null && data.getData() != null && requestCode == IMPORT_BACKUP) { readBackup(data.getData()); return; }
        if (requestCode == PICK_PHOTO && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) addPhotoUri(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) addPhotoUri(data.getData());
            renderEditorPhotos();
        }
    }

    private void addPhotoUri(Uri uri) { try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {} if (!editorPhotoUris.contains(uri.toString())) editorPhotoUris.add(uri.toString()); }

    @Override public void onBackPressed() {
        if (homeVisible) super.onBackPressed(); else showHome();
    }

    static class ExerciseDiagramView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String description;
        ExerciseDiagramView(Context context, String description) { super(context); this.description = description == null ? "" : description.toLowerCase(Locale.ITALY); setBackgroundColor(Color.rgb(239, 247, 241)); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w = getWidth(), h = getHeight(), pad = w * .07f;
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(54, 145, 83)); c.drawRoundRect(pad, pad, w - pad, h - pad, 18, 18, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4); paint.setColor(Color.WHITE);
            c.drawRoundRect(pad, pad, w - pad, h - pad, 18, 18, paint);
            c.drawLine(pad, h / 2, w - pad, h / 2, paint); c.drawCircle(w / 2, h / 2, w * .12f, paint);
            c.drawRect(w * .29f, pad, w * .71f, h * .25f, paint); c.drawRect(w * .29f, h * .75f, w * .71f, h - pad, paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(255, 213, 79));
            float keeperX = w / 2, keeperY = h * .79f; c.drawCircle(keeperX, keeperY, 16, paint);
            paint.setColor(Color.rgb(29, 74, 122));
            int players = description.contains("uno contro uno") ? 1 : description.contains("cross") || description.contains("uscit") ? 3 : 2;
            for (int i = 0; i < players; i++) { float x = w * (.32f + i * .18f); float y = h * (.30f + (i % 2) * .12f); c.drawCircle(x, y, 14, paint); drawArrow(c, x, y + 18, keeperX + (i - 1) * 35, keeperY - 24); }
            if (description.contains("lateral") || description.contains("tuff")) { paint.setColor(Color.rgb(255, 213, 79)); c.drawCircle(w * .32f, h * .80f, 12, paint); c.drawCircle(w * .68f, h * .80f, 12, paint); }
            if (description.contains("cono") || description.contains("slalom") || description.contains("appoggi")) { paint.setColor(Color.rgb(255, 132, 38)); for (int i = 0; i < 4; i++) c.drawCircle(w * (.32f + i * .12f), h * .58f, 8, paint); }
            paint.setColor(Color.WHITE); paint.setTextSize(28); paint.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("SCHEMA INDICATIVO", pad + 14, h - pad - 14, paint);
        }
        private void drawArrow(Canvas c, float x1, float y1, float x2, float y2) { paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(5); paint.setColor(Color.WHITE); c.drawLine(x1, y1, x2, y2, paint); double a = Math.atan2(y2-y1, x2-x1); Path p = new Path(); p.moveTo(x2,y2); p.lineTo((float)(x2-22*Math.cos(a-.5)),(float)(y2-22*Math.sin(a-.5))); p.moveTo(x2,y2); p.lineTo((float)(x2-22*Math.cos(a+.5)),(float)(y2-22*Math.sin(a+.5))); c.drawPath(p, paint); paint.setStyle(Paint.Style.FILL); }
    }

    static class Session {
        final long id; final String date, goal, work, notes, season, participants; final int minutes; final List<String> photoUris, diagrams;
        Session(long id, String date, String goal, int minutes, String work, String notes, String season, String participants, List<String> photoUris, List<String> diagrams) { this.id = id; this.date = date; this.goal = goal; this.minutes = minutes; this.work = work; this.notes = notes; this.season = season; this.participants = participants == null ? "" : participants; this.photoUris = photoUris == null ? new ArrayList<>() : photoUris; this.diagrams = diagrams == null ? new ArrayList<>() : diagrams; }
    }
}
