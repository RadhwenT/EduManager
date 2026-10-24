package application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    @Test
    void hashProduitLeFormatAttendu() {
        String h = PasswordUtil.hash("secret");
        assertTrue(h.startsWith("pbkdf2$120000$"));
        assertEquals(4, h.split("\\$").length);
        assertTrue(PasswordUtil.isHashed(h));
    }

    @Test
    void verifyAccepteLeBonMotDePasse() {
        assertTrue(PasswordUtil.verify("secret", PasswordUtil.hash("secret")));
    }

    @Test
    void verifyRefuseUnMauvaisMotDePasse() {
        assertFalse(PasswordUtil.verify("autre", PasswordUtil.hash("secret")));
    }

    @Test
    void deuxHashDuMemeMotDePasseSontDifferents() {
        assertNotEquals(PasswordUtil.hash("secret"), PasswordUtil.hash("secret"));
    }

    @Test
    void ancienFormatEnClairToujoursAccepte() {
        assertFalse(PasswordUtil.isHashed("dir2024"));
        assertTrue(PasswordUtil.verify("dir2024", "dir2024"));
        assertFalse(PasswordUtil.verify("faux", "dir2024"));
    }

    @Test
    void valeursNullesOuCorrompuesRefusees() {
        assertFalse(PasswordUtil.verify(null, PasswordUtil.hash("x")));
        assertFalse(PasswordUtil.verify("x", null));
        assertFalse(PasswordUtil.verify("x", "pbkdf2$abc"));
        assertFalse(PasswordUtil.verify("x", "pbkdf2$1$!!!$!!!"));
    }
}
