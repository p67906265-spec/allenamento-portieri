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
    private static final int GREEN = Color.rgb(18, 184, 134);
    private static final int PAGE = Color.rgb(242, 246, 248);
    private static final int MUTED = Color.rgb(74, 92, 108);
    private static final String[] GOALS = {"Reattività", "Uscite alte", "Uno contro uno", "Gioco con i piedi", "Forza e mobilità", "Altro"};
    private static final int PICK_PHOTO = 40;
    private static final int EXPORT_BACKUP = 41;
    private static final int IMPORT_BACKUP = 42;
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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("goalkeeper_training", MODE_PRIVATE);
        load();
        loadGoalkeepers();
        showHome();
    }

    private void base(String title, String subtitle) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(20), dp(18), dp(20), dp(16));
        head.setBackgroundColor(NAVY);
        Button menu = button("☰");
        menu.setTextSize(25);
        menu.setTextColor(Color.WHITE);
        menu.setBackgroundColor(Color.TRANSPARENT);
        menu.setPadding(0, 0, dp(14), 0);
        menu.setOnClickListener(v -> showMainMenu());
        head.addView(menu, new LinearLayout.LayoutParams(dp(48), dp(54)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(title, 25, Color.WHITE, true);
        titles.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView st = text(subtitle, 14, Color.rgb(202, 219, 231), false);
            st.setPadding(0, dp(4), 0, 0);
            titles.addView(st);
        }
        head.addView(titles, weight());
        root.addView(head);

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
        panel.setPadding(dp(12), dp(8), dp(12), dp(8));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Allenamento Portieri").setView(panel).setNegativeButton("Chiudi", null).create();
        addMenuItem(panel, "⌂  Home", dialog, this::showHome);
        addMenuItem(panel, "▣  Storico e calendario", dialog, this::showHistory);
        addMenuItem(panel, "＋  Nuovo allenamento", dialog, () -> showEditor(null));
        addMenuItem(panel, "♙  Gestione portieri", dialog, this::showGoalkeepers);
        addMenuItem(panel, "⚽  Suggerimenti", dialog, this::showPlanner);
        addMenuItem(panel, "↕  Backup e trasferimento", dialog, this::showBackupPanel);
        addMenuItem(panel, "?  Istruzioni di utilizzo", dialog, this::showInstructions);
        addMenuItem(panel, "ⓘ  Informazioni", dialog, this::showAbout);
        dialog.setOnShowListener(v -> tint(dialog));
        dialog.show();
    }

    private void addMenuItem(LinearLayout panel, String label, AlertDialog dialog, Runnable action) {
        Button item = secondary(label); item.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        item.setOnClickListener(v -> { dialog.dismiss(); action.run(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48)); lp.setMargins(0, 0, 0, dp(6)); panel.addView(item, lp);
    }

    private void showInstructions() {
        String message = "1. Crea la lista dei portieri.\n\n2. Registra una seduta indicando data, durata, partecipanti ed esercizi.\n\n3. Scrivi un esercizio per riga oppure separa le fasi con il punto e virgola (;).\n\n4. Apri Schemi esercizi per disegnare o correggere ogni esercizio. Trascina gli estremi delle frecce per cambiarne direzione.\n\n5. Consulta lo storico dal calendario e usa i filtri.\n\n6. Esporta periodicamente un backup per trasferire o conservare i dati.";
        tint(new AlertDialog.Builder(this).setTitle("Istruzioni di utilizzo").setMessage(message).setPositiveButton("Ho capito", null).show());
    }

    private void showAbout() {
        tint(new AlertDialog.Builder(this).setTitle("Allenamento Portieri").setMessage("Diario, archivio e schemi per le sedute dei portieri.\n\nPaolo Free 1.0").setPositiveButton("Chiudi", null).show());
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

        content.addView(section("Allenamenti per portiere"));
        addGoalkeeperCounts(content);
        Button manageGoalkeepers = secondary("GESTIONE PORTIERI");
        manageGoalkeepers.setOnClickListener(v -> showGoalkeepers());
        content.addView(manageGoalkeepers);

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

        content.addView(section("Backup e trasferimento"));
        LinearLayout backupActions = row();
        Button export = secondary("ESPORTA BACKUP");
        export.setOnClickListener(v -> exportBackup());
        backupActions.addView(export, weight());
        backupActions.addView(space(8));
        Button importButton = secondary("IMPORTA BACKUP");
        importButton.setOnClickListener(v -> importBackup());
        backupActions.addView(importButton, weight());
        content.addView(backupActions);
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
            TextView count = pill(trainingCount(name) + " sedute", Color.rgb(225, 233, 238), NAVY);
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
        filters.addView(label("Tipologia"));
        String[] goalFilters = withFirst("Tutte le tipologie", GOALS);
        Spinner goalFilter = spinner(goalFilters);
        goalFilter.setSelection(indexOf(goalFilters, historyGoalFilter));
        filters.addView(goalFilter);
        filters.addView(label("Stagione"));
        String[] seasonFilters = withFirst("Tutte le stagioni", seasons());
        Spinner seasonFilter = spinner(seasonFilters);
        seasonFilter.setSelection(indexOf(seasonFilters, historySeasonFilter));
        filters.addView(seasonFilter);
        filters.addView(label("Portiere"));
        String[] personFilters = withFirst("Tutti i portieri", people());
        Spinner personFilter = spinner(personFilters);
        personFilter.setSelection(indexOf(personFilters, historyPersonFilter));
        filters.addView(personFilter);
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
        monthBar.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(44)));
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.ITALY);
        TextView month = text(capitalize(monthFormat.format(historyMonth.getTime())), 19, NAVY, true);
        month.setGravity(Gravity.CENTER);
        monthBar.addView(month, weight());
        Button next = smallButton("›");
        next.setTextSize(22);
        next.setOnClickListener(v -> { historyMonth.add(Calendar.MONTH, 1); selectedHistoryDate = null; renderCalendar(); });
        monthBar.addView(next, new LinearLayout.LayoutParams(dp(48), dp(44)));
        calendarCard.addView(monthBar);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        String[] weekdays = {"L", "M", "M", "G", "V", "S", "D"};
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
            cell.setPadding(dp(2), dp(5), dp(2), dp(4));
            cell.setBackground(round(selected ? NAVY : Color.TRANSPARENT, 10, Color.TRANSPARENT, 0));
            TextView number = text(String.valueOf(day), 15, selected ? Color.WHITE : NAVY, selected || trained);
            number.setGravity(Gravity.CENTER);
            cell.addView(number, new LinearLayout.LayoutParams(-1, dp(24)));
            TextView dot = text(trained ? "●" : "", 11, selected ? Color.rgb(94, 234, 192) : GREEN, true);
            dot.setGravity(Gravity.CENTER);
            cell.addView(dot, new LinearLayout.LayoutParams(-1, dp(17)));
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
        p.height = dp(48);
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
        TextView goal = pill(s.goal, GREEN, Color.WHITE);
        badgeRow.addView(goal);
        TextView duration = pill(s.minutes + " min", Color.rgb(225, 233, 238), NAVY);
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
            TextView work = text(numberedExercises(s.work), 15, Color.rgb(35, 55, 70), false);
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
        toggle.setOnClickListener(v -> { boolean open = details.getVisibility() == View.VISIBLE; details.setVisibility(open ? View.GONE : View.VISIBLE); toggle.setText(open ? "VEDI  ▾" : "CHIUDI  ▴"); });
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
    private String formatDate(String value) { try { return capitalize(pretty.format(iso.parse(value))); } catch (ParseException e) { return value; } }
    private String capitalize(String s) { return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ITALY) + s.substring(1); }
    private int indexOf(String[] items, String value) { for (int i = 0; i < items.length; i++) if (items[i].equals(value)) return i; return 0; }
    private String[] withFirst(String first, String[] values) { String[] result = new String[values.length + 1]; result[0] = first; System.arraycopy(values, 0, result, 1, values.length); return result; }
    private String seasonForDate(String date) { try { Calendar c = Calendar.getInstance(); c.setTime(iso.parse(date)); int y = c.get(Calendar.YEAR); if (c.get(Calendar.MONTH) < Calendar.JULY) y--; return y + "/" + String.valueOf(y + 1).substring(2); } catch (Exception e) { return "2026/27"; } }
    private String[] seasonsAround(String date) { String center = seasonForDate(date); int y; try { y = Integer.parseInt(center.substring(0, 4)); } catch (Exception e) { y = Calendar.getInstance().get(Calendar.YEAR); } return new String[]{(y - 1) + "/" + String.valueOf(y).substring(2), center, (y + 1) + "/" + String.valueOf(y + 2).substring(2)}; }
    private String[] seasons() { List<String> values = new ArrayList<>(); for (Session s : sessions) if (!s.season.isEmpty() && !values.contains(s.season)) values.add(s.season); Collections.sort(values, Collections.reverseOrder()); if (values.isEmpty()) values.add(seasonForDate(iso.format(new Date()))); return values.toArray(new String[0]); }
    private List<String> splitPeople(String value) { List<String> result = new ArrayList<>(); if (value == null) return result; for (String raw : value.split("[\\n,;]+")) { String name = raw.trim(); if (!name.isEmpty() && !result.contains(name)) result.add(name); } return result; }
    private String[] people() { List<String> values = new ArrayList<>(); for (Session s : sessions) for (String name : splitPeople(s.participants)) if (!values.contains(name)) values.add(name); Collections.sort(values, String.CASE_INSENSITIVE_ORDER); return values.toArray(new String[0]); }
    private String numberedExercises(String work) { StringBuilder out = new StringBuilder(); int n = 1; for (String raw : work.split("\\n+")) { String line = raw.trim(); if (!line.isEmpty()) { if (out.length() > 0) out.append("\n"); out.append(n++).append(". ").append(line); } } return out.toString(); }
    private LinearLayout goalkeeperSelector(String selectedNames) { LinearLayout box = card(); List<String> selected = splitPeople(selectedNames); if (goalkeepers.isEmpty()) box.addView(text("Prima aggiungi almeno un portiere alla lista.", 14, MUTED, false)); else for (String name : goalkeepers) { CheckBox check = new CheckBox(this); check.setText(name); check.setTextSize(16); check.setTextColor(NAVY); check.setButtonTintList(ColorStateList.valueOf(GREEN)); check.setPadding(dp(2), dp(5), dp(2), dp(5)); check.setChecked(selected.contains(name)); box.addView(check); } return box; }
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

    private LinearLayout card() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); v.setPadding(dp(16), dp(15), dp(16), dp(14)); v.setBackground(round(Color.WHITE, 14, Color.rgb(222, 230, 235), 1)); v.setElevation(dp(2)); marginBottom(v, 12); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private View stat(String title, String value) { LinearLayout v = card(); TextView n = text(value, 27, NAVY, true); TextView l = text(title, 11, MUTED, true); l.setPadding(0, dp(3), 0, 0); v.addView(n); v.addView(l); return v; }
    private TextView section(String s) { TextView v = text(s, 19, NAVY, true); v.setPadding(dp(2), dp(22), 0, dp(11)); return v; }
    private TextView label(String s) { TextView v = text(s, 14, NAVY, true); v.setPadding(dp(2), dp(14), 0, dp(6)); return v; }
    private TextView empty(String s) { TextView v = text(s, 15, MUTED, false); v.setGravity(Gravity.CENTER); v.setPadding(dp(18), dp(28), dp(18), dp(28)); v.setBackground(round(Color.WHITE, 14, Color.rgb(222, 230, 235), 1)); return v; }
    private TextView text(String s, int sp, int color, boolean bold) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v; }
    private TextView pill(String s, int bg, int fg) { TextView v = text(s, 12, fg, true); v.setPadding(dp(10), dp(5), dp(10), dp(5)); v.setBackground(round(bg, 30, Color.TRANSPARENT, 0)); return v; }
    private TextView inputDisplay(String s) { TextView v = text(s, 16, Color.rgb(35, 55, 70), false); v.setPadding(dp(13), dp(13), dp(13), dp(13)); v.setBackground(round(Color.WHITE, 10, Color.rgb(201, 213, 221), 1)); return v; }
    private EditText input(String hint, int type) { EditText v = new EditText(this); v.setHint(hint); v.setTextSize(16); v.setTextColor(Color.rgb(30, 48, 62)); v.setHintTextColor(Color.rgb(135, 149, 159)); v.setPadding(dp(13), dp(11), dp(13), dp(11)); v.setInputType(type); v.setBackground(round(Color.WHITE, 10, Color.rgb(201, 213, 221), 1)); return v; }
    private Spinner spinner(String[] values) { Spinner v = new Spinner(this); ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) { @Override public View getView(int p, View c, ViewGroup parent) { TextView t = (TextView) super.getView(p, c, parent); t.setTextSize(16); t.setTextColor(Color.rgb(35, 55, 70)); t.setPadding(dp(13), dp(13), dp(13), dp(13)); return t; }}; v.setAdapter(adapter); v.setBackground(withCaret(round(Color.WHITE, 10, Color.rgb(201, 213, 221), 1))); v.setPadding(dp(13), dp(13), dp(30), dp(13)); return v; }
    private Button primary(String s) { Button b = button(s); b.setTextColor(Color.WHITE); b.setBackground(rippleRound(GREEN, 12)); b.setMinHeight(dp(52)); return b; }
    private Button secondary(String s) { Button b = button(s); b.setTextColor(NAVY); b.setBackground(rippleRound(Color.WHITE, 12, Color.rgb(188, 203, 213), 1)); b.setMinHeight(dp(50)); return b; }
    private Button smallButton(String s) { Button b = button(s); b.setTextColor(NAVY); b.setTextSize(12); b.setBackground(rippleRound(Color.rgb(241, 245, 247), 9, Color.rgb(218, 227, 232), 1)); return b; }
    private Button link(String s) { Button b = button(s); b.setTextColor(NAVY); b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); b.setPadding(0, 0, 0, 0); b.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(35, 16, 42, 67)), null, null)); return b; }
    private Button inputButton(String s) { Button b = button(s); b.setTextColor(Color.rgb(35, 55, 70)); b.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); b.setPadding(dp(13), 0, dp(13), 0); b.setBackground(rippleRound(Color.WHITE, 10, Color.rgb(201, 213, 221), 1)); return b; }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false); return b; }
    private ImageView trainingPhoto(String uri, int height) {
        ImageView v = new ImageView(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(180), dp(height)));
        v.setBackground(round(Color.rgb(225, 233, 238), 12, Color.TRANSPARENT, 0));
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
    private View space(int width) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(width), 1)); return v; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private void marginTop(View v, int n) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(n), 0, 0); v.setLayoutParams(p); }
    private void marginBottom(View v, int n) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 0, 0, dp(n)); v.setLayoutParams(p); }
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
