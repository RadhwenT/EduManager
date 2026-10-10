# EduManager — Gestion d'une école primaire privée

Application de bureau **JavaFX** connectée à une base **Oracle** pour gérer une école primaire privée : élèves, inscriptions, classes, enseignants, notes et paiements.

> Projet fédéré — 2e année Licence Génie Logiciel et Systèmes d'Information, Institut International de Technologie (IIT, Sfax). Note obtenue : **20/20**.

![Java](https://img.shields.io/badge/Java-21-orange) ![JavaFX](https://img.shields.io/badge/JavaFX-21-blue) ![Oracle](https://img.shields.io/badge/Oracle-XE-red) ![Maven](https://img.shields.io/badge/Build-Maven-informational)

---

## Fonctionnalités

L'application propose **4 espaces**, chacun accessible selon le rôle de l'utilisateur connecté.

| Espace | Fonctionnalités |
| --- | --- |
| **Directeur** | Tableau de bord (effectifs, classes, chiffre d'affaires attendu / encaissé par niveau), création des années scolaires et de leurs trimestres, gestion des comptes administrateur et financier, activation / désactivation des inscriptions et affectations |
| **Administrateur** | Fiches élèves, inscriptions annuelles avec matricule automatique, affectation des élèves aux classes (20 élèves max, déplacement et permutation), gestion des enseignants et de leurs affectations matière / classe avec détection des conflits, gestion des niveaux, classes et matières |
| **Enseignant** | Saisie des notes par classe, matière et trimestre, suivi des notes manquantes, validation des notes |
| **Financier** | Suivi des paiements par élève (reste à payer), encaissement (espèces / chèque), versements bancaires, tarifs par niveau |

**Sécurité :** mots de passe hachés (PBKDF2-HMAC-SHA256 avec salt), identifiants de la base hors du code source, requêtes SQL paramétrées.

## Stack technique

- **Java 21** et **JavaFX 21** (interface construite en code, sans FXML)
- **Oracle Database** via JDBC (`ojdbc11`)
- **Maven** pour les dépendances et l'exécution
- Architecture en couches : modèles (`Eleve`, `Classe`…), DAO (accès aux données), vues JavaFX

```
src/main/java/application/
├── Main.java / Launcher.java     # démarrage
├── DBConnection.java              # connexion Oracle (config externe)
├── DBInitializer.java             # création / mise à jour automatique du schéma
├── PasswordUtil.java              # hachage des mots de passe
├── *DAO.java                      # accès aux données
├── LoginView.java + *View.java    # interfaces par rôle
└── modèles (Eleve, Classe, Enseignant, Niveau, Matiere…)
src/main/resources/db/schema.sql   # schéma Oracle + données de démo
```

## Lancer le projet

### 1. Prérequis

- **JDK 21** ([Temurin](https://adoptium.net/) par exemple)
- **VS Code** avec l'extension **Extension Pack for Java** (proposée automatiquement à l'ouverture du dossier)
- Une base **Oracle** : soit Oracle XE installé localement, soit **Docker** (recommandé)

### 2. Démarrer Oracle

Avec Docker, depuis la racine du projet :

```bash
docker compose up -d
```

Le premier démarrage prend 1 à 2 minutes. Un utilisateur `edumanager` / `edumanager` est créé automatiquement.

Avec une installation Oracle XE existante, créez simplement un utilisateur dédié (ou utilisez le vôtre).

### 3. Configurer la connexion

```bash
cp db.properties.example db.properties
```

Modifiez `db.properties` si besoin (URL, utilisateur, mot de passe). Ce fichier est ignoré par Git.

### 4. Lancer l'application

- **Dans VS Code :** ouvrez le dossier, attendez le chargement du projet Java, puis lancez la configuration **EduManager** (onglet *Run and Debug*, ou *Run* au-dessus de `main` dans `Launcher.java`).
- **En ligne de commande** (Maven installé) : `mvn javafx:run`

Au premier lancement sur une base vide, l'application **crée automatiquement toutes les tables** et ajoute des données de démo : année 2026-2027, 6 niveaux, matières.

### Compte de démonstration

| Rôle | Login | Mot de passe |
| --- | --- | --- |
| Directeur | `directeur` | `dir2024` |

Depuis l'espace Directeur, créez ensuite les comptes **administrateur** et **financier**. Les comptes **enseignant** sont créés par l'administrateur. Pensez à changer le mot de passe du directeur après la première connexion.

## Auteur

**Radhwen Trabelsi** — étudiant en Génie Logiciel à l'IIT · [LinkedIn](https://www.linkedin.com/in/radhwen-trabelsi)
