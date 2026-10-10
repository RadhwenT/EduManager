package application;

public class AnneeScolaire {
	private int id;
	private String libelle;

	public AnneeScolaire(int id, String libelle) {
		this.id = id;
		this.libelle = libelle;
	}

	public int getId() {
		return id;
	}

	public String getLibelle() {
		return libelle;
	}

	@Override
	public String toString() {
		return libelle; 
	}
}