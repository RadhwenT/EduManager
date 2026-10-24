package application;

import org.testcontainers.containers.OracleContainer;

/**
 * Démarre une seule base Oracle jetable (Docker) pour tous les tests d'intégration,
 * puis initialise le schéma exactement comme l'application au démarrage.
 * DBConnection lit ses identifiants dans les propriétés système : aucun changement
 * du code de l'application n'est nécessaire.
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
    }
}
