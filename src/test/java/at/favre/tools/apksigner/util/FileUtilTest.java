package at.favre.tools.apksigner.util;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FileUtilTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File file;

    @Before
    public void setUp() throws Exception {
        file = temporaryFolder.newFile();
    }

    @Test
    public void createChecksum() throws Exception {
        String s = FileUtil.createChecksum(file, "SHA-256");
        assertNotNull(s);
        assertTrue(s.length() % 2 == 0);
        assertTrue(s.length() > 0);
    }

}