# Ce programme est une calculatrice contenant les opérations "+", "-", "*", "/", "%".
#
# Tous les tests sont fonctionnels et les erreurs sont gérées.
#
# Le fonctionnement de la calculatrice est le suivant :
# Le programme lit un premier nombre jusqu’à un opérateur, qui marque aussi la fin du nombre.
# Il lit ensuite un deuxième nombre jusqu’à un autre opérateur.
# Il effectue le calcul avec l’opérateur courant et met à jour le total.
# Le nouvel opérateur devient l’opérateur actif, et le processus recommence.
# Si = ou q est entré, le résultat est affiché (q quitte ensuite).

.eqv PrintInt 1
.eqv Exit 10
.eqv PrintChar 11
.eqv ReadChar 12

# Repartir la calculatrice lorsque le caractère "=" est entré.
reset:
  li t0, 0  # total
  li t1, 0  # opérateur à utiliser
  li t2, 0  # nombre lu
  li t3, 0  # opérateur lu

  li s0, 0  # first number and operator read
  li s1, 0  # flag
  li s2, 0  # type of last character entered (0: operator, 1: number, 2: =, 3:q)
  li s3, -1 # flag du type d'erreur (0 = E0, 1 = E1, 2 = E2)

# bloc pour lire le prochain nombre (le nombre se trouvant après l'opérateur)
read_next_number:
  li t2, 0

  li t5, 1
  bge s1, t5, print         # si flag = 1 (true)

# bloc pour lire le prochain caractère
read_next_car:
  li a7, ReadChar
  ecall

  # bloc pour le traitement des nombres
  jal ra, verifynumber
  li t5, 1 
  beq s1, t5, convertnumber # si flag = 1 (true)

  # bloc pour le traitement des caractères d'impression
  jal ra, verifyprint       # si a0 = 'q' ou '=', le flag s1 est mis à 1 ou 2

  # bloc pour vérifier si caractère lu (a0) = ' ', '\t' ou '\n'
  jal ra, verifyblank
  li t5, 1
  beq s1, t5, read_next_car # si flag = 1 (true)

  # bloc pour le traitement des opérateurs
  li t5, 37
  beq a0, t5, operator_handling # a0 == %

  li t5, 42
  beq a0, t5, operator_handling # a0 == *

  li t5, 43
  beq a0, t5, operator_handling # a0 == +

  li t5, 45
  beq a0, t5, operator_handling # a0 == -

  li t5, 47
  beq a0, t5, operator_handling # a0 == /

  # bloc pour le traitement des caractères autres que les nombres et les opérateurs
  li s3, 0                      # type d'erreur = E0
  j quitError

# bloc pour ajouter le caractère lu dans le nombre
convertnumber:
  li s2, 1          # Type du dernier caractère lu = nombre

  addi a0, a0, -48  # Converti en nombre (ASCII à entier)

  add t2, t2, t2            # * 2
  add t4, t2, t2            # * 4
  add t4, t4, t4            # * 8
  add t2, t2, t4            # * 10
  add t2, t2, a0

  li t5, 0
  bne s0, t5, read_next_car # si s0 != 0 (premier nombre nombre lu)

  mv t0, t2                 # total = nombre lu
  j read_next_car

# bloc pour le traitement des opérateurs
operator_handling:

  # si 2 opérateur de suites
  li s3, 0                  # type d'erreur = E0
  li t5, 0
  beq s2, t5, quitError     # si type du dernier caractère lu = opérateur
  li s3, -1

  li s2, 0                  # dernier nombre lu = type operator

  # si le premier nombre a été lu on calcule le résultat
  li t5, 0
  mv t1, t3                 # opérateur à utiliser
  mv t3, a0                 # nouvel opérateur lu
  bne s0, t5, calculate     # si premier nombre a été lu, on calcule le résultat

  # si non
  mv t1, a0
  li s0, 1                  # premier nombre et opérateur lu

  j read_next_number

# bloc calcul total
calculate:
  li t5, 37
  beq t1, t5, mod     # opérateur == %

  li t5, 42
  beq t1, t5, multi   # opérateur == *

  li t5, 43
  beq t1, t5, add     # opérateur == +

  li t5, 45
  beq t1, t5, sub     # opérateur == -

  li t5, 47
  beq t1, t5, divide  # opérateur == /

# bloc pour l'addition
add:
  add t0, t0, t2
  j read_next_number  # relit un nouveau caractère

# bloc pour la soustraction
sub:
  li s3, 1            # type d'erreur = E1
  blt t0, t2, erreur  # t0 < t2
  li s3, -1

  sub t0, t0, t2

  j read_next_number

# bloc pour la multiplication
multi:
  li t6, 0                        # compteur
  mv t5, t0                       # multiplicande
  li t0, 0                        # résultat

multloop:
  beq t6, t2, read_next_number    # si compteur == multiplicateur
  add t0, t0, t5
  addi t6, t6, 1
  j multloop

# bloc pour la division
divide:
  li s3, 2  
  beqz t2, erreur     # si / 0
  li s3, -1

  li t6, -1           # compteur
  
divideloop:
  sub t0, t0, t2
	addi t6, t6, 1
  bgez t0, divideloop # si t0 >= 0

  mv t0, t6           # total = résultat de la division
  j read_next_number

# bloc pour le modulo
mod:
  li s3, 2
  beqz t2, erreur     # si / 0
  li s3, -1

  li t6, -1           # compteur

modloop:
  sub t0, t0, t2
	addi t6, t6, 1
  bgez t0, modloop    # si t0 >= 0
  add t0, t0, t2
  
  j read_next_number

# bloc regardant si operator, =, q
verifyprint:

  li t5, 61
  beq a0, t5, equal   # si a0 == '='

  li t5, 113
  beq a0, t5, quit    # si a0 == 'q'

  ret

# bloc pour le traitement de l'égalité
equal:
  li s1, 1              # flag, on print le résultat
  mv t1, t3

  li t5, 1
  beq s0, t5, calculate # si premier nombre et opérateur lu, on calcule le résultat avant de print

  mv t0, t2
  j print

# bloc pour le traitement de la sortie du programme
quit:
  li s1, 2              # flag, on print le résultat et on quitte
  mv t1, t3

  li t5, 1
  beq s0, t5, calculate # si premier nombre et opérateur lu, on calcule le résultat avant de print

  mv t0, t2
  j print

# bloc pour vérifier si caractère lu est un espace, tabulation ou nouvelle ligne
verifyblank:
  li s1, 0              # flag false

  li t5, 32
  beq a0, t5, flagtrue  # a0 == ' '

  li t5, 9
  beq a0, t5, flagtrue  # a0 == \t

  li t5, 10
  beq a0, t5, flagtrue  # a0 == \n

  ret 

# bloc pour vérifier si le caractère lu est un nombre
verifynumber:
  li s1, 1              # flag true

  li t5, 48
  blt a0, t5, flagfalse # input != number

  li t5, 57
  bgt a0, t5, flagfalse # input != number
  ret

# bloc pour mettre le flag à true
flagtrue:
  li s1, 1
  ret

# bloc pour mettre le flag à false
flagfalse:
  li s1, 0
  ret

# bloc pour print le résultat
print:
  mv a0, t0
  li a7, PrintInt
  ecall

  jal ra, printEscape # affiche le caractère de fin de ligne

  li t5, 2
  beq s1, t5, exit    # flag == 2, on quitte le programme

  j reset

# bloc pour quitter le programme
exit:
  li a7, Exit
  ecall

# bloc contenant la logique pour print l'erreur.
erreur:
  li t5, 61
  beq a0, t5, printError  # si a0 == '='

  li t5, 113
  beq a0, t5, quitError   # si a0 == 'q'

#bloc pour lire des caractères en boucle jusqu'à ce qu'il entre le caractère '=' ou 'q'
boucleErreur:
  li a7, ReadChar
  ecall

  li t5, 61
  beq a0, t5, printError # a0 == '='

  li t5, 113
  beq a0, t5, quitError  # a0 == 'q'

j boucleErreur

# bloc pour print l'erreur et effectuer un nouveau calcul
printError:
  # print le caractère 'E'
  li a7, PrintChar
  li a0, 'E'
  ecall

  # print le type d'erreur
  li a7, PrintInt
  mv a0, s3
  ecall

  jal ra, printEscape # affiche le caractère de fin de ligne
  j reset

# bloc pour print l'erreur et quitter le programme
quitError:
  # print le caractère 'E'
  li a7, PrintChar
  li a0, 'E'
  ecall

  # print le type d'erreur
  li a7, PrintInt
  mv a0, s3
  ecall

  jal ra, printEscape # affiche le caractère de fin de ligne

  li a7, Exit # quitter le programme
  ecall

# bloc pour afficher le caractère de fin de ligne
printEscape:
  li a7, PrintInt
  li a0, 10             # code ASCII de '\n' = 10
  li a7, 11
  ecall
  ret
