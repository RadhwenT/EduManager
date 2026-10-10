package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatiereDAO {

    public List<Matiere> findAll() {
        List<Matiere> list = new ArrayList<>();
        String sql = "SELECT m.id, m.nom, m.niveau_id, n.libelle AS niveau_libelle " +
                     "FROM matiere m JOIN niveau n ON n.id = m.niveau_id ORDER BY n.libelle, m.nom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Matiere m = new Matiere(rs.getInt("id"), rs.getString("nom"));
                m.setNiveauId(rs.getInt("niveau_id"));
                m.setNiveauLibelle(rs.getString("niveau_libelle"));
                list.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Matiere> findByNiveau(int niveauId) {
        List<Matiere> list = new ArrayList<>();
        String sql = "SELECT id, nom FROM matiere WHERE niveau_id = ? ORDER BY nom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, niveauId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Matiere(rs.getInt("id"), rs.getString("nom")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Matiere> findMatieresByEnseignantAndClasse(User user, int classeId) {
        List<Matiere> list = new ArrayList<>();
        String sql = "SELECT DISTINCT m.id, m.nom FROM matiere m " +
                     "JOIN enseignant_classe_matiere ecm ON ecm.matiere_id = m.id " +
                     "JOIN enseignant e ON e.id = ecm.enseignant_id " +
                     "WHERE e.user_id = ? AND ecm.classe_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, user.getId()); ps.setInt(2, classeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Matiere(rs.getInt("id"), rs.getString("nom")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean create(Matiere m) {
        String sql = "INSERT INTO matiere (nom, niveau_id) VALUES (?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, m.getNom()); ps.setInt(2, m.getNiveauId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM matiere WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean update(Matiere m) {
        String sql = "UPDATE matiere SET nom = ?, niveau_id = ? WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, m.getNom()); ps.setInt(2, m.getNiveauId()); ps.setInt(3, m.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean isAssignedToTeacher(int matiereId) {
        String sql = "SELECT COUNT(*) FROM enseignant_classe_matiere WHERE matiere_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, matiereId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}