package application;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

	public User authenticate(String login, String password) {
		String sql = "SELECT id, login, role, password FROM user_account WHERE login = ?";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setString(1, login);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String stored = rs.getString("password");
					if (PasswordUtil.verify(password, stored)) {
						int id = rs.getInt("id");
						if (!PasswordUtil.isHashed(stored)) {
							upgradePassword(id, password);
						}
						return new User(id, rs.getString("login"), rs.getString("role"));
					}
				}
			}
		} catch (SQLException e) {
			System.err.println("❌ Erreur lors de l'authentification");
			e.printStackTrace();
		}
		return null;
	}

	public boolean update(int userId, String newLogin, String newPassword) {
		String sql = "UPDATE user_account SET login = ?, password = ? WHERE id = ?";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setString(1, newLogin);
			ps.setString(2, PasswordUtil.hash(newPassword));
			ps.setInt(3, userId);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/** Remplace un ancien mot de passe en clair par sa version hachée. */
	private void upgradePassword(int userId, String plain) {
		String sql = "UPDATE user_account SET password = ? WHERE id = ?";
		try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
			ps.setString(1, PasswordUtil.hash(plain));
			ps.setInt(2, userId);
			ps.executeUpdate();
		} catch (SQLException e) {
			System.err.println("⚠️ Mise à niveau du mot de passe impossible : " + e.getMessage());
		}
	}
}
