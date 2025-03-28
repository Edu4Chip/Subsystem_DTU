nop

addr_setup:
    loadi 0x00
    loadhi 0x80
    loadh2i 0x00
    store r1
    ldaddr r1

led_setup:
    loadi 0
    store r2

led_write:
    load r2
    addi 1
    store r2
    stind 0x41

loop_setup:
    loadi 80
    loadhi 96
    loadh2i 98
    loadh3i 00
loop:
    subi 1
    nop
    brnz loop
    br led_write
