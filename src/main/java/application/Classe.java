package application;

public class Classe {
	private int id;
	private String nom;
	private int niveauId;

	public Classe(int id, String nom) {
		this.id = id;
		this.nom = nom;
	}

	public int getId() { return id; }
	public String getNom() { return nom; }
	public String getLibelle() { return nom; }
	public int getNiveauId() { return niveauId; }
	public void setNiveauId(int niveauId) { this.niveauId = niveauId; }

	@Override
	public String toString() { return nom; }
}
