import java.util.List;

public class DocumentIndex {
    private int id;
    private List<String> motsCles;

    public DocumentIndex(int id, String titre) {
        this.id = id;
        this.motsCles = HelperDocumentIndex.extractionMotsCles(titre);
    }

    public List<String> getMotsCles() {
        return motsCles;
    }

    public int getId() {
        return id;
    }
}
