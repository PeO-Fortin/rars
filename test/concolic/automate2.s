# This program simulates a cellular automaton based on a given rule number and an initial generation.
# It reads the rule number and the initial generation from the input, processes the generations,
# and prints each generation until the specified number of generations is reached.

# syscall codes
.eqv PRINT_STRING, 4
.eqv PRINT_INT, 1
.eqv READ_INT, 5
.eqv READ_STRING, 8
.eqv EXIT, 10

.data
.eqv LINE_LENGTH, 32 # Length of the line to be processed
.eqv RULE_BIT_LENGTH, 8 # 8 bits for the rule number
.eqv INPUT_BUFFER_SIZE, 40 # Buffer for line 0 (LINE_LENGTH + 1), closest multiple of 8 for stack alignment.

# stores previous generation
previous_generation: .space LINE_LENGTH
# stores processed rule
rule_array: .space RULE_BIT_LENGTH

# strings for printing
generation_prefix: .string " # "
newline_char:      .string "\n"

.text
main:
    # s0 = generations to run
    # s1 = rule number
    # s2 = generation count

    li a7, READ_INT
    ecall
    mv s0, a0 # save number of generations

    li a7, READ_INT
    ecall
    mv s1, a0 # save rule number

    # make space on stack for input string
    addi sp, sp, -INPUT_BUFFER_SIZE

    # read input string to stack
    mv a0, sp
    li a1, INPUT_BUFFER_SIZE
    li a7, READ_STRING
    ecall

    li s2, 0 # init generation count to 0
    call process_rule

    # convert input string to initial generation
    mv a0, sp
    call initialize_generation_zero

    # free stack space from input string
    addi sp, sp, INPUT_BUFFER_SIZE

    # print generation 0
    la a0, previous_generation
    li a1, LINE_LENGTH
    mv a2, s2
    call print_and_update_previous

simulation_loop:
    beqz s0, exit_program # if generations is 0, exit

    addi s0, s0, -1 # decrement generations left
    addi s2, s2, 1  # increment generation count

    # make space on stack for next generation
    addi sp, sp, -LINE_LENGTH

    # calculate new generation
    mv a0, sp
    call generate_new_generation

    # print new generation and save it as previous
    mv a0, sp
    li a1, LINE_LENGTH
    mv a2, s2
    call print_and_update_previous

    # free stack space
    addi sp, sp, LINE_LENGTH

    j simulation_loop

# exit
exit_program:
    li a7, EXIT
    ecall

# function to convert the rule number into a bit array
process_rule:
    addi sp, sp, -32
    sd ra, 0(sp)
    sd s0, 8(sp)
    sd s2, 16(sp)
    sd s3, 24(sp)

    la s0, rule_array      # s0 = base address of rule_array
    li s2, 0               # s2 = loop counter
    li s3, RULE_BIT_LENGTH # s3 = loop limit

process_rule_loop:
    bge s2, s3, process_rule_done # loop until all bits are unpacked
    
    # Get the ith bit from the rule number in s1
    srl t0, s1, s2
    andi t0, t0, 1
    
    # Calculate address and store the bit
    add t1, s0, s2
    sb t0, 0(t1)
    
    addi s2, s2, 1  # increment loop counter
    j process_rule_loop
process_rule_done:
    ld ra, 0(sp)
    ld s0, 8(sp)
    ld s2, 16(sp)
    ld s3, 24(sp)
    addi sp, sp, 32
    ret

# function to initialize the previous generation from the input string
initialize_generation_zero:
    addi sp, sp, -48
    sd ra, 0(sp)
    sd s0, 8(sp)
    sd s1, 16(sp)
    sd s2, 24(sp)
    sd s3, 32(sp)
    sd s4, 40(sp)

    mv s0, a0                   # s0 = source address of input string
    la s1, previous_generation  # s1 = destination address
    li s2, 0                    # s2 = loop counter
    li s3, LINE_LENGTH          # s3 = loop limit
    li s4, 1                    # s4 = flag that indicates we have reached the end of input
initialize_loop:
    bge s2, s3, initialize_done       # Loop 32 times

    # Check s4 to see if we are still processing the input string
    beqz s4, store_zero_padding # If flag is 0, pad with zero

    lb t0, 0(s0)                # Load character from input
    li t1, '\n'
    beq t0, t1, input_ended     # if char == '\n', end of input
    beqz t0, input_ended        # if char == '\0', end of input

    # Convert char to int and store it
    li t1, '0'
    sub t0, t0, t1              # Convert '0' or '1' to 0 or 1
    sb t0, 0(s1)
    addi s0, s0, 1              # Advance pointer
    j next_iteration
input_ended:
    li s4, 0                    # Set flag to 0, indicating input string is finished
store_zero_padding:
    sb zero, 0(s1)              # Store a zero for the current position
next_iteration:
    addi s1, s1, 1              # Advance destination pointer
    addi s2, s2, 1              # Increment loop counter
    j initialize_loop
initialize_done:
    ld ra, 0(sp)
    ld s0, 8(sp)
    ld s1, 16(sp)
    ld s2, 24(sp)
    ld s3, 32(sp)
    ld s4, 40(sp)
    addi sp, sp, 48
    ret

# function to generate the new generation based on the previous one
generate_new_generation:
    addi sp, sp, -32
    sd ra, 0(sp)
    sd s0, 8(sp)
    sd s1, 16(sp)
    sd s2, 24(sp)

    mv s0, a0           # s0 = destination buffer address
    li s1, 0            # s1 = loop counter 'i'
    li s2, LINE_LENGTH  # s2 = loop limit

generate_loop:
    bge s1, s2, generate_done
    
    # get neighbor indices using the counter
    addi t0, s1, -1 # get left neighbor index
    bgez t0, left_value_ok
    li t0, LINE_LENGTH
    addi t0, t0, -1 # handle left wrap-around
left_value_ok:
    addi t1, s1, 1  # get right neighbor index
    blt t1, s2, right_value_ok
    li t1, 0
right_value_ok:
    
    # fetch neighbor values
    la t2, previous_generation
    add t3, t2, t0
    lb t0, 0(t3)    # t0 = left value
    add t3, t2, t1
    lb t1, 0(t3)    # t1 = right value
    add t3, t2, s1
    lb t2, 0(t3)    # t2 = middle value

    # form 3-bit pattern and get new state
    slli t0, t0, 2
    slli t2, t2, 1
    add t0, t0, t2
    add t0, t0, t1
    la t1, rule_array
    add t1, t1, t0
    lb t0, 0(t1) 

    # store new state in the destination buffer
    add t1, s0, s1
    sb t0, 0(t1)

    addi s1, s1, 1 # increment loop counter
    j generate_loop
generate_done:
    ld ra, 0(sp)
    ld s0, 8(sp)
    ld s1, 16(sp)
    ld s2, 24(sp)
    addi sp, sp, 32
    ret

# function to print the current generation and store it as previous
print_and_update_previous:
    addi sp, sp, -32
    sd ra, 0(sp)
    sd s0, 8(sp)
    sd s1, 16(sp)
    sd s2, 24(sp)

    mv s0, a0                   # source address
    la s1, previous_generation  # destination address
    mv s2, a1                   # counter

print_update_loop:
    # Loop until the counter is zero
    beqz s2, print_update_suffix
    
    # Load a cell's value from the source
    lb t0, 0(s0)
    
    # Print the cell's value and store it in the destination
    mv a0, t0
    li a7, PRINT_INT
    ecall
    sb t0, 0(s1)

    # Advance pointers and decrement counter
    addi s0, s0, 1
    addi s1, s1, 1
    addi s2, s2, -1
    j print_update_loop

print_update_suffix:
    # Print the generation suffix
    la a0, generation_prefix
    li a7, PRINT_STRING
    ecall
    mv a0, a2 # Use the argument a2 for generation number
    li a7, PRINT_INT
    ecall
    la a0, newline_char
    li a7, PRINT_STRING
    ecall

    ld ra, 0(sp)
    ld s0, 8(sp)
    ld s1, 16(sp)
    ld s2, 24(sp)
    addi sp, sp, 32
    ret
