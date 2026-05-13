# This program implements a simple calculator that can perform addition, subtraction,
# multiplication, division, and modulo operations on positive integers.
# It reads characters from the input, interprets them as numbers and operators,
# and performs calculations sequentially.

# syscall
.eqv PrintString, 4
.eqv PrintInt, 1
.eqv ReadChar, 12
.eqv PrintChar, 11
.eqv Exit, 10
# error codes
.eqv E1, 1
.eqv E2, 2

.text

start:
    # current char
    li s0, 0
    # first number
    li s1, -1
    # second number/result
    li s2, -1
    # operator
    li s3, -1
    # temp operator for consecutive operations
    li s4, -1

    # logical flow control
    # should print result next loop
    li s5, -1
    # should print result and quit next loop
    li s6, -1

    # error code
    li s7, -1


main_loop:
    bgez s5, restart_with_result
    bgez s6, exit_with_result
    bgez s4, clear_temp_operator

    jal read_input

    mv s0, a0

    li   t0, ' '
    beq  s0, t0, main_loop
    li   t0, '\t'        
    beq  s0, t0, main_loop
    li   t0, '\n'
    beq  s0, t0, main_loop
    li   t0, '\r'        
    beq  s0, t0, main_loop

    li   t0, '9'
    bgt s0, t0, validate_operator
    li   t0, '0'
    blt s0, t0, validate_operator

    bltz s1, first_number
after_first_number:
    # convert char to int using bit shifting 
    # multiply by 10
    slli t0, s1, 1
    slli t1, s1, 3
    add s1, t0, t1

    # convert char to it's int equivalent
    addi s0, s0, -0x30

    add s1, s1, s0

    j main_loop

first_number:
    li s1, 0
    j after_first_number

calculate:
    li t0, '+'
    beq s3, t0, addition
    li t0, '-'
    beq s3, t0, subtract
    li t0, '*'
    beq s3, t0, multiplication
    li t0, '/'
    beq s3, t0, division
    li t0, '%'
    beq s3, t0, modulo
    # if operator not found, should never happen
    j exit_with_e0_error

validate_operator:
    li   t0, 'q'
    beq  s0, t0, handle_quit

    bltz s1, exit_with_e0_error

    li   t0, '='
    beq  s0, t0, handle_equal

    li   t0, '+'
    beq  s0, t0, load_operator
    li   t0, '-'
    beq  s0, t0, load_operator
    li   t0, '*'
    beq  s0, t0, load_operator
    li   t0, '/'
    beq  s0, t0, load_operator
    li   t0, '%'
    beq  s0, t0, load_operator

    j exit_with_e0_error

handle_equal:
    li s5, 1
    # if error is already set, go to print directly
    bgez s7, main_loop
    bgez, s2, calculate
    mv s2, s1
    j print_result

handle_quit:
    li s6, 1
     # if error is already set, go to print and exit directly
    bgez s7, main_loop
    bgez, s2, calculate
    mv s2, s1
    j exit_with_result

load_operator:
    # if an error is already set, ignore operator
    bgez s7, main_loop
    bgez s3, consecutive_calculation
    mv s3, s0
    mv s2, s1
    li s1, 0
    jal main_loop

consecutive_calculation:
    mv s4, s0
    j calculate

clear_temp_operator:
    li s4, -1
    mv s3, s0
    li s1, 0
    j main_loop

addition:
    add s2, s2, s1
    j main_loop

subtract:
    sub s2, s2, s1
    bltz s2, set_e1_error
    j main_loop

multiplication:
    beqz s1, multiplication_by_zero
    mv t1, s2
    li s2, 0
    li t0, 1
    beq s1, t0, one_multiplier_done
multiplication_loop:
    add s2, s2, t1
    addi s1, s1, -1
    bgtz s1, multiplication_loop
    j main_loop
one_multiplier_done:
    mv s2, t1
    j main_loop
multiplication_by_zero:
    li s2, 0
    j main_loop

division:
    beqz s1, set_e2_error
    li t0, 0
    blt s2, s1, division_done
    beqz s2, division_done
division_loop:
    blt s2, s1, division_loop_end
    sub s2, s2, s1
    addi t0, t0, 1
    j division_loop
division_loop_end:
    mv s2, t0
    j main_loop
division_done:
    mv s2, t0
    j main_loop

modulo:
    beqz s1, set_e2_error
    beqz s2, modulo_done
    blt s2, s1, modulo_done
modulo_loop:
    sub s2, s2, s1
    bge s2, s1, modulo_loop
modulo_done:
    j main_loop

restart_with_result:
    bgez s7, print_error
    li a7, PrintInt
    mv a0, s2
    ecall
    jal print_newline
    j start

print_result:
    li a7, PrintInt
    mv a0, s2
    ecall
    jal print_newline
    j start

read_input:
    li a7, ReadChar
    ecall
    ret

print_newline:
    li a0, '\n'
    li a7, PrintChar
    ecall
    ret

set_e1_error:
    # error already set
    bgez s7, main_loop
    li s7, E1
    j main_loop

set_e2_error:
    # error already set
    bgez s7, main_loop
    li s7, E2
    j main_loop

print_error_prefix:
    li a0, 'E'
    li a7, PrintChar
    ecall
    ret

print_error:
    jal print_error_prefix
    mv a0, s7
    li a7, PrintInt
    ecall
    jal print_newline
    bgez s6, exit
    j start

exit_with_e0_error:
    jal print_error_prefix
    li a0, 0
    li a7, PrintInt
    ecall
    jal print_newline
    j exit

exit_with_result:
    bgez s7, print_error
    li a7, PrintInt
    mv a0, s2
    ecall
    jal print_newline
exit:
    li a7, Exit
    ecall
