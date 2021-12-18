/*
    Comp3290 Compiler Design
    Parser.java class - Recursively creates our AST from the given set of tokens, All language rules have been
                        Left factored to remove ambiguity - The error recovery seems to work, could be better
                        as it tends to skip a fair amount of source code and sometimes tends to just finish parsing.
                        Scope is just handled as mentioned in comments on NPROGnode method. I've tried to make it so
                        semantic analysis is easy enough to implement with just inserting some code into the language rules.

                        Semantic analysis - Currently <id> cant be redeclared if already used
                                            <id> must exist before being instantiated
                                            <id> = <expr> - The expr must match the type of <id>, if float and <id> is integer
                                                            <id> is promoted to float/real
                                            Functions must have atleast 1 return statement
                                            Function call parameters must match the amount of params in function
                                            also the params must be the same type

                                            TODO - Check array sizes, currently array size must be constant and known
                                                   during compile time

    Created by - Keeylan Hume C3320396

 */

package Parser;
import Models.*;
import Scanner.ErrorObject;

import java.util.ArrayList;

public class Parser {

    private final int START_TOKEN = 0, LOOK_AHEAD = 1;
    private int tokenCount, scopeNumber, returnCount, baseReg;
    private final SymbolTable st;
    private ArrayList<Token> tokenList;
    private Token token, lookAhead, prevToken;
    private ErrorObject error;
    private ArrayList<ErrorObject> syntaxErrors = new ArrayList<>();
    private ArrayList<ErrorObject> semanticErrors = new ArrayList<>();
    private ArrayList<Integer> syncSet = new ArrayList<>();
    private boolean isCorrect = true;
    private final int CONSTANT_BASE = 0, MAIN_BASE = 1, FUNCTION_BASE = 2;
    private String programName;

    // Dont need offset for constant - Constants sit in their own section
    private int constantOffset = 0, mainOffset = 0, functionOffset = 0;

    public Parser(ArrayList<Token> tokenList) {

        this.tokenList = tokenList;
        this.st = new SymbolTable();
        consumeToken();

    }

    // Remove tokens until we hit a token that synconizes our stream on follow set
    private void errorRecovery() {

        // Sets the program as incorrect
        isCorrect = false;

        // If the list is empty the file is done
        if(tokenList.isEmpty()) {
            return;
        }

        while(!syncSet.contains(token.getTokenNo())) {
            consumeToken();
        }
    }

    public TreeNode createMyTree() {
        return NPROGNode();
    }

    // Check if an identifier exists in symboltable
    private int checkIdentifier(SymbolTableData data) {

        // -1 case - Doesnt exist in the symbol table - FALSE
        //  1 case - exists but already exists in same scope - TRUE
        //  0 case - exists but different scope - TRUE
        return st.lookup(data);
    }

    /*
        <program> ::= CD21 <id> <globals> <funcs> <mainbody>

        Creates the root program NPROG node


                     NPROG                                  NPROG
             __________|_________                   __________|_________
             |         |        |                   |                  |
           NGLOB     NFUNC    NMAIN               NGLOB              NMAIN


           Scope numbers
                    GLOBAL: 0
                    Functions: 1...n
                    Main: Starts 1 above any function scope - So if we have 2 function declerations main will start at 3
                    Loops: increment from main and keep incrementing for increasing nested

           Sets base register for functions as 2, re-sets base register back to 1 for main
    */
    private TreeNode NPROGNode() {

        TreeNode nProg = new TreeNode(ENodeTypes.NPROG);

        if(token.getTokenNo() != Token.TCD21) {

            // Create error, TCD21 token expected
            error = new ErrorObject("Expecting program start CD21, line: ", 1);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);

        }

        consumeToken();

        if(token.getTokenNo() != Token.TIDEN) {

            error = new ErrorObject("Expecting identifier after program initialize, Line: ", 1);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());
        st.insert(data);

        programName = data.getName();

        consumeToken();

        TreeNode nGlobals = globNode();

        baseReg = 2;
        TreeNode nFunc = func();

        increaseScope();

        baseReg = 1;
        TreeNode nMain = mainNode();

        // Syntax error - missing the identifier post TCD21
        if(token.getTokenNo() != Token.TIDEN) {
            isCorrect = false;
            ErrorObject error = new ErrorObject("Missing identifier after TCD21, Line: ", prevToken.getLineNo());

            syntaxErrors.add(error);
            nProg.addError(error);
        }

        // Check if the token matches the original identifier
        // if not semantic fail
        if(!programName.equalsIgnoreCase(token.getLexeme())) {

            isCorrect = false;
            ErrorObject error = new ErrorObject("Invalid indentifier after TCD21, Line: ", token.getLineNo());
            error.setErrorType("Semantic Error: ");

            nProg.addError(error);

        }

        nProg.setLeft(nGlobals);
        nProg.setMiddle(nFunc);
        nProg.setRight(nMain);
        nProg.setData(data);

        return nProg;
    }

    /*
        <globals>:= <contants> <types> <arrays>

        Returns - NOTE theses children can be null so NGLOB is empty
                      NGLOB
             ___________|__________
             |          |          |
        <contants>   <types>    <arrays>

        Sets constants as baseRegister 0 & types/arrays as base register 1 for main
    */
    private TreeNode globNode() {

        TreeNode nConstant= null, nTypes = null, nArrays = null;

        if(token.getTokenNo() == Token.TCONS) {
            consumeToken();
            baseReg = 0;
            nConstant = ilistNode();
        }

        if(token.getTokenNo() == Token.TTYPS){
            baseReg = 1;
            nTypes = types();
        }

        if(token.getTokenNo() == Token.TARRS) {
            nArrays = arrays();
        }

        return new TreeNode(ENodeTypes.NGLOB, nConstant, nTypes, nArrays, null);
    }

    /*
        <types>:= types <typelist> | E
    */
    private TreeNode types() {

        if(token.getTokenNo() != Token.TTYPS) {
            return null;
        }

        // Consume types
        consumeToken();

        return typelist();
    }

    /*
        <typelist>:= <type> <typeRecur>
        <typeRecur>:= E | <typelist>

        Returns
                      NTYPEL                             NTYPEL
             ___________|__________             ___________|__________
             |                    |             |
           <type>            <typeRecur>     <type>
    */
    private TreeNode typelist() {

        TreeNode nType = type();

        TreeNode nTypeListRecur = typelistRecur();

        if(nTypeListRecur == null) {
            return new TreeNode(ENodeTypes.NTYPEL, nType, (SymbolTableData) null);
        }

        return new TreeNode(ENodeTypes.NTYPEL, nType, nTypeListRecur);
    }

    private TreeNode typelistRecur() {

        if(token.getTokenNo() == Token.TARRS || token.getTokenNo() == Token.TFUNC || token.getTokenNo() == Token.TMAIN) {
            // Epsilon path
            return null;
        }

        return typelist();
    }

    /*
        <type>:= <structid> is <fields> end | <typeid> is array [ <expr> ] of <structid> end

        If we have a typeid rule - link the structId symboltable into the NATYPE entry

        Returns
                      NRTYPE(structID)                  NATYPE(typeid, structid)
             ___________|__________             ___________|__________
             |                                  |
          <fields>                            <expr>



        ErrorRecovery - Token.TEND, Token.TMAIN

    */
    private TreeNode type() {

        TreeNode nType = new TreeNode();

        if(token.getTokenNo() != Token.TIDEN) {

            ErrorObject error = new ErrorObject("Missing Identifier type, Line ", prevToken.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TMAIN, Token.TTEND, Token.TARRS);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);

        }

        // Symbol table entry for either struct or typeid
        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        // Consume TIDEN
        consumeToken();

        // Check if the variable has been declared already in scope
        if(checkIdentifier(data) == 1) {

            isCorrect = false;

            ErrorObject error = new ErrorObject("Variable redecleration, Line: ", token.getLineNo());

            semanticErrors.add(error);
            nType.addError(error);

        }

        if(token.getTokenNo() != Token.TTTIS) {

            ErrorObject error = new ErrorObject("Missing keyword is in type decleration, Line ", prevToken.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TMAIN, Token.TTEND, Token.TARRS);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume TTTISS
        consumeToken();

        if(token.getTokenNo() != Token.TARAY) {

            // First language rule
            TreeNode nFields = fields();

            if(token.getTokenNo() != Token.TTEND) {

                ErrorObject error = new ErrorObject("Missing keyword end after type decleration, Line ", prevToken.getLineNo());
                createSyncSetAndRecover(Token.TSEMI, Token.TMAIN, Token.TTEND, Token.TARRS, Token.TIDEN);
                syntaxErrors.add(error);
                return new TreeNode(ENodeTypes.NUNDF, error);
            }

            // Consume TTEND
            consumeToken();

            data.setType("Struct");
            st.insert(data);

            nType.setNodeType(ENodeTypes.NRTYPE);
            nType.setData(data);

            return nType;
            //return new TreeNode(ENodeTypes.NRTYPE, nFields, data);
        }

        // TypeId instead of StructId
        data.setType("Type");

        // Second language rule - token should be array
        // Consume TARAY - this may be an issue if they forgot array?
        consumeToken();

        if(token.getTokenNo() != Token.TLBRK) {

            ErrorObject error = new ErrorObject("Expecting [ before array size decleration, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TMAIN, Token.TTEND, Token.TARRS);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume [
        consumeToken();

        // TODO - Need to make sure this is allowed - Array size [x]
        // May need to make a floating variable for nExpr to use
        TreeNode nExpr = expr();

        if(token.getTokenNo() != Token.TRBRK) {

            ErrorObject error = new ErrorObject("Expecting ] after array size decleration, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TMAIN, Token.TTEND, Token.TARRS);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume ]
        consumeToken();

        if(token.getTokenNo() != Token.TTTOF) {

            ErrorObject error = new ErrorObject("Missing of keyword in type decleration, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TMAIN, Token.TTEND, Token.TARRS);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume of
        consumeToken();

        if(token.getTokenNo() != Token.TIDEN) {

            ErrorObject error = new ErrorObject("Expecting struct type in type decleration, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TMAIN, Token.TTEND, Token.TARRS);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);

        }

        SymbolTableData structIdData = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        // Check if this exists as a type - Should be in the same scope anyway
        // But may need to make sure its in the types scope
        if(checkIdentifier(data) == -1) {

            isCorrect = false;

            ErrorObject error = new ErrorObject("Array decleration has not been declared, Line: ", token.getLineNo());

            semanticErrors.add(error);
            nType.addError(error);

        }

        // Add the struct type into the symbol table data
        data.setStructId(structIdData.hashCode());
        st.insert(data);

        nType.setData(data);

        // Consume TIDEN
        consumeToken();

        if(token.getTokenNo() != Token.TTEND) {

            ErrorObject error = new ErrorObject("Missing end keyword after type decleration, Line ", prevToken.getLineNo());
            createSyncSetAndRecover(Token.TIDEN, Token.TMAIN, Token.TTEND, Token.TARRS);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume TTEND
        consumeToken();

        nType.setNodeType(ENodeTypes.NRTYPE);
        nType.setLeft(nExpr);

        return nType;
        //return new TreeNode(ENodeTypes.NRTYPE, nExpr, data);
    }

    /*
        <fields>:= <sdecl> <fieldsRecur>
        <fieldsRecur>:= E | , <fields>

        Returns
                      NFLIST                             NFLIST
             ___________|__________             ___________|__________
             |          |          |            |          |          |
          <sdecl>            <fieldsRecur>   <sdecl>

     */
    private TreeNode fields() {

        TreeNode nSdecl = sDecl();

        TreeNode nFieldsRecur = fieldsRecur();

        return (nFieldsRecur == null) ? new TreeNode(ENodeTypes.NFLIST, nSdecl, (SymbolTableData) null)
                                      : new TreeNode(ENodeTypes.NFLIST, nSdecl, nFieldsRecur);
    }

    private TreeNode fieldsRecur() {

        if(token.getTokenNo() != Token.TCOMA) {
            return null;
        }

        // Consume ,
        consumeToken();

        return fields();
    }

    /*
        <arrays>:= arrays <arrdecls> | E
    */
    private TreeNode arrays() {

        if(token.getTokenNo() != Token.TARRS) {
            return null;
        }

        // Consume arrays
        consumeToken();

        return arrayDecls();
    }

    /*
        <arraydecls>:= <arrdecl>, <arrdecls> | <arrdecl>

        <arraydecls>:= <arrdecl> <arrdeclRecur>
        <arraydeclRecur>:= E | <arraydecls>

        Returns
                      NALIST                               NALIST
             ___________|__________               ___________|__________
             |                    |              |
        <arrdecl>           <arrdeclRecur>    <arrdecl>
    */
    private TreeNode arrayDecls() {

        TreeNode nArrdecl = arrDecl();

        TreeNode nArraydeclRecur = arrayDeclRecur();

        if(nArraydeclRecur == null) {
            return new TreeNode(ENodeTypes.NALIST, nArrdecl, (SymbolTableData) null);
        }

        return new TreeNode(ENodeTypes.NALIST, nArrdecl, nArraydeclRecur);
    }

    private TreeNode arrayDeclRecur() {

        TreeNode nArrdecl = arrDecl();

        if(nArrdecl == null) {
            return null;
        }

        return arrayDecls();
    }

    /*
        <arrdecl>:= <id> : <typeid>

        Returns
                      NARRD(id, typeid)
             ___________|__________

        ErrorRecovery - Token.TSEMI Token.TMAIN
    */
    private TreeNode arrDecl() {

        TreeNode nArrDecl = new TreeNode(ENodeTypes.NARRD);

        if(token.getTokenNo() != Token.TIDEN) {
            // Epsilon path
            return null;
        }

        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        // Consume TIDEN
        consumeToken();

        // Check if the variable has been declared already in scope
        if(checkIdentifier(data) == 1) {

            isCorrect = false;

            ErrorObject error = new ErrorObject("Variable redecleration, Line: ", token.getLineNo());

            semanticErrors.add(error);
            nArrDecl.addError(error);

        }

        if(token.getTokenNo() != Token.TCOLN) {

            ErrorObject error = new ErrorObject("Missing : after Identifier, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TMAIN);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume TCOLN
        consumeToken();

        if(token.getTokenNo() != Token.TIDEN) {

            ErrorObject error = new ErrorObject("Missing array type identifier, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TMAIN);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);

        }

        SymbolTableData typeData = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        // Get typeid symbol table and add to NARRD type
        data.setTypeId(typeData.hashCode());
        st.insert(data);

        // Consume TIDEN
        consumeToken();

        nArrDecl.setData(data);

        return nArrDecl;

       // return new TreeNode(ENodeTypes.NARRD, data);
    }

    /*
        <initlist>:= <init> <initTail>
        <initTail>:= , <initTail> | e

        Return NILIST
    */
    private TreeNode ilistNode() {

        TreeNode NINIT = initNode(); // This consumes up to the next possible token

        if(token.getTokenNo() == Token.TTYPS ||
           token.getTokenNo() == Token.TARRS ||
           token.getTokenNo() == Token.TFUNC ||
           token.getTokenNo() == Token.TMAIN) {
            // Epsilon path
            return NINIT;
        }

        if(token.getTokenNo() == Token.TCOMA) {
            consumeToken();

            TreeNode NILIST = ilistNode();

            return new TreeNode(ENodeTypes.NILIST, NINIT, NILIST);
        }

        // Was no coma - so return the NINIT node
        return NINIT;
    }

    /*
        <init>:= <id> is <expr>

        Return NINIT

        ErrorRecovery - Token.TSEMI

        each constant increases the offset by 8 at base reg of 0

    */
    private TreeNode initNode() {

        TreeNode nInit = new TreeNode(ENodeTypes.NINIT);

        if(token.getTokenNo() != Token.TIDEN) {

            ErrorObject error = new ErrorObject("Missing Identifier, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);

        }

        // Create symbol table entry for ident - Constants are of integer type
        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        data.setOffset(baseReg, constantOffset);
        constantOffset += 8;

        consumeToken();

        if(token.getTokenNo() != Token.TTTIS) {

            ErrorObject error = new ErrorObject("Missing = after Identifier, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        TreeNode nExpr = expr();

        // Check if the ident already exists
        if(checkIdentifier(data) == 1 || checkIdentifier(data) == 0) {

            isCorrect = false;

            ErrorObject error = new ErrorObject("Variable already declared " + data.getName() + ", Line: ", token.getLineNo());

            semanticErrors.add(error);
            nInit.addError(error);

        }

        ArrayList<String> types = new ArrayList<>();

        getExprTypes(types, nExpr);

        for (String type : types) {

            // Single type
            if(types.size() == 1) {
                data.setType(type);
                data.setValue(nExpr.getSymbolTableRecord().getValue());
                st.insert(data);
                break;
            }

            // If multiple + / - ext and it includes a float. Promote <id> to float
            if(types.contains("float")) {
                data.setType("float");
                break;
            }
        }


        // Need to figure out what the type of <id> is

        return new TreeNode(ENodeTypes.NINIT, nExpr, data);
    }

    /*
        <funcs>:= <func> <funcs> | E

        Returns
                      NFUNCS
             ___________|__________          Or    Epsilon
             |                    |
           <func>              <funcs>

    */
    private TreeNode funcs() {

        TreeNode nFunc = func();

        if(nFunc == null) {
            return null;
        }

        TreeNode nFuncRecur = funcs();

        return new TreeNode(ENodeTypes.NFUNCS, nFunc, nFuncRecur);
    }

    /*
        <func>:= func <id> ( <plist> ) : <rtype> <funcbody>
        <rtype>:= <stype> | Void

        Returns
                     NFUND(id, rtype)
             ___________|___________
             |          |           |
          <plist>    <locals>    <stats>

        ErrorRecovery - Token.TTEND, Token.TMAIN, Token.TFUNC

        Recovers To the end of the function, next function, or main

    */
    private TreeNode func() {

        if(token.getTokenNo() != Token.TFUNC) {
            return null;
        }

        // Increase scope for each function - Fucntions are global, so dont need scope will be 0
       // increaseScope();

        // Consume TFUNC
        consumeToken();

        if(token.getTokenNo() != Token.TIDEN) {

            ErrorObject error = new ErrorObject("Missing identifier in function call, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TMAIN, Token.TFUNC);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());
        data.setIsFunction();

        data.setOffset(baseReg, functionOffset);
        functionOffset += 8;

        // Consume TIDEN
        consumeToken();

        // TODO - Need to check if the TIDEN function name already exists - semantic error

        if(token.getTokenNo() != Token.TLPAR) {

            ErrorObject error = new ErrorObject("Missing openening ( in function call, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TMAIN, Token.TFUNC);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume (
        consumeToken();

        // These are the function params - save these - Pass through symboltable entry and add to func params
        TreeNode nPlist = pList(data);

        if (token.getTokenNo() != Token.TRPAR || lookAhead.getTokenNo() != Token.TCOLN) {

            ErrorObject error = new ErrorObject("Unexpected token in function call, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TMAIN, Token.TFUNC);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume ) :
        consumeToken();
        consumeToken();

        // Get funcs return type - No need to call a node, attach to the symbol table entry
        switch(token.getTokenNo()) {

            case Token.TVOID:

                data.setReturnType("Void");
                st.insert(data);
                break;

            case Token.TINTG:

                data.setReturnType("Integer");
                st.insert(data);
                break;
            case Token.TREAL:

                data.setReturnType("Real");
                st.insert(data);
                break;
            case Token.TBOOL:

                data.setReturnType("Boolean");
                st.insert(data);
                break;

            default:

                ErrorObject error = new ErrorObject("Expecting function return type, Line ", token.getLineNo());
                createSyncSetAndRecover(Token.TTEND, Token.TMAIN, Token.TFUNC);
                syntaxErrors.add(error);
                return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume function type
        consumeToken();

        TreeNode nFuncBody = funcBody(data);

        // parent middle is nfunc left, parent right nfunc right
        nFuncBody.setLeft(nPlist);
        nFuncBody.setData(data);

        return nFuncBody;
    }

    /*
        <plist>:= <params> | E
    */
    private TreeNode pList(SymbolTableData data) {

        return params(data);
    }

    /*
        <params>:= <param> <paramRecur>
        <paramRecur>:= E | , <params>

        <params>:= <param>
        <paramtail>:=  , <params> | E

        Returns
                      NPLIST
             ___________|___________
             |          |           |
          <param>              <paramRecur>

        Adding the function params to the functions symbol table entries list

    */
    private TreeNode params(SymbolTableData data) {

        TreeNode nParam = param(data);

        TreeNode nParamRecur = paramRecur(data);

        return (nParamRecur == null) ? nParam : new TreeNode(ENodeTypes.NPLIST, nParam, nParamRecur);
    }

    private TreeNode paramRecur(SymbolTableData data) {

        if(token.getTokenNo() != Token.TCOMA) {
            // Epsilon path
            return null;
        }

        // Consume ,
        consumeToken();

        return params(data);
    }

    /*
        <param>:= <sdecl> | <arrdecl> | const <arrdecl>

        Returns
                      NSIMP                               NARRP                               NARRC
             ___________|___________             ___________|___________             ___________|___________
             |                                   |                                   |
          <sdecl>                             <arrdecl>                          <arrdecl>

    */
    private TreeNode param(SymbolTableData data) {

        if(token.getTokenNo() == Token.TCONS) {
            TreeNode nArrdecl = arrDecl();

            if(nArrdecl == null) {
                // Error recovery
                return null;
            }

            data.addFunctionParam(nArrdecl.getSymbolTableRecord());

            return new TreeNode(ENodeTypes.NARRC, nArrdecl, (SymbolTableData) null);

        }

        Token lookahead = peek(1);
        TreeNode nSdecl = null, nArrdecl = null;

        if(lookahead == null) {
            error = new ErrorObject("Unexpected token, Line ", token.getLineNo());
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, (SymbolTableData) null);
        }

        // Now check if we have an SDECL or ARRDECL
        if(lookahead.getTokenNo() == Token.TINTG || lookahead.getTokenNo() == Token.TREAL || lookahead.getTokenNo() == Token.TBOOL) {
            nSdecl = sDecl();
            data.addFunctionParam(nSdecl.getSymbolTableRecord());
        }
        else {
            nArrdecl = arrDecl();

            if(nArrdecl == null) {
                // Error recovery
                return null;
            }
            data.addFunctionParam(nArrdecl.getSymbolTableRecord());
        }

        return (nSdecl == null) ? new TreeNode(ENodeTypes.NARRP, nArrdecl, (SymbolTableData) null) : new TreeNode(ENodeTypes.NSIMP, nSdecl, (SymbolTableData) null);
    }

    /*
        <funcbody>:= <locals> begin <stats> end
    */
    private TreeNode funcBody(SymbolTableData data) {

        TreeNode nFund = new TreeNode(ENodeTypes.NFUND);

        // Make a return variable, function needs atleast 1 return
        // stupid way of doing it, but easiest and no re-going over AST

        TreeNode nLocals = dlist();

        if(token.getTokenNo() != Token.TBEGN) {
            // Error recovery
            ErrorObject error = new ErrorObject("Expecting begin in function body, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TMAIN, Token.TFUNC);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume begin
        consumeToken();

        TreeNode nStats = stats();

        if(token.getTokenNo() != Token.TTEND) {

            ErrorObject error = new ErrorObject("Expecting end in function body, Line ", prevToken.getLineNo());
            createSyncSetAndRecover(Token.TMAIN, Token.TFUNC);
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume end
        consumeToken();

        if(returnCount == 0) {

            isCorrect = false;

            // TODO - Can add the full function in error if needed
            ErrorObject error = new ErrorObject("No return statement for function " + data.getName() + ", Line: ", token.getLineNo());

            semanticErrors.add(error);
            nFund.addError(error);

        }
        nFund.setLeft(nLocals);
        nFund.setRight(nStats);

        // Reset return count
        returnCount = 0;

        return nFund;
    }

    /*
        <dlist>:= <decl> <dlistRecur>
        <dlistRecur>:= E | , <dlist>

        decl = sdecl | arrdecl
     */
    private TreeNode dlist() {

        if(token.getTokenNo() != Token.TIDEN) {
            // Epsilon locals path
            return null;
        }

        TreeNode decl;

        // Look 2 ahead to check if we have an Sdecl or Adecl
        Token lookahead = peek(1);

        // Check if for some reason we have an index out of bounds
        if(lookahead == null) {
            error = new ErrorObject("Unexpected token, Line ", token.getLineNo());
            syntaxErrors.add(error);
            return new TreeNode(ENodeTypes.NUNDF, (SymbolTableData) null);
        }

        // Now check if we have an SDECL or ARRDECL
        if(lookahead.getTokenNo() == Token.TINTG || lookahead.getTokenNo() == Token.TREAL || lookahead.getTokenNo() == Token.TBOOL) {
            decl = sDecl();
        }
        else {
            decl = arrDecl();
        }

        TreeNode nDlistRecur = dlistRecur();

        return (nDlistRecur == null) ? new TreeNode(ENodeTypes.NDLIST, decl, (SymbolTableData) null) : new TreeNode(ENodeTypes.NDLIST, decl, nDlistRecur);
    }

    private TreeNode dlistRecur() {

        if(token.getTokenNo() != Token.TCOMA) {
            // Epsilon path
            return null;
        }

        // Consume ,
        consumeToken();

        return dlist();
    }

    /*
        <mainbody>:= main <slist> begin <stats> end CD21 <id>

        ErrorRecovery - TIDEN, TSEMI, TBEGIN
    */
     private TreeNode mainNode() {

        TreeNode nMain = new TreeNode(ENodeTypes.NMAIN);

         if(token.getTokenNo() != Token.TMAIN) {

             error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line", token.getLineNo());
             createSyncSetAndRecover(Token.TIDEN, Token.TSEMI, Token.TBEGN, Token.TCD21);
             syntaxErrors.add(error);

             nMain.addError(error);

         }

         // Consume main
         consumeToken();

        // Get <slist>
        TreeNode nList = sList();

        // Check if begin is there
        if(token.getTokenNo() != Token.TBEGN) {

            ErrorObject error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TBEGN, Token.TSEMI, Token.TTEND, Token.TCD21);
            syntaxErrors.add(error);

            nMain.addError(error);
            isCorrect = false;
        }

        // Consume begin
        consumeToken();

        TreeNode nStats = stats();

        if(token.getTokenNo() != Token.TTEND) {

            ErrorObject error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line", token.getLineNo());
            syntaxErrors.add(error);

            nMain.addError(error);
        }

        consumeToken();

        if(token.getTokenNo() != Token.TCD21) {

            ErrorObject error = new ErrorObject("Expecting CD21, Line ", token.getLineNo());
            syntaxErrors.add(error);

            nMain.addError(error);
        }

        // Consume TCD21
        consumeToken();

        nMain.setLeft(nList);
        nMain.setRight(nStats);

        return nMain;
    }

    /*
        <stats>:=  <strstat> <statRecur> | <stat> ; <statRecur>
        <statRecur>:= E | <stats>

        Follow set ()

        Returns NSTATS | NUNDF

    */
    private TreeNode stats() {

        TreeNode nStrstat;

        // Need to check if strStat is viable
        if(token.getTokenNo() == Token.TTFOR || token.getTokenNo() == Token.TIFTH) {
            nStrstat = strstat();

            TreeNode nStatRecur = statRecur();

            if(nStatRecur == null) {
                // Epsilon path
                return nStrstat;
            }

            return new TreeNode(ENodeTypes.NSTATS, nStrstat, nStatRecur);

        }

        TreeNode nStat = stat();

        if(token.getTokenNo() != Token.TSEMI) {

            ErrorObject error = new ErrorObject("Expecting ;, Line ", prevToken.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TCD21);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume ;
        consumeToken();

        TreeNode statRecur = statRecur();

        if(statRecur == null) {
            // Epsilon path
            return nStat;
        }


        return new TreeNode(ENodeTypes.NSTATS, nStat, statRecur);
    }

    private TreeNode statRecur() {

        // Check if stats or strStat is viable
        if(!checkStats() && !checkStrStats()) {
            return null;
        }

        return stats();
    }

    /*
        <strstat>:= <forstat> | <ifstat>

        Return NFORL | NIFTH | NIFTE

    */
    private TreeNode strstat() {

        TreeNode nForStat = forstat();

        if(nForStat == null) {
            return ifstat();
        }

        return forstat();
    }

    /*
        <stat>:= <reptstat> | <asgnstat> | <iostat> | <callstat> | <returnstat>

        Uses the current token number to check if it needs to call a node function
        asgnstat and callstat both start with TIDEN so need to call these functions
    */
    private TreeNode stat() {

        // Check if repstat is viable
        if(token.getTokenNo() == Token.TREPT) {
            return repstat();
        }

        // Check if asgnstat is viable
        TreeNode nAsgnstat = asgnstat();

        if(nAsgnstat != null) {
            return nAsgnstat;
        }

        // Check if iostat is viable
        if(token.getTokenNo() == Token.TOUTP || token.getTokenNo() == Token.TINPT) {
            return iostat();
        }

        TreeNode nCallstat = callstat();

        if(nCallstat != null) {
            return nCallstat;
        }

        return returnstat();
    }

    /*
        <iostat>:= In >> <vlist> | Out << <prlist> | Out << Line | Out << <prlist> << Line
    */
    private TreeNode iostat() {

        switch(token.getTokenNo()) {

            case Token.TINPT:

                consumeToken();

                if(token.getTokenNo() != Token.TGRGR) {

                    ErrorObject error = new ErrorObject("Expecting >>, Line ", token.getLineNo());
                    createSyncSetAndRecover(Token.TSEMI, Token.TTEND, Token.TCD21);
                    syntaxErrors.add(error);

                    return new TreeNode(ENodeTypes.NUNDF, error);
                }

                consumeToken();

                TreeNode nVlist = vlist();

                return new TreeNode(ENodeTypes.NINPUT, nVlist, (SymbolTableData) null);

            case Token.TOUTP:

                consumeToken();

                if(token.getTokenNo() != Token.TLSLS) {

                    ErrorObject error = new ErrorObject("Expecting <<, Line ", token.getLineNo());
                    createSyncSetAndRecover(Token.TSEMI, Token.TTEND, Token.TCD21);
                    syntaxErrors.add(error);

                    return new TreeNode(ENodeTypes.NUNDF, error);
                }

                consumeToken();

                if(token.getTokenNo() != Token.TOUTL) {

                    TreeNode nPrlist = prlist();

                    if(token.getTokenNo() != Token.TLSLS) {

                        ErrorObject error = new ErrorObject("Expecting <<, Line ", token.getLineNo());
                        createSyncSetAndRecover(Token.TSEMI, Token.TTEND, Token.TCD21);
                        syntaxErrors.add(error);

                        return new TreeNode(ENodeTypes.NUNDF, error);
                    }

                    consumeToken();

                    if(token.getTokenNo() != Token.TOUTL) {
                        consumeToken();
                        return new TreeNode(ENodeTypes.NOUTP, nPrlist, (SymbolTableData) null);
                    }
                    consumeToken();
                    return new TreeNode(ENodeTypes.NOUTL, nPrlist, (SymbolTableData) null);
                }

                break;

            default:

                ErrorObject error = new ErrorObject("Unexpected token: " + prevToken.getLexeme() + "Line ", prevToken.getLineNo());
                createSyncSetAndRecover(Token.TSEMI, Token.TTEND, Token.TCD21);
                syntaxErrors.add(error);

                return new TreeNode(ENodeTypes.NUNDF, error);

        }

        // Consume TOUTL token
        consumeToken();

        return new TreeNode(ENodeTypes.NOUTL, (SymbolTableData) null);
    }

    /*

        <vlist>:= <var> <vlistRecur>
        <vlistRecur>:= E | , <vlist>

        Returns:
                      NVLIST                             <var>
             ___________|__________             ___________|__________
             |                    |
           <var>               <vlist>

    */
    private TreeNode vlist() {

        TreeNode nVar = var();

        TreeNode nVlistRecur = vlistRecur();

        if(nVlistRecur == null) {
            // epsilon path
            return nVar;
        }

        return new TreeNode(ENodeTypes.NVLIST, nVar, nVlistRecur);
    }

    private TreeNode vlistRecur(){

        if(token.getTokenNo() != Token.TCOMA) {
            return null;
        }

       // Consume ,
        consumeToken();

        return vlist();
    }

    /*
        <prlist>:= <printitem> <prlistRecur>
        <prlistRecur>:= E | , <prlist>

        Returns
                      NPRLST                          <printitem>>
             ___________|__________             ___________|__________
             |                    |
         <printitem>          <prlist>
    */
    private TreeNode prlist() {

        TreeNode nPrintItem = printitem();

        TreeNode nPrintItemRecur= printitemRecur();

        if(nPrintItemRecur == null) {
            return nPrintItem;
        }

        return new TreeNode(ENodeTypes.NPRLST, nPrintItem, nPrintItemRecur);
    }

    private TreeNode printitemRecur() {

        if(token.getTokenNo() != Token.TCOMA) {
            // epsilon path
            return null;
        }

        // Consume ,
        consumeToken();

        return prlist();
    }

    /*
        <printitem>:= <expr> | <string>
    */
    private TreeNode printitem() {

        if(token.getTokenNo() != Token.TSTRG) {
            return expr();
        }

        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());
        st.insert(data);

        // Consume TSTRG
        consumeToken();

        return new TreeNode(ENodeTypes.NSTRG, data);
    }

    /*
        Left factored grammar is just done in the one method callstat

        <callstat>:= <id> ( <callStatTail>
        <callstatTail>:= ) | <elist> )

        Returns
                     NCALL(id)                           NCALL(id)
             ___________|__________             ___________|__________
             |                    |             |
                                             <elist>
    */
    private TreeNode callstat() {

        TreeNode nCall = new TreeNode(ENodeTypes.NCALL);

        if(token.getTokenNo() != Token.TIDEN) {
           return null;
        }

        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        // Consume TIDEN
        consumeToken();

        if(token.getTokenNo() != Token.TLPAR) {

            ErrorObject error = new ErrorObject("Expecting open (, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TCD21);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume (
        consumeToken();

        // Check if we just have <id> ()
        if(token.getTokenNo() == Token.TRPAR) {

            // Consume )
            consumeToken();

            new TreeNode(ENodeTypes.NCALL, data);
        }

        TreeNode nElist = eList();

        // Check if the id matches a function - Functions are global so any scope is fine
        if(checkIdentifier(data) == -1) {

            isCorrect = false;

            ErrorObject error = new ErrorObject("Array decleration has not been declared, Line: ", token.getLineNo());

            semanticErrors.add(error);
            nCall.addError(error);

        }
        else {

            // nElist has the function params - need to make sure they match the params in symtable
            // nElist can also be null for no params - need to recursivly go through nElist to get params
            ArrayList<String> funcParams = new ArrayList<>();
            getFunctionParams(funcParams, nElist);

            // Check the size if the params match
            if(funcParams.size() != data.getFuncParams().size()) {

                isCorrect = false;

                ErrorObject error = new ErrorObject("Function " + token.getLexeme() + " invalid number of parameters" + ", Line: ", token.getLineNo());

                semanticErrors.add(error);
                nCall.addError(error);

            }
            else {

                // Check each param and make sure they are the same type
                for(int i = 0; i < data.getFuncParams().size(); i++) {

                    if(!data.getFuncParams().get(i).getType().equalsIgnoreCase(funcParams.get(i))) {

                        // Incorrect function parameters
                        isCorrect = false;

                        ErrorObject error = new ErrorObject("Function " + token.getLexeme() + " invalid parameter type " + funcParams.get(i) + ", Line: ", token.getLineNo());

                        semanticErrors.add(error);
                        nCall.addError(error);

                        break;
                    }
                }
            }
        }

        if(token.getTokenNo() != Token.TRPAR) {

            ErrorObject error = new ErrorObject("Expecting ), Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TCD21);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume )
        consumeToken();

        nCall.setLeft(nElist);
        nCall.setData(data);

        return nCall;
        //return new TreeNode(ENodeTypes.NCALL, nElist, data);
    }

    /*
        <elist>:= <bool> <elistRecur>
        <elistRecur>:= E | , <elist>

        Returns
                      NEXPL                              <bool>
             ___________|__________             ___________|__________
             |                    |             |
          <bool>               <elist>
    */
    private TreeNode eList() {

        TreeNode nBool = bool();

        TreeNode nElistRecur = elistRecur();

        return (nElistRecur == null) ? nBool : new TreeNode(ENodeTypes.NEXPL, nBool, nElistRecur);
    }

    private TreeNode elistRecur() {

        if(token.getTokenNo() != Token.TCOMA) {
            // Epsilon path
            return null;
        }

        // Consume ,
        consumeToken();

        return eList();
    }

    /*
        <returnstat>:= return void | return <expr>

        If RETN has no nodes it returns void - Saves having to attach another symbol table entry

        Returns
                      NRETN(void)                         NRETN
             ___________|__________             ___________|__________
             |                    |             |
                                              <expr>
    */
    private TreeNode returnstat() {

        if(token.getTokenNo() != Token.TRETN) {

            ErrorObject error = new ErrorObject("Expeting return keyword, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TSEMI, Token.TTEND, Token.TCD21, Token.TFUNC);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        if(token.getTokenNo() != Token.TVOID) {
            TreeNode nExpr = expr();

            return new TreeNode(ENodeTypes.NRETN, nExpr, (SymbolTableData) null);
        }

        consumeToken();

        return new TreeNode(ENodeTypes.NRETN, (SymbolTableData) null);
    }

    /*
        <forstat>:= for ( <asgnlist> ; <bool> ) <stats> end

        Returns
                      NFORL
             ___________|__________
             |          |         |
         <asgnlist>  <bool>    <stats>
    */
    private TreeNode forstat() {

        if(token.getTokenNo() != Token.TTFOR) {
            return null;
        }

        consumeToken();

        if(token.getTokenNo() != Token.TLPAR) {

            ErrorObject error = new ErrorObject("Expecting ( in loop, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        TreeNode nAsgnList = asgnlist();

        if(token.getTokenNo() != Token.TSEMI) {

            ErrorObject error = new ErrorObject("Expecting ; after identifier, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        TreeNode nBool = bool();

        if(token.getTokenNo() != Token.TRPAR) {

            ErrorObject error = new ErrorObject("Expecting ( in loop, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        TreeNode nStats = stats();

        if(token.getTokenNo() != Token.TTEND) {

            ErrorObject error = new ErrorObject("Expecting end after loop, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TIDEN, Token.TTEND, Token.TSEMI, Token.TCD21);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume TTEND
        consumeToken();

        return new TreeNode(ENodeTypes.NFORL, nAsgnList, nBool, nStats, null);
    }

    /*
        <ifstat>:= if ( <bool> ) <stats> end | if ( <bool> ) <stats> else <stats> end

        Return

                     NINFTH                              NIFTE
             ___________|__________             ___________|__________
             |          |         |             |          |         |
         <bool>                <stats>       <bool>     <stats>   <stats>
    */
    private TreeNode ifstat() {

        if(token.getTokenNo() != Token.TIFTH) {
            return null;
        }

        consumeToken();

        if(token.getTokenNo() != Token.TLPAR) {

            ErrorObject error = new ErrorObject("Expecting ( in statement, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND);
            syntaxErrors.add(error);

            // If we have an error, we need to decrease the scope
            decreaseScope();

            return new TreeNode(ENodeTypes.NUNDF, error);
        }
        consumeToken();

        TreeNode nBool = bool();

        if(token.getTokenNo() != Token.TRPAR) {

            ErrorObject error = new ErrorObject("Expecting ) in statement, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TMAIN);
            syntaxErrors.add(error);

            // If we have an error, we need to decrease the scope
            decreaseScope();

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        // Increase our scope for each stats
        // in if statements
        increaseScope();

        TreeNode nStats = stats();

        if(token.getTokenNo() != Token.TTEND && token.getTokenNo() == Token.TELSE) {
            consumeToken();

            TreeNode nElseStats = stats();

            if (token.getTokenNo() == Token.TTEND) {
                // Consume TTEND
                consumeToken();

                return new TreeNode(ENodeTypes.NIFTE, nBool, nStats, nElseStats, null);

            }
            else {

                ErrorObject error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line", token.getLineNo());
                createSyncSetAndRecover(Token.TTEND, Token.TSEMI, Token.TIDEN);
                syntaxErrors.add(error);

                // If we have an error, we need to decrease the scope
                decreaseScope();

                return new TreeNode(ENodeTypes.NUNDF, error);
            }
        }

        if(token.getTokenNo() != Token.TTEND) {

            ErrorObject error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI, Token.TIDEN);
            syntaxErrors.add(error);

            // If we have an error, we need to decrease the scope
            decreaseScope();

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // Consume TTEND
        consumeToken();

        // Decrease the scope when we leave the if statement
        decreaseScope();

        return new TreeNode(ENodeTypes.NIFTH, nBool, nStats);
    }

    /*
        <asgnlist>:= <alist> | E
    */
    private TreeNode asgnlist() {

        return alist();
    }

    /*
        <alist>:= <asgnstat> <alistRecur>
        <alistRecur>:= E | , <alist>

        Returns
                      NASGNS                           <asgnstat>
             ___________|__________             ___________|__________
             |          |         |
         <asgnstat>             <alist>
    */
    private TreeNode alist() {

        TreeNode nAsgnStat = asgnstat();

        TreeNode nAlistRecur = aListRecur();

        return (nAlistRecur == null) ? nAsgnStat : new TreeNode(ENodeTypes.NASGNS, nAsgnStat, nAlistRecur) ;
    }

    private TreeNode aListRecur() {

        if(token.getTokenNo() != Token.TCOMA) {
            return null;
        }

        // Consume ,
        consumeToken();

        return alist();
    }

    /*
        <bool>:= <rel> <booltail>
        <booltail>:= E | <logop> <rel> <booltail>

        Returns NBOOL
    */
    private TreeNode bool() {

        TreeNode nRel = rel();

        TreeNode nBoolTail = booltail();

        if(nBoolTail == null) {
            return nRel;
        }

        return new TreeNode(ENodeTypes.NBOOL, nRel, nBoolTail);
    }

    private TreeNode booltail() {

        if(token.getTokenNo() != Token.TTAND || token.getTokenNo() != Token.TTTOR || token.getTokenNo() != Token.TTXOR) {
            // epsilon path
            return null;
        }

        consumeToken();

        TreeNode nRel = rel();

        return booltail();
    }

    /*
        <rel>:= not <relTail> | <relTail>
        <relTail>:= <expr> <relop> <expr> | <expr>

        Returns
                      NNOTT                             <relop>                             <expr>
             ___________|__________             ___________|__________             ___________|__________
             |                                  |                    |
         <relTail>                            <expr>              <expr>

    */
    private TreeNode rel() {

        if(token.getTokenNo() == Token.TNOTT) {

            // Consume TNOTT
            consumeToken();

            TreeNode nRelTail = relTail();

            return new TreeNode(ENodeTypes.NNOT, nRelTail, (SymbolTableData) null);
        }

        return relTail();
    }

    private TreeNode relTail() {

        TreeNode nExprLeft = expr();

        if(token.getTokenNo() != Token.TEQEQ && token.getTokenNo() != Token.TNEQL && token.getTokenNo() != Token.TGRTR &&
           token.getTokenNo() != Token.TLESS && token.getTokenNo() != Token.TLEQL && token.getTokenNo() != Token.TGEQL) {

            return nExprLeft;

        }

        TreeNode nExprRight;

        switch(token.getTokenNo()) {

            case Token.TEQEQ:

                consumeToken();
                nExprRight = expr();
                return new TreeNode(ENodeTypes.NEQL, nExprLeft, nExprRight);

            case Token.TNEQL:

                consumeToken();
                nExprRight = expr();
                return new TreeNode(ENodeTypes.NNEQ, nExprLeft, nExprRight);

            case Token.TGRTR:

                consumeToken();
                nExprRight = expr();
                return new TreeNode(ENodeTypes.NGRT, nExprLeft, nExprRight);

            case Token.TLESS:

                consumeToken();
                nExprRight = expr();
                return new TreeNode(ENodeTypes.NLSS, nExprLeft, nExprRight);

            case Token.TLEQL:

                consumeToken();
                nExprRight = expr();
                return new TreeNode(ENodeTypes.NLEQ, nExprLeft, nExprRight);

            case Token.TGEQL:

                consumeToken();
                nExprRight = expr();
                return new TreeNode(ENodeTypes.NGEQ, nExprLeft, nExprRight);

            default:

                ErrorObject error = new ErrorObject("Unexpected token, Line ", token.getLineNo());
                createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
                syntaxErrors.add(error);

                return new TreeNode(ENodeTypes.NUNDF, error);

        }
    }

    /*
        <repstat>:= repeat ( <asgnlist> ) <stats> until <bool>

        Returns:
                      NREPT                              NREPT
             ___________|__________             ___________|__________
             |          |         |             |          |         |
         <asgnlist>  <stats>   <bool>         <stats>              <bool>
    */
    private TreeNode repstat() {

        if(token.getTokenNo() != Token.TREPT) {

            ErrorObject error = new ErrorObject("Expected repeat keyword, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        // consume repeat
        consumeToken();

        if(token.getTokenNo() != Token.TLPAR) {

            ErrorObject error = new ErrorObject("Expecting ( in repeat statement, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        // NOTE - Can be null
        TreeNode nAsgnlist = asgnlist();

        if(token.getTokenNo() != Token.TRPAR) {

            ErrorObject error = new ErrorObject("Expecting ) in repeat statement, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        TreeNode nStats = stats();

        if(token.getTokenNo() != Token.TUNTL) {

            ErrorObject error = new ErrorObject("Expecting until keyword in statement, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        TreeNode nBool = bool();


        return (nAsgnlist == null) ? new TreeNode(ENodeTypes.NREPT, nStats, nBool) : new TreeNode(ENodeTypes.NREPT, nAsgnlist, nStats, nBool, null);
    }

    /*
        <asgnstat>:= <var> <asgnop> <bool>

        Returns NASGN | NPLEQ | NMNEQ | NSTEA | NDVEQ | null
    */
    private TreeNode asgnstat() {

        // Need to check if asgnstat is correct stat to call
        TreeNode nVar = var();

        if(nVar == null) {
            return null;
        }

        TreeNode nAsgnop = asgnop();

        if(nAsgnop.getValue() == ENodeTypes.NUNDF) {
            return nAsgnop;
        }

        TreeNode nBool = bool();

        // Here go down the bool tree - make sure the types are the same as
        // the nVar type

        SymbolTableData nVarData = nVar.getSymbolTableRecord();

        // Check <expr> tree if each matches the type of <id>
        // Modulo wont work with NFLIT/real
        // if reals within integers, int is promoted to NFLIT/real
        ArrayList<String> types = new ArrayList<>();

        getExprTypes(types, nBool);

        if(!types.contains(null)) {

            for (String type : types) {

                if (!type.equalsIgnoreCase(nVarData.getType())) {
                    // Mismatch in types
                    // example - count(int) = 2 + true
                    // count(int) = 2 + fncall() (returns true or false)

                    if (nVarData.getType().equalsIgnoreCase("integer") && type.equalsIgnoreCase("real")) {
                        // Integer maths with a float, changes nVar to a realit and converts to float value
                        nVarData.setType("real");
                        break;
                    }

                    isCorrect = false;
                    // Unsure what to add for this error
                    ErrorObject error = new ErrorObject("Incompatible variable type, " + nVarData.getName() + ": " + nVarData.getType()
                            + ", " + type + " Line: ", token.getLineNo());

                    semanticErrors.add(error);
                    nVar.addError(error);
                    break;

                }
            }

        }


        nVarData.setIsDeclared();

            if(!nAsgnop.getNodeValue().equalsIgnoreCase(ENodeTypes.NUNDF.nodeString)) {
            nAsgnop.setLeft(nVar);
            nAsgnop.setRight(nBool);
        }

        return nAsgnop;
    }

    /*
        Returns NASGN | NPLEQ | NMNEQ | NSTEA | NDVEQ
    */
    private TreeNode asgnop() {

        switch(token.getTokenNo()) {

            case Token.TEQUL:
                consumeToken();
                return new TreeNode(ENodeTypes.NASGN);

            case Token.TPLEQ:
                consumeToken();
                return new TreeNode(ENodeTypes.NPLEQ);

            case Token.TMNEQ:
                consumeToken();
                return new TreeNode(ENodeTypes.NMNEQ);

            case Token.TSTEQ:
                consumeToken();
                return new TreeNode(ENodeTypes.NSTEA);

            case Token.TDVEQ:
                consumeToken();
                return new TreeNode(ENodeTypes.NDVEQ);

        }

        ErrorObject error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line", token.getLineNo());
        createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
        syntaxErrors.add(error);

        return new TreeNode(ENodeTypes.NUNDF, error);

    }

    /*
        <slist>:= <sdecl> <slistTail>
        <slistTail>:= E | , <sdecl>

        Returns:
                      NSDLST                             NSDLST
             ___________|__________             ___________|__________
             |                    |             |
         <sdecl>            <slistTail>      <sdecl>

    */
    private TreeNode sList() {

        TreeNode nsDecl = sDecl();

        // Check slistTail
        TreeNode nsListTail = slistTail();

        if(nsListTail == null) {
            return new TreeNode(ENodeTypes.NSDLST, nsDecl, (SymbolTableData) null); // Why does this need to be casted
        }

        return new TreeNode(ENodeTypes.NSDLST, nsDecl, nsListTail);
    }

    private TreeNode slistTail() {

        if(token.getTokenNo() != Token.TCOMA) {
            // Epsilon path
            return null;
        }

        // Consume ,
        consumeToken();

        return sList();
    }

    /*
        <sdecl>:= <id> : <stype>

        Returns:
                      NSDECL(id, stype)
             ___________|__________

    */
    private TreeNode sDecl() {

        TreeNode nDecl = new TreeNode(ENodeTypes.NSDECL);

        if(token.getTokenNo() != Token.TIDEN) {

            ErrorObject error = new ErrorObject("Expecting Identifier, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI, Token.TIDEN, Token.TCOMA);
            syntaxErrors.add(error);

            //return new TreeNode(ENodeTypes.NUNDF, error);
            nDecl.addError(error);

        }

        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        // Consume TIDEN
        consumeToken();

        // Check if theres a :
        if(token.getTokenNo() != Token.TCOLN) {

            ErrorObject error = new ErrorObject("Expecting : after identifier, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI, Token.TIDEN, Token.TCOMA);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        consumeToken();

        // Check if the variable has been declared already in scope
        if(checkIdentifier(data) == 1) {

            isCorrect = false;
            
            ErrorObject error = new ErrorObject("Variable redecleration," + token.getLexeme() + " Line: ", token.getLineNo());

            semanticErrors.add(error);
            nDecl.addError(error);

        }

        // Set BMcounter address for code gen

        // Attach the DECL type to symbol table
        switch(token.getTokenNo()) {

            case Token.TINTG:

                data.setType("integer");
                st.insert(data);
                break;

            case Token.TREAL:

                data.setType("real");
                st.insert(data);
                break;

            case Token.TBOOL:

                data.setType("boolean");
                st.insert(data);
                break;

            default:

                ErrorObject error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line", token.getLineNo());
                createSyncSetAndRecover(Token.TTEND, Token.TSEMI, Token.TIDEN, Token.TCOMA);
                syntaxErrors.add(error);

                return new TreeNode(ENodeTypes.NUNDF, error);

        }

        // Consume type
        consumeToken();

        data.setOffset(baseReg, mainOffset);
        mainOffset += 8;

        nDecl.setData(data);

        return nDecl;
        //return new TreeNode(ENodeTypes.NSDECL, data);
    }

    /*
       <expr>:= <term> <exprRec>
       <exprRec>:= - <term> <exprRec> | + <term> <exprRec> | e
    */
    private TreeNode expr() {

        // Get term node
        TreeNode nTerm = term();

        // Check exprRec node
        TreeNode nExprRec = exprRec();

        if(nExprRec == null) {
            // Epsilon path
            return nTerm;
        }

        nExprRec.setLeft(nTerm);

        return nExprRec;
    }

    private TreeNode exprRec() {

        TreeNode n = null;

        switch(token.getTokenNo()) {

            case Token.TMINS:

                // Consume -
                consumeToken();
                n = new TreeNode(ENodeTypes.NSUB);
                break;

            case Token.TPLUS:

                // Consume +
                consumeToken();
                n = new TreeNode(ENodeTypes.NADD);
                break;

            case Token.TREAL:
            case Token.TILIT:
            case Token.TIDEN:
                // Error case missing + or -
                ErrorObject error = new ErrorObject("Unexpected token: "+ token.getLexeme() + " Line", token.getLineNo());
                createSyncSetAndRecover(Token.TTEND, Token.TRPAR, Token.TSEMI);
                syntaxErrors.add(error);

                return new TreeNode(ENodeTypes.NUNDF, error);

            default:
                // Epsilon path
                return null;
        }

        TreeNode nTerm = term();

        n.setRight(nTerm);

        TreeNode nExprRec = exprRec();

        if(nExprRec == null) {

            return n;
        }

        nExprRec.setRight(n);

        return nExprRec;
    }

    /*
        <term>:= <fact> <termRecur>
        <termRecur>:= * <fact> <termRecur> | / <fact> <termRecur> | % <fact> <termRecur> | e

        Returns:
                     NMUL                        NDIV                        NMOD                      <fact>
             __________|_________       __________|_________        __________|_________        __________|_________
             |                  |       |                  |        |                  |
          <term>             <fact>   <term>             <fact>   <term>             <fact>
    */
    private TreeNode term() {

        // Get fact
        TreeNode nFact = fact();

        // Check termRecur
        TreeNode nTermRecur = termRecur();

        if(nTermRecur == null) {
            return nFact;
        }

        nTermRecur.setLeft(nFact);

        return nTermRecur;
    }

    private TreeNode termRecur() {

        TreeNode n = null;

        switch(token.getTokenNo()) {
            case Token.TSTAR:

                // Consume *
                consumeToken();
                n = new TreeNode(ENodeTypes.NMUL);
                break;

            case Token.TDIVD:

                // Consume /
                consumeToken();
                n = new TreeNode(ENodeTypes.NDIV);
                break;

            case Token.TPERC:

                // Consume %
                consumeToken();
                n = new TreeNode(ENodeTypes.NMOD);
                break;

            default:
                // epsilon path
                return null;
        }

        TreeNode nFact = fact();
        n.setRight(nFact);

        TreeNode ntermRecur = termRecur();

        if(ntermRecur == null) {
            return n;
        }

        ntermRecur.setRight(n);

        return ntermRecur;
    }

    /*
        <fact>:= <exponent> <factRec>
        <factRecur>:= ^ <exponent> <factRecur> | e

        Returns:
                     NPOW                           <exponent>
             __________|_________             __________|_________
             |                  |
          <fact>            <exponent>
    */
    private TreeNode fact() {

        // Get exponent
        TreeNode nExponent = exponent();

        // Check factRecur
        TreeNode nFactRecur = factRecur();

        if(nFactRecur == null) {
            return nExponent;
        }

        nFactRecur.setLeft(nExponent);

        return nFactRecur;
    }

    private TreeNode factRecur() {

        TreeNode n = null;

        if(token.getTokenNo() == Token.TCART) {
            n = new TreeNode(ENodeTypes.NPOW);
        }
        else {
            return null;
        }

        TreeNode nExponent = exponent();
        n.setRight(nExponent);

        TreeNode nFactRecur = factRecur();

        if(nFactRecur == null) {
            return n;
        }

        nFactRecur.setRight(n);

        return nFactRecur;
    }

    /*

        <exponent>:= <var> | <intlit> | <reallit> | <fncall> | true | false | ( <bool> )

        Returns:
                  NILIT(intlit)                NFLIT(reallit)                    NTRUE
            __________|_________           __________|_________          __________|___________

                    NFALSE                         <var>                         <bool>
            __________|___________         __________|___________        __________|___________
    */
    private TreeNode exponent() {

        // Check both var and fncall
        TreeNode nFnCall = fncall();

        if(nFnCall != null) {
            return nFnCall;
        }

        TreeNode nVar = var();

        if(nVar != null) {
            return nVar;
        }

        SymbolTableData data;

        // If above are null - we have our end node leaf
        switch(token.getTokenNo()) {

            // Need to insert the symbol table records
            case Token.TILIT:

                data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(),token.getLineNo());
                data.setValue(token.getLexeme());
                data.setType("integer");
                st.insert(data);
                consumeToken();
                return new TreeNode(ENodeTypes.NILIT, data);

            case Token.TFLIT:

                data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());
                data.setValue(token.getLexeme());
                data.setType("real");
                st.insert(data);
                consumeToken();
                return new TreeNode(ENodeTypes.NFLIT, data);

            case Token.TTRUE:

                data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());
                data.setValue(token.getLexeme());
                data.setType("boolean");
                st.insert(data);
                consumeToken();
                return new TreeNode(ENodeTypes.NTRUE, data);

            case Token.TFALS:

                data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());
                data.setValue(token.getLexeme());
                data.setType("boolean");
                st.insert(data);
                consumeToken();
                return new TreeNode(ENodeTypes.NFALS, data);

        }

        if(token.getTokenNo() == Token.TLPAR) {

            // Consume (
            consumeToken();

            TreeNode nBool = bool();

            if(token.getTokenNo() != Token.TRPAR) {

                ErrorObject error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line", token.getLineNo());
                createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
                syntaxErrors.add(error);

                return new TreeNode(ENodeTypes.NUNDF, error);
            }

        }

        ErrorObject error = new ErrorObject("Unexpected token: " + token.getLexeme() + " Line", token.getLineNo());
        createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
        syntaxErrors.add(error);

        // Exponent cant be null, so error
        return new TreeNode(ENodeTypes.NUNDF, error);
    }

    /*
        <fncall>:= <id> ( <fncallTail>
        <fncallTail>:= <elist> ) | )

        <fncall>:= <id> ( <elist> ) |  <id> ()

        Returns:

                   NFCALL(id)                             NFCALL(id)
             __________|_________                   __________|_________
                                                    |
                                                 <elist>
    */
    private TreeNode fncall() {

        TreeNode nFcall = new TreeNode(ENodeTypes.NFCALL);

        if(token.getTokenNo() != Token.TIDEN || lookAhead.getTokenNo() != Token.TLPAR) {
            return null;
        }

        // All functions are global so scope number is 0
        SymbolTableData data = new SymbolTableData(0, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        // Calling a function - This should already exist - get from ST
        // Check if it exists in first, then grab the symbol entry
        // Check if the id matches a function - Functions are global so any scope is fine
        if(checkIdentifier(data) == -1) {

            isCorrect = false;

            ErrorObject error = new ErrorObject("Function has not been declared, Line: ", token.getLineNo());

            semanticErrors.add(error);
            nFcall.addError(error);

        }
        else {

            // The entry exists so grab it
            SymbolTableData funcationData = st.get(data);

            // Consume TIDEN
            consumeToken();

            if(token.getTokenNo() != Token.TLPAR) {

                ErrorObject error = new ErrorObject("Expecting ( in function call, Line ", token.getLineNo());
                createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
                syntaxErrors.add(error);

                return new TreeNode(ENodeTypes.NUNDF, error);
            }

            // consume (
            consumeToken();

            if(token.getTokenNo() == Token.TRPAR) { // What if they just missed the )?

                // Check if the function should have parameters

                if(data.getFuncParams() == null) {

                    isCorrect = false;

                    ErrorObject error = new ErrorObject("Function " + data.getName() + " invalid number of parameters" + ", Line: ", token.getLineNo());

                    semanticErrors.add(error);
                    nFcall.addError(error);

                }

                consumeToken();
                return new TreeNode(ENodeTypes.NFCALL, data);
            }

            TreeNode nElist = eList();

            // nElist has the function params - need to make sure they match the params in symtable
            // nElist can also be null for no params
            SymbolTableData params = nElist.getSymbolTableRecord();


            if(token.getTokenNo() != Token.TRPAR) {

                ErrorObject error = new ErrorObject("Expecting ) in function call, Line ", token.getLineNo());
                createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
                syntaxErrors.add(error);

                return new TreeNode(ENodeTypes.NUNDF, error);
            }

            // Consume )
            consumeToken();

            nFcall.setData(data);
            nFcall.setLeft(nElist);

        }

        return nFcall;
        //return new TreeNode(ENodeTypes.NFCALL, nElist, data);
    }

    /*

        <var>:= <id> <varTail>
        <varTail>:= E | [<expr>] | [<expr>] . <id>

        Returns:
                     NSIMV(id)                              NAELT(id)                             NARRV(id)
             __________|_________                   __________|_________                  __________|___________
                                                    |                                     |
                                                  <expr>                                 <expr>(id)
    */
    private TreeNode var() {

        TreeNode nVar = new TreeNode();

        // Check if the token is of TIDEN else return null for <var>
        if(token.getTokenNo() != Token.TIDEN) {
            return null;
        }

        // Create symbol table entry - Check if it has been created before
        SymbolTableData data = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());

        // Check if we have this ID already in the symbol table if we do attach that one
        int check = checkIdentifier(data);
        if(check == -1 || check == 0) {
            // Unidentified variable ... name
            isCorrect = false;

            ErrorObject error;

            if(check == -1) {
                error = new ErrorObject("Undefined variable, " + token.getLexeme() + " Line: ", token.getLineNo());

            }
            else {
                error = new ErrorObject("Incorrect scope for variable, " + token.getLexeme() + " Line: ", token.getLineNo());

            }
            semanticErrors.add(error);
            nVar.addError(error);
        }
        else {
            // Set the data to the existing one in ST
            data = st.get(data);
        }

        consumeToken();

        TreeNode varTail = varTail(data);

        if(varTail == null) {
            nVar.setNodeType(ENodeTypes.NSIMV);
            nVar.setData(data);
            return nVar;
        }

        if(varTail.getNodeValue().equalsIgnoreCase(ENodeTypes.NAELT.nodeString)) {
            return varTail;
        }

        nVar.setNodeType(ENodeTypes.NARRV);
        nVar.setLeft(varTail);
        nVar.setData(data);

        return nVar;
        //return (varTail.getNodeValue().equalsIgnoreCase(ENodeTypes.NAELT.nodeString)) ? varTail : new TreeNode(ENodeTypes.NARRV, varTail, data);
    }

    private TreeNode varTail(SymbolTableData data) {

        if(token.getTokenNo() != Token.TLBRK) {
            // Epsilon path
            return null;
        }

        // Consume [
        consumeToken();

        TreeNode nExpr = expr();

        // Above is an array index - Need to check if its within bounds

        // Consume ] make sure there is a ]
        if(token.getTokenNo() != Token.TRBRK) {

            ErrorObject error = new ErrorObject("Expecting ( in function call, Line ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);

        }
        // Consume ]
        consumeToken();

        if(token.getTokenNo() != Token.TDOTT) {
            return new TreeNode(ENodeTypes.NAELT, data);
        }

        // Consume .
        consumeToken();

        // Consume <id>
        if(token.getTokenNo() != Token.TIDEN) {

            ErrorObject error = new ErrorObject("Expecting Identifier after array call ", token.getLineNo());
            createSyncSetAndRecover(Token.TTEND, Token.TSEMI);
            syntaxErrors.add(error);

            return new TreeNode(ENodeTypes.NUNDF, error);
        }

        SymbolTableData exprData = new SymbolTableData(scopeNumber, token.getTokenNo(), token.getLexeme(), token.getLineNo());
        st.insert(exprData);

        consumeToken();

        nExpr.setRecord(exprData);

        return nExpr;
    }

    /*
        Used to grab the parameters types of a function to cross reference
        with the function call in CALLSTAT
    */
    private void getFunctionParams(ArrayList<String> params, TreeNode root) {

        if(root == null) {
            return;
        }

        if(root.getNodeValue().equalsIgnoreCase(ENodeTypes.NSIMV.nodeString) ||
           root.getNodeValue().equalsIgnoreCase(ENodeTypes.NILIT.nodeString) ||
           root.getNodeValue().equalsIgnoreCase(ENodeTypes.NFLIT.nodeString)) {

            params.add(root.getSymbolTableRecord().getType());
        }

        getFunctionParams(params, root.getLeft());
        getFunctionParams(params, root.getMiddle());
        getFunctionParams(params, root.getRight());

    }

    /*
        Used to recursivly go through an <expr> tree to add types
        and function return type to the types list

        This will be compared to the <id> type from INITNODE for strong typing

    */
    private void getExprTypes(ArrayList<String> types, TreeNode root) {

        if(root == null) {
            return;
        }

        if(root.getNodeValue().equalsIgnoreCase(ENodeTypes.NILIT.nodeString) || // NSIMV = <id>, get type of id
           root.getNodeValue().equalsIgnoreCase(ENodeTypes.NFLIT.nodeString) || // NTRUE / NFALSE , will grab type boolean
           root.getNodeValue().equalsIgnoreCase(ENodeTypes.NFALS.nodeString) ||
           root.getNodeValue().equalsIgnoreCase(ENodeTypes.NTRUE.nodeString) ||
           root.getNodeValue().equalsIgnoreCase(ENodeTypes.NFCALL.nodeString)) { // Function call get the return type

            if(root.getNodeValue().equalsIgnoreCase(ENodeTypes.NFCALL.nodeString)) {
                types.add(root.getSymbolTableRecord().getReturnType());
            }
            else {
                types.add(root.getSymbolTableRecord().getType());
            }
        }

        getExprTypes(types, root.getLeft());
        getExprTypes(types, root.getMiddle());
        getExprTypes(types, root.getRight());
    }

    private void consumeToken() {

        prevToken = token;

        token = tokenList.remove(START_TOKEN);

        if(token.getTokenNo() == Token.TRETN) {
            returnCount++;
        }

        if(tokenList.size() != 0) {
            lookAhead = tokenList.get(START_TOKEN);
        }

    }

    private Token getToken() {
        return tokenList.get(tokenCount);
    }

    private void increaseScope() {
        scopeNumber++;
    }

    private void decreaseScope() {
        scopeNumber--;
    }

    private boolean hasBeenDeclared(SymbolTableData data) {

        return false;
    }

    private void createSyncSetAndRecover(int... tokens) {

        syncSet.clear();
        for(int t : tokens) {
            syncSet.add(t);
        }

        errorRecovery();
    }

    public String printErrorOutput() {

        StringBuilder errorOutput = new StringBuilder();

        for(ErrorObject e : syntaxErrors) {

            errorOutput.append(e.printParserErrors());

        }

        return errorOutput.toString();
    }

    private Token peek(int number) {

        Token lookaheadToken;

        try {
            lookaheadToken = tokenList.get(number);

        }
        catch(Exception e) {
            // OutofBounds exception
            return null;
        }

        return lookaheadToken;
    }

    private Boolean checkStats() {

        return token.getTokenNo() == Token.TREPT || token.getTokenNo() == Token.TIDEN ||
                token.getTokenNo() == Token.TINPT || token.getTokenNo() == Token.TOUTP ||
                token.getTokenNo() == Token.TRETN;
    }

    private Boolean checkStrStats(){

        return token.getTokenNo() == Token.TTFOR || token.getTokenNo() == Token.TIFTH;
    }

    public boolean isCorrect() {
        return this.isCorrect;
    }

    private boolean checkIntegerAllowed(String value) {

        int testV = Integer.parseInt(value);

        char testValue = (char) testV;

        int testAnswer = Character.getNumericValue(testValue);

        return testV == Character.getNumericValue(testValue);

    }

    public ArrayList<ErrorObject> getSyntaxErrors() {
        return this.syntaxErrors;
    }

    public ArrayList<ErrorObject> getSemanticErrors() {
        return this.semanticErrors;
    }
}