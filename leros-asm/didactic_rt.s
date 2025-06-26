nop

addr_setup:
    // load peripheral base address 0x8000
    loadi 0x00
    loadhi 0x80
    loadh2i 0x00
    store r1
    ldaddr r1

led_setup:
    // initialize counter
    loadi 0
    store r2

led_write:
    load r2
    addi 1     // increment counter
    store r2
    stind 0    // write to CCR[0]
    stind 0x41 // write to GPIO
    andi 0x01  // mask to make binary
    addi 0x30  // make char '0' or '1'
    stind 0x45 // write to uart

loop_setup:
    // initialize sleep counter to 1s
    loadi 80
    loadhi 96
    loadh2i 98
    loadh3i 00
loop:
    // loop for 1s
    subi 1
    nop
    brnz loop
    br led_write
