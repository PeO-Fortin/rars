





# Rapport – Projet de recherche de 1er cycle

## Correction automatique de travaux étudiants dans un langage assembleur — RISC-V

Pierre-Olivier Fortin
Université du Québec à Montréal
23 août 2026

## Contexte

Le programme RARS (RISC-V Assembler and Runtime Simulator), un assembleur, simulateur et environnement d’exécution de programmes en langage assembleur RISC-V, a été choisi comme base pour l’implémentation du moteur d’exécution concolique. Utilisé dans le cours *Organisation des ordinateurs et assembleur* et maintenu à l’UQAM, en plus d’être open-source, RARS s’est imposé comme un choix logique, nous permettant d’avoir directement accès à un environnement complet permettant d’exécuter des programmes en RISC-V. Pour le présent projet, nous étions particulièrement intéressés par l’accès à un assembleur RISC-V et à toutes les représentations intermédiaires des programmes.

En préalable au projet de recherche, une première étape avait été réalisée afin d’implémenter des instructions simples, s’arrêtant au corpus enseigné avant l’énoncé du premier travail pratique à réaliser par les étudiants. Les représentations concrètes et symboliques des instructions suivantes avaient donc été implémentées :

- *Opérations arithmétiques* : add, sub, mul, div (incluant les variantes avec valeurs immédiates)
- *Opérations binaires* : xor, and, or, sll, srl, sra (incluant les variantes avec valeurs immédiates)
- *Branchements*: bge, blt, beq, bne, jal
- *Appels système* : readInt, readChar, printInt, printIntHex, printIntBinary, printChar, exit

## Gestion de la mémoire

#### Création des tableaux de mémoire

La première étape fut de représenter les différentes sections de la mémoire présente dans le simulateur RARS, de façon concolique. La décision a été prise de simplement créer des tableaux linéaires contenant des objets ConcolicValue. Comme la taille de la mémoire offerte dans RARS est plutôt limitée (4 Mo pour la section Data, incluant le tas, et 8 Ko pour la section de la pile), il n’est pas apparu nécessaire d’optimiser l’utilisation de l’espace mémoire autre que de laisser une initialisation à Null pour les cases vers lesquels aucun stockage n’a été fait par le programme.

Par ailleurs, le stockage simule au plus près le comportement de RARS en représentant les opérations de petit-boutisme aussi bien au niveau concret que symbolique. Chaque case représente donc spécifiquement la valeur concrète et symbolique d’un seul octet, facilitant la gestion d’une éventuelle différence entre la taille de la valeur stockée et celle chargée dans un registre ultérieurement.


#### Adresses symboliques

La détermination des adresses symboliques lors de l’exécution est une problématique majeure dans l’exécution symbolique des programmes, menant très vite à une explosion combinatoire. Plusieurs techniques ont été développées afin de pallier à cette problématique, où nous sacrifions en précision afin de tout de même réussir à compléter les exécutions symboliques avec des résultats satisfaisants.[^1] [^2] Comme RARS offre un environnement stable et prédictif, il nous est possible de déterminer l’adresse de toute demande de mémoire sur le tas, ainsi que tout décalage sur la pile, si l’ensemble des variables utilisées lors de l’appel sont des constantes. Si le programme fait un appel mémoire avec une adresse qu’il nous est impossible de précalculer, par exemple, un appel mémoire sur une valeur donnée par l’utilisateur, nous avons décidé de terminer l’exécution en cours. Pour le présent moteur symbolique, en se basant sur les exigences passées en matière de travaux pratiques en RISC-V, ce type d’adressage ne devrait pas se retrouver dans les travaux des étudiants et serait donc, de toute façon, considéré comme une erreur de programmation, rendant les sorties du programme invalides.

#### Implémentation des instructions

Avec ce nouveau module du moteur d’exécution concolique, il a été possible d’implémenter un ensemble d’instructions liées à l’utilisation de la mémoire:

- *Chargement* : lb, lbu, lh, lhu, lw, lwu, ld
- *Stockage* : sb, sh, sw, sd
- *Appel système* : Sbrk, readString, printString
- *Branchement*: jalr

## Heuristiques d’exploration

Afin de s’adapter aux défis d’exploration variant d’un programme à un autre, plusieurs heuristiques d’exploration ont été implémentées, rendant le moteur d’exploration concolique modulable par l’utilisateur selon ses besoins.


#### Implémentation d’un graphe de flot de contrôle (CFG)

Jusqu’à maintenant, le moteur d’exécution concolique utilise l’adresse de chacune des instructions, appelé compteur ordinal, afin de déterminer la prochaine instruction à exécuter. Bien que fonctionnelle, cette méthode de parcours montrait des limitations dans l’implémentation de différentes heuristiques d’exploration.

Un CFG a donc été implémenté dans RARS afin d’ouvrir de nouvelles possibilités. La division des différents blocs d’instructions composant le CFG se fait à chaque branche (instructions bge, blt, beq, bne) et à chaque saut inconditionnel (instructions jal, jalr). Comme l’instruction jalr, se base sur la valeur d’un registre pour déterminer le prochain bloc d’instructions à exécuter et que l’analyse statique fait pendant la création du CFG ne se fait que de surface, le prochain bloc à exécuter à la suite de jalr ne sera déterminé que lors de l’exécution et reste indéterminé dans le CFG.


#### Heuristique Distance to exit

En plus des deux heuristiques d’exploration déjà présentes dans le moteur d’exécution, soit *Parcours en largeur (BFS - Breadth-First Search)* et *Parcours en profondeur (DFS – Depth-First Search)*, la première heuristique d’exploration implémentée vise à se rendre à l’instruction de sortie du programme en priorité lors des exécutions. Comme mentionné ci-dessus, l’analyse statique lors de la création du CFG reste superficielle, donc nous ne pouvons déterminer avec certitude quel appel système mènera à la sortie du programme. Il a donc été décidé de viser l’instruction `addi x17, x0, 10` qui correspond au chargement de l’appel système de sortie dans le registre, ce qui, dans la forte majorité des cas, est exécuté parmi les dernières instructions du programme. Selon les besoins des utilisateurs du moteur d’exécution concolique, il serait peut-être pertinent d’ouvrir la possibilité d’appliquer cette heuristique à une instruction déterminée par l’utilisateur.


#### Heuristique Random

Cette heuristique choisit de façon complètement aléatoire la prochaine branche d’exécution à explorer, parmi celles découvertes lors d’exécutions précédentes. Avec cette heuristique, bien qu’il soit maintenant impossible de prédire avec certitude l’ordre des exécutions, elle augmente les probabilités d’explorer des branches qui, avec d’autres heuristiques, auraient demandé un nombre important d’exécutions pour être atteintes.


#### Heuristique Coverage

L’heuristique d’exploration implémentée, basée sur la couverture, détermine, parmi l’ensemble des branches inexplorées découvertes, celle menant vers le bloc d’instructions le moins exploré jusqu’à maintenant. Cette heuristique s’appuie sur la prémisse que les sections moins explorées du code seraient plus susceptible de générer des comportements comprenant de plus grandes différences que ce qui a déjà été vu. Il est à noter que, tel que décrit, le choix s’appuie exclusivement le taux d’exploration du bloc d’instructions suivant le branchement et non pas sur le taux d’exploration d’une branche entière de l’arbre d’exploration. Ce choix est une conséquence de la construction de l’arbre au fil des explorations, ce qui ne permet pas d’estimer le nombre total de nœuds de la suite d’une branche qui n’a pas encore été explorée. Plusieurs voies sont encore à explorer pour optimiser l’heuristique afin d’augmenter la couverture de code du présent moteur. Le moteur d’exécution concolique Klee, par exemple, mesure un poids pour chacune des branches inexplorées basé, entre autres, sur la distance pour atteindre une instruction encore inexplorée [^3].


#### Heuristique Random-Coverage

Finalement, la dernière heuristique implémentée combine l’heuristique *Random* et *Coverage*. Une fois sur deux, le choix de la prochaine branche à explorer s’appuie sur l’heuristique effectuant un choix aléatoire, puis, la fois suivante, ce choix s’appuie sur l’heuristique fondée sur le taux de couverture d’un bloc d’instructions. Ceci permet d’offrir une plus grande variation dans le choix des chemins d’exploration et ainsi d’obtenir des résultats reflétant un plus grand éventail des comportements du programme analysé.

## Correcteur

#### Implémentation

Le module de correction instrumentalise le moteur d’exécution concolique afin de comparer les comportements entre un programme dit *Solution* et différents programmes dits *Étudiants*, qui répondent aux mêmes directives. Une première passe sur l’ensemble des programmes est faite afin de générer un jeu d’entrée couvrant l’ensemble des branches de tous les programmes ou bien un nombre d’entrées correspondant au nombre maximal d’exécutions des programmes, si celui-ci est atteint avant d’avoir couvert toutes les branches possibles.

Ce jeu d’entrée est ensuite utilisé sur le programme *Solution* afin de générer les sorties qui constitueront la référence du comportement attendu. L’exécution avec ce même jeu d’entrées est alors utilisée sur chacun des programmes étudiants et leurs sorties sont comparées avec les sorties de référence. Un rapport est finalement généré indiquant, pour chaque programme *Étudiant*, s’il génère exactement les mêmes sorties pour toutes les entrées et indique les disparités rencontrées.


#### Correction basée sur la sortie standard

L’option par défaut du correcteur s’appuie sur les sorties dirigées vers la sortie standard et sur les erreurs pouvant être provoquées par le programme. Ceci demande ainsi que le programme comporte ce type de sortie, mais également que ces sorties soient représentatives du comportement attendu par ledit programme. Des mesures ont été mises en place afin de gérer et distinguer chacun des caractères dits non imprimables de la table ASCII. Il est à noter par contre que des tests restent à être faits afin de garantir que le correcteur effectue parfaitement les comparaisons pour des sorties comprenant des caractères encodés en UTF-8.


#### Correction basée sur la sortie bitmap

Le correcteur offre l’option de baser plutôt la comparaison des sorties destinées à l’afficheur bitmap de RARS. Ce sont cette fois directement le contenu de l’espace mémoire utilisé pour cet afficheur qui est comparé afin de valider que son état est le même entre les deux programmes, lorsque l’exécution se termine. Pour que la correction soit valide, il faut que la configuration de l’afficheur bitmap soit la même pour les deux programmes et que l’état de l’espace mémoire visé ne soit pas changé avant la fin de l’exécution du programme. L’option de corriger un état intermédiaire n’est donc pas encore disponible.

## Résultats

#### Moteur d’exécution concolique

Le moteur concolique a montré des résultats intéressants. Pour des programmes simples, tel qu’un programme cherchant à trouver le maximum entre 3 nombres, il arrive à explorer l’ensemble du programme (couverture de 100% des blocs d’instructions) avec un minimum d’exécution (4).

Au niveau de travaux d’étudiants considérés comme ayant répondu aux attentes selon la correction de l’enseignant, nous observons une couverture en moyenne de 71.67% des blocs d’instructions. Par contre, pour des programmes n’ayant pas répondu aux spécifications, nous avons obtenu des résultats marquants, avec d’importantes variations entre les programmes avec un taux de couverture aussi bas que 3.03%.


#### Correcteur

Le correcteur a démontré qu’il arrivait à distinguer les comportements divergents pour une même entrée entre différents programmes. Basé sur un échantillon de dix programmes étudiants, pour les programmes respectant les spécifications données, le correcteur a également confirmé que les comportements étaient similaires entre les programmes étudiants et la solution de l’enseignant, en relevant tout de même des disparités pour des cas où les spécifications concernant certains types d’entrées n’étaient pas spécifiées dans l’énoncé du travail donné. Le correcteur a également invalidé les programmes qui avaient été considérés comme invalides par l’enseignant. Des tests supplémentaires seront toutefois à effectuer sur un plus grand échantillon afin d’avoir des données plus probantes.

## Limitations

#### Gestion des boucles

La gestion des boucles dans les programmes analysés a vite fait ressortir l’une des limitations du présent moteur d’exécution concolique. Présentement, de la façon dont l’arbre d’exécution est construit, chaque tour de boucle constitue une branche à part entière. Cette façon de faire provoque un grand nombre de duplications de nœuds, particulièrement si la boucle mène elle-même vers plusieurs branchements possibles. Une explosion combinatoire survient au fil des exécutions, pouvant remplir rapidement la mémoire disponible, forçant à limiter le nombre d’exécutions possibles du moteur. De plus, selon l’heuristique d’exploration et la nature du programme, il devient parfois difficile d’explorer l’ensemble des branches qui pourrait nous intéresser. Au fil des exécutions, nous avons observé dans certains cas, une difficulté du moteur à se diriger vers le bon nombre de tours de boucle générant des sorties que nous désirions analyser.


L’extrait de code suivant provient d’un programme demandé comme travail pratique dans le cours *Organisation des ordinateurs et assembleur*. L’étudiant devait programmer une calculatrice simple.


```
lire:	li a7, ReadChar

ecall


#Vérification des espaces blancs

li t0, ' '

beq a0, t0, lire

li t0, '\t'

beq a0, t0, lire

li t0, '\n'

beq a0, t0, lire

li t0, '\r'

beq a0, t0, lire


#Vérification des chiffres

li t0, '0'

blt a0, t0, oper

li t0, '9'

bgt a0, t0, oper


#If (caractère = chiffre) {

addi a0, a0, -0x30 	#Transformation du caractère en entiers

li s3, 1 		#Boolean = chiffre trouvé


#Gestion des dizaines

add t0, s1, s1

add t0, t0, t0

add t0, t0, s1

add s1, t0, t0

add s1, s1, a0


bnez s2, lire 		#Si le nombre est le premier de l'opération

mv s0, s1			#Chiffre lu = total

j lire 			# }
```

Cette section, située au début du programme, lit chacun des caractères de l’entrée standard afin de constituer la chaine à calculer.

Cette boucle de lecture comporte un grand nombre de branches internes. À l’usage, dû à l’explosion de nombre de branches de l’arbre, nous avons dû fortement limiter le nombre d’exécutions possibles par le moteur concolique, sous peine de voir le processus être simplement arrêté par le système d’exploitation causé par une surutilisation des ressources.

Afin de pallier à cette problématique, la première solution testée a été de limiter le nombre de tours de boucle pouvant créer de nouveaux nœuds. Nous avons ainsi pu augmenter de 30% le nombre d’exécutions possibles, passant de 50 à 65, mais ceci implique de sacrifier de la précision dans le suivi de l’arbre d’exécution. Une autre solution envisagée, inspirée par l’implémentation de Klee, est le passage d’un arbre d’exécution à un graphe d’exécution basé sur le contexte d’appel, ce qui limiterait les doublons. L’implémentation de cette solution n’a pas été complétée, car elle a un impact généralisé le fonctionnement du moteur concolique, mais les premiers essais ont pu montrer que ce serait une solution pouvant potentiellement améliorer l’utilisation de la mémoire lors de l’utilisation du moteur.


#### Gestion des contraintes

Une seconde limitation rencontrée concerne la gestion des contraintes, autant au niveau des ressources nécessaires dans leur résolution que la difficulté à atteindre des points spécifiques lorsqu’il y a un grand nombre de contraintes.

Au niveau des ressources, nous avons vu une utilisation importante de la puissance de calcul nécessaire à la résolution d’un grand nombre de contraintes. Au fil des exécutions, la puissance de calcul nécessaire augmente considérablement, en raison de la demande du solveur Z3. Pour l’instant, l’une des solutions mise en place a été de se concentrer sur la réduction des ressources demandées par le reste du moteur concolique en ne calculant que les nouvelles contraintes liées à la découverte d’une nouvelle branche, plutôt que de recalculer l’ensemble des contraintes de l’arbre à chaque exécution. En fixant les contraintes déjà connues, nous avons pu augmenter le nombre d’exécutions possibles, mais nous avons sacrifié la possibilité de pouvoir changer les contraintes d’une exécution à une autre. À première vue, nous pouvons croire que la perte de cette possibilité est triviale puisque les contraintes d’un programme ne devraient pas changer d’une exécution à une autre, mais c’était une solution que nous avions mise en place pour contrebalancer la deuxième limitation dans la gestion des contraintes, solution que nous décrivons ci-dessous.

Afin d’illustrer la limitation rencontrée, nous allons reprendre l’extrait de code de la calculatrice, présenté antérieurement. Cette calculatrice lit chacun des caractères indépendamment et impose des contraintes sur les caractères menant ultimement à la résolution d’une équation. Par exemple, pour résoudre une addition, une chaîne de caractères acceptable aurait minimalement la forme suivante : \<nombre> <+> \<nombre> <=>. Lors de l’exécution concolique du programme, nous avons pu observer que le moteur, et ce, peu importe l’heuristique implémentée choisie, a besoin d’un grand nombre d’exécutions pour atteindre une forme menant à la résolution d’une équation. Ceci est dû au fait que le grand nombre de contraintes crée incidemment un grand nombre de possibilités de chemin d’exécution et que plusieurs de ces chemins mènent vers des chaînes de caractères qui n’ont pas la forme recherchée. Pour pallier à ce problème, nous avons limité le nombre de caractères permis à seulement des caractères imprimables, augmentant ainsi les chances de fournir un caractère menant vers la chaine désirée. Par contre, par définition, cette solution ne nous permettait plus d’analyser le comportement du programme pour des caractères non-imprimables. Nous avons donc implémenté un changement de contraintes pour les dernières exécutions, ouvrant à ce moment la porte à l’ensemble des caractères possibles. Nous avons alors observé des résultats plus probants dans l’atteinte de chaînes correspondant à une utilisation dite normale du programme. Par contre, avec l’implémentation de la gestion des contraintes qui évite le recalcul à chaque exécution, il ne nous est plus possible de modifier cette contrainte sur les caractères pour les dernières exécutions. Il serait donc intéressant de poursuivre le travail pour trouver une solution intermédiaire qui combinerait les deux approches d’optimisation explorées.


#### Instructions à implémenter

L’ensemble des instructions utilisées par les demandes des travaux étudiants des dernières années sont supportées par le proésent moteur concolique. Par contre, ceci ne couvre pas l’ensemble des instructions possibles du langage RISC-V. À titre d’exemple, le présent moteur ne supporte pas les instructions relatives aux nombres flottants, ni celles relatives à la gestion des interruptions. Afin de rendre ce moteur universel et utilisable par tout type de programme RISC-V, ces instructions devraient donc être implémentées.

## Conclusion

Cette recherche a permis le développement d’un correcteur de travaux en RISC-V, appuyé sur un moteur d’exécution symbolique. Par contre, nous avons fait face aux problématiques classiques de ce type de moteur, soit la gestion des boucles et la représentation de la mémoire symbolique. Il serait donc intéressant de poursuivre le travail afin de peaufiner le contrôle sur les différentes exécutions et obtenir des tests de correction qui s’adaptent plus aisément aux différentes formes que les programmes peuvent prendre.

[^1]: Martin Nowak, «Fine-Grain Memory Object Representation in Symbolic Execution», 34th IEEE/ACM International Conference on Automated Software Engineering (ASE), 2019 

[^2]: Daniil Kutz, «Towards Symbolic Pointers Reasoning in Dynamic Symbolic Execution», Ivannikov Memorial Workshop (IVMEM), 2021

[^3]: Roberto Baldoni, Emilio Coppa, Daniele Cono D'Elia, Camil Demetrescu, Irene Finocchi, «A Survey of Symbolic Execution Techniques», ACM Computing Surveys, v51, n3, 2019, p.8.
