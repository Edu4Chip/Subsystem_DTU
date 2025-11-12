# Ponte CLI

The Ponte CLI tool implements the Ponte protocol (see [doc](../src/main/scala/ponte/readme.md)) to send APB transactions over a serial link.

```shell
$ ./tools/ponte-cli --help
Simple CLI for communicating with the Ponte Serial → APB bridge

Usage: ponte-cli [OPTIONS] --port <PORT> <COMMAND>

Commands:
  read        Read a 32-bit word from address
  write       Write a 32-bit word to address
  write-file  
  read-file   
  help        Print this message or the help of the given subcommand(s)

Options:
  -p, --port <PORT>  Serial port device (e.g. /dev/ttyUSB0 or COM3)
  -b, --baud <BAUD>  Baud rate (default: 115200) [default: 115200]
  -h, --help         Print help
  -V, --version      Print version
```

## Read

```shell
$ ./tools/ponte-cli read --help
Read a 32-bit word from address

Usage: ponte-cli --port <PORT> read [OPTIONS] --address <ADDRESS>

Options:
  -n <WORDS>               Number of 32-bit words to read [default: 1]
  -a, --address <ADDRESS>  Byte address to read from
  -h, --help               Print help
```

Example:

```shell
$ ./tools/ponte-cli -p /dev/ttyUSB0 -b 921600 read --address 0x00000000 -n 4
0x12345678 0x9abcdef0 0x00000000 0xdeadbeef
```

# Write

```shell
./tools/ponte-cli write --help
Write a 32-bit word to address

Usage: ponte-cli --port <PORT> write --address <ADDRESS> [VALUES]...

Arguments:
  [VALUES]...  One or more 32-bit values to write (hex like 0x1234 or decimal)

Options:
  -a, --address <ADDRESS>  Start byte address to write to
  -h, --help               Print help
```

Example:

```shell
$ ./tools/ponte-cli -p /dev/ttyUSB0 -b 921600 write --address 0x00000000 0xdeadbeef 0x12345678
```


## Read File

```shell
./tools/ponte-cli read-file --help
Usage: ponte-cli --port <PORT> read-file --address <ADDRESS> -n <WORDS> <FILE_PATH>

Arguments:
  <FILE_PATH>  Path to the file to save the data to

Options:
  -a, --address <ADDRESS>  Byte address to read from
  -n <WORDS>               Number of 32-bit words to read
  -h, --help               Print help
```

Example:

```shell
$ ./tools/ponte-cli -p /dev/ttyUSB0 -b 921600 read-file --address 0x00000000 -n 1024 dump.bin
```

## Write File

```shell
./tools/ponte-cli write-file --help
Usage: ponte-cli --port <PORT> write-file --address <ADDRESS> <FILE_PATH>

Arguments:
  <FILE_PATH>  Path to the file to write

Options:
  -a, --address <ADDRESS>  Byte address to dump the file contents to
  -h, --help               Print help
```

Example:

```shell
$ ./tools/ponte-cli -p /dev/ttyUSB0 -b 921600 write-file --address 0x00000000 dump.bin
```
