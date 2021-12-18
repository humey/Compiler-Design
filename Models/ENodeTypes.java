/*
    Comp3290 Compiler Design
    EnodeTypes.java class - Enum style class that holds all the needed data for the node types
                            Easy accessible token numbers, string types

    Created by - Keeylan Hume C3320396

 */

package Models;

import java.util.HashMap;
import java.util.Map;

public enum ENodeTypes {

    NPROG(0, "NPROG"),
    NGLOB(1, "NGLOB"),
    NILIST(2, "NILIST"),
    NINIT(3, "NINIT"),
    NFUNCS(4, "NFUNCS"),
    NMAIN(5, "NMAIN"),
    NSDLST(6, "NSDLST"),
    NTYPEL(7, "NTYPEL"),
    NRTYPE(8, "NRTYPE"),
    NATYPE(9, "NATYPE"),
    NFLIST(10, "NFLIST"),
    NSDECL(11, "NSDECL"),
    NALIST(12, "NALIST"),
    NARRD(13, "NARRD"),
    NFUND(14, "NFUND"),
    NPLIST(15, "NPLIST"),
    NSIMP(16, "NSIMP"),
    NARRP(17, "NARRP"),
    NARRC(18, "NARRC"),
    NDLIST(19, "NDLIST"),
    NSTATS(20, "NSTATS"),
    NFORL(21, "NFORL"),
    NREPT(22, "NREPT"),
    NASGNS(23, "NASGNS"),
    NIFTH(24, "NIFTH"),
    NIFTE(25, "NIFTE"),
    NASGN(26, "NASGN"),
    NPLEQ(27, "NPLEQ"),
    NMNEQ(28, "NMNEQ"),
    NSTEA(29, "NSTEA"),
    NDVEQ(30, "NDVEQ"),
    NINPUT(31, "NINPUT"),
    NOUTP(33, "NOUTP"),
    NOUTL(34, "NOUTL"),
    NCALL(35, "NCALL"),
    NRETN(36, "NRETN"),
    NVLIST(37, "NVLIST"),
    NSIMV(38, "NSIMV"),
    NARRV(39, "NARRV"),
    NAELT(40, "NAELT"),
    NEXPL(41, "NEXPL"),
    NBOOL(42, "NBOOL"),
    NNOT(43, "NNOT"),
    NAND(44, "NAND"),
    NOR(45, "NOR"),
    NXOR(46, "NXOR"),
    NEQL(47, "NEQL"),
    NNEQ(48, "NNEQ"),
    NGRT(49, "NGRT"),
    NLSS(50, "NLSS"),
    NLEQ(51, "NLEQ"),
    NGEQ(52, "NGEQ"),
    NADD(53, "NADD"),
    NSUB(54, "NSUB"),
    NMUL(55, "NMUL"),
    NDIV(56, "NDIV"),
    NMOD(57, "NMOD"),
    NPOW(58, "NPOW"),
    NILIT(59, "NILIT"),
    NFLIT(60, "NFLIT"),
    NTRUE(61, "NTRUE"),
    NFALS(62, "NFALS"),
    NFCALL(63, "NFCALL"),
    NPRLST(64, "NPRLST"),
    NSTRG(65, "NSTRG"),
    NUNDF(66, "NUNDF");

    /*
        Mapping the NodeType values this is to grab the string value from the number
     */
    private static final Map<Integer, String> BY_NUMBER = new HashMap<>();
    static {
        for(ENodeTypes t : values()) {
            BY_NUMBER.put(t.nodeNumber, t.nodeString);
        }
    }

    public final int nodeNumber;
    public final String nodeString;

    ENodeTypes(int nodeNumber, String nodeString) {
        this.nodeNumber = nodeNumber;
        this.nodeString = nodeString;
    }

    public static String valueByNumber(int number) {
        return BY_NUMBER.get(number);
    }
}