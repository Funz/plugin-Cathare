package org.funz.Cathare;

import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import java.util.LinkedList;
import java.util.List;
import org.math.array.DoubleArray;
import org.funz.Project;
import org.funz.ioplugin.*;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.SyntaxRules;
import org.funz.parameter.InputFile;
import org.funz.util.ParserUtils;
import org.funz.util.ASCII;
import org.funz.util.Disk;

public class CathareIOPlugin extends ExtendedIOPlugin {

    public static String[] DOC_LINKS = {"http://www-cathare.cea.fr/"};
    public static String INFORMATION = "Cathare plugin made by IRSN/Yann Richet\nLesser General Public License";

    private static final String TIME_ = "TIME_";
    private static final String HEIGHT_ = "Z_";

    public CathareIOPlugin() {
        variableStartSymbol = SyntaxRules.START_SYMBOL_DOLLAR;
        variableLimit = SyntaxRules.LIMIT_SYMBOL_PARENTHESIS;
        formulaStartSymbol = SyntaxRules.START_SYMBOL_AT;
        formulaLimit = SyntaxRules.LIMIT_SYMBOL_BRACKETS;
        commentLine = "*";
    }

    @Override
    public boolean acceptsDataSet(File f) {
        return f.isFile() && ParserUtils.contains(f, "REACTOR", true);
    }

    @Override
    public LinkedList<File> getRelatedFiles(File f) {
        LinkedList<File> list = new LinkedList<File>();
        list.add(f);

        File[] same_path = f.getParentFile().listFiles();
        for (File file : same_path) {
            if (file!= f && file.getName().contains(f.getName())) {
                list.add(file);
            }
        }

        return list;
    }

    LinkedList<String> out = new LinkedList<String>();
    HashMap<String, String> xout = new HashMap<String, String>();
    String _main_name="?";

    @Override
    public Map<String, Object> readOutput(File outdir) {
        Map<String, Object> lout = super.readOutput(outdir);//new HashMap<String, Object>();     

        File outfile = new File(outdir, "FORT07");
        if (!outfile.exists()) {
            outfile = outdir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name != null && name.startsWith("FORT");
                }
            })[0];
        }

        String content = ParserUtils.getASCIIFileContent(outfile);
        String[] evolutions = content.split("EVOLUTION ");
        for (String evol : evolutions) {
            if (evol.length() > 0) {
                String name = evol.substring(0, evol.indexOf(" "));
                String x = "TIME";
                String xname = "TIME_";
                if (evol.contains("\nZSW ")) {
                    x = "ZSW";
                    xname = "Z_";
                } else if (evol.contains("\nZS ")) {
                    x = "ZS";
                    xname = "Z_";
                }
                int x_beg = evol.indexOf("\n", evol.indexOf(x)) + 1;
                int x_end = evol.indexOf("     ", x_beg);
                int dat_beg = evol.indexOf("\n", evol.indexOf("\n", x_end) + 1) + 1;

                String[] times = evol.substring(x_beg, x_end).replace("\n", "").replace("  ", " ").trim().split(" ");
                double[] times_array = new double[times.length];
                for (int i = 0; i < times_array.length; i++) {
                    times_array[i] = Double.parseDouble(times[i]);
                }

                String[] dats = evol.substring(dat_beg).replace("\n", "").replace("  ", " ").trim().split(" ");
                double[] dats_array = new double[dats.length];
                for (int i = 0; i < dats_array.length; i++) {
                    dats_array[i] = Double.parseDouble(dats[i]);
                }

                lout.put(name, dats_array);
                lout.put(xname + name, times_array);
            }
        }
        return lout;
    }

    @Override
    public void setInputFiles(File... inputfiles) {
        _inputfiles = inputfiles;
        _output.clear();

        out = new LinkedList<String>();
        xout = new HashMap<String, String>();
        File postprofile = null;

        if (_inputfiles.length > 0) {

            _main_name = inputfiles[0].getName();
            if (_main_name.contains("."))
                _main_name = _main_name.substring(0,_main_name.indexOf('.'));
    
            for (File file : _inputfiles) {
                if (file != _inputfiles[0] && 
                file.getName().contains(_main_name) && 
                ParserUtils.contains(file,"READ RESULT 21;", true) ) {
                    postprofile = file;
                    break;
                }
            }

            if (postprofile != null) {
                String[] lines = ParserUtils.getAllLinesContaining(postprofile, " = ");

                for (String l : lines) {
                    if (!l.startsWith(commentLine)) {
                        String o = l.substring(0, l.indexOf("=")).trim();
                        out.add(o);
                        _output.put(o, DoubleArray.fill(10, 0.1));

                        String x = l.substring(l.indexOf("=") + 1).trim();
                        x = x.substring(0, x.indexOf(" ")).trim();
                        //System.err.println(x);
                        if (x.equals("CHRONO")) {
                            xout.put(o, TIME_);
                            //out.add(TIME_ + o);
                            _output.put(TIME_ + o, DoubleArray.fill(10, 0.1));
                        } else if (x.equals("PHOTO")) {
                            xout.put(o, HEIGHT_);
                            //out.add(HEIGHT_ + o);
                            _output.put(HEIGHT_ + o, DoubleArray.fill(10, 0.1));
                        } else {
                            x = xout.get(x);
                            xout.put(o, x);
                            //out.add(x + o);
                            _output.put(x + o, DoubleArray.fill(10, 0.1));
                        }
                    }
                }
            }
        }
    }

    @Override
    public LinkedList<OutputFunctionExpression> suggestOutputFunctions() {
        LinkedList<OutputFunctionExpression> s = new LinkedList<OutputFunctionExpression>();
        for (String o : out) {
            s.add(new OutputFunctionExpression.Numeric2DArray(xout.get(o) + o, o));
        }
        return s;
    }

    @Override
    public InputFile[] importFileOrDir(File src) throws Exception {
        InputFile[] ifs = super.importFileOrDir(src);
        if (src.getName().endsWith("RESTART")) {
            ifs[0].hasParameters = false;
        }
        return ifs;
    }
}
