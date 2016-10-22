package at.favre.tools.apksigner.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PackageMatcherTest {

    @Test
    public void testMatches() throws Exception {
        assertTrue(PackageMatcher.match("com.*","com.example.android.livecubes"));
        assertFalse(PackageMatcher.match("com.*","de.example.android.livecubes"));
        assertTrue(PackageMatcher.match("com.*.android.*","com.example.android.livecubes"));
        assertFalse(PackageMatcher.match("com.*.android","com.example.android.livecubes"));
        assertTrue(PackageMatcher.match("com.*.android","com.example.android"));
        assertTrue(PackageMatcher.match("com.example.android.*","com.example.android.livecubes"));
        assertTrue(PackageMatcher.match("com.example.android*","com.example.android.livecubes"));
        assertTrue(PackageMatcher.match("com.example.android*","com.example.android"));
        assertFalse(PackageMatcher.match("com.example.android.*","com.example.android"));
        assertFalse(PackageMatcher.match("com.example.android.*","com.example.android"));
    }

    @Test
    public void testParseFilterArg() throws Exception {
        String[] filters1 = PackageMatcher.parseFiltersArg("com.android.*");
        assertTrue(filters1.length == 1);
        assertEquals("com.android.*",filters1[0]);

        String[] filters2 = PackageMatcher.parseFiltersArg("com.android.*,at.test.*");
        assertTrue(filters2.length == 2);
        assertEquals("com.android.*",filters2[0]);
        assertEquals("at.test.*",filters2[1]);
    }
}
