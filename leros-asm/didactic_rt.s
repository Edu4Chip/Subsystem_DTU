nop

addr_setup:
    // load peripheral base address 0x8000
    loadi 0x00
    loadhi 0x80
    loadh2i 0x00
    loadh3i 0x00
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
    stind 1    // write to CCR[1]
    stind 2    // write to CCR[2]
    stind 3    // write to CCR[3]
    stind 0x41 // write to GPIO
    andi 0x01  // mask to make binary
    addi 0x30  // make char '0' or '1'
    stind 0x45 // write to uart

loop_setup:
    // initialize sleep counter to CCR[0]
    ldind 0
    brnz loop // if CCR[0] is zero, set to (8_000_000 / 3) for 1s sleep
    loadi 0xaa
    loadhi 0xb0
    loadh2i 0x28
    loadh3i 0x00
loop:
    subi 1
    nop
    brnz loop
    br led_write
