.data

mot: .word 42

.text

li t0, 55555555555
li t1, 22222222222
#li t2, 0x10000000000002
mulw t3, t0, t1

#la t1, mot
#lw t0, 0(t1)
#sw t0, 0(t1)

li a7, 10
ecall
