/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaforkeycreator;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author holger
 */
public class CopyKeyCompatibleListenerTest {

    public CopyKeyCompatibleListenerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of generateKeyCompatible method, of class CopyKeyCompatibleListener.
     */
    @Test
    public void testGenerateKeyCompatible() throws IOException {
        final String classCode = "package jzip;\n"
                + "\n"
                + "import java.util.ArrayList;\n"
                + "import java.util.Iterator;\n"
                + "import java.util.LinkedList;\n"
                + "import java.util.List;\n"
                + "import java.util.Properties;\n"
                + "import java.util.zip.ZipEntry;\n"
                + "import java.util.zip.ZipInputStream;\n"
                + "import java.util.zip.ZipOutputStream;\n"
                + "\n"
                + "import org.apache.commons.cli.CommandLine;\n"
                + "import org.apache.commons.cli.CommandLineParser;\n"
                + "import org.apache.commons.cli.GnuParser;\n"
                + "import org.apache.commons.cli.HelpFormatter;\n"
                + "import org.apache.commons.cli.Option;\n"
                + "import org.apache.commons.cli.OptionBuilder;\n"
                + "import org.apache.commons.cli.Options;\n"
                + "import org.apache.commons.cli.ParseException;\n"
                + "\n"
                + "public class JZip {\n"
                + "	List<String> fileList;\n"
                + "\n"
                + "	private Properties CONFIGURATION = null;\n"
                + "	/**\n"
                + "	 * Configuration ist im Zip Folder zipIt wirte aufruf 268 zos.write senke\n"
                + "	 * parseAndRun Methode Configuration.getProperty als low annotieren\n"
                + "	 * \n"
                + "	 * Bisher configuration.load is low\n"
                + "	 * \n"
                + "	 * CONFIGURATION is low, CONFIGURATION.get Main-> start() -> parseAndRun()\n"
                + "	 * -> SourceFolder --> UnZipIt() --> fos.write() Es exisitert ein Path von\n"
                + "	 * CONFIGURATION zu fos.write False Positive: ONFIGURATION sagt nur wohin\n"
                + "	 * das file gelegt wird und nicht was in dem File steht. Kann man das ohne\n"
                + "	 * declassification �berhaupt sagen? Kann KeY mit den ganzen Streams und\n"
                + "	 * Buffern umgehen?\n"
                + "	 * \n"
                + "	 * **/\n"
                + "\n"
                + "	private CommandLineParser cmdParser;\n"
                + "	private Options opt;\n"
                + "	private boolean run;\n"
                + "	private String[] args;\n"
                + "	private String commandline = \"\";\n"
                + "\n"
                + "	public JZip(String[] args) {\n"
                + "		this.args = args;\n"
                + "		if (args != null && args.length > 0) {\n"
                + "			StringBuilder sb = new StringBuilder();\n"
                + "			for (String s : args) {\n"
                + "				sb.append(s);\n"
                + "			}\n"
                + "			this.commandline = sb.toString();\n"
                + "		}\n"
                + "	}\n"
                + "\n"
                + "	public JZip() {\n"
                + "	}\n"
                + "\n"
                + "	public static void main(String[] args) {\n"
                + "\n"
                + "		JZip zipper = new JZip();\n"
                + "		zipper.start();\n"
                + "	}\n"
                + "\n"
                + "	private void init() {\n"
                + "		this.run = true;\n"
                + "\n"
                + "		this.opt = new Options();\n"
                + "		Option help = new Option(\"help\", \"Print help\");\n"
                + "		try {\n"
                + "			opt.addOption(help);\n"
                + "		} catch (Throwable thb) {\n"
                + "		}\n"
                + "\n"
                + "		Option exit = new Option(\"exit\", \"Exit JZip\");\n"
                + "		try {\n"
                + "			opt.addOption(exit);\n"
                + "		} catch (Throwable thb) {\n"
                + "		}\n"
                + "\n"
                + "		Option loadconfig = OptionBuilder\n"
                + "				.withArgName(\"file\")\n"
                + "				.hasArg()\n"
                + "				.withDescription(\n"
                + "						\"Load the configuration file <file>. All existing configuration properties will be overwritten afterwards.\")\n"
                + "				.create(\"loadconfig\");\n"
                + "		try {\n"
                + "			opt.addOption(loadconfig);\n"
                + "		} catch (Throwable thb) {\n"
                + "		}\n"
                + "		Option storeconfig = OptionBuilder\n"
                + "				.withArgName(\"file\")\n"
                + "				.hasArg()\n"
                + "				.withDescription(\n"
                + "						\"Store the current configuration in file <file>\")\n"
                + "				.create(\"storeconfig\");\n"
                + "		try {\n"
                + "			opt.addOption(storeconfig);\n"
                + "		} catch (Throwable thb) {\n"
                + "		}\n"
                + "		Option setconfig = OptionBuilder\n"
                + "				.withArgName(\"name=value\")\n"
                + "				.hasOptionalArgs(2)\n"
                + "				.withValueSeparator('=')\n"
                + "				.withDescription(\n"
                + "						\"Assign the value <value> to configuration property <name>\")\n"
                + "				.create(\"setconfig\");\n"
                + "		try {\n"
                + "			opt.addOption(setconfig);\n"
                + "		} catch (Throwable thb) {\n"
                + "		}\n"
                + "		Option showConfig = new Option(\"showconfig\", \"List all properties\");\n"
                + "		try {\n"
                + "			opt.addOption(showConfig);\n"
                + "		} catch (Throwable thb) {\n"
                + "		}\n"
                + "		Option zip = OptionBuilder\n"
                + "				.withArgName(\"file> <source\")\n"
                + "				.hasOptionalArgs(2)\n"
                + "				.withDescription(\n"
                + "						\"Zip all files from directory <source> to archive file <file>. If <source> is not declared then this property is read from the configuration file. <source>'s default is '.'\")\n"
                + "				.create(\"zip\");\n"
                + "		try {\n"
                + "			opt.addOption(zip);\n"
                + "		} catch (Throwable thb) {\n"
                + "		}\n"
                + "		Option unzip = OptionBuilder\n"
                + "				.withArgName(\"file> <destination\")\n"
                + "				.hasOptionalArgs(2)\n"
                + "				.withDescription(\n"
                + "						\"Unzip all files from zip <file> into destination folder <destination>. If <destination> is not declared then this propery is read from the configuration file. <destination>'s default is '.'\")\n"
                + "				.create(\"unzip\");\n"
                + "		try {\n"
                + "			opt.addOption(unzip);\n"
                + "		} catch (Throwable thb) {\n"
                + "		}\n"
                + "		this.cmdParser = new GnuParser();\n"
                + "	}\n"
                + "\n"            
                + "}";
        CopyKeyCompatibleListener l = new CopyKeyCompatibleListener();
        
        System.out.println(l.generateKeyCompatible(classCode));
    }

}
