package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClasseDAO {

    public List<Classe> findAll(int anneeId) {
        List<Classe> list = new ArrayList<>();
        String sql = "SELECT id, nom, niveau_id FROM classe WHERE annee_id = ? ORDER BY nom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Classe c = new Classe(rs.getInt("id"), rs.getString("nom"));
                c.setNiveauId(rs.getInt("niveau_id"));
                list.add(c);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Classe> findByNiveauAndAnnee(int niveauId, int anneeId) {
        List<Classe> list = new ArrayList<>();
        String sql = "SELECT id, nom FROM classe WHERE niveau_id = ? AND annee_id = ? ORDER BY nom";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, niveauId); ps.setInt(2, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Classe c = new Classe(rs.getInt("id"), rs.getString("nom"));
                c.setNiveauId(niveauId);
                list.add(c);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Classe> findClassesByEnseignantAnneeAndNiveau(User user, int anneeId, int niveauId) {
        List<Classe> list = new ArrayList<>();
        String sql = "SELECT DISTINCT c.id, c.nom FROM classe c " +
                     "JOIN enseignant_classe_matiere ecm ON c.id = ecm.classe_id " +
                     "JOIN enseignant e ON e.id = ecm.enseignant_id " +
                     "WHERE e.user_id = ? AND ecm.annee_id = ? AND c.niveau_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, user.getId()); ps.setInt(2, anneeId); ps.setInt(3, niveauId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Classe(rs.getInt("id"), rs.getString("nom")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean create(int niveauId, int anneeId, String libelleNiveau) {
        int count = countByNiveauAndAnnee(niveauId, anneeId);
        String lettre = String.valueOf((char)('A' + count));
        String nom = libelleNiveau.replaceAll("\\s+", "") + "-" + lettre;
        String sql = "INSERT INTO classe (nom, niveau_id, annee_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, nom); ps.setInt(2, niveauId); ps.setInt(3, anneeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM classe WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public int countByNiveauAndAnnee(int niveauId, int anneeId) {
        String sql = "SELECT COUNT(*) FROM classe WHERE niveau_id = ? AND annee_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, niveauId); ps.setInt(2, anneeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}