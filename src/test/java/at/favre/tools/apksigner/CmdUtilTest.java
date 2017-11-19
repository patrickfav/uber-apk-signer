package at.favre.tools.apksigner;

import at.favre.tools.apksigner.util.CmdUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CmdUtilTest {

    @Test
    public void testCanRunCommand() throws Exception {
        assertFalse("should not be able to run random", CmdUtil.canRunCmd(new String[]{"Thisadhpiwadahdjsahduhduwaheuawez27371236"}));
        assertTrue("should be able to run cmd 'java -version'", CmdUtil.canRunCmd(new String[]{"java", "-version"}));
    }
}
