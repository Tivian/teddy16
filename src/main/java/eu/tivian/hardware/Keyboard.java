package eu.tivian.hardware;

// TODO
//  first make sure TED chips works!!!
//  Pin, Wire and Bus should have thread-safe counterparts (???)
public class Keyboard {
    public final Bus column = new Bus("column", "C", Pin.Direction.OUTPUT, 8);
    public final Bus row    = new Bus("row"   , "R", Pin.Direction.INPUT , 8);

    public final String[][] matrixLower = new String[][] {
        new String[] { "1"   , "home", "ctrl", "stop" , " "  , "cmd", "q" , "2"     },
        new String[] { "3"   , "w"   , "a"   , "shift", "z"  , "s"  , "e" , "4"     },
        new String[] { "5"   , "r"   , "d"   , "x"    , "c"  , "f"  , "t" , "6"     },
        new String[] { "7"   , "y"   , "g"   , "v"    , "b"  , "h"  , "u" , "8"     },
        new String[] { "9"   , "i"   , "j"   , "n"    , "m"  , "k"  , "o" , "0"     },
        new String[] { "down", "p"   , "l"   , ","    , "."  , ":"  , "-" , "up"    },
        new String[] { "left", "*"   , ";"   , "/"    , "esc", "="  , "+" , "right" },
        new String[] { "del" , "ret" , "gbp" , "@"    , "f1" , "f2" , "f3", "help"  }
    };

    public final String[][] matrixUpper = new String[][] {
        new String[] { "!"   , "clr" , "ctrl", "run"  , " "  , "cmd", "Q" , "\""    },
        new String[] { "#"   , "W"   , "A"   , "shift", "Z"  , "S"  , "E" , "$"     },
        new String[] { "%"   , "R"   , "D"   , "X"    , "C"  , "F"  , "T" , "&"     },
        new String[] { "'"   , "Y"   , "G"   , "V"    , "B"  , "H"  , "U" , "("     },
        new String[] { "("   , "I"   , "J"   , "N"    , "M"  , "K"  , "O" , "pow"   },
        new String[] { "down", "P"   , "L"   , "<"    , ">"  , "["  , "-" , "up"    },
        new String[] { "left", "*"   , "}"   , "?"    , "esc", "="  , "+" , "right" },
        new String[] { "inst", "ret" , "gbp" , "@"    , "f1" , "f2" , "f3", "help"  }
    };
}
