import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.types.BasicBSONList;
import org.neo4j.driver.v1.*;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Program {
    private static final int CHOIX_MIN = -1;
    private static final int CHOIX_MAX = 8;

    private static int choixUtilisateur = 0;
    private static boolean finish = true;
    private static Scanner sc;
    private static Driver driverNeo;
    private static Session sessionNeo;
    private static MongoClient clientMongo;
    private static MongoCollection<Document> collectionIndexMongo;
    private static MongoCollection<Document> collectionIndexInverseMongo;
    private static MongoDatabase dbMongo;
    private static Gson gson;

    /**
     * Programme principal de l'application
     * @param args
     */
    public static void main(String args[]) {
        // Initialisation
        sc = new Scanner(System.in);
        gson = new GsonBuilder().setPrettyPrinting().create();
        driverNeo = GraphDatabase.driver("bolt://192.168.56.50");
        clientMongo = new MongoClient("192.168.56.50");
        dbMongo = clientMongo.getDatabase("dbDocuments");
        collectionIndexMongo = dbMongo.getCollection("index");
        collectionIndexInverseMongo = dbMongo.getCollection("indexInverse");

        boolean isCorrect;

        do {
            do{
                afficherMenu();
                switch(choixUtilisateur){
                    case 1:
                        creerCollectionIndex();
                        break;
                    case 2:
                        creerStructureMiroir();
                        break;
                    case 3:
                        rechercheDeDocument();
                        break;
                    case 0:
                        quitter();
                        break;
                    default:
                        break;
                }
                // Vérifie la saisie de l'utilisateur
                isCorrect = !(choixUtilisateur > CHOIX_MIN && choixUtilisateur < CHOIX_MAX);
            } while(isCorrect);
        } while(finish);
    }

    /**
     * Affiche le menu à l'utilisateur et demande de saisir un choix
     */
    private static void afficherMenu(){
        System.out.println("\n\t\t\t * Menu * ");
        System.out.println("");
        System.out.println("\t1 - Lister les films disponibles");
        System.out.println("\t2 - Lister les personnes disponibles");
        System.out.println("\t3 - Afficher les 3 films les mieux notés");
        System.out.println("\t4 - Afficher au plus 5 films proches");
        System.out.println("\tPour aller plus loin :");
        System.out.println("\t5 - Créer un nouveau film");
        System.out.println("\t6 - Ajouter une limite d'âge à un film");
        System.out.println("\t7 - Ajouter une Personne qui a joué dans un film");
        System.out.println("\t8 - Supprimer une Personne avec toutes ses relations");
        System.out.println("");
        System.out.println("\t0 - Quitter l'application");
        System.out.println("");
        System.out.println("Saisir une valeur : ");
        choixUtilisateur = sc.nextInt();
        System.out.println("choix : " + choixUtilisateur);
    }


    /**
     * Créer une nouvelle collection dans MongoDB
     */
    private static void creerCollectionIndex() {
        // Connexion à la base Neo4J
        sessionNeo = driverNeo.session();

        // Création de la liste qui accueillera les articles
        List<DocumentIndex> articles = new ArrayList<>();

        // Requête pour récupérer les articles
        StatementResult result = sessionNeo.run( "match (n:Article) return ID(n) as id, n.titre as titre" );
        while (result.hasNext()) {
            // Pour chaque article creer un document Index et l'insérer
            Record articleResult = result.next();
            DocumentIndex articleIndex = new DocumentIndex(articleResult.get("id").asInt(),articleResult.get("titre").asString());
            articles.add(articleIndex);
        }
        // Fermeture de la session Neo
        sessionNeo.close();

        // Insertion des données dans MongoDB
        for(DocumentIndex art : articles) {
            Document doc = Document.parse(gson.toJson(art));
            collectionIndexMongo.insertOne(doc);
        }
    }


    /**
     * Création d'une structure miroir sur MongoDB
     */
    private static void creerStructureMiroir() {
        // Récupére les documents dans la collection index
        FindIterable<Document> documentsIndex = collectionIndexMongo.find();

        for (Document d: documentsIndex) {
            DocumentIndex art = gson.fromJson(d.toJson(), DocumentIndex.class);
            for (String mot: art.getMotsCles()) {
                Document dex = collectionIndexInverseMongo.find(Filters.eq("mot", mot)).first();
                if (dex != null) {
                    //Contenu de l'id article
                    BsonDocument idObj = new BsonDocument();
                    idObj.append("documents", new BsonInt32(art.getId()));
                    BsonDocument update = new BsonDocument("$addToSet", idObj);
                    // Mise à jour de la collection
                    collectionIndexInverseMongo.updateOne(Filters.eq("mot", mot), update);
                } else {
                    BasicBSONList listeArticles = new BasicBSONList();
                    listeArticles.put(0, art.getId());
                    Document newDoc = new Document("mot", mot)
                            .append("articles", listeArticles );
                    // Insertion du document dans la collection
                    collectionIndexInverseMongo.insertOne(newDoc);
                }
            }
        }
    }

    private static void rechercheDeDocument() {
        String motCle;
        sc.nextLine();
        System.out.println("Saisir un mot que vous recherchez :");
        motCle = sc.nextLine();

        FindIterable<Document> documentIndex = collectionIndexInverseMongo.find(Filters.eq("mot", motCle));

        // Connexion à la base Neo4J
        sessionNeo = driverNeo.session();

        for (Document d : documentIndex) {
            DocumentIndexInverse indexInverse = gson.fromJson(d.toJson(), DocumentIndexInverse.class);
            List<Integer> idDesDocuments = indexInverse.getDocuments();
            StatementResult result = sessionNeo.run( "match (n:Article) WHERE ID(n) IN " + idDesDocuments +" return n.titre as titre ORDER BY n.titre ASC");
            while (result.hasNext()) {
                // Pour chaque article creer un document Index et l'insérer
                Record articleResult = result.next();
                System.out.println(articleResult.get("titre").asString());
            }
       }
        // Fermeture de la session Neo
        sessionNeo.close();
    }
    /**
     * Clôture l'application et ferme tous les services
     */
    private static void quitter() {
        driverNeo.close();
        clientMongo.close();
        finish = false;
        System.out.println("A bientot");
    }
}
