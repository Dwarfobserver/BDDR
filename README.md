 
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

 - Hadoop 2.7.0+ installé dans C:/hadoop
 - Apache Spark 2.3.0+
 - JDK 1.8.0+
 - Scala 2.11.12
 - Node.js

### Exercice 1 :

Récupérer monstres (node) -> Générer les sorts (scala)
