.eqv readChar, 12
.eqv printInt, 1
.eqv exit, 10

    li a7, readChar
    ecall
    mv s0, a0
    li t0, 112
    beq s0, t0, c1_ok
    li a0, 0
    j end_prog
c1_ok:

    ecall
    mv s1, a0
    addi t0, s0, 2
    beq s1, t0, c2_ok
    li a0, 0
    j end_prog
c2_ok:

    ecall
    mv s2, a0
    xori t0, s0, 31
    beq s2, t0, c3_ok
    li a0, 0
    j end_prog
c3_ok:

    ecall
    mv s3, a0
    li t1, 97
    or t0, t1, t1
    and t0, t0, t1
    and t0, t0, t1
    ori t0, t0, 2
    ori t0, t0, 4
    beq s3, t0, c4_ok
    li a0, 0
    j end_prog
c4_ok:

    ecall
    mv s4, a0
    or t0, s0, s1
    beq s4, t0, c5_ok
    li a0, 0
    j end_prog
c5_ok:

    ecall
    mv s5, a0
    xor t0, s1, s4
    addi t0, t0, 97
    beq s5, t0, c6_ok
    li a0, 0
    j end_prog
c6_ok:

    ecall
    mv s6, a0
    li t1, 97
    or t0, s3, t1
    and t0, t0, t1
    addi t1, s0, -100
    add t0, t0, t1
    beq s6, t0, c7_ok
    li a0, 0
    j end_prog
c7_ok:

    ecall
    mv s7, a0
    addi t0, s0, -100
    slli t0, t0, 3
    addi t1, s7, -1
    beq t1, t0, c8_ok
    li a0, 0
    j end_prog
c8_ok:

    ecall
    mv t6, a0
    addi t0, s2, -1
    beq t6, t0, c9_ok
    li a0, 0
    j end_prog
c9_ok:

    ecall
    mv a5, a0
    li t1, 97
    sub t0, t6, s6
    mul t0, t1, t0
    li t1, -1
    mul t0, t1, t0
    sub t0, zero, t0
    beq a5, t0, c10_ok
    li a0, 0
    j end_prog
c10_ok:

    ecall
    mv a4, a0
    sub t0, t6, s6
    sub t1, t6, t0
    sub t1, t1, t0
    beq a4, t1, c11_ok
    li a0, 0
    j end_prog
c11_ok:

    ecall
    mv a3, a0
    mv a0, t6
    li a1, 10
    j division
ret_div:
    mv t0, a0
    mul t0, t0, t0
    beq a3, t0, c12_ok
    li a0, 0
    j end_prog
c12_ok:

    ecall
    mv a2, a0

    andi t0, a5, 123
    andi t0, t0, 456
    slli t0, t0, 1
    sub t1, a3, a4
    sub t0, t0, t1
    beq a2, t0, c13_ok
    li a0, 0
    j end_prog
c13_ok:

    ecall
    mv t5, a0
    and t0, a3, s6
    beq t5, t0, c14_ok
    li a0, 0
    j end_prog
c14_ok:

    ecall
    mv t4, a0

    and t0, s1, s2
    or t0, t0, a2
    beq t4, t0, c15_ok
    li a0, 0
    j end_prog
c15_ok:
    li a0, 1

end_prog:
    li a7, printInt
    ecall
    li a7, exit
    ecall
    
division:
    li t0, 1
    bge a0, zero, div_positive
    sub a0, zero, a0
    li t0, -1

div_positive:
    li t1, 0

div_loop:
    addi t2, a0, 1
    blt t2, a1, div_end
    sub a0, a0, a1
    addi t1, t1, 1
    j div_loop

div_end:
    mul a0, t1, t0
    j ret_div