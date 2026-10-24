package io.github.radhwent.edumanager.dao;

import io.github.radhwent.edumanager.db.DBConnection;
import io.github.radhwent.edumanager.model.Enseignant;
import io.github.radhwent.edumanager.security.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnseignantDAO {

    public List<Enseignant> findAll() {
        List<Enseignant> list = new ArrayList<>();
        String sql = "SELECT e.id, e.code, e.nom, e.prenom, e.telephone, e.user_id, u.login " +
                     "FROM enseignant e JOIN user_account u ON u.id = e.user_id ORDER BY e.nom, e.prenom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean create(Enseignant ens, String password) {
        String sqlUser = "INSERT INTO user_account (login, password, role) VALUES (?, ?, 'ENSEIGNANT')";
        String sqlEns  = "INSERT INTO enseignant (code, nom, prenom, telephone, user_id) VALUES (?,?,?,?,?)";
        try {
            Connection c = DBConnection.getConnection();
            int userId = -1;
            try (PreparedStatement ps = c.prepareStatement(sqlUser, new String[]{"id"})) {
                ps.setString(1, ens.getLogin()); ps.setString(2, PasswordUtil.hash(password));
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) userId = keys.getInt(1);
            }
            if (userId == -1) {
                try (PreparedStatement ps = c.prepareStatement("SELECT id FROM user_account WHERE login = ?")) {
                    ps.setString(1, ens.getLogin());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) userId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(sqlEns)) {
                ps.setString(1, ens.getCode()); ps.setString(2, ens.getNom());
                ps.setString(3, ens.getPrenom()); ps.setString(4, ens.getTelephone());
                ps.setInt(5, userId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean update(Enseignant ens) {
        String sql = "UPDATE enseignant SET code=?, nom=?, prenom=?, telephone=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, ens.getCode()); ps.setString(2, ens.getNom());
            ps.setString(3, ens.getPrenom()); ps.setString(4, ens.getTelephone());
            ps.setInt(5, ens.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean delete(Enseignant ens) {
        try {
            Connection c = DBConnection.getConnection();
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM enseignant_classe_matiere WHERE enseignant_id = ?")) {
                ps.setInt(1, ens.getId()); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM enseignant WHERE id = ?")) {
                ps.setInt(1, ens.getId()); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM user_account WHERE id = ?")) {
                ps.setInt(1, ens.getUserId()); ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean affecter(int enseignantId, int classeId, List<Integer> matiereIds, int anneeId) {
        String delSpecific = "DELETE FROM enseignant_classe_matiere WHERE classe_id=? AND matiere_id=? AND annee_id=?";
        String ins = "INSERT INTO enseignant_classe_matiere (enseignant_id, classe_id, matiere_id, annee_id) VALUES (?,?,?,?)";
        try {
            Connection c = DBConnection.getConnection();
            c.setAutoCommit(false);
            try (PreparedStatement psDel = c.prepareStatement(delSpecific);
                 PreparedStatement psIns = c.prepareStatement(ins)) {
                
                for (int mId : matiereIds) {
                    
                    psDel.setInt(1, classeId); psDel.setInt(2, mId); psDel.setInt(3, anneeId);
                    psDel.executeUpdate();

                    
                    psIns.setInt(1, enseignantId); psIns.setInt(2, classeId);
                    psIns.setInt(3, mId); psIns.setInt(4, anneeId);
                    psIns.addBatch();
                }
                psIns.executeBatch();
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                e.printStackTrace();
                return false;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public String genererLogin(String prenom, String nom) {
        String base = (prenom.charAt(0) + nom).toLowerCase().replaceAll("\\s+", "");
        String login = base;
        int i = 1;
        while (loginExiste(login)) {
            i++;
            login = base + i;
        }
        return login;
    }

    private boolean loginExiste(String login) {
        String sql = "SELECT COUNT(*) FROM user_account WHERE login = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); return true; }
    }

    public String genererCode() {
        String sql = "SELECT COUNT(*) FROM enseignant";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return "ENS" + String.format("%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { e.printStackTrace(); }
        return "ENS001";
    }

    public List<Integer> getMatiereAffecter(int enseignantId, int classeId, int anneeId) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT matiere_id FROM enseignant_classe_matiere WHERE enseignant_id = ? AND classe_id = ? AND annee_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, enseignantId); ps.setInt(2, classeId); ps.setInt(3, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getInt("matiere_id"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public String checkConflicts(int classeId, List<Integer> matiereIds, int anneeId, int currentTeacherId) {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT e.nom, e.prenom, m.nom as mat_nom " +
                     "FROM enseignant_classe_matiere ecm " +
                     "JOIN enseignant e ON e.id = ecm.enseignant_id " +
                     "JOIN matiere m ON m.id = ecm.matiere_id " +
                     "WHERE ecm.classe_id = ? AND ecm.matiere_id = ? AND ecm.annee_id = ? AND ecm.enseignant_id != ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            for (int mId : matiereIds) {
                ps.setInt(1, classeId); ps.setInt(2, mId); ps.setInt(3, anneeId); ps.setInt(4, currentTeacherId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("- ").append(rs.getString("mat_nom")).append(" est déjà affectée à ").append(rs.getString("nom")).append(" ").append(rs.getString("prenom"));
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return sb.length() == 0 ? null : sb.toString();
    }

    private Enseignant map(ResultSet rs) throws SQLException {
        return new Enseignant(
            rs.getInt("id"), rs.getString("code"), rs.getString("nom"),
            rs.getString("prenom"), rs.getString("telephone"),
            rs.getInt("user_id"), rs.getString("login")
        );
    }

    public List<TeacherAssignmentInfo> getTeacherAssignments(int teacherId, int anneeId) {
        List<TeacherAssignmentInfo> list = new ArrayList<>();
        String sql = "SELECT c.id classe_id, c.nom classe_nom, n.libelle niveau_libelle, " +
                     "RTRIM(XMLAGG(XMLELEMENT(e, m.nom || ', ') ORDER BY m.nom).EXTRACT('//text()').getStringVal(), ', ') matieres " +
                     "FROM enseignant_classe_matiere ecm " +
                     "JOIN classe c ON c.id = ecm.classe_id " +
                     "JOIN niveau n ON n.id = c.niveau_id " +
                     "JOIN matiere m ON m.id = ecm.matiere_id " +
                     "WHERE ecm.enseignant_id = ? AND ecm.annee_id = ? " +
                     "GROUP BY c.id, c.nom, n.libelle " +
                     "ORDER BY n.libelle, c.nom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            ps.setInt(2, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TeacherAssignmentInfo(
                    rs.getInt("classe_id"),
                    rs.getString("classe_nom"),
                    rs.getString("niveau_libelle"),
                    rs.getString("matieres")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean deleteAssignmentGroup(int teacherId, int classeId, int anneeId) {
        String sql = "DELETE FROM enseignant_classe_matiere WHERE enseignant_id = ? AND classe_id = ? AND annee_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, teacherId); ps.setInt(2, classeId); ps.setInt(3, anneeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static class TeacherAssignmentInfo {
        private int classeId;
        private String classeNom, niveauLibelle, matieres;
        public TeacherAssignmentInfo(int classeId, String classeNom, String niveauLibelle, String matieres) {
            this.classeId = classeId; this.classeNom = classeNom; 
            this.niveauLibelle = niveauLibelle; this.matieres = matieres;
        }
        public int getClasseId() { return classeId; }
        public String getClasseNom() { return classeNom; }
        public String getNiveauLibelle() { return niveauLibelle; }
        public String getMatieres() { return matieres; }
    }
}