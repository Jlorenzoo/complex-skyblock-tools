import com.skyblocktoolkit.ToolkitLayout;
public final class ToolkitLayoutTest {
 public static void main(String[] args){
  for(int w:new int[]{240,320,426,640,960})for(int h:new int[]{180,240,360,540}){
   check(ToolkitLayout.of(w,h),w,h);check(ToolkitLayout.hub(w,h),w,h);check(ToolkitLayout.popup(w,h),w,h);
  }
  for(String query:new String[]{"sell all","SELL ALL","sellall","  sell   all  ","farming sell all"}){
   var results=com.skyblocktoolkit.ToolkitSearch.find(query);
   if(results.size()!=1||!results.getFirst().setting().equals("Sell all key")||results.getFirst().page()!=10)
    throw new AssertionError("Expected one canonical sell-all setting: "+query);
  }
  var minimum=com.skyblocktoolkit.ToolkitSearch.find("  MiNiMuM   boost ");
  if(minimum.size()!=1||!minimum.getFirst().setting().equals("Minimum boost (%)"))throw new AssertionError("Individual setting terms");
  if(!com.skyblocktoolkit.ToolkitSearch.find("coconut history").isEmpty())throw new AssertionError("Unrelated controls must not combine keywords");
  if(!com.skyblocktoolkit.ToolkitSearch.find("no-such-setting").isEmpty())throw new AssertionError("No matches");
  System.out.println("PASS: global setting search, canonical sell-all result, responsive bounds, fixed footer and scroll limits");
 }
 static void check(ToolkitLayout l,int w,int h){
   if(l.x()<0||l.y()<0||l.x()+l.width()>w||l.y()+l.height()>h)throw new AssertionError("frame bounds");
   if(l.bodyX()<l.x()||l.bodyX()+l.bodyWidth()>l.x()+l.width())throw new AssertionError("body bounds");
   if(l.bodyY()+l.viewport()>l.footerY()-10)throw new AssertionError("footer overlap");
   for(int content:new int[]{100,260,400}){
    int max=l.maxScroll(content);
    if(max<0||l.fits(-1,22,0))throw new AssertionError("scroll clipping");
    if(content>l.viewport()&&content-max!=l.viewport())throw new AssertionError("scroll limit");
   }
 }
}
