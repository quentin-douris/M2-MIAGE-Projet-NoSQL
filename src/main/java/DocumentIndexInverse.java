import java.util.List;

public class DocumentIndexInverse {
    private String mot;
    private List<Integer> documents;

    public DocumentIndexInverse(String mot, List<Integer> documents) {
        this.mot = mot;
        this.documents = documents;
    }

    public String getMot() {
        return mot;
    }

    public List<Integer> getDocuments() {
        return documents;
    }

    public void addIdDocument(int id) {
        this.documents.add(id);
    }
}
