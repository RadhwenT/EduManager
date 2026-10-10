package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NiveauDAO {

    public List<Niveau> findAll() {
        List<Niveau> list = new ArrayList<>();
        String sql = "SELECT id, libelle, prix FROM niveau ORDER BY libelle";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(new Niveau(rs.getInt("id"), rs.getString("libelle"), rs.getDouble("prix")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Niveau> findNiveauxByEnseignantAndAnnee(User user, int anneeId) {
        List<Niveau> list = new ArrayList<>();
        String sql = "SELECT DISTINCT n.id, n.libelle, n.prix FROM niveau n " +
                     "JOIN classe c ON c.niveau_id = n.id " +
                     "JOIN enseignant_classe_matiere ecm ON ecm.classe_id = c.id " +
                     "JOIN enseignant e ON e.id = ecm.enseignant_id " +
                     "WHERE e.user_id = ? AND ecm.annee_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, user.getId()); ps.setInt(2, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new Niveau(rs.getInt("id"), rs.getString("libelle"), rs.getDouble("prix")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean create(Niveau n) {
        String sql = "INSERT INTO niveau (libelle, prix) VALUES (?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, n.getLibelle()); ps.setDouble(2, n.getPrix());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean update(Niveau n) {
        String sql = "UPDATE niveau SET libelle=?, prix=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, n.getLibelle()); ps.setDouble(2, n.getPrix()); ps.setInt(3, n.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM niveau WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }
}