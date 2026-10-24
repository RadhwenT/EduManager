package io.github.radhwent.edumanager.view;

import io.github.radhwent.edumanager.dao.AnneeScolaireDAO;
import io.github.radhwent.edumanager.dao.ClasseDAO;
import io.github.radhwent.edumanager.dao.FinancierDAO;
import io.github.radhwent.edumanager.dao.NiveauDAO;
import io.github.radhwent.edumanager.dao.UserDAO;
import io.github.radhwent.edumanager.model.AnneeScolaire;
import io.github.radhwent.edumanager.model.Classe;
import io.github.radhwent.edumanager.model.Niveau;
import io.github.radhwent.edumanager.model.User;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class FinancierView {

    private Stage stage;
    private User currentUser;
    private AnneeScolaire currentAnnee;
    
    private final String BLUE = "#2563EB";
    private final String BG = "#F8FAFC";
    private final String WHITE = "#FFFFFF";
    private final String TEXT_DARK = "#1E293B";
    private final String TEXT_MID = "#475569";
    private final String BORDER = "#E2E8F0";
    private final String GREEN = "#10B981";
    private final String RED = "#EF4444";

    private VBox contentArea;
    private FinancierDAO financierDAO = new FinancierDAO();

    public FinancierView(Stage stage, User user) {
        this.stage = stage;
        this.currentUser = user;
        List<AnneeScolaire> annees = new AnneeScolaireDAO().findAll();
        if (!annees.isEmpty()) {
            this.currentAnnee = annees.get(0);
        }
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #1E293B;");

        Label logo = new Label("EduManager");
        logo.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 25 20 25 20;");

        VBox menu = new VBox(5);
        menu.setPadding(new Insets(10, 0, 10, 0));

        ToggleButton btnPay = menuBtn("💰 Encaissements");
        ToggleButton btnTarifs = menuBtn("🏷️ Tarifs Niveaux");
        ToggleButton btnVers = menuBtn("🏦 Versements");

        ToggleGroup tg = new ToggleGroup();
        btnTarifs.setToggleGroup(tg);
        btnPay.setToggleGroup(tg);
        btnVers.setToggleGroup(tg);

        btnTarifs.setOnAction(e -> showTarifs());
        btnPay.setOnAction(e -> showEncaissements());
        btnVers.setOnAction(e -> showVersements());

        menu.getChildren().addAll(btnPay, btnTarifs, btnVers);
        sidebar.getChildren().addAll(logo, menu);

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 24, 15, 24));
        topBar.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        Label lblUser = new Label("Connecté : " + currentUser.getLogin() + " (Financier)");
        lblUser.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MID + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("Déconnexion");
        btnLogout.setStyle("-fx-background-color: transparent; -fx-border-color: " + BORDER + "; -fx-border-radius: 4px; -fx-text-fill: " + TEXT_MID + "; -fx-font-size: 11px; -fx-cursor: hand;");
        btnLogout.setOnAction(e -> new LoginView(stage).show());

        Button btnSettings = new Button("⚙️ Paramètres");
        btnSettings.setStyle("-fx-background-color: transparent; -fx-border-color: " + BORDER + "; -fx-border-radius: 4px; -fx-text-fill: " + BLUE + "; -fx-font-size: 11px; -fx-cursor: hand;");
        btnSettings.setOnAction(e -> showAccountSettingsDialog());

        topBar.getChildren().addAll(lblUser, spacer, btnSettings, btnLogout);

        contentArea = new VBox(20);
        contentArea.setPadding(new Insets(24));
        
        root.setLeft(sidebar);
        root.setTop(topBar);
        root.setCenter(contentArea);

        btnPay.setSelected(true);
        showEncaissements();

        Scene scene = new Scene(root, 1100, 750);
        stage.setScene(scene);
        stage.setTitle("Espace Financier — EduManager");
        stage.setMaximized(true);
        stage.centerOnScreen();
        stage.show();
    }

    private ToggleButton menuBtn(String text) {
        ToggleButton b = new ToggleButton(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(12, 20, 12, 20));
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-cursor: hand;");
        
        b.selectedProperty().addListener((o, old, sel) -> {
            if (sel) b.setStyle("-fx-background-color: " + BLUE + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
            else b.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        });
        return b;
    }

    private void showTarifs() {
        contentArea.getChildren().clear();
        Label title = new Label("Gestion des Tarifs par Niveau");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";");

        TableView<Niveau> table = new TableView<>();
        styleTable(table);
        
        TableColumn<Niveau, String> colLib = new TableColumn<>("Niveau");
        colLib.setCellValueFactory(new PropertyValueFactory<>("libelle"));
        colLib.setPrefWidth(300);

        TableColumn<Niveau, Double> colPrix = new TableColumn<>("Tarif (DT)");
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colPrix.setPrefWidth(150);

        TableColumn<Niveau, Void> colActions = new TableColumn<>("Action");
        colActions.setPrefWidth(120);
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button btn = smallBtn("Modifier", BLUE);
            { btn.setOnAction(e -> showEditTarifDialog(getTableView().getItems().get(getIndex()), table)); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : btn); setAlignment(Pos.CENTER); }
        });

        table.getColumns().addAll(colLib, colPrix, colActions);
        table.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));

        contentArea.getChildren().addAll(title, table);
    }

    private void showEditTarifDialog(Niveau n, TableView<Niveau> table) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Modifier Tarif : " + n.getLibelle());
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(24));
        
        TextField fPrix = new TextField(String.valueOf(n.getPrix()));
        fPrix.setPromptText("Entrez le nouveau prix");
        styleField(fPrix);

        Button btnSave = new Button("Enregistrer");
        stylePrimaryButton(btnSave);
        btnSave.setMaxWidth(Double.MAX_VALUE);
        
        btnSave.setOnAction(e -> {
            try {
                n.setPrix(Double.parseDouble(fPrix.getText()));
                if (new NiveauDAO().update(n)) {
                    table.refresh();
                    dlg.close();
                }
            } catch (Exception ex) { alert("Prix invalide."); }
        });

        root.getChildren().addAll(new Label("Nouveau tarif pour " + n.getLibelle() + " :"), fPrix, btnSave);
        dlg.setScene(new Scene(root, 350, 200));
        dlg.show();
    }

    private void showEncaissements() {
        contentArea.getChildren().clear();
        Label title = new Label("Enregistrement des Paiements");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";");

        TextField search = new TextField();
        search.setPromptText("🔍 Rechercher un élève...");
        styleField(search);
        search.setPrefWidth(250);

        ComboBox<Niveau> cbNiveau = new ComboBox<>();
        cbNiveau.setPromptText("Tous les niveaux");
        cbNiveau.setItems(FXCollections.observableArrayList(new NiveauDAO().findAll()));
        cbNiveau.getItems().add(0, null); // Option "Tous"
        cbNiveau.setPrefHeight(38);

        ComboBox<Classe> cbClasse = new ComboBox<>();
        cbClasse.setPromptText("Toutes les classes");
        cbClasse.setPrefHeight(38);

        cbNiveau.valueProperty().addListener((o, old, niv) -> {
            cbClasse.setValue(null);
            if (niv == null) {
                cbClasse.setItems(FXCollections.observableArrayList(new ClasseDAO().findAll(currentAnnee.getId())));
            } else {
                cbClasse.setItems(FXCollections.observableArrayList(
                    new ClasseDAO().findByNiveauAndAnnee(niv.getId(), currentAnnee.getId())));
            }
        });
        cbClasse.setItems(FXCollections.observableArrayList(new ClasseDAO().findAll(currentAnnee.getId())));

        TableView<FinancierDAO.StudentPaymentInfo> table = new TableView<>();
        styleTable(table);
        
        table.getColumns().addAll(
            col("Matricule", "matricule", 100),
            col("Élève", "eleveNom", 180),
            col("Classe", "classe", 120),
            col("Niveau", "niveau", 120),
            col("Tarif Niveau", "tarif", 110),
            col("Déjà Payé", "paye", 110),
            col("Reste", "reste", 110)
        );

        TableColumn<FinancierDAO.StudentPaymentInfo, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(200);
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button btnPay = smallBtn("Payer", GREEN);
            final Button btnHist = smallBtn("Historique", BLUE);
            final HBox box = new HBox(8, btnPay, btnHist);
            {
                box.setAlignment(Pos.CENTER);
                btnPay.setOnAction(e -> showPaymentDialog(getTableView().getItems().get(getIndex()), table));
                btnHist.setOnAction(e -> showHistoryDialog(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) { 
                super.updateItem(v, empty); 
                if (empty) setGraphic(null);
                else {
                    FinancierDAO.StudentPaymentInfo info = getTableView().getItems().get(getIndex());
                    btnPay.setDisable(info.getReste() <= 0);
                    setGraphic(box);
                }
                setAlignment(Pos.CENTER); 
            }
        });
        table.getColumns().add(colActions);

        Runnable refresh = () -> {
            Integer nid = cbNiveau.getValue() != null ? cbNiveau.getValue().getId() : null;
            Integer cid = cbClasse.getValue() != null ? cbClasse.getValue().getId() : null;
            table.setItems(FXCollections.observableArrayList(financierDAO.findStudentPayments(currentAnnee.getId(), nid, cid, search.getText())));
        };

        search.textProperty().addListener((o, old, v) -> refresh.run());
        cbNiveau.valueProperty().addListener((o, old, v) -> refresh.run());
        cbClasse.valueProperty().addListener((o, old, v) -> refresh.run());

        refresh.run();

        HBox filters = new HBox(15, 
            new VBox(5, new Label("Niveau"), cbNiveau),
            new VBox(5, new Label("Classe"), cbClasse),
            new VBox(5, new Label("Nom de l'élève"), search)
        );
        filters.setAlignment(Pos.BOTTOM_LEFT);
        
        contentArea.getChildren().addAll(title, filters, table);
    }

    private void showPaymentDialog(FinancierDAO.StudentPaymentInfo info, TableView<FinancierDAO.StudentPaymentInfo> table) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Nouveau Paiement : " + info.getEleveNom());
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(24));

        Label lblInfo = new Label("Reste à payer : " + String.format("%.2f", info.getReste()) + " DT");
        lblInfo.setStyle("-fx-font-weight: bold; -fx-text-fill: " + BLUE + ";");

        TextField fMontant = new TextField(String.valueOf(info.getReste()));
        styleField(fMontant);
        
        ComboBox<String> cbMode = new ComboBox<>(FXCollections.observableArrayList("Espèces", "Chèque", "Virement"));
        cbMode.setValue("Espèces");
        cbMode.setMaxWidth(Double.MAX_VALUE);
        cbMode.setPrefHeight(38);

        Button btnPreview = new Button("👁️ Aperçu du reçu");
        styleSecondaryButton(btnPreview);
        btnPreview.setMaxWidth(Double.MAX_VALUE);

        btnPreview.setOnAction(e -> {
            try {
                double m = Double.parseDouble(fMontant.getText());
                showReceiptPreview(info, m, cbMode.getValue(), table, dlg);
            } catch (Exception ex) { alert("Montant invalide."); }
        });

        root.getChildren().addAll(
            new Label("Paiement pour " + info.getEleveNom() + " (" + info.getMatricule() + ")"),
            lblInfo, 
            new Label("Montant à payer :"), fMontant, 
            new Label("Mode de règlement :"), cbMode, 
            btnPreview
        );
        dlg.setScene(new Scene(root, 400, 480));
        dlg.show();
    }

    private void showReceiptPreview(FinancierDAO.StudentPaymentInfo info, double m, String mode, TableView<FinancierDAO.StudentPaymentInfo> table, Stage parentDlg) {
        Stage dlg = new Stage();
        dlg.setTitle("Aperçu du reçu");
        
        VBox previewBox = buildReceiptNode(info, m, mode);
        previewBox.setStyle("-fx-background-color: white; -fx-padding: 40; -fx-border-color: #EEE; -fx-border-width: 1;");
        
        Button btnPrint = new Button("🖨️ Confirmer & Imprimer");
        stylePrimaryButton(btnPrint);
        btnPrint.setMaxWidth(Double.MAX_VALUE);
        
        btnPrint.setOnAction(e -> {
            if (financierDAO.recordPayment(info.getEleveId(), currentAnnee.getId(), m, mode, "")) {
                printReceipt(previewBox);
                table.setItems(FXCollections.observableArrayList(financierDAO.findStudentPayments(currentAnnee.getId(), null, null, "")));
                dlg.close();
                parentDlg.close();
                alert("✓ Paiement validé et reçu imprimé.");
            }
        });

        VBox root = new VBox(20, previewBox, btnPrint);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F1F5F9;");
        dlg.setScene(new Scene(root));
        dlg.show();
    }

    private void showHistoryDialog(FinancierDAO.StudentPaymentInfo info) {
        Stage dlg = new Stage();
        dlg.setTitle("Historique des paiements : " + info.getEleveNom());
        
        TableView<FinancierDAO.Payment> table = new TableView<>();
        styleTable(table);
        
        table.getColumns().addAll(
            col("Date", "date", 200),
            col("Montant (DT)", "montant", 120),
            col("Mode", "mode", 120)
        );
        
        table.setItems(FXCollections.observableArrayList(financierDAO.getPaymentHistory(info.getEleveId(), currentAnnee.getId())));
        
        VBox root = new VBox(15, new Label("Historique complet pour l'année en cours"), table);
        root.setPadding(new Insets(20));
        dlg.setScene(new Scene(root, 500, 400));
        dlg.show();
    }

    private void styleSecondaryButton(Button btn) {
        btn.setStyle("-fx-background-color: transparent; -fx-border-color: " + BLUE + "; -fx-border-radius: 6px; -fx-text-fill: " + BLUE + "; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 10 20 10 20;");
    }

    private void styleTable(TableView<?> t) {
        t.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER + "; -fx-border-radius: 8px;");
        VBox.setVgrow(t, Priority.ALWAYS);
    }

    private <T> TableColumn<T, String> col(String header, String property, double w) {
        TableColumn<T, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(w);
        return c;
    }

    private void styleField(TextField tf) {
        tf.setPrefHeight(38);
        tf.setStyle("-fx-background-color: " + WHITE + "; -fx-border-color: " + BORDER + "; -fx-border-radius: 6px; -fx-padding: 6 12 6 12;");
    }

    private void stylePrimaryButton(Button btn) {
        btn.setStyle("-fx-background-color: " + BLUE + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 10 20 10 20;");
    }

    private Button smallBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-border-color: " + color + "; -fx-border-radius: 4px; -fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-cursor: hand;");
        return b;
    }

    private VBox buildReceiptNode(FinancierDAO.StudentPaymentInfo info, double amount, String mode) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(30));
        card.setPrefWidth(400);
        card.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1;");

        Label school = new Label("EduManager - REÇU DE PAIEMENT");
        school.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        VBox details = new VBox(10);
        details.getChildren().addAll(
            new Label("Date : " + new java.util.Date()),
            new Label("Élève : " + info.getEleveNom()),
            new Label("Niveau : " + info.getNiveau()),
            new Label("-------------------------------------------"),
            new Label("Montant Payé : " + String.format("%.2f DT", amount)),
            new Label("Mode : " + mode),
            new Label("-------------------------------------------"),
            new Label("Reste à Payer : " + String.format("%.2f DT", info.getReste() - amount))
        );

        Label footer = new Label("Merci pour votre confiance.");
        footer.setStyle("-fx-font-style: italic; -fx-font-size: 10px;");

        card.getChildren().addAll(school, new Separator(), details, new Separator(), footer);
        return card;
    }

    private void printReceipt(javafx.scene.Node node) {
        javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(stage)) {
            if (job.printPage(node)) {
                job.endJob();
            }
        }
    }

    private void showVersements() {
        contentArea.getChildren().clear();
        
        HBox header = new HBox(20);
        Label title = new Label("Gestion des Versements Bancaires");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_DARK + ";");
        header.getChildren().add(title);
        header.setAlignment(Pos.CENTER_LEFT);

        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-tab-min-width: 150px;");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Tab tabEncaisse = new Tab("🛒 En attente");
        tabEncaisse.setClosable(false);
        tabEncaisse.setContent(buildPendingVersementView());

        Tab tabHist = new Tab("📜 Historique");
        tabHist.setClosable(false);
        tabHist.setContent(buildVersementHistoryView());

        tabs.getTabs().addAll(tabEncaisse, tabHist);
        
        contentArea.getChildren().addAll(header, tabs);
    }

    private Node buildPendingVersementView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));

        VBox cashBox = new VBox(15);
        cashBox.setPadding(new Insets(20));
        cashBox.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER + "; -fx-border-radius: 8px;");
        cashBox.setPrefWidth(350);
        
        double cash = financierDAO.getCashInHand(currentAnnee.getId());
        Label lblCash = new Label(String.format("%.2f DT", cash));
        lblCash.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + GREEN + ";");
        
        Button btnVersementCash = new Button("Nouveau versement (Espèces)");
        stylePrimaryButton(btnVersementCash);
        btnVersementCash.setDisable(cash <= 0);
        btnVersementCash.setMaxWidth(Double.MAX_VALUE);
        btnVersementCash.setOnAction(e -> showVersementCashDialog(cash));

        cashBox.getChildren().addAll(
            new Label("Espèces en Caisse (non versées)") {{ setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_MID + ";"); }},
            lblCash,
            new Separator(),
            btnVersementCash
        );

        VBox checksBox = new VBox(15);
        checksBox.setPadding(new Insets(20));
        checksBox.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER + "; -fx-border-radius: 8px;");
        HBox.setHgrow(checksBox, Priority.ALWAYS);

        TableView<FinancierDAO.Payment> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        styleTable(table);
        table.getColumns().addAll(
            col("Date", "date", 180),
            col("Élève", "eleveNom", 250),
            col("Montant", "montant", 120)
        );
        table.setItems(FXCollections.observableArrayList(financierDAO.getPendingChecks(currentAnnee.getId())));

        Button btnVersChecks = new Button("Verser les chèques sélectionnés");
        stylePrimaryButton(btnVersChecks);
        btnVersChecks.setOnAction(e -> showVersementChecksDialog(table.getSelectionModel().getSelectedItems()));

        checksBox.getChildren().addAll(
            new Label("Chèques en Caisse (Sélectionner pour verser)") {{ setStyle("-fx-font-weight: bold; -fx-text-fill: " + TEXT_MID + ";"); }},
            table,
            btnVersChecks
        );

        HBox layout = new HBox(20, cashBox, checksBox);
        VBox.setVgrow(layout, Priority.ALWAYS);
        root.getChildren().add(layout);
        return root;
    }

    private Node buildVersementHistoryView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        TextField fBank = new TextField();
        fBank.setPromptText("Filtrer par banque...");
        styleField(fBank);
        
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("Tous", "ESPECES", "CHEQUE"));
        cbType.setValue("Tous");
        cbType.setPrefHeight(38);

        TableView<FinancierDAO.Versement> table = new TableView<>();
        styleTable(table);
        table.getColumns().addAll(
            col("Date", "date", 180),
            col("Type", "type", 100),
            col("Montant", "montant", 120),
            col("Banque", "banque", 180),
            col("N° Bon", "numBon", 150)
        );

        Runnable refresh = () -> {
            String type = cbType.getValue().equals("Tous") ? null : cbType.getValue();
            table.setItems(FXCollections.observableArrayList(financierDAO.getVersementHistory(currentAnnee.getId(), type, fBank.getText())));
        };

        fBank.textProperty().addListener((o, old, v) -> refresh.run());
        cbType.valueProperty().addListener((o, old, v) -> refresh.run());
        refresh.run();

        HBox filters = new HBox(15, fBank, cbType);
        root.getChildren().addAll(new Label("Historique des versements bancaires"), filters, table);
        return root;
    }

    private void showVersementCashDialog(double currentBalance) {
        Stage dlg = new Stage();
        dlg.setTitle("Nouveau Versement d'Espèces");
        dlg.initModality(Modality.APPLICATION_MODAL);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(24));

        TextField fAmount = new TextField(String.valueOf(currentBalance));
        styleField(fAmount);
        TextField fBank = new TextField();
        fBank.setPromptText("Ex: BIAT, STB...");
        styleField(fBank);
        TextField fBon = new TextField();
        fBon.setPromptText("Référence du bordereau");
        styleField(fBon);

        Button btnSave = new Button("Valider le versement");
        stylePrimaryButton(btnSave);
        btnSave.setMaxWidth(Double.MAX_VALUE);

        btnSave.setOnAction(e -> {
            try {
                double amt = Double.parseDouble(fAmount.getText());
                if (amt <= 0 || amt > currentBalance) { alert("Le montant doit être compris entre 0 et " + currentBalance); return; }
                if (fBank.getText().isBlank()) { alert("Veuillez saisir le nom de la banque."); return; }
                
                if (financierDAO.recordCashVersement(currentAnnee.getId(), amt, fBank.getText().trim(), fBon.getText().trim())) {
                    dlg.close();
                    showVersements();
                    alert("✓ Versement d'espèces enregistré.");
                }
            } catch (Exception ex) { alert("Format de montant invalide."); }
        });

        fAmount.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) fBank.requestFocus(); });
        fBank.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) fBon.requestFocus(); });
        fBon.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) btnSave.fire(); });
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, k -> { if(k.getCode() == javafx.scene.input.KeyCode.ESCAPE) dlg.close(); });

        root.getChildren().addAll(
            new Label("Montant en caisse : " + String.format("%.2f", currentBalance) + " DT"),
            new Label("Montant à verser :"), fAmount,
            new Label("Banque de destination :"), fBank,
            new Label("N° de Bordereau / Bon :"), fBon,
            btnSave
        );
        dlg.setScene(new Scene(root, 400, 480));
        dlg.show();
    }

    private void showVersementChecksDialog(List<FinancierDAO.Payment> selected) {
        if (selected.isEmpty()) { alert("Veuillez sélectionner au moins un chèque dans la liste."); return; }
        
        Stage dlg = new Stage();
        dlg.setTitle("Versement de chèques (" + selected.size() + ")");
        dlg.initModality(Modality.APPLICATION_MODAL);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(24));

        double total = selected.stream().mapToDouble(FinancierDAO.Payment::getMontant).sum();
        Label lblTotal = new Label("Total à verser : " + String.format("%.2f", total) + " DT");
        lblTotal.setStyle("-fx-font-weight: bold; -fx-text-fill: " + BLUE + "; -fx-font-size: 16px;");

        TextField fBank = new TextField();
        fBank.setPromptText("Banque");
        styleField(fBank);
        TextField fBon = new TextField();
        fBon.setPromptText("N° Bordereau");
        styleField(fBon);

        Button btnSave = new Button("Confirmer le versement");
        stylePrimaryButton(btnSave);
        btnSave.setMaxWidth(Double.MAX_VALUE);

        btnSave.setOnAction(e -> {
            if (fBank.getText().isBlank()) { alert("Banque obligatoire."); return; }
            List<Integer> ids = selected.stream().map(FinancierDAO.Payment::getId).collect(java.util.stream.Collectors.toList());
            if (financierDAO.recordChecksVersement(currentAnnee.getId(), ids, fBank.getText().trim(), fBon.getText().trim())) {
                dlg.close();
                showVersements();
                alert("✓ Versement de " + selected.size() + " chèques enregistré.");
            }
        });

        fBank.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) fBon.requestFocus(); });
        fBon.setOnKeyPressed(k -> { if(k.getCode() == javafx.scene.input.KeyCode.ENTER) btnSave.fire(); });
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, k -> { if(k.getCode() == javafx.scene.input.KeyCode.ESCAPE) dlg.close(); });

        root.getChildren().addAll(lblTotal, new Label("Banque :"), fBank, new Label("N° Bordereau :"), fBon, btnSave);
        dlg.setScene(new Scene(root, 400, 420));
        dlg.show();
    }


    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.show();
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

        Button btnSave = new Button("Enregistrer");
        stylePrimaryButton(btnSave);
        btnSave.setMaxWidth(Double.MAX_VALUE);

        btnSave.setOnAction(e -> {
            if (fLogin.getText().isBlank() || fPass.getText().isBlank()) {
                alert("Veuillez remplir tous les champs.");
                return;
            }
            if (new UserDAO().update(currentUser.getId(), fLogin.getText().trim(), fPass.getText())) {
                currentUser.setLogin(fLogin.getText().trim());
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
            new Label("Modifier vos accès") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 16px;"); }},
            new VBox(5, new Label("Login"), fLogin),
            new VBox(5, new Label("Mot de passe"), fPass),
            btnSave
        );

        dlg.setScene(new Scene(form));
        dlg.show();
    }
}
