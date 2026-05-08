# TP2 - Family Tree Simulator
# Félix Servant-L'Heureux - SERF08069508 - (groupe 050)
#
# This program simulates a family tree structure, allowing the creation of persons,
# adding children, and calculating the generation level of a person.

# syscall codes
.eqv SBRK, 9

.data
# struct person {
.eqv t_person_father,  +0
.eqv t_person_mother,  +8
.eqv t_person_year,    +16
.eqv t_person_children,+24
.eqv t_person_size,    32
# }

# struct child_node {
.eqv t_child_node,      +0
.eqv t_child_next,      +8
.eqv t_child_prev,      +16
.eqv t_child_node_size, 24
# }

newline_char: .string "\n"
space_char:   .string " "

.text
.global newP
.global mGen
.global printLignePere
.global addEnf
.global sizeArbre

newP:
  addi sp, sp, -32
  sd ra, 0(sp)
  sd s0, 8(sp)
  sd s1, 16(sp)
  sd s2, 24(sp)

  mv s0, a0
  mv s1, a1
  mv s2, a2

  li a0, t_person_size    # request memory for person node
  li a7, SBRK
  ecall

  li t0, -1
  beq a0, t0, new_p_error     # check for allocation error

  sd s1, t_person_father(a0)  # initialize node fields
  sd s2, t_person_mother(a0)
  sd s0, t_person_year(a0)
  sd zero, t_person_children(a0)
  j new_p_end

new_p_error:
  li a0, 0                # return 0 on error

new_p_end:
  ld ra, 0(sp)            # restore context
  ld s0, 8(sp)
  ld s1, 16(sp)
  ld s2, 24(sp)
  addi sp, sp, 32         # release stack space
  ret

mGen:
  beqz a0, mgen_return_zero # a null person has level 0

  addi sp, sp, -24
  sd ra, 0(sp)            # save context
  sd s0, 8(sp)
  sd s1, 16(sp)

  mv s0, a0               # s0 = current person

  ld a0, t_person_father(s0) # get father's level
  call mGen
  mv s1, a0               # s1 = father's level

  ld a0, t_person_mother(s0) # get mother's level
  call mGen
  # a0 now holds mother's level

  bgeu s1, a0, mgen_father_max # find max(father_level, mother_level)
  j mgen_calc_level

mgen_father_max:
  mv a0, s1

mgen_calc_level:
  addi a0, a0, 1          # level = 1 + max of parents

  ld ra, 0(sp)            # restore context
  ld s0, 8(sp)
  ld s1, 16(sp)
  addi sp, sp, 24
  ret

mgen_return_zero:
  li a0, 0
  ret

printLignePere:
  beqz a0, print_line_return # do nothing for null input

  addi sp, sp, -16
  sd ra, 0(sp)
  sd s0, 8(sp)

  mv s0, a0               # s0 = current person pointer

print_line_loop:
  beqz s0, print_line_end_loop # exit if no more fathers

  ld a0, t_person_year(s0) # print current person's year
  call printInt
  la a0, space_char
  call printString

  ld s0, t_person_father(s0) # move to the next father
  j print_line_loop

print_line_end_loop:
  la a0, newline_char     # print final newline
  call printString

  ld ra, 0(sp)            # restore context
  ld s0, 8(sp)
  addi sp, sp, 16

print_line_return:
  ret

addEnf:
  beqz a1, add_enf_error  # fail if parent is null

  addi sp, sp, -40        # save context
  sd ra, 0(sp)
  sd s0, 8(sp)
  sd s1, 16(sp)
  sd s2, 24(sp)
  sd s3, 32(sp)

  mv s0, a0               # s0 = child
  mv s1, a1               # s1 = parent

  li a0, t_child_node_size # allocate a new child list node
  li a7, SBRK
  ecall
  li t0, -1
  beq a0, t0, add_enf_error # check for allocation error
  mv s2, a0               # s2 = new list node

  sd s0, t_child_node(s2) # initialize new node
  sd zero, t_child_next(s2)

  ld s3, t_person_children(s1) # s3 = head of children list
  beqz s3, add_enf_list_empty

find_last_loop:               # find the last node in the list
  ld t0, t_child_next(s3)
  bnez t0, find_last_continue
  j append_node

find_last_continue:
  mv s3, t0
  j find_last_loop

add_enf_list_empty:           # handle case where list was empty
  sd s2, t_person_children(s1)
  sd zero, t_child_prev(s2)
  j add_enf_success

append_node:                  # append new node to the end
  sd s2, t_child_next(s3)
  sd s3, t_child_prev(s2)

add_enf_success:
  li a0, 1
  j add_enf_end

add_enf_error:
  li a0, 0

add_enf_end:                  # restore and return
  ld ra, 0(sp)
  ld s0, 8(sp)
  ld s1, 16(sp)
  ld s2, 24(sp)
  ld s3, 32(sp)
  addi sp, sp, 40
  ret

sizeArbre:
  beqz a0, size_arbre_return_zero # null person has no descendants

  addi sp, sp, -24
  sd ra, 0(sp)            # save context
  sd s0, 8(sp)
  sd s1, 16(sp)

  li s0, 0                # s0 = total descendant count
  ld s1, t_person_children(a0) # s1 = current child list node

size_arbre_loop:
  beqz s1, size_arbre_end_loop # exit when out of children

  addi s0, s0, 1          # count direct child

  ld a0, t_child_node(s1) # get child's person node
  call sizeArbre          # recursively count their descendants
  add s0, s0, a0          # add to total

  ld s1, t_child_next(s1) # move to next child
  j size_arbre_loop

size_arbre_end_loop:
  mv a0, s0               # set return value

  ld ra, 0(sp)            # restore context
  ld s0, 8(sp)
  ld s1, 16(sp)
  addi sp, sp, 24
  ret

size_arbre_return_zero:
  li a0, 0
  ret