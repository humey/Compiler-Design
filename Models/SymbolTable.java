package Models;

import java.util.ArrayList;
import java.util.HashMap;

/*
    Methods:
        Insert method:
        Lockup method:
        Get method:
        Update method:

    Type of scope:
        Global scope
        Function scope
        Main scope
 */

public class SymbolTable {

    private HashMap<Integer, ArrayList<SymbolTableData>> st = new HashMap<>();
    private ArrayList<SymbolTableData> temp;

    public SymbolTable() {}

    public void insert(SymbolTableData data) {
        // Check if there is already an entry at the items hashcode
        if(getBucket(data) == null) {

            ArrayList<SymbolTableData> newEntry = new ArrayList<>();
            newEntry.add(data);

            st.put(data.hashCode(), newEntry);
        }
        else {
            // Already a bucket in the entry
            ArrayList<SymbolTableData> bucket = getBucket(data);

            bucket.add(data);
        }
    }

    /*
        Lookup a symbol table entry

        Returns: 1 if the symbol being entered exists within the same scope
                 0 if the symbol exists in the symbol table but different scope
                -1 if it doesnt exist
    */
    public int lookup(SymbolTableData data) {

        if(st.get(data.hashCode()) != null) {
            // Entry exists check the bucket if we have a match for value and scope
            ArrayList<SymbolTableData> bucket = getBucket(data);

            for(SymbolTableData d : bucket) {

                if(d.getTokenNo() == data.getTokenNo() && d.getName().equalsIgnoreCase(data.getName())) {

                    // The entry is in the symbol table, check if the scope is the same or an allowed scope
                    if(d.getScopeNumber() == data.getScopeNumber() || d.getScopeNumber() <= data.getScopeNumber()) {

                        return 1;
                    }
                    return 0;
                }

            }
        }
        return -1;
    }

    public SymbolTableData get(SymbolTableData data) {

        ArrayList<SymbolTableData> bucket = getBucket(data);

        for(SymbolTableData d : bucket) {

            if(d.getTokenNo() == data.getTokenNo() && d.getName().equalsIgnoreCase(data.getName()) && d.getScopeNumber() == data.getScopeNumber() || d.getScopeNumber() <= data.getScopeNumber()) {
                // Update the line number with the new entry
                d.setLineNo(data.getLineNo());
                return d;
            }
        }
        // Shouldnt return null, but if it returns null doesnt exist
        return null;
    }

    private ArrayList<SymbolTableData> getBucket(SymbolTableData data) {
        return st.get(data.hashCode());
    }

}
