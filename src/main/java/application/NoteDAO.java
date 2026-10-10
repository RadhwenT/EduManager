package application;

import java.sql.*;
import java.util.*;

public class NoteDAO {

	public Map<String, Double> loadNotes(int userId, int classeId, int anneeId, int trimestreNumero) {
		Map<String, Double> notes = new HashMap<>();
		String sql = """
				    SELECT n.eleve_id, n.matiere_id, n.valeur
				    FROM note n
				    JOIN trimestre t ON t.id = n.trimestre_id
				    JOIN enseignant_classe_matiere ecm
				           ON ecm.classe_id = n.classe_id
				          AND ecm.matiere_id = n.matiere_id
				    JOIN enseignant e ON e.id = ecm.enseignant_id
				    WHERE e.user_id = ?
				      AND n.classe_id = ?
				      AND t.numero = ?
				      AND t.annee_id = ?
				""";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setInt(1, userId);
			ps.setInt(2, classeId);
			ps.setInt(3, trimestreNumero);
			ps.setInt(4, anneeId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String key = rs.getInt("eleve_id") + "_" + rs.getInt("matiere_id");
				notes.put(key, rs.getDouble("valeur"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return notes;
	}

	public Map<String, Double> loadNotesByTrimestreId(int userId, int classeId, int trimestreId) {
		Map<String, Double> notes = new HashMap<>();
		String sql = """
				    SELECT n.eleve_id, n.matiere_id, n.valeur
				    FROM note n
				    WHERE n.trimestre_id = ? AND n.classe_id = ?
				""";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setInt(1, trimestreId);
			ps.setInt(2, classeId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String key = rs.getInt("eleve_id") + "_" + rs.getInt("matiere_id");
				notes.put(key, rs.getDouble("valeur"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return notes;
	}

	public void saveOrUpdate(int eleveId, int matiereId, int classeId, int trimestreId, double valeur, int userId) {
		String checkSql = """
				    SELECT id FROM note
				    WHERE eleve_id    = ?
				      AND matiere_id  = ?
				      AND trimestre_id = ?
				""";
		String updateSql = """
				    UPDATE note
				       SET valeur = ?, classe_id = ?
				     WHERE eleve_id    = ?
				       AND matiere_id  = ?
				       AND trimestre_id = ?
				""";
		String insertSql = """
				    INSERT INTO note (eleve_id, matiere_id, classe_id, trimestre_id, valeur, validee)
				    VALUES (?, ?, ?, ?, ?, 0)
				""";
		try {
			Connection conn = DBConnection.getConnection();
			try (PreparedStatement check = conn.prepareStatement(checkSql)) {
				check.setInt(1, eleveId);
				check.setInt(2, matiereId);
				check.setInt(3, trimestreId);
				ResultSet rs = check.executeQuery();
				boolean exists = rs.next();
				rs.close();
				if (exists) {
					try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
						ps.setDouble(1, valeur);
						ps.setInt(2, classeId);
						ps.setInt(3, eleveId);
						ps.setInt(4, matiereId);
						ps.setInt(5, trimestreId);
						ps.executeUpdate();
					}
				} else {
					try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
						ps.setInt(1, eleveId);
						ps.setInt(2, matiereId);
						ps.setInt(3, classeId);
						ps.setInt(4, trimestreId);
						ps.setDouble(5, valeur);
						ps.executeUpdate();
					}
				}
				if (!conn.getAutoCommit()) {
					conn.commit();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void validerNotes(int userId, int classeId, int trimestreId) {
		String sql = """
				    UPDATE note
				    SET validee = 1
				    WHERE classe_id    = ?
				      AND trimestre_id = ?
				""";
		try {
			Connection conn = DBConnection.getConnection();
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setInt(1, classeId);
				ps.setInt(2, trimestreId);
				ps.executeUpdate();
				if (!conn.getAutoCommit()) {
					conn.commit();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean areNotesValidated(int userId, int classeId, int trimestreId) {
		String sql = """
				    SELECT COUNT(*) AS total
				    FROM note n
				    WHERE n.classe_id    = ?
				      AND n.trimestre_id = ?
				      AND n.validee      = 1
				""";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setInt(1, classeId);
			ps.setInt(2, trimestreId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("total") > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean areAllNotesValidated(int classeId, int trimestreId) {
		String sql = "SELECT COUNT(*) FROM note WHERE classe_id = ? AND trimestre_id = ? AND validee = 0";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setInt(1, classeId);
			ps.setInt(2, trimestreId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return rs.getInt(1) == 0;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public Map<String, String> loadObservations(int classeId, int trimestreId) {
		Map<String, String> obs = new java.util.HashMap<>();
		String sql = "SELECT eleve_id, matiere_id, observation FROM note WHERE classe_id = ? AND trimestre_id = ?";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setInt(1, classeId);
			ps.setInt(2, trimestreId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String key = rs.getInt(1) + "_" + rs.getInt(2);
				String val = rs.getString(3);
				if (val != null)
					obs.put(key, val);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return obs;
	}

	public List<String> getNonValidatedTeachers(int classeId, int anneeId, int trimestreId) {
		List<String> teachers = new ArrayList<>();
		String sql = """
				SELECT DISTINCT e.nom, e.prenom
				FROM enseignant_classe_matiere ecm
				JOIN enseignant e ON e.id = ecm.enseignant_id
				WHERE ecm.classe_id = ?
				  AND ecm.annee_id = ?
				  AND NOT EXISTS (
				      SELECT 1 FROM note n
				      WHERE n.classe_id = ecm.classe_id
				        AND n.matiere_id = ecm.matiere_id
				        AND n.trimestre_id = ?
				        AND n.validee = 1
				  )
				""";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setInt(1, classeId);
			ps.setInt(2, anneeId);
			ps.setInt(3, trimestreId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				teachers.add(rs.getString("nom") + " " + rs.getString("prenom"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return teachers;
	}

	public int countMissingNotes(int teacherUserId, int classeId, int trimestreId) {
		int totalEleves = 0;
		String sqlEleves = "SELECT COUNT(*) FROM affectation_eleve WHERE classe_id = ? AND date_fin IS NULL";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sqlEleves)) {
			ps.setInt(1, classeId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				totalEleves = rs.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		int totalMatieres = 0;
		String sqlMats = "SELECT COUNT(*) FROM enseignant_classe_matiere ecm JOIN enseignant e ON e.id = ecm.enseignant_id WHERE e.user_id = ? AND ecm.classe_id = ? AND ecm.annee_id = (SELECT annee_id FROM trimestre WHERE id = ?)";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sqlMats)) {
			ps.setInt(1, teacherUserId);
			ps.setInt(2, classeId);
			ps.setInt(3, trimestreId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				totalMatieres = rs.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		int expectedNotes = totalEleves * totalMatieres;
		int actualNotes = 0;
		String sqlActual = """
				SELECT COUNT(*) FROM note n
				JOIN enseignant_classe_matiere ecm ON ecm.classe_id = n.classe_id AND ecm.matiere_id = n.matiere_id
				JOIN enseignant e ON e.id = ecm.enseignant_id
				WHERE e.user_id = ? AND n.classe_id = ? AND n.trimestre_id = ?
				""";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sqlActual)) {
			ps.setInt(1, teacherUserId);
			ps.setInt(2, classeId);
			ps.setInt(3, trimestreId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				actualNotes = rs.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return Math.max(0, expectedNotes - actualNotes);
	}
}