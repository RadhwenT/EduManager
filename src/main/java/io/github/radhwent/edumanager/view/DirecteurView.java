package io.github.radhwent.edumanager.view;

import io.github.radhwent.edumanager.dao.AnneeScolaireDAO;
import io.github.radhwent.edumanager.dao.AppSettings;
import io.github.radhwent.edumanager.dao.DirecteurDAO;
import io.github.radhwent.edumanager.dao.UserDAO;
import io.github.radhwent.edumanager.model.AnneeScolaire;
import io.github.radhwent.edumanager.model.User;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;

public class DirecteurView {

    private Stage stage;
    private User currentUser;
    private DirecteurDAO dao = new DirecteurDAO();
    private AnneeScolaire currentAnnee;
    private VBox contentArea;

    private static final String BLUE    = "#2563EB";
    private static final String BG      = "#F1F5F9";
    private static final String WHITE   = "#FFFFFF";
    private static final String SIDEBAR = "#0F172A";
    private static final String BORDER  = "#E2E8F0";
    private static final String TEXT    = "#1E293B";
    private static final String MUTED   = "#64748B";
    private static final String GREEN   = "#10B981";
    private static final String RED     = "#EF4444";
    private static final String AMBER   = "#F59E0B";

    public DirecteurView(Stage stage, User user) {
        this.stage = stage;
        this.currentUser = user;
        List<AnneeScolaire> annees = new AnneeScolaireDAO().findAll();
        if (!annees.isEmpty()) currentAnnee = annees.get(0);
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");

        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(230);
        sidebar.setStyle("-fx-background-color:" + SIDEBAR + ";");

        Label logo = new Label("EduManager");
        logo.setStyle("-fx-text-fill:white;-fx-font-size:18px;-fx-font-weight:bold;-fx-padding:28 20 8 20;");
        Label role = new Label("Espace Directeur");
        role.setStyle("-fx-text-fill:#94A3B8;-fx-font-size:11px;-fx-padding:0 20 24 20;");

        ToggleGroup tg = new ToggleGroup();
        ToggleButton btnDash  = sideBtn("📊  Tableau de Bord", tg);
        ToggleButton btnAnnee = sideBtn("📅  Années Scolaires", tg);
        ToggleButton btnUsers = sideBtn("👥  Utilisateurs", tg);
        ToggleButton btnParam = sideBtn("⚙  Paramètres", tg);

        btnDash .setOnAction(e -> showDashboard());
        btnAnnee.setOnAction(e -> showAnnees());
        btnUsers.setOnAction(e -> showUsers());
        btnParam.setOnAction(e -> showParametres());

        sidebar.getChildren().addAll(logo, role, btnDash, btnAnnee, btnUsers, btnParam);

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(14, 24, 14, 24));
        top.setStyle("-fx-background-color:" + WHITE + ";-fx-border-color:" + BORDER + ";-fx-border-width:0 0 1 0;");
        Label user = new Label("Directeur : " + currentUser.getLogin());
        user.setStyle("-fx-font-weight:bold;-fx-text-fill:" + TEXT + ";");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button logout = new Button("Déconnexion");
        logout.setStyle("-fx-background-color:transparent;-fx-border-color:" + BORDER + ";-fx-border-radius:4;-fx-cursor:hand;-fx-text-fill:" + MUTED + ";");
        logout.setOnAction(e -> new LoginView(stage).show());

        Button btnSettings = new Button("⚙ Paramètres");
        btnSettings.setStyle("-fx-background-color:transparent;-fx-border-color:" + BORDER + ";-fx-border-radius:4;-fx-cursor:hand;-fx-text-fill:" + BLUE + ";-fx-font-size:11px;");
        btnSettings.setOnAction(e -> showAccountSettingsDialog());
        top.getChildren().addAll(user, sp, btnSettings, logout);

        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(28));

        root.setLeft(sidebar);
        root.setTop(top);
        root.setCenter(new ScrollPane(contentArea) {{
            setFitToWidth(true);
            setStyle("-fx-background-color:transparent;-fx-background:" + BG + ";");
        }});

        btnDash.setSelected(true);
        showDashboard();

        stage.setScene(new Scene(root, 1200, 780));
        stage.setTitle("Espace Directeur — EduManager");
        stage.centerOnScreen();
        stage.show();
    }

    private void showDashboard() {
        contentArea.getChildren().clear();

        ComboBox<AnneeScolaire> cbAnnee = new ComboBox<>();
        cbAnnee.setItems(FXCollections.observableArrayList(new AnneeScolaireDAO().findAll()));
        if (currentAnnee != null) cbAnnee.setValue(currentAnnee);
        cbAnnee.setPrefHeight(36);
        cbAnnee.valueProperty().addListener((o, old, v) -> { if(v!=null){currentAnnee=v; showDashboard();} });

        HBox header = new HBox(16, pageTitle("Tableau de Bord"), new Region() {{ HBox.setHgrow(this,Priority.ALWAYS); }},
                new Label("Année :"), cbAnnee);
        header.setAlignment(Pos.CENTER_LEFT);

        if (currentAnnee == null) {
            contentArea.getChildren().addAll(header, new Label("Aucune année scolaire. Créez-en une d'abord."));
            return;
        }

        int anneeId = currentAnnee.getId();
        int nbEleves     = dao.countEleves(anneeId);
        int nbEnseig     = dao.countEnseignants();
        int nbClasses    = dao.countClasses(anneeId);
        double taux      = dao.getTauxPaiement(anneeId);
        double encaisse  = dao.getTotalEncaisse(anneeId);
        double attendu   = dao.getTotalAttendu(anneeId);

        HBox kpis = new HBox(16,
            kpiCard("👩‍🎓 Élèves inscrits", String.valueOf(nbEleves), BLUE),
            kpiCard("👨‍🏫 Enseignants",      String.valueOf(nbEnseig), "#7C3AED"),
            kpiCard("🏫 Classes actives",   String.valueOf(nbClasses), AMBER),
            kpiCard("💰 Taux de paiement",  String.format("%.1f%%", taux), taux>=75?GREEN:RED)
        );

        PieChart pieChart = new PieChart(FXCollections.observableArrayList(
            new PieChart.Data("Encaissé (" + String.format("%.0f",encaisse) + " DT)",  encaisse),
            new PieChart.Data("Restant (" + String.format("%.0f",attendu-encaisse) + " DT)", Math.max(0, attendu-encaisse))
        ));
        pieChart.setTitle("Répartition des Paiements");
        pieChart.setPrefHeight(280);
        pieChart.setLegendVisible(true);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Niveau");
        yAxis.setLabel("Élèves");
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Élèves par Niveau");
        barChart.setPrefHeight(280);
        barChart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Object[] row : dao.getStatsByNiveau(anneeId)) {
            series.getData().add(new XYChart.Data<>((String)row[0], (Integer)row[1]));
        }
        barChart.getData().add(series);

        HBox charts = new HBox(20, wrapChart(pieChart), wrapChart(barChart));

        TableView<Object[]> table = new TableView<>();
        table.setPrefHeight(200);
        table.setStyle("-fx-background-color:white;-fx-border-color:" + BORDER + ";");
        table.getColumns().addAll(
            objCol("Niveau", 0, 180),
            objCol("Élèves", 1, 100),
            objCol("Classes", 2, 100),
            objCol("Tarif (DT)", 3, 120)
        );
        table.setItems(FXCollections.observableArrayList(dao.getStatsByNiveau(anneeId)));

        contentArea.getChildren().addAll(header, kpis, charts, sectionTitle("Détail par Niveau"), table);
    }

    private void showAnnees() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(pageTitle("Gestion des Années Scolaires"));

        TextField tfLibelle = styledField("Ex: 2025-2026");
        Button btnCreer = primaryBtn("+ Créer l'année");
        Label feedback = new Label();
        feedback.setStyle("-fx-font-size:12px;");
        HBox form = new HBox(12, tfLibelle, btnCreer, feedback);
        form.setAlignment(Pos.CENTER_LEFT);

        TableView<AnneeScolaire> table = new TableView<>();
        table.setPrefHeight(350);
        table.setStyle("-fx-background-color:white;-fx-border-color:" + BORDER + ";");
        TableColumn<AnneeScolaire, Integer> colId  = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(80);
        TableColumn<AnneeScolaire, String> colLib = new TableColumn<>("Libellé");
        colLib.setCellValueFactory(new PropertyValueFactory<>("libelle"));
        colLib.setPrefWidth(300);
        table.getColumns().addAll(colId, colLib);
        table.setItems(FXCollections.observableArrayList(dao.getAllAnnees()));

        btnCreer.setOnAction(e -> {
            String lib = tfLibelle.getText().trim();
            if (lib.isEmpty()) { feedback.setText("⚠ Libellé requis."); feedback.setStyle("-fx-text-fill:"+RED+";"); return; }
            if (dao.createAnnee(lib)) {
                feedback.setText("✓ Année " + lib + " créée avec 3 trimestres.");
                feedback.setStyle("-fx-text-fill:"+GREEN+";");
                tfLibelle.clear();
                table.setItems(FXCollections.observableArrayList(dao.getAllAnnees()));
                currentAnnee = table.getItems().get(table.getItems().size()-1);
            } else {
                feedback.setText("✗ Erreur : vérifiez que ce libellé n'existe pas déjà.");
                feedback.setStyle("-fx-text-fill:"+RED+";");
            }
        });

        contentArea.getChildren().addAll(
            new Label("Créer une nouvelle année (3 trimestres créés automatiquement) :") {{ setStyle("-fx-text-fill:"+MUTED+";-fx-font-size:12px;"); }},
            form, new Separator(), sectionTitle("Années existantes"), table
        );
    }

    private void showUsers() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(pageTitle("Gestion des Utilisateurs"));

        Button btnNew = primaryBtn("+ Nouveau compte");

        TableView<User> table = new TableView<>();
        table.setPrefHeight(400);
        table.setStyle("-fx-background-color:white;-fx-border-color:" + BORDER + ";");

        TableColumn<User, Integer> colId   = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);
        TableColumn<User, String> colLogin = new TableColumn<>("Login");
        colLogin.setCellValueFactory(new PropertyValueFactory<>("login"));
        colLogin.setPrefWidth(200);
        TableColumn<User, String> colRole  = new TableColumn<>("Rôle");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setPrefWidth(150);

        table.getColumns().addAll(colId, colLogin, colRole);
        table.setItems(FXCollections.observableArrayList(dao.getStaff()));

        btnNew.setOnAction(e -> showCreateUserDialog(table));
        HBox bar = new HBox(btnNew); bar.setPadding(new Insets(0,0,8,0));
        contentArea.getChildren().addAll(bar, table);
    }

    private void showCreateUserDialog(TableView<User> table) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Nouveau compte");
        VBox root = new VBox(14); root.setPadding(new Insets(24));
        TextField tfLogin = styledField("Login");
        PasswordField tfPass = new PasswordField(); tfPass.setPromptText("Mot de passe"); tfPass.setPrefHeight(38);
        tfPass.setStyle("-fx-background-color:#F8FAFC;-fx-border-color:#CBD5E1;-fx-border-radius:6;-fx-padding:6 12;");
        ComboBox<String> cbRole = new ComboBox<>(FXCollections.observableArrayList("ADMIN", "FINANCIER"));
        cbRole.setValue("ADMIN"); cbRole.setPrefHeight(38); cbRole.setMaxWidth(Double.MAX_VALUE);
        Button btnSave = primaryBtn("Créer le compte");
        Label fb = new Label(); fb.setStyle("-fx-font-size:11px;");
        btnSave.setOnAction(e -> {
            if (tfLogin.getText().isEmpty() || tfPass.getText().isEmpty()) {
                fb.setText("⚠ Tous les champs sont requis."); fb.setStyle("-fx-text-fill:"+RED+";"); return;
            }
            if (dao.createUser(tfLogin.getText().trim(), tfPass.getText(), cbRole.getValue())) {
                table.setItems(FXCollections.observableArrayList(dao.getStaff()));
                dlg.close();
            } else { fb.setText("✗ Login déjà existant ?"); fb.setStyle("-fx-text-fill:"+RED+";"); }
        });

        tfLogin.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) tfPass.requestFocus(); });
        tfPass.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) btnSave.fire(); });
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, k -> { if(k.getCode() == javafx.scene.input.KeyCode.ESCAPE) dlg.close(); });

        root.getChildren().addAll(new Label("Login"), tfLogin, new Label("Mot de passe"), tfPass,
                new Label("Rôle"), cbRole, btnSave, fb);
        dlg.setScene(new Scene(root, 360, 340)); dlg.show();
    }

    private void showParametres() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(pageTitle("Paramètres du Système"));

        AppSettings s = AppSettings.get();
        s.reload();
        VBox box = new VBox(20);
        box.setPadding(new Insets(20)); box.setMaxWidth(550);
        box.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:"+BORDER+";-fx-border-radius:8;");

        box.getChildren().addAll(
            toggleRow("📝 Inscription ouverte",
                "Autoriser l'inscription des élèves dans AdminView",
                s.isInscriptionActive(), "inscription_active"),
            new Separator(),
            toggleRow("🏫 Affectation ouverte",
                "Autoriser l'affectation des élèves aux classes",
                s.isAffectationActive(), "affectation_active")
        );

        contentArea.getChildren().add(box);
    }

    private HBox toggleRow(String title, String desc, boolean current, String key) {
        VBox labels = new VBox(3,
            new Label(title) {{ setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:"+TEXT+";"); }},
            new Label(desc)  {{ setStyle("-fx-font-size:11px;-fx-text-fill:"+MUTED+";"); }}
        );
        HBox.setHgrow(labels, Priority.ALWAYS);

        CheckBox chk = new CheckBox();
        chk.setSelected(current);
        chk.setScaleX(1.4); chk.setScaleY(1.4);
        chk.selectedProperty().addListener((o, old, v) -> AppSettings.get().toggle(key, v));

        HBox row = new HBox(labels, chk);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        return row;
    }

    private Label pageTitle(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:"+TEXT+";");
        return l;
    }

    private Label sectionTitle(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:"+MUTED+";-fx-padding:8 0 4 0;");
        return l;
    }

    private VBox kpiCard(String label, String value, String color) {
        VBox c = new VBox(6);
        c.setPadding(new Insets(18)); c.setPrefWidth(230);
        c.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:"+BORDER+";-fx-border-radius:8;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label lbl = new Label(label); lbl.setStyle("-fx-font-size:11px;-fx-text-fill:"+MUTED+";");
        Label val = new Label(value); val.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:"+color+";");
        c.getChildren().addAll(lbl, val);
        return c;
    }

    private VBox wrapChart(javafx.scene.Node chart) {
        VBox box = new VBox(chart);
        box.setStyle("-fx-background-color:white;-fx-background-radius:8;-fx-border-color:"+BORDER+";-fx-border-radius:8;-fx-padding:12;");
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private ToggleButton sideBtn(String text, ToggleGroup tg) {
        ToggleButton b = new ToggleButton(text);
        b.setToggleGroup(tg);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(13, 20, 13, 20));
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:#94A3B8;-fx-font-size:13px;-fx-cursor:hand;");
        b.selectedProperty().addListener((o, old, v) ->
            b.setStyle(v ? "-fx-background-color:"+BLUE+";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;"
                         : "-fx-background-color:transparent;-fx-text-fill:#94A3B8;-fx-font-size:13px;"));
        return b;
    }

    private Button primaryBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:"+BLUE+";-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:9 18;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:#1D4ED8;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:9 18;"));
        b.setOnMouseExited(e ->  b.setStyle("-fx-background-color:"+BLUE+";-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:9 18;"));
        return b;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(38);
        tf.setStyle("-fx-background-color:#F8FAFC;-fx-border-color:#CBD5E1;-fx-border-radius:6;-fx-padding:6 12;");
        return tf;
    }

    @SuppressWarnings("unchecked")
    private TableColumn<Object[], String> objCol(String header, int idx, double w) {
        TableColumn<Object[], String> col = new TableColumn<>(header);
        col.setPrefWidth(w);
        col.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue()[idx] != null ? d.getValue()[idx].toString() : ""));
        return col;
    }

    private void showAccountSettingsDialog() {
        Stage dlg = new Stage();
        dlg.setTitle("Paramètres du compte");
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.initOwner(stage);

        VBox form = new VBox(15);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white;");

        TextField fLogin = new TextField(currentUser.getLogin());
        fLogin.setPromptText("Nouveau Login");
        fLogin.setPrefHeight(36);

        PasswordField fPass = new PasswordField();
        fPass.setPromptText("Nouveau Mot de passe");
        fPass.setPrefHeight(36);

        Button btnSave = primaryBtn("Enregistrer");
        btnSave.setMaxWidth(Double.MAX_VALUE);

        btnSave.setOnAction(e -> {
            if (fLogin.getText().isBlank() || fPass.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Veuillez remplir tous les champs.").show();
                return;
            }
            if (new UserDAO().update(currentUser.getId(), fLogin.getText().trim(), fPass.getText())) {
                currentUser.setLogin(fLogin.getText().trim());
                new Alert(Alert.AlertType.INFORMATION, "✓ Paramètres mis à jour avec succès.").show();
                dlg.close();
            } else {
                new Alert(Alert.AlertType.ERROR, "❌ Erreur lors de la mise à jour.").show();
            }
        });

        fLogin.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) fPass.requestFocus(); });
        fPass.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) btnSave.fire(); });
        form.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, k -> { if(k.getCode() == javafx.scene.input.KeyCode.ESCAPE) dlg.close(); });

        form.getChildren().addAll(
            new Label("Modifier vos accès") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 16px;"); }},
            new VBox(5, new Label("Login"), fLogin),
            new VBox(5, new Label("Mot de passe"), fPass),
            btnSave
        );

        dlg.setScene(new Scene(form));
        dlg.show();
    }
}
