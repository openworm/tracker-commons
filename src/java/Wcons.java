package org.openworm.trackercommons.javawcon;

import java.io.*;
import org.openworm.trackercommons.compatibility.*;

public class Wcons {
    public static Wcon read(File f) throws IOException {
        scala.util.Either< String, org.openworm.trackercommons.DataSet > result =
            org.openworm.trackercommons.ReadWrite.read(f);
        if (result.isRight()) return Wcon.from(result.right().get());
        else throw new IOException(result.left().get());
    }

    public static void write(File f, Wcon w, Boolean asZip) throws IOException {
        String fname =
            w.myFile().isEmpty() ?
            (
                (asZip && f.getName().toLowerCase().endsWith(".zip")) ?
                f.getName().substring(0, f.getName().length() - 4) :
                f.getName()
            ) :
            w.myFile();
        scala.util.Either< String, scala.runtime.BoxedUnit > result =
            (asZip) ?
            org.openworm.trackercommons.ReadWrite.writeZip(w.toUnderlying(), f, fname) :
            org.openworm.trackercommons.ReadWrite.write(w.toUnderlying(), f, fname);
        if (result.isLeft()) throw new IOException(result.left().get());
    }

    private static File anotherFileOrNull(File wfile, String me, String list[]) {
        if (list.length == 0) return null;
        String p = wfile.getPath();
        String ext = p.substring(Math.max(p.length() - 5, 0)).toLowerCase();
        if (
            !(p.endsWith(me)) &&
            !(ext.equalsIgnoreCase(".wcon") && p.substring(0, p.length()-ext.length()).endsWith(me))
        ) throw new IllegalArgumentException("Mismatch between actual and specified file name");
        if (p.endsWith(me)) {
            return new File(p.substring(0, p.length() - me.length()) + list[0]);
        }
        else {
            return new File(p.substring(0, p.length() - me.length() - ext.length()) + list[0] + ext);
        } 
    }

    public static File nextFileOrNull(File wfile, Wcon w) { return anotherFileOrNull(wfile, w.myFile(), w.nextFiles()); }

    public static File previousFileOrNull(File wfile, Wcon w) { return anotherFileOrNull(wfile, w.myFile(), w.previousFiles()); }
}
