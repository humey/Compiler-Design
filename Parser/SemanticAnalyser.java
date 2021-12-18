package Parser;

/*
    Class that goes through the already constructed AST and checks for syntax
    errors on strong typing, functions ext

*/


import Models.TreeNode;

public class SemanticAnalyser {

    public SemanticAnalyser() {
    }

    public void semanticCheck(TreeNode root) {

        // Recursivly go through the AST for semantic checks
        if(root == null) {
            return;
        }


        switch(root.getNodeValue()) {

            // Check NASGN if allowed
            // eg - k = 2 + l (l is boolean)
            case "NASGN":







        }

        semanticCheck(root.getLeft());
        semanticCheck(root.getMiddle());
        semanticCheck(root.getRight());

    }
}


// CALL STAT NODE
//        if(checkIdentifier(data) == -1) {
//
//                isCorrect = false;
//
//                ErrorObject error = new ErrorObject("Array decleration has not been declared, Line: ", token.getLineNo());
//
//                semanticErrors.add(error);
//                nCall.addError(error);
//
//                }
//                else {
//
//                // nElist has the function params - need to make sure they match the params in symtable
//                // nElist can also be null for no params - need to recursivly go through nElist to get params
//                ArrayList<String> funcParams = new ArrayList<>();
//        getFunctionParams(funcParams, nElist);
//
//        // Check the size if the params match
//        if(funcParams.size() != data.getFuncParams().size()) {
//
//        isCorrect = false;
//
//        ErrorObject error = new ErrorObject("Function " + token.getLexeme() + " invalid number of parameters" + ", Line: ", token.getLineNo());
//
//        semanticErrors.add(error);
//        nCall.addError(error);
//
//        }
//        else {
//
//        // Check each param and make sure they are the same type
//        for(int i = 0; i < data.getFuncParams().size(); i++) {
//
//        if(!data.getFuncParams().get(i).getType().equalsIgnoreCase(funcParams.get(i))) {
//
//        // Incorrect function parameters
//        isCorrect = false;
//
//        ErrorObject error = new ErrorObject("Function " + token.getLexeme() + " invalid parameter type " + funcParams.get(i) + ", Line: ", token.getLineNo());
//
//        semanticErrors.add(error);
//        nCall.addError(error);
//
//        break;
//        }
//        }
//        }
//        }





// NINIT NODE
//        if(checkIdentifier(data) != 1 || checkIdentifier(data) != 0) {
//
//                isCorrect = false;
//
//                ErrorObject error = new ErrorObject("Unknown variable " + data.getName() + ", Line: ", token.getLineNo());
//
//                semanticErrors.add(error);
//                nInit.addError(error);
//
//                }
//                else {
//                // Our ID exists, so link this nodes data to the existing in ST
//                // Symbol table entry should have the type of <id> - to match against the <expr> for strong typing
//                data = st.get(data);
//
//                // Check <expr> tree if each matches the type of <id>
//                // Modulo wont work with NFLIT/real
//                // if reals within integers, int is promoted to NFLIT/real
//
//                ArrayList<String> types = new ArrayList<>();
//
//        getExprTypes(types, nExpr);
//
//        for(int i = 0; i < types.size(); i++ ) {
//
//        if(!types.get(i).equalsIgnoreCase(data.getType())) {
//
//        // Mismatch in types
//        // example - count(int) = 2 + true
//        // count(int) = 2 + fncall() (returns true or false)
//
//        }
//
//
//        }
//
//
//        }