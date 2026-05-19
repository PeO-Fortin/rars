
# Ce programme implémente un automate cellulaire élémentaire 1D en fournissant le nombre de générations, la règle de l'automate et l'entrée initiale.
# Il n'y a aucun bogue connu.

	.data
	# Appels système RARS
	.eqv PrintInt, 1			# afficher entier
	.eqv ReadInt, 5				# lire entier
	.eqv Exit, 10				# quitter
	.eqv PrintChar, 11			# afficher caractère
	.eqv ReadChar, 12			# lire caractère
	
	# Constantes
	.eqv taille_tab, 32			# taille max tableau
	.eqv saut_ligne, '\n'			# saut de ligne
	.eqv cptr_init, 0			# valeur initiale compteur
	.eqv espace, ' '			# espace
	.eqv hastag, '#'			# symbole numéro
	
	# Allocation mémoire
	niv_courant:	.space taille_tab	# tableau de 32 cellules (courant)
	niv_suivant:	.space taille_tab	# tableau de 32 cellules (suivant)
	
	
	.text
# -----------------------------------------------------------------------------------------------
# Routine: main => Programme principal qui appelle les autres routines
# -----------------------------------------------------------------------------------------------
main:
	call lire_info				# lire nb itérations, règle et valeur initiale (vi)
	
	# afficher valeur initiale
	la a0, niv_courant
	li a1, cptr_init
	call afficher_niv

	li t0, 1				# t0: compteur d'itération
# Affichage et calculs des autres niveaux
boucle_main:
	bgt t0, s1, fin_automate		# while (compteur <= nb itérations) {

	call calculer_niv_suivant		# 	calculer niveau suivant

	la a0, niv_suivant
	mv a1, t0
	call afficher_niv			# 	afficher niveau nouvellement calculé
	
	call copier_niv_suivant_dans_courant	# 	mettre-à-jour niveau courant
	
	addi t0, t0, 1				# 	compteur++

	j boucle_main				# }


# -----------------------------------------------------------------------------------------------
# Routine: lire_info => Lire nb itérations, règle et valeur initiale
# -----------------------------------------------------------------------------------------------
lire_info:
	# lire nb itérations
	li a7, ReadInt
	ecall
	mv s1, a0        			# s1: nb itérations

	# lire règle
	li a7, ReadInt
	ecall
	mv s2, a0        			# s2: règle
	
	la t0, niv_courant			# t0: pointeur niv_courant (position)
	li t1, cptr_init         		# t1: compteur nb char lu

# Lire valeur initiale (reste des 32 valeurs binaires initialisées à 0)
boucle_lire_vals_vi:				# do {
	li a7, ReadChar
	ecall					# 	lire char

	li t2, saut_ligne
	beq a0, t2, boucle_zeros		# 	if (char != '\n'

	li t2, -1
	beq a0, t2, boucle_zeros		# 			&& char != EOF

	li t2, taille_tab
	bge t1, t2, boucle_zeros		# 					&& comtpeur < 32) {

	addi a0, a0, -48			# 		convertir '0' ou '1' en 0 ou 1
	sb a0, 0(t0)				# 		et stocker dans niv_courant

	addi t0, t0, 1				# 		position++
	addi t1, t1, 1				# 		compteur++

	j boucle_lire_vals_vi			# 	} else {

# Remplir cellules restantes avec 0
boucle_zeros:
	li t2, taille_tab
	bge t1, t2, fin_lecture_info		# 		while (compteur < 32) {
	sb zero, 0(t0)				# 			stocker 0 dans niv_courant
	addi t0, t0, 1				# 			position++
	addi t1, t1, 1				# 			compteur++
	j boucle_zeros				# 		}
						# 	}
						# } while (char != '\n' && char != EOF && comtpeur < 32);

# Retour au main pour lire_info
fin_lecture_info:
	ret


# -----------------------------------------------------------------------------------------------
# Routine: afficher_niv => Afficher niveau donné
# -----------------------------------------------------------------------------------------------
# paramètres:
# 	a0: pointeur niv_courant(si valeur initiale) ou niv_suivant(sinon)
# 	a1: numéro itération
afficher_niv:
	mv t3, a0				# t3: pointeur tableau à afficher (position)
	li t1, cptr_init			# t1: compteur nb cellules à afficher

boucle_afficher_cellules:			# do {
	lb t2, 0(t3)				#	t2: cellule à afficher
	addi t2, t2, 48				# 	convertir 0 ou 1 en '0' ou '1'

	mv a0, t2
	li a7, PrintChar
	ecall					#	afficher cellule

	addi t3, t3, 1				#	position++
	addi t1, t1, 1				#	compteur++

	li t2, taille_tab
	blt t1, t2, boucle_afficher_cellules	# } while (compteur < 32);

	# afficher " # "
	li a0, espace
	li a7, PrintChar
	ecall
	li a0, hastag
	li a7, PrintChar
	ecall
	li a0, espace
	li a7, PrintChar
	ecall

	# afficher numéro itération
	mv a0, a1
	li a7, PrintInt
	ecall

	# afficher saut de ligne
	li a0, saut_ligne
	li a7, PrintChar
	ecall

	ret					# retour au main pour afficher_niv


# -----------------------------------------------------------------------------------------------
# Routine: calculer_niv_suivant => Calculer niveau suivant
# -----------------------------------------------------------------------------------------------
# 	t1: cellule gauche
# 	t2: cellule courante
# 	t3: cellule droite
calculer_niv_suivant:
	li t2, cptr_init                	# t2: index cellule courante dans niv_courant
	li t4, taille_tab               	# t4: borne tableau (max 32 cellules)

boucle_calcul:					# do {
	addi t1, t2, -1
	bgez t1, val_gauche_ok			# 	if (index cellule gauche <= 0) {
	li t1, 31				# 		gauche = dernière cellule

val_gauche_ok:					# 	}
	addi t3, t2, 1
	blt t3, t4, val_droite_ok		# 	if (index cellule droite >= 32)
	li t3, 0				#		droite = première cellule

val_droite_ok:					# 	}
	la t5, niv_courant
	add t6, t5, t1
	lb t1, 0(t6)            		# 	charger cellule gauche dans tableau

	add t6, t5, t2
	lb t6, 0(t6)            		# 	charger cellule milieu dans tableau

	add t5, t5, t3
	lb t3, 0(t5)            		# 	charger cellule droite dans tableau

	# former indice (0 à 7) pour ième résultat de règle
	slli t1, t1, 2				# 	t1: t1 * 4
	slli t6, t6, 1				# 	t2: t2 * 2
	or t5, t1, t6
	or t5, t5, t3				# 	t5: indice final

	# appliquer la règle
	mv t1, s2
	srl t1, t1, t5				# 	décaler résultat de règle vers bit le plus faible
	andi t1, t1, 1				# 	extraire résultat (bit le plus faible)

	# stocker résultat dans niv_suivant
	la t3, niv_suivant
	add t3, t3, t2
	sb t1, 0(t3)

	addi t2, t2, 1				# 	index++
	blt t2, t4, boucle_calcul		# } while (position < 32);

    	ret					# retour au main pour calculer_niv_suivant


# -----------------------------------------------------------------------------------------------
# Routine: copier_niv_suivant_dans_courant => Copier niv_suivant dans niv_courant pour affichage
# -----------------------------------------------------------------------------------------------
copier_niv_suivant_dans_courant:
	la t3, niv_suivant			# t3: pointeur niv_suivant
	la t2, niv_courant			# t2: pointeur niv_courant
	li t1, cptr_init			# t1: compteur cellules copiées

boucle_copie:					# do {
	lb t4, 0(t3)
	sb t4, 0(t2)				# 	stocker cellule de niv_suivant dans niv_courant
	addi t3, t3, 1				# 	position niv_suivant ++
	addi t2, t2, 1				# 	position niv_courant ++
	addi t1, t1, 1				# 	compteur++
	li t5, taille_tab
	blt t1, t5, boucle_copie		# } while (compteur < 32);

    	ret					# retour au main pour copier_niv_suivant_dans_courant


# -----------------------------------------------------------------------------------------------
# Routine: fin_automate => Quitter
# -----------------------------------------------------------------------------------------------
fin_automate:
	li a7, Exit
	ecall
