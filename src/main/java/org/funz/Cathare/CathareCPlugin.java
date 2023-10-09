package org.funz.Cathare;

import java.io.File;
import java.io.FileFilter;
import java.util.Properties;
import org.funz.calculator.plugin.CodeLauncher;
import org.funz.calculator.plugin.DataChannel;
import org.funz.calculator.plugin.DefaultCalculatorPlugin;
import org.funz.calculator.plugin.DefaultCodeLauncher;
import org.funz.calculator.plugin.OutputReader;
import org.funz.util.ParserUtils;

public class CathareCPlugin extends DefaultCalculatorPlugin {


    public CodeLauncher createCodeLauncher(Properties variables, DataChannel channel) {
        super.createCodeLauncher(variables, channel);
        return new CathareLauncher(this);
    }

    private class CathareLauncher extends DefaultCodeLauncher {

       CathareLauncher(CathareCPlugin plugin) {
            super(plugin);
            _progressSender = new CathareOutReader(this);
        }

        private class CathareOutReader extends OutputReader {

            public CathareOutReader(CathareLauncher l) {
                super(l);
                _information = "?";
            }

            public void run() {
                if (getDataChannel() == null) {
                    return;
                }

                while (!_stopMe) {
                    synchronized (this) {
                        try {
                            wait(1000);
                        } catch (Exception e) {
                        }
                    }

                    File[] outfiles = _dir.listFiles(new FileFilter() {

                        public boolean accept(File pathname) {
                            return pathname.getName().equals("out.txt");
                        }
                    });

                    if (outfiles == null || outfiles.length < 1) {
                        _information = "?";
                    } else {

                        File outfile = outfiles[0];

                            _information = ParserUtils.getLastFullLine(outfile);
                        
                    }

                    if (_information != null && _information.length() > 0) {
                        if (!getDataChannel().sendInfomationLineToConsole(_information)) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
