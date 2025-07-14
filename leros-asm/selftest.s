// Set indirect base address to 0x8000
        loadi   0x00
        loadhi  0x80
        loadh2i 0x00
        store   r1
        ldaddr  r1

wait_seed:
        ldind   0           // Load CCR[0]
        brz     wait_seed   // If 0, loop

        // check r1
        store   r1
        load    r1

        // check r2
        store    r2
        load    r2

        // check r3
        store   r3
        load    r3

        // check r4
        store   r4
        load    r4

        // check r5
        store   r5
        load    r5

        // check r6
        store   r6
        load    r6  

        // check r7
        store   r7
        load    r7

        // check gpio
        stind 0x41 // write to GPIO
        ldind 0x41 // read GPIO

        // check uart

        store r1
        stind 0x45 // write to uart

        // wait for uart
wait_uart:
        ldind 0x44 // read flags
        andi 0x01 // check if uart is ready
        brz wait_uart // If not ready, loop

        // load uart data
        ldind 0x45 // read uart data

// finalize
        stind 1 // write final result to CCR[1]
        loadi 1 
        stind 0 // signal ready on CCR[0]