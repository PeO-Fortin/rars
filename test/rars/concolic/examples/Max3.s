	# Écrivez un programme max3.s qui lit trois nombres et affiche le plus grand des 3.
	# Note: on peut s’en sortir avec seulement 2 comparaisons (instructions RISC-V branch).
	
	.eqv printInt, 1
	.eqv readInt, 5
	.eqv exit, 10
	
	# Premier nombre
	li a7, readInt
	ecall
	mv s0, a0 # s0 = max, la première saisie est naturellement la plus grande.
	
	# Deuxième nombre
	ecall
	bge s0, a0, troisiemeNombre # si s0 >= a0, 3emeNbr
	mv s0, a0		    # sinon s0 <- a0 pour y mettre le nouveau max
	
troisiemeNombre:
	ecall
	bge s0, a0, fin # si s0 >= a0, fin
	mv s0, a0	# sinon s0 <- a0 pour y mettre le nouveau max
	
fin:
	# Imprimer la somme
	mv a0, s0
	li a7, printInt
	ecall 
	
	# Fin du programme
	li a0, 0
	li a7, exit
	ecall	
