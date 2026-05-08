.eqv PrintInt, 1
.eqv ReadInt, 5
.eqv Exit, 10

	#Lecture premier nombre
	li a7, ReadInt
	ecall
	mv s0, a0
	
	#Lecture du deuxieme nombre
	li a7, ReadInt
	ecall
	mv s1, a0
	
	#Lecture du troisieme nombre
	li a7, ReadInt
	ecall
	mv s2, a0
	
	#Differencier plus grand et plus petit entre chiffre 1 et chiffre 2
	bge s0, s1, plusGrand
	mv t0, s1
	mv t1, s0
	j instruDeux
	
	plusGrand:
	mv t0, s0
	mv t1, s1
	
	#Differencier plus grand entre chiffre 3 et la reponse plus grand precedent. Garder le plus petit. 
	instruDeux:
	bge s2, t0, reponse
	mv t0, s2
	
	#Garder plus grand entre plus petit operation 1 et plus petit operation 2.
	reponse:
	bge t0, t1, fin
	mv t0, t1
	
	#Affichage
	fin:
	mv a0, t0
	li a7, PrintInt
	ecall
	
	li a7, Exit
	ecall