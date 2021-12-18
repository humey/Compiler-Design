package Models;

/*
    Comp3290 Compiler Design
    SymbolTableData.java class - Class that holds the symboltable entry data
                                 Variables:
                                    Name:    Store this in a string
                                    Type:    Variable, function, procedure name, array - This is still a work in progress and will change
                                             during semantic phase no doubt
                                    Scope:   Given a scope number for reference - from 0..n
                                    Line:    Line number where it lives
                                    isArray: Boolean if token is an array
                                             Array size - These will be used during semantic phase so havent used during parsing yet
                                    Function paramters:  A list of symbol table data entries for the function params
                                    structId and typeId: Are integers for the symboltable entry keys for the other entries

                                    This lives in a hashtable partition:

                                      Int   List<SymbolTableData>
                                    |     |                      |
                                    |     |                      |
                                    |     |                      |
                                    |_____|______________________|

   Created by - Keeylan Hume C3320396
 */

import java.util.ArrayList;

public class SymbolTableData {

    private int scopeNumber, arraySize;
    private int tokenNo, keyId, lineNo;
    private String type, value, returnType, name;
    private boolean isArray, isConstant, isFunction, isDeclared;
    private ArrayList<SymbolTableData> funcParams;
    private int structId, typeId;
    private int counter[] = { 0, 0, 0 };

    public SymbolTableData(int scopeNumber, int tokenNo, String name, int lineNo) {

        this.scopeNumber = scopeNumber;
        this.tokenNo = tokenNo;
        this.arraySize = 0;
        this.isArray = false;
        this.name = name;
        this.lineNo = lineNo;
        this.isDeclared = false;

        this.keyId = hashCode();
    }

    public SymbolTableData(){}

    public void setIsFunction() {
        this.isFunction = true;
        funcParams = new ArrayList<>();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getScopeNumber() {
        return scopeNumber;
    }

    public void setScopeNumber(int scopeNumber) {
        this.scopeNumber = scopeNumber;
    }

    public int getTokenNo() {
        return tokenNo;
    }

    public void setTokenNo(int tokenNo) {
        this.tokenNo = tokenNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setIsConstant() {
        isConstant = true;

    }

    public void addFunctionParam(SymbolTableData param) {
        this.funcParams.add(param);
    }

    public ArrayList<SymbolTableData> getFuncParams() {
        return this.funcParams;
    }

    public void setIsDeclared() {
        this.isDeclared = true;
    }

    public boolean getIsDeclared() {
        return this.isDeclared;
    }

    public void setStructId(int structId) {
        this.structId = structId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getReturnType() {
        return this.returnType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
    }

    public int getLineNo() {
        return this.lineNo;
    }

    public void setOffset(int base, int offset) {
        counter[base] += offset;
    }

    public int getOffset(int base) {
        return counter[base];
    }


    @Override
    public int hashCode() {

        return this.keyId = this.name.hashCode() + this.tokenNo;

    }

}