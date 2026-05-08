# TP3 - Arbre généalogique
# Cynthia Tran TRAC83550401 (groupe 50)
#
# Ce programme implémente une bibliothèque de gestion d'arbres généalogiques qui permet de créer,
# manipuler et interroger des structures familiales représentées sous forme d'arbres.
# Il n'y a aucun bogue connu.

	# Étiquettes exportées
	.global newP
	.global mGen
	.global printLignePere
	.global addEnf
	.global sizeArbre
	
	# Appels systèmes RARS utilisés
	.eqv PrintInt, 1
	.eqv Sbrk, 9
	.eqv PrintChar, 11
	
	.text

########################################################################
# ROUTINE: newP
########################################################################
# Créer une nouvelle personne avec aollcation dynamique dans le tas
# * a0: année de naissance
# * a1: adresse du père (0 si non identifié)
# * a2: adresse de la mère (0 si non identifié)
# * résultat a0: adresse du nouveau noeud personne (0 si erreur d'allocation)
newP:
	# Prologue
	addi sp, sp, -16
	sd ra, 8(sp)
	sd a0, 0(sp)			# année en paramètre
	
	# Allocation dans tas
	li a7, Sbrk
	li a0, 32			# de 32 octets
	ecall
	beqz a0, err_alloc_P		# if (adresse alloc != 0) {

	# Initialisation champs
	mv t0, a0			# 	t0: adresse du nouveau noeud personne
	sd a1, 0(t0)			# 	père
	sd a2, 8(t0)			# 	mère
	ld t1, 0(sp)
	sd t1, 16(t0)			# 	année (récupérée de pile)
	li t2, 0
	sd t2, 24(t0)			# 	enfants: 0
	
	mv a0, t0			# 	return t0
	j fin_newP

# Erreur allocation
err_alloc_P:				# } else {
	li a0, 0			# 	return 0
					# }
fin_newP:
	# Épilogue
	ld ra, 8(sp)
	addi sp, sp, 16
	ret


########################################################################
# ROUTINE: mGen
########################################################################
# Détermine le niveau de génération d'une personne dnas l'arbre généalogique
# * a0: adresse du noeud personne
# * résultat a0: niveau de génération (nombre d'étages dans l'arbre ascendant)
mGen:
	# Prologue
	addi sp, sp, -24
	sd ra, 16(sp)
	sd s0, 8(sp)
	sd s1, 0(sp)
	
	# Vérification existence parents
	beqz a0, pas_parent		# if (personne != 0) {
	
	mv s0, a0			# 	s0: adresse noeud personne
	
	# Analyse du père
	ld a0, 0(s0)			# 	a0: père (paramètre)
	call mGen
	mv s1, a0			# 	s1: niveau père (max pour l'instant)
	
	# Analyse de mère
	ld a0, 8(s0)			# 	a0: mère (paramètre)
	call mGen
					#	résultat a0: niveau mère
	# Niveau max
	bge s1, a0, max_pere		# 	if(niveau_père <= niveau_mère) {
	mv s1, a0			# 		max = niveau_mère
					# 	} else {
max_pere:				# 		max = niveau_père
	addi a0, s1, 1			# 	}	
	j fin_analyse_niv		#	return max + 1

pas_parent:				# } else {
	li a0, 0			# 	return 0
					# }
fin_analyse_niv:
	# Épilogue
	ld s1, 0(sp)
	ld s0, 8(sp)
	ld ra, 16(sp)
	addi sp, sp 24
	ret


########################################################################
# ROUTINE: printLignePere
########################################################################
# Affiche l'année de naissance de chaque membre de la lignée paternelle
# * a0: adresse du noeud personne (enfant)
printLignePere:
	# Prologue
	addi sp, sp -8
	sd ra, 0(sp)
	
	mv t0, a0			# t0: adresse noeud personne (enfant)
	
# Boucle pour parcourir pères
boucle_afficher:
	beqz t0, fin_afficher		# while (courant != 0) {
	
	ld t1, 16(t0)			# 	t1: année courante
	
	# Affichage année + ' '
	li a7, PrintInt
	mv a0, t1
	ecall
	li a7, PrintChar
	li a0, 32			# 	' ' dans table ascii
	ecall
	
	ld t0, 0(t0)			# 	courant: père
	
	j boucle_afficher		# }

fin_afficher:
	# Affichage retour à la ligne
	li a7, PrintChar
	li a0, 10			# 	'\n' dans table ascii
	ecall
	
	# Épilogue
	ld ra, 0(sp)
	addi sp, sp, 8
	ret


########################################################################
# ROUTINE: addEnf
########################################################################
# Ajoute un enfant à un parent dans l'arbre généalogique
# * a0: adresse du noeud enfant
# * a1: adresse du noeud parent
# * résultat a0: 1 si succès, 0 si erreur
addEnf:
	# Prologue
	addi sp, sp, -8
	sd ra, 0(sp)
	
	mv t0, a0			# t0: enfant
	mv t1, a1			# t1: parent
	
	ld t2, 24(t1)			# t2: adresse tête liste enfants
	
	# Vérification existence enfant(s)
	beqz t2, pas_enfant		# if(enfant != 0) {
	
# Boucle pour parcourir enfants
boucle_enfants:
	ld t3, 8(t2)			# 	t3: enfant suivant
	beqz t3, fin_enfants		# 	while (suivant != 0) {
	mv t2, t3			#		courant: enfant suivant
	j boucle_enfants		# 	}

fin_enfants:
	# Allocation dans tas
	li a7, Sbrk
	li a0, 24			# 	de 24 octets
	ecall
	beqz a0, err_alloc_E		# 	if (adresse alloc != 0) {
	
	# Initialisation champs nouvel enfant
	mv t4, a0			# 		t4: adresse du nouveau noeud enfant
	sd t0, 0(t4)			# 		adresse nouvel enfant
	li t5, 0
	sd t5, 8(t4)			# 		suivant: 0
	sd t2, 16(t4)			# 		précédent: courant
	
	# Mise-à-jour noeud courant
	sd t4, 8(t2)			# 		suivant: adresse nouvel enfant

	# Retour avec succès (1)
	li a0, 1			#		return 1
	j fin_addEnf			# 	} else {
					#		return 0
					#	}
pas_enfant:				# } else {	
	# Allocation dans tas
	li a7, Sbrk
	li a0, 24			# 	de 24 octets
	ecall
	beqz a0, err_alloc_E		# 	if (adresse alloc != 0) {
	
	# Initialisation champs nouvel enfant
	mv t4, a0			# 		t4: adresse du nouveau noeud enfant
	sd t0, 0(t4)			# 		adresse nouvel enfant
	li t5, 0
	sd t5, 8(t4)			# 		suivant: 0
	sd t5, 16(t4)			# 		precedent: 0
	
	# Mise-à-jour tête de liste chaînée enfants
	sd t4, 24(t1)			#		tete: adresse nouvel enfant
	
	# Retour avec succès (1)
	li a0, 1			#		return 1
	j fin_addEnf			#	}

# Erreur allocation
err_alloc_E:				# } else {
	# Retour avec échec (0)
	li a0, 0			#	return 0
					# }
fin_addEnf:
	# Épilogue
	ld ra, 0(sp)
	addi sp, sp, 8
	ret


########################################################################
# ROUTINE: sizeArbre
########################################################################
# Calcule le nombre total de descendants d'une personne
# * a0: adresse du noeud personne
# * résultat a0: nombre total de descendant
# Récursivité pour itérer à travers les générations
# Boucle pour itérer à travers la liste d'enfants d'une génération
sizeArbre:
	# Prologue
	addi sp, sp -32
	sd ra, 24(sp)
	sd s0, 16(sp)
	sd s1, 8(sp)
	sd s2, 0(sp)
	
	mv s0, a0			# s0: adresse personne
	li s1, 0			# compteur: 0
	
	beqz s0, fin_size		# if(personne != 0) {
	
	ld t0, 24(s0)			# 	t0: tete enfants
	beqz t0, fin_size		# 	if(enfant courant != 0) {

# Boucle pour parcourir enfants d'une génération
boucle_enfants_size:			#	do {
	ld t2, 0(t0)			# 		t2: adresse enfant
	addi s1, s1, 1			# 		compteur++ (comptabillise enfant courant)
	
	mv a0, t2			#		a0: enfant (paramètre)
	mv s2, t0
	call sizeArbre			#		appel récursif pour enfant courant
	mv t0, s2			# 		reprendre boucle de enfant courant
	
	add s1, s1, a0			# 		ajouter ses propres descendants
	
	ld t3, 8(t0)
	mv t0, t3			#		courant: enfant suivant
	bnez t3, boucle_enfants_size	#	} while(suivant != 0)

fin_size:				# }
	mv a0, s1			# return compteur
	
	# Épilogue
	ld s2, 0(sp)
	ld s1, 8(sp)
	ld s0, 0(sp)
	ld ra, 24(sp)
	addi sp, sp, 32
	ret
	