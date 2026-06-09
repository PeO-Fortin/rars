# ============================
# Programme : debordement.s
# Demande le nombre de bits 
# Demande si signé ou non signé
# Lit deux nombres 
# Affiche la somme des deux nombres en Hexadécimal en indiquant s'il y a un débordement ou non
# ============================
	# Appels systeme RARS utilisés
	.eqv PrintString, 4
	.eqv ReadInt, 5
	.eqv ReadChar, 12
	.eqv PrintIntHex, 34
	.eqv Exit, 10
	.data
str_bits:	.string "nombre de bits "
str_signe:	.string "signe ou non-signe "
str_oprA:	.string "\noperande A "
str_oprB:	.string "operande B "
str_ovf:	.string " debordement\n"
str_succes:	.string " succes\n"
str_err_signe: .string "\nmauvais signe\n"
	.text
	li t0, 's' 			# 's' (signé)
	li t1, 'n'			# 'n' (non signé) 
	li t2, 2			# le min des bits acceptés
	li t3, 64			# le max des bits acceptés
# Demander le nombre de bits souhaité et le caractère du signement
demander:
	la s6, str_succes			
	# Lecture du nombre de bits dans s1
	li a7, PrintString
	la a0, str_bits
	ecall
	li a7, ReadInt
	ecall
	beq zero, a0, arret 			# if (a0==0) 
	blt a0, t2, demander			# if (a0<2)
	blt t3,a0, demander			# if (64<a0)
	mv s1, a0				# else
	# Lecture du signé ou non-signé dans s2
	li a7, PrintString
	la a0, str_signe
	ecall
	li a7, ReadChar
	ecall
	mv s2, a0
	beq s2, t0, demanderOperandes 	# if (s2=='s') 
	beq s2, t1, demanderOperandes 	# if (s2=='n') 
	j msgErr			#ese
# Afficher le message d'erreur du signé ou non signé	
msgErr:						# else
	li a7, PrintString
	la a0, str_err_signe
	ecall
	j demander	
# Demander les opérandes
demanderOperandes:
	#Lecture d'opérande A dans s3
	li a7, PrintString
	la a0, str_oprA
	ecall
	li a7, ReadInt
	ecall
	mv s3, a0	
	#Lecture d'opérande B dans s4
	li a7, PrintString
	la a0, str_oprB
	ecall
	li a7, ReadInt
	ecall
	mv s4, a0	
	# Créer un masque de n bits à 1
	sub t5, t3, s1			# t5= 64- s1(nbr de bits souaité)
	li t4, -1
	srl t4, t4, t5
	and s3, s3, t4			# Garder le nombre de bits souaitée pour A
	and s4, s4, t4			# Garder le nombre de bits souaitée pour B
	add s5, s3, s4			# s5=A+B
	and s5, s5, t4			# Garder le nombre de bits souaitée pour la somme
	beq s2, t0, signe		# if (s2=='s')
	beq s2, t1, nonSigne		# if (s2=='n')
signe:
	addi t5, s1, -1			#t5= nbr de bits souhaité - 1 pour garder le bit de signe
	li t6, 1
	sll t6, t6, t5
	xor t5, s5, s3
	and t5, t5, t6			# comparer le bit de signe 
	xor s7, s5, s4
	and s7, s7, t6
	and t5, t5, s7
	bne t5, zero, deborder		# if (t5!=0) c'est un débordement car le signe du résultat est différent du signe de l'opérande
	j afficher
nonSigne:	
	bltu s5, s3, deborder
	j afficher
deborder:
	la s6, str_ovf
	j afficher
afficher:
	mv a0, s5
	li a7, PrintIntHex
	ecall
	li a7, PrintString
	mv a0, s6
	ecall
	j demander	
# Arrêter le programme	
arret:
	li a7, Exit
	ecall	
	


