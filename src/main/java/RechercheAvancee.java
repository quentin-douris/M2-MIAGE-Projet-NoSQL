public class RechercheAvancee {
    private int _id;
    private String titre;
    private int nbMots;

    public RechercheAvancee(int _id, String titre, int nbMots) {
        this._id = _id;
        this.titre = titre;
        this.nbMots = nbMots;
    }

    public RechercheAvancee(int _id, int nbMots) {
        this._id = _id;
        this.nbMots = nbMots;
    }

    public int getId() {
        return _id;
    }

    public void setId(int id) {
        this._id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public int getNbMots() {
        return nbMots;
    }

    public void setNbMots(int nbMot) {
        this.nbMots = nbMot;
    }

    @Override
    public String toString() {
        return this._id  + " - " + this.titre + " - " + this.nbMots;
    }
}
