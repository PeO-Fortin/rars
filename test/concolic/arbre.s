# TP3 - Arbre
# Pierre-Olivier Fortin FORP22028608 (groupe 050)
#
# Cette bibliothèque expose des routines pour gérer des arbres généalogiques.
# Elle comprend les routines suivantes:
# - newP: Ajouter une nouvelle personne
# - mGen: Déterminer le niveau de génération d'une personne
# - printLignePere: Afficher la lignée paternelle
# - addEnf: Ajouter un enfant à un parent
# - sizeArbre: Déterminer le nombre de descendants

.global newP
.global mGen
.global printLignePere
.global addEnf
.global sizeArbre

# Appels systeme utilisés
.eqv Sbrk, 9
	
# Structure d'une personne
.eqv pPere, +0				# champ `pere` (dword), adresse du noeud père ou null
.eqv pMere, +8				# champ `mere` (word), adresse du noeud mère ou null
.eqv pAnnee, +16			# champ `annee` (dword), année de naissance
.eqv pEnfants, +24			# champ `enfants` (dword), adresse de la liste d'enfants ou null
.eqv pTaille, 32			# taille d'une personne en octets

# Structure d'une liste enfant
.eqv eEnfant, +0			# champ 'enfant' (dword), adresse du noeud enfant
.eqv eSuivant, +8			# champ 'suivant' (dword), adresse du noeud suivant ou null
.eqv ePrecedent +16			# champ 'precedent' (dword), adresse du noeud précédent ou null
.eqv eTaille, 24			# taille d'un noeud de la liste

.text
# newP #######################################################
	# Crée une nouvelle personne avec allocation dynamique dans le tas.
	# IN: 	a0: année de naissance
	#	a1: adresse du père (0 si non identifié)
	#	a2: adresse de la mère (0 si non identifié)
	# OUT:  a0: l'adresse du nouveau nœud personne, ou 0 en cas d'erreur d'allocation
newP:	
	mv t0, a0
	
	# Allocation de la mémoire pour la nouvelle personne
	li a7, Sbrk
	li a0, pTaille
	ecall
	
	beqz a0, finNewP		# if (allocation réussie) {
	# Initialisation des champs
	sd a1, pPere(a0)
	sd a2, pMere(a0)
	sd t0, pAnnee(a0)
	sd zero, pEnfants(a0)		# }
	
finNewP:
	ret

# mGen #######################################################
	# Détermine le niveau de génération d'une personne dans l'arbre généalogique.
	# IN:	a0: l'adresse du nœud personne
	# OUT:	a0: le niveau de génération (nombre d'étages dans l'arbre ascendant)
mGen:	
	mv a3, a0
	li a0, 0			# Conteneur du niveau généalogique maximum
	li a1, 0			# Conteneur du niveau généalogique courant	
debutMGen:
	# Prologue
	addi sp, sp, -16
	sd ra, 0(sp)
	sd a2, 8(sp)
	
	beqz a3, finMGen		# if {personne != null) {
	mv a2, a3
	
	ld a3, pPere(a2)			# Déplacement vers la lignée paternelle
	addi a1, a1, 1				# Montée d'un niveau généalogique
	call max
	call debutMGen
	addi a1, a1, -1				# Redescente d'un niveau généalogique
	
	ld a3, pMere(a2)			# Déplacement vers la lignée maternelle
	addi a1, a1, 1				# Montée d'un niveau généalogique
	call max
	call debutMGen
	addi a1, a1, -1				# Redescente d'un niveau généalogique
					# }
finMGen:
	# Epilogue
	ld a2, 8(sp)
	ld ra, 0(sp)
	addi sp, sp, 16

	ret

# printLignePere #############################################
	# Affiche l'année de naissance de chaque membre de la lignée paternelle.
	# IN:	a0: l'adresse du nœud personne (enfant)
	# OUT:	rien
printLignePere:
	# Prologue
	addi sp, sp, -16
	sd ra, 0(sp)
	sd s0, 8(sp)
	
loopPLP:
	mv s0, a0
	beqz s0, finPLP			# while (personne != null) {
	
	# Affichage de l'année de naissance
	ld a0, pAnnee(s0)
	call printInt
	li a0, ' '
	call printChar
	
	ld a0, pPere(s0)			# Passer au prochain père
	
	j loopPLP			# }
	
finPLP:	li a0, '\n'
	call printChar

	# Epilogue
	ld s0, 8(sp)
	ld ra, 0(sp)
	addi sp, sp, 16
	
	ret

# addEnf #####################################################
	# Ajoute un enfant à un parent dans l'arbre généalogique.
	# IN:	a0: l'adresse du nœud enfant
	#	a1: l'adresse du nœud parent
	# OUT:	a0: 1 si succès, 0 si erreur
addEnf:	
	mv a2, a0
	
	# Vérification si parent ou enfant != null, sinon retourne le code d'erreur (0)
	li a0, 0
	beqz a1, finAddEnf	
	beqz a2, finAddEnf
	
	# Création du maillon pour la liste d'enfants
	li a7, Sbrk
	li a0, eTaille
	ecall
	mv a3, a0
	sd a2, eEnfant(a3)		# Sauvegarde de l'adresse du noeud enfant dans le maillon
	sd zero, eSuivant(a3)	
	
	li a0, 1			# Code de retour: succès (1)
	ld a4, pEnfants(a1)		# Récupération de la liste d'enfants du parent
	
	# Ajout du maillon à la liste
	bnez a4, addListe		# if (listeEnfants == null) {
	sd zero, ePrecedent(a3)		
	sd a3, pEnfants(a1)
	j finAddEnf				
					
addListe:				# } else {
	ld a5, eSuivant(a4)			# while (suivant != null) {
	beqz a5, suiteAddListe			 
	mv a4, a5
	j addListe				# }
	
suiteAddListe:
	sd a4, ePrecedent(a3)
	sd a3, eSuivant(a4)		
					# }
finAddEnf:
	ret

# sizeArbre ##################################################
	# Calcule le nombre total de descendants d'une personne.
	# IN:	a0: l'adresse du nœud personne
	# OUT:	a0: le nombre total de descendants
sizeArbre:
	mv a1, a0
	li a0, 0			# Conteneur du compteur
	
debutSA:
	# Prologue
	addi sp, sp, -16
	sd ra, 0(sp)
	sd a2, 8(sp)
	
	beqz a1, finSA			# Fin si personne == null
	
	ld a2, pEnfants(a1)		# Chargement du premier maillon de la liste d'enfants
	beqz a2, finSA			# if (maillon != null) {
suiteSA:
	ld a1, eEnfant(a2)			# Chargement de la personne pointée par le maillon
	addi a0, a0, 1
	call debutSA			# }
	
	ld a2, eSuivant(a2)		# Chargement du maillon suivant de la liste d'enfants
	beqz a2, finSA			# if (maillon != null) {
	call suiteSA			# }

finSA:	# Epilogue
	ld a2, 8(sp)
	ld ra, 0(sp)
	addi sp, sp, 16
	
	ret

# max ########################################################
	# Détermine le maximum entre deux nombres
	# IN:	a0: le premier nombre
	#	a1: le deuxieme nombre
	# OUT:	a0: le max des deux nombres
max:	
	bgt a0, a1, finMax
	mv a0, a1
finMax:	ret
