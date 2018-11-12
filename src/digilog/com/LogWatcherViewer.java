package digilog.com;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogWatcherViewer {
    private JFrame frame = new JFrame("LogWatcher(Beta ver 0.8)");
    private final JTextArea textArea = new JTextArea(40, 60);
    private final JScrollPane jScrollBar;
    private JTextField jtfFile;
    private JTextField jtfRegexp;
    private JTextField jtfTailCnt;
    private String fileName;
    private JButton browseBtn;
    private JButton startBtn;
    private JButton stopBtn;
    private JButton saveBtn;
    public JComboBox<String> dataBaseBox;
    File file = null;
    BufferedReader logReader = null;
    Pattern pattern;
    String TailCntInit = "10";
    int TailCnt;
    long fileLength;
    long init;
    static boolean stopThread = true;
    Runnable r = new digilog.com.LogWatcherViewer.WatcherThread();
    Thread t;

    public LogWatcherViewer() {
        this.jScrollBar = new JScrollPane(this.textArea);
        this.browseBtn = new JButton("Select File");
        this.startBtn = new JButton("Start");
        this.stopBtn = new JButton("Stop");
        this.saveBtn = new JButton("Save");
        String[] dataBaseGroup = new String[]{"UTF-8", "EUC-KR", "MS949", "EUC_JP","MS932","ISO2022JP2"};
        this.dataBaseBox = new JComboBox(dataBaseGroup);
        this.browseBtn.addActionListener(new digilog.com.LogWatcherViewer.SelectLogFileListener());
        this.jtfFile = new JTextField(25);
        this.jtfFile.addActionListener(new digilog.com.LogWatcherViewer.SelectLogFileListener());
        this.startBtn.addActionListener(new digilog.com.LogWatcherViewer.StartShowLogListener());
        this.stopBtn.addActionListener(new digilog.com.LogWatcherViewer.StopShowLogListener());
        this.jtfRegexp = new JTextField(15);
        this.jtfTailCnt = new JTextField(2);
        JPanel panel = new JPanel();
        panel.add(new JLabel("Log File"));
        panel.add(this.jtfFile);
        panel.add(this.browseBtn);
        panel.add(this.dataBaseBox);
        panel.add(new JLabel("RegExp"));
        panel.add(this.jtfRegexp);
        panel.add(this.jtfTailCnt);
        panel.add(this.startBtn);
        panel.add(this.stopBtn);
        panel.add(this.saveBtn);
        DefaultCaret caret = (DefaultCaret) this.textArea.getCaret();
        caret.setUpdatePolicy(2);
        this.stopBtn.setEnabled(false);
        this.saveBtn.setEnabled(false);
        this.textArea.setEditable(true);
        this.jtfTailCnt.setText(this.TailCntInit);
        this.frame.add(panel, "North");
        this.frame.add(this.jScrollBar, "Center");
        this.frame.setDefaultCloseOperation(3);
        this.frame.pack();
        this.frame.setLocationByPlatform(true);
        this.frame.setVisible(true);
    }

    public final void LogWatchDiff() {
        this.file = new File(this.jtfFile.getText());
        if (!this.file.exists()) {
            JOptionPane.showMessageDialog((Component) null, "OOPS~ Check File Path or Name~!");
            this.t.interrupt();
        }

        if (this.file.exists() && this.file.canRead()) {
            this.stopBtn.setEnabled(true);
            this.startBtn.setEnabled(false);
            this.fileLength = this.file.length();

            try {
                if (this.init == 0L) {
                    this.init = this.tailRandomAccessFile(this.file, this.fileLength);
                }

                this.readFile(this.file, this.init);

                do {
                    if (this.fileLength < this.file.length()) {
                        this.readFile(this.file, this.fileLength);
                        this.fileLength = this.file.length();
                    }
                } while (stopThread);
            } catch (IOException var2) {
                JOptionPane.showMessageDialog((Component) null, var2.getMessage());
            }
        }

    }

    private long tailRandomAccessFile(File file, Long fileLength) throws FileNotFoundException, IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        long seek = 0L;
        int lines = 0;
        randomAccessFile.seek(fileLength);

        for (seek = fileLength; seek > 0L; --seek) {
            randomAccessFile.seek(seek);
            char c = (char) randomAccessFile.read();
            if (c == '\n') {
                ++lines;
                if (this.jtfTailCnt.getText().equals("")) {
                    return 0L;
                }

                if (lines == this.TailCnt) {
                    break;
                }
            }
        }

        return seek;
    }

    public void readFile(File file, long init) throws IOException {
        String line = null;
        FileInputStream stream = new FileInputStream(file);
        FileChannel channel = stream.getChannel();
        channel.position(init);
        this.logReader = new BufferedReader(new InputStreamReader(stream, this.dataBaseBox.getSelectedItem().toString()));
        if (!this.jtfRegexp.getText().isEmpty()) {
            this.pattern = Pattern.compile(this.jtfRegexp.getText());
        }

        while ((line = this.logReader.readLine()) != null) {
            if (!this.jtfRegexp.getText().isEmpty()) {
                Matcher matcher = this.pattern.matcher(line);
                if (matcher.find()) {
                    this.textArea.append(line + "\n");
                }
            } else {
                this.textArea.append(line + "\n");
            }
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new digilog.com.LogWatcherViewer();
            }
        });
    }

    public class SelectLogFileListener implements ActionListener {
        public SelectLogFileListener() {
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(digilog.com.LogWatcherViewer.this.frame);
            if (result == 0) {
                digilog.com.LogWatcherViewer.this.file = chooser.getSelectedFile();
                digilog.com.LogWatcherViewer.this.fileName = digilog.com.LogWatcherViewer.this.file.getAbsolutePath();
                digilog.com.LogWatcherViewer.this.jtfFile.setText(digilog.com.LogWatcherViewer.this.fileName);
            }

        }
    }

    public class StartShowLogListener implements ActionListener {
        public StartShowLogListener() {
        }

        public void actionPerformed(ActionEvent e) {
            try {
                if (!digilog.com.LogWatcherViewer.this.jtfTailCnt.getText().equals("")) {
                    digilog.com.LogWatcherViewer.this.TailCnt = Integer.parseInt(digilog.com.LogWatcherViewer.this.jtfTailCnt.getText());
                }

                digilog.com.LogWatcherViewer.stopThread = true;
                digilog.com.LogWatcherViewer.this.init = 0L;
                digilog.com.LogWatcherViewer.this.t = new Thread(digilog.com.LogWatcherViewer.this.r);
                digilog.com.LogWatcherViewer.this.t.start();
            } catch (NumberFormatException var3) {
                JOptionPane.showMessageDialog((Component) null, "Check LogView Tail Line (Number!)");
            }

        }
    }

    public class StopShowLogListener implements ActionListener {
        public StopShowLogListener() {
        }

        public void actionPerformed(ActionEvent e) {
            if (digilog.com.LogWatcherViewer.this.t != null) {
                try {
                    digilog.com.LogWatcherViewer.this.logReader.close();
                } catch (IOException var3) {
                    var3.printStackTrace();
                }

                digilog.com.LogWatcherViewer.stopThread = false;
                digilog.com.LogWatcherViewer.this.stopBtn.setEnabled(false);
                digilog.com.LogWatcherViewer.this.startBtn.setEnabled(true);
            }

        }
    }

    public class WatcherThread implements Runnable {
        public WatcherThread() {
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted() && digilog.com.LogWatcherViewer.stopThread) {
                digilog.com.LogWatcherViewer.this.LogWatchDiff();
            }

        }
    }
}