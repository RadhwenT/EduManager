package io.github.radhwent.edumanager.dao;

import io.github.radhwent.edumanager.db.DBConnection;
import io.github.radhwent.edumanager.model.Eleve;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EleveDAO {

    public List<Eleve> findAll() {
        List<Eleve> list = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, TO_CHAR(date_naissance, 'YYYY-MM-DD') as date_naissance, adresse, telephone, matricule FROM eleve ORDER BY nom, prenom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Eleve> findElevesByClasse(int classeId) {
        List<Eleve> list = new ArrayList<>();
        String sql = """
            SELECT e.id, e.nom, e.prenom, TO_CHAR(e.date_naissance, 'YYYY-MM-DD') as date_naissance, e.adresse, e.telephone, e.matricule
            FROM eleve e
            JOIN affectation_eleve a ON a.eleve_id = e.id
            WHERE a.classe_id = ?
            AND a.date_fin IS NULL
            ORDER BY e.nom, e.prenom
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, classeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Eleve(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("date_naissance"),
                    rs.getString("adresse"),
                    rs.getString("telephone")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Eleve> findNonInscrits(int anneeId) {
        List<Eleve> list = new ArrayList<>();
        String sql = "SELECT e.id, e.nom, e.prenom, e.date_naissance, e.adresse, e.telephone, e.matricule " +
                     "FROM eleve e " +
                     "WHERE e.id NOT IN (SELECT i.eleve_id FROM inscription i WHERE i.annee_id = ?) " +
                     "ORDER BY e.nom, e.prenom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Eleve> findInscritsNonAffectes(int anneeId, int niveauId) {
        List<Eleve> list = new ArrayList<>();
        String sql = "SELECT e.id, e.nom, e.prenom, e.date_naissance, e.adresse, e.telephone, e.matricule " +
                     "FROM eleve e " +
                     "JOIN inscription i ON i.eleve_id = e.id " +
                     "JOIN niveau n ON n.id = i.niveau_id " +
                     "WHERE i.annee_id = ? AND n.id = ? " +
                     "AND e.id NOT IN ( " +
                     "  SELECT a.eleve_id FROM affectation_eleve a " +
                     "  JOIN classe cl ON cl.id = a.classe_id " +
                     "  WHERE cl.annee_id = ? AND cl.niveau_id = ? " +
                     ") ORDER BY e.nom, e.prenom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId); ps.setInt(2, niveauId);
            ps.setInt(3, anneeId); ps.setInt(4, niveauId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Eleve> findAffectesDansClasse(int classeId) {
        List<Eleve> list = new ArrayList<>();
        String sql = "SELECT e.id, e.nom, e.prenom, e.date_naissance, e.adresse, e.telephone, e.matricule " +
                     "FROM eleve e JOIN affectation_eleve a ON a.eleve_id = e.id " +
                     "WHERE a.classe_id = ? ORDER BY e.nom, e.prenom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, classeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public String genererMatricule(String libelleAnnee, String libelleNiveau) {
        String prefix = libelleAnnee.replace("/", "-") + "-" + libelleNiveau.replaceAll("\\s+", "") + "-";
        String sql = "SELECT COUNT(*) FROM inscription i " +
                     "JOIN annee_scolaire a ON a.id = i.annee_id " +
                     "JOIN niveau n ON n.id = i.niveau_id " +
                     "WHERE a.libelle = ? AND n.libelle = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, libelleAnnee); ps.setString(2, libelleNiveau);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return prefix + String.format("%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { e.printStackTrace(); }
        return prefix + "001";
    }

    public String genererMatriculeEleve() {
        String sql = "SELECT COUNT(*) FROM eleve";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return "EL" + String.format("%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { e.printStackTrace(); }
        return "EL001";
    }

    public boolean create(Eleve e) {
        String sql = "INSERT INTO eleve (nom, prenom, date_naissance, adresse, telephone, matricule) VALUES (?,?,TO_DATE(?, 'YYYY-MM-DD'),?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, e.getNom()); ps.setString(2, e.getPrenom());
            ps.setString(3, e.getDateNaissance()); ps.setString(4, e.getAdresse());
            ps.setString(5, e.getTelephone()); ps.setString(6, e.getMatricule());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean update(Eleve e) {
        String sql = "UPDATE eleve SET nom=?, prenom=?, date_naissance=TO_DATE(?, 'YYYY-MM-DD'), adresse=?, telephone=?, matricule=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, e.getNom()); ps.setString(2, e.getPrenom());
            ps.setString(3, e.getDateNaissance()); ps.setString(4, e.getAdresse());
            ps.setString(5, e.getTelephone()); ps.setString(6, e.getMatricule()); ps.setInt(7, e.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    public boolean delete(int id) {
        String sqlNotes = "DELETE FROM note WHERE eleve_id = ?";
        String sqlAff = "DELETE FROM affectation_eleve WHERE eleve_id = ?";
        String sqlIns = "DELETE FROM inscription WHERE eleve_id = ?";
        String sqlEleve = "DELETE FROM eleve WHERE id = ?";
        
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sqlNotes)) { ps.setInt(1, id); ps.executeUpdate(); }
                try (PreparedStatement ps = c.prepareStatement(sqlAff)) { ps.setInt(1, id); ps.executeUpdate(); }
                try (PreparedStatement ps = c.prepareStatement(sqlIns)) { ps.setInt(1, id); ps.executeUpdate(); }
                try (PreparedStatement ps = c.prepareStatement(sqlEleve)) { 
                    ps.setInt(1, id); 
                    int affected = ps.executeUpdate();
                    c.commit();
                    return affected > 0;
                }
            } catch (SQLException ex) { c.rollback(); throw ex; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean inscrire(int eleveId, int anneeId, int niveauId, String modePaiement) {
        String sql = "INSERT INTO inscription (eleve_id, annee_id, niveau_id, mode_paiement) VALUES (?,?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, eleveId); ps.setInt(2, anneeId);
            ps.setInt(3, niveauId); ps.setString(4, modePaiement);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean affecter(int eleveId, int classeId) {
        String sql = "INSERT INTO affectation_eleve (eleve_id, classe_id, date_affectation) VALUES (?, ?, SYSDATE)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, eleveId); ps.setInt(2, classeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean reaffecter(int eleveId, int nouvelleClasseId) {
        String sql = "UPDATE affectation_eleve SET classe_id = ?, date_affectation = SYSDATE WHERE eleve_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, nouvelleClasseId); ps.setInt(2, eleveId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean permuter(int eleveId1, int eleveId2) {
        String getClasse = "SELECT classe_id FROM affectation_eleve WHERE eleve_id = ?";
        String update    = "UPDATE affectation_eleve SET classe_id = ? WHERE eleve_id = ?";
        try {
            Connection c = DBConnection.getConnection();
            int c1 = -1, c2 = -1;
            try (PreparedStatement ps = c.prepareStatement(getClasse)) {
                ps.setInt(1, eleveId1); ResultSet rs = ps.executeQuery();
                if (rs.next()) c1 = rs.getInt(1);
                ps.setInt(1, eleveId2); rs = ps.executeQuery();
                if (rs.next()) c2 = rs.getInt(1);
            }
            if (c1 == -1 || c2 == -1) return false;
            try (PreparedStatement ps = c.prepareStatement(update)) {
                ps.setInt(1, c2); ps.setInt(2, eleveId1); ps.executeUpdate();
                ps.setInt(1, c1); ps.setInt(2, eleveId2); ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public int countAffectesClasse(int classeId) {
        String sql = "SELECT COUNT(*) FROM affectation_eleve WHERE classe_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, classeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private Eleve map(ResultSet rs) throws SQLException {
        Eleve e = new Eleve(
            rs.getInt("id"), rs.getString("nom"), rs.getString("prenom"),
            rs.getString("date_naissance"), rs.getString("adresse"), rs.getString("telephone")
        );
        e.setMatricule(rs.getString("matricule"));
        return e;
    }
    
    public String findClasseActive(int eleveId, int anneeId) {
        String sql = """
            SELECT c.nom
            FROM affectation_eleve a
            JOIN classe c ON c.id = a.classe_id
            WHERE a.eleve_id = ?
            AND a.date_fin IS NULL
            AND c.annee_id = ?
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, eleveId);
            ps.setInt(2, anneeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nom");
        } catch (SQLException e) { e.printStackTrace(); }
        return "-";
    }

    public List<InscriptionInfo> findInscriptionsByAnnee(int anneeId) {
        List<InscriptionInfo> list = new ArrayList<>();
        String sql = """
            SELECT e.id as eleve_id, e.nom, e.prenom, e.matricule, n.libelle as niveau, 
                   (SELECT c.nom FROM affectation_eleve a JOIN classe c ON c.id = a.classe_id 
                    WHERE a.eleve_id = e.id AND c.annee_id = ? AND a.date_fin IS NULL) as classe,
                   i.mode_paiement
            FROM inscription i
            JOIN eleve e ON e.id = i.eleve_id
            JOIN niveau n ON n.id = i.niveau_id
            WHERE i.annee_id = ?
            ORDER BY e.nom, e.prenom
        """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId);
            ps.setInt(2, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new InscriptionInfo(
                    rs.getInt("eleve_id"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("matricule"),
                    rs.getString("niveau"),
                    rs.getString("classe") == null ? "Non affecté" : rs.getString("classe"),
                    rs.getString("mode_paiement")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean annulerInscription(int eleveId, int anneeId) {
        String sqlDelAff = "DELETE FROM affectation_eleve WHERE eleve_id = ? AND classe_id IN (SELECT id FROM classe WHERE annee_id = ?)";
        String sqlDelIns = "DELETE FROM inscription WHERE eleve_id = ? AND annee_id = ?";
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = c.prepareStatement(sqlDelAff)) {
                    ps1.setInt(1, eleveId); ps1.setInt(2, anneeId); ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = c.prepareStatement(sqlDelIns)) {
                    ps2.setInt(1, eleveId); ps2.setInt(2, anneeId); 
                    int affected = ps2.executeUpdate();
                    c.commit();
                    return affected > 0;
                }
            } catch (SQLException ex) { c.rollback(); throw ex; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static class InscriptionInfo {
        private int eleveId;
        private String nom, prenom, matricule, niveau, classe, modePaiement;
        public InscriptionInfo(int eleveId, String nom, String prenom, String matricule, String niveau, String classe, String modePaiement) {
            this.eleveId = eleveId; this.nom = nom; this.prenom = prenom; this.matricule = matricule;
            this.niveau = niveau; this.classe = classe; this.modePaiement = modePaiement;
        }
        public int getEleveId() { return eleveId; }
        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
        public String getMatricule() { return matricule; }
        public String getNiveau() { return niveau; }
        public String getClasse() { return classe; }
        public String getModePaiement() { return modePaiement; }
    }

    public List<Integer> findInscritsIds(int anneeId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT eleve_id FROM inscription WHERE annee_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) { e.printStackTrace(); }
        return ids;
    }
}