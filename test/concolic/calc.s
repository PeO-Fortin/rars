# TP1 - Calculatrice
# Pierre-Olivier Fortin FORP22028608 (groupe 050)
#
# Ce programme effectue des opérations entières de base sur des nombres positifs.
# Les débordements ne sont pas gérés par le programme.
# Les priorités d'opérations ne sont pas gérés par le programme.

.eqv PrintInt, 1
.eqv Exit, 10
.eqv PrintChar, 11
.eqv ReadChar, 12

debut:	li s0, 0 		#Conteneur du total
	li s1, 0 		#Conteneur du chiffre lu
	li s2, 0 		#Conteneur de l'opérateur
	li s3, 0		#Boolean = 0 si aucun chiffre lu avant l'opérateur

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
	mv s0, s1		#Chiffre lu = total
	j lire 			# }
	
oper:	beqz s3, eChar 		#Erreur si aucun chiffre lu avant l'opérateur
	
	#Exécution de l'opération en mémoire
	li t0, '+'
	beq s2, t0, addit
	li t0, '-'
	beq s2, t0, sous
	li t0, '*'
	beq s2, t0, mult
	li t0, '/'
	beq s2, t0, divi
	li t0, '%'
	beq s2, t0, divi
	
	li s1, 0 		#Réinitialisation du conteneur de chiffre lu
	li s3, 0 		#Réinitialisation du boolean de chiffre lu
	
	#Vérification des opérateurs
	li t0, '='
	beq a0, t0, egal
	li t0, 'q'
	beq a0, t0, egal
	
	li t0, '+'
	bne a0, t0, valSub
	mv s2, a0
	j lire

valSub:	li t0, '-'
	bne a0, t0, valMul
	mv s2, a0
	j lire

valMul:	li t0, '*'
	bne a0, t0, valDiv
	mv s2, a0
	j lire
	
valDiv:	li t0, '/'
	bne a0, t0, valMod
	mv s2, a0
	j lire
	
valMod:	li t0, '%'
	bne a0, t0, eChar 	#Erreur si caractère invalide
	mv s2, a0
	j lire

#***Section de l'exécution des opérations***

addit:	add s0, s0, s1 		#Addition au total
	j finOpe

sous:	bgt s1, s0, eSous 	#Erreur si le résultat sera négatif
	sub s0, s0, s1 		#Soustraction au total
	j finOpe

mult:	li t0, 0 		#Initialisation du conteneur du produit

loopMul:
	beqz s1, finMul 	#While (multiplicateur != 0) {
	add t0, t0, s0
	addi s1, s1, -1
	j loopMul 		# }
	
finMul:	mv s0, t0 		#Le produit devient le total
	j finOpe

divi:	beqz s1, eDiv 		#Erreur si divison par 0
	li t1, 0 		#Initialisation du conteneur du quotient
		
loopDiv:
	blt s0, s1, finDiv 	#While (total > diviseur) {
	sub s0, s0, s1
	addi t1, t1, 1
	j loopDiv 		# }
	
finDiv: li t0, '%'
	beq s2, t0, finOpe 	#If (opération == '/') {
	mv s0, t1		# }
	j finOpe

finOpe:	li s2, 0		#Réinitialisation du conteneur d'opérateur
	j oper

egal:	mv s2, a0 		#Sauvegarde l'opérateur '=' ou 'q'
	mv a0, s0 		#Affiche total
	li a7, PrintInt 
	ecall
	
	li t0, 'q'
	beq s2, t0, fin 	#If (opérateur != 'q') {
	
	li a0, '\n'
	li a7, PrintChar
	ecall
	j debut 		# }

#***Section des erreurs***

#Erreur caractère invalide
eChar:	li t1, '0'
	j affErr

#Erreur soustraction avec résultat < 0
eSous:	li t1, '1'
	j affErr

#Erreur division par 0
eDiv:	li t1, '2'
	j affErr

#Affichage de l'erreur
affErr:	mv t2, a0 		#Stockage du dernier caractère lu
	li a7, PrintChar
	li a0, 'E'
	ecall
	mv a0, t1
	ecall
	
	li t0, '0'
	beq t1, t0, fin 	#Fin du programme si E0
	
	mv a0, t2 		#Recupération du dernier caractère lu

#Ignorer le reste de l'opération
nxtOpe:	li t0, '='
	beq a0, t0, finNxtOpe 	#If (caractère != '=') {
	li t0, 'q'
	beq a0, t0, fin 	# } else if (caractère != 'q') {
	li t0, -1
	beq a0, t0, fin 	# } else if (caractère != fin de l'input) {
	li a7, ReadChar
	ecall
	j nxtOpe 		# }

#Reprendre le déroulement normal du programme
finNxtOpe:
	li a7, PrintChar
	li a0, '\n'
	ecall
	j debut

#Fin du programme
fin:	li a7, Exit
	ecall
