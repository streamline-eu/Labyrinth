package gg.partitioners;

public class Forward<T> extends Partitioner<T> {

	public Forward(int targetPara) {
		super(targetPara);
	}

	@Override
	public short getPart(T e) {
		return 0;
	}
}
