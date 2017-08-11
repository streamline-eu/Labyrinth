package gg.operators;

import org.apache.flink.api.java.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Join extends BagOperator<Tuple2<Integer,Integer>, Tuple2<Integer,Integer>> {

    private static final Logger LOG = LoggerFactory.getLogger(Join.class);

    private HashMap<Integer, ArrayList<Tuple2<Integer,Integer>>> ht;
    private ArrayList<Tuple2<Integer, Integer>> probeBuffered;
    private boolean buildDone;
    private boolean probeDone;

    @Override
    public void openOutBag() {
        super.openOutBag();
        ht = new HashMap<>();
        probeBuffered = new ArrayList<>();
        buildDone = false;
        probeDone = false;
    }

    @Override
    public void pushInElement(Tuple2<Integer, Integer> e, int logicalInputId) {
        super.pushInElement(e, logicalInputId);
        if (logicalInputId == 0) { // build side
            assert !buildDone;
            ArrayList<Tuple2<Integer,Integer>> l = ht.get(e.f0);
            if (l == null) {
                l = new ArrayList<>();
                l.add(e);
                ht.put(e.f0,l);
            } else {
                l.add(e);
            }
        } else { // probe side
            if (!buildDone) {
                probeBuffered.add(e);
            } else {
                probe(e);
            }
        }
    }

    @Override
    public void closeInBag(int inputId) {
        super.closeInBag(inputId);
        if (inputId == 0) { // build side
            assert !buildDone;
            LOG.info("Build side finished");
            buildDone = true;
            for (Tuple2<Integer, Integer> e: probeBuffered) {
                probe(e);
            }
            if (probeDone) {
                out.closeBag();
            }
        } else { // probe side
            assert inputId == 1;
            assert !probeDone;
            LOG.info("Probe side finished");
            probeDone = true;
            if (buildDone) {
                out.closeBag();
            }
        }
    }

    private void probe(Tuple2<Integer, Integer> e) {
        ArrayList<Tuple2<Integer, Integer>> l = ht.get(e.f0);
        if (l != null) {
            for (Tuple2<Integer, Integer> b: l) {
                udf(b, e);
            }
        }
    }

    protected abstract void udf(Tuple2<Integer,Integer> a, Tuple2<Integer,Integer> b); // Uses out
}