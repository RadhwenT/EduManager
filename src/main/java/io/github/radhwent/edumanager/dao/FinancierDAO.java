package io.github.radhwent.edumanager.dao;

import io.github.radhwent.edumanager.db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinancierDAO {

    public List<StudentPaymentInfo> findStudentPayments(int anneeId, Integer niveauId, Integer classeId, String search) {
        List<StudentPaymentInfo> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT e.id, e.nom, e.prenom, e.matricule, c.nom AS classe_nom, n.libelle AS niveau, n.prix AS tarif, " +
            "(SELECT SUM(p.montant) FROM paiement p WHERE p.eleve_id = e.id AND p.annee_id = ?) AS paye " +
            "FROM eleve e " +
            "JOIN affectation_eleve ae ON ae.eleve_id = e.id " +
            "JOIN classe c ON c.id = ae.classe_id " +
            "JOIN niveau n ON n.id = c.niveau_id " +
            "WHERE c.annee_id = ? "
        );
        if (niveauId != null) sql.append(" AND n.id = ? ");
        if (classeId != null) sql.append(" AND c.id = ? ");
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (LOWER(e.nom) LIKE ? OR LOWER(e.prenom) LIKE ? OR LOWER(e.matricule) LIKE ?) ");
        }
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, anneeId);
            ps.setInt(idx++, anneeId);
            if (niveauId != null) ps.setInt(idx++, niveauId);
            if (classeId != null) ps.setInt(idx++, classeId);
            if (search != null && !search.isEmpty()) {
                String s = "%" + search.toLowerCase() + "%";
                ps.setString(idx++, s);
                ps.setString(idx++, s);
                ps.setString(idx++, s);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double tarif = rs.getDouble("tarif");
                double paye = rs.getDouble("paye");
                list.add(new StudentPaymentInfo(
                    rs.getInt("id"),
                    rs.getString("nom") + " " + rs.getString("prenom"),
                    rs.getString("matricule"),
                    rs.getString("classe_nom"),
                    rs.getString("niveau"),
                    tarif, paye, tarif - paye
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Payment> getPaymentHistory(int eleveId, int anneeId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT id, montant, date_paiement, mode_paiement FROM paiement WHERE eleve_id = ? AND annee_id = ? ORDER BY date_paiement DESC";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, eleveId); ps.setInt(2, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Payment(rs.getInt("id"), rs.getDouble("montant"), rs.getTimestamp("date_paiement").toString(), rs.getString("mode_paiement")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean recordPayment(int eleveId, int anneeId, double montant, String mode, String comment) {
        String sql = "INSERT INTO paiement (id, eleve_id, annee_id, montant, mode_paiement, commentaire) " +
                     "VALUES (seq_paiement.NEXTVAL, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, eleveId);
            ps.setInt(2, anneeId);
            ps.setDouble(3, montant);
            ps.setString(4, mode);
            ps.setString(5, comment);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public double getTotalRevenue(int anneeId) {
        String sql = "SELECT SUM(montant) FROM paiement WHERE annee_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public double getExpectedRevenue(int anneeId) {
        String sql = "SELECT SUM(n.prix) FROM affectation_eleve ae " +
                     "JOIN classe c ON c.id = ae.classe_id " +
                     "JOIN niveau n ON n.id = c.niveau_id " +
                     "WHERE c.annee_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static class StudentPaymentInfo {
        private int eleveId;
        private String eleveNom, matricule, classe, niveau;
        private double tarif, paye, reste;

        public StudentPaymentInfo(int eleveId, String eleveNom, String matricule, String classe, String niveau, double tarif, double paye, double reste) {
            this.eleveId = eleveId; this.eleveNom = eleveNom; this.matricule = matricule;
            this.classe = classe; this.niveau = niveau;
            this.tarif = tarif; this.paye = paye; this.reste = reste;
        }

        public int getEleveId() { return eleveId; }
        public String getEleveNom() { return eleveNom; }
        public String getMatricule() { return matricule; }
        public String getClasse() { return classe; }
        public String getNiveau() { return niveau; }
        public double getTarif() { return tarif; }
        public double getPaye() { return paye; }
        public double getReste() { return reste; }
    }

    public double getCashInHand(int anneeId) {
        double totalRecu = 0;
        double totalVerse = 0;
        String sqlRecu = "SELECT SUM(montant) FROM paiement WHERE annee_id = ? AND mode_paiement = 'Espèces'";
        String sqlVerse = "SELECT SUM(montant) FROM versement WHERE annee_id = ? AND type_versement = 'ESPECES'";
        try (PreparedStatement ps1 = DBConnection.getConnection().prepareStatement(sqlRecu);
             PreparedStatement ps2 = DBConnection.getConnection().prepareStatement(sqlVerse)) {
            ps1.setInt(1, anneeId);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) totalRecu = rs1.getDouble(1);
            ps2.setInt(1, anneeId);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) totalVerse = rs2.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return Math.max(0, totalRecu - totalVerse);
    }

    public List<Payment> getPendingChecks(int anneeId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.id, p.montant, p.date_paiement, e.nom || ' ' || e.prenom as eleve_nom " +
                     "FROM paiement p JOIN eleve e ON e.id = p.eleve_id " +
                     "WHERE p.annee_id = ? AND p.mode_paiement = 'Chèque' AND p.statut_versement = 'EN_CAISSE' AND p.versement_id IS NULL " +
                     "ORDER BY p.date_paiement DESC";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Payment(rs.getInt("id"), rs.getDouble("montant"), rs.getTimestamp("date_paiement").toString(), rs.getString("eleve_nom")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean recordCashVersement(int anneeId, double amount, String banque, String numBon) {
        String sql = "INSERT INTO versement (id, annee_id, type_versement, montant, banque, num_bon, date_versement) " +
                     "VALUES (seq_versement.NEXTVAL, ?, 'ESPECES', ?, ?, ?, SYSDATE)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, anneeId); ps.setDouble(2, amount);
            ps.setString(3, banque); ps.setString(4, numBon);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean recordChecksVersement(int anneeId, List<Integer> paymentIds, String banque, String numBon) {
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            double total = 0;
            String sqlSum = "SELECT SUM(montant) FROM paiement WHERE id IN (" +
                             paymentIds.stream().map(String::valueOf).reduce((a,b)->a+","+b).orElse("0") + ")";
            try(Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlSum)) {
                if(rs.next()) total = rs.getDouble(1);
            }
            int versementId = -1;
            String sqlV = "INSERT INTO versement (id, annee_id, type_versement, montant, banque, num_bon, date_versement) " +
                          "VALUES (seq_versement.NEXTVAL, ?, 'CHEQUE', ?, ?, ?, SYSDATE)";
            try(PreparedStatement psV = conn.prepareStatement(sqlV, new String[]{"ID"})) {
                psV.setInt(1, anneeId); psV.setDouble(2, total); psV.setString(3, banque); psV.setString(4, numBon);
                psV.executeUpdate();
                try(ResultSet rs = psV.getGeneratedKeys()) { if(rs.next()) versementId = rs.getInt(1); }
            }
            String sqlUpd = "UPDATE paiement SET statut_versement = 'VERSE', versement_id = ? WHERE id = ?";
            try(PreparedStatement psU = conn.prepareStatement(sqlUpd)) {
                for(int pid : paymentIds) {
                    psU.setInt(1, versementId); psU.setInt(2, pid); psU.addBatch();
                }
                psU.executeBatch();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) {}
        }
    }

    public List<Versement> getVersementHistory(int anneeId, String type, String bankSearch) {
        List<Versement> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, type_versement, montant, banque, num_bon, date_versement FROM versement WHERE annee_id = ?");
        if(type != null) sql.append(" AND type_versement = ?");
        if(bankSearch != null && !bankSearch.isEmpty()) sql.append(" AND LOWER(banque) LIKE ?");
        sql.append(" ORDER BY date_versement DESC");
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, anneeId);
            if(type != null) ps.setString(idx++, type);
            if(bankSearch != null && !bankSearch.isEmpty()) ps.setString(idx++, "%" + bankSearch.toLowerCase() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Versement(
                    rs.getInt("id"), rs.getString("type_versement"), rs.getDouble("montant"),
                    rs.getString("banque"), rs.getString("num_bon"), rs.getTimestamp("date_versement").toString()
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static class Payment {
        private int id;
        private double montant;
        private String date, mode, eleveNom;

        public Payment(int id, double m, String d, String modeOrName) {
            this.id = id; this.montant = m; this.date = d;
            if (modeOrName != null && (modeOrName.equals("Espèces") || modeOrName.equals("Chèque") || modeOrName.equals("Virement"))) {
                this.mode = modeOrName;
            } else {
                this.eleveNom = modeOrName;
            }
        }

        public int getId() { return id; }
        public double getMontant() { return montant; }
        public String getDate() { return date; }
        public String getMode() { return mode; }
        public String getEleveNom() { return eleveNom; }
    }

    public static class Versement {
        private int id;
        private String type, banque, numBon, date;
        private double montant;

        public Versement(int id, String t, double m, String b, String n, String d) {
            this.id = id; this.type = t; this.montant = m; this.banque = b; this.numBon = n; this.date = d;
        }

        public int getId() { return id; }
        public String getType() { return type; }
        public double getMontant() { return montant; }
        public String getBanque() { return banque; }
        public String getNumBon() { return numBon; }
        public String getDate() { return date; }
    }
}
