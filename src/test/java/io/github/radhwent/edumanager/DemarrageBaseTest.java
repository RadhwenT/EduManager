package io.github.radhwent.edumanager;

import io.github.radhwent.edumanager.dao.AnneeScolaireDAO;
import io.github.radhwent.edumanager.dao.NiveauDAO;
import io.github.radhwent.edumanager.dao.UserDAO;
import io.github.radhwent.edumanager.db.DBInitializer;
import io.github.radhwent.edumanager.model.User;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Vérifie que la base se crée comme au premier lancement de l'application. */
class DemarrageBaseTest extends OracleTestBase {

    @Test
    void schemaEtDonneesDeDemoCrees() {
        assertEquals(6, new NiveauDAO().findAll().size());
        assertFalse(new AnneeScolaireDAO().findAll().isEmpty());
    }

    @Test
    void compteDirecteurParDefautPeutSeConnecter() {
        User u = new UserDAO().authenticate("directeur", "dir2024");
        assertNotNull(u);
        assertEquals("DIRECTEUR", u.getRole());
    }

    @Test
    void mauvaisMotDePasseRefuse() {
        assertNull(new UserDAO().authenticate("directeur", "faux"));
    }

    @Test
    void initialisationRelanceeSansErreur() {
        // L'application appelle checkAndInit à chaque démarrage : un 2e appel ne doit rien casser.
        DBInitializer.checkAndInit();
        assertEquals(6, new NiveauDAO().findAll().size());
        assertNotNull(new UserDAO().authenticate("directeur", "dir2024"));
    }
}
