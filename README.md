# Formation Hibernate & JPA – Projet du participant

Projet de départ de la formation **Hibernate – Niveau intermédiaire** (16 h, 2 jours).
Formateur : OUEDRAOGO Dramane.

Vous partez d'une application Spring Boot 3.4.3 vide (aucune entité) et vous construisez, module après module,
une gestion commerciale : `Client → Commande → LigneCommande → Produit` (+ `Categorie`, `Utilisateur`).

## Prérequis

- JDK 17 ou plus récent
- Maven 3.9+
- MySQL 8 installé en local (port 3306)
- Git
- VS Code (avec l'Extension Pack for Java et le Spring Boot Extension Pack) ou Eclipse : voir [PROCEDURE_ECLIPSE.md](PROCEDURE_ECLIPSE.md)

## Créer la base de données

À exécuter une seule fois, connecté à MySQL en tant que `root` :

```sql
CREATE DATABASE IF NOT EXISTS formation_hibernate;
CREATE USER IF NOT EXISTS 'formation'@'localhost' IDENTIFIED BY 'formation';
GRANT ALL PRIVILEGES ON formation_hibernate.* TO 'formation'@'localhost';
```

Ces valeurs correspondent à `src/main/resources/application.properties`.

## Vérifier son poste avant le jour 1

1. **Compiler et tester** (sans MySQL, les tests tournent sur une base H2 en mémoire) :

   ```bash
   mvn test
   ```

   Résultat attendu : `Tests run: 1, Failures: 0, Errors: 0` puis `BUILD SUCCESS`.

2. **Lancer l'application** (MySQL doit tourner) :

   ```bash
   mvn spring-boot:run
   ```

   Résultat attendu : la ligne `HikariPool-1 - Start completed.` apparaît (connexion à MySQL établie),
   puis `Started GestionApplication` et `BUILD SUCCESS`. L'application s'arrête toute seule : c'est normal,
   elle ne contient encore aucun TP.

En cas d'échec à l'étape 2 :

| Message | Cause probable |
| --- | --- |
| `Communications link failure` | MySQL n'est pas démarré, ou n'écoute pas sur le port 3306 |
| `Access denied for user 'formation'` | le compte n'a pas été créé (voir le script SQL ci-dessus) |

## Organisation du projet

Tout le code va dans `src/main/java/com/formation/gestion`, avec **un package par rôle** :

| Package | Contenu |
| --- | --- |
| `entity` | les entités JPA et leurs énumérations |
| `repository` | l'accès aux données (requêtes), sans transaction |
| `service` | les règles métier et les transactions (`@Transactional`) |
| `dto` | les objets exposés en sortie des services (des `record`) |
| `tp` | un `CommandLineRunner` par TP |

Chaque TP est un `CommandLineRunner` activé par un profil Spring : `@Profile("tp1")`, `@Profile("tp2")` … `@Profile("tp8")`,
et `@Profile("projet")` pour le mini-projet. Un seul runner s'exécute donc à la fois :

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=tp1"
```

Dans VS Code : onglet **Run and Debug**, puis choisir la configuration (TP1 … TP8, Mini-projet).

Règles à respecter pendant toute la formation :

- les transactions sont déclarées **dans les services uniquement**, jamais dans les repositories ni dans les runners ;
- toutes les associations sont en chargement **paresseux** (LAZY) ;
- `spring.jpa.open-in-view=false` reste tel quel ;
- la base est **recréée à chaque lancement** (`ddl-auto=create`) : chaque TP crée lui-même ses données ;
- dans un runner, annoncez chaque étape par un titre affiché en console (par exemple `=== TP1 : creation ===`)
  pour retrouver facilement le SQL correspondant.

## Énoncés des travaux pratiques

Pour chaque TP : écrivez le code, lancez le profil, puis **lisez le SQL affiché en console** et comparez-le à ce que
l'énoncé vous demande d'observer. Les énoncés décrivent ce qu'il faut obtenir, pas comment l'écrire.

### TP 1 – Cycle de vie d'une entité (profil `tp1`)

**Objectif :** voir à quel moment Hibernate émet du SQL selon l'état de l'entité (nouvelle, gérée, détachée, supprimée).

À créer :

- l'entité `Utilisateur` : identifiant généré par la base (auto-incrément), `nom`, `email`, `dateCreation` renseignée
  automatiquement à la création de l'objet. Prévoyez un constructeur `(nom, email)` et le constructeur sans argument
  exigé par JPA ;
- le service `UtilisateurService`, qui travaille directement avec l'`EntityManager` et propose :
  - `creer(nom, email)` : enregistre un utilisateur et renvoie son identifiant ;
  - `trouver(id)` : renvoie l'utilisateur (lecture seule) ;
  - `changerEmail(id, email)` : modifie l'email **sans appeler aucune méthode d'enregistrement** ;
  - `enregistrer(utilisateur)` : rattache un utilisateur détaché et renvoie l'instance gérée ;
  - `supprimer(id)` ;
  - `deuxFindMemeInstance(id)` : charge deux fois le même utilisateur dans une seule transaction et indique
    si les deux références désignent le même objet.
- le runner du TP, qui enchaîne : création, changement d'email, récupération de l'utilisateur puis modification de son
  nom **en dehors de toute transaction**, appel à `enregistrer`, double lecture, suppression.

À observer dans le SQL :

- l'`INSERT` part dès la création ;
- le changement d'email produit un `SELECT` puis un `UPDATE`, alors que vous n'avez rien demandé d'autre qu'un setter ;
- modifier l'objet détaché ne produit **aucun** SQL ; c'est `enregistrer` qui déclenche `SELECT` + `UPDATE` ;
- l'instance renvoyée par `enregistrer` n'est **pas** celle que vous lui avez passée ;
- la double lecture ne produit qu'**un seul** `SELECT`.

### TP 2 – Mapping avancé (profil `tp2`)

**Objectif :** maîtriser le schéma généré : contraintes, index, énumération, objet embarqué.

À créer :

- l'énumération `StatutClient` : `PROSPECT`, `ACTIF`, `INACTIF` ;
- la classe `Adresse` (`rue` 150 caractères, `ville` 80, `pays` 80) : un objet valeur **sans identifiant**, stocké dans
  la table de l'entité qui le contient ;
- l'entité `Client`, table `client` :
  - `nom` obligatoire (100), `email` obligatoire (150), `telephone` facultatif (20) ;
  - `statut` obligatoire, enregistré **sous forme de texte**, `PROSPECT` par défaut ;
  - `dateInscription`, par défaut la date du jour ;
  - `adresse` embarquée ;
  - `selectionne`, un booléen d'état d'écran qui **ne doit pas être enregistré** ;
  - un index nommé `idx_client_nom` sur le nom ;
  - une contrainte d'unicité nommée `uk_client_email` sur l'email ;
  - un constructeur `(nom, email, adresse)`.
- le service `Tp2Service` avec `creerClient(nom, email, adresse)`, qui renvoie l'identifiant ;
- le runner : crée un client, puis tente d'en créer un second **avec le même email**, intercepte l'exception et
  affiche son type ainsi que le message de la cause SQL d'origine.

À observer :

- dans le `CREATE TABLE` : les colonnes de l'adresse sont dans la table `client`, le statut est un texte et non un
  numéro, et il n'existe aucune colonne pour `selectionne` ;
- dans MySQL, `SHOW CREATE TABLE client;` montre l'index et la contrainte avec les noms que vous avez choisis ;
- le second enregistrement échoue sur une violation de contrainte qui cite `uk_client_email`.

### TP 3 – Relations (profil `tp3`)

**Objectif :** relier les entités, choisir où placer les cascades, rencontrer la `LazyInitializationException`.

À créer :

- l'énumération `StatutCommande` : `BROUILLON`, `VALIDEE`, `LIVREE`, `ANNULEE` ;
- l'entité `Produit` : `code` obligatoire et unique (30), `nom` obligatoire (120), `prix` obligatoire (décimal, 12 chiffres
  dont 2 décimales), `stock` entier. Méthode `retirerStock(quantite)` qui diminue le stock et lève une
  `IllegalStateException` au message explicite si le stock est insuffisant ;
- l'entité `Categorie` : `nom` obligatoire et unique (60). Elle est liée aux produits par une relation
  plusieurs-à-plusieurs dont **`Produit` est le propriétaire** (table de jointure `produit_categorie`, colonnes
  `produit_id` et `categorie_id`). `Produit` offre une méthode `ajouterCategorie` qui met à jour les deux côtés.
  Définissez l'égalité de `Categorie` sur son nom ;
- l'entité `Commande` : `reference` obligatoire et unique (20), `dateCommande` (par défaut maintenant), `statut` en texte
  (`BROUILLON` par défaut), un `client` obligatoire (colonne `client_id`) et une liste de lignes triées par identifiant.
  La commande **possède** ses lignes : tout ce qui arrive à la commande se propage aux lignes, et une ligne retirée de
  la liste doit être supprimée de la base. Méthodes `ajouterLigne(produit, quantite)`, `retirerLigne(ligne)` et
  `getMontantTotal()` ;
- l'entité `LigneCommande`, table `ligne_commande` : une `commande` et un `produit` obligatoires (colonnes `commande_id`
  et `produit_id`), une `quantite`, et un `prixUnitaire` **copié depuis le prix du produit au moment de la création de la
  ligne**. Méthode `getSousTotal()` ;
- côté `Client`, la liste de ses commandes, **sans aucune cascade** ;
- le service `Tp3Service` :
  - `creerJeuDeDonnees()` : un client, deux produits (riz 25 kg à 18 500, huile 5 L à 7 500) rangés dans une catégorie
    « Alimentaire », et une commande de 2 riz et 3 huiles. Les lignes ne doivent **pas** être enregistrées une par une.
    Renvoie les identifiants du client et de la commande ;
  - `retirerPremiereLigne(commandeId)` ;
  - `trouver(commandeId)` et `nombreDeLignes(commandeId)` ;
  - `supprimerClient(clientId)`.
- le runner : crée le jeu de données, retire la première ligne et affiche le nombre de lignes restantes, récupère la
  commande puis parcourt ses lignes **une fois la transaction terminée**, et enfin tente de supprimer le client.

À observer :

- un seul enregistrement de la commande produit 1 `INSERT` dans `commande` et 2 dans `ligne_commande` ;
- retirer une ligne de la liste produit un `DELETE` au commit, sans aucun appel de suppression ; il reste 1 ligne ;
- parcourir les lignes hors transaction lève une `LazyInitializationException` ;
- supprimer un client qui a des commandes est refusé par la clé étrangère. Conclusion à retenir : on désactive un
  client (statut `INACTIF`), on ne le supprime pas.

### Jeu de données commun aux TP 4 à 9

À partir du TP 4, les TP ont besoin de données. Créez un service `JeuDeDonneesService` avec :

- `creerClientsEtCatalogue()` : crée 3 clients — Traore Moussa (Abidjan, `ACTIF`), Kone Awa (Bouaké, `ACTIF`),
  Diallo Fatou (Abidjan, `PROSPECT`) — et 3 produits — `RIZ-25` (18 500, stock 100), `HUI-5` (7 500, stock 50),
  `SUC-50` (32 000, stock 40). Renvoie les identifiants du premier client et des trois produits ;
- `creerCommandes(nombre)` : crée des commandes référencées `CMD-0001`, `CMD-0002`…, attribuées aux clients à tour de
  rôle et espacées de 3 jours en remontant dans le temps (la première est datée d'aujourd'hui). En numérotant les
  commandes *i* = 0, 1, 2…, chacune a deux lignes : le produit de rang *i* (en parcourant le catalogue en boucle) en
  quantité (*i* modulo 5) + 1, et le produit suivant en quantité 1 ;
- `genererClientsAvecCommandes(nbClients, commandesParClient)` (à partir du TP 6) : génère un grand nombre de clients
  ayant chacun plusieurs commandes d'une ligne, avec des emails et références uniques. Pensez à ne pas laisser grossir
  le contexte de persistance.

### TP 4 – Requêtes (profil `tp4`)

**Objectif :** écrire des requêtes JPQL, projeter dans un DTO, paginer, charger un graphe en une requête.

À créer :

- le DTO `CommandeResumeDto` (un `record`) : `id`, `reference`, `dateCommande`, `nomClient`, `montant` ;
- le repository `CommandeQueries`, écrit avec l'`EntityManager` et **sans transaction**, qui propose :
  1. `clientParEmail(email)` ;
  2. `commandesDuClient(clientId)`, de la plus récente à la plus ancienne ;
  3. `commandesEntre(debut, fin)` ;
  4. `commandesAuDessusDe(montantMinimum)` : renvoie des `CommandeResumeDto`, le montant étant calculé **par la base** ;
  5. `page(page, taille)` et `compterCommandes()` ;
  6. `detailComplet(id)` : la commande avec son client, ses lignes et leurs produits, **en une seule requête** ;
  - `resumesEntre(debut, fin)` : des `CommandeResumeDto` sur une période ;
  - `rechercherClients(nom, ville, statut)` : recherche dont **chaque critère est facultatif** (API Criteria) ;
  - `augmenterPrixProduitsEnStock(facteur)` : une mise à jour en masse, qui renvoie le nombre de produits modifiés ;
  - `produitsEnRupture(seuil)` : en SQL natif.
- le service `Tp4Service`, en lecture seule (sauf la mise à jour en masse), qui délègue au repository ;
- le runner : crée les clients, le catalogue et 12 commandes, puis appelle chaque requête et affiche le résultat.

À observer :

- résultats attendus : Traore Moussa possède les commandes `CMD-0001`, `CMD-0004`, `CMD-0007` et `CMD-0010` ;
  8 commandes atteignent 50 000 ; la page 1 (taille 5) va de `CMD-0006` à `CMD-0010` sur 12 ; l'augmentation de 5 %
  modifie 3 produits ; seul `SUC-50` a un stock inférieur à 45 ;
- la requête DTO ne sélectionne que les colonnes utiles, avec un `GROUP BY` et un `HAVING` ;
- la pagination se traduit par un `LIMIT` dans le SQL ;
- le détail complet tient en **un** `SELECT` avec jointures, et ses lignes restent lisibles après la fin de la transaction ;
- question : après la mise à jour en masse, que valent les prix des produits déjà chargés en mémoire ? Que faut-il faire ?

### TP 5 – Transactions (profil `tp5`)

**Objectif :** vérifier le « tout ou rien », et tomber dans le piège des exceptions contrôlées.

À créer :

- l'exception **contrôlée** `StockInsuffisantException` ;
- le service `CommandeService` :
  - `passerCommande(clientId, quantitesParProduit)` : reçoit une table « identifiant de produit → quantité », retire le
    stock de chaque produit, crée la commande et ses lignes, renvoie l'identifiant. Refuse un client inconnu. La
    référence de la commande est générée automatiquement et doit être unique ;
  - `passerCommandeSansRollback(...)` : même traitement, mais le stock insuffisant est signalé par
    `StockInsuffisantException`, sans réglage particulier de la transaction ;
  - `passerCommandeAvecRollback(...)` : identique, en demandant explicitement l'annulation sur cette exception ;
  - `stock(produitId)` et `nombreDeCommandes()`, en lecture seule.
- le runner, à partir des clients et du catalogue :
  1. commande valide : 2 riz et 1 huile ;
  2. commande impossible avec `passerCommande` : 1 riz **puis** 9 999 huiles (l'ordre de traitement compte : utilisez
     une table qui conserve l'ordre d'insertion) ;
  3. la même commande impossible avec `passerCommandeSansRollback` ;
  4. la même avec `passerCommandeAvecRollback`.

  Après chaque étape, affichez le stock de riz et le nombre de commandes.

À observer :

- étape 1 : stock de riz à 98, 1 commande ;
- étape 2 : l'exception annule tout, le stock de riz est inchangé et aucune commande n'est ajoutée ;
- étape 3 : le stock de riz passe à **97 alors qu'aucune commande n'a été créée** : la transaction a été validée
  malgré l'exception. Expliquez pourquoi ;
- étape 4 : le stock est de nouveau inchangé.

### TP 6 – Performance (profil `tp6`)

**Objectif :** mesurer le problème N+1, puis le corriger de trois façons.

Préparation : dans `application.properties`, activez les statistiques Hibernate, fixez une taille de lot JDBC à 50 et
activez le tri des insertions et des mises à jour.

À créer :

- le DTO `ClientNbCommandes` : `nom`, `nbCommandes` ;
- le service `RapportService`, en lecture seule, qui produit **le même rapport** (nom du client et nombre de ses
  commandes, pour tous les clients) de quatre façons :
  - `rapportClientsNPlus1()` : version naïve, qui charge les clients puis parcourt leurs commandes ;
  - `rapportClientsJoinFetch()` : avec une jointure de chargement, sans perdre les clients sans commande ;
  - `rapportClientsEntityGraph()` : avec un graphe d'entités, sans modifier la requête ;
  - `rapportClientsDto()` : avec une projection DTO et un comptage fait par la base.
- le service `ImportService` avec `importerProduits(nombre)` : enregistre *nombre* produits en vidant régulièrement le
  contexte de persistance (tous les 50) ;
- le runner : génère 100 clients ayant 3 commandes chacun, puis, pour chaque version du rapport, remet les statistiques
  à zéro, exécute le rapport et affiche le **nombre de requêtes SQL préparées**. Il termine par l'import de
  1 000 produits, en affichant la durée et le nombre de requêtes.

À observer :

- version naïve : **101 requêtes** pour 100 clients (1 + 100) ;
- les trois corrections : **1 requête** chacune ;
- l'import émet 1 000 requêtes malgré la taille de lot : expliquez le lien avec la stratégie de génération des identifiants ;
- question : que se passe-t-il si `RapportService` n'est pas transactionnel ?

### TP 7 – Cache (profil `tp7`)

**Objectif :** mettre en cache un référentiel stable et le résultat d'une requête.

Préparation :

- ajoutez au `pom.xml` les dépendances `org.hibernate.orm:hibernate-jcache` (dans la version d'Hibernate gérée par
  Spring Boot) et `org.ehcache:ehcache` 3.10.8 avec le classifieur `jakarta` ;
- dans `src/main/resources/application.properties`, activez le cache de second niveau avec la fabrique de régions
  `jcache` et le fournisseur JCache d'Ehcache, activez le cache de requête, choisissez le mode de cache partagé
  « sélectif » (seules les entités marquées sont mises en cache) et demandez la création automatique des caches
  manquants.

À créer :

- rendez `Categorie` éligible au cache de second niveau, en lecture-écriture ;
- le service `CategorieService` :
  - `creer(nom)` : renvoie l'identifiant ;
  - `nom(id)` : renvoie le nom d'une catégorie, en lecture seule ;
  - `lister()` : renvoie les noms triés, la requête étant marquée comme pouvant être mise en cache.
- le runner : crée trois catégories, **vide le cache** et remet les statistiques à zéro, puis lit deux fois la même
  catégorie (deux appels, donc deux transactions) et affiche le nombre de requêtes SQL, de lectures servies par le
  cache et de mises en cache. Il fait ensuite de même avec deux appels à `lister()`.

À observer :

- deux lectures de la même catégorie : **1 requête SQL, 1 lecture servie par le cache, 1 mise en cache** ;
- deux appels à `lister()` : **1 requête SQL, 1 succès du cache de requête** ;
- question : pourquoi faut-il vider le cache juste après la création des catégories pour obtenir ces chiffres ?

### TP 8 – Architecture Service / Repository, Spring Data JPA (profil `tp8`)

**Objectif :** séparer les responsabilités, ne plus exposer les entités, découvrir Spring Data JPA.

À créer :

- le DTO `ClientDto` : `id`, `nom`, `email`, `telephone`, `ville`, `statut`, avec une méthode de fabrique à partir d'un
  `Client` (attention au client sans adresse) ;
- l'exception métier **non contrôlée** `MetierException` ;
- le repository `JpaClientRepository`, écrit à la main avec l'`EntityManager` : recherche par email et enregistrement.
  Il sert de point de comparaison ;
- le repository Spring Data `ClientRepository`, qui fournit sans implémentation de votre part :
  - `findByEmail(email)`, par requête dérivée du nom de la méthode ;
  - `findByStatut(statut, pagination)`, paginée ;
  - `findWithCommandesById(id)`, qui charge le client **et** ses commandes en une requête ;
  - `compterCommandesParClient()`, une requête explicite renvoyant des `ClientNbCommandes` ;
  - `changerStatutAvant(statut, date)`, une mise à jour en masse des clients inscrits avant une date.
- le service `ClientService` :
  - `creer(nom, email, adresse)` : refuse un email déjà utilisé (`MetierException`), renvoie un `ClientDto` ;
  - `modifier(id, nom, telephone)` : sans appeler la méthode d'enregistrement du repository ; client inconnu refusé ;
  - `rechercher(nom, ville, statut)` : s'appuie sur la recherche multicritère du TP 4 ;
  - `parStatut(statut, page, taille)` : page de `ClientDto` triée par nom ;
  - `detailAvecCommandes(id)` : nom du client et nombre de ses commandes ;
  - `nombreDeCommandesParClient()` ;
  - `desactiverInscritsAvant(date)` : renvoie le nombre de clients passés à `INACTIF`.
- le runner : crée les clients, le catalogue et 6 commandes, puis crée le client Bamba Ali, tente un doublon d'email,
  modifie Traore Moussa, affiche la page 0 (taille 2) des clients `ACTIF`, le détail de Traore Moussa, le nombre de
  commandes par client, et désactive les clients inscrits avant demain.

À observer :

- résultats attendus : le doublon est refusé par votre règle métier **avant** toute erreur SQL ; 2 clients `ACTIF` sur
  1 page ; Traore Moussa a 2 commandes ; le comptage donne 2, 2, 2 et 0 ; 4 clients sont désactivés ;
- la modification produit un `UPDATE` sans appel d'enregistrement ;
- le détail avec commandes tient en **une** requête ;
- aucune entité ne sort des services : les runners ne manipulent que des DTO.

### Module 9 – Mini-projet de synthèse (profil `projet`)

**Objectif :** assembler tout ce qui précède en 11 fonctionnalités, exposées par des services et déroulées par un runner.

À créer en complément :

- le DTO `ClientCaDto` : `nom`, `nbCommandes`, `chiffreAffaires` (zéro, et non « rien », pour un client sans commande) ;
- le service `ProduitService` avec `creer(code, nom, prix, stock)`, qui refuse un prix négatif ou nul ;
- le repository Spring Data `CommandeRepository` : `resumesDuClient(clientId)` et `resumes(pagination)`, qui renvoient
  des `CommandeResumeDto` ;
- dans `CommandeService` : `creer(clientId)`, `ajouterProduit(commandeId, produitId, quantite)` (uniquement sur une
  commande à l'état `BROUILLON`, avec retrait du stock), `montantTotal(commandeId)` calculé par la base,
  `commandesDuClient(clientId)` et `lister(page, taille)` ;
- dans `RapportService` : `chiffreAffairesParClientNaif()` et `chiffreAffairesParClient()`, qui renvoient tous deux des
  `ClientCaDto` pour **tous** les clients.

Les 11 fonctionnalités à dérouler dans le runner :

1. créer un client (Ouattara Salif, Abidjan) ;
2. modifier ce client (ajout d'un téléphone) ;
3. rechercher les clients d'Abidjan ;
4. créer deux produits : café 1 kg à 6 500 (stock 30) et thé vert 500 g à 3 000 (stock 20) ;
5. créer une commande pour le client ;
6. y ajouter 2 cafés et 3 thés ;
7. afficher le montant total de la commande ;
8. lister les commandes du client, en DTO ;
9. ajouter le jeu de données commun (clients, catalogue, 12 commandes) puis afficher la page 0 (taille 5) de toutes
   les commandes, avec le nombre total de commandes et de pages ;
10. passer une commande en une seule transaction (1 café puis 999 thés) et vérifier l'annulation complète ;
11. générer 50 clients ayant 3 commandes chacun, puis mesurer le nombre de requêtes du rapport « chiffre d'affaires par
    client » en version naïve et en version optimisée, et vérifier que les deux donnent les mêmes résultats.

À observer :

- étape 7 : 22 000 ;
- étape 9 : 13 commandes, 3 pages ;
- étape 10 : le stock de café reste à 28 ;
- étape 11 : plus de 200 requêtes pour la version naïve, **1 seule** pour la version optimisée, sur 54 clients et avec
  des résultats identiques. Attention au nombre de commandes dans la version optimisée : une commande de plusieurs
  lignes ne doit être comptée qu'une fois.

## Livrable de fin de formation

À remettre en fin de formation, dans votre dépôt Git :

- **le projet complet** : les 8 TP et le mini-projet se lancent chacun par leur profil, et `mvn test` passe ;
- **un commit par module** (modules 1 à 9), avec un message qui nomme le module ;
- **un fichier `REQUETES.md`** à la racine, qui liste les principales requêtes JPQL du projet : pour chacune, son rôle,
  son texte, et le nombre de requêtes SQL qu'elle produit.
