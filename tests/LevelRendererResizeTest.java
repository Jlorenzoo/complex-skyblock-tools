import com.skyblocktoolkit.LevelRendererResizeHelper;

public final class LevelRendererResizeTest {
    public static void main(String[] args) {
        // Zero or negative dimensions during minimize/unmap on Alt-Tab must be skipped
        check(LevelRendererResizeHelper.shouldSkipResize(0, 0, 1920, 1080), "0x0 dimensions should be skipped");
        check(LevelRendererResizeHelper.shouldSkipResize(0, 1080, 1920, 1080), "0 width should be skipped");
        check(LevelRendererResizeHelper.shouldSkipResize(1920, 0, 1920, 1080), "0 height should be skipped");
        check(LevelRendererResizeHelper.shouldSkipResize(-1, -1, 1920, 1080), "Negative dimensions should be skipped");

        // Identical dimensions (redundant focus/unfocus events) must be skipped
        check(LevelRendererResizeHelper.shouldSkipResize(1920, 1080, 1920, 1080), "Identical dimensions should be skipped");
        check(LevelRendererResizeHelper.shouldSkipResize(1134, 1528, 1134, 1528), "Identical tiled dimensions should be skipped");

        // Actual genuine window size changes must be processed
        check(!LevelRendererResizeHelper.shouldSkipResize(1920, 1080, -1, -1), "Initial resize must be processed");
        check(!LevelRendererResizeHelper.shouldSkipResize(1920, 1080, 1280, 720), "Resolution change must be processed");
        check(!LevelRendererResizeHelper.shouldSkipResize(1921, 1080, 1920, 1080), "Width change must be processed");
        check(!LevelRendererResizeHelper.shouldSkipResize(1920, 1081, 1920, 1080), "Height change must be processed");

        System.out.println("PASS: Alt-Tab and redundant resize events are safely skipped without invalidating occlusion");
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
