/** ------------------------------------------------------------------------- *
 * libpomdp
 * ========
 * File: catchGen.java
 * Description: 
 --------------------------------------------------------------------------- */

// imports
import java.io.*;
import java.util.*;

public class catchGen {

    // grid size
    private int n;

    // rock positions
    private int k[][]; 

    // output stream
    private PrintStream out;
	
    // initial position of the agent
    private int[] apos;

    // # of rocks
    private int nr;

    // parameters 
    final int TERMINAL_COST            = -10;
    final int ILLEGAL_PENALTY          = 100;
    final int SAMPLE_GOOD_COST         = -10;
    final int SAMPLE_BAD_PENALTY       = 10;
    final int SENSOR_HALF_EFF_DISTANCE = 20;

    // constructor:
    // n: n x n grid
    // k: k x 2 array with (j,i) positions of the k rocks
    // out: output filename
    public rocksampleGen(int n, int k[][], int apos[], PrintStream out) {
	this.k   = k;
	this.apos= apos;
	this.n   = n;
	this.nr  = k.length; // nr of rows!
	this.out = out;
	generate();
    }

    public void generate() {
	String rv[] = {"g", "b"};
	int c,d;
	// header
	out.println("// ------------------------------------------------------------------------- *");
	out.println("// libpomdp");
	out.println("// ========");
	out.println("// File: autogenerated by rocksampleGen.java");
	out.println("// Description: generate catch problem");
	out.println("// ------------------------------------------------------------------------- *");

	// world description
	out.print("// grid size: ");
	out.println(n + " x " + n); 
	out.print("// agent's initial position: ");
	out.println("["+apos[0]+","+apos[1]+"]");
	out.println("// rock locations: ");
	for(c=0; c<nr; c++) out.println("// r"+c+": ["+k[c][0]+","+k[c][1]+"]");
	out.println();

	// states
	// position
	out.println("// state variables");
	out.println("// ---------------");
	out.println("(variables");
	out.print(" " + varDecl("j", n+1));
	out.print(" " + varDecl("i", n));
	// rocks
	for(c=0; c<nr; c++) out.print(" " + varDecl("r"+c, rv, 2));
	out.println(")");

	// observations
	out.println();
	out.println("// observation variables");
	out.println("// ---------------------");
	out.println("(observations");
	out.println(" (o og ob)");
	out.println(")");

	// initial belief state
	out.println();
	out.println("// initial belief");
	out.println("// --------------");
	out.println("init [* (j " + massX("j", n+1, apos[0]) + ")");
	out.println("        (i " + massX("i", n  , apos[1]) + ")");
	for(c=0; c<nr; c++) 
	    out.println("        (r"+c + " " + unifD("r"+c, rv, 2) + ")");
	out.println("     ]");

	// move actions
	// north
	out.println();
	out.println("// move actions");
	out.println("// ------------");
	out.println("action north");
	out.println("    j (SAMEj)");
	out.print("    i " + shiftUp(7, "i", n));
	for(c=0; c<nr; c++) out.println("    r"+c + " (SAMEr"+c+")");
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (o' (og (1.0)) (ob (0.0)))");
	out.println("    endobserve");	
	// there is a cost for illegal moves
	out.print("    cost"+illegalMoveUp(9, "i", n));
	out.println("endaction");

	// south
	out.println();
	out.println("action south");
	out.println("    j (SAMEj)");
	out.print("    i " + shiftDown(7, "i", n));
	for(c=0; c<nr; c++) out.println("    r"+c + " (SAMEr"+c+")");
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (o' (og (1.0)) (ob (0.0)))");
	out.println("    endobserve");
	// there is a cost for illegal moves
	out.print("    cost"+illegalMoveDown(9, "i", n));
	out.println("endaction");

	// east
	out.println();
	out.println("action east");
	out.print("    j " + shiftUp(7, "j", n+1));
	out.println("    i (SAMEi)");
	for(c=0; c<nr; c++) out.println("    r"+c + " (SAMEr"+c+")");
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (o' (og (1.0)) (ob (0.0)))");
	out.println("    endobserve");
	// reward for moving off the grid goes here
	out.println("    cost"+terminalReward(9, "j", n+1));
	out.println("endaction");

	// west
	out.println();
	out.println("action west");
	out.print("    j " + shiftDownJ(7, "j", n+1));
	out.println("    i (SAMEi)");
	for(c=0; c<nr; c++) out.println("    r"+c + " (SAMEr"+c+")");
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (o' (og (1.0)) (ob (0.0)))");
	out.println("    endobserve");
	// there is a cost for illegal moves
	out.print("    cost"+illegalMoveDown(9, "j", n+1));
	out.println("endaction");

	// sample action
	out.println();
	out.println("action sample");
	out.println("    j (SAMEj)");
	out.println("    i (SAMEi)");
	// if the agent is colocated with a rock, its value
	// becomes bad right after sampling it
	for(c=0; c<nr; c++) 
	    out.print("    r"+c+ " "+ sampleRockState(7, c));
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (o' (og (1.0)) (ob (0.0)))");
	out.println("    endobserve");
	out.print("    cost" + " "+ sampleRockCost(9));
	out.println("endaction");

	// check_i actions
	// none of the state variables are affected 
	for(c=0; c<nr; c++) {
	    	out.println();
		out.println("action check"+c);
		out.println("    j (SAMEj)");
		out.println("    i (SAMEi)");
		for(d=0;d<nr;d++) out.println("    r"+d+" (SAMEr"+d+")");
		out.println("    observe");
		out.print("        o " + checkRockObs(10, c));
		out.println("    endobserve");
		out.println("endaction");
	}

	// reward for being in the end state - this causes infinite rewards!!!!
	// 	out.println();
	// 	out.println("reward (j");
	// 	for(c=0; c<n+1; c++) {
	// 	    if (c < n )
	// 		out.println("        (j"+c+" (0))");
	// 	    else
	// 		out.println("        (j"+c+" (10))");
	// 	}
	// 	out.println("      )");

	// discount and tolerance
	out.println();
	out.println("discount 0.95");
	out.println("tolerance 0.00001");
    } // generate

    // consturct a string of the form
    // (varname (varname1 varname2 ... ))
    private String varDecl(String varname, int n) {
	String v = "(" + varname;
	int c;
	for(c=0; c<n; c++) v = v.concat(" " + varname + c);
	v = v.concat(")\n");
	return v;
    } // varDecl

    // with value names
    private String varDecl(String varname, String vn[], int n) {
	String v = "(" + varname;
	int c;
	for(c=0; c<n; c++) v = v.concat(" " + vn[c]);
	v = v.concat(")\n");
	return v;
    } // varDecl

    // // print rock declarations with indentation
//     private String rockDecl(String ind, int nr) {
// 	String v = "";
// 	int c;
// 	for(c=0; c<nr; c++) v = v.concat(ind + "(r" + c + " g b)\n");
// 	return v;
//     } // rockDecl 
	
    // concentrate all mass in value pos
    private String massX(String varname, int l, int pos) {
	String v = "";
	String prob = "";
	int c;
	for(c=0; c<l; c++) {
	    if (c == pos)
		prob = "1.0";
	    else
		prob = "0.0";
	    v = v.concat("(" + varname + c + " (" + prob + "))");
	}
	return v;
    } // massX

    // uniform dis of values
    private String unifD(String varname, int l) {
	String v="";
	int c;
	double prob = 1.0/l;
	for(c=0; c<l; c++) 
	    v = v.concat("(" + varname + c + " (" + prob + "))");
	return v;
    } // unifD

    // with value names
    private String unifD(String varname, String vn[], int l) {
	String v="";
	int c;
	double prob = 1.0/l;
	for(c=0; c<l; c++) 
	    v = v.concat("(" + vn[c] + " (" + prob + "))");
	return v;
    } // unifD
	
    // transition by shifting all the mass one value "up"
    private String shiftUp(int ind, String varname, int l) {
	String v = "(" + varname + "\n";
	int c, s;
	for(c=0; c<l; c++) {
	    if (c < l-1)
		s = c + 1;
	    else
		s = c;		    
	    v = v.concat(indent(ind) + "("+varname+c+ " ("+varname+"'" + massX(varname, l, s) + "))\n");
	}
	v = v.concat(indent(ind-1)+")\n");
	return v;
    } // shiftUp

    // terminal reward for moving off the grid
    private String terminalReward(int ind, String varname, int l) {
	String v = "(" + varname + "\n";
	int c;
	for(c=0; c<l; c++) {
	    if (c==l-2)
		v=v.concat(indent(ind) + "("+varname+c+ " (-10))\n");
	    else
		v=v.concat(indent(ind) + "("+varname+c+ " (0))\n");
	}
	v = v.concat(indent(ind-1)+")\n");
	return v;
    }

    // transition by shifting all the mass one value "down"
    private String shiftDown(int ind, String varname, int l) {
	String v = "(" + varname + "\n";
	int c, s;
	for(c=0; c<l; c++) {
	    if (c > 0)
		s = c - 1;
	    else
		s = c;		    
	    v = v.concat(indent(ind) + "("+varname+c+ " ("+varname+"'" + massX(varname, l, s) + "))\n");
	}
	v = v.concat(indent(ind-1)+")\n");
	return v;
    } // shiftDown

    // transition by shifting all the mass one value "down" for j
    private String shiftDownJ(int ind, String varname, int l) {
	String v = "(" + varname + "\n";
	int c, s;
	for(c=0; c<l; c++) {
	    if (c > 0 && c < l-1)
		s = c - 1;
	    else
		s = c;		    
	    v = v.concat(indent(ind) + "("+varname+c+ " ("+varname+"'" + massX(varname, l, s) + "))\n");
	}
	v = v.concat(indent(ind-1)+")\n");
	return v;
    } // shiftDown

    // penalty cost for illegal move
    private String illegalMoveUp(int ind, String varname, int l) {
	String v="("+varname+"\n";
	int c;
	for(c=0; c<l; c++) {
	    if (c<l-1)
		v=v.concat(indent(ind)+"("+varname+""+c+" (0))\n");
	    else
		v=v.concat(indent(ind)+"("+varname+""+c+" ("+ILLEGAL_PENALTY+"))\n");
	}
	v=v.concat(indent(ind)+")\n");
	return v;
    } // illegalMoveUp

    // penalty cost for illegal move
    private String illegalMoveDown(int ind, String varname, int l) {
	String v="("+varname+"\n";
	int c;
	for(c=0; c<l; c++) {
	    if (c>0)
		v=v.concat(indent(ind)+"("+varname+""+c+" (0))\n");
	    else
		v=v.concat(indent(ind)+"("+varname+""+c+" ("+ILLEGAL_PENALTY+"))\n");
	}
	v=v.concat(indent(ind)+")\n");
	return v;
    } // illegalMoveDown

    // indentation
    private String indent(int i) {
	String v="";
	int c;
	for(c=0; c<i; c++) v=v.concat(" ");
	return v;
    }

    // print out all possible positions
    // if we match a rock, alter its variable
    private String sampleRockState(int ind, int r) {
	String v="(j\n";
	int c,d;
	for(c=0; c<n+1; c++) {
	    v=v.concat(indent(ind+1)+"(j"+c+"\n");
	    v=v.concat(indent(ind+3) + "(i\n");
	    for(d=0; d<n; d++) {
		v=v.concat(indent(ind+4)+"(i"+d);
		//		for(r=0; r<nr; r++) {		
		if (c==k[r][0] && d==k[r][1]) {
		    v=v.concat("\n");
		    v=v.concat(indent(ind+6)+"(r"+r+" (g (r"+r+"' (g (0.0)) (b (1.0))))"+"\n" +	     
			       indent(ind+9)+       " (b (r"+r+"' (g (0.0)) (b (1.0))))))"+"\n");
		}else {			
		    v=v.concat(" (SAMEr"+r+"))\n");
		}
		    //}		
	    }
	    v=v.concat(indent(ind+3) + "))\n");
	    //  v=v.concat(indent(ind) + ")");
	}
	v=v.concat(indent(ind) + ")\n");
	return v;
    } // sampleRockState

    // cost of the sample action
    private String sampleRockCost(int ind) {
	String v="(j\n";
	int c,d,r;
	boolean matched;
	for(c=0; c<n+1; c++) {
	    v=v.concat(indent(ind+1)+"(j"+c+"\n");
	    v=v.concat(indent(ind+3) + "(i\n");
	    for(d=0; d<n; d++) {
		v=v.concat(indent(ind+4)+"(i"+d);
		// here we ask if any rock matches this agent position
		// only one can, since there are <= 1 rocks per square
		matched = false;
		for(r=0; r<nr; r++) {		
		    if (c==k[r][0] && d==k[r][1]) {
			matched = true;
			v=v.concat("\n");
			v=v.concat(indent(ind+6)+"(r"+r+
				   " (g ("+SAMPLE_GOOD_COST+")) (b ("+SAMPLE_BAD_PENALTY+"))))"+"\n"); 	     
			//            indent(ind+9)+       " (b (r"+r+"' (g (0.0)) (b (1.0))))))"+"\n");
			break;
		    }
		}
		// otherwise this action is illegal
		if(!matched) v=v.concat(" ("+ILLEGAL_PENALTY+"))\n");				
	    }
	    v=v.concat(indent(ind+3) + "))\n");
	    //  v=v.concat(indent(ind) + ")");
	}
	v=v.concat(indent(ind) + ")\n");
	return v;
    } // sampleRockCost

    // observation mode for check_r
    private String checkRockObs(int ind, int r) {
	String v="(j'\n";
	int c,d;
	for(c=0; c<n+1; c++) {	    
	    v=v.concat(indent(ind+1)+"(j"+c+"\n");
	    // check for the end state
	    if(c==n) {
		v=v.concat(indent(ind+3)+"(o' (og (0.5)) (ob (0.5))))\n");
		break;
	    }
	    v=v.concat(indent(ind+3) + "(i'\n");
	    for(d=0; d<n; d++) {
		v=v.concat(indent(ind+4)+"(i"+d);
		//if (c==k[r][0] && d==k[r][1]) {
		v=v.concat("\n");
		// assign probs to og and ob according to distance to the rock
		v=v.concat(indent(ind+6)+"(r"+r+"' (g (o' (og (" +acc(c,d,r)+")) (ob (" +(1-acc(c,d,r))+"))))"+"\n" +	     
			   indent(ind+10)+        " (b (o' (og (" +(1-acc(c,d,r))+")) (ob ("+acc(c,d,r)+"))))))"+"\n");
		//}else {			
		//v=v.concat(" (SAMEr"+r+"))\n");
		//}
	    }
	    v=v.concat(indent(ind+3) + "))\n");
	}
	v=v.concat(indent(ind) + ")\n");
	return v;
    } // checkRockObs
	
    // compute accuracy of sensor based on distance
    private double acc(int j, int i, int r) {
	// double d0 = 1.0;
	//	System.out.println(Math.exp(-4));
	//	System.out.println(Math.pow(4,1));
	// compute euclidean distance
	double dist = Math.sqrt(Math.pow(j-k[r][0], 2) + Math.pow(i-k[r][1], 2));
	//	double eta  = Math.pow(2,-dist/d0);
	double eta  = Math.exp(- dist / SENSOR_HALF_EFF_DISTANCE * Math.log(2.0));
	double acc  = (1.0 + eta) / 2.0;
	return acc;
    } // acc

} // rocksampleGen