package io.github.radhwent.edumanager.view;

import io.github.radhwent.edumanager.dao.UserDAO;
import io.github.radhwent.edumanager.model.User;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class LoginView {

	private Stage stage;

	public LoginView(Stage stage) {
		this.stage = stage;
	}

	public void show() {

		VBox leftPanel = new VBox();
		leftPanel.setPrefWidth(8);
		leftPanel.setStyle("-fx-background-color: #2563EB;");

		Label appName = new Label("EduManager");
		appName.setStyle("-fx-font-size: 11px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #2563EB;"
				+ "-fx-letter-spacing: 2px;");

		Label titleLabel = new Label("Connexion");
		titleLabel.setStyle("-fx-font-size: 26px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #1E293B;");

		Label subtitleLabel = new Label("Bienvenue. Veuillez vous identifier.");
		subtitleLabel.setStyle("-fx-font-size: 12px;" + "-fx-text-fill: #64748B;");

		VBox headerBox = new VBox(4, appName, titleLabel, subtitleLabel);
		headerBox.setPadding(new Insets(0, 0, 24, 0));

		Label loginLabel = new Label("Identifiant");
		loginLabel.setStyle("-fx-font-size: 11px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #475569;");

		TextField loginField = new TextField();
		loginField.setPromptText("Entrez votre login");
		loginField.setPrefHeight(38);
		loginField.setStyle("-fx-background-color: #F8FAFC;" + "-fx-border-color: #CBD5E1;" + "-fx-border-radius: 6px;"
				+ "-fx-background-radius: 6px;" + "-fx-padding: 6 12 6 12;" + "-fx-font-size: 13px;"
				+ "-fx-text-fill: #1E293B;");
		loginField.focusedProperty().addListener((o, old, focused) -> {
			if (focused) {
				loginField.setStyle(loginField.getStyle().replace("-fx-border-color: #CBD5E1;",
						"-fx-border-color: #2563EB; -fx-border-width: 1.5px;"));
			} else {
				loginField.setStyle(loginField.getStyle().replace("-fx-border-color: #2563EB; -fx-border-width: 1.5px;",
						"-fx-border-color: #CBD5E1;"));
			}
		});

		Label passwordLabel = new Label("Mot de passe");
		passwordLabel.setStyle("-fx-font-size: 11px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #475569;");

		PasswordField passwordField = new PasswordField();
		passwordField.setPromptText("••••••••");
		passwordField.setPrefHeight(38);
		passwordField.setStyle("-fx-background-color: #F8FAFC;" + "-fx-border-color: #CBD5E1;"
				+ "-fx-border-radius: 6px;" + "-fx-background-radius: 6px;" + "-fx-padding: 6 12 6 12;"
				+ "-fx-font-size: 13px;" + "-fx-text-fill: #1E293B;");
		passwordField.focusedProperty().addListener((o, old, focused) -> {
			if (focused) {
				passwordField.setStyle(passwordField.getStyle().replace("-fx-border-color: #CBD5E1;",
						"-fx-border-color: #2563EB; -fx-border-width: 1.5px;"));
			} else {
				passwordField.setStyle(passwordField.getStyle()
						.replace("-fx-border-color: #2563EB; -fx-border-width: 1.5px;", "-fx-border-color: #CBD5E1;"));
			}
		});

		Label messageLabel = new Label();
		messageLabel.setStyle("-fx-text-fill: #DC2626;" + "-fx-font-size: 11px;" + "-fx-padding: 0;");
		messageLabel.setWrapText(true);

		Button loginButton = new Button("Se connecter  →");
		loginButton.setPrefHeight(40);
		loginButton.setMaxWidth(Double.MAX_VALUE);
		loginButton.setStyle("-fx-background-color: #2563EB;" + "-fx-text-fill: white;" + "-fx-font-size: 13px;"
				+ "-fx-font-weight: bold;" + "-fx-background-radius: 6px;" + "-fx-cursor: hand;");
		loginButton.setOnMouseEntered(e -> loginButton
				.setStyle("-fx-background-color: #1D4ED8;" + "-fx-text-fill: white;" + "-fx-font-size: 13px;"
						+ "-fx-font-weight: bold;" + "-fx-background-radius: 6px;" + "-fx-cursor: hand;"));
		loginButton.setOnMouseExited(e -> loginButton
				.setStyle("-fx-background-color: #2563EB;" + "-fx-text-fill: white;" + "-fx-font-size: 13px;"
						+ "-fx-font-weight: bold;" + "-fx-background-radius: 6px;" + "-fx-cursor: hand;"));

		VBox loginGroup = new VBox(5, loginLabel, loginField);
		VBox passwordGroup = new VBox(5, passwordLabel, passwordField);

		VBox form = new VBox(16, headerBox, loginGroup, passwordGroup, messageLabel, loginButton);
		form.setPadding(new Insets(40, 40, 40, 40));
		form.setAlignment(Pos.CENTER_LEFT);
		form.setStyle("-fx-background-color: white;");
		VBox.setVgrow(form, Priority.ALWAYS);

		HBox root = new HBox(leftPanel, form);
		root.setStyle("-fx-background-color: white;");
		HBox.setHgrow(form, Priority.ALWAYS);

		loginButton.setOnAction(e -> {
			String login = loginField.getText().trim();
			String password = passwordField.getText();

			if (login.isEmpty() || password.isEmpty()) {
				messageLabel.setText("⚠  Veuillez remplir tous les champs.");
				return;
			}

			messageLabel.setText("");

			UserDAO userDAO = new UserDAO();
			User user = userDAO.authenticate(login, password);

			if (user == null) {
				messageLabel.setText("✕  Login ou mot de passe incorrect.");
				passwordField.clear();
			} else {
				redirectUser(user);
			}
		});

		passwordField.setOnAction(e -> loginButton.fire());
		loginField.setOnAction(e -> passwordField.requestFocus());

		Scene scene = new Scene(root, 550, 420);
		stage.setTitle("Connexion — EduManager");
		stage.setScene(scene);
		stage.setResizable(false);
		stage.show();

		javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
		stage.setX((screen.getWidth() - stage.getWidth()) / 2);
		stage.setY((screen.getHeight() - stage.getHeight()) / 2);
	}

	private void redirectUser(User user) {
		if (user.getRole().equals("ADMIN")) {
			new AdminView(stage, user).show();
		} else if (user.getRole().equals("ENSEIGNANT")) {
			new EnseignantView(stage, user).show();
		} else if (user.getRole().equals("FINANCIER")) {
			new FinancierView(stage, user).show();
		} else if (user.getRole().equals("DIRECTEUR")) {
			new DirecteurView(stage, user).show();
		}
	}
}