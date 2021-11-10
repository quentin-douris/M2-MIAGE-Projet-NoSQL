import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class HelperDocumentIndex {
    /**
     * Formate les mots clés extraits à partir du titre
     * @param titre
     * @return
     */
    public static List<String> extractionMotsCles(String titre) {
        List<String> toReturn = new ArrayList<>();
        List<String> temp = new ArrayList<>();

        // Découpe la chaine
        String titreWithoutSpace = titre;
        StringTokenizer st = new StringTokenizer(titreWithoutSpace.toLowerCase(), ",'-:;.()+[]{}?! ");

        // Ajoute les mots clés à la collection
        while (st.hasMoreTokens())
        {
            String mot = st.nextToken();
            toReturn.add(mot.trim());
        }

//        // Permet de supprimer les espaces
//        for (String item : temp) {
//            String[] parts = item.trim().split(" ");
//            for (String s : parts) {
//                toReturn.add(s);
//            }
//        }

        return toReturn;
    }
}
