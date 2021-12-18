/*
    Comp3290 Compiler Design
    CodeGen.java class - Codegen class that just does some simple maths, prints out strings
                         ability to use floats
    Created by - Keeylan Hume C3320396

 */

package Generator;

import Models.ENodeTypes;
import Models.EOpCodes;
import Models.TreeNode;

import java.util.*;

public class CodeGen {

    private ArrayList<String> programCode = new ArrayList<>();
    private String opString = "";
    private int programCounter = 0, stringCounter = 0, lineCount = 0, addressCount = 0, stringLineCount = 0;
    private int lb = 0;
    private ArrayList<String> instructionSet = new ArrayList<>();
    private StringBuilder sbIs = new StringBuilder(); // Instruction Set
    private StringBuilder sbSc = new StringBuilder(); // String constants
    private StringBuilder sbIc = new StringBuilder(); // Integer constants
    private StringBuilder sbFc = new StringBuilder(); // Float constants
    private ArrayList<byte[]> stringConstants = new ArrayList<>();
    private ArrayList<String> intConstants = new ArrayList<>();
    private ArrayList<String> floatConstants = new ArrayList<>();
    private ArrayList<Integer> backFillString = new ArrayList<>();
    private ArrayList<Integer> backFillFloat = new ArrayList<>();

    public CodeGen() {}

    public String generateMyCode(TreeNode root) {

        // Run main node first
        TreeNode main = root.getRight();

        // Get the amount of variable bytes needing to load
        getMainList(main.getLeft());

        // Add start of instruction
        addInstruction(EOpCodes.LB.opCodeString);
        addInstruction(String.valueOf(lb));
        addInstruction(EOpCodes.ALLOC.opCodeString);

        statsNode(main.getRight());

        // End of stats fill end of instruction set
        fillConstants();

        String s = constructOutput();

        return s;

    }

    /*
        Adds to the instruction set arrayList while keeping track
        of address count, line numbers and program count to help with
        back filling later
     */
    private void addInstruction(String i) {

        StringBuilder sb = new StringBuilder();


//        if(i.length() < 2) {
//            sb.append("0");
//        }

        if(stringCounter >= 8) {
            stringCounter = 0;

           // sb.append("\n");

            addressCount += 8;

            lineCount++;
        }

        sb.append(i).append(" ");
        instructionSet.add(sb.toString());

        stringCounter++;

        programCounter++;

    }

    /*
        Used for the constant section as we use different string builders
        for each section
     */
    private void appendInstruction(String i, StringBuilder sb) {

        // Keeps the string within the 8 byte length rule
        if(stringLineCount >= 8) {

            // Append new line and start counter again
            stringLineCount = 0;
            sb.append("\n");

            addressCount += 8;

            // Increase Line count for mod file
            lineCount++;
        }

        programCounter++;

        sb.append(i).append(" ");
        stringLineCount++;
    }

    private void statsNode(TreeNode n) {

        // Recurse through stats node, others as well?
        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NSTATS.nodeString) ||
           n.getNodeValue().equalsIgnoreCase(ENodeTypes.NASGN.nodeString)) {

            if(n.getLeft() != null) {
                statsNode(n.getLeft());
            }

            if(n.getMiddle() != null) {
                statsNode(n.getMiddle());
            }

            if(n.getRight() != null) {
                statsNode(n.getRight());
            }
        }

        // Check the main nodes for which action to take
        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NOUTL.nodeString)) {
            // Left will be NPRLST
            outLine(n.getLeft());
        }

        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NINPUT.nodeString)) {

            statsNode(n.getLeft());
            addInstruction(EOpCodes.READI.opCodeString);
            addInstruction(EOpCodes.ST.opCodeString);

        }

        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NSIMV.nodeString)) {

            addInstruction(EOpCodes.LA1.opCodeString);
            //addAddress(String.valueOf(n.getSymbolTableRecord().getOffset(1)));
            addInstruction("0");
            addInstruction("0");
            addInstruction("0");
            addInstruction(String.valueOf(n.getSymbolTableRecord().getOffset(1)));

        }

        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NADD.nodeString)) {

            addSubMulDivNode(n, "add");

            //addInstruction(EOpCodes.ST.opCodeString);

        }

        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NSUB.nodeString)) {

            addSubMulDivNode(n, "sub");

            //addInstruction(EOpCodes.ST.opCodeString);

        }

        if (n.getNodeValue().equalsIgnoreCase(ENodeTypes.NMUL.nodeString)) {

            addSubMulDivNode(n, "multiply");

        }

        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NILIT.nodeString)) {

            addInstruction(EOpCodes.LB.opCodeString);
            String lb = n.getSymbolTableRecord().getValue();
            lb += " ";
            instructionSet.add(lb);

        }

        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NFLIT.nodeString)) {

            addInstruction(EOpCodes.LV0.opCodeString);

            if(!floatConstants.contains(n.getSymbolTableRecord().getValue())) {
                floatConstants.add(n.getSymbolTableRecord().getValue());
            }

            addInstruction("0");
            addInstruction("0");
            addInstruction("0");
            addInstruction(n.getSymbolTableRecord().getValue());
            backFillFloat.add(programCounter);

        }

        if(n.getNodeValue().equalsIgnoreCase(ENodeTypes.NDIV.nodeString)) {
            addSubMulDivNode(n, "divide");
        }

    }

    /*
        Recursive add / sub / multiply and divide code gen
     */
    private void addSubMulDivNode(TreeNode n, String type) {
        if(n.getLeft().getNodeValue().equalsIgnoreCase(ENodeTypes.NSIMV.nodeString)) {

            addInstruction(EOpCodes.LV1.opCodeString);
            addInstruction("0");
            addInstruction("0");
            addInstruction("0");
            addInstruction(String.valueOf(n.getLeft().getSymbolTableRecord().getOffset(1)));

        }
        else if(n.getLeft().getNodeValue().equalsIgnoreCase(ENodeTypes.NILIT.nodeString)) {
            // NILIT - check if it has the ability to lb without using constants
            // Load the constant value
            addInstruction(EOpCodes.LB.opCodeString);

            String lb = n.getLeft().getSymbolTableRecord().getValue();
            lb += " ";
            addInstruction(lb);
        }
        else {
            statsNode(n.getLeft());
        }

        if(n.getRight().getNodeValue().equalsIgnoreCase(ENodeTypes.NSIMV.nodeString)) {

            addInstruction(EOpCodes.LV1.opCodeString);
            addInstruction("0");
            addInstruction("0");
            addInstruction("0");
            addInstruction(String.valueOf(n.getRight().getSymbolTableRecord().getOffset(1)));

        }
        else if(n.getRight().getNodeValue().equalsIgnoreCase(ENodeTypes.NILIT.nodeString)) {
            // NILIT - check if it has the ability to lb without using constants
            addInstruction(EOpCodes.LB.opCodeString);

            String lb = n.getRight().getSymbolTableRecord().getValue();
            lb += " ";
            addInstruction(lb);
        }
        else {

            statsNode(n.getRight());

        }

        switch(type) {

            case "add":
                addInstruction(EOpCodes.ADD.opCodeString);
                break;
            case "sub":
                addInstruction(EOpCodes.SUB.opCodeString);
                break;

            case "multiply":
                addInstruction(EOpCodes.MUL.opCodeString);

            case "divide":
                addInstruction(EOpCodes.DIV.opCodeString);

        }

        addInstruction(EOpCodes.ST.opCodeString);
    }

    /*
                        NPRLIST
        NSTRG, NULL | NSTRG, NSTRG | NSTRG , NPRLST
     */
    private void outLine(TreeNode n) {

        if(n == null) {
            return;
        }

        switch(n.getNodeValue()) {

            case "NSTRG":
                //appendInstruction(EOpCodes.LA0.opCodeString, sbIs);
                addInstruction(EOpCodes.LA0.opCodeString);
                // Need to put a place holder for when we know the
                // instruction area addres - 4 bytes long


                // Add string to byte array
                // Need to check if already exists
                if(!checkExists(n.getSymbolTableRecord().getName().getBytes())) {
                    // New string doesnt exist in the byte array
                    stringConstants.add(n.getSymbolTableRecord().getName().getBytes());
                }
                // Use programCounter * 3 to get the place to start replacing the placeholders
//                backFillPlaces.put(programCounter * 3, n.getSymbolTableRecord().getName().getBytes());
//
//                setPlaceHolder("s");

                // Add the string to Instruction set to set the address value after

                // First 3 bytes of address
                addInstruction("0");
                addInstruction("0");
                addInstruction("0");
                // Last byte being the address - place the string and backfill later
                addInstruction(n.getSymbolTableRecord().getName());
                backFillString.add(programCounter);

                addInstruction(EOpCodes.STRPR.opCodeString);
               // appendInstruction(EOpCodes.STRPR.opCodeString, sbIs);
                break;

            case "NPRLST":
                outLine(n.getLeft());
                outLine(n.getRight());

            // null case
            default:
               break;
        }
    }

    /*
        Used to check if a string constant has been used again
        saves saving multiple and re-checking later
     */
    private boolean checkExists(byte[] bString) {

        for(byte[] b : stringConstants) {
            if(Arrays.equals(b, bString)) {
                return true;
            }
        }
        return false;
    }

    /*
        Recursive method to get the amount of bytes needed to load
        on instruction set
    */
    private void getMainList(TreeNode root) {

        if(root == null) {
            return;
        }

        if(root.getNodeValue().equalsIgnoreCase(ENodeTypes.NSDECL.nodeString)) {
            lb++;
        }

        getMainList(root.getLeft());
        getMainList(root.getRight());
    }

    /*
        Fills the constant section and back fills the instruction set
        The constant section in the language hasn't been included
     */
    private void fillConstants() {

        // We are now in constants so we know the IS is finished
        // Add to our addressCount for each constant section


        // Append line after instruction set
        if(intConstants.size() != 0) {
            // Add amount of constants
            sbIc.append(intConstants.size()).append("\n");

            for(String i : intConstants) {
                sbIc.append(i).append("\n");
            }
        }
        else {
          // addInstruction("0");
           //instructionSet.add("\n");

           sbIc.append("0").append("\n");
        }

        if(floatConstants.size() != 0) {
            // Add amount of constants
            sbFc.append(floatConstants.size()).append("\n");

            for(String f : floatConstants) {
                sbFc.append(f).append("\n");
                addressCount += 8;

                f += " ";

                // Go back and backfill the address placeholders
                Collections.replaceAll(instructionSet, f, addressCount + " ");

            }
        }
        else {
//            addInstruction("0");
//            instructionSet.add("\n");
            sbFc.append("0").append("\n");
        }

        if(stringConstants.size() != 0) {

            // Start of our string constant will be +8 of current address
            addressCount += 8;

            int totalStringLines = 0;
            int lines = 0;
            int stringSectionLength = 0;

            for (byte[] stringConstant : stringConstants) {

                totalStringLines += stringConstant.length;

            }

            while(totalStringLines > 0) {

                lines++;

                totalStringLines -= 8;
            }

            sbSc.append(lines).append("\n");

            for (byte[] stringConstant : stringConstants) {

                int wordLocation = addressCount;

                if(stringConstant.length > 8) {
                    addressCount += 8;
                }

                for (byte b : stringConstant) {
                    stringSectionLength++;
                    //addInstruction(Byte.toString(b));
                    //sbSc.append(Byte.toString(b))
                    appendInstruction(Byte.toString(b), sbSc);

                }

                // Go back fill the instruction set as we now know the LA0
                String word = new String(stringConstant);
                word += " ";
                Collections.replaceAll(instructionSet, word, wordLocation + " ");

            }


            fillInstruction(sbSc, stringSectionLength);


        }
        else {
            sbSc.append("0").append("\n");
        }

    }

    /*
        Fills the rest of the instruction set or string constant
        section with blank bytes
     */
    private void fillInstruction(StringBuilder sb, int length) {

        while(length % 8 != 0) {

            // Pad 0 onto instruction set
            sb.append("00 ");
            length++;

        }
    }

    /*
        constructs the output within the simulator rules
     */
    private String constructOutput() {

        StringBuilder sb = new StringBuilder();

        int instructionCount = 0;
        int lineCount = 1;

        for(String s : instructionSet) {

            if(instructionCount == 8) {
                sb.append("\n");

                lineCount++;
                instructionCount = 0;
            }

            sb.append(s);

            instructionCount++;

        }

        while(instructionCount % 8 != 0) {

            // Pad 0 onto instruction set
            sb.append("00 ");

            instructionCount++;

        }

        sb.insert(0, lineCount + "\n");
        sb.append("\n");

        sb.append(sbIc.toString());

        sb.append(sbFc.toString());

        sb.append(sbSc.toString());

        return sb.toString();
    }

}