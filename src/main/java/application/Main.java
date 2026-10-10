package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage stage) {
		if (DBConnection.getConnection() == null) {
			showConnectionError();
			Platform.exit();
			return;
		}
		DBInitializer.checkAndInit();
		new LoginView(stage).show();
	}

	private void showConnectionError() {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("EduManager — Connexion impossible");
		alert.setHeaderText("Impossible de se connecter à la base de données Oracle.");
		TextArea details = new TextArea(
				"Vérifiez que :\n"
				+ "  1. Oracle est démarré (ou le conteneur Docker : docker compose up -d)\n"
				+ "  2. le fichier db.properties existe à la racine du projet\n"
				+ "     (copie de db.properties.example avec vos identifiants)\n\n"
				+ "Détail : " + DBConnection.getLastError());
		details.setEditable(false);
		details.setWrapText(true);
		details.setPrefRowCount(7);
		alert.getDialogPane().setContent(details);
		alert.showAndWait();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
