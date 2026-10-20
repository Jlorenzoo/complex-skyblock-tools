package com.moneybooster;

public final class XpEarningsTest {
    public static void main(String[] args) {
        check(XpEarnings.points(0,0)==0,"zero XP");
        check(XpEarnings.points(16,0)==352,"first curve boundary");
        check(XpEarnings.points(17,0)==394,"second curve");
        check(XpEarnings.points(31,0)==1507,"second boundary");
        check(XpEarnings.points(32,0)==1628,"third curve");
        for (int level=0;level<1000;level++) {
            check(XpEarnings.points(level,1)==XpEarnings.points(level+1,0),"level-up continuity at "+level);
        }
        check(XpEarnings.points(-1,0)==-1&&XpEarnings.points(0,Float.NaN)==-1,"invalid bar ignored");
        var xp=new XpEarnings();
        check(xp.observe(16,0,150,1000)==null,"joining establishes baseline");
        var gain=xp.observe(17,0,150,2000);
        close(gain.total(),42,"level-up XP is points, not levels");close(gain.base(),16.8,"+150% removed by dividing by 2.5");
        check(xp.observe(17,0,150,3000)==null,"unchanged XP is not income");
        check(xp.observe(0,0,0,4000)==null,"death or spending rebases");
        close(xp.observe(1,0,0,5000).base(),7,"new gains after a loss");
        xp.reset();check(xp.observe(50,0,0,6000)==null,"reconnect does not invent XP");
        var measurement=new XpRateMeasurement();measurement.start(1000,60);
        measurement.add(new XpEarnings.Gain(50,20,500,1500)); // half before start
        measurement.add(new XpEarnings.Gain(75,75,1500,2000)); // booster expired
        close(measurement.earned(true),100,"actual XP across changing boosters");
        close(measurement.earned(false),85,"base XP normalized per gain");
        measurement.add(new XpEarnings.Gain(100,40,60500,61500)); // half after deadline
        measurement.stop(62000);
        close(measurement.perHour(100000,true),9000,"fixed duration excludes late tick overshoot");
        close(measurement.perHour(100000,false),6300,"completed base rate stable");
        measurement.add(new XpEarnings.Gain(500,500,62000,63000));
        close(measurement.earned(true),150,"post-stop XP excluded");
        measurement.start(100000,0);measurement.add(new XpEarnings.Gain(100,40,100000,102000));measurement.stop(104000);
        close(measurement.perHour(200000,true),90000,"free run uses elapsed seconds");
        measurement.reset();close(measurement.perHour(200000,true),0,"disconnect clears test");
        System.out.println("PASS: XP level curves, gains/losses, booster normalization, timed/free samples, boundaries and reset");
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void close(double actual,double expected,String message){if(Math.abs(actual-expected)>.0001)throw new AssertionError(message+": "+actual);}
}
