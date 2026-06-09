	# Écrivez un programme strlen.s qui lit des caractères jusqu’à '.' et affiche le nombre de caractères qui ont
	# été saisis (sans compter le . final).
	# Exemple « Hello, World123. » doit donner 15.
	
	.eqv printInt, 1
	.eqv exit, 10
	.eqv readChar, 12
	
	li s0, 0 # Compteur nombre chars
	li s1, '.' # Char de fin de saisie
	
loopSaisie : 
	li a7, readChar
	ecall
	beq a0, s1, fin # si la saisie == '.', on va dans fin.
	addi s0, s0, 1 #Après le beq puisqu'il ne faut pas compter le '.'
	j loopSaisie
	
fin:
	li a7, printInt
	mv a0, s0
	ecall
	
	li a7, exit
	ecall
