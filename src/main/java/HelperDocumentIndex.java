import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Classe avec des méthodes techniques (boite à outils)
 */
public class HelperDocumentIndex {

    /**
     * Formate les mots clés extraits à partir du titre
     * @param titre
     * @return
     */
    public static List<String> extractionMotsCles(String titre) {
        List<String> toReturn = new ArrayList<>();

        // Découpe la chaine
        String titreWithoutSpace = titre;
        StringTokenizer st = new StringTokenizer(titreWithoutSpace.toLowerCase(), ",'-:;.()+[]{}?! ");

        // Ajoute les mots clés à la collection
        while (st.hasMoreTokens())
        {
            String mot = st.nextToken();
            toReturn.add(mot.trim());
        }

        return toReturn;
    }
}
