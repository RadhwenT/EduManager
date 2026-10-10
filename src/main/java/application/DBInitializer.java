package application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBInitializer {

    public static void checkAndInit() {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            createSchemaIfMissing(conn);
            widenPasswordColumn(conn);
            createAppSettings(conn);
            createSequences(conn);
            updateEleveTable(conn);
            addDirecteurRole(conn);
            createDirecteurAccount(conn);
            updatePaymentTable(conn);
            createVersementTable(conn);
            conn.commit();
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'initialisation de la base : " + e.getMessage());
        }
    }

    private static void createAppSettings(Connection conn) throws SQLException {
        if (!tableExists(conn, "APP_SETTINGS")) {
            exec(conn, "CREATE TABLE app_settings (" +
                    "cle VARCHAR2(50) PRIMARY KEY, " +
                    "valeur VARCHAR2(10) DEFAULT 'true')");
            exec(conn, "INSERT INTO app_settings VALUES ('inscription_active', 'true')");
            exec(conn, "INSERT INTO app_settings VALUES ('affectation_active', 'true')");
            System.out.println("✅ Table app_settings créée.");
        }
    }

    private static void createSequences(Connection conn) throws SQLException {
        if (!sequenceExists(conn, "SEQ_ANNEE")) {
            exec(conn, "CREATE SEQUENCE seq_annee START WITH 10 INCREMENT BY 1");
            System.out.println("✅ Séquence seq_annee créée.");
        }
        if (!sequenceExists(conn, "SEQ_TRIMESTRE")) {
            exec(conn, "CREATE SEQUENCE seq_trimestre START WITH 10 INCREMENT BY 1");
            System.out.println("✅ Séquence seq_trimestre créée.");
        }
        if (!sequenceExists(conn, "SEQ_USER")) {
            exec(conn, "CREATE SEQUENCE seq_user START WITH 10 INCREMENT BY 1");
            System.out.println("✅ Séquence seq_user créée.");
        }
        if (!sequenceExists(conn, "SEQ_PAIEMENT")) {
            exec(conn, "CREATE SEQUENCE seq_paiement START WITH 1 INCREMENT BY 1");
            System.out.println("✅ Séquence seq_paiement créée.");
        }
    }

    /**
     * Première exécution sur une base vide : crée toutes les tables à partir de
     * src/main/resources/db/schema.sql (avec quelques données de départ).
     */
    private static void createSchemaIfMissing(Connection conn) throws SQLException {
        if (tableExists(conn, "USER_ACCOUNT")) return;
        System.out.println("ℹ️ Base vide détectée : création du schéma EduManager...");
        String script;
        try (InputStream in = DBInitializer.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) throw new SQLException("Script db/schema.sql introuvable dans les ressources.");
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Lecture de db/schema.sql impossible : " + e.getMessage(), e);
        }
        StringBuilder withoutComments = new StringBuilder();
        for (String line : script.split("\\R")) {
            if (!line.trim().startsWith("--")) withoutComments.append(line).append('\n');
        }
        for (String statement : withoutComments.toString().split(";\\s*\\n")) {
            String sql = statement.trim();
            if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();
            if (!sql.isEmpty()) exec(conn, sql);
        }
        conn.commit();
        System.out.println("✅ Schéma créé.");
    }

    /** Les mots de passe hachés font ~90 caractères : on élargit la colonne si besoin. */
    private static void widenPasswordColumn(Connection conn) throws SQLException {
        String sql = "SELECT char_length FROM user_tab_columns WHERE table_name = 'USER_ACCOUNT' AND column_name = 'PASSWORD'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) < 255) {
                exec(conn, "ALTER TABLE user_account MODIFY password VARCHAR2(255)");
                System.out.println("✅ Colonne password élargie à 255 caractères.");
            }
        }
    }

    private static void addDirecteurRole(Connection conn) throws SQLException {
        String checkSql = "SELECT search_condition FROM user_constraints " +
                "WHERE table_name = 'USER_ACCOUNT' AND constraint_type = 'C'";
        boolean hasDirecteur = false;
        try (PreparedStatement ps = conn.prepareStatement(checkSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String condition = rs.getString("search_condition");
                if (condition != null && condition.toUpperCase().contains("DIRECTEUR")) {
                    hasDirecteur = true;
                    break;
                }
            }
        }
        if (!hasDirecteur) {
            try {
                exec(conn, "ALTER TABLE user_account DROP CONSTRAINT chk_user_role");
            } catch (SQLException ignored) {}
            exec(conn, "ALTER TABLE user_account ADD CONSTRAINT chk_user_role " +
                    "CHECK (role IN ('ADMIN','ENSEIGNANT','FINANCIER','DIRECTEUR'))");
            System.out.println("✅ Rôle DIRECTEUR ajouté à la contrainte.");
        }
    }

    private static void createDirecteurAccount(Connection conn) throws SQLException {
        String check = "SELECT COUNT(*) FROM user_account WHERE role = 'DIRECTEUR'";
        try (PreparedStatement ps = conn.prepareStatement(check);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO user_account (id, login, password, role) " +
                        "VALUES (seq_user.NEXTVAL, 'directeur', ?, 'DIRECTEUR')";
                try (PreparedStatement ins = conn.prepareStatement(insert)) {
                    ins.setString(1, PasswordUtil.hash("dir2024"));
                    ins.executeUpdate();
                }
                System.out.println("✅ Compte directeur créé (login: directeur / mdp: dir2024) — changez-le après la première connexion.");
            }
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_tables WHERE table_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName.toUpperCase());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static boolean sequenceExists(Connection conn, String seqName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_sequences WHERE sequence_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seqName.toUpperCase());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static void updateEleveTable(Connection conn) throws SQLException {
        if (!columnExists(conn, "ELEVE", "MATRICULE")) {
            try {
                exec(conn, "ALTER TABLE eleve ADD matricule VARCHAR2(20)");
                System.out.println("✅ Colonne matricule ajoutée à la table eleve.");
            } catch (SQLException e) {
                System.err.println("⚠️ Erreur lors de l'ajout de la colonne matricule : " + e.getMessage());
            }
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_tab_columns WHERE table_name = ? AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName.toUpperCase());
            ps.setString(2, columnName.toUpperCase());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static void updatePaymentTable(Connection conn) throws SQLException {
        if (!columnExists(conn, "PAIEMENT", "STATUT_VERSEMENT")) {
            try {
                exec(conn, "ALTER TABLE paiement ADD statut_versement VARCHAR2(20) DEFAULT 'EN_CAISSE'");
            } catch (SQLException ignored) {}
        }
        if (!columnExists(conn, "PAIEMENT", "VERSEMENT_ID")) {
            try {
                exec(conn, "ALTER TABLE paiement ADD versement_id NUMBER");
            } catch (SQLException ignored) {}
        }
    }

    private static void createVersementTable(Connection conn) throws SQLException {
        if (!tableExists(conn, "VERSEMENT")) {
            exec(conn, "CREATE TABLE versement (" +
                    "id NUMBER PRIMARY KEY, " +
                    "annee_id NUMBER, " +
                    "type_versement VARCHAR2(20), " +
                    "montant NUMBER(12,2), " +
                    "banque VARCHAR2(100), " +
                    "num_bon VARCHAR2(50), " +
                    "date_versement DATE DEFAULT SYSDATE)");
            exec(conn, "CREATE SEQUENCE seq_versement START WITH 1 INCREMENT BY 1");
            System.out.println("✅ Table versement créée.");
        } else {
            if (!columnExists(conn, "VERSEMENT", "TYPE_VERSEMENT")) {
                exec(conn, "ALTER TABLE versement ADD type_versement VARCHAR2(20)");
            }
            if (!columnExists(conn, "VERSEMENT", "BANQUE")) {
                exec(conn, "ALTER TABLE versement ADD banque VARCHAR2(100)");
            }
            if (!columnExists(conn, "VERSEMENT", "NUM_BON")) {
                exec(conn, "ALTER TABLE versement ADD num_bon VARCHAR2(50)");
            }
            if (!columnExists(conn, "VERSEMENT", "DATE_VERSEMENT")) {
                exec(conn, "ALTER TABLE versement ADD date_versement DATE DEFAULT SYSDATE");
            }
        }
        if (!sequenceExists(conn, "SEQ_VERSEMENT")) {
            exec(conn, "CREATE SEQUENCE seq_versement START WITH 1 INCREMENT BY 1");
        }
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }
}
