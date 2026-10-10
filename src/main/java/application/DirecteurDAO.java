package application;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DirecteurDAO {

    public int countEleves(int anneeId) {
        return count("SELECT COUNT(DISTINCT eleve_id) FROM inscription WHERE annee_id = ?", anneeId);
    }

    public int countEnseignants() {
        return count("SELECT COUNT(*) FROM enseignant", -1);
    }

    public int countClasses(int anneeId) {
        return count("SELECT COUNT(*) FROM classe WHERE annee_id = ?", anneeId);
    }

    public double getTauxPaiement(int anneeId) {
        double encaisse = queryDouble(
            "SELECT NVL(SUM(montant),0) FROM paiement WHERE annee_id = ?", anneeId);
        double attendu = queryDouble(
            "SELECT NVL(SUM(n.prix),0) FROM inscription i " +
            "JOIN niveau n ON n.id = i.niveau_id WHERE i.annee_id = ?", anneeId);
        return attendu == 0 ? 0 : Math.min(100.0, (encaisse / attendu) * 100.0);
    }

    public double getTotalEncaisse(int anneeId) {
        return queryDouble("SELECT NVL(SUM(montant),0) FROM paiement WHERE annee_id = ?", anneeId);
    }

    public double getTotalAttendu(int anneeId) {
        return queryDouble(
            "SELECT NVL(SUM(n.prix),0) FROM inscription i " +
            "JOIN niveau n ON n.id = i.niveau_id WHERE i.annee_id = ?", anneeId);
    }

    public List<Object[]> getStatsByNiveau(int anneeId) {
        List<Object[]> list = new ArrayList<>();
        String sql =
            "SELECT n.libelle, " +
            "COUNT(DISTINCT i.eleve_id) AS nb_eleves, " +
            "COUNT(DISTINCT c.id) AS nb_classes, " +
            "NVL(SUM(DISTINCT n.prix),0) AS tarif " +
            "FROM niveau n " +
            "LEFT JOIN inscription i ON i.niveau_id = n.id AND i.annee_id = ? " +
            "LEFT JOIN classe c ON c.niveau_id = n.id AND c.annee_id = ? " +
            "GROUP BY n.libelle ORDER BY n.libelle";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId); ps.setInt(2, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("libelle"),
                    rs.getInt("nb_eleves"),
                    rs.getInt("nb_classes"),
                    rs.getDouble("tarif")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<AnneeScolaire> getAllAnnees() {
        return new AnneeScolaireDAO().findAll();
    }

    public boolean createAnnee(String libelle) {
        String sql = "INSERT INTO annee_scolaire (id, libelle) VALUES (seq_annee.NEXTVAL, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql,
                new String[]{"ID"})) {
            ps.setString(1, libelle);
            if (ps.executeUpdate() > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    createTrimestres(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private void createTrimestres(int anneeId) throws SQLException {
        String sql = "INSERT INTO trimestre (id, numero, annee_id) VALUES (seq_trimestre.NEXTVAL, ?, ?)";
        for (int i = 1; i <= 3; i++) {
            try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, i); ps.setInt(2, anneeId);
                ps.executeUpdate();
            }
        }
    }

    public List<User> getStaff() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, login, role FROM user_account " +
                     "WHERE role IN ('ADMIN','FINANCIER') ORDER BY role, login";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                list.add(new User(rs.getInt("id"), rs.getString("login"), rs.getString("role")));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean createUser(String login, String password, String role) {
        String sql = "INSERT INTO user_account (id, login, password, role) " +
                     "VALUES (seq_user.NEXTVAL, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, login); ps.setString(2, PasswordUtil.hash(password)); ps.setString(3, role);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM user_account WHERE id = ? AND role NOT IN ('DIRECTEUR')";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private int count(String sql, int param) {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            if (param >= 0) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private double queryDouble(String sql, int param) {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            if (param >= 0) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
