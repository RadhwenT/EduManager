package io.github.radhwent.edumanager;

/**
 * Point d'entrée à lancer depuis VS Code (bouton « Run » au-dessus de main).
 *
 * Cette classe n'hérite pas de javafx.application.Application : cela permet de démarrer
 * l'application avec JavaFX sur le classpath, sans configurer de --module-path.
 */
public class Launcher {

	public static void main(String[] args) {
		Main.main(args);
	}
}
