package Models;

import java.util.HashMap;
import java.util.Map;

public enum EOpCodes {

    HALT(0,"0"), // Stop exectution
    NOOP(1,"1"), // Do nothing
    TRAP(2,"2"), // Stop execution - abort
    ZERO(3,"3"), // Push the INTG zero onto the stack
    FALSE(4,"4"), // Push the BOOL fast onto the stack
    TRUE(5,"5"), // Push the BOOL true onto the stack
    TYPE(7,"7"), // Swap types INTG >> FLOT , FLOT >> INTG
    ITYPE(8,"8"), // type for TOS to INTG
    FTYPE(9,"9"), // type for TOS to FLOT
    ADD(11,"11"), // add them, push the result
    SUB(12,"12"), // subtract first popped from second, push
    MUL(13,"13"), // multiply them, push the result
    DIV(14,"14"), // divide second popped by first, push
    REM(15,"15"), // second popped MOD first, push
    POW(16,"16"), // raise second popped to power of first, push
    CHS(17,"17"), // push negative of operand popped
    ABS(18,"18"), // push absolute value of operand popped
    GT(21,"21"), // if TOS > 0, push true else false
    GE(22,"22"), // if TOS >= 0, push true else false
    LT(23,"23"), // if TOS < 0, push true else false
    LE(24,"24"), // if TOS <= 0, push true else false
    EQ(25,"25"), // if TOS == 0, push true else false ---- if | TOS | < 0.000001, push true else false
    NE(26,"26"), // if TOS != 0, push true else false ---- if | TOS | > 0.000001, push true else false
    AND(31,"31"), // if (popped)(b1 & b2), push true else false
    OR(32,"32"), // if (b1 | b2), push true else false
    XOR(33,"33"), // if (b1 excusive - or b2), push true else false
    NOT(34,"34"),
    BT(35,"35"),
    BF(36,"36"),
    BR(37,"37"),
    L(40,"40"),
    LB(41,"41"), // Load bytes
    LH(42,"42"),
    ST(43,"43"), // store
    STEP(51,"51"),
    ALLOC(52,"52"),
    ARRAY(53,"53"),
    INDEX(54,"54"),
    SIZE(55,"55"),
    DUP(56,"56"),
    READF(60,"60"),
    READI(61,"61"),
    VALPR(62,"62"),
    STRPR(63,"63"),
    CHRPR(64,"64"),
    NEWLN(65,"65"),
    SPACE(66,"66"),
    RVAL(70,"70"),
    RETN(71,"71"),
    JS2(72,"72"),

    /*
        Load value - Base register 0/1/2
    */
    LV0(80, "80"),
    LV1(81,"81"),
    LV2(82, "82"),

    /*
        Load address - Base register 0/1/2
    */
    LA0(90,"90"),
    LA1(91,"91"),
    LA2(92,"92");

    private static final Map<Integer, String> BY_NUMBER = new HashMap<>();
    static {
        for(EOpCodes t : values()) {
            BY_NUMBER.put(t.opcode, t.opCodeString);
        }
    }

    public final int opcode;
    public final String opCodeString;

    EOpCodes(int opcode, String opCodeString) {
        this.opcode = opcode;
        this.opCodeString = opCodeString;
    }

    public static String valueByNumber(int number) {
        return BY_NUMBER.get(number);
    }
}