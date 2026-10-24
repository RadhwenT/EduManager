package io.github.radhwent.edumanager;

import static org.junit.jupiter.api.Assertions.*;

import io.github.radhwent.edumanager.dao.ClasseDAO;
import io.github.radhwent.edumanager.dao.DirecteurDAO;
import io.github.radhwent.edumanager.dao.FinancierDAO;
import io.github.radhwent.edumanager.dao.NoteDAO;
import io.github.radhwent.edumanager.dao.UserDAO;
import io.github.radhwent.edumanager.db.DBConnection;
import io.github.radhwent.edumanager.model.Classe;
import io.github.radhwent.edumanager.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

/** Vérifie que scripts/demo-data.sql produit des données cohérentes avec les règles de l'application. */
class DonneesDemoTest extends OracleTestBase {

    private int nombre(String sql) throws Exception {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static final String ELEVES_DEMO = "SELECT COUNT(*) FROM eleve WHERE REGEXP_LIKE(matricule, '^EL[0-9]{3}$')";

    @Test
    void eleveInscritsEtAffectes() throws Exception {
        assertEquals(90, nombre(ELEVES_DEMO));
        assertEquals(90, nombre("SELECT COUNT(*) FROM inscription i JOIN eleve e ON e.id = i.eleve_id WHERE REGEXP_LIKE(e.matricule, '^EL[0-9]{3}$')"));
        assertEquals(90, nombre("SELECT COUNT(*) FROM affectation_eleve a JOIN eleve e ON e.id = a.eleve_id WHERE REGEXP_LIKE(e.matricule, '^EL[0-9]{3}$')"));
    }

    @Test
    void scriptRelanceSansDoublon() throws Exception {
        int paiements = nombre("SELECT COUNT(*) FROM paiement WHERE commentaire LIKE 'demo-%'");
        int notes = nombre("SELECT COUNT(*) FROM note");
        chargerDonneesDemo();
        assertEquals(90, nombre(ELEVES_DEMO));
        assertEquals(paiements, nombre("SELECT COUNT(*) FROM paiement WHERE commentaire LIKE 'demo-%'"));
        assertEquals(notes, nombre("SELECT COUNT(*) FROM note"));
    }

    @Test
    void comptesDeDemoPeuventSeConnecter() {
        User admin = new UserDAO().authenticate("admin.demo", "demo2026");
        assertNotNull(admin);
        assertEquals("ADMIN", admin.getRole());
        assertEquals("FINANCIER", new UserDAO().authenticate("financier.demo", "demo2026").getRole());
        assertEquals("ENSEIGNANT", new UserDAO().authenticate("mbensalah", "demo2026").getRole());
    }

    @Test
    void reglesDeLApplicationRespectees() throws Exception {
        // 20 élèves maximum par classe
        assertEquals(0, nombre("SELECT COUNT(*) FROM (SELECT classe_id FROM affectation_eleve GROUP BY classe_id HAVING COUNT(*) > 20)"));
        // une seule personne par matière et par classe
        assertEquals(0, nombre("SELECT COUNT(*) FROM (SELECT classe_id, matiere_id FROM enseignant_classe_matiere GROUP BY classe_id, matiere_id, annee_id HAVING COUNT(*) > 1)"));
        // les 5 matières de chaque classe de démo ont un enseignant
        assertEquals(45, nombre("SELECT COUNT(*) FROM enseignant_classe_matiere ecm JOIN enseignant e ON e.id = ecm.enseignant_id WHERE e.code LIKE 'ENS1__'"));
        // aucun élève n'a payé plus que le tarif de son niveau
        assertTrue(new FinancierDAO().findStudentPayments(1, null, null, null).stream().allMatch(p -> p.getReste() >= 0));
    }

    @Test
    void tableauDeBordEtNotesAlimentes() {
        DirecteurDAO dir = new DirecteurDAO();
        assertTrue(dir.countEleves(1) >= 90);
        assertTrue(dir.countClasses(1) >= 9);
        assertTrue(dir.getTauxPaiement(1) > 0);

        Classe c = new ClasseDAO().findAll(1).stream().filter(x -> x.getNom().equals("4èmeannée-A")).findFirst().orElseThrow();
        NoteDAO notes = new NoteDAO();
        assertTrue(notes.areAllNotesValidated(c.getId(), 1));   // 1er trimestre validé
    }
}
