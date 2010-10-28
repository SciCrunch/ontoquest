package edu.sdsc.ontoquest.query;

import java.util.ArrayList;

import edu.sdsc.ontoquest.query.plan.PlanNode;

/**
 * <p>$Id
 *
 */
public class SymbolTable {
  // Currently use linear search. Since the estimated number
  // of variable is likely less than 10, it isn't efficient to
  // use hash.
  private Symbol[] symtab;
  private int count = 0;
  private int increment = 10;
  private Symbol nullVar = new Symbol(Symbol.NULL_VAR, null, null);

  /**
   * Constructs an empty symbol table.
   */
  public SymbolTable() {
    this(10, 10);
  }

  /** Constructs an empty symbol table.
   *  @param initialCapacity: intial capacity of symbol table.
   *  @param increment: increment size
   */
  public SymbolTable(int initialCapacity, int increment) {
    symtab = new Symbol[initialCapacity];
    if (increment > 0) {
      this.increment = increment;
    }
  }

  /**
   * Adds symbol into symbol table.
   */
  public void addSymbol(Symbol symbol) {
    if (count >= symtab.length) {
      // resize symbol table
      Symbol[] s = new Symbol[symtab.length + increment];
      System.arraycopy(symtab, 0, s, 0, symtab.length);
      symtab = s;
    }

    symtab[count++] = symbol;
  }

  /**
   * Returns the size of this table.
   */
  public int size() {
    trimToSize();
    return count;
  }

  /**
   * Gets the SQL variable in a query.
   * @param name: variable name
   * @param query: the SQL query which the variable belongs to.
   */
  public Symbol getSymbol(String name, PlanNode node) {
    Symbol[] result = getSymbols(name, node, true);
    if (result == null || result.length == 0) {
      return null;
    }
    return result[0];
  }

  /**
   * Get symbols in a <I>query</I>.
   * @param name: the name of the symbols to retrieve. If null, get all symbols
   * @param query: the query which the symbols belong to.
   * @param isOne: Get one or all matched symbols. If true, get the first
   * matched symbol, otherwise, get all matched ones.
   */
  private Symbol[] getSymbols(String name, PlanNode node, boolean isOne) {
    ArrayList<Symbol> tmp = new ArrayList<Symbol>();
    boolean isNameMatched = false;
    boolean isQueryMatched = false;

    for (int i = 0; i < symtab.length; i++) {
      if (symtab[i] == null) {
        continue;
      }
      isNameMatched = (name == null) ? true : (symtab[i].getName().equals(name));
      isQueryMatched = (node == null) ? true :
          (symtab[i].getNode().equals(node));
      if (isNameMatched && isQueryMatched) {
        if (isOne) {
          return new Symbol[] {
              symtab[i]};
        }
        tmp.add(symtab[i]);
      }
    }
    if (tmp.size() == 0) {
      return new Symbol[0];
    }

    Symbol[] result = new Symbol[tmp.size()];
    tmp.toArray(result);
    return result;
  }

  /**
   * Gets all matching variables in all nodes.
   */
  public Symbol[] getSymbols(String name) {
    return getSymbols(name, null, false);
  }

  /**
   * Gets all matching variables in a query.
   */
  public Symbol[] getSymbols(String name, PlanNode node) {
    return getSymbols(name, node, false);
  }

  /**
   * Gets all symbols(variables) in a plan node.
   */
  public Symbol[] getAllSymbolsInOneQuery(PlanNode node) {
    return getSymbols(null, node, false);
  }

  /**
   * Gets all distinct symbols in a node. If there are more than
   * two symbols having the same name, return only one of them.
   */
  public Symbol[] getDistinctSymbolsInOneQuery(PlanNode node) {
    Symbol[] symbols = getAllSymbolsInOneQuery(node);
    Symbol[] tmp = new Symbol[symbols.length];
    boolean hasMatched = false;
    int count = 0;
    String name = null;
    // iterate symbols[] and tmp[] by nested loop. Since the
    // estimated size of variables in a node is less than 10^2,
    // hashtable is not necessary.
    for (int i = 0; i < symbols.length; i++) {
      hasMatched = false;
      name = symbols[i].getName();
      for (int j = 0; j < count; j++) {
        if (name.equalsIgnoreCase(tmp[j].getName())) {
          hasMatched = true;
          break;
        }
      }
      if (!hasMatched) {
        tmp[count++] = symbols[i];
      }
    }
    Symbol[] distinctSymbols = new Symbol[count];
    System.arraycopy(tmp, 0, distinctSymbols, 0, count);
    return distinctSymbols;
  }

  /**
   * Gets all symbols in this symbol table.
   */
  public Symbol[] getAllSymbols() {
    trimToSize();
    return symtab;
  }

  /**
   * trim the capacity to table size.
   */
  public void trimToSize() {
    int mycount = 0;
    Symbol[] tmp = new Symbol[symtab.length];
    for (int i = 0; i < symtab.length; i++) {
      if (symtab[i] != null && !symtab[i].equals(nullVar)) {
        tmp[mycount++] = symtab[i];
      }
    }
    symtab = new Symbol[mycount];
    System.arraycopy(tmp, 0, symtab, 0, mycount);
  }


}
