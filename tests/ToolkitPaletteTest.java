import com.skyblocktoolkit.ToolkitPalette;

public final class ToolkitPaletteTest {
    public static void main(String[] args) {
        for (var palette : new ToolkitPalette[]{ToolkitPalette.DARK, ToolkitPalette.LIGHT}) {
            for (int background : new int[]{palette.panel(),palette.sidebar(),palette.surface(),palette.hover()}) {
                contrast(palette.text(),background,"primary text");
                contrast(palette.muted(),background,"secondary text");
            }
            contrast(palette.accent(),palette.accentSurface(),"selected navigation");
            contrast(palette.error(),palette.surface(),"invalid price");
            contrast(palette.moneyText(),palette.panel(),"money status");
            contrast(palette.xpText(),palette.panel(),"XP status");
        }
        if (ToolkitPalette.of(true)!=ToolkitPalette.LIGHT||ToolkitPalette.of(false)!=ToolkitPalette.DARK)throw new AssertionError("theme selection");
        System.out.println("PASS: readable primary/secondary text, selected navigation, errors and timer status in light and dark palettes (contrast >= 4.5:1)");
    }
    static double luminance(int color){
        double result=0;double[] weights={.2126,.7152,.0722};
        for(int i=0;i<3;i++){double c=((color>>(16-8*i))&255)/255.0;result+=weights[i]*(c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4));}
        return result;
    }
    static void contrast(int foreground,int background,String label){
        double a=luminance(foreground),b=luminance(background),ratio=(Math.max(a,b)+.05)/(Math.min(a,b)+.05);
        if(ratio<4.5)throw new AssertionError(label+" contrast: "+ratio);
    }
}
