package application;

public class Enseignant {
    private int id;
    private String code;
    private String nom;
    private String prenom;
    private String telephone;
    private int userId;
    private String login;

    public Enseignant() {}

    public Enseignant(int id, String code, String nom, String prenom, String telephone, int userId, String login) {
        this.id = id;
        this.code = code;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.userId = userId;
        this.login = login;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getNomComplet() { return nom + " " + prenom; }

    @Override
    public String toString() { return getNomComplet(); }
}