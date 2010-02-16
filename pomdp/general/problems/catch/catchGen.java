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

    // initial position of the wumpus
    private int[] wpos;

    // # of rocks
    private int nr;

    // parameters 
    final int TERMINAL_COST            = -10;
    final int ILLEGAL_PENALTY          = 100;
    final int CATCH_GOOD_COST          = -10;
    final int CATCH_BAD_PENALTY        =  10;
    final int SENSOR_HALF_EFF_DISTANCE =  20;
    final int CHECK_RANGE              =   3; // must be an odd int >= 3

    // constructor:
    // n: n x n grid
    // apos: agent's initial position
    // wpos: wumpus' initial position
    // out: output filename
    public catchGen(int n, int apos[], int wpos[], PrintStream out) {
	//this.k    = k;
	this.apos = apos;
	this.wpos = wpos;
	this.n    = n;
	//this.nr   = k.length; // nr of rows!
	this.out  = out;
	//	generate();
    }

    public void generate() {
	//String rv[] = {"g", "b"};
	//int c,d;
	// header
	out.println("// ------------------------------------------------------------------------- *");
	out.println("// libpomdp");
	out.println("// ========");
	out.println("// File: autogenerated by catchGen.java");
	out.println("// Description: the catch problem");
	out.println("// ------------------------------------------------------------------------- *");

	// world description
	out.print("// grid size: ");
	out.println(n + " x " + n); 
	out.print("// agent's initial position: ");
	out.println("["+apos[0]+","+apos[1]+"]");
	out.print("// wumpus' initial position: ");
	out.println("["+wpos[0]+","+wpos[1]+"]");
	// out.println("// rock locations: ");
	// for(c=0; c<nr; c++) out.println("// r"+c+": ["+k[c][0]+","+k[c][1]+"]");
	out.println();

	// states
	// agent and wumpus positions
	out.println("// state variables");
	out.println("// ---------------");
	out.println("(variables");
	out.print  (" " + varDecl("aj", n));
	out.print  (" " + varDecl("ai", n));
	out.print  (" " + varDecl("wj", n));
	out.print  (" " + varDecl("wi", n));

	// rocks
	//for(c=0; c<nr; c++) out.print(" " + varDecl("r"+c, rv, 2));
	//out.println(")");

	// observations
	// wp: wumpus present
	// wa: wumpus absent
	out.println();
	out.println("// observation variables");
	out.println("// ---------------------");
	out.println("(observations");
	out.println(" (o wp wa)");
	out.println(")");

	// initial belief state
	out.println();
	out.println("// initial belief");
	out.println("// --------------");
	out.println("init [* (aj " + massX("aj", n, apos[0]) + ")");
	out.println("        (ai " + massX("ai", n, apos[1]) + ")");
	out.println("        (oj " + massX("wj", n, wpos[0]) + ")");
	out.println("        (oi " + massX("wi", n, wpos[1]) + ")");
	//for(c=0; c<nr; c++) 
	//    out.println("        (r"+c + " " + unifD("r"+c, rv, 2) + ")");
	out.println("     ]");


	// wumpus' behaviour
	out.println();
	out.println("// wumpus' behaviour");
	out.println("// -----------------");
	out.println("dd wumpusb");
	out.print  (randomizeLoc("wj")); 
	out.print  (randomizeLoc("wi"));
	out.println("enddd");

	// uninformative observation
	out.println();
	out.println("// uninformative observation");
	out.println("// -------------------------");
	out.println("dd uninfobs");
	out.println("    (o' (wp (1.0)) (wa (0.0)))");
	out.println("enddd");

	// move actions
	// north
	out.println();
	out.println("// move actions");
	out.println("// ------------");
	out.println("action north");
	out.println("    aj (SAMEaj)");
	out.print  ("    ai " + shiftUp(9, "ai", n));
	// the wumpus' location
	out.println("    wj (wumpusb)");
	out.println("    wi (wumpusb)");	
	//for(c=0; c<nr; c++) out.println("    r"+c + " (SAMEr"+c+")");
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (uninfobs)");
	out.println("    endobserve");	
	// there is a cost for illegal moves
	out.print  ("    cost"+illegalMoveUp(9, "ai", n));
	out.println("endaction");

	// south
	out.println();
	out.println("action south");
	out.println("    aj (SAMEaj)");
	out.print  ("    ai " + shiftDown(9, "ai", n));
	// the wumpus' location
	out.println("    wj (wumpusb)");
	out.println("    wi (wumpusb)");	
	//for(c=0; c<nr; c++) out.println("    r"+c + " (SAMEr"+c+")");
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (uninfobs)");
	out.println("    endobserve");
	// there is a cost for illegal moves
	out.print  ("    cost"+illegalMoveDown(9, "ai", n));
	out.println("endaction");

	// east
	out.println();
	out.println("action east");
	out.print  ("    aj " + shiftUp(9, "aj", n));
	out.println("    ai (SAMEai)");
	// the wumpus' location
	out.println("    wj (wumpusb)");
	out.println("    wi (wumpusb)");
	//for(c=0; c<nr; c++) out.println("    r"+c + " (SAMEr"+c+")");
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (uninfobs)");
	out.println("    endobserve");
	// reward for moving off the grid goes here
	//out.println("    cost"+terminalReward(9, "aj", n));
	out.println("endaction");

	// west
	out.println();
	out.println("action west");
	out.print  ("    aj " + shiftDown(9, "aj", n));
	out.println("    ai (SAMEai)");
	// the wumpus' location
	out.println("    wj (wumpusb)");
	out.println("    wi (wumpusb)");
	//for(c=0; c<nr; c++) out.println("    r"+c + " (SAMEr"+c+")");
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (uninfobs)");
	out.println("    endobserve");
	// there is a cost for illegal moves
	out.print  ("    cost"+illegalMoveDown(9, "aj", n));
	out.println("endaction");

	// sample action
	out.println();
	out.println("action catch");
	out.println("    aj (SAMEaj)");
	out.println("    ai (SAMEai)");
	// the wumpus' location
	out.println("    wj (wumpusb)");
	out.println("    wi (wumpusb)");
	// if the agent is colocated with a rock, its value
	// becomes bad right after sampling it
	//for(c=0; c<nr; c++) 
	//    out.print("    r"+c+ " "+ sampleRockState(7, c));
	// uninformative observation here
	out.println("    observe");	
	out.println("        o (uninfobs)");
	out.println("    endobserve");
	// if the agent is colocated with the wumpus, he receives a reward
	out.print  ("    cost  " + catchWumpusCost(10));
	out.println("endaction");

	// check_i actions
	// none of the state variables are affected 
	//for(c=0; c<nr; c++) {
	for(int x=0;x<CHECK_RANGE;x++) {
	    for(int y=0;y<CHECK_RANGE;y++) {		
		out.println();
		out.println("action check_" + x + "_" + y);
		out.println("    aj (SAMEaj)");
		out.println("    ai (SAMEai)");
		//for(d=0;d<nr;d++) out.println("    r"+d+" (SAMEr"+d+")");
		out.println("    wj (wumpusb)");
		out.println("    wi (wumpusb)");
		out.println("    observe");
		out.print  ("        o " + checkWumpusObs(x, y, 9));
		out.println("    endobserve");
		out.println("endaction");
	    }
	}

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

    // randomize wumpu's location
    private String randomizeLoc(String varname) {
	String v="";
	v = v.concat("(" + varname + "\n");
	for(int c=0;c<n;c++) {
	    v=v.concat("    (" + varname + c +" (" + varname + "'");
	    v=v.concat(randomNext(c,varname));
	    v=v.concat("))\n");
	}
	v=v.concat(")\n");
	return v;
    }

    // randomly advance or move back in a string topoly - with bouncing
    private String randomNext(int c, String varname) {
	String v="";
	int ch1, ch2;
	ch1 = c-1; ch2 = c+1;
	if(0   == c) { ch1 =  0;  ch2 =   1; }
	if(n-1 == c) { ch1 = n-1; ch2 = n-2; }
	for(int k=0;k<n;k++) {
	    if(ch1 == k || ch2 == k) 
		v=v.concat(" (" + varname + k + " (0.5))");
	    else
		v=v.concat(" (" + varname + k + " (0.0))");
	}
	return v;
    }


    // // uniform dis of values
//     private String unifD(String varname, int l) {
// 	String v="";
// 	int c;
// 	double prob = 1.0/l;
// 	for(c=0; c<l; c++) 
// 	    v = v.concat("(" + varname + c + " (" + prob + "))");
// 	return v;
//     } // unifD

//     // with value names
//     private String unifD(String varname, String vn[], int l) {
// 	String v="";
// 	int c;
// 	double prob = 1.0/l;
// 	for(c=0; c<l; c++) 
// 	    v = v.concat("(" + vn[c] + " (" + prob + "))");
// 	return v;
//     } // unifD
	
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

   //  // terminal reward for moving off the grid
//     private String terminalReward(int ind, String varname, int l) {
// 	String v = "(" + varname + "\n";
// 	int c;
// 	for(c=0; c<l; c++) {
// 	    if (c==l-2)
// 		v=v.concat(indent(ind) + "("+varname+c+ " (-10))\n");
// 	    else
// 		v=v.concat(indent(ind) + "("+varname+c+ " (0))\n");
// 	}
// 	v = v.concat(indent(ind-1)+")\n");
// 	return v;
//     }

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

   //  // transition by shifting all the mass one value "down" for j
//     private String shiftDownJ(int ind, String varname, int l) {
// 	String v = "(" + varname + "\n";
// 	int c, s;
// 	for(c=0; c<l; c++) {
// 	    if (c > 0 && c < l-1)
// 		s = c - 1;
// 	    else
// 		s = c;		    
// 	    v = v.concat(indent(ind) + "("+varname+c+ " ("+varname+"'" + massX(varname, l, s) + "))\n");
// 	}
// 	v = v.concat(indent(ind-1)+")\n");
// 	return v;
//     } // shiftDown

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

    // // print out all possible positions
//     // if we match a rock, alter its variable
//     private String sampleRockState(int ind, int r) {
// 	String v="(j\n";
// 	int c,d;
// 	for(c=0; c<n+1; c++) {
// 	    v=v.concat(indent(ind+1)+"(j"+c+"\n");
// 	    v=v.concat(indent(ind+3) + "(i\n");
// 	    for(d=0; d<n; d++) {
// 		v=v.concat(indent(ind+4)+"(i"+d);
// 		//		for(r=0; r<nr; r++) {		
// 		if (c==k[r][0] && d==k[r][1]) {
// 		    v=v.concat("\n");
// 		    v=v.concat(indent(ind+6)+"(r"+r+" (g (r"+r+"' (g (0.0)) (b (1.0))))"+"\n" +	     
// 			       indent(ind+9)+       " (b (r"+r+"' (g (0.0)) (b (1.0))))))"+"\n");
// 		}else {			
// 		    v=v.concat(" (SAMEr"+r+"))\n");
// 		}
// 		    //}		
// 	    }
// 	    v=v.concat(indent(ind+3) + "))\n");
// 	    //  v=v.concat(indent(ind) + ")");
// 	}
// 	v=v.concat(indent(ind) + ")\n");
// 	return v;
//     } // sampleRockState


    private String catchWumpusCost(int ind) {
	String v="";
	v=v.concat("(aj \n");	// aj
	for (int aj=0;aj<n;aj++) { 	
	    v=v.concat(indent(ind+3) + "(aj" + aj + " (ai\n");	// ai
	    for(int ai=0;ai<n;ai++) {	
		v=v.concat(indent(ind+11) + "(ai" + ai + " (wj\n"); // wj
		for(int wj=0;wj<n;wj++) {	
		    v=v.concat(indent(ind+20) + "(wj" + wj + " (wi"); // wi
		    for(int wi=0;wi<n;wi++) { 
			if (aj == wj && ai == wi)
			    v=v.concat(" (wi" + wi + " (-10))");
			else
			    v=v.concat(" (wi" + wi + " (0))  ");

		    } v=v.concat("))\n");	       // wi
		} v=v.concat(indent(ind+20) + "))\n"); // wj
	    } v=v.concat(indent(ind+11)+ "))\n");      // ai
	} v=v.concat(indent(ind+3) + ")\n");			       // aj
	return v;
    } // catchWumpusCost


    // // cost of the sample action
//     private String sampleRockCost(int ind) {
// 	String v="(j\n";
// 	//int c,d,r;
// 	boolean matched;
// 	for(int c=0; c<n; c++) {
// 	    v=v.concat(indent(ind+1)+"(j"+c+"\n");
// 	    v=v.concat(indent(ind+3) + "(i\n");
// 	    for(d=0; d<n; d++) {
// 		v=v.concat(indent(ind+4)+"(i"+d);
// 		// here we ask if any rock matches this agent position
// 		// only one can, since there are <= 1 rocks per square
// 		matched = false;
// 		for(r=0; r<nr; r++) {		
// 		    if (c==k[r][0] && d==k[r][1]) {
// 			matched = true;
// 			v=v.concat("\n");
// 			v=v.concat(indent(ind+6)+"(r"+r+
// 				   " (g ("+CATCH_GOOD_COST+")) (b ("+CATCH_BAD_PENALTY+"))))"+"\n"); 	     
// 			//            indent(ind+9)+       " (b (r"+r+"' (g (0.0)) (b (1.0))))))"+"\n");
// 			break;
// 		    }
// 		}
// 		// otherwise this action is illegal
// 		if(!matched) v=v.concat(" ("+ILLEGAL_PENALTY+"))\n");				
// 	    }
// 	    v=v.concat(indent(ind+3) + "))\n");
// 	    //  v=v.concat(indent(ind) + ")");
// 	}
// 	v=v.concat(indent(ind) + ")\n");
// 	return v;
//     } // sampleRockCost

    // // observation mode for check_r
//     private String checkRockObs(int ind, int r) {
// 	String v="(j'\n";
// 	int c,d;
// 	for(c=0; c<n+1; c++) {	    
// 	    v=v.concat(indent(ind+1)+"(j"+c+"\n");
// 	    // check for the end state
// 	    if(c==n) {
// 		v=v.concat(indent(ind+3)+"(o' (og (0.5)) (ob (0.5))))\n");
// 		break;
// 	    }
// 	    v=v.concat(indent(ind+3) + "(i'\n");
// 	    for(d=0; d<n; d++) {
// 		v=v.concat(indent(ind+4)+"(i"+d);
// 		//if (c==k[r][0] && d==k[r][1]) {
// 		v=v.concat("\n");
// 		// assign probs to og and ob according to distance to the rock
// 		v=v.concat(indent(ind+6)+"(r"+r+"' (g (o' (og (" +acc(c,d,r)+")) (ob (" +(1-acc(c,d,r))+"))))"+"\n" +	     
// 			   indent(ind+10)+        " (b (o' (og (" +(1-acc(c,d,r))+")) (ob ("+acc(c,d,r)+"))))))"+"\n");
// 		//}else {			
// 		//v=v.concat(" (SAMEr"+r+"))\n");
// 		//}
// 	    }
// 	    v=v.concat(indent(ind+3) + "))\n");
// 	}
// 	v=v.concat(indent(ind) + ")\n");
// 	return v;
//     } // checkRockObs
	
    // return acc for matching wumpus and 1-acc otherwise
    private String checkWumpusObs(int x, int y, int ind) {
	//int dx, dy;
	// center position of the obs square defined by range
	int cp = CHECK_RANGE / 2; // prob dont need the floor function
	int dx = x - cp;
	int dy = y - cp;
	int tpx, tpy;
	String v="";
	v=v.concat("(aj'\n");	// aj
	for (int aj=0;aj<n;aj++) { 	
	    v=v.concat(indent(ind+3) + "(aj" + aj + " (ai'\n"); // ai
	    for(int ai=0;ai<n;ai++) {	
		v=v.concat(indent(ind+11) + "(ai" + ai + " (wj'\n"); // wj
		for(int wj=0;wj<n;wj++) {	
		    v=v.concat(indent(ind+20) + "(wj" + wj + " (wi'"); // wi
		    for(int wi=0;wi<n;wi++) { 
			tpx = aj + dx;
			tpy = ai + dy;
			if (tpx == wj && tpy == wi)
			    v=v.concat(" (wi" + wi + " (o' (wp ("+acc(dx,dy)     + ") wa(" + (1-acc(dx,dy)) + "))))  ");
			else
			    v=v.concat(" (wi" + wi + " (o' (wp ("+(1-acc(dx,dy)) + ") wa(" + acc(dx,dy)     + "))))  ");

		    } v=v.concat("))\n");	       // wi
		} v=v.concat(indent(ind+20) + "))\n"); // wj
	    } v=v.concat(indent(ind+11)+ "))\n");      // ai
	} v=v.concat(indent(ind+3) + ")\n");			       // aj
	return v;
    } // checkWumpusObs


    // compute accuracy of sensor based on distance
    // when dist == d_o, eta == 1/2
    private double acc(int dx, int dy) {
	// compute euclidean distance
	double dist = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
	// compute efficiency
	double eta  = Math.exp(- dist / SENSOR_HALF_EFF_DISTANCE * Math.log(2.0));
	// compute accuracy of sensor
	double acc  = (1.0 + eta) / 2.0;
	return acc;
    } // acc

} // rocksampleGen