import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
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
                    case 4:
                        auteursAyantPlusArticles();
                        break;
                    case 5:
                         rechercheDeDocumentAvancee();
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
     * 3.1. Créer une nouvelle collection dans MongoDB
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
     * 3.3. Création d'une structure miroir sur MongoDB
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
                            .append("documents", listeArticles );
                    // Insertion du document dans la collection
                    collectionIndexInverseMongo.insertOne(newDoc);
                }
            }
        }
    }

    /**
     * 3.4. Recherche de document
     */
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
     * 3.5. Retourne les auteurs ayant écrits le plus d'articles (résultat retourné par ordre croissant du nombre d'article)
     */
    private static void auteursAyantPlusArticles() {
        System.out.println("Les 10 auteurs ayant écrit le plus d'articles");
        // Connexion à la base Neo4J
        sessionNeo = driverNeo.session();

        // Requête pour récupérer les articles
        StatementResult result = sessionNeo.run( "MATCH(aut:Auteur)-[e:Ecrire]-(art:Article) return aut.nom as nom, count(e) as nbArticle ORDER BY nbArticle DESC LIMIT 10");
        while (result.hasNext()) {
            // Pour chaque enregistrement au parcours
            Record record = result.next();
            System.out.println("\t" + record.get("nbArticle").asInt() + " - " + record.get("nom").asString());
        }
        // Fermeture de la session Neo
        sessionNeo.close();
    }

    /**
     * 3.6. Recherche de document avancée
     */
    private static void rechercheDeDocumentAvancee() {
        int nbMots = 0;
        int cpt = 0;
        System.out.println("Veuillez saisir le nombre de mots que vous voulez saisir : ");
        nbMots = sc.nextInt();
        List<String> mots = new ArrayList<>();
        ArrayList<RechercheAvancee> resultatRecherche = new ArrayList<>();

        /*do {
            System.out.print("Saisir le mot n° " + cpt + " : ");
            sc.nextLine();
            String m = sc.nextLine();
            mots.add(m);
            cpt++;
        } while (cpt < nbMots);*/
        mots.add("with");
        mots.add("new");
        mots.add("systems");

        // Formate les mots saisi pour être paramètre de la requête
        StringBuilder sr = new StringBuilder();
        int cptSr = 0;
        for(String mot : mots) {
            if(cptSr != 0 && cptSr != mots.size()) {
                sr.append(",");
            }
            sr.append("'" + mot + "'");
            cptSr++;
        }

        // Exécution de ma requête
        AggregateIterable<Document> documents =
                collectionIndexInverseMongo.aggregate(java.util.Arrays.asList(
                        Document.parse("{$match : { mot : {$in : [ " + sr.toString() +"]}}}"),
                        Document.parse("{$unwind: \"$documents\"}"),
                        Document.parse("{$group: {_id:\"$documents\",nbMots: {$sum :1}}}"),
                        Document.parse("{$sort :{nbMots : -1}}"),
                        Document.parse("{$limit:10}")
                ));

        for (MongoCursor<Document> it = documents.iterator(); it.hasNext();){
            Document doc2 = it.next();
            System.out.println(doc2.toJson());

            RechercheAvancee item = gson.fromJson(doc2.toJson(), RechercheAvancee.class);
            resultatRecherche.add(item);
        }

        // Connexion à la base Neo4J
        sessionNeo = driverNeo.session();


        List<Integer> idDesDocuments = new ArrayList<Integer>();
        for (RechercheAvancee res : resultatRecherche) {
            idDesDocuments.add(res.getId());
        }
            StatementResult result = sessionNeo.run( "match (n:Article) WHERE ID(n) IN " + idDesDocuments +" return ID(n) as id, n.titre as titre");
            while (result.hasNext()) {
                Record articleResult = result.next();
                for (RechercheAvancee r : resultatRecherche) {
                    if (articleResult.get("id").asInt() == r.getId()) {
                        r.setTitre(articleResult.get("titre").asString());
                    }
                }
            }

            for (RechercheAvancee r : resultatRecherche) {
                System.out.println(r.toString());
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
