nop

regs:
    loadi 0x00
    loadhi 0x80
    loadh2i 0x00
    store r1
    ldaddr r1
    loadi 0xAF
    loadhi 0xBA
    loadh2i 0xED
    loadh3i 0xDA
    stind 0
    stind 1
    stind 2
    stind 3
uart:
    loadi 0x75
    stind 0x45
start:
    loadi 0x01
    stind 0x41
loop:
    ldind 0x41
    addi 1
    stind 0x41
    br loop
