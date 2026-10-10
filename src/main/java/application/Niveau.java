package application;

public class Niveau {
    private int id;
    private String libelle;
    private double prix;

    public Niveau() {}

    public Niveau(int id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public Niveau(int id, String libelle, double prix) {
        this.id = id;
        this.libelle = libelle;
        this.prix = prix;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    @Override
    public String toString() { return libelle; }
}