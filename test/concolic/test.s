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

li a7, exit
ecall
