package io.github.radhwent.edumanager.model;

public class Matiere {
    private int id;
    private String nom;
    private int niveauId;
    private String niveauLibelle;

    public Matiere() {}

    public Matiere(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public Matiere(int id, String nom, int niveauId) {
        this.id = id;
        this.nom = nom;
        this.niveauId = niveauId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getNiveauId() { return niveauId; }
    public void setNiveauId(int niveauId) { this.niveauId = niveauId; }

    public String getNiveauLibelle() { return niveauLibelle; }
    public void setNiveauLibelle(String niveauLibelle) { this.niveauLibelle = niveauLibelle; }

    @Override
    public String toString() { return nom; }
}