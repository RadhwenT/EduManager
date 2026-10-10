package application;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.util.Map;
import java.sql.*;

public class EnseignantView {

	private Stage stage;
	private User user;
	private boolean notesValidees = false;
	private int currentTrimestreId = -1;
	private Classe currentClasse = null;
	private Map<String, Double> currentNotesMap;
	private TableView<Eleve> table;

	private static final String BLUE = "#2563EB";
	private static final String BG = "#F8FAFC";
	private static final String WHITE = "#FFFFFF";
	private static final String TEXT_DARK = "#1E293B";
	private static final String TEXT_LIGHT = "#64748B";

	public EnseignantView(Stage stage, User user) {
		this.stage = stage;
		this.user = user;
	}

	public void show() {
		VBox accentStrip = new VBox();
		accentStrip.setPrefWidth(8);
		accentStrip.setStyle("-fx-background-color:" + BLUE);

		Label title = new Label("Espace Enseignant");
		title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:" + TEXT_DARK);

		Label userLabel = new Label("Connecté : " + user.getLogin());
		userLabel.setStyle("-fx-font-size:12px;-fx-text-fill:" + TEXT_LIGHT);

		VBox titleBox = new VBox(4, title, userLabel);

		Button btnLogout = new Button("Déconnexion");
		btnLogout.setOnAction(e -> new LoginView(stage).show());

		Button btnSettings = new Button("⚙️ Paramètres");
		btnSettings.setOnAction(e -> showAccountSettingsDialog());

		HBox topButtons = new HBox(10, btnSettings, btnLogout);
		topButtons.setAlignment(Pos.CENTER_RIGHT);

		HBox header = new HBox(titleBox, topButtons);
		HBox.setHgrow(titleBox, Priority.ALWAYS);
		header.setAlignment(Pos.CENTER_LEFT);
		header.setPadding(new Insets(20));
		header.setStyle("-fx-background-color:" + WHITE);

		ComboBox<AnneeScolaire> cbAnnee = new ComboBox<>();
		ComboBox<Niveau> cbNiveau = new ComboBox<>();
		ComboBox<Classe> cbClasse = new ComboBox<>();
		ComboBox<String> cbTrimestre = new ComboBox<>();

		cbNiveau.setDisable(true);
		cbClasse.setDisable(true);
		cbTrimestre.setDisable(true);

		cbAnnee.setItems(FXCollections.observableArrayList(new AnneeScolaireDAO().findAll()));

		cbAnnee.setOnAction(e -> {
			if (cbAnnee.getValue() == null) {
				cbNiveau.getItems().clear();
				cbNiveau.setDisable(true);
				return;
			}
			cbNiveau.setDisable(false);
			cbNiveau.setItems(FXCollections.observableArrayList(
					new NiveauDAO().findNiveauxByEnseignantAndAnnee(user, cbAnnee.getValue().getId())));
			cbClasse.getItems().clear();
			cbClasse.setDisable(true);
			cbTrimestre.getItems().clear();
			cbTrimestre.setDisable(true);
		});

		cbNiveau.setOnAction(e -> {
			if (cbNiveau.getValue() == null) {
				cbClasse.getItems().clear();
				cbClasse.setDisable(true);
				return;
			}
			cbClasse.setDisable(false);
			cbClasse.setItems(FXCollections.observableArrayList(new ClasseDAO().findClassesByEnseignantAnneeAndNiveau(
					user, cbAnnee.getValue().getId(), cbNiveau.getValue().getId())));
		});

		cbClasse.setOnAction(e -> {
			if (cbClasse.getValue() == null)
				return;
			cbTrimestre.setDisable(false);
			cbTrimestre.setItems(FXCollections.observableArrayList("Trimestre 1", "Trimestre 2", "Trimestre 3"));
		});

		Button btnAfficher = new Button("Afficher");

		HBox filters = new HBox(10, new Label("Année"), cbAnnee, new Label("Niveau"), cbNiveau, new Label("Classe"),
				cbClasse, new Label("Trimestre"), cbTrimestre, btnAfficher);
		filters.setAlignment(Pos.CENTER_LEFT);
		filters.setPadding(new Insets(10));
		filters.setStyle("-fx-background-color:" + BG);

		table = new TableView<>();
		table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
		VBox.setVgrow(table, Priority.ALWAYS);

		Button btnSave = new Button("💾 Enregistrer");
		btnSave.setStyle("-fx-padding: 10px 20px; -fx-font-size: 13px; -fx-background-color: " + BLUE
				+ "; -fx-text-fill: " + WHITE + "; -fx-cursor: hand; -fx-border-radius: 6px;");
		btnSave.setDisable(true);

		Button btnValidate = new Button("✓ Valider");
		btnValidate.setStyle("-fx-padding: 10px 20px; -fx-font-size: 13px; -fx-background-color: #16A34A; -fx-text-fill: "
				+ WHITE + "; -fx-cursor: hand; -fx-border-radius: 6px;");
		btnValidate.setDisable(true);

		Label statusLabel = new Label("Aucune donnée chargée");
		statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_LIGHT + ";");

		HBox buttonBox = new HBox(12, btnSave, btnValidate, new Separator(javafx.geometry.Orientation.VERTICAL),
				statusLabel);
		buttonBox.setAlignment(Pos.CENTER_LEFT);
		buttonBox.setPadding(new Insets(12, 10, 12, 10));
		buttonBox.setStyle("-fx-background-color: " + BG + ";");

		btnAfficher.setOnAction(e -> {
			if (cbAnnee.getValue() == null || cbNiveau.getValue() == null || cbClasse.getValue() == null
					|| cbTrimestre.getValue() == null) {
				alert("Veuillez sélectionner tous les filtres.");
				return;
			}
			table.getColumns().clear();
			currentClasse = cbClasse.getValue();
			int trimestreNumero = Integer.parseInt(cbTrimestre.getValue().split(" ")[1]);
			currentTrimestreId = new TrimestreDAO().findIdByNumeroAndAnnee(trimestreNumero, cbAnnee.getValue().getId());

			EleveDAO eleveDAO = new EleveDAO();
			MatiereDAO matiereDAO = new MatiereDAO();
			NoteDAO noteDAO = new NoteDAO();

			table.setItems(FXCollections.observableArrayList(eleveDAO.findElevesByClasse(currentClasse.getId())));
			currentNotesMap = noteDAO.loadNotesByTrimestreId(user.getId(), currentClasse.getId(), currentTrimestreId);

			notesValidees = noteDAO.areNotesValidated(user.getId(), currentClasse.getId(), currentTrimestreId);
			btnSave.setDisable(notesValidees);
			btnValidate.setDisable(notesValidees);

			if (notesValidees) {
				statusLabel.setText("✓ Notes validées - Modification interdite");
				statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #16A34A; -fx-font-weight: bold;");
			} else {
				statusLabel.setText("En cours de modification");
				statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_LIGHT + ";");
			}

			TableColumn<Eleve, String> colEleve = new TableColumn<>("Élève");
			colEleve.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomComplet()));
			colEleve.setMinWidth(180);
			colEleve.setPrefWidth(220);
			colEleve.setMaxWidth(260);
			table.getColumns().add(colEleve);

			for (Matiere m : matiereDAO.findMatieresByEnseignantAndClasse(user, currentClasse.getId())) {
				final int matiereId = m.getId();
				TableColumn<Eleve, String> col = new TableColumn<>(m.getNom());
				col.setMinWidth(90);
				col.setPrefWidth(100);
				col.setMaxWidth(110);
				col.setCellFactory(tc -> new NoteCell(matiereId, notesValidees));
				table.getColumns().add(col);
			}
		});

		btnSave.setOnAction(e -> {
			try {
				btnSave.requestFocus();
				Connection conn = DBConnection.getConnection();
				if (!conn.getAutoCommit()) {
					conn.commit();
				}
				alert("✓ Toutes les notes ont été enregistrées et persistées avec succès.");
			} catch (SQLException ex) {
				alert("❌ Erreur lors de l'enregistrement définitif : " + ex.getMessage());
			}
		});

		btnValidate.setOnAction(e -> {
			int missing = new NoteDAO().countMissingNotes(user.getId(), currentClasse.getId(), currentTrimestreId);
			if (missing > 0) {
				alert("❌ Impossible de valider : il manque " + missing + " note(s).\nVous devez saisir les notes de TOUS les élèves pour TOUTES vos matières dans cette classe.");
				return;
			}
			if (new Alert(Alert.AlertType.CONFIRMATION,
					"Êtes-vous certain de vouloir valider ces notes ?\nLa modification sera interdite après la validation.",
					ButtonType.YES, ButtonType.NO).showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
				new NoteDAO().validerNotes(user.getId(), currentClasse.getId(), currentTrimestreId);
				alert("✓ Notes validées avec succès !");
				cbAnnee.setValue(null);
				cbNiveau.setValue(null);
				cbClasse.setValue(null);
				cbTrimestre.setValue(null);
				cbNiveau.setDisable(true);
				cbClasse.setDisable(true);
				cbTrimestre.setDisable(true);
				table.getItems().clear();
				table.getColumns().clear();
				btnSave.setDisable(true);
				btnValidate.setDisable(true);
				statusLabel.setText("Aucune donnée chargée");
				statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_LIGHT + ";");
			}
		});

		VBox center = new VBox(header, filters, table, buttonBox);
		VBox.setVgrow(table, Priority.ALWAYS);
		center.setFillWidth(true);

		HBox root = new HBox(accentStrip, center);
		HBox.setHgrow(center, Priority.ALWAYS);

		Scene scene = new Scene(root, 1200, 700);
		stage.setScene(scene);
		stage.setTitle("Espace Enseignant — EduManager");
		stage.setResizable(true);
		stage.centerOnScreen();
		stage.show();

		centerStage(stage);
	}

	private class NoteCell extends TableCell<Eleve, String> {
		private final TextField tf = new TextField();
		private final int matiereId;
		private final boolean isValidated;

		NoteCell(int matiereId, boolean isValidated) {
			this.matiereId = matiereId;
			this.isValidated = isValidated;
			tf.setAlignment(Pos.CENTER);
			tf.setEditable(!isValidated);
			tf.focusedProperty().addListener((o, a, b) -> {
				if (!b && !isValidated)
					save();
			});
			tf.setOnKeyPressed(k -> {
				if (!isValidated)
					navigate(k);
			});
		}

		private void save() {
			if (getIndex() < 0)
				return;
			String text = tf.getText().trim();
			if (text.isEmpty())
				return;
			try {
				double v = Double.parseDouble(text);
				if (v < 0 || v > 20) {
					alert("La note doit être comprise entre 0 et 20.");
					return;
				}
				Eleve e = getTableView().getItems().get(getIndex());
				currentNotesMap.put(e.getId() + "_" + matiereId, v);
				new NoteDAO().saveOrUpdate(e.getId(), matiereId, currentClasse.getId(), currentTrimestreId, v, user.getId());
			} catch (NumberFormatException nfe) {
				alert("Veuillez saisir un nombre valide (ex: 15.5).");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		private void navigate(KeyEvent k) {
			int r = getIndex();
			int c = getTableView().getColumns().indexOf(getTableColumn());
			int maxR = getTableView().getItems().size() - 1;
			int maxC = getTableView().getColumns().size() - 1;
			if (k.getCode() == KeyCode.ENTER || k.getCode() == KeyCode.DOWN) {
				if (r < maxR) move(r + 1, c);
			} else if (k.getCode() == KeyCode.UP) {
				if (r > 0) move(r - 1, c);
			} else if (k.getCode() == KeyCode.RIGHT || k.getCode() == KeyCode.TAB) {
				if (c < maxC) move(r, c + 1);
			} else if (k.getCode() == KeyCode.LEFT) {
				if (c > 1) move(r, c - 1);
			}
		}

		private void move(int r, int c) {
			TableView<Eleve> tv = getTableView();
			tv.getSelectionModel().clearAndSelect(r, tv.getColumns().get(c));
			tv.scrollTo(r);
			Platform.runLater(() -> {
				for (var n : tv.lookupAll(".table-cell")) {
					if (n instanceof TableCell<?, ?> cell) {
						if (cell.getIndex() == r && tv.getColumns().indexOf(cell.getTableColumn()) == c) {
							for (var ch : ((Parent) cell).getChildrenUnmodifiable()) {
								if (ch instanceof TextField t) {
									t.requestFocus();
									t.selectAll();
								}
							}
						}
					}
				}
			});
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || getIndex() < 0) {
				setGraphic(null);
				return;
			}
			Eleve e = getTableView().getItems().get(getIndex());
			Double v = currentNotesMap.get(e.getId() + "_" + matiereId);
			tf.setText(v == null ? "" : v.toString());
			tf.setStyle(isValidated ? "-fx-control-inner-background: #F0F0F0;" : "");
			setGraphic(tf);
		}
	}

	private void showAccountSettingsDialog() {
		Stage dlg = new Stage();
		dlg.setTitle("Paramètres du compte");
		dlg.initOwner(stage);

		VBox form = new VBox(15);
		form.setPadding(new Insets(24));
		form.setStyle("-fx-background-color: white;");

		TextField fLogin = new TextField(user.getLogin());
		fLogin.setPromptText("Nouveau Login");
		fLogin.setPrefHeight(36);

		PasswordField fPass = new PasswordField();
		fPass.setPromptText("Nouveau Mot de passe");
		fPass.setPrefHeight(36);

		Button btnSave = new Button("Enregistrer");
		btnSave.setStyle("-fx-background-color: " + BLUE + "; -fx-text-fill: white; -fx-padding: 8 20; -fx-cursor: hand;");

		btnSave.setOnAction(e -> {
			if (fLogin.getText().isBlank() || fPass.getText().isBlank()) {
				alert("Veuillez remplir tous les champs.");
				return;
			}
			if (new UserDAO().update(user.getId(), fLogin.getText().trim(), fPass.getText())) {
				user.setLogin(fLogin.getText().trim());
				alert("✓ Paramètres mis à jour avec succès.");
				dlg.close();
			} else {
				alert("❌ Erreur lors de la mise à jour.");
			}
		});

		fLogin.setOnKeyPressed(k -> { if(k.getCode() == KeyCode.ENTER) fPass.requestFocus(); });
		fPass.setOnKeyPressed(k -> { if(k.getCode() == KeyCode.ENTER) btnSave.fire(); });
		form.addEventHandler(KeyEvent.KEY_PRESSED, k -> { if(k.getCode() == KeyCode.ESCAPE) dlg.close(); });

		form.getChildren().addAll(
			new Label("Modifier vos accès") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 16px;"); }},
			new VBox(5, new Label("Login"), fLogin),
			new VBox(5, new Label("Mot de passe"), fPass),
			btnSave
		);

		dlg.setScene(new Scene(form));
		dlg.show();
	}

	private void alert(String msg) {
		new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
	}

	private void centerStage(Stage s) {
		var r = Screen.getPrimary().getVisualBounds();
		s.setX((r.getWidth() - s.getWidth()) / 2);
		s.setY((r.getHeight() - s.getHeight()) / 2);
	}
}