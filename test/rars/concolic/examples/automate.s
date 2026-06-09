	.data
	
	# Appels système utilisés
	.eqv	Exit, 10
	.eqv	ReadInt, 5
	.eqv	PrintInt, 1
	.eqv	ReadChar, 12
	.eqv	PrintChar, 11
	.eqv	PrintString, 4

	.eqv regleLen, 8
regle:	.space regleLen
	.eqv tabLen,32
tab:	.space tabLen


.text
	la s0, regle			# Conteneur adresse de la regle a appliquer
	la s1, tab			# Conteneur adresse du tableau a afficher
	li s2, 0			# Conteneur du nombre de lignes a afficher
	li s3, 0			# Compteur de la ligne a afficher
	
	li a7, ReadInt
	ecall
	mv s2, a0			# Sauvegarde du nombre de lignes a afficher
	
	li a7, ReadInt
	ecall
	
	# Transformation en ASCII des bits composant la regle et sauvegarde en memoire
	mv t0, s0
	li t1, regleLen		
lireRegle:				# do{
	andi t2, a0, 1
	addi t2, t2, 0x30	
	sb t2, 0(t0)
	addi t0, t0, 1 
	addi t1, t1, -1
	srli a0, a0, 1
	bgtz t1, lireRegle		# }while (i > longueurRegle)
	
	# Lecture du tableau et sauvegarde en memoire 
	mv t0, s1
	li t1, tabLen
lireLigne:				# do{
	li a7, ReadChar
	ecall
	bltz a0, mettreZero			# if (entree utilisateur lue){ 
	sb a0, 0(t0)
	j suiteLire
mettreZero:					# }sinon{
	li a0, '0'	
	sb a0, 0(t0)				# }
suiteLire:
	addi t0, t0, 1
	addi t1, t1, -1
	bgtz t1, lireLigne		# }while (i > longueurTableau)
	
automate:
	mv a0, s1
	mv a1, s3
	call afficher
	
	addi s3, s3, 1		
	bgt s3, s2, fin			# Sortie si toutes les lignes ont ete affichees
	
	li a0, '\n'
	li a7, PrintChar
	ecall
	
	addi sp, sp, -tabLen		# Espace pour stocker le nouveau tableau temporairement
	mv s4, s1
	li s5, 0			# Compteur de l'avancee du tableau
	

modTableau:				# do {
	li s6, 0			# Conteneur du triplet d'octets
	# Lecture de la case gauche du tableau
	addi a0, s4, -1	
	bge a0, s1, suiteSous			# if (index < 0) {
	addi a0, a0, tabLen			# }
suiteSous:
	call lireIndex
	add s6, s6, a0
	slli s6, s6, 1
	
	# Lecture de la case centrale du tableau
	mv a0, s4
	call lireIndex
	add s6, s6, a0
	slli s6, s6, 1
	
	# Lecture de la case droite du tableau
	addi a0, s4, 1
	addi t0, s1, tabLen
	blt a0, t0, suiteAdd
	addi, a0, a0, -tabLen
suiteAdd:
	call lireIndex
	add s6, s6, a0

	# Application de la regle de transformation	
	add t0, s0, s6			# Trouve le bon cas de figure de la regle
	lb t0, 0(t0)
	add t1, sp, s5
	sb t0, 0(t1)			# Sauvegarde la case du nouveau tableau
	
	addi s5, s5, 1
	addi s4, s4, 1
	li t0, tabLen
	blt s5, t0, modTableau		# } while (i < longueurTableau)

	# Transfert du tableau de la pile vers le tableau en memoire
	mv t0, sp
	mv t1, s1
	li t2, 0
chgTab:	ld t3, 0(t0)			# do {
	sd t3, 0(t1)
	addi t0, t0, 8
	addi t1, t1, 8
	addi t2, t2, 1
	li t4, 4
	blt t2, t4, chgTab		# }while (i < (longueurTableau / tailleDoubleWord))
	
	addi sp, sp, tabLen
	j automate

	# Affiche le tableau a l'adresse en argument (a0) suivi du numero de ligne en argument (a1)
afficher:
	li a7, PrintString		
	ecall
	li a0, ' '
	li a7, PrintChar
	ecall
	li a0, '#'
	li a7, PrintChar
	ecall
	li a0, ' '
	li a7, PrintChar
	ecall
	mv a0, a1
	li a7, PrintInt
	ecall
	ret
	
	# Retourne en int, le caractere ASCII de l'adresse en argument (a0)
lireIndex:
	lb a0, 0(a0)
	addi a0, a0, -0x30		# Transformation ASCII vers int
	ret

fin:	li a7, Exit
	ecall
