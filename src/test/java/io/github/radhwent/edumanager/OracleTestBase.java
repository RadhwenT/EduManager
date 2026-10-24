package io.github.radhwent.edumanager;

import io.github.radhwent.edumanager.db.DBConnection;
import io.github.radhwent.edumanager.db.DBInitializer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;

import org.testcontainers.containers.OracleContainer;

/**
 * Démarre une seule base Oracle jetable (Docker) pour tous les tests d'intégration,
 * puis initialise le schéma exactement comme l'application au démarrage.
 * DBConnection lit ses identifiants dans les propriétés système : aucun changement
 * du code de l'application n'est nécessaire.
 * Les données de démo (scripts/demo-data.sql) sont ensuite chargées, pour vérifier
 * le script lui-même et faire tourner les tests sur une base réaliste.
 */
abstract class OracleTestBase {

    static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withUsername("edumanager")
            .withPassword("edumanager");

    static {
        ORACLE.start();
        System.setProperty("db.url", ORACLE.getJdbcUrl());
        System.setProperty("db.user", ORACLE.getUsername());
        System.setProperty("db.password", ORACLE.getPassword());
        DBInitializer.checkAndInit();
        chargerDonneesDemo();
    }

    /** Exécute scripts/demo-data.sql via JDBC (mêmes instructions que dans SQL*Plus). */
    static void chargerDonneesDemo() {
        try {
            String script = Files.readString(Path.of("scripts/demo-data.sql"), StandardCharsets.UTF_8);
            StringBuilder sansCommentaires = new StringBuilder();
            for (String ligne : script.split("\\R")) {
                if (!ligne.trim().startsWith("--")) sansCommentaires.append(ligne).append('\n');
            }
            try (Statement st = DBConnection.getConnection().createStatement()) {
                for (String sql : sansCommentaires.toString().split(";\\s*\n")) {
                    String instr = sql.trim();
                    if (instr.isEmpty() || instr.equalsIgnoreCase("COMMIT") || instr.equalsIgnoreCase("EXIT")) continue;
                    st.execute(instr);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Échec du script de démo : " + e.getMessage(), e);
        }
    }
}
