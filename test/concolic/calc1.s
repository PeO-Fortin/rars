# TP1 - Calculatrice positive, mais bête
# Cynthia Tran TRAC83550401 (groupe 50)
#
# Ce programme est une calculatrice gérant 5 opérations arithmétiques de base sur des entiers.
# Le but étamt de lire une séquence de nombres et d'opérations en alternance, le programme va soit faire l'opération arithmétique ou faire un affichage du résultat.
# Aucun bogue rencontré.

	# Appels système RARS utilisés
	.eqv PrintInt, 1
	.eqv Exit, 10
	.eqv PrintChar, 11
	.eqv ReadChar, 12

initialiser_registres:
	li s0, 0		# Resultat courant
	li s1, 0		# Operande (nombre)
	li s2, 0		# Operateur precedent
	li s3, 0		# Code d'erreur (1 = resultat negatif, 2 = division par 0)

boucle_lire_car:				# FAIRE
	# Lire (prochain) caractere
	li a7, ReadChar
	ecall
	mv t0, a0

	# Ignorer espaces blancs (white space)
	li t1, ' '
	beq t0, t1, boucle_lire_car
	li t1, '\t'
	beq t0, t1, boucle_lire_car
	li t1, '\n'
	beq t0, t1, boucle_lire_car
	li t1, '\r'
	beq t0, t1, boucle_lire_car

	li t1, '='
	beq t0, t1, traiter_fin
	li t1, 'q'
	beq t0, t1, traiter_fin
	
	# Si err, skip le reste de l'equation jusqu'a l'affichage
	bnez s3, boucle_lire_car

	# Verifier [0-9]
	li t1, '0'
	blt t0, t1, calcul_precedent
	li t1, '9'
	bgt t0, t1, calcul_precedent

	# Convertir le caractere en entier
	li t1, '0'	# Code ascii = 48
	sub t0, t0, t1	# Garder meme chiffre, mais en version en decimal dans la table ascii
	# Changer de position decimal (boucle d'additions x10)
	mv t2, s1	# Valeur fixe de s1
	li s1, 0	# Va cummuler 10x s1
	li t3, 10	# index de boucle i
	
multiplier_par_10:
	beqz t3, fin_mult_10	# for(i=10; i>0; i--) {
	add s1, s1, t2
	addi t3, t3, -1		#	// decrementer boucle
	j multiplier_par_10	# }
	
fin_mult_10:
	add s1, s1, t0		# Additionner chiffre au nombre
	
	j boucle_lire_car			# TANT QUE QU'ON NE QUITTE PAS ('q')

# Gestion des operations selon l'operation a faire precedemment
calcul_precedent:
	beqz s2, premier_operateur
	
	li t1, '+'
	beq s2, t1, op_addition
	li t1, '-'
	beq s2, t1, op_soustraction
	li t1, '*'
	beq s2, t1, op_multiplication
	li t1, '/'
	beq s2, t1, op_division
	li t1, '%'
	beq s2, t1, op_modulo

# Afficher erreur E0 si cacartere inconnu (ni chiffre, ni operateur) et quitter
afficher_errE0_et_quitter:
	li a0, 'E'
	li a7, PrintChar
	ecall
	li a0, '0'
	li a7, PrintChar
	ecall
	j quitter
	
premier_operateur:
	mv s0, s1				# Premier nombre devient le resultat s'il n'y a pas encore d'operateur

# Enregistrer le nouvel operateur lu
enregistrer_nouv_operateur:
	mv s2, t0
	li s1, 0
	
	# retour a l'affichage du resultat
	li t1, '='
	beq t0, t1, retour
	li t1, 'q'
	beq t0, t1, retour
	
	j boucle_lire_car

# Modulo avec soustraction
op_modulo:
	beqz s1, afficher_errE2			# Erreur E2 si diviseur est 0
	mv t2, s0				# Reste
	mv t3, s1				# Diviseur

# Boucle pour soustraire entierement le diviseur
boucle_modulo:
	blt t2, t3, fin_modulo			# while (reste > diviseur) {
	sub t2, t2, t3 				#	// Soustraire diviseur du reste
	j boucle_modulo				# }

fin_modulo:
	mv s0, t2                		# s0 = reste
	j enregistrer_nouv_operateur

# Division avec soustraction
op_division:
	beqz s1, afficher_errE2			# Erreur E2 si diviseur est 0
	mv t2, s0				# Dividende
	mv t3, s1				# Diviseur
	li t4, 0				# Quotient (accumule nombre de soustraction)

# Boucle pour enlever la dividende petit a petit
boucle_division:
	blt t2, t3, fin_division		# while (dividende > diviseur) {
	sub t2, t2, t3
	addi t4, t4, 1				# 	// Accumule nombre de soustraction
	j boucle_division				# }

fin_division:
	mv s0, t4				# s0 = quotient
	j enregistrer_nouv_operateur
	
# Multiplication par addition				
op_multiplication:
	li t2, 0				# Produit (accumule)
	mv t3, s0				# Multiplicande (fixe)
 	mv t4, s1				# Multiplicateur (index de la boucle)

boucle_multiplication:
	beqz t4, fin_multiplication		# while (multiplicateur != 0)
	add t2, t2, t3				# 	// Accumuler la multiplicande
	addi t4, t4, -1				# 	// Decrementer la boucle
	j boucle_multiplication				# }

fin_multiplication:
	mv s0, t2				# s0 = produit
	j enregistrer_nouv_operateur

# Soustraction
op_soustraction:
	sub s0, s0, s1
	bltz s0, afficher_errE1			# Err E1 si difference < 0
	j enregistrer_nouv_operateur

# Afficher erreur E1
afficher_errE1:
	li s3, 1
	li a0, 'E'
	li a7, PrintChar
	ecall
	li a0, '1'
	li a7, PrintChar
	ecall
	li t1, 'q'				
	beq t0, t1, quitter			# Quitter si 'q'
	li a0, '\n'
	li a7, PrintChar
	ecall
	li t1, '='
	beq t0, t1, prochaine_equation		# Passer a la prochaine equation si '='
	j boucle_lire_car			# Sinon lire prochaine caractere

# Addition
op_addition:
	add s0, s0, s1
	j enregistrer_nouv_operateur

# Si 'q' ou '='
traiter_fin:
	bnez s3, prochaine_equation		# Ne pas faire le calcul precedent s'il y a erreur
	beqz s2, afficher_s1_directement	# Si aucun operateur, afficher le nombre directement comme resultat
	jal calcul_precedent			# Sinon, effectuer le calcul qui reste

# Afficher le resultat
afficher_resultat:
	mv a0, s0
	li a7, PrintInt
	ecall
	li t1, 'q'
	beq t0, t1, quitter			# Quitter si 'q'
	li a0, '\n'
	li a7, PrintChar
	ecall
	j initialiser_registres			# Sinon continuer avec la prochaine equation

# Quitter
quitter:
	li a7, Exit
	ecall

# Afficher directement le premier nombre
afficher_s1_directement:
	mv s0, s1				# Premier nombre devient le resultat
	j afficher_resultat

# Passer a la prochaine equation si pas 'q'
prochaine_equation:
	li s3, 0
	li t1, 'q'
	beq t0, t1, quitter
	j initialiser_registres
	
# Afficher erreur E2					
afficher_errE2:
	li s3, 2
	li a0, 'E'
	li a7, PrintChar
	ecall
	li a0, '2'
	li a7, PrintChar
	ecall
	li t1, 'q'
	beq t0, t1, quitter			# Quitter si 'q'
	li a0, '\n'
	li a7, PrintChar
	ecall
	li t1, '='
	beq t0, t1, prochaine_equation		# Passer a la prochaine equation si '='
	j boucle_lire_car			# Sinon continuer a lire le prochain caractere

# Utilise pour les branchement conditionnel
retour:
	ret
