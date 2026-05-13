# TP2 - Automate cellulaire
# Charles-Antoine Lanthier LANC70040208 (groupe 050)
#
# Ce programme est un automate cellulaire qui prend le nombre d'itération, la règle ainsi que la valeur initiale
# de l'automate en entrée
#
# Tous les tests sont fonctionnels et les erreurs sont gérées.
#
# Le fonctionnement du programme est le suivant :
# 1-  allocation de 32 octets pour l'itération sur laquelle nous allons généré la nouvelle
# 2-  allocation de 32 octets pour la nouvelle itération générée à partir de l'ancienne
# 3-  lecture du nombre d'itération et de la règle
# 4-  lecture de la valeur à l'indice de la cellule de l'itération initiale, bit par bit, en chargeant chaque bit dans un octet en mémoire,
# 	  tout en faisant un print du bit dans la console (pour afficher l'itération principale)
# 5-  Assemblage du motif définissant l'index du bit de la règle. Cela est fait en regardant 
#     les bits en mémoire de l'ancienne génération. (oldIteration)
# 6-  Lecture du bit à l'indice du motif de la règle.
# 7-  Chargement du bit en mémoire, à l'octet de la nouvelle itération, correspondant à l'indice.
# 8-  Répété les étapes 5 à 7 pour les 32 cellules. Afin de charger en mémoire tout la nouvelle itération (1 bit par case mémoire
# 9-  Afficher les 32 cellules dans la console
# 10- Copier les 32 octets de la nouvelle génération dans les octets correspondant de l'ancienne génération
# 11- Répété les 5 à 10 pour le nombre d'itération demandé

.eqv PrintInt 1
.eqv Exit 10
.eqv PrintChar 11
.eqv ReadChar 12
.eqv ReadInt 5

.data
oldIteration: .space 32     # ancienne itération
newIteration: .space 32 	# nouvelle itération

.text
# Lecture du nombre d'itération
call readInt
mv s1, a0 # nombre iteration

# Lecture de la regle (0 à 255)
call readInt
mv s2, a0 # regle

# Lecture de la valeur initiale (32 bits)
call read_binary

li s3, 0 # index itération

j start

boucleIteration:

	# print l'itération
	call print_binary
	
	call switch_iter_memory

start:
	beq s3, s1, exit # si index > nb iteration on quitte
	addi s3, s3, 1
	
	li s4, 0 # index cellule

boucleCellule:

	# si index cellule > 31 jump boucleIteration
	li t0, 32
	bge s4, t0, boucleIteration
	
	call getNewIndex
	
	call get_binary_rule
	
	# store binary
	mv a1, s4 # index cellule en a1 pour routine
	call store_new_binary
	
	addi s4, s4, 1
	j boucleCellule


# routine pour voir l'indice du bit de la règle à prendre.
getNewIndex:
	# créer motif
	la t1, oldIteration # bit au milieu
	
	add t1, t1, s4 # adresse + i milieu
	
	li t5, 0
	beq s4, t5, leftBit31 # si index = 0, le bit de gauche est celui à l'index 31
	addi t0, t1, -1 # bit gauche

rightBit: 
	li t5, 31
	bge s4, t5, rightBit0
	addi t2, t1, 1 # bit droite

	j createPattern

leftBit31:
	addi t0, t1, 31 # bit gauche
	j rightBit

rightBit0:
	la t2, oldIteration


createPattern:
	lb t0, 0(t0) # load bit gauche
	
	slli t0, t0, 1 
	
	lb t1, 0(t1) # load bit milieu
	
	mv a0, t1
	
	add t0, t0, t1
	
	slli t0, t0, 1
	
	lb t2, 0(t2) # load bit droite
	
	add t0, t0, t2
	
	mv a0, t0
	
	ret
	
get_binary_rule:

	srl t0, s2, a0 # règle << t0
	
	andi t0, t0, 1 # isoler bit de la nouvelle valeur
	
	mv a0, t0
	
	ret

store_new_binary:
	la t0, newIteration
	
	add t0, t0, a1 # adresse ou stocker
	
	sb a0, 0(t0)
	
	ret
	
switch_iter_memory:
	
	la t0, newIteration
	la t1, oldIteration
	li t2, 0           # compteur i = 0

copy_loop:
    li t3, 32
    bge t2, t3, end_copy   # si i >= 32 → fin

    lb t4, 0(t0)     # charger l’octet depuis la source
    sb t4, 0(t1)     # le stocker à l’adresse destination

    addi t0, t0, 1   # avancer source
    addi t1, t1, 1   # avancer destination
    addi t2, t2, 1   # i++

    j copy_loop

end_copy:
    ret

print_binary:
	la t0, newIteration
	li a7, PrintChar
	li a0, 10 # \n
	ecall
	li a7, PrintInt
	li t1, 0 # index
print_binary_loop:
	li t2, 32
	bge t1, t2, end_print
	lb a0, 0(t0)
	ecall
	addi t0, t0, 1
	addi t1, t1, 1

	j print_binary_loop
	
end_print:
	li a7, PrintChar
	li a0, ' '
	ecall
	li a0, '#'
	ecall
	li a0, ' '
	ecall
	
	mv a0, s3
	li a7, PrintInt
	ecall
	
	ret
	


read_binary:
    la t0, oldIteration         # pointeur vers la mémoire
    li t2, 0            		# compteur = 0

binary_loop:
    li t3, 32
    bge t2, t3, end_binary

    li a7, ReadChar
    ecall

    li t1, 10           		# saut de ligne '\n'
    beq a0, t1, binary_loop  	# ignorer les sauts de ligne

    li t1, '0'
    beq a0, t1, store_zero

    li t1, '1'
    beq a0, t1, store_one

	j store_zero				# Si aucun caractère trouvé, on store des 0

store_zero:
    li t1, 0
    sb t1, 0(t0)
    
    li a0, 0
    li a7, PrintInt
    ecall
    j increment

store_one:
    li t1, 1
    sb t1, 0(t0)
    li a0, 1
    li a7, PrintInt
    ecall

increment:
    addi t0, t0, 1
    addi t2, t2, 1
    j binary_loop

end_binary:

	li a7, PrintChar
	li a0, ' '
	ecall
	li a0, '#'
	ecall
	li a0, ' '
	ecall
	
	li a0, 0
	li a7, PrintInt
	ecall
    ret

exit:
	# ajout du dernier saut de ligne
	li a7, PrintChar
	li a0, 10 # \n
	ecall
    li a7, Exit
    ecall






