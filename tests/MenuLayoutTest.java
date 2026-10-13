package com.moneybooster;
public class MenuLayoutTest {
 public static void main(String[] args){
  for(int h:new int[]{128,144,160,180,240,360,480})for(int w:new int[]{304,360,640})for(int preferred:new int[]{240,260}){
   var p=MenuLayout.of(w,h,preferred);
   if(p.x()<0||p.y()<0||p.x()+p.width()>w||p.y()+p.height()>h||p.footerY()+20>h||p.footerY()+20>p.y()+p.height())throw new AssertionError("Clipped footer at "+w+"x"+h);
   int count=Math.max(1,Math.min(4,(p.footerY()-p.y()-76)/27));
   if(p.y()+58+(count-1)*27+25>p.footerY())throw new AssertionError("History rows collide with footer");
  }
  System.out.println("PASS: panel/footer/history-row bounds across 42 screen size/layout combinations");
 }
}
