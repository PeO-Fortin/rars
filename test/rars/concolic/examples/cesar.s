	# Écrivez un programme cesar.s qui lit un nombre puis des lettres majuscules et les affiche
	# décalés d’autant dans l’ordre alphabétique. Le décalage est circulaire, A est la lettre 
	# qui suit Z. Le programme s’arrête quand un caractère qui n’est pas une lettre majuscule est rencontré.
	
	# On ne gère pas les cas ou un décalage est plus petit que -26 ou plus grand que 26 puisqu'il requiert
	# Des concepts que vous n'avez pas encore vu
	
	.eqv readInt, 5
	.eqv exit, 10
	.eqv printChar, 11
	.eqv readChar, 12
	
	li a7, readInt
	ecall
	mv s0, a0 #s0 = décalage
	
	li s1, 'A'
	li s2, 'Z'
	
loopSaisie:
	li a7, readChar
	ecall
	blt a0, s1, fin # si a0 < 'A'
	bgt a0, s2, fin # si a0 > 'Z'
	
	# Le char est valide, on lui fait subir le décalage circulaire
	add a0, a0, s0
	blt a0, s1, fix1
	bgt a0, s2, fix2
	j afficher

fix1: 
	addi a0, a0, 26 # si a0 < 'A', on fait + 26 pour aller dans l'autre extrémité du tableau de l'alphabet
	j afficher

fix2:
	addi a0, a0, -26 # si a0 > 'Z', on fait - 26 pour aller dans l'autre extrémité du tableau de l'alphabet
	
afficher:
	li a7, printChar
	ecall
	j loopSaisie
	
fin:
	li a7, exit
	li a0, 0
	ecall
