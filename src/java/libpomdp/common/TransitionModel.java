package libpomdp.common;


public abstract class TransitionModel {
	
	public abstract AlphaVector project(AlphaVector alpha,int a);
	public abstract CustomMatrix getMatrix(int a);
	public abstract int sampleNextState(int state, int action);
	
}
