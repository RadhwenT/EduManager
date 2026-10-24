package io.github.radhwent.edumanager.view;

import io.github.radhwent.edumanager.dao.AnneeScolaireDAO;
import io.github.radhwent.edumanager.dao.AppSettings;
import io.github.radhwent.edumanager.dao.ClasseDAO;
import io.github.radhwent.edumanager.dao.EleveDAO;
import io.github.radhwent.edumanager.dao.EnseignantDAO;
import io.github.radhwent.edumanager.dao.MatiereDAO;
import io.github.radhwent.edumanager.dao.NiveauDAO;
import io.github.radhwent.edumanager.dao.NoteDAO;
import io.github.radhwent.edumanager.dao.TrimestreDAO;
import io.github.radhwent.edumanager.dao.UserDAO;
import io.github.radhwent.edumanager.db.DBConnection;
import io.github.radhwent.edumanager.model.AnneeScolaire;
import io.github.radhwent.edumanager.model.Classe;
import io.github.radhwent.edumanager.model.Eleve;
import io.github.radhwent.edumanager.model.Enseignant;
import io.github.radhwent.edumanager.model.Matiere;
import io.github.radhwent.edumanager.model.Niveau;
import io.github.radhwent.edumanager.model.User;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class AdminView {

    
    private static final String BLUE      = "#2563EB";
    private static final String BLUE_DARK = "#1D4ED8";
    private static final String BG        = "#F8FAFC";
    private static final String WHITE     = "#FFFFFF";
    private static final String BORDER    = "#CBD5E1";
    private static final String TEXT_DARK = "#1E293B";
    private static final String TEXT_MID  = "#475569";
    private static final String TEXT_LIGHT= "#64748B";
    private static final String RED       = "#DC2626";
    private static final String GREEN     = "#16A34A";
    private static final String SIDEBAR   = "#1E293B";
    private static final String SIDEBAR_HOVER = "#334155";
    private static final String SIDEBAR_SEL   = "#2563EB";
    private Stage stage;
    private User  adminUser;

    
    private AnneeScolaire currentAnnee = null;
    private VBox           contentArea;

    
    private Button btnEleves, btnEnseignants, btnClasses, btnMatieres, btnNiveaux, btnBulletins;
    private Button activeBtn = null;

    public AdminView(Stage stage, User adminUser) {
        this.stage     = stage;
        this.adminUser = adminUser;
    }

    
    public void show() {

        VBox accentStrip = new VBox();
        accentStrip.setPrefWidth(8);
        accentStrip.setStyle("-fx-background-color: " + BLUE + ";");

        Label appName = new Label("EduManager");
        appName.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + BLUE + "; -fx-letter-spacing: 2px;");
        Label title = new Label("Espace Administrateur");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";");
        Label sub = new Label("Connecté : " + adminUser.getLogin());
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_LIGHT + ";");
        VBox titleBox = new VBox(2, appName, title, sub);

        Label lblAnnee = new Label("Année scolaire :");
        lblAnnee.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MID + ";");
        ComboBox<AnneeScolaire> cbAnnee = styledCombo("Sélectionner…");
        cbAnnee.setPrefWidth(160);
        ObservableList<AnneeScolaire> annees = FXCollections.observableArrayList(new AnneeScolaireDAO().findAll());
        cbAnnee.setItems(annees);
        if (!annees.isEmpty()) {
            cbAnnee.setValue(annees.get(0));
            currentAnnee = cbAnnee.getValue();
        }

        cbAnnee.valueProperty().addListener((o, old, val) -> {
            currentAnnee = val;
            if (activeBtn != null) activeBtn.fire();
        });

        Button btnLogout = new Button("Déconnexion");
        styleSecondaryButton(btnLogout);
        btnLogout.setOnAction(e -> new LoginView(stage).show());

        Button btnSettings = new Button("⚙️");
        styleSecondaryButton(btnSettings);
        btnSettings.setTooltip(new Tooltip("Paramètres du compte"));
        btnSettings.setOnAction(e -> showAccountSettingsDialog(adminUser));

        HBox anneeBox = new HBox(8, lblAnnee, cbAnnee);
        anneeBox.setAlignment(Pos.CENTER);

        HBox header = new HBox(titleBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        header.getChildren().addAll(anneeBox, btnSettings, btnLogout);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(20);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        VBox sidebar = buildSidebar();

        contentArea = new VBox();
        contentArea.setStyle("-fx-background-color: " + BG + ";");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + "; -fx-border-color: transparent;");
        HBox.setHgrow(scroll, Priority.ALWAYS);

        HBox body = new HBox(sidebar, scroll);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox rightColumn = new VBox(header, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        HBox root = new HBox(accentStrip, rightColumn);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        root.setStyle("-fx-background-color: " + WHITE + ";");

        Scene scene = new Scene(root, 1200, 700);
        stage.setScene(scene);
        stage.setTitle("Espace Administrateur — EduManager");
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();

        
        Platform.runLater(() -> btnEleves.fire());
    }

    private VBox buildSidebar() {
        Label menuTitle = new Label("MENU");
        menuTitle.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; -fx-letter-spacing: 1.5px; -fx-padding: 20 0 8 20;");

        btnEleves      = sidebarBtn("👤  Élèves");
        btnEnseignants = sidebarBtn("📋  Enseignants");
        btnClasses     = sidebarBtn("🏫  Classes");
        btnMatieres    = sidebarBtn("📚  Matières");
        btnNiveaux     = sidebarBtn("🎓  Niveaux");
        btnBulletins   = sidebarBtn("📜  Bulletins");

        btnEleves     .setOnAction(e -> Platform.runLater(() -> { activateBtn(btnEleves);      showElevesSection();      }));
        btnEnseignants.setOnAction(e -> Platform.runLater(() -> { activateBtn(btnEnseignants); showEnseignantsSection(); }));
        btnClasses    .setOnAction(e -> Platform.runLater(() -> { activateBtn(btnClasses);     showClassesSection();     }));
        btnMatieres   .setOnAction(e -> Platform.runLater(() -> { activateBtn(btnMatieres);    showMatieresSection();    }));
        btnNiveaux    .setOnAction(e -> Platform.runLater(() -> { activateBtn(btnNiveaux);     showNiveauxSection();     }));
        btnBulletins  .setOnAction(e -> Platform.runLater(() -> { activateBtn(btnBulletins);    showBulletinsSection();   }));

        VBox sidebar = new VBox(0, menuTitle, btnEleves, btnEnseignants, btnClasses, btnMatieres, btnNiveaux, btnBulletins);
        sidebar.setPrefWidth(200);
        sidebar.setMinWidth(200);
        sidebar.setStyle("-fx-background-color: " + SIDEBAR + ";");
        return sidebar;
    }

    private Button sidebarBtn(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(12, 20, 12, 20));
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-font-size: 13px; -fx-border-color: transparent; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> { if (b != activeBtn) b.setStyle("-fx-background-color: " + SIDEBAR_HOVER + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;"); });
        b.setOnMouseExited (e -> { if (b != activeBtn) b.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-font-size: 13px; -fx-cursor: hand;"); });
        return b;
    }

    private void activateBtn(Button b) {
        if (activeBtn != null)
            activeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CBD5E1; -fx-font-size: 13px; -fx-cursor: hand;");
        activeBtn = b;
        b.setStyle("-fx-background-color: " + SIDEBAR_SEL + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");
    }

    private void showElevesSection() {
        contentArea.getChildren().clear();

        
        ToggleButton tabFiches       = subTab("Fiches élèves");
        ToggleButton tabInscription  = subTab("Inscription");
        ToggleButton tabAffectation  = subTab("Affectation");

        ToggleGroup tg = new ToggleGroup();
        tabFiches.setToggleGroup(tg); tabInscription.setToggleGroup(tg); tabAffectation.setToggleGroup(tg);
        tabFiches.setSelected(true);

        HBox tabBar = new HBox(0, tabFiches, tabInscription, tabAffectation);
        tabBar.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        tabBar.setPadding(new Insets(0, 24, 0, 24));

        StackPane subContent = new StackPane();
        subContent.setPadding(new Insets(20, 24, 20, 24));

        tg.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == tabFiches)      subContent.getChildren().setAll(buildElevesFiches());
            else if (sel == tabInscription)  subContent.getChildren().setAll(buildElevesInscription());
            else if (sel == tabAffectation)  subContent.getChildren().setAll(buildElevesAffectation());
        });

        subContent.getChildren().setAll(buildElevesFiches());

        contentArea.getChildren().addAll(sectionTitle("Gestion des élèves"), tabBar, subContent);
        VBox.setVgrow(subContent, Priority.ALWAYS);
    }

    private VBox buildElevesFiches() {
        TableView<Eleve> table = new TableView<>();
        styleTable(table);

        TableColumn<Eleve, String> colMat    = col("Matricule", "matricule", 100);
        TableColumn<Eleve, String> colNom    = col("Nom", "nom", 140);
        TableColumn<Eleve, String> colPrenom = col("Prénom", "prenom", 120);
        TableColumn<Eleve, String> colDate   = col("Date naiss.", "dateNaissance", 110);
        TableColumn<Eleve, String> colTel    = col("Téléphone parent", "telephone", 140);
        TableColumn<Eleve, String> colAddr   = col("Adresse", "adresse", 200);

        ObservableList<Eleve> allEleves = FXCollections.observableArrayList(new EleveDAO().findAll());
        
        TableColumn<Eleve, Void> colActions = actionCol(table, new EleveDAO(),
            e -> showEleveDialog(e, table), null);

        table.getColumns().setAll(List.of(colMat, colNom, colPrenom, colDate, colTel, colAddr, colActions));
        javafx.collections.transformation.FilteredList<Eleve> filteredData = new javafx.collections.transformation.FilteredList<>(allEleves, p -> true);
        table.setItems(filteredData);
        table.refresh();

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher par nom ou matricule...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 12 6 12; -fx-font-size: 13px;");
        
        ComboBox<String> filterStatus = styledCombo("Filtre");
        filterStatus.setItems(FXCollections.observableArrayList("Inscrits", "Non-inscrits", "Tous"));
        filterStatus.setValue("Inscrits");
        filterStatus.setPrefWidth(150);

        List<Integer> enrolledIds = new EleveDAO().findInscritsIds(currentAnnee.getId());

        java.lang.Runnable updateFilter = () -> {
            filteredData.setPredicate(eleve -> {
                String search = searchField.getText().toLowerCase();
                boolean matchesSearch = search.isEmpty() || 
                                       eleve.getNom().toLowerCase().contains(search) || 
                                       eleve.getPrenom().toLowerCase().contains(search) ||
                                       (eleve.getMatricule() != null && eleve.getMatricule().toLowerCase().contains(search));
                if (!matchesSearch) return false;

                String status = filterStatus.getValue();
                boolean isEnrolled = enrolledIds.contains(eleve.getId());
                if ("Inscrits".equals(status)) return isEnrolled;
                if ("Non-inscrits".equals(status)) return !isEnrolled;
                return true;
            });
        };

        searchField.textProperty().addListener((o, old, v) -> updateFilter.run());
        filterStatus.valueProperty().addListener((o, old, v) -> updateFilter.run());
        
        updateFilter.run();

        Button btnAjouter = new Button("+ Ajouter un élève");
        stylePrimaryButton(btnAjouter);
        btnAjouter.setOnAction(e -> showEleveDialog(null, table));

        HBox toolbar = new HBox(15, searchField, filterStatus, new Region(), btnAjouter);
        HBox.setHgrow(toolbar.getChildren().get(2), Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));

        VBox box = new VBox(8, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private void showEleveDialog(Eleve existing, TableView<Eleve> table) {
        Stage dlg = dialogStage(existing == null ? "Ajouter un élève" : "Modifier l'élève");
        VBox form = dialogForm();

        TextField fMat   = styledField("Matricule");
        TextField fNom   = styledField("Nom");
        TextField fPrenom= styledField("Prénom");
        DatePicker dpDate = new DatePicker();
        dpDate.setPrefHeight(36);
        dpDate.setPrefWidth(Double.MAX_VALUE);
        dpDate.setMaxWidth(Double.MAX_VALUE);
        dpDate.getEditor().setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_DARK + ";");
        dpDate.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        
        TextField fAddr  = styledField("Adresse");
        TextField fTel   = styledField("Téléphone");

        if (existing != null) {
            fMat.setText(existing.getMatricule());
            fNom.setText(existing.getNom()); fPrenom.setText(existing.getPrenom());
            if (existing.getDateNaissance() != null && !existing.getDateNaissance().isEmpty()) {
                try { dpDate.setValue(LocalDate.parse(existing.getDateNaissance())); } catch (Exception ex) {}
            }
            fAddr.setText(existing.getAdresse());
            fTel.setText(existing.getTelephone());
        } else {
            fMat.setText(new EleveDAO().genererMatriculeEleve());
        }

        Button btnSave = new Button(existing == null ? "Ajouter" : "Enregistrer");
        stylePrimaryButton(btnSave);
        Label errLbl = errorLabel();

        btnSave.setOnAction(e -> {
            if (fNom.getText().isBlank() || fPrenom.getText().isBlank()) { errLbl.setText("Nom et prénom obligatoires."); return; }
            Eleve el = existing != null ? existing : new Eleve();
            el.setMatricule(fMat.getText().trim());
            el.setNom(fNom.getText().trim()); el.setPrenom(fPrenom.getText().trim());
            if (dpDate.getValue() != null) el.setDateNaissance(dpDate.getValue().toString());
            el.setAdresse(fAddr.getText().trim());
            el.setTelephone(fTel.getText().trim());
            EleveDAO dao = new EleveDAO();
            if (existing == null) { 
                dao.create(el);
                
                if (table.getItems() instanceof javafx.collections.transformation.FilteredList) {
                    ((javafx.collections.ObservableList<Eleve>) ((javafx.collections.transformation.FilteredList<Eleve>) table.getItems()).getSource()).add(el);
                } else {
                    table.getItems().add(el);
                }
            }
            else { dao.update(el); table.refresh(); }
            dlg.close();
        });

        form.getChildren().addAll(formRow("Matricule", fMat), formRow("Nom", fNom), formRow("Prénom", fPrenom),
            formRow("Date naissance", dpDate), formRow("Adresse", fAddr),
            formRow("Téléphone", fTel), errLbl, btnSave);
        
        setupNav(fMat, fNom, fPrenom, dpDate, fAddr, fTel, btnSave);
        
        dlg.setScene(new Scene(form, 420, -1));
        dlg.centerOnScreen();
        dlg.showAndWait();
    }

    private javafx.scene.Node buildElevesInscription() {
        if (currentAnnee == null) return noAnneePane();
        if (!AppSettings.get().isInscriptionActive()) return lockedPane("L'inscription des élèves est actuellement désactivée par le Directeur.");

        TabPane tp = new TabPane();
        tp.getStyleClass().add("floating");
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Object[] listData = buildListeInscriptions();
        VBox listView = (VBox) listData[0];
        Runnable refreshList = (Runnable) listData[1];

        Tab tabNew = new Tab("Nouvelle Inscription");
        tabNew.setContent(buildNouvelleInscription(refreshList));

        Tab tabList = new Tab("Liste des Inscriptions");
        tabList.setContent(listView);
        tabList.setOnSelectionChanged(e -> { if (tabList.isSelected()) refreshList.run(); });

        tp.getTabs().addAll(tabNew, tabList);
        return tp;
    }

    private VBox buildNouvelleInscription(Runnable onInscrit) {
        ComboBox<Niveau> cbNiveau = styledCombo("Niveau");
        cbNiveau.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));

        TableView<Eleve> tableNonInscrits = new TableView<>();
        styleTable(tableNonInscrits);
        TableColumn<Eleve, String> cM = col("Matricule", "matricule", 100);
        TableColumn<Eleve, String> c1 = col("Nom", "nom", 140);
        TableColumn<Eleve, String> c2 = col("Prénom", "prenom", 120);
        tableNonInscrits.getColumns().addAll(cM, c1, c2);
        tableNonInscrits.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);


        Label lblMatricule = new Label();
        lblMatricule.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_LIGHT + ";");

        cbNiveau.valueProperty().addListener((o, old, niv) -> {
            if (niv == null) return;
            tableNonInscrits.setItems(FXCollections.observableArrayList(
                new EleveDAO().findNonInscrits(currentAnnee.getId())));
            String mat = new EleveDAO().genererMatricule(currentAnnee.getLibelle(), niv.getLibelle());
            lblMatricule.setText("Prochain matricule : " + mat);
        });

        Button btnInscrire = new Button("Inscrire la sélection");
        stylePrimaryButton(btnInscrire);
        Label feedback = new Label();
        feedback.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 12px;");

        TextField searchField = styledField("🔍 Rechercher par nom ou matricule...");
        searchField.setPrefWidth(200);

        ObservableList<Eleve> masterData = FXCollections.observableArrayList(new EleveDAO().findNonInscrits(currentAnnee.getId()));
        javafx.collections.transformation.FilteredList<Eleve> filteredData = new javafx.collections.transformation.FilteredList<>(masterData, p -> true);
        tableNonInscrits.setItems(filteredData);

        searchField.textProperty().addListener((o, old, v) -> {
            filteredData.setPredicate(el -> {
                if (v == null || v.isEmpty()) return true;
                String low = v.toLowerCase();
                return el.getNom().toLowerCase().contains(low) || 
                       el.getPrenom().toLowerCase().contains(low) ||
                       (el.getMatricule() != null && el.getMatricule().toLowerCase().contains(low));
            });
        });

        Label infoTarif = new Label();
        cbNiveau.valueProperty().addListener((o, old, niv) -> {
            if (niv != null) infoTarif.setText("Tarif du niveau : " + String.format("%.2f", niv.getPrix()) + " DT");
        });
        infoTarif.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_MID + ";");

        HBox filters = new HBox(25,
            formRow("Rechercher par nom ou matricule", searchField),
            formRow("Niveau", cbNiveau),
            new VBox(6, new Label(" "), infoTarif)
        );
        filters.setPadding(new Insets(20, 0, 20, 0));
        filters.setAlignment(Pos.BOTTOM_LEFT);

        btnInscrire.setOnAction(e -> {
            ObservableList<Eleve> sel = tableNonInscrits.getSelectionModel().getSelectedItems();
            if (sel.isEmpty()) { feedback.setText("Sélectionnez au moins un élève."); feedback.setStyle("-fx-text-fill: " + RED + ";"); return; }
            if (cbNiveau.getValue() == null) { feedback.setText("Sélectionnez un niveau."); feedback.setStyle("-fx-text-fill: " + RED + ";"); return; }
            
            EleveDAO dao = new EleveDAO();
            int ok = 0;
            for (Eleve el : new ArrayList<>(sel)) { 
                if (dao.inscrire(el.getId(), currentAnnee.getId(), cbNiveau.getValue().getId(), null)) {
                    masterData.remove(el);
                    ok++;
                }
            }
            feedback.setStyle("-fx-text-fill: " + GREEN + ";");
            feedback.setText(ok + " élève(s) inscrit(s) avec succès.");
            if (ok > 0 && onInscrit != null) onInscrit.run();
        });

        Label tip = new Label("Sélection multiple : Ctrl+clic ou Maj+clic");
        tip.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_LIGHT + "; -fx-font-style: italic;");

        VBox box = new VBox(10, filters, tip, tableNonInscrits, btnInscrire, feedback);
        box.setPadding(new Insets(0, 24, 24, 24));
        VBox.setVgrow(tableNonInscrits, Priority.ALWAYS);
        box.setPrefHeight(600);
        return box;
    }

    private Object[] buildListeInscriptions() {
        EleveDAO dao = new EleveDAO();
        ObservableList<EleveDAO.InscriptionInfo> masterData = FXCollections.observableArrayList(dao.findInscriptionsByAnnee(currentAnnee.getId()));
        
        TableView<EleveDAO.InscriptionInfo> table = new TableView<>();
        styleTable(table);

        TableColumn<EleveDAO.InscriptionInfo, String> colMat = new TableColumn<>("Matricule");
        colMat.setCellValueFactory(new PropertyValueFactory<>("matricule"));
        colMat.setPrefWidth(100);

        TableColumn<EleveDAO.InscriptionInfo, String> colNom = new TableColumn<>("Nom");
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colNom.setPrefWidth(130);

        TableColumn<EleveDAO.InscriptionInfo, String> colPrenom = new TableColumn<>("Prénom");
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colPrenom.setPrefWidth(120);

        TableColumn<EleveDAO.InscriptionInfo, String> colNiv = new TableColumn<>("Niveau");
        colNiv.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colNiv.setPrefWidth(130);

        TableColumn<EleveDAO.InscriptionInfo, String> colClasse = new TableColumn<>("Classe");
        colClasse.setCellValueFactory(new PropertyValueFactory<>("classe"));
        colClasse.setPrefWidth(110);

        TableColumn<EleveDAO.InscriptionInfo, Void> colActions = new TableColumn<>("Action");
        colActions.setPrefWidth(140);
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button btnAnnuler = smallBtn("Annuler inscription", RED);
            {
                btnAnnuler.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                        EleveDAO.InscriptionInfo info = getTableView().getItems().get(getIndex());
                        if (confirmDelete("l'inscription de " + info.getNom() + " " + info.getPrenom() + " (cela annulera aussi son affectation)")) {
                            if (dao.annulerInscription(info.getEleveId(), currentAnnee.getId())) {
                                masterData.remove(info);
                            }
                        }
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(btnAnnuler);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        table.getColumns().addAll(colMat, colNom, colPrenom, colNiv, colClasse, colActions);

        ComboBox<Niveau> filterNiveau = styledCombo("Tous les niveaux");
        filterNiveau.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        
        ComboBox<Classe> filterClasse = styledCombo("Toutes les classes");
        Classe CLASSE_NON_AFF = new Classe(-1, "Non affectés");
        filterNiveau.valueProperty().addListener((o, old, niv) -> {
            filterClasse.getItems().clear();
            if (niv != null) {
                filterClasse.getItems().add(CLASSE_NON_AFF);
                filterClasse.getItems().addAll(new ClasseDAO().findByNiveauAndAnnee(niv.getId(), currentAnnee.getId()));
            }
        });

        TextField filterNom = new TextField();
        filterNom.setPromptText("🔍 Rechercher par nom ou matricule...");
        filterNom.setPrefWidth(200);
        filterNom.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 12 6 12;");

        javafx.collections.transformation.FilteredList<EleveDAO.InscriptionInfo> filteredData = new javafx.collections.transformation.FilteredList<>(masterData, p -> true);
        table.setItems(filteredData);

        java.lang.Runnable updateFilter = () -> {
            filteredData.setPredicate(info -> {
                String search = filterNom.getText().toLowerCase();
                if (!search.isEmpty() && 
                    !info.getNom().toLowerCase().contains(search) && 
                    !info.getPrenom().toLowerCase().contains(search) &&
                    !(info.getMatricule() != null && info.getMatricule().toLowerCase().contains(search))) return false;
                
                Niveau n = filterNiveau.getValue();
                if (n != null && !info.getNiveau().equals(n.getLibelle())) return false;
                
                Classe c = filterClasse.getValue();
                if (c != null) {
                    if (c.getId() == -1) {
                        if (!"Non affecté".equals(info.getClasse())) return false;
                    } else if (!info.getClasse().equals(c.getNom())) return false;
                }
                
                return true;
            });
        };

        filterNom.textProperty().addListener((o, old, v) -> updateFilter.run());
        filterNiveau.valueProperty().addListener((o, old, v) -> updateFilter.run());
        filterClasse.valueProperty().addListener((o, old, v) -> updateFilter.run());

        java.lang.Runnable refreshLogic = () -> {
            masterData.setAll(dao.findInscriptionsByAnnee(currentAnnee.getId()));
            updateFilter.run();
        };

        HBox toolBar = new HBox(12, filterNom, filterNiveau, filterClasse);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(20, 0, 10, 0));

        VBox box = new VBox(10, toolBar, table);
        box.setPadding(new Insets(0, 24, 24, 24));
        VBox.setVgrow(table, Priority.ALWAYS);
        return new Object[] { box, refreshLogic };
    }


    private javafx.scene.Node buildElevesAffectation() {
        if (currentAnnee == null) return noAnneePane();
        if (!AppSettings.get().isAffectationActive()) return lockedPane("L'affectation aux classes est actuellement désactivée par le Directeur.");

        VBox panel = new VBox(16);
        panel.setPadding(new Insets(0));
        panel.setStyle("-fx-background-color: " + WHITE + ";");

        Label lblNiveau = new Label("Sélectionner le niveau :");
        lblNiveau.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";");
        
        ComboBox<Niveau> cbNiveau = styledCombo("Choisir un niveau");
        cbNiveau.setPrefWidth(250);
        cbNiveau.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        
        HBox niveauBox = new HBox(12, lblNiveau, cbNiveau);
        niveauBox.setAlignment(Pos.CENTER_LEFT);
        niveauBox.setPadding(new Insets(0, 0, 16, 0));

        HBox mainArea = new HBox(20);
        mainArea.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        VBox leftColumn = new VBox(12);
        leftColumn.setPrefWidth(450);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        Classe NON_AFFECTE = new Classe(-1, "(Non affectés)");

        ComboBox<Classe> cbClasseGauche = styledCombo("Sélectionner une classe");
        cbClasseGauche.setPrefWidth(300);

        TableView<Eleve> tableGauche = new TableView<>();
        tableGauche.setPlaceholder(new Label("Sélectionnez un niveau et une classe"));
        styleTable(tableGauche);
        VBox.setVgrow(tableGauche, Priority.ALWAYS);

        TableColumn<Eleve, String> colNomG = col("Nom", "nom", 150);
        TableColumn<Eleve, String> colPrenomG = col("Prénom", "prenom", 150);
        tableGauche.getColumns().addAll(colNomG, colPrenomG);
        tableGauche.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        leftColumn.getChildren().addAll(cbClasseGauche, tableGauche);

        VBox centerButtons = new VBox(16);
        centerButtons.setAlignment(Pos.CENTER);
        centerButtons.setPadding(new Insets(100, 0, 0, 0));

        Button btnToRight = new Button(">");
        Button btnToLeft = new Button("<");
        Button btnSwap = new Button("<>");
        
        String arrowStyle = "-fx-background-color: " + BLUE + "; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 5 0 5 0;";
        btnToRight.setStyle(arrowStyle);
        btnToLeft.setStyle(arrowStyle);
        btnSwap.setStyle(arrowStyle);

        btnToRight.setPrefWidth(50);
        btnToLeft.setPrefWidth(50);
        btnSwap.setPrefWidth(50);
        
        btnToRight.setTooltip(new Tooltip("Affecter l'élève à la classe"));
        btnToLeft.setTooltip(new Tooltip("Retirer l'élève de la classe"));
        btnSwap.setTooltip(new Tooltip("Permuter les deux élèves sélectionnés"));

        centerButtons.getChildren().addAll(btnToRight, btnToLeft, btnSwap);

        VBox rightColumn = new VBox(12);
        rightColumn.setPrefWidth(450);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);

        ComboBox<Classe> cbClasseDroite = styledCombo("Sélectionner une classe");
        cbClasseDroite.setPrefWidth(300);

        TableView<Eleve> tableDroite = new TableView<>();
        tableDroite.setPlaceholder(new Label("Sélectionnez une classe"));
        styleTable(tableDroite);
        VBox.setVgrow(tableDroite, Priority.ALWAYS);

        TableColumn<Eleve, String> colNomD = col("Nom", "nom", 150);
        TableColumn<Eleve, String> colPrenomD = col("Prénom", "prenom", 150);
        tableDroite.getColumns().addAll(colNomD, colPrenomD);
        tableDroite.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        rightColumn.getChildren().addAll(cbClasseDroite, tableDroite);

        mainArea.getChildren().addAll(leftColumn, centerButtons, rightColumn);

        
        Label feedback = new Label();
        feedback.setStyle("-fx-font-size: 12px; -fx-padding: 8 0 0 0;");

        panel.getChildren().addAll(niveauBox, mainArea, feedback);

        EleveDAO eleveDAO = new EleveDAO();
        ClasseDAO classeDAO = new ClasseDAO();

        cbNiveau.valueProperty().addListener((o, old, niveau) -> {
            if (niveau == null) {
                cbClasseGauche.getItems().clear();
                cbClasseDroite.getItems().clear();
                tableGauche.getItems().clear();
                tableDroite.getItems().clear();
                feedback.setText("");
                return;
            }

            List<Classe> classes = classeDAO.findByNiveauAndAnnee(niveau.getId(), currentAnnee.getId());
            ObservableList<Classe> obsClasses = FXCollections.observableArrayList();
            obsClasses.add(NON_AFFECTE);
            obsClasses.addAll(classes);
            
            cbClasseGauche.setItems(obsClasses);
            cbClasseDroite.setItems(obsClasses);
            
            cbClasseGauche.setValue(NON_AFFECTE);
            cbClasseDroite.setValue(null);
            
            feedback.setStyle("-fx-text-fill: " + TEXT_MID + ";");
            feedback.setText("✓ " + classes.size() + " classe(s) disponible(s) pour ce niveau");
        });

        java.util.function.BiConsumer<ComboBox<Classe>, TableView<Eleve>> refreshList = (cb, table) -> {
            Classe c = cb.getValue();
            if (c == null || cbNiveau.getValue() == null) {
                table.getItems().clear();
            } else if (c == NON_AFFECTE) {
                table.setItems(FXCollections.observableArrayList(eleveDAO.findInscritsNonAffectes(currentAnnee.getId(), cbNiveau.getValue().getId())));
            } else {
                table.setItems(FXCollections.observableArrayList(eleveDAO.findAffectesDansClasse(c.getId())));
            }
        };

        cbClasseGauche.valueProperty().addListener((o, old, val) -> refreshList.accept(cbClasseGauche, tableGauche));
        cbClasseDroite.valueProperty().addListener((o, old, val) -> refreshList.accept(cbClasseDroite, tableDroite));

        btnToRight.setOnAction(e -> {
            ObservableList<Eleve> selected = tableGauche.getSelectionModel().getSelectedItems();
            Classe src = cbClasseGauche.getValue();
            Classe dest = cbClasseDroite.getValue();
            if (selected.isEmpty()) { feedback.setText("⚠ Sélectionnez au moins un élève à gauche"); feedback.setStyle("-fx-text-fill: " + RED + ";"); return; }
            if (dest == null || dest == NON_AFFECTE) { feedback.setText("⚠ Sélectionnez une classe cible à droite"); feedback.setStyle("-fx-text-fill: " + RED + ";"); return; }

            int currentCount = eleveDAO.countAffectesClasse(dest.getId());
            if (currentCount + selected.size() > 20) {
                feedback.setText("⚠ La classe " + dest.getNom() + " ne peut pas dépasser 20 élèves (Places restantes: " + (20 - currentCount) + ")");
                feedback.setStyle("-fx-text-fill: " + RED + ";");
                return;
            }

            int okCount = 0;
            for (Eleve el : new ArrayList<>(selected)) {
                boolean ok = (src == NON_AFFECTE) ? eleveDAO.affecter(el.getId(), dest.getId()) : eleveDAO.reaffecter(el.getId(), dest.getId());
                if (ok) okCount++;
            }

            if (okCount > 0) {
                feedback.setText("✓ " + okCount + " élève(s) affecté(s) à " + dest.getNom());
                feedback.setStyle("-fx-text-fill: " + GREEN + ";");
                refreshList.accept(cbClasseGauche, tableGauche);
                refreshList.accept(cbClasseDroite, tableDroite);
            }
        });

        btnToLeft.setOnAction(e -> {
            ObservableList<Eleve> selected = tableDroite.getSelectionModel().getSelectedItems();
            Classe src = cbClasseDroite.getValue();
            Classe dest = cbClasseGauche.getValue();
            if (selected.isEmpty()) { feedback.setText("⚠ Sélectionnez au moins un élève à droite"); feedback.setStyle("-fx-text-fill: " + RED + ";"); return; }
            if (src == null || src == NON_AFFECTE) return;

            if (dest != null && dest != NON_AFFECTE) {
                int currentCount = eleveDAO.countAffectesClasse(dest.getId());
                if (currentCount + selected.size() > 20) {
                    feedback.setText("⚠ La classe " + dest.getNom() + " est pleine (Places restantes: " + (20 - currentCount) + ")");
                    feedback.setStyle("-fx-text-fill: " + RED + ";");
                    return;
                }
            }

            int okCount = 0;
            for (Eleve el : new ArrayList<>(selected)) {
                boolean ok = false;
                if (dest == null || dest == NON_AFFECTE) {
                    String sql = "DELETE FROM affectation_eleve WHERE eleve_id = ? AND classe_id = ?";
                    try (Connection cn = DBConnection.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
                        ps.setInt(1, el.getId()); ps.setInt(2, src.getId());
                        ok = ps.executeUpdate() > 0;
                    } catch (SQLException ex) { ex.printStackTrace(); }
                } else {
                    ok = eleveDAO.reaffecter(el.getId(), dest.getId());
                }
                if (ok) okCount++;
            }

            if (okCount > 0) {
                String msg = (dest == null || dest == NON_AFFECTE) ? "retiré(s) de " + src.getNom() : "déplacé(s) vers " + dest.getNom();
                feedback.setText("✓ " + okCount + " élève(s) " + msg);
                feedback.setStyle("-fx-text-fill: " + GREEN + ";");
                refreshList.accept(cbClasseGauche, tableGauche);
                refreshList.accept(cbClasseDroite, tableDroite);
            }
        });

        btnSwap.setOnAction(e -> {
            Eleve eG = tableGauche.getSelectionModel().getSelectedItem();
            Eleve eD = tableDroite.getSelectionModel().getSelectedItem();
            Classe cG = cbClasseGauche.getValue();
            Classe cD = cbClasseDroite.getValue();
            if (eG == null || eD == null || cG == null || cD == null) {
                feedback.setText("⚠ Sélectionnez un élève de chaque côté pour permuter");
                feedback.setStyle("-fx-text-fill: " + RED + ";");
                return;
            }
            if (eleveDAO.permuter(eG.getId(), eD.getId())) {
                feedback.setText("✓ Permutation effectuée");
                feedback.setStyle("-fx-text-fill: " + GREEN + ";");
                refreshList.accept(cbClasseGauche, tableGauche);
                refreshList.accept(cbClasseDroite, tableDroite);
            }
        });

        tableGauche.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.RIGHT) btnToRight.fire();
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) btnSwap.fire();
        });
        tableDroite.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.LEFT) btnToLeft.fire();
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) btnSwap.fire();
        });

        return panel;
    }


    private void showEnseignantsSection() {
        contentArea.getChildren().clear();

        ToggleButton tabFiches    = subTab("Enseignants");
        ToggleButton tabAffecter  = subTab("Affectation matières");
        ToggleGroup tg = new ToggleGroup();
        tabFiches.setToggleGroup(tg); tabAffecter.setToggleGroup(tg);
        tabFiches.setSelected(true);

        HBox tabBar = new HBox(0, tabFiches, tabAffecter);
        tabBar.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        tabBar.setPadding(new Insets(0, 24, 0, 24));

        StackPane subContent = new StackPane();
        subContent.setPadding(new Insets(20, 24, 20, 24));

        tg.selectedToggleProperty().addListener((o, old, sel) -> {
            if (sel == tabFiches)   subContent.getChildren().setAll(buildEnseignantsFiches());
            else                    subContent.getChildren().setAll(buildEnseignantsAffectation());
        });
        subContent.getChildren().setAll(buildEnseignantsFiches());

        contentArea.getChildren().addAll(sectionTitle("Gestion des enseignants"), tabBar, subContent);
        VBox.setVgrow(subContent, Priority.ALWAYS);
    }

    private VBox buildEnseignantsFiches() {
        TableView<Enseignant> table = new TableView<>();
        styleTable(table);
        table.getColumns().addAll(
            col("Code", "code", 90), col("Nom", "nom", 120), col("Prénom", "prenom", 110),
            col("Téléphone", "telephone", 120), col("Login", "login", 130)
        );

        ObservableList<Enseignant> allEns = FXCollections.observableArrayList(new EnseignantDAO().findAll());
        
        TableColumn<Enseignant, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(100);
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button btnEdit = smallBtn("Modifier", BLUE);
            { HBox hb = new HBox(btnEdit); hb.setAlignment(Pos.CENTER); setGraphic(hb);
              btnEdit.setOnAction(e -> showEnseignantDialog(getTableView().getItems().get(getIndex()), allEns, table));
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : ((HBox)getGraphic())); }
        });
        table.getColumns().add(colActions);
        
        javafx.collections.transformation.FilteredList<Enseignant> filteredData = new javafx.collections.transformation.FilteredList<>(allEns, p -> true);
        table.setItems(filteredData);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher par nom ou code...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 12 6 12; -fx-font-size: 13px;");
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(ens -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String low = newValue.toLowerCase();
                return ens.getNom().toLowerCase().contains(low) || 
                       ens.getPrenom().toLowerCase().contains(low) ||
                       (ens.getCode() != null && ens.getCode().toLowerCase().contains(low));
            });
        });

        Button btnAjouter = new Button("+ Ajouter un enseignant");
        stylePrimaryButton(btnAjouter);
        btnAjouter.setOnAction(e -> showEnseignantDialog(null, allEns, table));

        HBox toolbar = new HBox(15, searchField, new Region(), btnAjouter);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        
        VBox box = new VBox(8, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private void showEnseignantDialog(Enseignant existing, ObservableList<Enseignant> masterData, TableView<Enseignant> table) {
        Stage dlg = dialogStage(existing == null ? "Ajouter un enseignant" : "Modifier l'enseignant");
        VBox form = dialogForm();
        EnseignantDAO dao = new EnseignantDAO();

        TextField fCode = styledField("Code");
        TextField fNom  = styledField("Nom");
        TextField fPrenom = styledField("Prénom");
        TextField fTel  = styledField("Téléphone");
        TextField fLogin = styledField("Login (généré automatiquement)");
        PasswordField fPass = new PasswordField();
        fPass.setPromptText("Mot de passe");
        fPass.setPrefHeight(36);
        fPass.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 13px;");

        if (existing != null) {
            fCode.setText(existing.getCode()); fNom.setText(existing.getNom());
            fPrenom.setText(existing.getPrenom()); fTel.setText(existing.getTelephone());
            fLogin.setText(existing.getLogin()); fLogin.setDisable(true); fPass.setDisable(true);
        } else {
            fCode.setText(dao.genererCode());
            fNom.textProperty().addListener((o, a, b) -> fLogin.setText(
                fPrenom.getText().isBlank() || b.isBlank() ? "" : dao.genererLogin(fPrenom.getText(), b)));
            fPrenom.textProperty().addListener((o, a, b) -> fLogin.setText(
                fNom.getText().isBlank() || b.isBlank() ? "" : dao.genererLogin(b, fNom.getText())));
        }

        Button btnSave = new Button(existing == null ? "Créer" : "Enregistrer");
        stylePrimaryButton(btnSave);
        Label errLbl = errorLabel();

        btnSave.setOnAction(e -> {
            if (fNom.getText().isBlank() || fPrenom.getText().isBlank()) { errLbl.setText("Nom et prénom obligatoires."); return; }
            Enseignant ens = existing != null ? existing : new Enseignant();
            ens.setCode(fCode.getText().trim()); ens.setNom(fNom.getText().trim());
            ens.setPrenom(fPrenom.getText().trim()); ens.setTelephone(fTel.getText().trim());
            ens.setLogin(fLogin.getText().trim());
            if (existing == null) { 
                if (dao.create(ens, fPass.getText())) {
                    masterData.add(ens); 
                    dlg.close();
                } else {
                    errLbl.setText("Erreur: Login déjà utilisé ou problème DB.");
                }
            } else { 
                if (dao.update(ens)) {
                    table.refresh(); 
                    dlg.close();
                } else {
                    errLbl.setText("Erreur lors de la mise à jour.");
                }
            }
        });

        form.getChildren().addAll(
            formRow("Code", fCode), formRow("Nom", fNom), formRow("Prénom", fPrenom),
            formRow("Téléphone", fTel), formRow("Login", fLogin), 
            (existing == null ? formRow("Mot de passe", fPass) : new Region()), 
            errLbl, btnSave
        );

        setupNav(fCode, fNom, fPrenom, fTel, fLogin, fPass, btnSave);

        dlg.setScene(new Scene(form, 420, -1));
        dlg.centerOnScreen();
        dlg.showAndWait();
    }

    private javafx.scene.Node buildEnseignantsAffectation() {
        if (currentAnnee == null) return noAnneePane();

        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        VBox leftSide = new VBox(10);
        leftSide.setPadding(new Insets(0, 15, 0, 0));
        
        TextField searchEns = new TextField();
        searchEns.setPromptText("🔍 Filtrer enseignant...");
        searchEns.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-padding: 6 12 6 12;");
        
        ListView<Enseignant> listEns = new ListView<>();
        listEns.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Enseignant ens, boolean empty) {
                super.updateItem(ens, empty);
                if (empty || ens == null) { setText(null); setStyle(""); }
                else { 
                    setText(ens.getNom() + " " + ens.getPrenom());
                    setStyle("-fx-padding: 10 12 10 12; -fx-font-size: 13px;");
                }
            }
        });
        
        ObservableList<Enseignant> allEns = FXCollections.observableArrayList(new EnseignantDAO().findAll());
        javafx.collections.transformation.FilteredList<Enseignant> filteredEns = new javafx.collections.transformation.FilteredList<>(allEns, p -> true);
        listEns.setItems(filteredEns);
        searchEns.textProperty().addListener((o, old, v) -> filteredEns.setPredicate(e -> v == null || v.isEmpty() || e.getNom().toLowerCase().contains(v.toLowerCase()) || e.getPrenom().toLowerCase().contains(v.toLowerCase())));

        leftSide.getChildren().addAll(new Label("Enseignants"), searchEns, listEns);
        VBox.setVgrow(listEns, Priority.ALWAYS);

        VBox rightSide = new VBox(15);
        rightSide.setPadding(new Insets(0, 0, 0, 15));
        
        Label lblTarget = new Label("Sélectionnez un enseignant");
        lblTarget.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + BLUE + ";");
        
        TableView<EnseignantDAO.TeacherAssignmentInfo> tableWorkload = new TableView<>();
        styleTable(tableWorkload);
        tableWorkload.setPlaceholder(new Label("Aucune affectation pour cet enseignant"));
        
        tableWorkload.getColumns().addAll(
            col("Niveau", "niveauLibelle", 120),
            col("Classe", "classeNom", 120),
            col("Matières affectées", "matieres", 280)
        );
        
        TableColumn<EnseignantDAO.TeacherAssignmentInfo, Void> colActions = new TableColumn<>("Action");
        colActions.setPrefWidth(140);
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button btnDel = smallBtn("Annuler affectation", RED);
            { btnDel.setOnAction(e -> {
                EnseignantDAO.TeacherAssignmentInfo info = getTableView().getItems().get(getIndex());
                if (confirmDelete("cette affectation (toutes les matières dans cette classe)")) {
                    new EnseignantDAO().deleteAssignmentGroup(listEns.getSelectionModel().getSelectedItem().getId(), info.getClasseId(), currentAnnee.getId());
                    getTableView().getItems().remove(info);
                }
            }); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : btnDel); setAlignment(Pos.CENTER); }
        });
        tableWorkload.getColumns().add(colActions);

        Button btnAdd = new Button("+ Nouvelle Affectation");
        stylePrimaryButton(btnAdd);
        btnAdd.setDisable(true);

        listEns.getSelectionModel().selectedItemProperty().addListener((o, old, ens) -> {
            if (ens != null) {
                lblTarget.setText("Affectations de " + ens.getNom() + " " + ens.getPrenom());
                tableWorkload.setItems(FXCollections.observableArrayList(new EnseignantDAO().getTeacherAssignments(ens.getId(), currentAnnee.getId())));
                btnAdd.setDisable(false);
            } else {
                lblTarget.setText("Sélectionnez un enseignant");
                tableWorkload.setItems(FXCollections.emptyObservableList());
                btnAdd.setDisable(true);
            }
        });

        btnAdd.setOnAction(e -> showAddAssignmentDialog(listEns.getSelectionModel().getSelectedItem(), tableWorkload));

        HBox header = new HBox(10, lblTarget, new Region(), btnAdd);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        rightSide.getChildren().addAll(header, new Label("Récapitulatif des cours par classe :"), tableWorkload);
        VBox.setVgrow(tableWorkload, Priority.ALWAYS);

        split.getItems().addAll(leftSide, rightSide);
        split.setDividerPositions(0.3);
        
        return split;
    }

    private void showAddAssignmentDialog(Enseignant ens, TableView<EnseignantDAO.TeacherAssignmentInfo> tableToRefresh) {
        Stage dlg = dialogStage("Nouvelle affectation : " + ens.getNom() + " " + ens.getPrenom());
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        ComboBox<Niveau> cbNiv = styledCombo("Choisir un niveau");
        cbNiv.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        cbNiv.setPrefWidth(Double.MAX_VALUE);

        Label lblClasses = infoLabel("Classes (sélectionnez-en une ou plusieurs)");
        ListView<Classe> listClasses = new ListView<>();
        listClasses.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listClasses.setPrefHeight(120);
        
        cbNiv.valueProperty().addListener((o, old, niv) -> {
            if (niv != null) listClasses.setItems(FXCollections.observableArrayList(new ClasseDAO().findByNiveauAndAnnee(niv.getId(), currentAnnee.getId())));
            else listClasses.getItems().clear();
        });

        Label lblMat = infoLabel("Matières du niveau");
        VBox matBox = new VBox(5);
        matBox.setPadding(new Insets(10));
        matBox.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px;");
        ScrollPane scrollMat = new ScrollPane(matBox);
        scrollMat.setFitToWidth(true); scrollMat.setPrefHeight(150);

        cbNiv.valueProperty().addListener((o, old, niv) -> {
            matBox.getChildren().clear();
            if (niv != null) {
                for (Matiere m : new MatiereDAO().findByNiveau(niv.getId())) {
                    CheckBox cb = new CheckBox(m.getNom());
                    cb.setUserData(m.getId());
                    cb.setStyle("-fx-padding: 4px;");
                    matBox.getChildren().add(cb);
                }
            }
        });

        Button btnOk = new Button("Enregistrer l'affectation");
        stylePrimaryButton(btnOk);
        btnOk.setPrefWidth(Double.MAX_VALUE);
        
        Label feedback = new Label();
        feedback.setStyle("-fx-font-size: 11px;");

        btnOk.setOnAction(e -> {
            List<Classe> selClasses = listClasses.getSelectionModel().getSelectedItems();
            List<Integer> selMats = new ArrayList<>();
            for (javafx.scene.Node n : matBox.getChildren()) if (n instanceof CheckBox cb && cb.isSelected()) selMats.add((Integer)cb.getUserData());

            if (selClasses.isEmpty() || selMats.isEmpty()) {
                feedback.setText("Sélectionnez au moins une classe et une matière.");
                feedback.setStyle("-fx-text-fill: " + RED + ";");
                return;
            }

            EnseignantDAO dao = new EnseignantDAO();
            StringBuilder allConflicts = new StringBuilder();
            for (Classe cl : selClasses) {
                String conf = dao.checkConflicts(cl.getId(), selMats, currentAnnee.getId(), ens.getId());
                if (conf != null) {
                    if (allConflicts.length() > 0) allConflicts.append("\n\n");
                    allConflicts.append("Classe ").append(cl.getNom()).append(" :\n").append(conf);
                }
            }

            if (allConflicts.length() > 0) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Conflit d'affectation");
                alert.setHeaderText("Certaines matières sont déjà affectées à d'autres enseignants :");
                alert.setContentText(allConflicts.toString() + "\n\nVoulez-vous forcer l'affectation à " + ens.getNom() + " ?\n(Cela retirera les matières aux autres enseignants)");
                if (alert.showAndWait().get() != ButtonType.OK) return;
            }

            int count = 0;
            for (Classe cl : selClasses) {
                if (dao.affecter(ens.getId(), cl.getId(), selMats, currentAnnee.getId())) count++;
            }
            
            tableToRefresh.setItems(FXCollections.observableArrayList(dao.getTeacherAssignments(ens.getId(), currentAnnee.getId())));
            dlg.close();
        });

        root.getChildren().addAll(new Label("1. Niveau"), cbNiv, lblClasses, listClasses, new Label("2. Matières"), scrollMat, btnOk, feedback);
        dlg.setScene(new Scene(root, 450, 600));
        dlg.showAndWait();
    }

    private void showClassesSection() {
        contentArea.getChildren().clear();
        contentArea.getChildren().addAll(sectionTitle("Gestion des classes"), buildClassesPanel());
    }

    private VBox buildClassesPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(20, 24, 20, 24));

        if (currentAnnee == null) { panel.getChildren().add(noAnneePane()); return panel; }

        VBox addCard = new VBox(12);
        addCard.setPadding(new Insets(20));
        addCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        addCard.setMaxWidth(600);

        ComboBox<Niveau> cbNiv = styledCombo("Sélectionner un niveau");
        cbNiv.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        cbNiv.setPrefWidth(220);

        Button btnAdd = new Button("+ Ajouter une classe");
        stylePrimaryButton(btnAdd);
        btnAdd.setPrefWidth(220);

        Label feedback = new Label();
        feedback.setStyle("-fx-font-size: 12px;");

        Label lblInfo = new Label();
        lblInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_MID + "; -fx-font-weight: bold;");

        addCard.getChildren().addAll(
            new Label("Ajouter une nouvelle classe") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); }},
            new HBox(15, formRow("Choisir le niveau", cbNiv), btnAdd) {{ setAlignment(Pos.BOTTOM_LEFT); }},
            feedback
        );

        TableView<Classe> table = new TableView<>();
        styleTable(table);
        table.setMaxWidth(500);
        TableColumn<Classe, String> cNom = col("Nom de la classe", "nom", 180);
        
        TableColumn<Classe, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(100);
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button btnDel = smallBtn("Supprimer", RED);
            { btnDel.setOnAction(e -> {
                Classe c = getTableView().getItems().get(getIndex());
                if (new EleveDAO().countAffectesClasse(c.getId()) > 0) {
                    alert("❌ Impossible de supprimer une classe qui n'est pas vide.");
                } else if (confirmDelete("la classe " + c.getNom())) {
                    new ClasseDAO().delete(c.getId());
                    getTableView().getItems().remove(c);
                }
            }); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : btnDel); }
        });

        table.getColumns().addAll(cNom, colActions);

        cbNiv.valueProperty().addListener((o, a, niv) -> {
            if (niv == null) return;
            List<Classe> classes = new ClasseDAO().findByNiveauAndAnnee(niv.getId(), currentAnnee.getId());
            table.setItems(FXCollections.observableArrayList(classes));
            lblInfo.setText("📊 " + classes.size() + " classe(s) existante(s) — " + (classes.size() * 20) + " places max");
        });

        btnAdd.setOnAction(e -> {
            if (cbNiv.getValue() == null) { feedback.setText("Choisissez un niveau."); feedback.setStyle("-fx-text-fill:" + RED + ";"); return; }
            boolean ok = new ClasseDAO().create(cbNiv.getValue().getId(), currentAnnee.getId(), cbNiv.getValue().getLibelle());
            if (ok) {
                List<Classe> classes = new ClasseDAO().findByNiveauAndAnnee(cbNiv.getValue().getId(), currentAnnee.getId());
                table.setItems(FXCollections.observableArrayList(classes));
                lblInfo.setText("📊 " + classes.size() + " classe(s) existante(s) — " + (classes.size() * 20) + " places max");
                feedback.setStyle("-fx-text-fill:" + GREEN + ";"); feedback.setText("Classe ajoutée.");
            } else { feedback.setStyle("-fx-text-fill:" + RED + ";"); feedback.setText("Erreur lors de la création."); }
        });

        panel.getChildren().addAll(addCard, new Separator(), lblInfo, table);
        panel.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(table, Priority.ALWAYS);
        return panel;
    }


    private void showMatieresSection() {
        contentArea.getChildren().clear();
        contentArea.getChildren().addAll(sectionTitle("Gestion des matières"), buildMatieresPanel());
    }

    private VBox buildMatieresPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(20, 24, 20, 24));

        ComboBox<Niveau> cbNiv = styledCombo("Filtrer par niveau");
        cbNiv.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        cbNiv.setPrefWidth(200);

        TableView<Matiere> table = new TableView<>();
        styleTable(table);
        table.setMaxWidth(700);
        TableColumn<Matiere, String> cNom = col("Matière", "nom", 180);
        TableColumn<Matiere, String> cNiv = new TableColumn<>("Niveau");
        cNiv.setPrefWidth(140);
        cNiv.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNiveauLibelle()));
        
        TableColumn<Matiere, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(180);
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button btnEdit = smallBtn("Modifier", BLUE);
            final Button btnDel  = smallBtn("Supprimer", RED);
            { HBox hb = new HBox(8, btnEdit, btnDel); hb.setAlignment(Pos.CENTER); setGraphic(hb);
              btnEdit.setOnAction(e -> showMatiereDialog(getTableView().getItems().get(getIndex()), table));
              btnDel.setOnAction(e -> {
                  Matiere m = getTableView().getItems().get(getIndex());
                  if (new MatiereDAO().isAssignedToTeacher(m.getId())) {
                      alert("❌ Impossible de supprimer une matière affectée à des enseignants.");
                  } else if (confirmDelete("la matière " + m.getNom())) {
                      new MatiereDAO().delete(m.getId());
                      table.getItems().remove(m);
                  }
              });
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : (HBox)getGraphic()); }
        });

        table.getColumns().addAll(cNom, cNiv, colActions);

        MatiereDAO dao = new MatiereDAO();
        table.setItems(FXCollections.observableArrayList(dao.findAll()));

        cbNiv.valueProperty().addListener((o, a, niv) ->
            table.setItems(FXCollections.observableArrayList(
                niv == null ? dao.findAll() : dao.findByNiveau(niv.getId()))));

        VBox addCard = new VBox(12);
        addCard.setPadding(new Insets(20));
        addCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        addCard.setMaxWidth(700);

        ComboBox<Niveau> cbNivAdd = styledCombo("Niveau");
        cbNivAdd.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        cbNivAdd.setPrefWidth(200);
        TextField fNom = styledField("Nom de la matière");
        fNom.setPrefWidth(250);
        Button btnAdd = new Button("+ Ajouter");
        stylePrimaryButton(btnAdd);
        Label feedback = new Label();
        feedback.setStyle("-fx-font-size: 12px;");

        btnAdd.setOnAction(e -> {
            if (fNom.getText().isBlank() || cbNivAdd.getValue() == null) { feedback.setText("Remplissez tous les champs."); feedback.setStyle("-fx-text-fill:" + RED + ";"); return; }
            Matiere m = new Matiere(); m.setNom(fNom.getText().trim()); m.setNiveauId(cbNivAdd.getValue().getId());
            if (dao.create(m)) {
                fNom.clear(); cbNivAdd.setValue(null);
                table.setItems(FXCollections.observableArrayList(dao.findAll()));
                feedback.setStyle("-fx-text-fill:" + GREEN + ";"); feedback.setText("Matière ajoutée.");
            } else { feedback.setStyle("-fx-text-fill:" + RED + ";"); feedback.setText("Erreur."); }
        });
        
        fNom.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ENTER) btnAdd.fire(); });

        addCard.getChildren().addAll(
            new Label("Nouvelle Matière") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); }},
            new HBox(15, formRow("Niveau", cbNivAdd), formRow("Nom", fNom), btnAdd) {{ setAlignment(Pos.BOTTOM_LEFT); }},
            feedback
        );

        HBox filterBar = new HBox(10, formRow("Filtrer par niveau", cbNiv));
        filterBar.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(addCard, new Separator(), filterBar, table);
        panel.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(table, Priority.ALWAYS);
        return panel;
    }

    private void showMatiereDialog(Matiere existing, TableView<Matiere> table) {
        Stage dlg = dialogStage("Modifier la matière");
        VBox form = dialogForm();
        MatiereDAO dao = new MatiereDAO();

        TextField fNom = styledField("Nom");
        fNom.setText(existing.getNom());
        ComboBox<Niveau> cbNiv = styledCombo("Niveau");
        cbNiv.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        for (Niveau n : cbNiv.getItems()) if (n.getId() == existing.getNiveauId()) { cbNiv.setValue(n); break; }

        Button btnSave = new Button("Enregistrer");
        stylePrimaryButton(btnSave);
        btnSave.setOnAction(e -> {
            existing.setNom(fNom.getText().trim());
            existing.setNiveauId(cbNiv.getValue().getId());
            if (dao.update(existing)) { table.refresh(); dlg.close(); }
        });

        form.getChildren().addAll(formRow("Nom", fNom), formRow("Niveau", cbNiv), btnSave);
        dlg.setScene(new Scene(form, 350, -1));
        dlg.show();
    }


    private void showNiveauxSection() {
        contentArea.getChildren().clear();
        contentArea.getChildren().addAll(sectionTitle("Gestion des niveaux"), buildNiveauxPanel());
    }

    private VBox buildNiveauxPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(20, 24, 20, 24));
        NiveauDAO dao = new NiveauDAO();

        TableView<Niveau> table = new TableView<>();
        styleTable(table);
        table.setMaxWidth(600);
        TableColumn<Niveau, String> cLib  = col("Libellé", "libelle", 220);
        
        TableColumn<Niveau, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(120);
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button btnEdit = smallBtn("Modifier", BLUE);
            { btnEdit.setOnAction(e -> showNiveauDialog(getTableView().getItems().get(getIndex()), table)); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : btnEdit); setAlignment(Pos.CENTER); }
        });

        table.getColumns().addAll(cLib, colActions);
        table.setItems(FXCollections.observableArrayList(dao.findAll()));

        VBox addCard = new VBox(12);
        addCard.setPadding(new Insets(20));
        addCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");
        addCard.setMaxWidth(600);
        
        TextField fLib  = styledField("Libellé du niveau (ex: 1ère année)");
        fLib.setPrefWidth(300);
        Button btnAdd = new Button("+ Ajouter");
        stylePrimaryButton(btnAdd);
        Label feedback = new Label();
        feedback.setStyle("-fx-font-size: 12px;");

        btnAdd.setOnAction(e -> {
            if (fLib.getText().isBlank()) { feedback.setText("Entrez un libellé."); feedback.setStyle("-fx-text-fill:" + RED + ";"); return; }
            Niveau n = new Niveau(); 
            n.setLibelle(fLib.getText().trim()); 
            n.setPrix(0.0); 
            if (dao.create(n)) {
                fLib.clear();
                table.setItems(FXCollections.observableArrayList(dao.findAll()));
                feedback.setStyle("-fx-text-fill:" + GREEN + ";"); feedback.setText("Niveau ajouté.");
            } else { feedback.setStyle("-fx-text-fill:" + RED + ";"); feedback.setText("Erreur."); }
        });
        
        fLib.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ENTER) btnAdd.fire(); });

        addCard.getChildren().addAll(
            new Label("Nouveau Niveau") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); }},
            new HBox(15, formRow("Libellé", fLib), btnAdd) {{ setAlignment(Pos.BOTTOM_LEFT); }},
            feedback
        );

        panel.getChildren().addAll(addCard, new Separator(), table);
        panel.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(table, Priority.ALWAYS);
        return panel;
    }

    private void showNiveauDialog(Niveau existing, TableView<Niveau> table) {
        Stage dlg = dialogStage("Modifier le niveau");
        VBox form = dialogForm();
        TextField fLib = styledField("Libellé");
        fLib.setText(existing.getLibelle());
        Button btnSave = new Button("Enregistrer");
        stylePrimaryButton(btnSave);
        btnSave.setOnAction(e -> {
            existing.setLibelle(fLib.getText().trim());
            if (new NiveauDAO().update(existing)) { table.refresh(); dlg.close(); }
        });
        form.getChildren().addAll(formRow("Libellé", fLib), btnSave);
        dlg.setScene(new Scene(form, 350, -1));
        dlg.show();
    }


    private HBox sectionTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";");
        HBox box = new HBox(lbl);
        box.setPadding(new Insets(16, 24, 0, 24));
        box.setStyle("-fx-background-color: " + WHITE + ";");
        return box;
    }

    private ToggleButton subTab(String text) {
        ToggleButton tb = new ToggleButton(text);
        tb.setPadding(new Insets(10, 20, 10, 20));
        tb.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 13px; -fx-text-fill: " + TEXT_MID + "; -fx-cursor: hand;");
        tb.selectedProperty().addListener((o, a, sel) -> tb.setStyle(sel
            ? "-fx-background-color: transparent; -fx-border-color: " + BLUE + "; -fx-border-width: 0 0 2 0; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + BLUE + "; -fx-cursor: hand;"
            : "-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 13px; -fx-text-fill: " + TEXT_MID + "; -fx-cursor: hand;"));
        return tb;
    }

    private <T> ComboBox<T> styledCombo(String prompt) {
        ComboBox<T> cb = new ComboBox<>(); cb.setPromptText(prompt); cb.setPrefHeight(34);
        cb.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 12px;");
        return cb;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt); tf.setPrefHeight(36);
        tf.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 13px; -fx-prompt-text-fill: #999;");
        return tf;
    }

    private <T> TableColumn<T, String> col(String header, String property, double w) {
        TableColumn<T, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(w); return c;
    }

    private void styleTable(TableView<?> t) {
        t.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px;");
        t.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        t.setPrefHeight(500);
    }

    private <T> TableColumn<T, Void> actionCol(TableView<T> table, Object dao,
            java.util.function.Consumer<T> onEdit, java.util.function.Consumer<T> onDel) {
        TableColumn<T, Void> col = new TableColumn<>("Actions");
        col.setPrefWidth(onDel != null ? 180 : 100);
        col.setCellFactory(tc -> new TableCell<>() {
            private final Button bEdit = smallBtn("Modifier", BLUE);
            private final Button bDel = onDel != null ? smallBtn("Supprimer", RED) : null;
            private final HBox hb = new HBox(8);
            {
                hb.setAlignment(Pos.CENTER);
                if (bEdit != null) {
                    hb.getChildren().add(bEdit);
                    bEdit.setOnAction(e -> {
                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                            onEdit.accept(getTableView().getItems().get(getIndex()));
                        }
                    });
                }
                if (bDel != null) {
                    hb.getChildren().add(bDel);
                    bDel.setOnAction(e -> {
                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                            onDel.accept(getTableView().getItems().get(getIndex()));
                        }
                    });
                }
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(hb);
                }
            }
        });
        return col;
    }

    private Button smallBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-border-color: " + color + "; -fx-border-radius: 4px; -fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 3 8 3 8;");
        return b;
    }

    private void stylePrimaryButton(Button btn) {
        String base  = "-fx-background-color: " + BLUE + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 7 16 7 16;";
        String hover = "-fx-background-color: " + BLUE_DARK + "; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 7 16 7 16;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited (e -> btn.setStyle(base));
    }

    private void styleSecondaryButton(Button btn) {
        String base  = "-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: " + TEXT_DARK + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 7 16 7 16;";
        String hover = "-fx-background-color: " + BG + "; -fx-border-color: " + BLUE + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: " + BLUE + "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 7 16 7 16;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited (e -> btn.setStyle(base));
    }

    private VBox formRow(String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MID + ";");
        VBox row = new VBox(6, lbl, field);
        return row;
    }

    private Label errorLabel() {
        Label l = new Label();
        l.setStyle("-fx-text-fill: " + RED + "; -fx-font-size: 11px;");
        l.setWrapText(true);
        return l;
    }

    private Label infoLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MID + ";");
        return l;
    }

    private Stage dialogStage(String title) {
        Stage dlg = new Stage();
        dlg.setTitle(title);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.initOwner(stage);
        dlg.setResizable(false);
        return dlg;
    }

    private VBox dialogForm() {
        VBox form = new VBox(12);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: " + WHITE + ";");
        form.setMinWidth(380);
        return form;
    }

    private VBox noAnneePane() {
        Label lbl = new Label("⚠  Veuillez sélectionner une année scolaire en haut de l'écran.");
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + TEXT_MID + ";");
        VBox box = new VBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }


    private void showBulletinsSection() {
        contentArea.getChildren().clear();
        if (currentAnnee == null) { contentArea.getChildren().add(noAnneePane()); return; }

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));

        HBox filters = new HBox(15);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-border-radius: 8;");

        ComboBox<Niveau> cbNiveau = styledCombo("Niveau");
        cbNiveau.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        
        ComboBox<Classe> cbClasse = styledCombo("Classe");
        ComboBox<Eleve> cbEleve = styledCombo("Élève (Optionnel)");
        cbEleve.setPromptText("Toute la classe");

        ComboBox<Integer> cbTrim = styledCombo("Trimestre");
        cbTrim.setItems(FXCollections.observableArrayList(1, 2, 3));
        cbTrim.setValue(1);

        cbNiveau.valueProperty().addListener((o, old, val) -> {
            if (val != null) {
                cbClasse.setItems(FXCollections.observableArrayList(new ClasseDAO().findByNiveauAndAnnee(val.getId(), currentAnnee.getId())));
                cbEleve.setItems(FXCollections.emptyObservableList());
            }
        });

        cbClasse.valueProperty().addListener((o, old, val) -> {
            if (val != null) {
                cbEleve.setItems(FXCollections.observableArrayList(new EleveDAO().findElevesByClasse(val.getId())));
            }
        });

        Button btnGen = new Button("Générer");
        stylePrimaryButton(btnGen);

        Button btnPrintAll = new Button("🖨️ Imprimer Tout");
        styleSecondaryButton(btnPrintAll);
        btnPrintAll.setDisable(true);

        filters.getChildren().addAll(
            new VBox(5, infoLabel("Niveau"), cbNiveau),
            new VBox(5, infoLabel("Classe"), cbClasse),
            new VBox(5, infoLabel("Élève"), cbEleve),
            new VBox(5, infoLabel("Trimestre"), cbTrim),
            new VBox(5, new Label(""), new HBox(10, btnGen, btnPrintAll))
        );

        VBox bulletinContainer = new VBox(40);
        bulletinContainer.setAlignment(Pos.TOP_CENTER);
        bulletinContainer.setPadding(new Insets(20, 0, 40, 0));

        btnGen.setOnAction(e -> {
            if (cbClasse.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner au moins une classe.").show();
                return;
            }
            bulletinContainer.getChildren().clear();
            
            int trimId = new TrimestreDAO().findIdByNumeroAndAnnee(cbTrim.getValue(), currentAnnee.getId());
            List<String> nonValidated = new NoteDAO().getNonValidatedTeachers(cbClasse.getValue().getId(), currentAnnee.getId(), trimId);
            
            if (!nonValidated.isEmpty()) {
                StringBuilder msg = new StringBuilder("Impossible de générer les bulletins.\nLes enseignants suivants n'ont pas encore validé leurs notes :\n\n");
                for (String name : nonValidated) msg.append("- ").append(name).append("\n");
                msg.append("\nTous les enseignants doivent valider avant la génération.");
                
                Alert alert = new Alert(Alert.AlertType.ERROR, msg.toString());
                alert.setTitle("Validation manquante");
                alert.setHeaderText(null);
                alert.show();
                return;
            }

            if (cbEleve.getValue() != null) {
                bulletinContainer.getChildren().add(buildBulletinCard(cbEleve.getValue(), cbClasse.getValue(), cbTrim.getValue()));
            } else {
                for (Eleve el : cbEleve.getItems()) {
                    bulletinContainer.getChildren().add(buildBulletinCard(el, cbClasse.getValue(), cbTrim.getValue()));
                }
            }
            btnPrintAll.setDisable(bulletinContainer.getChildren().isEmpty());
        });

        btnPrintAll.setOnAction(e -> {
            for (javafx.scene.Node node : bulletinContainer.getChildren()) {
                if (node instanceof VBox rb) {
                    for (javafx.scene.Node n : rb.getChildren()) {
                        if (n.getStyleClass().contains("bulletin-card")) {
                            printBulletin(n);
                        }
                    }
                }
            }
        });

        root.getChildren().addAll(sectionTitle("Bulletins de Notes"), filters, bulletinContainer);
        contentArea.getChildren().add(root);
    }

    private VBox buildBulletinCard(Eleve el, Classe cl, int trimNum) {
        VBox rootCard = new VBox(10);
        rootCard.setAlignment(Pos.TOP_RIGHT);

        Button btnPrint = new Button("🖨️ Imprimer");
        styleSecondaryButton(btnPrint);
        
        VBox card = new VBox(25);
        card.getStyleClass().add("bulletin-card");
        card.setMaxWidth(800);
        card.setStyle("-fx-background-color: white; -fx-padding: 50; -fx-background-radius: 2; -fx-border-color: #000; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        btnPrint.setOnAction(e -> printBulletin(card));

        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        Label school = new Label("SYSTÈME DE GESTION SCOLAIRE — " + currentAnnee.getLibelle());
        school.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
        Label title = new Label("BULLETIN DE NOTES - TRIMESTRE " + trimNum);
        title.setStyle("-fx-font-size: 15px; -fx-text-fill: " + TEXT_MID + "; -fx-letter-spacing: 2px;");
        header.getChildren().addAll(school, title, new Separator() {{ setStyle("-fx-background-color: black;"); }});

        GridPane info = new GridPane();
        info.setHgap(60); info.setVgap(12);
        info.setAlignment(Pos.CENTER);
        
        info.add(new Label("Nom & Prénom :") {{ setStyle("-fx-text-fill: " + TEXT_MID + ";"); }}, 0, 0);
        info.add(new Label(el.getNomComplet()) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 14px;"); }}, 1, 0);
        
        info.add(new Label("Classe :") {{ setStyle("-fx-text-fill: " + TEXT_MID + ";"); }}, 0, 1);
        info.add(new Label(cl.getLibelle()) {{ setStyle("-fx-font-weight: bold;"); }}, 1, 1);
        
        info.add(new Label("Matricule :") {{ setStyle("-fx-text-fill: " + TEXT_MID + ";"); }}, 2, 0);
        info.add(new Label(el.getMatricule() != null ? el.getMatricule() : "N/A") {{ setStyle("-fx-font-weight: bold;"); }}, 3, 0);
        
        info.add(new Label("Date de naissance :") {{ setStyle("-fx-text-fill: " + TEXT_MID + ";"); }}, 2, 1);
        info.add(new Label(el.getDateNaissance() != null ? el.getDateNaissance() : "N/A") {{ setStyle("-fx-font-weight: bold;"); }}, 3, 1);

        
        VBox table = new VBox(0);
        table.setStyle("-fx-border-color: #CCC; -fx-border-width: 1 1 0 1;");
        
        HBox tHead = new HBox(0);
        tHead.setStyle("-fx-background-color: #F1F5F9; -fx-border-color: #CCC; -fx-border-width: 0 0 1 0;");
        Label lMat = new Label("Matière"); lMat.setPrefWidth(300); lMat.setPadding(new Insets(8));
        Label lNote = new Label("Note / 20"); lNote.setPrefWidth(100); lNote.setPadding(new Insets(8));
        Label lCoef = new Label("Coef."); lCoef.setPrefWidth(80); lCoef.setPadding(new Insets(8));
        Label lObs = new Label("Observation par matière"); lObs.setPrefWidth(220); lObs.setPadding(new Insets(8));
        tHead.getChildren().addAll(lMat, lNote, lCoef, lObs);
        table.getChildren().add(tHead);

        
        int trimId = new TrimestreDAO().findIdByNumeroAndAnnee(trimNum, currentAnnee.getId());
        NoteDAO noteDAO = new NoteDAO();
        Map<String, Double> allNotes = noteDAO.loadNotesByTrimestreId(0, cl.getId(), trimId);
        Map<String, String> allObs = noteDAO.loadObservations(cl.getId(), trimId);
        List<Matiere> matieres = new MatiereDAO().findByNiveau(cl.getNiveauId());
        
        double sum = 0;
        int count = 0;
        for (Matiere m : matieres) {
            HBox row = new HBox(0);
            row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0;");
            Label mName = new Label(m.getNom()); mName.setPrefWidth(300); mName.setPadding(new Insets(8));
            
            Double val = allNotes.get(el.getId() + "_" + m.getId());
            String noteStr = (val != null) ? String.format("%.2f", val) : "N.N";
            if (val != null) { sum += val; count++; }
            
            Label mNote = new Label(noteStr); mNote.setPrefWidth(100); mNote.setPadding(new Insets(8));
            Label mCoef = new Label("1.00"); mCoef.setPrefWidth(80); mCoef.setPadding(new Insets(8));
            
            String obsText = allObs.get(el.getId() + "_" + m.getId());
            if (obsText == null || obsText.trim().isEmpty()) {
                obsText = "........................................";
            }
            Label mObs = new Label(obsText); mObs.setPrefWidth(220); mObs.setPadding(new Insets(8));
            mObs.setStyle("-fx-font-style: italic; -fx-font-size: 11px;");

            row.getChildren().addAll(mName, mNote, mCoef, mObs);
            table.getChildren().add(row);
        }

        
        HBox footer = new HBox(0);
        footer.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #000; -fx-border-width: 1 1 1 1;");
        Label lMoyTxt = new Label("MOYENNE GÉNÉRALE"); lMoyTxt.setPrefWidth(400); lMoyTxt.setPadding(new Insets(12));
        lMoyTxt.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        double moy = count > 0 ? sum / count : 0;
        Label lMoyVal = new Label(String.format("%.2f / 20", moy)); lMoyVal.setPrefWidth(400); lMoyVal.setPadding(new Insets(12));
        lMoyVal.setAlignment(Pos.CENTER_RIGHT);
        lMoyVal.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: black;");
        footer.getChildren().addAll(lMoyTxt, lMoyVal);

        HBox signatures = new HBox(0);
        signatures.setPadding(new Insets(30, 0, 0, 0));
        VBox sigAdmin = new VBox(60, new Label("Signature du Directeur") {{ setStyle("-fx-font-weight: bold;"); }}, new Label("........................................"));
        sigAdmin.setAlignment(Pos.CENTER);
        VBox sigObs = new VBox(10, new Label("Observations du conseil des classes :") {{ setStyle("-fx-font-weight: bold;"); }}, new Label("................................................................................\n................................................................................"));
        HBox.setHgrow(sigObs, Priority.ALWAYS);
        
        signatures.getChildren().addAll(sigObs, sigAdmin);

        card.getChildren().addAll(header, info, table, footer, signatures);
        
        rootCard.getChildren().addAll(btnPrint, card);
        return rootCard;
    }

    private void printBulletin(javafx.scene.Node node) {
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(null)) {
            
            double scaleX = job.getJobSettings().getPageLayout().getPrintableWidth() / node.getBoundsInParent().getWidth();
            node.getTransforms().add(new javafx.scene.transform.Scale(scaleX, scaleX));
            
            if (job.printPage(node)) {
                job.endJob();
            }
            node.getTransforms().clear();
        }
    }

    private VBox lockedPane(String msg) {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));
        Label icon = new Label("🔒");
        icon.setStyle("-fx-font-size: 40px;");
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_MID + "; -fx-text-alignment: center;");
        lbl.setWrapText(true);
        box.getChildren().addAll(icon, lbl);
        return box;
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean confirmDelete(String target) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmation de suppression");
        a.setHeaderText("Supprimer " + target + " ?");
        a.setContentText("Cette action est irréversible.");
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void setupNav(javafx.scene.Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            final int index = i;
            nodes[i].setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    if (index < nodes.length - 1) {
                        if (index == nodes.length - 2 && nodes[nodes.length - 1] instanceof Button btn) {
                            btn.fire();
                        } else {
                            nodes[index + 1].requestFocus();
                            if (nodes[index + 1] instanceof TextField tf) tf.selectAll();
                        }
                    } else if (nodes[index] instanceof Button b) {
                        b.fire();
                    }
                } else if (e.getCode() == javafx.scene.input.KeyCode.DOWN) {
                    if (index < nodes.length - 1) {
                        nodes[index + 1].requestFocus();
                        if (nodes[index + 1] instanceof TextField tf) tf.selectAll();
                    }
                } else if (e.getCode() == javafx.scene.input.KeyCode.UP) {
                    if (index > 0) {
                        nodes[index - 1].requestFocus();
                        if (nodes[index - 1] instanceof TextField tf) tf.selectAll();
                    }
                }
            });
        }
    }

    private void showAccountSettingsDialog(User u) {
        Stage dlg = dialogStage("Paramètres du compte");
        VBox form = dialogForm();

        TextField fLogin = styledField("Nouveau Login");
        fLogin.setText(u.getLogin());
        
        PasswordField fPass = new PasswordField();
        fPass.setPromptText("Nouveau Mot de passe");
        fPass.setPrefHeight(36);
        fPass.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        Button btnSave = new Button("Enregistrer");
        stylePrimaryButton(btnSave);

        btnSave.setOnAction(e -> {
            if (fLogin.getText().isBlank() || fPass.getText().isBlank()) {
                alert("Veuillez remplir tous les champs.");
                return;
            }
            if (new UserDAO().update(u.getId(), fLogin.getText().trim(), fPass.getText())) {
                u.setLogin(fLogin.getText().trim());
                alert("✓ Paramètres mis à jour avec succès.");
                dlg.close();
            } else {
                alert("❌ Erreur lors de la mise à jour.");
            }
        });

        fLogin.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) fPass.requestFocus(); });
        fPass.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) btnSave.fire(); });
        form.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, k -> { if(k.getCode() == javafx.scene.input.KeyCode.ESCAPE) dlg.close(); });

        form.getChildren().addAll(
            new Label("Modifier vos accès") {{ setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";"); }},
            formRow("Identifiant", fLogin),
            formRow("Mot de passe", fPass),
            btnSave
        );

        dlg.setScene(new Scene(form));
        dlg.show();
    }
}
