package io.github.radhwent.edumanager.dao;

import io.github.radhwent.edumanager.db.DBConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AppSettings {

    private static AppSettings instance;
    private final Map<String, Boolean> settings = new HashMap<>();

    private AppSettings() {
        reload();
    }

    public static AppSettings get() {
        if (instance == null) instance = new AppSettings();
        return instance;
    }

    public void reload() {
        settings.clear();
        String sql = "SELECT cle, valeur FROM app_settings";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                settings.put(rs.getString("cle"), "true".equalsIgnoreCase(rs.getString("valeur")));
            }
        } catch (SQLException e) {
            settings.put("inscription_active", true);
            settings.put("affectation_active", true);
        }
    }

    public boolean isInscriptionActive() {
        return settings.getOrDefault("inscription_active", true);
    }

    public boolean isAffectationActive() {
        return settings.getOrDefault("affectation_active", true);
    }

    public boolean toggle(String key, boolean value) {
        String sql = "MERGE INTO app_settings USING dual ON (cle = ?) " +
                     "WHEN MATCHED THEN UPDATE SET valeur = ? " +
                     "WHEN NOT MATCHED THEN INSERT (cle, valeur) VALUES (?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value ? "true" : "false");
            ps.setString(3, key);
            ps.setString(4, value ? "true" : "false");
            ps.executeUpdate();
            settings.put(key, value);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
