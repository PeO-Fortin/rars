	# Écrivez un programme milieu.s qui lit trois nombres et affiche celui qui est compris entre les deux autres.
	# Notes:
	# Si un nombre est répété deux fois, c’est forcément celui-ci qu’il faut afficher.
	# On peut s’en sortir avec seulement trois comparaisons.	
	
	.eqv printInt, 1
	.eqv readInt, 5
	.eqv exit, 10
	
	# Premier nombre
	li a7, readInt
	ecall
	mv s0, a0 
	
	# Deuxieme nombre
	ecall
	blt a0, s0, deuxiemePlusPetit # si a0 > s0, branch dans 2eme plus petit
	mv s1, a0 # sinon saisie plus grande ou égale, on stock a0 dans s1 
	j troisiemeNombre 
	
deuxiemePlusPetit:# si la 2eme saisie est plus petite, on bouge
	mv s1, s0 # s0 dans s1 et a0 dans s0 afin de garder un
	mv s0, a0 # ordre logique des valeurs (classées en croissant)

troisiemeNombre:
	ecall
	blt a0, s0, troisiemePlusPetit # si a0 < s0, branch dans 3eme plus petit
	bge a0, s1, fin # si a0 > s1, on imprime le milieu déja défini
	mv s1, a0 # sinon, s0 < a0 < s1, a0 est le milieu, on le mets dans s1
	j fin
	
troisiemePlusPetit:
	mv s1, s0 #s0 est le milieu, on le bouge dans s1 (registre qui indique le milieu)
	
fin:
	li a7, printInt
	mv a0, s1 # on bouge le milieu dans a0 pour l'imprimer
	ecall
	
	li a7, exit
	li a0, 0
	ecall
