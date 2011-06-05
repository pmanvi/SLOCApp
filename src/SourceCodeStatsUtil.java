import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Utility providing the statistics for for given directory containing src code
 *
 * @author Praveena.Manvi
 */
public class SourceCodeStatsUtil {

    private static SourceCodeStatsUtil instance = new SourceCodeStatsUtil();

    SourceCodeStatsUtil() {
    }

    private static String dirName = null;

    public static SourceCodeStatsUtil forSrcDirectory(final String dirName) {
        File f = new File(dirName);
        if (dirName == null || !f.isDirectory()) {
            throw new IllegalArgumentException("Input : " + dirName + " has to a valid File directory");
        }
        SourceCodeStatsUtil.dirName = dirName;
        return instance;
    }

    public ISrcStats getJavaStatsNeglectingComments() {
        LineCounter s = new LineCounter();
        s.endingWith(".java");
        s.count(new File(dirName));
        return s;
    }

    public ISrcStats getSrcStatsFor(String... vals) {
        LineCounter s = new LineCounter();
        s.endingWith(vals);
        s.totalLines = s.count(new File(dirName));
        return s;
    }

    public static class LineCounter implements ISrcStats {
        private Map<String, Integer> fileCountMap = new HashMap<String, Integer>();
        private int totalLines = 0;

        public ISrcStats fileListToCsv(Writer writer) {
            if (writer == null) {
                throw new IllegalArgumentException("Writer can't be null");
            }
            try {
                String s = "Sl #, Java File Name, Active Source Lines Of Code\n";
                int count = 0;
                for (Entry e : fileCountMap.entrySet()) {
                    s += (String.valueOf(++count) + "," + e.getKey() + "," + e.getValue() + "\n");
                }
                writer.append(s);
                writer.flush();
                writer.close();
                //System.out.println(" CSV ");
                return this;
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new UnknownError("Failed to write to writer");
            }
        }

        private Set<String> fileExtSet = new HashSet<String>() {
        };

        private int count(File file) {
            int count = 0;
            if (file.isDirectory()) {
                // Get the listing in this directory
                count = recurseDir(file, count);
            } else {
                // It is a file
                count = countLinesIn(file);
            }
            return count;
        }

        private boolean withComments = false;

        public ISrcStats withComments() {
            withComments = true;
            return this;
        }

        /**
         * Counts code excluding comments and blank lines in the given file
         *
         * @param file
         * @return int
         */
        public int countLinesIn(File file) {
            if (file.isDirectory() || !file.canRead()
                    || file.toString().endsWith(".jar")
                    || file.toString().endsWith(".class")
                    || file.toString().endsWith(".exe")) return 0;
            int count = 0;
            try {
                BufferedReader reader =
                        new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = null;
                do {
                    line = reader.readLine();
                    if (line != null && line.indexOf("*") == -1 && line.indexOf("//") == -1 && line.length() > 0) {
                        count++;
                    }
                } while (line != null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String packageName = "";
            try {
                packageName = packageName(file) + ".";
                if (!".".equals(packageName)) {
                    packages.add(packageName);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if(packageName.equals(".")) packageName="";

            fileCountMap.put(packageName + file.getName(), count);
            return count;
        }

        Set<String> packages = new TreeSet<String>();

        private int recurseDir(File file, int count) {
            File[] files = file.listFiles(new FileFilter() {

                public boolean accept(File file) {
                    for (String s : fileExtSet) {
                        if (file.getName().indexOf(s) != -1 || file.isDirectory()) {
                            return true;
                        }
                    }
                    if (file.getName().indexOf(".java") != -1 || file.isDirectory()) {
                        return true;
                    }

                    return false;
                }
            });
            for (int i = 0; i < files.length; i++) {
                count += count(files[i]);
            }
            //System.out.println("Package :"+file.getName()+" # Files "+java.util.Arrays.asList(files)+" Line Count "+ count);
            return count;
        }

        public ISrcStats endingWith(String... suffix) {
            this.fileExtSet.addAll(packages);
            return this;
        }

        public ISrcStats regenerateStats() {
            return this;
        }

        public int activeTotalLines() {
            int c = 0;
            for (Entry<String, Integer> e : this.fileCountMap.entrySet()) {
                c += e.getValue();
            }
            return c;
        }

        public int fileCount() {
            return this.fileCountMap.size();
        }

        public int packageCount() {
            return this.packages.size();
        }

        public String summary() {
            return "Total # Lines = " + this.totalLines + "\n" + "Total # of Files = " + fileCount() + "\n" + "Avg # Lines per file = " + (this.totalLines / fileCount()) + "\n" + "Total # of packages = " + packageCount();
        }

        public String toString() {
            return summary();
        }
    }

    public static void main(String[] args) throws Exception {
        if(true){
            swingMain(args);
            return;
        }
        args = new String[1];
        args[0] = "C:/QuestionBuilder";
        args[0] ="C:\\javaiq\\src";
        if (args.length < 1) {
            System.out.println("Usage SourceCodeStatsUtil <dir name containing source code>");
            System.exit(0);
        }
        File file = new File(args[0]);
        if (!file.isDirectory()) {
            System.out.println("'" + args[0] + "' is not a directory");
            System.out.println("Usage SourceCodeStatsUtil <dir name containing source code>");
            System.exit(0);
        }
        LineCounter lc = new LineCounter();
        long count = lc.count(file);
        System.out.println("COUNT :" + count);
        lc.fileListToCsv(new FileWriter("test.csv"));

        if (true) {
            return;
        }
        Properties props = new Properties();
        props.load(new java.io.FileInputStream(new File("packages.props")));

        Set<Entry<Object, Object>> s = props.entrySet();
        for (Entry e : s) {
            String srcDir = String.valueOf(e.getValue()).trim();
            lc = (LineCounter) SourceCodeStatsUtil.forSrcDirectory(srcDir).getJavaStatsNeglectingComments();
            count = lc.activeTotalLines();
            System.out.println("*******************");
            System.out.println("" + e.getKey() + "");
            System.out.println("*******************");
            System.out.println("Total # Lines = " + count);
            System.out.println("Total # of Files = " + lc.fileCount());
            System.out.println("Avg # Lines per file = " + (count / lc.fileCount()));
            System.out.println("Total # of packages = " + lc.packageCount() + "");
            //System.out.println("Packages "+lc.packages);

            System.out.println("Packages : " + lc.packages.toString().replace(".,", "\n"));
        }
    }

    public interface ISrcStats {
        ISrcStats fileListToCsv(Writer writer);

        int activeTotalLines();

        int fileCount();

        int packageCount();
    }

    private static String packageName(File file) throws Exception {
        LineNumberReader reader = new LineNumberReader(new FileReader(file));
        String str = reader.readLine();
        while (str != null) {
            if (str.startsWith("package")) {
                break;
            }
            str = reader.readLine();
        }
        reader.close();
        if (str == null) {
            return "";
        }
        String s[] = str.split(";");
        return s[0].replaceAll("package", "").trim();
    }


    public static void swingMain(String[] args) throws IOException {
          final JFrame frame = new JFrame("Source Lines of Code (SLOC) Counter");
          final JButton select = new JButton("Select a directory having java source code");
          final JTextArea area = new JTextArea();
          final JTable table = new JTable();
          final File tempFile = File.createTempFile("SrcStats",".csv");
          select.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  File file = getFile("Directory having java source code", false, frame, "");
                  if (file == null) return;
                  StringWriter writer = new StringWriter();
                  LineCounter lc = (LineCounter) SourceCodeStatsUtil.forSrcDirectory(file.getAbsolutePath()).getJavaStatsNeglectingComments();
                  int count = lc.activeTotalLines();
                  System.out.println("*******************");
                  if (count == 0) {
                      return;
                  }
                  StringBuilder builder = new StringBuilder();
                  builder.append("***********************************\n");
                  builder.append("**********  Summary *************\n");
                  builder.append("***********************************\n");
                  builder.append("Total # Lines = " + count);
                  builder.append("\nTotal # of Files = " + lc.fileCount());
                  builder.append("\nAvg # Lines per file = " + (count / lc.fileCount()));
                  builder.append("\nTotal # of packages = " + lc.packageCount() + "\n");
                  builder.append("***********************************\n");
                  lc.fileListToCsv(writer);
                  writer.flush();
                  area.setText(builder.toString());
                  try {
                      FileWriter fWriter = new FileWriter(tempFile);
                      fWriter.write(writer.toString());
                      fWriter.flush();
                      fWriter.close();
                      table.setModel(parse(tempFile));
                      table.validate();
                      frame.validate();
                      frame.repaint();
                      frame.pack();
                  } catch (Exception e1) {
                      frame.add(area);
                      e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                  }
              }
          });
          frame.add(select, BorderLayout.NORTH);
          frame.add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel jp = new JPanel();
        jp.setLayout(new GridLayout(1,2));
        jp.add(area);
        JButton jb = new JButton("Export to CSV");
        jb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                File file = getFile("Exporting to csv", true, frame, "csv");
                if (file != null) {
                    tempFile.renameTo(file);
                }

            }

        });
        jp.add(jb);
          frame.add(jp,BorderLayout.SOUTH);

                         frame.setSize(400,400);
          frame.setVisible(true);
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

     public static File getFile(String title, boolean b, JFrame frame, final String type) {

        JFileChooser   chooser = new JFileChooser();
        File dir = null;
        chooser.setDialogTitle(
                title);
        try {
          dir = (new File(".")).getCanonicalFile();
            if(b){
                chooser.showSaveDialog(frame);
                return chooser.getSelectedFile();
             } else{
                chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    public boolean accept(File f) {
                            return  f.isDirectory();
                        }
                        @Override
                        public String getDescription() {
                            return type;
                        }
                    });
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.showDialog(frame,"Java src directory");
            return chooser.getSelectedFile();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error :" + ex.getMessage());
        }

        return dir;
    }

    public static TableModel parse(File f) throws FileNotFoundException {
           Scanner lineScan = new Scanner(f);
           System.out.println(f.getAbsolutePath());
           Scanner s = new Scanner(lineScan.nextLine());
           s.useDelimiter(",");
          DefaultTableModel model = new DefaultTableModel();
           while (s.hasNext()) {
               String next=s.next();
               model.addColumn(next);
           }
           while (lineScan.hasNextLine()) {
               s = new Scanner(lineScan.nextLine());
               s.useDelimiter(",");
               int rowCounter = 0;
               String[] rows = new String[model.getColumnCount()];
               while (s.hasNext()) {
                   String next=s.next();
                   rows[rowCounter]=next;
                   rowCounter++;
                   if(rowCounter>=model.getColumnCount()) {
                       rowCounter=0;
                       model.addRow(rows);
                       rows = new String[rowCounter];
                   }
               }
           }
           return model;
       }

}



