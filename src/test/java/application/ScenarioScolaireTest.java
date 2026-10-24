package application;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests de caractérisation : ils décrivent le comportement ACTUEL de l'application
 * (inscriptions, classes, affectations, paiements, enseignants, notes) afin que toute
 * réorganisation future du code soit vérifiée automatiquement.
 * Chaque test crée ses propres données (noms uniques) sur la base jetable.
 */
class ScenarioScolaireTest extends OracleTestBase {

    static final int ANNEE = 1; // année 2026-2027 des données de démo

    static Niveau niveau1, niveau2;

    final EleveDAO eleves = new EleveDAO();
    final ClasseDAO classes = new ClasseDAO();
    final FinancierDAO finances = new FinancierDAO();
    final EnseignantDAO enseignants = new EnseignantDAO();

    @BeforeAll
    static void niveaux() {
        List<Niveau> all = new NiveauDAO().findAll();
        niveau1 = all.stream().filter(n -> n.getLibelle().equals("1ère année")).findFirst().orElseThrow();
        niveau2 = all.stream().filter(n -> n.getLibelle().equals("2ème année")).findFirst().orElseThrow();
    }

    // ---------- outils ----------

    private Eleve creerEleve() {
        String nom = "Test" + UUID.randomUUID().toString().substring(0, 8);
        Eleve e = new Eleve(0, nom, "Ali", "2018-05-12", "Sfax", "20000000");
        e.setMatricule(nom);
        assertTrue(eleves.create(e));
        return eleves.findAll().stream().filter(x -> nom.equals(x.getMatricule())).findFirst().orElseThrow();
    }

    private Classe creerClasse(Niveau n) {
        int avant = classes.countByNiveauAndAnnee(n.getId(), ANNEE);
        assertTrue(classes.create(n.getId(), ANNEE, n.getLibelle()));
        assertEquals(avant + 1, classes.countByNiveauAndAnnee(n.getId(), ANNEE));
        List<Classe> liste = classes.findByNiveauAndAnnee(n.getId(), ANNEE);
        return liste.stream().max((a, b) -> Integer.compare(a.getId(), b.getId())).orElseThrow();
    }

    private int compter(String sql, int... params) throws Exception {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setInt(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    // ---------- élèves et inscriptions ----------

    @Test
    void creerEtModifierUnEleve() {
        Eleve e = creerEleve();
        Eleve modifie = new Eleve(e.getId(), e.getNom(), "Mohamed", "2018-05-12", "Tunis", "21111111");
        modifie.setMatricule(e.getMatricule());
        assertTrue(eleves.update(modifie));
        Eleve relu = eleves.findAll().stream().filter(x -> x.getId() == e.getId()).findFirst().orElseThrow();
        assertEquals("Mohamed", relu.getPrenom());
        assertEquals("Tunis", relu.getAdresse());
    }

    @Test
    void matriculeDInscriptionSuitLeFormatAnneeNiveauNumero() {
        String m = eleves.genererMatricule("2026-2027", "1ère année");
        assertTrue(m.matches("2026-2027-1èreannée-\\d{3}"), m);
    }

    @Test
    void inscriptionPuisAnnulation() {
        Eleve e = creerEleve();
        assertFalse(eleves.findInscritsIds(ANNEE).contains(e.getId()));
        assertTrue(eleves.inscrire(e.getId(), ANNEE, niveau1.getId(), "Espèces"));
        assertTrue(eleves.findInscritsIds(ANNEE).contains(e.getId()));
        assertTrue(eleves.findInscritsNonAffectes(ANNEE, niveau1.getId()).stream().anyMatch(x -> x.getId() == e.getId()));

        assertTrue(eleves.annulerInscription(e.getId(), ANNEE));
        assertFalse(eleves.findInscritsIds(ANNEE).contains(e.getId()));
    }

    // ---------- classes et affectations ----------

    @Test
    void nomDeClasseGenereAvecLaLettreSuivante() {
        int existantes = classes.countByNiveauAndAnnee(niveau2.getId(), ANNEE);
        Classe c = creerClasse(niveau2);
        assertEquals("2èmeannée-" + (char) ('A' + existantes), c.getNom());
    }

    @Test
    void affectationReaffectationEtClasseActive() {
        Classe a = creerClasse(niveau1);
        Classe b = creerClasse(niveau1);
        Eleve e = creerEleve();
        eleves.inscrire(e.getId(), ANNEE, niveau1.getId(), "Espèces");

        assertTrue(eleves.affecter(e.getId(), a.getId()));
        assertEquals(1, eleves.countAffectesClasse(a.getId()));
        assertEquals(a.getNom(), eleves.findClasseActive(e.getId(), ANNEE));
        assertTrue(eleves.findInscriptionsByAnnee(ANNEE).stream()
                .anyMatch(i -> i.getEleveId() == e.getId() && i.getClasse().equals(a.getNom())));

        assertTrue(eleves.reaffecter(e.getId(), b.getId()));
        assertEquals(0, eleves.countAffectesClasse(a.getId()));
        assertEquals(b.getNom(), eleves.findClasseActive(e.getId(), ANNEE));
    }

    @Test
    void eleveNonAffecteAfficheNonAffecte() {
        Eleve e = creerEleve();
        eleves.inscrire(e.getId(), ANNEE, niveau1.getId(), "Chèque");
        assertEquals("-", eleves.findClasseActive(e.getId(), ANNEE));
        assertTrue(eleves.findInscriptionsByAnnee(ANNEE).stream()
                .anyMatch(i -> i.getEleveId() == e.getId() && i.getClasse().equals("Non affecté")));
    }

    @Test
    void permutationEchangeLesClasses() {
        Classe a = creerClasse(niveau1);
        Classe b = creerClasse(niveau1);
        Eleve e1 = creerEleve(), e2 = creerEleve();
        eleves.affecter(e1.getId(), a.getId());
        eleves.affecter(e2.getId(), b.getId());

        assertTrue(eleves.permuter(e1.getId(), e2.getId()));
        assertEquals(b.getNom(), eleves.findClasseActive(e1.getId(), ANNEE));
        assertEquals(a.getNom(), eleves.findClasseActive(e2.getId(), ANNEE));
    }

    @Test
    void permutationImpossibleSiUnEleveNEstPasAffecte() {
        Classe a = creerClasse(niveau1);
        Eleve e1 = creerEleve(), e2 = creerEleve();
        eleves.affecter(e1.getId(), a.getId());
        assertFalse(eleves.permuter(e1.getId(), e2.getId()));
        assertEquals(a.getNom(), eleves.findClasseActive(e1.getId(), ANNEE));
    }

    @Test
    void suppressionDUnEleveAvecSesDonnees() throws Exception {
        Classe a = creerClasse(niveau1);
        Eleve e = creerEleve();
        eleves.inscrire(e.getId(), ANNEE, niveau1.getId(), "Espèces");
        eleves.affecter(e.getId(), a.getId());

        assertTrue(eleves.delete(e.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM eleve WHERE id = ?", e.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM inscription WHERE eleve_id = ?", e.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM affectation_eleve WHERE eleve_id = ?", e.getId()));
        // La connexion partagée doit rester utilisable après une suppression.
        assertFalse(new NiveauDAO().findAll().isEmpty());
    }

    // ---------- paiements ----------

    @Test
    void resteAPayerCalculeAPartirDuTarifDuNiveau() {
        Classe a = creerClasse(niveau1);
        Eleve e = creerEleve();
        eleves.inscrire(e.getId(), ANNEE, niveau1.getId(), "Espèces");
        eleves.affecter(e.getId(), a.getId());

        assertTrue(finances.recordPayment(e.getId(), ANNEE, 1000, "ESPECES", "1er versement"));
        assertTrue(finances.recordPayment(e.getId(), ANNEE, 500, "CHEQUE", null));

        FinancierDAO.StudentPaymentInfo info = finances.findStudentPayments(ANNEE, null, a.getId(), null)
                .stream().filter(p -> p.getEleveId() == e.getId()).findFirst().orElseThrow();
        assertEquals(niveau1.getPrix(), info.getTarif(), 0.001);
        assertEquals(1500, info.getPaye(), 0.001);
        assertEquals(niveau1.getPrix() - 1500, info.getReste(), 0.001);
        assertEquals(2, finances.getPaymentHistory(e.getId(), ANNEE).size());
    }

    @Test
    void rechercheDePaiementParNom() {
        Classe a = creerClasse(niveau1);
        Eleve e = creerEleve();
        eleves.affecter(e.getId(), a.getId());
        assertEquals(1, finances.findStudentPayments(ANNEE, null, null, e.getNom().toUpperCase()).size());
    }

    @Test
    void recetteEncaisseeAugmenteDuMontantPaye() {
        Eleve e = creerEleve();
        double avant = finances.getTotalRevenue(ANNEE);
        finances.recordPayment(e.getId(), ANNEE, 250, "ESPECES", null);
        assertEquals(avant + 250, finances.getTotalRevenue(ANNEE), 0.001);
    }

    // ---------- enseignants ----------

    private Enseignant creerEnseignant() {
        String nom = "Prof" + UUID.randomUUID().toString().substring(0, 6);
        String login = enseignants.genererLogin("Sami", nom);
        Enseignant ens = new Enseignant(0, enseignants.genererCode(), nom, "Sami", "22000000", 0, login);
        assertTrue(enseignants.create(ens, "motdepasse"));
        return enseignants.findAll().stream().filter(x -> nom.equals(x.getNom())).findFirst().orElseThrow();
    }

    @Test
    void enseignantCreeAvecUnComptePourSeConnecter() {
        Enseignant ens = creerEnseignant();
        assertEquals(("s" + ens.getNom()).toLowerCase(), ens.getLogin());
        User u = new UserDAO().authenticate(ens.getLogin(), "motdepasse");
        assertNotNull(u);
        assertEquals("ENSEIGNANT", u.getRole());
    }

    @Test
    void loginEnseignantNumeroteSiDejaPris() {
        Enseignant ens = creerEnseignant();
        assertEquals(ens.getLogin() + "2", enseignants.genererLogin("Sami", ens.getNom()));
    }

    @Test
    void conflitDetecteSiLaMatiereEstDejaAffectee() {
        Classe c = creerClasse(niveau1);
        int matiere = new MatiereDAO().findByNiveau(niveau1.getId()).get(0).getId();
        Enseignant e1 = creerEnseignant(), e2 = creerEnseignant();

        assertNull(enseignants.checkConflicts(c.getId(), List.of(matiere), ANNEE, e1.getId()));
        assertTrue(enseignants.affecter(e1.getId(), c.getId(), List.of(matiere), ANNEE));
        assertEquals(List.of(matiere), enseignants.getMatiereAffecter(e1.getId(), c.getId(), ANNEE));

        // Pas de conflit avec soi-même, conflit pour un autre enseignant
        assertNull(enseignants.checkConflicts(c.getId(), List.of(matiere), ANNEE, e1.getId()));
        String conflit = enseignants.checkConflicts(c.getId(), List.of(matiere), ANNEE, e2.getId());
        assertNotNull(conflit);
        assertTrue(conflit.contains(e1.getNom()), conflit);
    }

    @Test
    void reaffecterUneMatiereRemplaceLAncienEnseignant() {
        Classe c = creerClasse(niveau1);
        int matiere = new MatiereDAO().findByNiveau(niveau1.getId()).get(0).getId();
        Enseignant e1 = creerEnseignant(), e2 = creerEnseignant();
        enseignants.affecter(e1.getId(), c.getId(), List.of(matiere), ANNEE);
        enseignants.affecter(e2.getId(), c.getId(), List.of(matiere), ANNEE);

        assertTrue(enseignants.getMatiereAffecter(e1.getId(), c.getId(), ANNEE).isEmpty());
        assertEquals(List.of(matiere), enseignants.getMatiereAffecter(e2.getId(), c.getId(), ANNEE));
    }

    // ---------- notes ----------

    @Test
    void saisieDeNoteMetAJourSansDoublon() throws Exception {
        Classe c = creerClasse(niveau1);
        Eleve e = creerEleve();
        eleves.affecter(e.getId(), c.getId());
        int matiere = new MatiereDAO().findByNiveau(niveau1.getId()).get(0).getId();
        int trimestre = new TrimestreDAO().findIdByNumeroAndAnnee(1, ANNEE);
        NoteDAO notes = new NoteDAO();

        notes.saveOrUpdate(e.getId(), matiere, c.getId(), trimestre, 12.5, 0);
        notes.saveOrUpdate(e.getId(), matiere, c.getId(), trimestre, 15.0, 0);

        assertEquals(1, compter("SELECT COUNT(*) FROM note WHERE eleve_id = ? AND matiere_id = ? AND trimestre_id = ?",
                e.getId(), matiere, trimestre));
        assertEquals(15, compter("SELECT valeur FROM note WHERE eleve_id = ? AND matiere_id = ? AND trimestre_id = ?",
                e.getId(), matiere, trimestre));
    }
}
