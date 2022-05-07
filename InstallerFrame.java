import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class InstallerFrame extends JFrame {

    public String minecraftFolder;

    public static String version = "0.0.4";

    private JPanel openPage;

    private URL dataDownloadURL;

    private String latestCtVersion;
    private String latestCtFile;
    private Long latestSoopyV2Version;
    private HashMap<String, String> ctFileToVersion = new HashMap<String, String>();
    private URL ctDownloadURL;
    private URL soopyv2DownloadURL;

    public InstallerFrame() {
        super("SoopyV2 Installer v" + version);

        if (System.getenv("APPDATA") != null) {
            minecraftFolder = System.getenv("APPDATA") + File.separator + ".minecraft";
        } else {
            minecraftFolder = System.getProperty("user.home");
        }
        try {
            dataDownloadURL = new URL("http://soopymc.my.to/api/soopyv2/installerData.json");
        } catch (MalformedURLException e) {
        }

        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

        JLabel title = new JLabel("SoopyV2 Installer");
        title.setFont(new Font("Serif", Font.BOLD, 36));
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setVerticalAlignment(JLabel.TOP);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        this.add(title);

        this.add(new JSeparator());

        JPanel loadingPage = new JPanel();
        loadingPage.add(new JLabel("Downloading data..."));

        JProgressBar progressBar = new JProgressBar();

        progressBar.setIndeterminate(true);

        loadingPage.add(progressBar);

        this.openPage(loadingPage);

        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
        this.setResizable(false);

        new Thread(() -> {
            this.downloadData();

            this.openPage(this.selectMinecraftFilePanel());
        }).start();
    }

    private void resize() {
        this.update(this.getGraphics());
        this.pack();
        this.setSize(this.getSize().width + 100, this.getSize().height + 100);
    }

    private void openPage(JPanel page) {
        if (openPage != null) {
            this.remove(openPage);
        }

        openPage = page;

        this.add(openPage);

        this.resize();
    }

    public JPanel selectMinecraftFilePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Select your minecraft folder");
        title.setFont(new Font("Serif", Font.PLAIN, 36));
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setVerticalAlignment(JLabel.TOP);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(title);
        JLabel title2 = new JLabel("For windows users the default should be correct!");
        title2.setHorizontalAlignment(JLabel.CENTER);
        title2.setVerticalAlignment(JLabel.TOP);
        title2.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(title2);

        panel.add(titlePanel, BorderLayout.NORTH);

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(minecraftFolder));
        chooser.setDialogTitle("Select your minecraft directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        panel.add(chooser);

        JButton selectButton = new JButton("Select Folder");
        selectButton.setSize((int) selectButton.getSize().getWidth(), (int) selectButton.getSize().getHeight() * 2);

        selectButton.addActionListener(event -> {
            minecraftFolder = chooser.getCurrentDirectory().toString();

            if (!this.isValidMinecraftFolder(minecraftFolder)) {
                boolean parentIsMcFolder = this.isValidMinecraftFolder(new File(minecraftFolder).getParent());
                if (!parentIsMcFolder) {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "The selected folder is not a valid minecraft folder!\nDo you want to use it anyway?");
                    if (confirm != 0)
                        return;
                } else {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "The selected folder is not a valid minecraft folder!\nDo you want to select "
                                    + new File(minecraftFolder).getParent() + " instead?");
                    if (confirm == 0) {
                        minecraftFolder = new File(minecraftFolder).getParent();
                    }
                    if (confirm == 2) {
                        return;
                    }
                }
            }
            if (new File(minecraftFolder + File.separator + "skyclient").exists()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "You appear to have skyclient installed!\nDo you want to select the skyclient folder instead?");
                if (confirm == 0) {
                    minecraftFolder = minecraftFolder + File.separator + "skyclient";
                }
                if (confirm == 2) {
                    return;
                }
            }

            this.openPage(this.mainInstallPanel());
        });

        panel.add(selectButton, BorderLayout.SOUTH);

        return panel;
    }

    public JPanel mainInstallPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Boolean isChattriggersInstalled = this.isChattriggersInstalled();
        String chattriggersVersion = this.getChattriggersVersion();
        Boolean chattriggersVersionIsLatest = isChattriggersInstalled
                && this.latestCtVersion.equals(chattriggersVersion);
        Boolean isSoopyV2Installed = this.isSoopyV2Installed();
        String soopyV2Version = this.getSoopyV2Version();
        Boolean isSoopyV2Latest = this.isSoopyV2Latest();

        // Start of install Chattriggers
        JPanel ctInstallPanel = new JPanel();

        JLabel ctInstalledLabel = new JLabel(
                isChattriggersInstalled ? "Chattriggers: Installed (version " + chattriggersVersion + ")"
                        : "Chattriggers not installed");

        ctInstallPanel.add(ctInstalledLabel, BorderLayout.WEST);

        if (!isChattriggersInstalled || !chattriggersVersionIsLatest) {
            JButton installCtButton = new JButton(isChattriggersInstalled ? "Update" : "Install");

            installCtButton.addActionListener(event -> {
                new Thread(() -> {
                    this.installChattriggers();
                    this.openPage(mainInstallPanel());
                }).start();
            });

            ctInstallPanel.add(installCtButton, BorderLayout.EAST);
        } else {
            JButton deleteCtButton = new JButton("Delete");

            deleteCtButton.addActionListener(event -> {
                this.uninstallChattriggers();
                this.openPage(mainInstallPanel());
            });

            ctInstallPanel.add(deleteCtButton, BorderLayout.EAST);
        }
        panel.add(ctInstallPanel);
        // End of install Chattriggers

        // Start of install SoopyV2
        JPanel soopyV2InstallPanel = new JPanel();

        JLabel soopyV2InstallLabel = new JLabel(
                isSoopyV2Installed ? "SoopyV2: Installed (v" + soopyV2Version + ")" : "SoopyV2: Not Installed");

        soopyV2InstallPanel.add(soopyV2InstallLabel, BorderLayout.WEST);

        if (!isSoopyV2Installed || !isSoopyV2Latest) {
            JButton installSoopyV2Button = new JButton(isSoopyV2Latest ? "Install" : "Update");

            installSoopyV2Button.addActionListener(event -> {
                new Thread(() -> {
                    if (!isSoopyV2Latest)
                        this.uninstallSoopyV2();
                    this.installSoopyV2();
                    this.openPage(mainInstallPanel());
                }).start();
            });

            soopyV2InstallPanel.add(installSoopyV2Button, BorderLayout.EAST);
        } else {
            JButton deleteSoopyV2Button = new JButton("Delete");

            deleteSoopyV2Button.addActionListener(event -> {
                this.uninstallSoopyV2();
                this.openPage(mainInstallPanel());
            });

            soopyV2InstallPanel.add(deleteSoopyV2Button, BorderLayout.EAST);
        }
        panel.add(soopyV2InstallPanel);
        // End of install SoopyV2

        if (!isChattriggersInstalled || !isSoopyV2Installed) {
            JButton installAllButton = new JButton("Install All");

            installAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);

            installAllButton.addActionListener(event -> {
                new Thread(() -> {
                    if (!isChattriggersInstalled)
                        this.installChattriggers();
                    if (isSoopyV2Installed) {
                        if (!isSoopyV2Latest) {
                            this.uninstallSoopyV2();
                            this.installSoopyV2();
                        }
                    } else {
                        this.installSoopyV2();
                    }
                    this.openPage(mainInstallPanel());
                }).start();
            });

            panel.add(installAllButton);
        }

        return panel;
    }

    public void installChattriggers() {
        JPanel loadingPage = new JPanel();

        loadingPage.add(new JLabel("Installing ChatTriggers..."));

        JProgressBar progressBar = new JProgressBar();

        progressBar.setIndeterminate(true);

        loadingPage.add(progressBar);

        this.openPage(loadingPage);

        if (!new File(minecraftFolder + File.separator + "mods").exists())
            new File(minecraftFolder + File.separator + "mods").mkdirs();

        for (String key : ctFileToVersion.keySet()) {
            if (new File(minecraftFolder + File.separator + "mods" + File.separator + "" + key + ".jar").exists())
                new File(minecraftFolder + File.separator + "mods" + File.separator + "" + key + ".jar").delete();
        }
        String downloadLoc = minecraftFolder + File.separator + "mods";
        if (new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9").exists()) {
            for (String key : ctFileToVersion.keySet()) {
                if (new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9" + File.separator + ""
                        + key + ".jar").exists())
                    new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9" + File.separator + ""
                            + key + ".jar").delete();
            }

            downloadLoc = minecraftFolder + File.separator + "mods" + File.separator + "1.8.9";
        }

        this.urlToFile(ctDownloadURL, downloadLoc + File.separator + "" + latestCtFile + ".jar", 10000, 10000);
    }

    public void uninstallChattriggers() {
        for (String key : ctFileToVersion.keySet()) {
            if (new File(minecraftFolder + File.separator + "mods" + File.separator + "" + key + ".jar").exists())
                new File(minecraftFolder + File.separator + "mods" + File.separator + "" + key + ".jar").delete();
        }
        if (new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9").exists()) {
            for (String key : ctFileToVersion.keySet()) {
                if (new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9" + File.separator + ""
                        + key + ".jar").exists())
                    new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9" + File.separator + ""
                            + key + ".jar").delete();
            }
        }
    }

    public void installSoopyV2() {

        JPanel loadingPage = new JPanel();

        loadingPage.add(new JLabel("Installing SoopyV2..."));

        JProgressBar progressBar = new JProgressBar();

        progressBar.setIndeterminate(true);

        loadingPage.add(progressBar);

        this.openPage(loadingPage);

        if (!new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules").exists())
            new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                    + "modules").mkdirs();

        if (new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules" + File.separator + "SoopyV2").exists())
            this.deleteDirectory(new File(
                    minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                            + "modules" + File.separator + "SoopyV2"));

        this.urlToFile(soopyv2DownloadURL,
                minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                        + "modules" + File.separator + "SoopyV2.zip",
                10000,
                10000);

        this.unzip(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules" + File.separator + "SoopyV2.zip",
                minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                        + "modules");

        new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules" + File.separator + "SoopyV2.zip").delete();
    }

    public void uninstallSoopyV2() {
        if (new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules" + File.separator + "SoopyV2").exists())
            this.deleteDirectory(new File(
                    minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                            + "modules" + File.separator + "SoopyV2"));
    }

    private void urlToFile(URL url, String destination, int connecttimeout, int readtimeout) {
        File d = new File(destination);
        d.getParentFile().mkdirs();
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        connection.setDoOutput(true);
        connection.setConnectTimeout(connecttimeout);
        connection.setReadTimeout(readtimeout);
        try (InputStream IS = connection.getInputStream()) {
            PrintStream FilePS = new PrintStream(destination);
            byte[] buf = new byte[65536];
            int len = 0;
            try {
                while ((len = IS.read(buf)) > 0) {
                    FilePS.write(buf, 0, len);
                }
                IS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FilePS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void unzip(String zipFile, String unziploc) { // code translated from chattriggers FileLib.unzip
        File unzipDir = new File(unziploc);
        if (!unzipDir.exists())
            unzipDir.mkdir();

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String filePath = unziploc + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    this.extractFile(zipIn, filePath);
                } else {
                    // if the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractFile(ZipInputStream zipIn, String filePath) {
        try {
            File toWrite = new File(filePath);
            toWrite.getParentFile().mkdirs();
            toWrite.createNewFile();

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
            byte[] bytesIn = new byte[4096];
            int read = zipIn.read(bytesIn);
            while (read != -1) {
                bos.write(bytesIn, 0, read);
                read = zipIn.read(bytesIn);
            }
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public Boolean isChattriggersInstalled() {

        if (!new File(minecraftFolder).exists())
            return false;
        if (!new File(minecraftFolder + File.separator + "mods").exists())
            return false;

        for (String key : ctFileToVersion.keySet()) {
            if (new File(minecraftFolder + File.separator + "mods" + File.separator + "" + key + ".jar").exists())
                return true;
        }
        if (!new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9").exists())
            return false;

        for (String key : ctFileToVersion.keySet()) {
            if (new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9" + File.separator + ""
                    + key + ".jar").exists())
                return true;
        }

        return false;
    }

    public Boolean isSoopyV2Installed() {

        if (!new File(minecraftFolder).exists())
            return false;
        if (!new File(minecraftFolder + File.separator + "config").exists())
            return false;
        if (!new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers").exists())
            return false;
        if (!new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules").exists())
            return false;
        if (!new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules" + File.separator + "SoopyV2").exists())
            return false;

        return true;
    }

    public Boolean isSoopyV2Latest() {
        if (!new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules" + File.separator + "SoopyV2" + File.separator + "metadata.json").exists())
            return false;
        JSONParser parser = new JSONParser();
        try {
            Scanner scanner = new Scanner(new File(
                    minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                            + "modules" + File.separator + "SoopyV2" + File.separator + "metadata.json"));
            String file = "";
            while (scanner.hasNextLine()) {
                file += scanner.nextLine();
            }
            scanner.close();

            JSONObject jobj = (JSONObject) parser.parse(file);
            Long version = (Long) jobj.get("versionId");

            return latestSoopyV2Version == version;
        } catch (FileNotFoundException e) {
        } catch (ParseException e) {
        }
        return false;
    }

    public String getSoopyV2Version() {
        if (!new File(minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                + "modules" + File.separator + "SoopyV2" + File.separator + "metadata.json").exists())
            return "";
        JSONParser parser = new JSONParser();
        try {
            Scanner scanner = new Scanner(new File(
                    minecraftFolder + File.separator + "config" + File.separator + "ChatTriggers" + File.separator
                            + "modules" + File.separator + "SoopyV2" + File.separator + "metadata.json"));
            String file = "";
            while (scanner.hasNextLine()) {
                file += scanner.nextLine();
            }
            scanner.close();

            JSONObject jobj = (JSONObject) parser.parse(file);
            String version = (String) jobj.get("version");

            return version;
        } catch (FileNotFoundException e) {
        } catch (ParseException e) {
        }
        return "";
    }

    public String getChattriggersVersion() {
        if (!new File(minecraftFolder).exists())
            return null;
        if (!new File(minecraftFolder + File.separator + "mods").exists())
            return null;

        for (String key : ctFileToVersion.keySet()) {
            if (new File(minecraftFolder + File.separator + "mods" + File.separator + "" + key + ".jar").exists())
                return ctFileToVersion.get(key);
        }
        if (!new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9").exists())
            return null;

        for (String key : ctFileToVersion.keySet()) {
            if (new File(minecraftFolder + File.separator + "mods" + File.separator + "1.8.9" + File.separator + ""
                    + key + ".jar").exists())
                return ctFileToVersion.get(key);
        }

        return null;
    }

    public void downloadData() {
        try {
            HttpURLConnection conn = (HttpURLConnection) dataDownloadURL.openConnection();

            conn.connect();
            int responsecode = conn.getResponseCode();
            if (responsecode != 200) {
                JOptionPane.showMessageDialog(this,
                        "Error loading download urls and latest version data",
                        "Network connection failed",
                        JOptionPane.WARNING_MESSAGE);
                System.exit(1);
            }

            Scanner sc = new Scanner(conn.getInputStream());
            String data = "";
            while (sc.hasNext()) {
                data += sc.nextLine();
            }
            sc.close();

            JSONParser parse = new JSONParser();

            JSONObject jobj = (JSONObject) parse.parse(data);

            String latestVersion = (String) jobj.get("latestInstallerVersion");

            if (!version.equals(latestVersion)) {
                JOptionPane.showMessageDialog(this,
                        "This installer is version v" + version
                                + " while the latest version of the installer is version v" + latestVersion,
                        "Installer not up to date",
                        JOptionPane.WARNING_MESSAGE);
            }

            latestCtVersion = (String) jobj.get("latestCtVersion");

            latestCtFile = (String) jobj.get("latestCtFile");

            latestSoopyV2Version = (Long) jobj.get("latestSoopyV2Version");

            JSONObject ctFileToVersiond = (JSONObject) jobj.get("ctFileToVersion");

            for (Object key : ctFileToVersiond.keySet().toArray()) {
                ctFileToVersion.put((String) key, (String) ctFileToVersiond.get((String) key));
            }

            ctDownloadURL = new URL((String) jobj.get("ctDownloadURL"));
            soopyv2DownloadURL = new URL((String) jobj.get("soopyv2DownloadUrl"));
        } catch (IOException | ParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading download urls and latest version data",
                    "Network connection failed",
                    JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean isValidMinecraftFolder(String path) {
        if (!new File(path + File.separator + "logs").exists())
            return false;
        if (!new File(path + File.separator + "saves").exists())
            return false;
        if (!new File(path + File.separator + "options.txt").exists())
            return false;
        return true;
    }
}