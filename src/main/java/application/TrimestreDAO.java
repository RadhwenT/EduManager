package application;

import java.sql.*;

public class TrimestreDAO {

	public int findIdByNumeroAndAnnee(int numero, int anneeId) {

		String sql = "SELECT id FROM trimestre WHERE numero = ? AND annee_id = ?";

		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setInt(1, numero);
			ps.setInt(2, anneeId);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("id");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
}