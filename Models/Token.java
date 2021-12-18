package Models;

/*
    Comp3290 Compiler Design
    Models.Token.java class - Handles all data about the tokens
    Created by - Keeylan Hume C3320396

 */

public class Token {

    private int lineNo, tokenNo, colNo;
    private String lexeme;

    public static final int
		T_EOF =  0,	  // Models.Token value for end of file
	
        // The 31 keywords
        TCD21 = 1, // CD21
        TCONS = 2,  // Constants
        TTYPS = 3,	// Types
        TTTIS = 4,	// Is?
        TARRS = 5,  // Arrays
        TMAIN = 6,  // Main
        TBEGN = 7, // Begin
        TTEND = 8,  // End
        TARAY = 9,  // Array
        TTTOF = 10, // Of
        TFUNC = 11, // Func
        TVOID = 12, // Void
        TCNST = 13, // Const
        TINTG = 14, // Integer
        TREAL = 15, // Real
        TBOOL = 16, // Boolean
        TTFOR = 17, // For
        TREPT = 18, // Repeat
        TUNTL = 19, // Until
        TIFTH = 20, // If
        TELSE = 21, // Else
        TINPT = 22, // In
        TOUTP = 23, // Out
        TOUTL = 24, // Line
        TRETN = 25, // Return
        TNOTT = 26, // Not
        TTAND = 27, // And
        TTTOR = 28, // Or
        TTXOR = 29, // Xor
        TTRUE = 30, // True
        TFALS = 31, // False

        // the operators and delimiters
        TCOMA = 32, // ","
        TLBRK = 33, // "["
        TRBRK = 34, // "]"
        TLPAR = 35, // "("
        TRPAR = 36, // ")"
        TEQUL = 37, // "="
        TPLUS = 38, // "+"
        TMINS = 39, // "-"
        TSTAR = 40, // "*"
        TDIVD = 41, // "/"
        TPERC = 42, // "%"
        TCART = 43, // "^"
        TLESS = 44, // "<"
        TGRTR = 45, // ">"
        TCOLN = 46, // ":"
        TLEQL = 47, // "<="
        TGEQL = 48, // ">="
        TNEQL = 49, // "!="
        TEQEQ = 50, // "=="
        TPLEQ = 51, // "+="
        TMNEQ = 52, // "-="
        TSTEQ = 53, // "*="
        TDVEQ = 54, // "/="
        TSEMI = 55, // ";"
        TDOTT = 56, // "."
        TGRGR = 57, // >>
        TLSLS = 58, // <<

        // the tokens which need tuple values
        TIDEN = 59,  // Ident -> a-zA-Z or 0-9
        TILIT = 60,	// 0-9
        TFLIT = 61, // 0-9+.0-9+
        TSTRG = 62, // " "
        TUNDF = 63; // Undefined token

    private static final String[] TPRINT = {
            "T_EOF ",
            "TCD21 ",	"TCONS ",	"TTYPS ",	"TTTIS ",	"TARRS ",	"TMAIN ",
            "TBEGN ",	"TTEND ",	"TARAY ",	"TTTOF ",	"TFUNC ",	"TVOID ",
            "TCNST ",	"TINTG ",	"TREAL ",	"TBOOL ",	"TTFOR ",	"TREPT ",
            "TUNTL ",	"TIFTH ",	"TELSE ",	"TINPT ",	"TOUTP ",	"TOUTL ",
            "TRETN ",	"TNOTT ",	"TTAND ",	"TTTOR ",	"TTXOR ",	"TTRUE ",
            "TFALS ",	"TCOMA ",	"TLBRK ",	"TRBRK ",	"TLPAR ",	"TRPAR ",
            "TEQUL ",	"TPLUS ",	"TMINS ",	"TSTAR ",	"TDIVD ",	"TPERC ",
            "TCART ",	"TLESS ",	"TGRTR ",	"TCOLN ",	"TLEQL ",	"TGEQL ",
            "TNEQL ",	"TEQEQ ",	"TPLEQ ",	"TMNEQ ",	"TSTEQ ",	"TDVEQ ",
            "TSEMI ",	"TDOTT ",   "TGRGR ",   "TLSLS ",
            "TIDEN ",	"TILIT ",	"TFLIT ",	"TSTRG ",	"TUNDF "
    };

    private static final String[] TPRINT_TOKEN = {
            "T_EOF ",
            "CD21",	"constans",	"types",	"is ",	"arrays ",	"main ",
            "begin ",	"end ",	"array ",	"of ",	"func ",	"void ",
            "constant ",	"integer ",	"real ",	"boolean ",	"for ",	"repeat ",
            "until ",	"if ",	"else ",	"in ",	"out ",	"line ",
            "return ",	"not ",	"and ",	"or ",	"xor ",	"true ",
            "false ",	", ",	"[ ",	"] ",	"( ",	") ",
            "= ",	"+ ",	"- ",	"* ",	"/ ",	"% ",
            "^ ",	"< ",	"> ",	": ",	"<= ",	">= ",
            "!= ",	"== ",	"+= ",	"-= ",	"*= ",	"/= ",
            "; ",	". ",   ">> ",   "<< ",
            "identifier ",	"integer literal ",	"float literal ",	"string ",	"TUNDF "
    };

    /*
        Default Models.Token constructor
    */
    public Token() {
        this.tokenNo = 0;
        this.lexeme = "";
        this.lineNo = 0;
        this.colNo = 0;
    }

    /*
        Initiate Models.Token with constructed values
    */
    public Token(int tokenNo, String lexeme, int lineNo, int colNo) {
        this.tokenNo = tokenNo;
        this.lexeme = lexeme;
        this.lineNo = lineNo;
        this.colNo = colNo;
    }

    /*
        Overloaded method for setting token details
    */
    public void setTokenDetails(int tokenNo, int lineNo, int colNo) {
        this.tokenNo = tokenNo;
        this.lineNo = lineNo;
        this.colNo = colNo;
    }

    public void setTokenDetails(int tokenNo, String lexeme, int lineNo, int colNo) {
        this.tokenNo = tokenNo;
        this.lexeme = lexeme;
        this.lineNo = lineNo;
        this.colNo = colNo;
    }

    /*
        Check the token if its part of the reserved words list
     */
    public int checkReservedWords(String s) {

        switch(s.toLowerCase()) {

            case "cd21":
                return TCD21;

            case "constants":
                return TCONS;

            case "types":
                return TTYPS;

            case "is":
                return TTTIS;

            case "array":
                return TARAY;

            case "main":
                return TMAIN;

            case "begin":
                return TBEGN;

            case "end":
                return TTEND;

            case "arrays":
                return TARRS;

            case "of":
                return TTTOF;

            case "func":
                return TFUNC;

            case "void":
                return TVOID;

            case "const":
                return TCNST;

            case "integer":
                return TINTG;

            case "real":
                return TREAL;

            case "boolean":
                return TBOOL;

            case "for":
                return TTFOR;

            case "repeat":
                return TREPT;

            case "until":
                return TUNTL;

            case "if":
                return TIFTH;

            case "else":
                return TELSE;

            case "in":
                return TINPT;

            case "out":
                return TOUTP;

            case "line":
                return TOUTL;

            case "return":
                return TRETN;

            case "not":
                return TNOTT;

            case "and":
                return TTAND;

            case "or":
                return TTTOR;

            case "xor":
                return TTXOR;

            case "true":
                return TTRUE;

            case "false":
                return TFALS;

        }

        // Doesnt match any reserved word
        return -1;
    }

    /*
        Overloaded checkDeliminators - Due to the possibility
        of 2 chars, !=, -=, *= ....
     */
    public int checkDeliminators(String s) {

        switch(s) {
            case "<=":
                return TLEQL;

            case ">=":
                return TGEQL;

            case "!=":
                return TNEQL;

            case "==":
                return TEQEQ;

            case "+=":
                return TPLEQ;

            case "-=":
                return TMNEQ;

            case "*=":
                return TSTEQ;

            case "/=":
                return TDVEQ;

            case "<<":
                return TLSLS;

            case ">>":
                return TGRGR;
        }
        return -1;
    }

    public int checkDeliminators(char c) {

        switch(String.valueOf(c)) {

            case ",": // ,
                return TCOMA;

            case "[": // [
                return TLBRK;

            case "]": // ]
                return TRBRK;

            case "(": // (
                return TLPAR;

            case ")": // )
                return TRPAR;

            case "=": // =
                return TEQUL;

            case "+": // +
                return TPLUS;

            case "-": // -
                return TMINS;

            case "*": // *
                return TSTAR;

            case "/": // /
                return TDIVD;

            case "%": // %
                return TPERC;

            case "^": // ^
                return TCART;

            case "<": // <
                return TLESS;

            case ">": // >
                return TGRTR;

            case ":": // :
                return TCOLN;

            case "<=":
                return TLEQL;

            case ">=":
                return TGEQL;

            case "!=":
                return TNEQL;

            case "==":
                return TEQEQ;

            case "+=":
                return TPLEQ;

            case "-=":
                return TMNEQ;

            case "*=":
                return TSTEQ;

            case "/=":
                return TDVEQ;

            case "<<":
                return TLSLS;

            case ">>":
                return TGRGR;

            case ";": // ;
                return TSEMI;

            case ".": // .
                return TDOTT;

        }
        return -1;
    }

    /*
        Returns the token to string for printing out via scanner
        Checks if the token has a lexeme to print, or just the tokenID
        Error printing will use the line/colNo
     */
    public String toString() {

        if(this.lexeme.isEmpty()) {

            return TPRINT[this.tokenNo];
        }

        return TPRINT[this.tokenNo] + this.lexeme + " ";

    }

    /*
        Below are boring getters/setters
     */
    public int getLineNo() {
        return lineNo;
    }

    public int getColNo() {
        return colNo;
    }

    public int getTokenNo() {
        return this.tokenNo;
    }

    public String getTokenType() {
        return TPRINT[this.tokenNo];
    }

    public String getLexeme() {
        return (lexeme.equals("")) ? TPRINT_TOKEN[this.tokenNo] : this.lexeme;
    }
}
