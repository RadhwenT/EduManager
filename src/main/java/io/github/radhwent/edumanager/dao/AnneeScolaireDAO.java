package io.github.radhwent.edumanager.dao;

import io.github.radhwent.edumanager.db.DBConnection;
import io.github.radhwent.edumanager.model.AnneeScolaire;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnneeScolaireDAO {

	public List<AnneeScolaire> findAll() {
		List<AnneeScolaire> list = new ArrayList<>();
		String sql = "SELECT id, libelle FROM annee_scolaire ORDER BY libelle DESC";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				list.add(new AnneeScolaire(rs.getInt("id"), rs.getString("libelle")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}
}