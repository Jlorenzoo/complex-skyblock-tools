package com.moneybooster;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reconciles a short settlement window so chat and scoreboard packet order do not matter. */
public final class MoneyLedger {
    private static final Pattern SHOP = Pattern.compile(
        "^\\(CS\\) .+? (?:bought|sold) .+? for ([+-])\\$([0-9,]+(?:\\.[0-9]+)?) \\(\\$([0-9,]+(?:\\.[0-9]+)?) → \\$([0-9,]+(?:\\.[0-9]+)?)\\)$");
    private static final String MONEY="([0-9,]+(?:\\.[0-9]+)?)";
    private static final Pattern AUCTION=Pattern.compile("^\\(/ah\\) .+ purchased your listing of .+ for:\\s*\\$"+MONEY+"\\.$");
    private static final Pattern FEE=Pattern.compile("^You paid a listing fee of \\$"+MONEY+" \\(\\d+(?:\\.\\d+)?%\\)$");
    private static final Pattern PAY_IN=Pattern.compile("^\\(/pay\\) .+ has sent you: \\$"+MONEY+" \\(\\$"+MONEY+" [➜→] \\$"+MONEY+"\\)$");
    private static final Pattern PAY_OUT=Pattern.compile("^\\(/pay\\) (?:[✔✓]\\s*)?You paid .+?: \\$"+MONEY+" \\(\\$"+MONEY+" [➜→] \\$"+MONEY+"\\)(?: for 'money'\\.)?$");
    public record Receipt(double earned, int percentage, int testId, long boosterId, long fromMs, long toMs) {}
    public record Shop(double delta, double before, double after) {}
    private static final class Window {
        double baseline, balance, external;
        long baselineAt;
        long opened = -1, lastShop = -1, sampled = -1, closed = -1;
        boolean shopReflected;
        final int percentage, testId;
        final long boosterId;
        Window(double baseline, int pct, int id, long boosterId) { this.boosterId = boosterId; this.baseline = this.balance = baseline; percentage = pct; testId = id; }
        void open(long now) { if (opened < 0) opened = now; }
        boolean contains(Shop shop) {
            double lo = Math.min(baseline, balance) - 0.02, hi = Math.max(baseline, balance) + 0.02;
            return shop.before() >= lo && shop.before() <= hi && shop.after() >= lo && shop.after() <= hi;
        }
    }
    private final java.util.ArrayList<Window> windows = new java.util.ArrayList<>();
    private Window current;
    private final Consumer<Receipt> sink;
    public MoneyLedger(Consumer<Receipt> sink) { this.sink = sink; }

    public static double shopDelta(String text) {
        Shop shop = parseTransaction(text); return shop == null ? Double.NaN : shop.delta();
    }
    public static Shop parseTransaction(String text) {
        Shop shop=parseShop(text); if(shop!=null)return shop;
        if(text==null)return null; text=text.replaceAll("§.","").strip();
        try {
            Matcher m=AUCTION.matcher(text);
            if(m.matches())return amountOnly(m.group(1),1);
            m=FEE.matcher(text);if(m.matches())return amountOnly(m.group(1),-1);
            m=PAY_IN.matcher(text);if(m.matches())return payment(m,1);
            m=PAY_OUT.matcher(text);if(m.matches())return payment(m,-1);
        }catch(NumberFormatException ignored){}
        return null;
    }
    private static Shop amountOnly(String value,int sign){double amount=Double.parseDouble(value.replace(",",""))*sign;return Double.isFinite(amount)?new Shop(amount,Double.NaN,Double.NaN):null;}
    private static Shop payment(Matcher m,int sign){
        double delta=Double.parseDouble(m.group(1).replace(",",""))*sign,before=Double.parseDouble(m.group(2).replace(",","")),after=Double.parseDouble(m.group(3).replace(",",""));
        return Double.isFinite(delta)&&Double.isFinite(before)&&Double.isFinite(after)&&Math.abs(after-before-delta)<=1.0?new Shop(delta,before,after):null;
    }
    public static Shop parseShop(String text) {
        if (text == null) return null;
        Matcher m = SHOP.matcher(text.replaceAll("§.", "").strip());
        if (!m.matches()) return null;
        try {
            double amount = Double.parseDouble(m.group(2).replace(",", ""));
            double before = Double.parseDouble(m.group(3).replace(",", ""));
            double after = Double.parseDouble(m.group(4).replace(",", ""));
            double delta = m.group(1).equals("-") ? -amount : amount;
            if (!Double.isFinite(delta) || !Double.isFinite(before) || !Double.isFinite(after) || Math.abs(after - before - delta) > 0.02) return null;
            return new Shop(delta, before, after);
        } catch (NumberFormatException e) { return null; }
    }
    private void context(long now, int pct, int id, long boosterId) {
        if (current == null || (current.percentage == pct && current.testId == id && current.boosterId == boosterId)) return;
        current.closed = now;
        current = new Window(current.balance, pct, id, boosterId); current.baselineAt=now; windows.add(current);
    }
    public void observe(double value, long now, int pct, int id) { observe(value, now, pct, id, -1); }
    public void observe(double value, long now, int pct, int id, long boosterId) {
        if (!Double.isFinite(value) || value < 0) return;
        if (current == null) { current = new Window(value, pct, id, boosterId); current.baselineAt=now; windows.add(current); }
        // Finish a known transaction across a state change before freezing the old window.
        if (current.external != 0 && !current.shopReflected &&
            (current.percentage != pct || current.testId != id || current.boosterId != boosterId)) {
            current.sampled = now;
            if (value == current.balance && now - current.lastShop < 1500) return;
            current.balance = value; current.shopReflected = true;
        }
        context(now, pct, id, boosterId);
        if (value != current.balance) { if(current.opened<0)current.baselineAt=current.sampled>=0?current.sampled:now; current.open(now); current.balance = value; }
        current.sampled = now;
    }
    public void shop(Shop shop, long now, int pct, int id) { shop(shop, now, pct, id, -1); }
    public void shop(Shop shop, long now, int pct, int id, long boosterId) {
        if (current == null || shop == null) return;
        // Prefer the window whose observed balance range contains the exact transaction.
        // This also lets delayed chat correct a just-finished test window.
        Window target = null;
        // A transaction starting beyond the frozen boundary belongs to the new window.
        // Otherwise a large auction jump can make later purchases fit an older range.
        boolean changed=current.percentage!=pct||current.testId!=id||current.boosterId!=boosterId;
        double boundary=changed?current.balance:current.baseline;
        if(Double.isFinite(shop.before()) && ((shop.delta()>0 && shop.before()>=boundary-0.02) || (shop.delta()<0 && shop.before()<=boundary+0.02))){
            if(changed)context(now,pct,id,boosterId);
            target=current;
        }
        for (int i = windows.size() - 1; target==null && i >= 0; i--) {
            Window w = windows.get(i);
            if (w.opened >= 0 && (w.closed<0 || now-w.closed<=2000) && w.contains(shop)) { target = w; break; }
        }
        if (target == null) { context(now, pct, id, boosterId); target = current; }
        target.shopReflected = target.contains(shop);
        target.open(now); target.external += shop.delta(); target.lastShop = now;
    }
    public void tick(long now) {
        var iterator = windows.iterator();
        while (iterator.hasNext()) {
            Window w = iterator.next();
            if (w.opened < 0) { if (w != current) iterator.remove(); continue; }
            if (now - w.opened < 2000 || (w.lastShop >= 0 && now - w.lastShop < 1500)) continue;
            if (w == current && (w.sampled < w.opened || (w.lastShop >= 0 && w.sampled < w.lastShop + 500))) continue;
            double earned = w.balance - w.baseline - w.external;
            if (Double.isFinite(earned) && earned > 0) sink.accept(new Receipt(earned, w.percentage, w.testId, w.boosterId, w.baselineAt, w.closed >= 0 ? w.closed : w.sampled));
            if (w != current) iterator.remove();
            else { w.baseline = w.balance; w.baselineAt=w.sampled; w.external = 0; w.opened = w.lastShop = -1; }
        }
    }
    public boolean pendingForTest(int id) {
        for (Window w : windows) if (w.opened >= 0 && w.testId == id) return true;
        return false;
    }
    public void reset() { current = null; windows.clear(); }
}
