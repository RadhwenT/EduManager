# EduManager — Gestion d'une école primaire privée

Application de bureau **JavaFX** connectée à une base **Oracle** pour gérer une école primaire privée : élèves, inscriptions, classes, enseignants, notes et paiements.

> Projet fédéré — 2e année Licence Génie Logiciel et Systèmes d'Information, Institut International de Technologie (IIT, Sfax). Note obtenue : **20/20**.

[![CI](https://github.com/RadhwenT/edumanager/actions/workflows/ci.yml/badge.svg)](https://github.com/RadhwenT/edumanager/actions/workflows/ci.yml) ![Java](https://img.shields.io/badge/Java-21-orange) ![JavaFX](https://img.shields.io/badge/JavaFX-21-blue) ![Oracle](https://img.shields.io/badge/Oracle-XE-red) ![Maven](https://img.shields.io/badge/Build-Maven-informational)

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
src/main/java/io/github/radhwent/edumanager/
├── Main.java / Launcher.java     # démarrage
├── db/          DBConnection (config externe), DBInitializer (création / mise à jour du schéma)
├── security/    PasswordUtil (hachage PBKDF2)
├── model/       Eleve, Classe, Enseignant, Niveau, Matiere, AnneeScolaire, User
├── dao/         accès aux données (requêtes SQL paramétrées)
└── view/        LoginView + une vue JavaFX par rôle
src/test/java/                     # tests JUnit 5 sur une base Oracle jetable (Testcontainers)
src/main/resources/db/schema.sql   # schéma Oracle + données de démo
```

## Tests

Les tests (JUnit 5) vérifient la logique métier sur une **vraie base Oracle** lancée automatiquement dans Docker par Testcontainers : hachage des mots de passe, création du schéma, inscriptions et matricules, classes et affectations (réaffectation, permutation), paiements et reste à payer, comptes enseignants et détection des conflits d'affectation, saisie des notes.

```bash
./mvnw test      # Docker doit être démarré (sous Windows : .\mvnw test)
```

Ils s'exécutent aussi à chaque push grâce à GitHub Actions.

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
- **En ligne de commande** (Maven non nécessaire) : `./mvnw javafx:run` (sous Windows : `.\mvnw javafx:run`)

Au premier lancement sur une base vide, l'application **crée automatiquement toutes les tables** et ajoute des données de démo : année 2026-2027, 6 niveaux, matières.

### Comptes

| Rôle | Login | Mot de passe |
| --- | --- | --- |
| Directeur | `directeur` | `dir2024` |

Depuis l'espace Directeur, créez ensuite les comptes **administrateur** et **financier**. Les comptes **enseignant** sont créés par l'administrateur. Pensez à changer le mot de passe du directeur après la première connexion.

### 5. (Optionnel) Charger des données de démonstration

Pour explorer l'application avec une école déjà remplie (90 élèves, 9 classes, 6 enseignants, notes du 1er trimestre, paiements), lancez une fois l'application pour créer les tables, puis :

```bash
docker cp scripts/demo-data.sql edumanager-oracle:/tmp/demo-data.sql
docker exec -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 edumanager-oracle sqlplus -s edumanager/edumanager@//localhost/XEPDB1 @/tmp/demo-data.sql
```

Le script peut être relancé sans créer de doublons. Comptes ajoutés (mot de passe `demo2026`) : `admin.demo`, `financier.demo` et les enseignants `mbensalah`, `khaddad`, `smzali`, `hboukadi`, `ldammak`, `wsmaoui`.

## Auteur

**Radhwen Trabelsi** — étudiant en Génie Logiciel à l'IIT · [LinkedIn](https://www.linkedin.com/in/radhwen-trabelsi)
