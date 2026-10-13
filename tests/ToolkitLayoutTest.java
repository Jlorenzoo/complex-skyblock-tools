import com.skyblocktoolkit.ToolkitLayout;
public final class ToolkitLayoutTest {
 public static void main(String[] args){
  for(int w:new int[]{240,320,426,640,960})for(int h:new int[]{180,240,360,540}){
   var l=ToolkitLayout.of(w,h);
   if(l.x()<0||l.y()<0||l.x()+l.width()>w||l.y()+l.height()>h)throw new AssertionError("frame bounds");
   if(l.bodyX()<l.x()||l.bodyX()+l.bodyWidth()>l.x()+l.width())throw new AssertionError("body bounds");
   if(l.bodyY()+l.viewport()>l.footerY()-10)throw new AssertionError("footer overlap");
   for(int content:new int[]{100,260,400}){
    int max=l.maxScroll(content);
    if(max<0||l.fits(-1,22,0))throw new AssertionError("scroll clipping");
    if(content>l.viewport()&&content-max!=l.viewport())throw new AssertionError("scroll limit");
   }
  }
  System.out.println("PASS: responsive panel bounds, narrow navigation, fixed footer and scroll limits");
 }
}
