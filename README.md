 
# Base de données réparties
 
Etudiants :
 
 - Sidney Congard CONS07039601
 - Nicolas Zerbib ZERN25039502
 
## TP1 - MapReduce
 
### Prérequis :
 
 - Python 3.6+ avec pymongo installé
 - MongoDB Community Server
 - Visual Studio 2017
 - Node.js
 
### Exercice 1 :
 
Pour récupérer les sorts, ouvrez une console dans le dossier Exercice1 et exécutez la commande "python ./get_spells.py". Cela génère le fichier spells.json qui sera ensuite utilisé pour remplir les bases de données MongoDB et SQLite.
 
Pour remplir la base de données MongoDB et effectuer le map reduce pour sélectionner les sorts utilisables, ouvrez un terminal dans le dossier MongoDB, exécutez la commande "mongod" pour lancer la base de données puis dans un autre terminal exécutez "python ./print_usable_spells.py".
 
Pour effectuer la même chose sur SQLite, exécutez le projet sur Visual Studio. Si des erreurs apparaissent, rajoutez la définition de préprocesseur "_SILENCE_CXX17_OLD_ALLOCATOR_MEMBERS_DEPRECATION_WARNING" (Projet -> propriétés -> C/C++ -> Préprocesseur -> Définitions de préprocesseur) et utilisez la norme de C++17 (Projet -> propriétés -> C/C++ -> Langage -> Norme du langage C++).
 
### Exercice 2 :
 
Pour effectuer le pagerank sur les données de pages fournies, placez-vous dans le dossier Exercice2 puis exécutez la commande "npm install" pour installer les dépendances suivi de "node index.js".

## TP2 - Apache Spark

### Prérequis :

 - Hadoop 2.7.0+ installé dans C:/hadoop (avec C:/hadoop/bin/winutils.exe)
 - Apache Spark 2.3.0+
 - JDK 1.8.0+
 - Scala 2.11.12
 - Node.js

### Exercice 1 :

The fichier 'index.js' va récupérer les données des monstres et les stocker dans le json  'monsters.json'. Afin d'accélérer la recherche, on utilise une TaskQueue qui va premettre au maximum 100 connexions simultanées, pour obtenir les données de façon asynchrone sans pour autant faire timeout les requêtes.

Le projet 'ScalaRDD' va ouvrir le fichier json et le parser de façon asynchrone (pendant que Spark se lance). Ensuite, il va faire une batch view avec le code suivant :

```scala

// Monster = (name: String, spells: List[String])

val rddSpells = rddMonsters
        .flatMap(monster => {
            for (spell <- monster._2)
                yield (spell, monster._1)
        })
        .groupByKey()

// Spell = (name: String, monsters: List[String])

```

### Exercice 2 :

The projet lance une interface graphique qui permet de créer, sauvegarder et charger des scènes. Chaque créature est détaillée dans le fichier 'actor.json', qui est parsée par le moteur.

La synchronisation entre la fenêtre et le moteur est faite grâce à une classe 'Channel[List(Actor)]' : le moteur va, après chaque tour, passer dans le channel le nouvel état des acteurs. L'interface peut tenter de récupérer ces états de façon thread-safe, en obtenant un Option[List(Actor)] du channel (égal à None si il n'y en a pas de noubeaux).

Pour optimiser la sérialisation et le stockage des données, les créatures (nommées 'Actor') voient leur modèle (contenant leurs caractéristiques) stockée dans une map qui est disponible en tant que broadcast variable.

Lors de chaque tour, un 'aggregateMessages' est effectué : il associe à chaque acteur vivant sa cible privilégiée, déterminée par la distance qui les sépare et les points de vie restants de la cible. Ensuite, chaque acteur attaque sa cible, ou avance vers elle si il n'est pas à portée.
