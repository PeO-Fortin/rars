.data

.eqv exit 10
.eqv printInt 1

mot: .word 42

.text

li t0, 43

la t1, mot
sw t0, 0(t1)
lw a0, 0(t1)

li a7, printInt
ecall

addi sp, sp, -1
li t0, 44
sb t0, 0(sp)
lb a0, 0(sp)
addi sp, sp, 1

ecall

li a7, exit
ecall
